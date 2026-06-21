# CLAUDE.md

Руководство по работе с репозиторием **errlog** - сервисом централизованного сбора и анализа ошибок информационных систем. ВКР (Java, Spring Boot). Документация и коммиты - на русском, идентификаторы кода - на английском.

## Обзор

Сквозной конвейер («стенд» = два Docker Compose-контура):

```
Jerrgen (генератор логов) → stdout JSON
  → Vector (читает docker logs, фильтрует WARN/ERROR/FATAL, обогащает) → Kafka topic errors-raw
    → Ingestor (нормализация → fingerprint → запись) → ClickHouse (error_events)
      → Errapi (JWT-аутентификация + роли, читает ClickHouse) → REST API
```

- `errlog-core`: Kafka, Ingestor, ClickHouse, PostgreSQL, Errapi.
- `errlog-demo`: Jerrgen (×N реплик) + Vector. Заменяем реальными источниками.
- `at-least-once`: Ingestor подтверждает Kafka offset только после успешной записи в ClickHouse.

Полное функциональное описание - в `README.md` (аккуратно и выверено). `REVIEW.md` - если есть, это незакоммиченный черновик протокола, не учитывать для решений по коду.

## Модули (три независимых Maven-проекта)

Каждый модуль - отдельный Maven-проект со своим `pom.xml` и `./mvnw` (без родительского агрегатора). Базовый пакет `ru.vspochernin.<module>`. Spring Boot 3.5.10, Java 21, Lombok.

- **jerrgen** - `spring-boot-starter-web` + actuator. `SimpleGenerator` по расписанию эмитит WARN/ERROR/INFO-логи (Logstash JSON через `logback-spring.xml`). Внешних зависимостей нет; тривиальный.
- **ingestor** - `spring-kafka` + `spring-boot-starter-jdbc` + `clickhouse-jdbc`. Читает Kafka батчами, нормализует, строит fingerprint, батчем пишет в ClickHouse. Тесты: `spring-kafka-test`, Testcontainers (ClickHouse).
- **errapi** - `spring-boot-starter-web/security/validation/data-jpa` + Flyway (Postgres) + `clickhouse-jdbc` + jjwt 0.11.5 + springdoc. Тесты: `spring-security-test`, Testcontainers (PostgreSQL + ClickHouse). Два датасорса: Postgres (`@Primary`, JPA+Flyway) и ClickHouse (ручной JDBC, `@Qualifier("clickhouseJdbcTemplate")`).

---

## Ingestor

### Слои и пакеты

**kafka**
- `RawEventJsonKafkaListener` (`@Component`, `@RequiredArgsConstructor`, `@Slf4j`) - точка входа батчей. `@KafkaListener(topics = "${KAFKA_TOPIC}")`, метод `listen(List<String> rawEventJsons, Acknowledgment ack)`. Batch-режим (`listener.type: batch`), ручной ack (`ack-mode: manual`).

**processing**
- `RawEventJsonProcessor` (interface) - `Optional<ErrorEvent> process(String rawEventJson)`.
- `DefaultRawEventJsonProcessor` (`@Component`) - оркестратор: парсинг JSON → нормализация → fingerprint → `ErrorEvent`. Поля: `ObjectMapper`, `RawEventNormalizerRegistry`, `FingerprintBuilder`.

**normalization**
- `RawEventNormalizer` (interface) - `String sourceType()`, `Optional<NormalizedErrorEvent> normalize(JsonNode rawEvent)`.
- `JavaSpringLogbackRawEventNormalizer` - нормализатор для `sourceType() == "java-spring-logback"`.
- `UnknownRawEventNormalizer` - fallback. `SOURCE_TYPE = "__unknown__"`, `normalize` всегда возвращает empty.
- `RawEventNormalizerRegistry` - реестр нормализаторов по sourceType + default.

**fingerprint**
- `FingerprintBuilder` (interface) - `FingerprintResult build(NormalizedErrorEvent)`.
- `DefaultFingerprintBuilder` - 4 ветки fingerprint по приоритету. `Pattern DIGITS = Pattern.compile("\\d")`.
- `FingerprintResult` (record) - `(String fingerprintBase, FingerprintSource fingerprintSource)`.
- `FingerprintSource` (enum) - `STACKTRACE, EXCEPTION, MESSAGE_TEMPLATE, MINIMAL`.

**writer**
- `ErrorEventWriter` (interface) - `void write(List<ErrorEvent> events)`.
- `ClickHouseEventWriter` - батч-INSERT в ClickHouse. `INSERT_BATCH_SIZE = 1000`.

**model** - `NormalizedErrorEvent` (record, 13 полей), `ErrorEvent` (record: `UUID eventId, NormalizedErrorEvent, FingerprintResult`).
**dto** - `JavaSpringLogbackRawEventDto` (record + вложенные `ThrowableDto`/`StepDto` + `getStacktraceFormatted()`).
**utils** - `StringUtils` (3 статических метода, приватный конструктор).

### Логика `RawEventJsonKafkaListener.listen()`

1. Для каждой строки вызывает `processor.process(...)`; успешные попадают в `events`, пропуски считаются `skipCount`.
2. Если `events` не пуст → `writer.write(events)` в try/catch: при исключении логирует и **пробрасывает наружу** (ack НЕ вызывается → Kafka переотдаст батч, at-least-once).
3. Если `events` пуст → `writer.write` не вызывается.
4. В конце (если не было исключения) → `ack.acknowledge()` + `log.info(... total/inserted/skipped)`.

Порядок: сначала `write`, потом `ack`. Пустой батч → ack вызывается.

### Логика `DefaultRawEventJsonProcessor.process()`

Возвращает `Optional<ErrorEvent>`, **никогда не пробрасывает исключения** (всё глотается с `log.warn`):
1. null/blank вход → `empty` (без логирования, без вызовов registry/fingerprint).
2. `objectMapper.readTree(json)` в try/catch → ошибка парсинга: warn + `empty`.
3. `sourceType = rawEvent.path("sourceType").asText(null)` (отсутствие → null).
4. `registry.getNormalizer(sourceType)` → `normalize(rawEvent)` в try/catch → исключение: warn + `empty`.
5. Пустой результат нормализатора → warn + `empty` (fingerprint НЕ строится).
6. `fingerprintBuilder.build(normalized)` в try/catch → исключение: warn + `empty`.
7. Успех → `Optional.of(new ErrorEvent(UUID.randomUUID(), normalized, fingerprint))`. **`eventId` случайный** - в тестах игнорировать.

### Нормализация

`JavaSpringLogbackRawEventNormalizer.normalize`:
- `objectMapper.treeToValue(rawEvent, JavaSpringLogbackRawEventDto.class)` в try/catch `JsonProcessingException` → ошибка: error + `empty`.
- `ts = dto.timestamp()`. **Если `ts <= 0`** → error + `empty`.
- Маппинг в `NormalizedErrorEvent`:
  - `timestamp` = `Instant.ofEpochMilli(ts)`
  - `sourceType` = `"java-spring-logback"` (всегда константа, не из DTO)
  - `service` = `getOrDefault(dto.service(), "__unknown-service__")`
  - `level` = `getOrDefault(dto.level(), "__UNKNOWN__")`
  - `messageFormatted` = `getFirstNonBlankOrDefault(dto.formattedMessage(), dto.message(), "__empty message__")` - **приоритет formattedMessage над message**
  - `instance`, `serviceVersion`, `logger`(=loggerName), `thread`(=threadName), `messageTemplate`(=message) - как есть, nullable
  - `exceptionClass` = `throwable != null ? throwable.className() : null`
  - `exceptionMessage` = `throwable != null ? throwable.message() : null`
  - `stacktrace` = `dto.getStacktraceFormatted()`

`UnknownRawEventNormalizer`: всегда `log.error` + `Optional.empty()`.

### `RawEventNormalizerRegistry`

- Конструктор из `List<RawEventNormalizer>`: строит `Map<String, RawEventNormalizer>` по `sourceType`. **Дубликат sourceType** → `IllegalStateException` (сообщение содержит "Duplicate"). **Отсутствие UnknownRawEventNormalizer** → `IllegalStateException` (сообщение содержит "default raw event normalizer").
- `getNormalizer(String sourceType)` = `Optional.ofNullable(sourceType).map(bySourceType::get).orElse(default)`.
  - `sourceType == null` → default.
  - **Неизвестный non-null sourceType** → `bySourceType::get` вернёт null → `map` даст empty → `orElse(default)` → **default** (а не null). Оба случая заканчиваются пропуском события.

### Fingerprint - `DefaultFingerprintBuilder`

Подготовка: `service/logger/level` через `getOrDefault(..., "")` (null/blank → пустая строка). Четыре ветки по приоритету (if-else):
1. **STACKTRACE** (высший): `isNotBlank(stacktrace)` → `base = service|logger|level|stacktraceNoDigits`, где `stacktraceNoDigits = DIGITS.matcher(stacktrace).replaceAll("")`. **Цифры удаляются ТОЛЬКО из stacktrace.**
2. **EXCEPTION**: stacktrace blank, но `isNotBlank(exceptionClass) && isNotBlank(exceptionMessage)` (оба) → `base = service|logger|level|exceptionClass|exceptionMessage`. Цифры НЕ удаляются.
3. **MESSAGE_TEMPLATE**: `isNotBlank(messageTemplate)` → `base = service|logger|level|messageTemplate`. Цифры НЕ удаляются.
4. **MINIMAL**: `base = service|logger|level`.

Разделитель `"|"`. Одинаковые stacktrace, отличающиеся только цифрами → одинаковый `fingerprintBase`.

### Writer - `ClickHouseEventWriter`

`INSERT_SQL` (таблица `errlog_ch.error_events`, 16 колонок):
```sql
INSERT INTO errlog_ch.error_events (event_id, timestamp, source_type, service, level,
  message_formatted, fingerprint, fingerprint_source, instance, service_version, logger,
  thread, message_template, exception_class, exception_message, stacktrace)
VALUES (?,?,?,?,?,?,xxh3(?),?,?,?,?,?,?,?,?,?)
```
- `write(List<ErrorEvent>)`: ранний return на null/пустом списке; иначе `jdbcTemplate.batchUpdate(INSERT_SQL, events, 1000, setter)`.
- Параметр 7 = `fingerprintBase` (строка) → ClickHouse считает `xxh3(?)` → `UInt64`. **Хэш в SQL, не в Java.**
- Параметр 8 = `fingerprintSource.name()`.
- Параметры 9-16 (instance, serviceVersion, logger, thread, messageTemplate, exceptionClass, exceptionMessage, stacktrace) могут быть null (`Nullable(String)` в схеме).

### DTO `JavaSpringLogbackRawEventDto` и `getStacktraceFormatted()`

Поля (record, все `@JsonProperty`): `formattedMessage, instance, level, loggerName, message, service, serviceVersion, sourceType, threadName, throwable (ThrowableDto), timestamp (long)`.
- `ThrowableDto(className, message, stepArray: List<StepDto>)`.
- `StepDto(className, fileName, lineNumber: int, methodName)`.

`getStacktraceFormatted()`:
1. `throwable == null` → `null`.
2. Если `isNotBlank(className)`: append `className`; если `isNotBlank(message)`: append `": " + message`; append `'\n'`.
3. `steps = stepArray`; если null/пуст → `sb.isEmpty() ? null : sb.toString().trim()` (только заголовок или null).
4. Для каждого step: append `"\tat " + getOrDefault(className, "Unknown className") + "." + getOrDefault(methodName, "Unknown methodName") + "(" + formatLocation(...) + ")\n"`.
5. `return sb.toString().trim()`.

`formatLocation(fileName, lineNumber)`:
- `fileName == null || isBlank || equals("null")` → `"Unknown source"`. **Строка `"null"` обрабатывается как отсутствие файла.**
- `lineNumber > 0` → `fileName + ":" + lineNumber`.
- Иначе (<=0) → `fileName` (без номера).

### `StringUtils`

- `getOrDefault(value, default)`: null/blank → default.
- `getFirstNonBlankOrDefault(value1, value2, default)`: первый non-blank из value1, value2; иначе default.
- `isNotBlank(value)`: `value != null && !value.isBlank()`. **Blank = null ИЛИ пустая ИЛИ только пробелы.**

### Конфигурация (`application.yaml`)

```yaml
spring:
  application.name: ingestor
  kafka:
    bootstrap-servers: ${KAFKA_BOOTSTRAP_SERVERS}
    consumer:
      group-id: ${KAFKA_GROUP_ID}
      enable-auto-commit: false
      auto-offset-reset: earliest
      key/value-deserializer: StringDeserializer
      max-poll-records: 1000
    listener:
      type: batch
      ack-mode: manual
  datasource:
    url: ${CLICKHOUSE_JDBC_URL}
    username: ${CLICKHOUSE_USER}
    password: ${CLICKHOUSE_PASSWORD}
    driver-class-name: com.clickhouse.jdbc.ClickHouseDriver
```

Все env без дефолтов. Тестового `application.yaml` нет.

### Env Ingestor (обязательны)

`KAFKA_BOOTSTRAP_SERVERS`, `KAFKA_GROUP_ID`, `KAFKA_TOPIC` (в `@KafkaListener`), `CLICKHOUSE_JDBC_URL`, `CLICKHOUSE_USER`, `CLICKHOUSE_PASSWORD`.

---

## Errapi

### Слои и пакеты

**controller**
- `AuthController` (`/api/auth`, без классовой `@PreAuthorize`): `POST /register` (201, permitAll), `POST /login` (200, permitAll), `PUT /password` (204, требует JWT, `@AuthenticationPrincipal`).
- `ErrorsController` (`/api/errors`, классовая `@PreAuthorize("hasAnyRole('READER','ADMIN','OWNER')")`): `GET /filters`, `POST /events` (limit/offset, body опционален), `GET /events/{eventId}`, `POST /groups`, `POST /timeseries` (bucket опционален).
- `UserController` (`/api/users`, классовая `@PreAuthorize("hasAnyRole('ADMIN','OWNER')")`): `GET`, `GET /{id}`, `PUT /{id}/role?role=` (роль биндится в `UserRole` enum).

**service**
- `AuthService` (`UserRepository`, `PasswordEncoder`, `AuthenticationManager`, `JwtService`).
- `ErrorsService` (`ErrorsRepository`).
- `UserService` (`UserRepository`).

**repository**
- `UserRepository` (`JpaRepository<User, Long>`): `findByLogin`, `existsByLogin`, `existsByEmail`, `existsByRole`.
- `ErrorsRepository` (`@Repository`, `@Qualifier("clickhouseJdbcTemplate") NamedParameterJdbcTemplate`, `ErrorEventRowMapper`, таблица `errlog_ch.error_events`).

**security** - `SecurityConfig`, `JwtService`, `JwtAuthenticationFilter`, `AuthUserDetails`, `AuthUserDetailsService`, `JsonAuthenticationEntryPoint`, `JsonAccessDeniedHandler`, `SecurityErrorWriter`.
**config** - `DataSourcesConfig`, `ErrorsAllowlist`, `OpenApiConfig`.
**dto/auth**, **dto/errors** - records.
**model/auth** - `User` (JPA entity), `UserRole`.
**model/errors** - records/enums.
**exception** - `ErrapiException`, `ErrapiErrorType`, `ErrorMessage`, `ExceptionApiHandler`.
**util** - `ErrorsWhereBuilder`, `ErrorsFiltersParser`, `FingerprintParser`, `TimeWindowParser`, `ValidationUtils`.
**mapper** - `ErrorEventRowMapper`.
**init** - `OwnerInitializer`.

### Security

`SecurityConfig` (`@EnableMethodSecurity`):
- `PasswordEncoder` → `BCryptPasswordEncoder`.
- `securityFilterChain`: CSRF off, `STATELESS`, exception handling → `JsonAuthenticationEntryPoint` (401) + `JsonAccessDeniedHandler` (403). permitAll: `/api/auth/register`, `/api/auth/login`, `/swagger-ui/**`, `/v3/api-docs/**`, `/actuator/health/**`, `/actuator/info`. Остальное `authenticated()`. `JwtAuthenticationFilter` добавлен `addFilterBefore(..., UsernamePasswordAuthenticationFilter.class)`.

`JwtService` (`@Component`): конструктор `@Value("${JWT_SECRET}")`, `@Value("${JWT_EXPIRES_SECONDS}")`. Секрет → UTF-8 байты; **если < 32 байт → `IllegalArgumentException`** (HS256). Методы: `generateToken(login)` (subject=login, HS256), `extractLogin(token)` (subject), `isValid(token)` (true/false, ловит `JwtException`/`IllegalArgumentException`).

`JwtAuthenticationFilter.doFilterInternal`:
1. Нет заголовка `Authorization` или не `Bearer ` → `filterChain.doFilter` (пропуск).
2. `token = header.substring(7).trim()`. Если `!isValid(token)` → `request.setAttribute(ATTR_ERROR_TYPE, INVALID_TOKEN)`; продолжить цепочку; return.
3. Если контекст пуст: `login = extractLogin(token)`; `userDetailsService.loadUserByUsername(login)` с catch `UsernameNotFoundException` → `setAttribute(ATTR_ERROR_TYPE, USER_DOES_NOT_EXIST)`; продолжить; return.
4. `UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities())` → `SecurityContextHolder.getContext().setAuthentication(auth)`.
5. `filterChain.doFilter`.

`AuthUserDetails.getAuthorities()` → `List.of(new SimpleGrantedAuthority("ROLE_" + role.name()))`. **В БД роль без префикса, Spring требует `ROLE_`.**

`JsonAuthenticationEntryPoint.ATTR_ERROR_TYPE = "ERRAPI_AUTH_ERROR_TYPE"` (public static final). Если атрибут null → `AUTH_REQUIRED`.

`@PreAuthorize`: `ErrorsController` → `hasAnyRole('READER','ADMIN','OWNER')`; `UserController` → `hasAnyRole('ADMIN','OWNER')`; `AuthController` - без классовой, `/register` и `/login` permitAll, `/password` требует authenticated.

### Сервисы

`AuthService`:
- `register`: `existsByLogin` → `LOGIN_EXISTS`; `existsByEmail` → `EMAIL_EXISTS`; создаёт `User` с ролью `NONE`, хеш через `passwordEncoder.encode`; `UserDto.fromUser(save)`.
- `login`: `authenticationManager.authenticate(UsernamePasswordAuthenticationToken(login, password))` (выброс `BadCredentialsException` пробрасывается); `new LoginResponse(jwtService.generateToken(login))`.
- `changePassword(request, actor)`: `passwordEncoder.matches(oldPassword, actor.getPassword())` иначе `PASSWORD_DOES_NOT_MATCH`; **затем** если `oldPassword.equals(newPassword)` → `INVALID_PASSWORD` ("New password matches the old one") - порядок важен; `findById(actor.getId())` иначе `BAD_CREDENTIALS`; `setPasswordHash(encode(newPassword))`; `save`.

`ErrorsService` (`ErrorsRepository`):
- `getFilters()` → `ErrorsAllowlist.FIELDS`.
- `getEvents(request, limit, offset)`: `ValidationUtils.validateLimitOffset`; `ErrorsQuery.parseFromErrorsRequest`; `countEvents` → eventsTotal; `findEvents` → items.
- `getEventById(eventId)`: `validateUuid`; `findEventById` → `ErrorsEventResponse`, иначе `NOT_FOUND` ("Event not found").
- `getGroups(request, limit, offset)`: валидация; parse; `countEventsAndGroupsTotals`; `findGroups`.
- `getTimeseries(request, bucketRaw)`: parse query (**БЕЗ валидации limit/offset** - нет пагинации); bucket null/blank → `TimeBucket.byTimeWindow(timeWindow)`, иначе `byName(bucketRaw)`; `findTimeseries`; ответ с `bucket.getName()`.

`UserService`:
- `listUsers`, `getUser(id)` (не найден → `NOT_FOUND`).
- `changeRole(targetUserId, newRole, actor)`: `targetUserId == actor.getId()` → `INCORRECT_ROLE_CHANGE` ("Can't change own role"); `actorRole.validateCanModify(targetRole, newRole)`; `setRole(newRole)`; `save`.

### Роли (`UserRole`)

`OWNER(3) > ADMIN(2) > READER(1) > NONE(0)`. `validateCanModify(targetRole, newRole)`:
- target == OWNER → `INCORRECT_ROLE_CHANGE` ("Can't change owner role").
- newRole == OWNER → "Can't assign owner role".
- `targetRole.level >= this.level` → "Can't change higher or equal role".
- `newRole.level >= this.level` → "Can't assign higher or equal role".

Новые пользователи регистрируются как `NONE`. Примечание: READER (1) может изменить NONE (0), т.к. 0 < 1.

### Слой errors - парсеры/билдеры

`ErrorsWhereBuilder.buildWhere(query, columnPrefix)` (static): всегда `"{prefix}timestamp >= :from AND {prefix}timestamp < :to"` (params `from`, `to` = `Timestamp.from(...)`). Если `fingerprintO` present → `AND {prefix}fingerprint = toUInt64(:fingerprint)` (значение `BigInteger.toString()`). Для каждого фильтра: колонка `prefix + field.column()`, параметр `filter_{n}` (n=0,1,...); `EQ` → `= :filter_N`, `NE` → `!= :filter_N`, `IN` → `IN (:filter_N)` (список), `LIKE` → `LIKE :filter_N`; default → `BAD_REQUEST`. `Where` record `(String sql, MapSqlParameterSource params)`.

`ErrorsFiltersParser.parse(List<Filter>)` (static): null/пусто → `List.of()`. Иначе валидация каждого: null-элемент, blank field (`ErrorsAllowlist.byName`), blank operation (`FilterOperation.byName`), `checkOperationSupport`, пустые values, множественные values для не-IN. Возвращает `List<ErrorsFilter>`.

`FingerprintParser.parseO(String)` (static): `MAX_UINT64 = 2^64-1`. null/blank → `Optional.empty()`. Не все цифры → `BAD_REQUEST` ("fingerprint must be UInt64 string"). Отрицательный или > MAX → `BAD_REQUEST` ("out of range UInt64"). Возвращает `Optional<BigInteger>`.

`TimeWindowParser` (static, `DEFAULT_TIME_WINDOW = 24h`):
- `parse(Instant from, Instant to)`: to=null → now; from=null → to−24h; from>to → `INCORRECT_TIME_BORDERS` ("from must be <= to").
- `parse(String, String)`: blank → null; `Instant.parse` с catch → `INCORRECT_TIME_BORDERS` ("must be ISO-8601 UTC").

`ValidationUtils` (static, `LIMIT_MIN=1, LIMIT_MAX=500`): `validateLimitOffset` (limit вне [1,500] → `BAD_REQUEST`; offset<0 → "Offset can't be negative"); `validateUuid` (`UUID.fromString` с catch → "eventId must be UUID").

`TimeBucket` (enum): `M1("1m",1m,"INTERVAL 1 MINUTE")`, `M5`, `M15`, `H1`, `H6`, `D1`. `byName` → lookup иначе `BAD_REQUEST`. `byTimeWindow` (duration from..to): ≤1ч→M1, ≤6ч→M5, ≤24ч→M15, ≤7д→H1, ≤30д→H6, иначе D1.

`ErrorsAllowlist.FIELDS` - 11 полей (name/column/operations/description): sourceType, service, level (БЕЗ LIKE!), messageFormatted, instance, serviceVersion, logger, thread, messageTemplate, exceptionClass, exceptionMessage. `byName` иначе `BAD_REQUEST`.

`FilterOperation` (enum): `EQ("eq",false)`, `NE("ne",false)`, `IN("in",true)`, `LIKE("like",false)`. `byName` иначе `BAD_REQUEST`. `IN` - единственная с `allowsMultipleValues=true`.

`ErrorsQuery.parseFromErrorsRequest` (static): `TimeWindowParser.parse` + `FingerprintParser.parseO` + `ErrorsFiltersParser.parse` → `new ErrorsQuery(timeWindow, fingerprintO, filters)`.

### `ErrorsRepository` (ClickHouse)

- `countEvents(query)` → `SELECT count() ... WHERE` → `Long` (null → 0).
- `findEvents(query, limit, offset)` → SELECT полей, `toString(fingerprint) AS fingerprint_str`, **`CAST(NULL,'Nullable(String)') AS stacktrace`** (намеренно NULL), `ORDER BY timestamp DESC, event_id DESC`, `LIMIT :limit OFFSET :offset`.
- `findEventById(eventId)` → SELECT со **stacktrace** (полный), `WHERE event_id = toUUID(:eventId)`, `LIMIT 1` → `Optional`.
- `countEventsAndGroupsTotals(query)` → `count() AS events_total, uniqExact(fingerprint) AS groups_total` → `EventsAndGroupsTotals`.
- `findGroups(query, limit, offset)` → WHERE с префиксом `"src."`; `GROUP BY src.fingerprint`; агрегаты `argMax(col, tuple(timestamp, event_id))`; `ORDER BY group_count DESC, group_last_seen DESC, group_fingerprint DESC`.
- `findTimeseries(query, bucket)` → `toStartOfInterval(timestamp, %s) AS bucket_start, count() ... GROUP BY bucket_start ORDER BY bucket_start ASC` (bucket.getIntervalSql() подставляется в SQL - НЕ параметр).

`ErrorEventRowMapper` читает `fingerprint_str` (именно `_str`), `event_id`, `timestamp` (Timestamp→Instant), остальные колонки через `getString`.

### Два датасорса (`DataSourcesConfig`)

- Postgres: `@Bean @Primary @ConfigurationProperties("spring.datasource") DataSourceProperties`; `@Bean @Primary @FlywayDataSource DataSource` (HikariDataSource). Для JPA + Flyway.
- ClickHouse: `@Bean(name="clickhouseDataSource")` из `@Value` `CLICKHOUSE_*`, `HikariConfig` (driver `ClickHouseDriver`, pool `"clickhouse-pool"`, maxPool=5, minIdle=0, connTimeout=10000, validationTimeout=5000). `@Bean(name="clickhouseJdbcTemplate") NamedParameterJdbcTemplate`.

### DTO и модели

**dto/auth** (records): `RegisterRequest` (`@NotBlank @Size(3..64) login`, `@NotBlank @Email @Size(max=255) email`, `@NotBlank @Size(6..128) password`), `LoginRequest` (`@NotBlank login/password`), `LoginResponse` (`token`), `ChangePasswordRequest` (`@NotBlank oldPassword`, `@NotBlank @Size(6..128) newPassword`), `UserDto` (`id, login, email, role` + `fromUser`).

**dto/errors** (records): `ErrorsRequest` (`from, to, fingerprint, List<Filter> filters` + `empty()`; `Filter(field, operation, values)`), `ErrorSmallDto` (`fromRow`, без stacktrace), `ErrorBigDto` (`@JsonUnwrapped ErrorSmallDto + stacktrace`, `fromRow`), `ErrorsEventResponse` (`@JsonUnwrapped ErrorBigDto`), `ErrorsEventsResponse` (`items + eventsTotal`), `ErrorsGroupsResponse` (`items + eventsTotal + groupsTotal`), `ErrorsTimeseriesResponse` (`items + bucketSize`), `ErrorsFiltersResponse` (`items`; `Item(name, operations, description)`).

**model/auth**: `User` (`@Entity @Table("users")`: `id BIGSERIAL`, `login VARCHAR(64) UNIQUE`, `email VARCHAR(255) UNIQUE`, `password_hash VARCHAR(128)`, `role VARCHAR(32) @Enumerated(STRING) DEFAULT 'NONE'`). `UserRole` (см. выше).

**model/errors**: `ErrorsQuery`, `ErrorsFilter`, `FilterField` (`checkOperationSupport`), `FilterOperation`, `TimeBucket`, `TimeWindow(from, to)`, `ErrorEventRow` (16 полей), `ErrorGroupRow(groupFingerprint, groupCount, groupLastSeen, lastEvent)`, `ErrorTimeseriesRow(bucketStart, count)`, `EventsAndGroupsTotals(eventsTotal, groupsTotal)`.

### Exception handling

`ErrapiException` (`extends RuntimeException`): `ErrapiErrorType errorType`, `String additionalInfo`. Конструкторы вызывают `super(...)`: без info → `super(description)`; с info → `super(description + ": " + additionalInfo)` (если info не blank). **Формат сообщения: `description` или `description: additionalInfo`.** (Раньше `getMessage()` возвращал null - баг, исправлен.)

`ErrapiErrorType` (enum, `id/description/httpStatus`): `UNEXPECTED_ERROR(0,500)`, `LOGIN_EXISTS(1,400)`, `EMAIL_EXISTS(2,400)`, `INVALID_LOGIN(3,400)`, `INVALID_EMAIL(4,400)`, `INVALID_PASSWORD(5,400)`, `PASSWORD_DOES_NOT_MATCH(6,401)`, `INCORRECT_ROLE_CHANGE(7,403)`, `BAD_REQUEST(8,400)`, `NOT_FOUND(9,404)`, `BAD_CREDENTIALS(10,401)`, `USER_DOES_NOT_EXIST(11,401)`, `AUTH_REQUIRED(12,401)`, `INVALID_TOKEN(13,401)`, `FORBIDDEN(14,403)`, `INCORRECT_TIME_BORDERS(15,400)`.

`ErrorMessage` (record `id, description, errorType, additionalInfo`): фабрики `fromErrorType`, `fromErrorTypeWithAdditionalInfo`, `fromErrapiException`.

`ExceptionApiHandler` (`@RestControllerAdvice`): `ErrapiException` → статус из типа; `BadCredentialsException` → `BAD_CREDENTIALS`; `MethodArgumentNotValidException` → маппинг поля (login→INVALID_LOGIN, email→INVALID_EMAIL, password→INVALID_PASSWORD, default→BAD_REQUEST); `HttpMessageNotReadableException`/`NumberFormatException`/`MethodArgumentTypeMismatchException` → `BAD_REQUEST`; `NoSuchElementException` → `NOT_FOUND`; `AuthorizationDeniedException`/`AccessDeniedException` → `FORBIDDEN`; `Exception` → `UNEXPECTED_ERROR`.

### Конфигурация

`application.yaml`: `spring.datasource` (Postgres, `${POSTGRES_*}`), `jpa.open-in-view: false`, `hibernate.ddl-auto: validate`, `flyway.enabled: true` (classpath:db/migration). Миграция `V1__implement_db_schema.sql` создаёт `users` (BIGSERIAL PK, UNIQUE login/email, CHECK role IN ('NONE','READER','ADMIN','OWNER'), DEFAULT 'NONE'). `OpenApiConfig` - security scheme `bearerAuth`.

### Env Errapi (обязательны)

`POSTGRES_JDBC_URL/USER/PASSWORD`, `CLICKHOUSE_JDBC_URL/USER/PASSWORD`, `JWT_SECRET` (≥32 байта), `JWT_EXPIRES_SECONDS`, `ERRLOG_OWNER_LOGIN/EMAIL/PASSWORD` (при первом старте, пока нет OWNER).

### `OwnerInitializer`

`@Component implements ApplicationRunner`. Env через `System.getenv`. Если `existsByRole(OWNER)` → return (идемпотентно). Иначе читает 3 env (null/blank → `IllegalStateException`); если `existsByLogin`/`existsByEmail` → `IllegalStateException`; создаёт OWNER.

---

## Сборка и тесты - ВНИМАНИЕ

**Дефолтная `java` на машине - JDK 17, проекту нужна JDK 21.** Maven упадёт с `release version 21 not supported` без явного указания:

```bash
export JAVA_HOME=$(/usr/libexec/java_home -v 21)
```

Команды - **по модулям** (сначала cd в модуль):

```bash
cd ingestor && JAVA_HOME=$(/usr/libexec/java_home -v 21) ./mvnw test          # юниты
JAVA_HOME=$(/usr/libexec/java_home -v 21) ./mvnw verify                       # юниты + интеграционные (нужен Docker)
JAVA_HOME=$(/usr/libexec/java_home -v 21) ./mvnw -Dtest=DefaultFingerprintBuilderTest test   # один класс

# Всё разом (из корня репозитория):
./scripts/run-all-tests.sh
```

Верхнего Maven-команды нет - каждый модуль отдельный. `run-all-tests.sh` прогоняет все модули.

### Текущее состояние тестов

224 теста (199 юнит + 23 интеграционных + 2 отключённых smoke). Подробно - в `TESTING.md`.
- jerrgen: 1 тест - `contextLoads()` проходит автономно.
- ingestor: 79 юнит + 10 интеграционных (Testcontainers: ClickHouse). `./mvnw test` без инфраструктуры; `./mvnw verify` требует Docker.
- errapi: 119 юнит + 13 интеграционных (Testcontainers: ClickHouse + PostgreSQL). `./mvnw test` без инфраструктуры; `./mvnw verify` требует Docker.
- Интеграционные - Maven Failsafe (`*IT.java`), фаза `verify`. Testcontainers reuse (`.withReuse(true)`).
- `IngestorApplicationTests` и `ErrapiApplicationTests` - `@Disabled` smoke-тесты: требуют полный Spring-контекст + живую инфраструктуру + env. Инструкция запуска - в javadoc каждого класса.

### Стратегия тестирования

Предпочитать **чистые юниты** и **Mockito** вместо `@SpringBootTest`. Доменная логика изолирована в статических утилитах / компонентах без зависимостей - покрывать в первую очередь.

**Чистые юниты (без Spring, без инфры):**
- ingestor: `DefaultFingerprintBuilder` (4 ветки; удаление цифр **только** в STACKTRACE), `StringUtils` (blank не только null), `JavaSpringLogbackRawEventDto.getStacktraceFormatted()` (null throwable, пустой stepArray, `fileName=="null"`, `lineNumber<=0`), `JavaSpringLogbackRawEventNormalizer` (реальный `new ObjectMapper()`; дефолты, `ts<=0` → empty), `UnknownRawEventNormalizer`, `RawEventNormalizerRegistry`.
- errapi: `FingerprintParser`, `TimeWindowParser`, `ErrorsFiltersParser`, `ErrorsWhereBuilder`, `TimeBucket` (границы `byName`/`byTimeWindow`), `ValidationUtils`, `ErrorsAllowlist`/`FilterField`/`FilterOperation`, `ErrorsQuery.parseFromErrorsRequest`, `UserRole.validateCanModify`, `JwtService` (`new JwtService(secret, seconds)`, секрет ≥32 байта), `ErrapiErrorType`/`ErrorMessage`, `*Dto.fromRow/fromUser`, `AuthUserDetails.getAuthorities` (префикс `ROLE_`).

**Mockito (без контекста):** `DefaultRawEventJsonProcessor` (мок registry + builder, реальный ObjectMapper; eventId случайный - игнорировать), `ClickHouseEventWriter` (мок `JdbcTemplate`; ранний return, `batchUpdate`), `RawEventJsonKafkaListener` (прямой вызов `listen(...)`, мок processor/writer/`Acknowledgment`; ack при успехе, **без ack** при ошибке writer), `ErrorsService`/`UserService`/`AuthService`, `JwtAuthenticationFilter`, `ExceptionApiHandler`.

**Интеграционные (Testcontainers):** реальные SQL `ErrorsRepository` и JPA/Flyway `UserRepository` покрыты через `*IT.java`. Зависимости Testcontainers уже в обоих pom.

## Нюансы, важные для сверки тестов

- **Fingerprint: ветка `EXCEPTION` недостижима для `java-spring-logback`.** `exceptionClass` берётся из `throwable.className`, который формирует заголовок стектрейса → если класс есть, STACKTRACE выигрывает; если нет - условие EXCEPTION не выполняется. Корректный fallback для других источников. Покрыто тестами (отдельный кейс с прямым конструированием модели).
- **`xxh3` считается в ClickHouse, не в Java.** `ClickHouseEventWriter` отдаёт сырой `fingerprintBase`, `xxh3(?)` - в SQL. Проверено интеграционно.
- **`at-least-once` протестирована дважды.** Юнит (ошибка writer → исключение пробрасывается, ack не вызывается) + интеграционный (остановка контейнера ClickHouse → writer падает → ack не вызывается).
- **`eventId` недетерминирован** (`UUID.randomUUID()`). В тестах процессора игнорируется.
- **`Instant.now()` внутри `TimeWindowParser`/`JwtService`** - тесты используют `isCloseTo` (допуск 5с) или передают оба значения.
- **`ErrapiException` вызывает `super(message)`.** Формат: `description` или `description: additionalInfo`. Production-фикс (раньше `getMessage()` возвращал null).
- **`level` - единственное поле без LIKE** в allowlist.
- **`FilterOperation.IN` - единственная с `allowsMultipleValues=true`**; для остальных `values.size() > 1` → ошибка.
- **`getTimeseries` НЕ валидирует limit/offset** (нет пагинации).
- **`findEvents` намеренно возвращает `stacktrace = NULL`**; полный stacktrace только в `findEventById`.
- **`changePassword`**: порядок проверок - сначала `PASSWORD_DOES_NOT_MATCH` (неверный старый), потом `INVALID_PASSWORD` (новый == старый).

## Known gaps (не покрыто юнитами)

- **`OwnerInitializer`** - читает `System.getenv()`, нельзя замокать без Spring-контекста. Нужен `@SpringBootTest` с env. Логика простая, проверена вручную.
- **Контроллеры** (`AuthController`, `ErrorsController`, `UserController`) - тонкий прокси, логика в сервисах (покрыты). Нужен `@WebMvcTest`.
- **Конфиги** (`DataSourcesConfig`, `OpenApiConfig`, `SecurityConfig`) - тестируются фактом запуска.

## Соглашения

- Стиль: Lombok (`@RequiredArgsConstructor`, `@Slf4j`), constructor injection, `record` для DTO/value objects, статические утилиты с приватным конструктором. Комментарии на русском, идентификаторы на английском.
- ClickHouse-таблица `errlog_ch.error_events` создаётся `docker/clickhouse/init.sql` (внешняя). Таблица `users` - Flyway `errapi/.../db/migration/V1__implement_db_schema.sql`.
- Ветвление: нумерованные feature-ветки (напр. `9-test-ingestor`); PR squash-merge в `master`. Коммитить только по просьбе. В конце сообщений - Co-Authored-By trailer.

## Запуск стенда (для ручных/интеграционных проверок)

```bash
docker compose -f docker/docker-compose.core.yml up -d --build   # из корня репозитория
docker compose -f docker/docker-compose.demo.yml up -d --build
```

Скрипты в `scripts/` (из корня): `restart-core.sh`, `restart-demo.sh`, `restart-stand.sh`, `stop-stand.sh`, `stop-stand-remove-volumes.sh`, `run-all-tests.sh`. Swagger: `http://localhost:8080/swagger-ui/index.html`.

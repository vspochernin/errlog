# Тестирование

Этот документ описывает тестовую стратегию проекта, состав тестов и нюансы покрытия. Краткая сводка и команды запуска есть в `README.md` (раздел "Тесты").

## Стратегия

Тесты делятся на три слоя - от быстрых и изолированных к медленным и инфраструктурным:

1. **Чистые юнит-тесты** (JUnit 5 + AssertJ). Никакого Spring, никакой инфраструктуры. Тестируют доменную логику, изолированную в статических утилитах и компонентах без зависимостей. Запускаются за миллисекунды, не требуют ничего, кроме JDK 21.
2. **Mockito-тесты** (JUnit 5 + Mockito). Без Spring-контекста, но с моками зависимостей. Тестируют компоненты, которые обращаются к репозиториям, Kafka, ClickHouse, кодировщику паролей и т.д.
3. **Интеграционные тесты** (Testcontainers). Поднимают реальные контейнеры Kafka, ClickHouse и PostgreSQL и проверяют работу с настоящей инфраструктурой: SQL-запросы, `xxh3`-хэш в ClickHouse, миграции Flyway и constraint'ы PostgreSQL.

Принцип: **сначала покрываем чистые юниты (максимальная ценность при минимальной стоимости), затем Mockito, и только инфраструктурно-зависимые части - через Testcontainers.** `@SpringBootTest` используется минимально и только как smoke-тесты полного контекста (см. раздел "Smoke-тесты").

## Как писались тесты

Тесты спроектированы, написаны и отлажены с помощью LLM (Claude Code). Процесс:

- **Сверка с кодом.** Тесты писались на основе реальной реализации - веток логики, форматов сообщений, схемы БД. Расхождения фиксировались по факту: например, тест, утверждавший что `RawEventNormalizerRegistry` возвращает `null` для неизвестного `sourceType`, падал и был исправлен (registry отдаёт default-нормализатор).
- **Прогон.** Каждый набор запускался реальным `./mvnw test` / `verify` (юниты - без инфраструктуры, интеграционные - с Testcontainers). Красный результат доводился до зелёного.
- **Баги в production-коде.** При написании тестов на `ErrapiException` обнаружено, что конструкторы не вызывали `super(message)`, и `getMessage()` возвращал `null`. Исправлено (см. "Нюансы и особенности").
- **Known gaps.** Компоненты, которые нельзя покрыть чистыми юнитами без Spring-контекста (`OwnerInitializer`, контроллеры, конфиги), зафиксированы как непокрытые с объяснением, а не тестировались для галочки.

## Состав

| Модуль | Юнит-тесты | Интеграционные | Всего |
|---|---|---|---|
| Ingestor | 79 (+1 disabled) | 10 | 90 |
| Errapi | 119 (+1 disabled) | 13 | 133 |
| Jerrgen | 1 | - | 1 |
| **Всего** | **200** | **23** | **224** |

Все юнит-тесты запускаются без внешней инфраструктуры (только JDK 21). Интеграционные требуют Docker.

## Запуск

```bash
# Всё одной командой (рекомендуется)
./scripts/run-all-tests.sh

# По модулям - только юниты (быстро, без Docker)
cd ingestor && JAVA_HOME=$(/usr/libexec/java_home -v 21) ./mvnw test
cd errapi && JAVA_HOME=$(/usr/libexec/java_home -v 21) ./mvnw test

# По модулям - юниты + интеграционные (нужен Docker)
cd ingestor && JAVA_HOME=$(/usr/libexec/java_home -v 21) ./mvnw verify
cd errapi && JAVA_HOME=$(/usr/libexec/java_home -v 21) ./mvnw verify

# Один класс
cd ingestor && ./mvnw -Dtest=DefaultFingerprintBuilderTest test
```

**JDK 21 обязательно.** На этой машине JDK 17 по умолчанию, поэтому везде нужен `export JAVA_HOME=$(/usr/libexec/java_home -v 21)` - иначе Maven падает с `release version 21 not supported`.

Интеграционные тесты запускаются Maven Failsafe Plugin по классам `*IT.java` на фазе `verify`. Testcontainers переиспользует контейнеры между запусками (`.withReuse(true)`), поэтому повторные прогоны быстрее первого.

## Ingestor

### Чистые юниты - 60 тестов

| Класс | Тестов | Что проверяет |
|---|---|---|
| `StringUtilsTest` | 13 | `getOrDefault`, `getFirstNonBlankOrDefault`, `isNotBlank` - null, empty, blank |
| `DefaultFingerprintBuilderTest` | 12 | 4 ветки fingerprint, приоритет веток, удаление цифр только в STACKTRACE, null-логгер |
| `JavaSpringLogbackRawEventDtoTest` | 15 | `getStacktraceFormatted()`: null throwable, заголовок, stepArray, `fileName="null"`, границы lineNumber |
| `JavaSpringLogbackRawEventNormalizerTest` | 13 | Маппинг JSON->модель, дефолты (`__unknown-service__`/`__UNKNOWN__`), `ts<=0`, битый JSON, приоритет formattedMessage |
| `UnknownRawEventNormalizerTest` | 3 | `sourceType()`, всегда empty |
| `RawEventNormalizerRegistryTest` | 5 | Выбор нормализатора, null->default, unknown non-null->default, дубликат, отсутствие default |

### Mockito - 18 тестов

| Класс | Тестов | Что проверяет |
|---|---|---|
| `DefaultRawEventJsonProcessorTest` | 9 | null/blank/битый JSON, normalizer бросает/empty, fingerprint бросает, unknown sourceType, happy path |
| `ClickHouseEventWriterTest` | 4 | null/пустой список->ранний return, batchUpdate с проверкой всех 16 параметров PreparedStatement |
| `RawEventJsonKafkaListenerTest` | 5 | Все успешны, все пропущены, частично, ошибка writer->исключение пробрасывается и ack не вызывается, пустой батч |

### Интеграционные (Testcontainers) - 10 тестов

| Класс | Тестов | Что проверяет |
|---|---|---|
| `ClickHouseEventWriterIT` | 6 | Реальный INSERT в ClickHouse, `xxh3`-хэш (одинаковая база -> одинаковый fingerprint, разные базы -> разные), null/пустой список |
| `RawEventJsonKafkaListenerIT` | 4 | Сквозной тест: процессор->нормализация->fingerprint->ClickHouse, неизвестный sourceType пропускается, ошибка writer -> ack не вызывается |

## Errapi

### Чистые юниты - 94 теста

| Класс | Тестов | Что проверяет |
|---|---|---|
| `FingerprintParserTest` | 8 | null/blank->empty, не-цифры->BAD_REQUEST, 0, 2^64-1, на 1 больше, отрицательное |
| `TimeWindowParserTest` | 8 | from/to заданы, null->now, null->now-24ч, from>to, невалидный формат, пустые строки |
| `ValidationUtilsTest` | 7 | limit [1,500], offset>=0, валидный/невалидный UUID |
| `ErrorsWhereBuilderTest` | 8 | Базовый WHERE, fingerprint, EQ/NE/IN/LIKE, columnPrefix, несколько фильтров |
| `TimeBucketTest` | 10 | `byName` (все 6) + неизвестный, `byTimeWindow` (все 6 границ) |
| `FilterFieldTest` | 2 | checkOperationSupport - supported/unsupported |
| `FilterOperationTest` | 4 | `byName` (все 4) + неизвестный, in allows multiple, eq/ne/like нет |
| `ErrorsAllowlistTest` | 3 | `byName` - существует/не существует, level без LIKE |
| `ErrorsQueryTest` | 2 | parseFromErrorsRequest - полный, пустой |
| `UserRoleTest` | 10 | OWNER меняет всё кроме OWNER, ADMIN меняет READER/NONE, READER меняет NONE, запреты |
| `JwtServiceTest` | 6 | Секрет <32->ошибка, генерация, извлечение логина, валидный, битый токен |
| `ErrorsFiltersParserTest` | 12 | null/empty, одиночный/множественные фильтры, null-элемент, blank поле, неизвестное поле/операция, unsupported, пустые/множественные значения, IN с множеством |
| `ExceptionApiHandlerTest` | 14 | ErrapiException (с/без info), BadCredentials, MethodArgumentNotValid (с ошибкой поля/без, mapping login/email/password), HttpMessageNotReadable, NumberFormat, NoSuchElement, TypeMismatch, запрет доступа, generic Exception |

### Mockito - 25 тестов

| Класс | Тестов | Что проверяет |
|---|---|---|
| `ErrorsServiceTest` | 9 | getFilters, getEvents (успех, невалидный limit), getEventById (успех, UUID, not found), getGroups, getTimeseries (авто/ручной бакет) |
| `UserServiceTest` | 5 | listUsers, getUser (найден/не найден), changeRole (успех, своя роль) |
| `AuthServiceTest` | 7 | register (успех, login exists, email exists), login (успех, bad credentials), changePassword (успех, старый не совпадает) |
| `JwtAuthenticationFilterTest` | 4 | Без заголовка, валидный токен, невалидный токен, пользователь не найден |

### Интеграционные (Testcontainers) - 13 тестов

| Класс | Тестов | Что проверяет |
|---|---|---|
| `ErrorsRepositoryIT` | 8 | Реальные SQL к ClickHouse: countEvents, findEvents (пагинация), findEventById (найден/не найден), countEventsAndGroupsTotals, findGroups, findTimeseries, фильтрация |
| `UserRepositoryIT` | 5 | Реальная БД на PostgreSQL + Flyway: создание, уникальность логина, CHECK-ограничение роли, existsByRole, existsByLogin |

## Jerrgen

1 тест. `contextLoads()` - `@SpringBootTest`, проходит без инфраструктуры (генератор не имеет внешних зависимостей). Jerrgen - демонстрационный источник логов, бизнес-логики для тестирования нет.

## Smoke-тесты (`@Disabled`)

`IngestorApplicationTests` и `ErrapiApplicationTests` - это `@SpringBootTest` smoke-тесты, которые поднимают полный Spring-контекст. Они **отключены**, потому что:

- Читают `application.yaml`, где env-переменные (`KAFKA_BOOTSTRAP_SERVERS`, `CLICKHOUSE_JDBC_URL`, `POSTGRES_*`, `JWT_SECRET`, `ERRLOG_OWNER_*`) заданы **без дефолтов**.
- `@KafkaListener` (ingestor) и JPA/Flyway (errapi) при старте контекста требуют живую инфраструктуру.
- Контур стенда сам не прокидывает эти переменные в JVM теста - их нужно передавать вручную.

Тесты **рабочие** - они проходят, когда поднят core-контур и переданы env-переменные, указывающие на localhost-порты контейнеров. Точная команда запуска - в javadoc каждого класса. Бизнес-логика полностью покрыта отдельными юнит- и интеграционными тестами, поэтому smoke-тесты не блокируют обычный прогон `./mvnw test`.

## Нюансы и особенности

- **Fingerprint: ветка `EXCEPTION` недостижима для `java-spring-logback`.** `exceptionClass` берётся из `throwable.className`, который одновременно формирует заголовок стектрейса -> если класс есть, STACKTRACE выигрывает; если нет - условие EXCEPTION не выполняется. Это поведение покрыто тестами (`DefaultFingerprintBuilderTest`), ветка EXCEPTION тестируется отдельным кейсом с прямым конструированием модели.
- **`xxh3` считается в ClickHouse, не в Java.** `ClickHouseEventWriter` отдаёт сырой `fingerprintBase`, а `xxh3(?)` вычисляется в SQL. Это проверено интеграционно (`ClickHouseEventWriterIT`): одинаковая база -> одинаковый fingerprint, разные базы -> разные.
- **`at-least-once` протестирована дважды.** Юнит-тест (`RawEventJsonKafkaListenerTest`) проверяет, что при ошибке writer исключение пробрасывается и `ack` не вызывается. Интеграционный тест (`RawEventJsonKafkaListenerIT`) повторяет это на реальном ClickHouse (контейнер останавливается, writer падает, ack не вызывается).
- **`eventId` недетерминирован** (`UUID.randomUUID()` в `DefaultRawEventJsonProcessor`). В тестах процессора он игнорируется в ассертах.
- **`Instant.now()` внутри `TimeWindowParser` и `JwtService`** - тесты используют диапазонные expectations (`isCloseTo` с допуском 5 секунд) или передают оба значения `from`/`to` явно.
- **`ErrapiException` вызывает `super(message)`.** Конструкторы передают описание ошибки (и `additionalInfo`) в `RuntimeException`, иначе `getMessage()` возвращал `null`. Формат: `description` или `description: additionalInfo`. Это production-фикс, обнаруженный при написании тестов.

## Что НЕ покрыто (known gaps)

- **`OwnerInitializer`** - читает конфигурацию через `System.getenv()`, что нельзя замокать без Spring-контекста. Для тестирования нужен `@SpringBootTest` с env-переменными. Логика простая, проверена вручную при запуске стенда.
- **Контроллеры** (`AuthController`, `ErrorsController`, `UserController`) - тонкий прокси-слой без бизнес-логики (вся логика в сервисах, которые покрыты). Для осмысленного покрытия потребовался бы `@WebMvcTest`.
- **Конфигурационные классы** (`DataSourcesConfig`, `OpenApiConfig`, `SecurityConfig`) - конфигурация бинов, тестируется фактом запуска приложения и smoke-тестами.

Эти пробелы не критичны: основная бизнес-логика и работа с инфраструктурой покрыты.

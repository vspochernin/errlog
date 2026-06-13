# errlog

Выпускная квалификационная работа «Сервис сбора и анализа ошибок информационных систем». 2-й семестр 2-го курса МИФИ. ИИКС РПО (2025-2026 уч. г.).

## Описание

`errlog` - сервис для централизованного сбора и анализа ошибок информационных систем.

Проект полезен в ситуациях, когда системы уже пишут логи, содержащие события ошибок, и хочется работать с ними как с отдельными проблемами: видеть группы однотипных ошибок, смотреть детали событий и анализировать динамику их возникновения во времени.

Идея проекта состоит в том, чтобы не заставлять источник специально отправлять ошибки через SDK. Источник продолжает писать события журналирования, внешний агент сбора забирает события нужных уровней, а ядро сервиса нормализует их, группирует по `fingerprint`, сохраняет в аналитическое хранилище и предоставляет REST API для поиска и анализа.

Проект настроен на работу с Java Spring Logback ошибками, но может быть универсально сконфигурирован для любого другого вида источника.

## Основные возможности

- Сбор событий ошибок уровней `WARN`, `ERROR` и `FATAL` из логов источников.
- Фильтрация и обогащение событий на стороне Vector.
- Асинхронная доставка событий через Kafka.
- Нормализация входных событий к единой модели ошибки.
- Группировка однотипных ошибок на основе детерминированного `fingerprint`.
- Хранение событий ошибок в ClickHouse.
- REST API для получения групп ошибок, отдельных событий, карточки события, временных рядов и списка доступных фильтров.
- Регистрация, аутентификация, JWT и роли пользователей.
- Swagger/OpenAPI-документация API.
- Воспроизводимый запуск через Docker Compose.

## Состав проекта

Проект разделен на 2 Docker Compose-контура:

- `errlog-core` - ядро сервиса: Kafka, Ingestor, ClickHouse, PostgreSQL, Errapi.
  - Этот контур представляет собой сам сервис сбора и анализа ошибок информационных систем.
- `errlog-demo` - демонстрационный контур: Jerrgen и Vector.
  - Этот контур нужен для демонстрации работы сервиса и имитации внешних микросервисов, генерирующих ошибки в процессе своей работы.
  - При подключении своих систем предлагается ориентироваться на реализацию данного контура.

Совместная работа обоих контуров называется стендом. При подключении реальных систем demo-контур может быть заменен на собственные источники событий и агент сбора логов.

## Состав репозитория

- `jerrgen` - генератор WARN/ERROR логов, написанный на Java (Spring Boot + Logback).
- `ingestor` - Java Spring Boot сервис, читает Kafka, нормализует события, вычисляет `fingerprint` и пишет события в ClickHouse.
- `errapi` - REST API с JWT и ролями для взаимодействия пользователей с сервисом.
- `docker/docker-compose.core.yml` - core-контур.
- `docker/docker-compose.demo.yml` - demo-контур.
- `docker/vector/vector.yaml` - конфигурация Vector.
- `docker/clickhouse/init.sql` - SQL-скрипт инициализации ClickHouse.
- `docker/kafka/init.sh` - скрипт инициализации Kafka и создания топика `errors-raw`.
- `scripts/` - вспомогательные скрипты запуска, остановки и перезапуска стенда.

## Принцип работы стенда

1. Jerrgen пишет логи в `stdout` в формате JSON Lines.
2. Vector в demo-контуре читает Docker-логи контейнеров с лейблом `errlog.collect=true`.
3. Vector отбрасывает не-JSON строки, оставляет только события уровней `WARN`, `ERROR`, `FATAL`, добавляет метаданные источника и отправляет события в Kafka из `errlog-core`.
4. Ingestor читает события из Kafka, нормализует их, вычисляет `fingerprint` и записывает результат в ClickHouse.
5. Errapi осуществляет аутентификацию пользователя, читает данные из ClickHouse и предоставляет API для поиска и аналитики по ошибкам.

Ingestor подтверждает Kafka offset только после успешной записи в ClickHouse, что обеспечивает семантику доставки сообщений `at-least-once`.

## Как подключить новый источник ошибок

В текущей версии поддерживается формат `java-spring-logback` - JSON-строки Java Spring Boot + Logback логов с выводом шаблона сообщения.

Чтобы подключить другой источник, например приложение на Python, Go или другом стеке, нужно выполнить два шага:

1. Доставить события в Kafka `errlog-core`.
2. Научить Ingestor обрабатывать новый `sourceType`.

### 1. Доставить события в Kafka `errlog-core`

- `errlog-core` принимает входные события из Kafka-топика `errors-raw`.
- Для внешних источников необходимо использовать внешний listener Kafka: `${ERRLOG_KAFKA_EXTERNAL_HOST}:9094` (см. `docker/.env`).
- Сообщение в Kafka должно быть в формате JSON: одна запись Kafka - одно событие.
- Сообщение должно содержать поле `sourceType` на верхнем уровне.

Минимальный пример JSON:

```json
{
  "sourceType": "my-app-source-type",
  "timestamp": 1770000000000,
  "service": "billing",
  "level": "ERROR",
  "message": "Something failed"
}
```

Поля можно расширять. Способ доставки сообщений в `errlog-core` может быть произвольным, но в проекте предлагается использовать Vector как удобный агент сбора и отправки логов.

Важно: если `sourceType` неизвестен Ingestor, событие будет пропущено.

### 2. Научить Ingestor обрабатывать новый `sourceType`

Ingestor выбирает нормализатор по полю `sourceType` и преобразует raw JSON в каноническую модель `NormalizedErrorEvent`.

Чтобы добавить новый формат, необходимо:

1. Создать класс, реализующий `RawEventNormalizer`.
2. Вернуть нужный `sourceType()` - строго то же значение, которое приходит в JSON-сообщениях.
3. В `normalize(JsonNode rawEvent)` распарсить поля события и вернуть `NormalizedErrorEvent`.
4. Пометить класс `@Component`, чтобы он автоматически попал в `RawEventNormalizerRegistry`.

Скелет:

```java
@Component
public class MyAppRawEventNormalizer implements RawEventNormalizer {

    @Override
    public String sourceType() {
        return "my-app-source-type";
    }

    @Override
    public Optional<NormalizedErrorEvent> normalize(JsonNode rawEvent) {
        // Распарсить timestamp/service/level/message и опциональные поля.
        // Вернуть new NormalizedErrorEvent(...).
    }
}
```

После этого сообщения с `sourceType = "my-app-source-type"` начнут приниматься и обрабатываться.

## Вычисление fingerprint

Для группировки ошибок у каждого обрабатываемого события в Ingestor вычисляется `fingerprint`.

Основа всегда начинается с:

```text
service|logger|level
```

Далее используется один из следующих вариантов:

- Если у события есть `stacktrace`: `service|logger|level|stacktraceWithoutDigits`, `fingerprintSource=STACKTRACE`.
- Иначе если у события одновременно есть `exceptionClass` и `exceptionMessage`: `service|logger|level|exceptionClass|exceptionMessage`, `fingerprintSource=EXCEPTION`.
- Иначе если у события есть `messageTemplate`: `service|logger|level|messageTemplate`, `fingerprintSource=MESSAGE_TEMPLATE`.
- Иначе: `service|logger|level`, `fingerprintSource=MINIMAL`.

Для уменьшения влияния шума из `stacktrace` (адреса, идентификаторы, номера строк) перед включением в основу из самого `stacktrace` удаляются все цифры. Остальные части основы при этом не изменяются.

Хэш вычисляется в ClickHouse как `xxh3(fingerprintBase)` и хранится как `UInt64`.

Так как `logger` является опциональным полем, в случае его отсутствия при вычислении `fingerprint` используется пустая строка.

## Конфигурация

В папке `docker` лежит `.env` - файл переменных окружения для Docker Compose.

Основной параметр сейчас один:

```dotenv
ERRLOG_KAFKA_EXTERNAL_HOST=host.docker.internal
```

Если оба контура подняты на одной машине, этого достаточно для корректной работы.

Если core- и demo-контуры находятся на разных машинах, значением данного параметра следует поставить IP или DNS машины, где поднят `errlog-core`. Например:

```dotenv
ERRLOG_KAFKA_EXTERNAL_HOST=192.168.1.50
```

## Запуск

Все команды ниже предполагают запуск **из корня репозитория**.

### 1. Поднять core-контур

```bash
docker compose -f docker/docker-compose.core.yml up -d --build
```

### 2. Поднять demo-контур

```bash
docker compose -f docker/docker-compose.demo.yml up -d --build
```

## Вспомогательные скрипты

В папке `scripts` представлены вспомогательные скрипты. Их также следует запускать **из корня репозитория**.

Перезапустить core-контур:

```bash
./scripts/restart-core.sh
```

Перезапустить demo-контур:

```bash
./scripts/restart-demo.sh
```

Перезапустить весь стенд:

```bash
./scripts/restart-stand.sh
```

Остановить весь стенд:

```bash
./scripts/stop-stand.sh
```

Остановить весь стенд с удалением Docker volumes проекта:

```bash
./scripts/stop-stand-remove-volumes.sh
```

## Проверки после запуска

### Core-контур

Статус контейнеров:

```bash
docker compose -f docker/docker-compose.core.yml ps -a
```

Ожидается, что:

- сервисы `kafka-init` и `clickhouse-init` будут завершены с `Exited (0)`;
- остальные сервисы будут находиться в состоянии `Up`.

Проверка ClickHouse:

```bash
curl "http://localhost:8123/?user=errlog_ch_user&password=errlog_ch_password&query=SELECT%201"
```

Здесь и далее `%20` - экранирование пробела в HTTP-запросе.

Ожидается вывод:

```text
1
```

Количество событий:

```bash
curl "http://localhost:8123/?user=errlog_ch_user&password=errlog_ch_password&query=SELECT%20count()%20FROM%20errlog_ch.error_events"
```

Ожидается вывод количества записанных событий. Значение должно стать ненулевым после запуска demo-контура и поступления событий.

Логи Ingestor:

```bash
docker logs -f errlog-core-ingestor-1
```

Ожидается поток логов обработки событий, если уже поднят demo-контур.

### Demo-контур

Статус контейнеров:

```bash
docker compose -f docker/docker-compose.demo.yml ps -a
```

Ожидается, что все сервисы будут находиться в состоянии `Up`.

Логи Vector:

```bash
docker logs -f errlog-demo-vector-1
```

Ожидается успешное подключение к Kafka и начало обработки логов.

## Errapi

Swagger доступен по адресу:

```text
http://localhost:8080/swagger-ui/index.html
```

Пользователь с ролью `OWNER` создается при старте Errapi из переменных окружения `ERRLOG_OWNER_*` (см. `docker/docker-compose.core.yml`), если такого пользователя еще нет в PostgreSQL.

### Получить JWT

```bash
curl -X POST "http://localhost:8080/api/auth/login" \
  -H "Content-Type: application/json" \
  -d '{"login":"owner","password":"owner_password"}'
```

Для удобства можно добавить полученный токен в переменную окружения:

```bash
export ERRLOG_OWNER_JWT="<token>"
```

Проверка корректности токена:

```bash
curl -sS \
  -H "Authorization: Bearer $ERRLOG_OWNER_JWT" \
  "http://localhost:8080/api/users"
```

Ожидается вывод информации о зарегистрированных пользователях.

## Errors API

### Allowlist фильтров

```bash
curl -sS \
  -H "Authorization: Bearer $ERRLOG_OWNER_JWT" \
  "http://localhost:8080/api/errors/filters" | jq
```

### Список событий

По умолчанию возвращаются события за последние 24 часа.

```bash
curl -sS \
  -H "Authorization: Bearer $ERRLOG_OWNER_JWT" \
  -H "Content-Type: application/json" \
  -X POST "http://localhost:8080/api/errors/events?limit=10&offset=0" \
  -d '{}' | jq
```

Пример с границами времени:

```bash
curl -sS \
  -H "Authorization: Bearer $ERRLOG_OWNER_JWT" \
  -H "Content-Type: application/json" \
  -X POST "http://localhost:8080/api/errors/events?limit=20&offset=0" \
  -d '{"from":"2026-02-24T00:00:00Z","to":"2027-02-25T00:00:00Z"}' | jq
```

Пример с фильтрами:

```bash
curl -sS \
  -H "Authorization: Bearer $ERRLOG_OWNER_JWT" \
  -H "Content-Type: application/json" \
  -X POST "http://localhost:8080/api/errors/events?limit=20&offset=0" \
  -d '{"filters":[{"field":"service","operation":"in","values":["jerrgen-alpha","jerrgen-gamma"]},{"field":"level","operation":"eq","values":["ERROR"]}]}' | jq
```

### Детальная информация по событию

```bash
curl -sS \
  -H "Authorization: Bearer $ERRLOG_OWNER_JWT" \
  "http://localhost:8080/api/errors/events/<eventId>" | jq
```

### Группы ошибок

```bash
curl -sS \
  -H "Authorization: Bearer $ERRLOG_OWNER_JWT" \
  -H "Content-Type: application/json" \
  -X POST "http://localhost:8080/api/errors/groups?limit=10&offset=0" \
  -d '{}' | jq
```

### Timeseries

Автоматический выбор размера бакета:

```bash
curl -sS \
  -H "Authorization: Bearer $ERRLOG_OWNER_JWT" \
  -H "Content-Type: application/json" \
  -X POST "http://localhost:8080/api/errors/timeseries" \
  -d '{}' | jq
```

Ручной выбор размера бакета:

```bash
curl -sS \
  -H "Authorization: Bearer $ERRLOG_OWNER_JWT" \
  -H "Content-Type: application/json" \
  -X POST "http://localhost:8080/api/errors/timeseries?bucket=1m" \
  -d '{}' | jq
```

## Пагинация событий

- Список событий сортируется по `timestamp DESC, eventId DESC`.
- Список групп событий сортируется по `groupCount DESC, groupLastSeen DESC, groupFingerprint DESC`.

Если поток живой и в систему продолжают приходить новые события, для стабильной offset-пагинации следует зафиксировать `to` на момент первого запроса и переиспользовать его на следующих страницах, меняя только `offset`.

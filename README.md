# errlog

Выпускная квалификационная работа «Сервис сбора и анализа ошибок информационных систем». 2-й семестр 2-го курса МИФИ.
ИИКС РПО (2025-2026 уч. г).

## Описание

`errlog` - сервис для сбора и анализа ошибок информационных систем.

В стенде есть:

- `jerrgen` - генератор логов в `stdout` (`JSON lines`)
- `vector` - читает docker-логи контейнеров генераторов по label `errlog.collect=true`, оставляет `WARN/ERROR`, обогащает события и пишет их в Kafka
- `kafka` + `kafka-init` - Kafka и одноразовое создание топика `errors-raw`
- `clickhouse` + `clickhouse-init` - ClickHouse и инициализация БД `errlog_ch` с таблицей `error_events`
- `ingestor` - читает `errors-raw`, нормализует события, вычисляет fingerprint и пишет данные в ClickHouse
- `postgres` - хранение пользователей и ролей для `errapi`
- `errapi` - REST API (JWT, роли, Swagger UI) для работы с пользователями и ошибками

## Пайплайн

`Generators (stdout json)` -> `Vector` -> `Kafka (errors-raw)` -> `Ingestor` -> `ClickHouse (errlog_ch.error_events)`

`Ingestor` делает `ack` только после успешной вставки в ClickHouse, поэтому семантика доставки - `at-least-once`.

## Fingerprint

Основа всегда начинается с: `service|logger|level`.

- Если есть `stacktrace`: `service|logger|level|exceptionClass|stacktraceWithoutDigits` и `fingerprintSource=STACKTRACE`
- Иначе если есть `messageTemplate`: `service|logger|level|messageTemplate` и `fingerprintSource=TEMPLATE`
- Иначе: `service|logger|level` и `fingerprintSource=MINIMAL`

Хэш вычисляется в ClickHouse как `xxh3(fingerprintBase)` -> `UInt64`.

## Быстрый старт

### 1. Поднять стенд

```bash
docker compose up -d --build
```

### 2. Проверить состояние контейнеров

```bash
docker compose ps -a
```

Ожидаемо:

- `kafka-init` и `clickhouse-init` завершены с `Exited (0)`
- остальные сервисы находятся в состоянии `Up`

## Проверка стенда

### 1. Kafka

Проверить, что в топик идут raw события:

```bash
docker compose exec kafka /opt/kafka/bin/kafka-console-consumer.sh   --bootstrap-server kafka:9092   --topic errors-raw   --from-beginning
```

### 2. Ingestor

Посмотреть логи вставок:

```bash
docker compose logs -f ingestor
```

### 3. ClickHouse

Проверить доступность:

```bash
curl "http://localhost:8123/?user=errlog_ch_user&password=errlog_ch_password&query=SELECT%201"
```

Проверить количество событий:

```bash
curl "http://localhost:8123/?user=errlog_ch_user&password=errlog_ch_password&query=SELECT%20count()%20FROM%20errlog_ch.error_events"
```

Посмотреть распределение по `fingerprint_source`:

```bash
curl "http://localhost:8123/?user=errlog_ch_user&password=errlog_ch_password&query=SELECT%20fingerprint_source,%20count()%20c%20FROM%20errlog_ch.error_events%20GROUP%20BY%20fingerprint_source%20ORDER%20BY%20c%20DESC"
```

Посмотреть топ повторяющихся `fingerprint`:

```bash
curl "http://localhost:8123/?user=errlog_ch_user&password=errlog_ch_password&query=SELECT%20fingerprint,%20count()%20c%20FROM%20errlog_ch.error_events%20GROUP%20BY%20fingerprint%20ORDER%20BY%20c%20DESC%20LIMIT%2020"
```

### 4. Postgres

Проверить доступность:

```bash
docker compose exec postgres psql -U errlog_pg_user -d errlog_pg -c "select 1;"
```

## Errapi

### Swagger UI

```text
http://localhost:8080/swagger-ui/index.html
```

### OWNER bootstrap

OWNER пользователь создается при старте `errapi` из переменных окружения `ERRLOG_OWNER_*` в `docker-compose.yml`, если владельца еще нет в БД.

### Получить JWT

```bash
curl -X POST "http://localhost:8080/api/auth/login"   -H "Content-Type: application/json"   -d '{"login":"owner","password":"owner_password"}' | jq
```

Чтобы не вставлять токен в каждый запрос вручную:

```bash
export ERRLOG_OWNER_JWT="<token>"
```

Проверка авторизации:

```bash
curl -H "Authorization: Bearer $ERRLOG_OWNER_JWT"   "http://localhost:8080/api/users" | jq
```

## Errors API

### 1. Allowlist фильтров

```bash
curl -sS   -H "Authorization: Bearer $ERRLOG_OWNER_JWT"   "http://localhost:8080/api/errors/filters" | jq
```

### 2. Список событий

По умолчанию вернет события за последние 24 часа.  
`stacktrace` в этом списке не возвращается.

```bash
curl -sS   -H "Authorization: Bearer $ERRLOG_OWNER_JWT"   -H "Content-Type: application/json"   -X POST "http://localhost:8080/api/errors/events?limit=10&offset=0"   -d '{}' | jq
```

С явными границами времени:

```bash
curl -sS   -H "Authorization: Bearer $ERRLOG_OWNER_JWT"   -H "Content-Type: application/json"   -X POST "http://localhost:8080/api/errors/events?limit=20&offset=0"   -d '{"from":"2026-02-24T00:00:00Z","to":"2026-02-25T00:00:00Z"}' | jq
```

Пример с фильтрами:

```bash
curl -sS   -H "Authorization: Bearer $ERRLOG_OWNER_JWT"   -H "Content-Type: application/json"   -X POST "http://localhost:8080/api/errors/events?limit=20&offset=0"   -d '{"filters":[{"field":"service","operation":"eq","values":["jerrgen-alpha"]},{"field":"level","operation":"in","values":["ERROR"]}]}' | jq
```

### 3. Детальная информация по событию

```bash
curl -sS   -H "Authorization: Bearer $ERRLOG_OWNER_JWT"   "http://localhost:8080/api/errors/events/<eventId>" | jq
```

### 4. Группы ошибок

```bash
curl -sS   -H "Authorization: Bearer $ERRLOG_OWNER_JWT"   -H "Content-Type: application/json"   -X POST "http://localhost:8080/api/errors/groups?limit=10&offset=0"   -d '{}' | jq
```

### 5. Timeseries

Автоматический выбор бакета:

```bash
curl -sS   -H "Authorization: Bearer $ERRLOG_OWNER_JWT"   -H "Content-Type: application/json"   -X POST "http://localhost:8080/api/errors/timeseries"   -d '{}' | jq
```

Явный бакет:

```bash
curl -sS   -H "Authorization: Bearer $ERRLOG_OWNER_JWT"   -H "Content-Type: application/json"   -X POST "http://localhost:8080/api/errors/timeseries?bucket=15m"   -d '{}' | jq
```

## Пагинация событий

При `offset`-пагинации на живом потоке новые события могут вклиниваться между страницами.

Если нужен стабильный просмотр списка, клиент может зафиксировать `to` на момент первого запроса и потом переиспользовать его при запросе следующих страниц, меняя только `offset`.

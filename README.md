# errlog

Выпускная квалификационная работа «Сервис сбора и анализа ошибок информационных систем». 2-й семестр 2-го курса МИФИ.
ИИКС РПО (2025-2026 уч. г).

## Быстрый старт

1) Поднять стенд:
```bash
docker compose up -d --build
```

2) Посмотреть, что init-контейнеры отработали, а остальные успешно работают:
```bash
docker compose ps -a
```
Ожидаемо: `kafka-init` и `clickhouse-init` завершены (`Exited (0)`), остальные сервисы `Up`.

## Что внутри (MVP)

- `jerrgen` - генератор логов (в stdout JSON lines).
- `vector` - читает docker-логи контейнеров генераторов по label `errlog.collect=true`, фильтрует WARN/ERROR, обогащает метаданными, пишет в Kafka.
- `kafka` + `kafka-init` - Kafka и одноразовое создание топика `errors-raw`.
- `clickhouse` + `clickhouse-init` - ClickHouse и одноразовая инициализация `errlog_ch` + таблицы `error_events`.
- `ingestor` - читает `errors-raw` батчами, делает нормализацию, вычисление фингерпринта, вставку в ClickHouse и ручной ack для кафки.
- `postgres` - хранение пользователей/ролей для `errapi` (Flyway + JPA).
- `errapi` - REST API сервис (JWT + роли, Swagger UI) для работы с пользователями и запросами к ошибкам.

## Пайплайн данных

`Generators (stdout json)` -> `Vector` -> `Kafka (errors-raw)` -> `Ingestor` -> `ClickHouse (errlog_ch.error_events)`

Семантика: Ingestor делает `ack` только после успешной вставки в ClickHouse -> **at-least-once** (дубликаты допустимы).

Errapi читает пользователей из Postgres и ошибки из ClickHouse.

## Fingerprint (упрощённый алгоритм)

Основа всегда начинается с: `service|logger|level`.

- Если есть `stacktrace`: `service|logger|level|exceptionClass|stacktraceWithoutDigits` и `fingerprintSource=STACKTRACE`.
- Иначе если есть `messageTemplate`: `service|logger|level|messageTemplate` и `fingerprintSource=TEMPLATE`.
- Иначе: `service|logger|level` и `fingerprintSource=MINIMAL`.

Хэш: вычисляется в ClickHouse как `xxh3(fingerprintBase)` -> `UInt64`.

## Проверка стенда

### 1) Проверить, что в Kafka идут raw события

```bash
docker compose exec kafka /opt/kafka/bin/kafka-console-consumer.sh --bootstrap-server kafka:9092 --topic errors-raw --from-beginning
```
Выведет сырые события (уже отфильтрованные/обогащённые Vector).

### 2) Посмотреть логи ingestor

```bash
docker compose logs -f ingestor
```
Должны быть логи об успешных вставках батчей.


### 3) Проверить ClickHouse (таблица и данные)

- Проверить, что ClickHouse жив:
```bash
curl "http://localhost:8123/?user=errlog_ch_user&password=errlog_ch_password&query=SELECT%201"
```

- Количество событий:
```bash
curl "http://localhost:8123/?user=errlog_ch_user&password=errlog_ch_password&query=SELECT%20count()%20FROM%20errlog_ch.error_events"
```

- Распределение по источнику fingerprint:
```bash
curl "http://localhost:8123/?user=errlog_ch_user&password=errlog_ch_password&query=SELECT%20fingerprint_source,%20count()%20c%20FROM%20errlog_ch.error_events%20GROUP%20BY%20fingerprint_source%20ORDER%20BY%20c%20DESC"
```

- Топ повторяющихся fingerprint:
```bash
curl "http://localhost:8123/?user=errlog_ch_user&password=errlog_ch_password&query=SELECT%20fingerprint,%20count()%20c%20FROM%20errlog_ch.error_events%20GROUP%20BY%20fingerprint%20ORDER%20BY%20c%20DESC%20LIMIT%2020"
```

### 4) Проверить Postgres

```bash
docker compose exec postgres psql -U errlog_pg_user -d errlog_pg -c "select 1;"
```

### 5) Проверить Errapi (Swagger + JWT)

- Swagger UI:
  - `http://localhost:8080/swagger-ui/index.html`

- OWNER пользователь создаётся при старте `errapi` (если в БД ещё нет владельца) из переменных окружения `ERRLOG_OWNER_*` в `docker-compose.yml`.

- Получить JWT:
```bash
curl -X POST "http://localhost:8080/api/auth/login" \
  -H "Content-Type: application/json" \
  -d '{"login":"owner","password":"owner_password"}'
```

- Дальше токен можно вставить в Swagger через кнопку **Authorize** или использовать в curl.

- Чтобы не копировать токен каждый раз, можно сохранить его в переменную окружения:
```bash
export ERRLOG_OWNER_JWT="<token>"
```

- Пример запроса:
```bash
curl -H "Authorization: Bearer $ERRLOG_OWNER_JWT" http://localhost:8080/api/users
```

- Проверить Errors API (шаг 1):

- Allowlist фильтров:
```bash
curl -sS -H "Authorization: Bearer $ERRLOG_OWNER_JWT" "http://localhost:8080/api/errors/filters"
```

- Список событий (по умолчанию последние 24 часа; без `stacktrace`):
```bash
curl -sS -H "Authorization: Bearer $ERRLOG_OWNER_JWT" "http://localhost:8080/api/errors/events"
```

- С явными границами времени:
```bash
curl -sS -H "Authorization: Bearer $ERRLOG_OWNER_JWT" "http://localhost:8080/api/errors/events?from=2026-02-24T00:00:00Z&to=2026-02-25T00:00:00Z&limit=20&offset=0"
```

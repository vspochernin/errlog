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
- `postgres` - поднят для будущего API (пока без активного использования).

## Пайплайн данных

`Generators (stdout json)` -> `Vector` -> `Kafka (errors-raw)` -> `Ingestor` -> `ClickHouse (errlog_ch.error_events)`

Семантика: Ingestor делает `ack` только после успешной вставки в ClickHouse -> **at-least-once** (дубликаты допустимы).

## Fingerprint (упрощённый алгоритм)

Основа всегда начинается с: `service|logger|level`.

- Если есть `stacktrace`: `service|logger|level|exceptionClass|stacktraceWithoutDigits` и `fingerprintSource=STACKTRACE`.
- Иначе если есть `messageTemplate`: `service|logger|level|messageTemplate` и `fingerprintSource=TEMPLATE`.
- Иначе: `service|logger|level` и `fingerprintSource=MINIMAL`.

Хэш: SHA-256, первые 8 байт -> `UInt64` в ClickHouse.

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

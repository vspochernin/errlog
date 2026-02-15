# errlog

Выпускная квалификационная работа «Сервис сбора и анализа ошибок информационных систем». 2-й семестр 2-го курса МИФИ.
ИИКС РПО (2025-2026 уч. г).

## Запуск стенда

- Запустить стенд:
  - `docker compose up -d --build`

Сервисы в стенде:
- `jerrgen` - генератор логов (INFO/WARN/ERROR), далее по пайплайну берутся только WARN/ERROR.
- `vector` - читает docker logs генераторов, фильтрует/обогащает и пишет в Kafka.
- `kafka` + `kafka-init` - Kafka (KRaft single-node) и одноразовая инициализация топика.
- `clickhouse` + `clickhouse-init` - ClickHouse и одноразовая инициализация БД/таблиц.
- `postgres` - пока "в холостую" (под будущий API).
- `ingestor` - читает `errors-raw` топик из Kafka и батчами пишет события в ClickHouse (at-least-once).

---

## Этап 1

### Реализовано

- Генератор ошибок запускается через Docker Compose и пишет INFO/WARN/ERROR события в ожидаемом JSON формате.

### Проверка

- Запустить стенд `docker compose up -d --build`.
- Проверить наличие генерируемых событий: `docker compose logs -f jerrgen`

---

## Этап 2

### Реализовано

- Стенд, состоящий из генератора ошибок, Vector, Kafka, ClickHouse, PostgreSQL запускается через Docker Compose.
- Генератор ошибок (jerrgen) пишет JSON события ошибок в stdout.
- Vector читает события из docker-логов генератора, оставляет только WARN и ERROR события и обогащает их метаданными контейнера.
- События попадают в Kafka топик `errors-raw` (который создается сервисом `kafka-init`).
- ClickHouse и PostgreSQL поднимаются и успешно работают (на этом этапе “в холостую”).

### Проверка

- Запустить стенд `docker compose up -d --build`.
- Проверить наличие событий ошибок в Kafka:
  - `docker compose exec kafka /opt/kafka/bin/kafka-console-consumer.sh --bootstrap-server kafka:9092 --topic errors-raw --from-beginning`
- Проверить работу ClickHouse:
  - `curl "http://localhost:8123/?user=errlog_ch_user&password=errlog_ch_password&query=SELECT%201"`
- Проверить работу PostgreSQL:
  - `docker compose exec postgres psql -U errlog_pg_user -d errlog_pg -c "select 1;"`

---

## Этап 3 (недели 3–4)

### Реализовано

- Добавлен сервис `clickhouse-init`, который инициализирует базу `errlog_ch` и таблицу `errlog_ch.error_events` через `init.sql`.
- Добавлен Ingestor:
  - Читает `errors-raw` из Kafka.
  - Обрабатывает события батчами (batch listener).
  - Нормализует raw события в модель `ErrorEvent` через интерфейсы (стратегия по `sourceType`).
  - Пишет `ErrorEvent` в ClickHouse батчами через JDBC.
  - Делает manual ack только после успешной вставки (семантика at-least-once, дубликаты допустимы).
- Fingerprint пока в режиме fallback:
  - `fingerprint = 0`
  - `fingerprintSource = UNKNOWN`
  - (алгоритм fingerprint будет реализован на следующем этапе)

### Проверка

- Запустить стенд:
  - `docker compose up -d --build`
- Проверить Kafka (видим raw события):
  - `docker compose exec kafka /opt/kafka/bin/kafka-console-consumer.sh --bootstrap-server kafka:9092 --topic errors-raw --from-beginning`

- Проверить ClickHouse (таблица/данные):
  - `curl "http://localhost:8123/?user=errlog_ch_user&password=errlog_ch_password&query=SHOW%20TABLES%20FROM%20errlog_ch"` - получить список таблиц.
  - `curl "http://localhost:8123/?user=errlog_ch_user&password=errlog_ch_password&query=SELECT%20count()%20FROM%20errlog_ch.error_events"` получить количество записанных эвентов.
  - `curl "http://localhost:8123/?user=errlog_ch_user&password=errlog_ch_password&query=SELECT%20timestamp,service,level,fingerprint,fingerprint_source%20FROM%20errlog_ch.error_events%20ORDER%20BY%20timestamp%20DESC%20LIMIT%205"` - вывести последние 5 эвентов.
- Проверить Ingestor (логи):
  - `docker compose logs -f ingestor`
- Быстрые аналитические запросы (MVP):
  - `curl "http://localhost:8123/?user=errlog_ch_user&password=errlog_ch_password&query=SELECT%20service,level,count()%20c%20FROM%20errlog_ch.error_events%20GROUP%20BY%20service,level%20ORDER%20BY%20c%20DESC"` - получить эвенты, сгруппированные по сервису и уровню лога.
  - `curl "http://localhost:8123/?user=errlog_ch_user&password=errlog_ch_password&query=SELECT%20toStartOfMinute(timestamp)%20m,count()%20c%20FROM%20errlog_ch.error_events%20GROUP%20BY%20m%20ORDER%20BY%20m%20DESC%20LIMIT%2010"` - получить количество ошибок в бакетах по 20 минут.

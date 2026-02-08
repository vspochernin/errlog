# errlog

Выпускная квалификационная работа «Сервис сбора и анализа ошибок информационных систем». 2-й семестр 2-го курса МИФИ
ИИКС РПО (2025-2026 уч. г).

## Этап 1

### Реализовано

- Генератор ошибок запускается через Docker Compose и пишет INFO/WARN/ERROR события в ожидаемом JSON формате.

### Проверка

- Запустить стенд `docker compose up -d --build`.
- Проверить наличие генерируемых событий `docker logs errlog-jerrgen-1`.

## Этап 2

### Реализовано

- Стенд, состоящий из генератора ошибок, Vector, Kafka, ClickHouse, PostgreSQL запускается через Docker Compose.
- Генератор ошибок (jerrgen) пишет JSON события ошибок в stdout.
- Vector читает события из docker-логов генератора, оставляет только WARN и ERROR события и обогащает их метаданными контейнера.
- События попадают в Kafka топик `errors-raw` (который создается сервисом kafka-init).
- ClickHouse и PostgreSQL поднимаются и успешно работают (пока что "в холостую").

### Проверка

- Запустить стенд `docker compose up -d --build`.
- Проверить наличие событий ошибок в Kafka `docker compose exec --workdir /opt/kafka/bin kafka sh -lc './kafka-console-consumer.sh --bootstrap-server kafka:9092 --topic errors-raw --from-beginning'`
- Проверить работу ClickHouse `curl "http://localhost:8123/?user=errlog&password=errlog&query=SELECT%201"`.
- Проверить работу PostgreSQL `docker compose exec postgres psql -U errlog -d errlog -c "select 1;"`.

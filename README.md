# errlog

Выпускная квалификационная работа «Сервис сбора и анализа ошибок информационных систем». 2-й семестр 2-го курса МИФИ. ИИКС РПО (2025-2026 уч. г).

## Описание

`errlog` - сервис для сбора и анализа ошибок информационных систем.

Проект разделен на 2 docker контура:
- `errlog-core` - ядро системы: Kafka, Ingestor, ClickHouse, PostgreSQL, Errapi.
  - Данный контур представляет собой непосредственно сервис сбора и анализа ошибок информационных систем.
- `errlog-demo` - демонстрационный контур: генераторы ошибок и Vector.
  - Данный контур нужен для демонстрации работы сервиса и представляет собой имитацию реальных микросервисов, генерирующих ошибки в процессе своей работы.

В дальнейшем совместная работа обоих контуров будет называться стендом. А под demo контуром может пониматься любая система микросервисов, генерирующая ошибки.

## Состав репозитория

- `generators/jerrgen` - генератор WARN/ERROR логов, написанный на Java (Spring Boot Logback).
- `ingestor` - Java Spring Boot сервис, читает Kafka и пишет нормализованные события в ClickHouse.
- `errapi` - REST API с JWT и ролями для взаимодействия пользователей с сервисом.
- `docker-compose.core.yml` - core контур.
- `docker-compose.demo.yml` - demo контур.
- `docker/vector/vector.yaml` - конфиг Vector.
- `docker/clickhouse/init.sql` - SQL скрипт инициализации ClickHouse (создание базы и таблицы).
- `docker/kafka/init.sh` - скрипт инициализации Kafka (создание топика `errors-raw`).

## Принцип работы стенда

1. Генераторы ошибок пишут логи в `stdout` в формате JSON lines.
2. Vector в demo контуре читает docker-логи контейнеров с лейблом `errlog.collect=true`.
3. Vector оставляет только WARN/ERROR/FATAL события, добавляет метаданные и отправляет события в Kafka core контура.
4. Ingestor читает Kafka, нормализует событие, вычисляет fingerprint и осуществляет запись в ClickHouse.
5. Errapi осуществляет аутентификацию пользователя, читает данные из ClickHouse и предоставляет поиск и аналитику по ошибкам.

`Ingestor` подтверждает Kafka offset только после успешной записи в ClickHouse, что обеспечивает семантику доставки сообщений at-least-once.

## Как подключить новый источник ошибок

В MVP "из коробки" поддерживается формат `java-spring-logback` - JSON строки Java Spring Logback логов с выводом шаблона сообщения (demo контур).
Чтобы подключить любой другой источник (Python, Go, и т.п.), необходимо сделать две вещи:

### 1) Доставить события в Kafka core контура

- Core контур принимает входные события из Kafka топика `errors-raw`.
- Для внешних источников необходимо использовать внешний Kafka listener core контура: `${ERRLOG_KAFKA_EXTERNAL_HOST}:9094` (см. `.env`).
- Сообщение в Kafka должно быть в формате JSON (одна запись на одно сообщение) и содержать поле `sourceType` на верхнем уровне.
- Минимальный пример JSON (поля можно расширять, но `sourceType` обязателен):

```json
{
  "sourceType": "my-app-source-type",
  "timestamp": 1770000000000,
  "service": "billing",
  "level": "ERROR",
  "message": "Something failed"
}
```

Способ доставки сообщений в core контур допускается делать произвольным, однако предлагается использовать Vector как удобный инструмент.
Пример настройки в Vector можно найти в demo контуре.

Важно: если `sourceType` неизвестен Ingestor, событие будет пропущено.

### 2) Научить Ingestor обрабатывать новый `sourceType`

Ingestor выбирает нормализатор по полю `sourceType` и преобразует raw JSON в каноническую модель `NormalizedErrorEvent`.

Чтобы добавить новый формат, необходимо:
1. Создать класс, реализующий `RawEventNormalizer`.
2. Вернуть нужный `sourceType()` (строго то же значение, что в JSON-сообщениях).
3. В `normalize(JsonNode rawEvent)` распарсить поля и вернуть `NormalizedErrorEvent`.
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

После этого любые сообщения с `sourceType = "my-app-source-type"` начнут приниматься и обрабатываться.

## Вычисление fingerprint

Для группировки ошибок у каждого обрабатываемого события в Ingestor вычисляется fingerprint.

Основа всегда начинается с:
`service|logger|level`

Далее:
- Если у события есть `stacktrace`: `service|logger|level|exceptionClass|stacktraceWithoutDigits`, `fingerprintSource=STACKTRACE`.
- Иначе если есть `messageTemplate`: `service|logger|level|messageTemplate`, `fingerprintSource=TEMPLATE`.
- Иначе: `service|logger|level`, `fingerprintSource=MINIMAL`.

Хэш вычисляется в ClickHouse как `xxh3(fingerprintBase)` и хранится как `UInt64`.

Так как `logger` является опциональным полем, то в случае его отсутствия при вычислении fingerprint будет использоваться пустая строка.

## Конфигурация

В корне лежит `.env` - файл переменных окружения для Docker.

Основной параметр сейчас один:
```dotenv
ERRLOG_KAFKA_EXTERNAL_HOST=host.docker.internal
```

Если оба контура подняты на одной машине, этого достаточно для корректной работы.

Если core и demo находятся на разных машинах, значением данного параметра следует поставить IP или DNS машины, где поднят `errlog-core`. Например:
```dotenv
ERRLOG_KAFKA_EXTERNAL_HOST=192.168.1.50
```

## Запуск

### 1. Поднять core контур

```bash
docker compose -f docker-compose.core.yml up -d --build
```

### 2. Поднять demo контур

```bash
docker compose -f docker-compose.demo.yml up -d --build
```

## Вспомогательные скрипты

В корне репозитория также представлены вспомогательные скрипты.

- Перезапустить core контур:
```bash
./restart-core.sh
```

- Перезапустить demo контур:
```bash
./restart-demo.sh
```

- Перезапустить весь стенд:
```bash
./restart-stand.sh
```

- Остановить весь стенд:
```bash
./stop-stand.sh
```

Остановить весь стенд с удалением Docker volumes проекта:
```bash
./stop-stand-remove-volumes.sh
```

## Проверки после запуска

### Core контур

Статус контейнеров:
```bash
docker compose -f docker-compose.core.yml ps -a
```

Ожидается, что:
- Сервисы `kafka-init` и `clickhouse-init` будут завершены с `Exited (0)`.
- Остальны сервисы будет находиться в состоянии `Up`.

Проверка ClickHouse:
```bash
curl "http://localhost:8123/?user=errlog_ch_user&password=errlog_ch_password&query=SELECT%201"
```

Здесь и далее `%20` - экранирование пробела в `HTTP` запросе.

Ожидается вывод `1`.

Количество событий:
```bash
curl "http://localhost:8123/?user=errlog_ch_user&password=errlog_ch_password&query=SELECT%20count()%20FROM%20errlog_ch.error_events"
```

Ожидается вывод количества записанных событий (ненулевой, если уже поднят `demo` контур).

Логи ingestor:
```bash
docker logs -f errlog-core-ingestor-1
```

Ожидается поток логов обработки событий (если уже поднят `demo` контур).

### Demo контур

Статус контейнеров:
```bash
docker compose -f docker-compose.demo.yml ps -a
```
Ожидается, что все сервисы будет находиться в состоянии `Up`.

Логи Vector:
```bash
docker logs -f errlog-demo-vector-1
```

Ожидается успешное подключение к Kafka и начало обработки логов.

## Errapi

Swagger: http://localhost:8080/swagger-ui/index.html.

Пользователь с ролью OWNER создается при старте `errapi` из переменных окружения `ERRLOG_OWNER_*` (см. `docker-compose.core.yml`), если такого пользователя еще нет в PostgreSQL.

### Получить JWT

```bash
curl -X POST "http://localhost:8080/api/auth/login"   -H "Content-Type: application/json"   -d '{"login":"owner","password":"owner_password"}'
```

Для удобства можно добавить полученный токен в переменную окружения:
```bash
export ERRLOG_OWNER_JWT="<token>"
```

Проверка корректности токена:
```bash
curl -sS   -H "Authorization: Bearer $ERRLOG_OWNER_JWT"   "http://localhost:8080/api/users"
```

Ожидается вывод информации о зарегистрированных пользователях.

## Errors API

### Allowlist фильтров

```bash
curl -sS   -H "Authorization: Bearer $ERRLOG_OWNER_JWT"   "http://localhost:8080/api/errors/filters" | jq
```

### Список событий

По умолчанию возвращаются события за последние 24 часа.
```bash
curl -sS   -H "Authorization: Bearer $ERRLOG_OWNER_JWT"   -H "Content-Type: application/json"   -X POST "http://localhost:8080/api/errors/events?limit=10&offset=0"   -d '{}' | jq
```

Пример с границами времени:
```bash
curl -sS   -H "Authorization: Bearer $ERRLOG_OWNER_JWT"   -H "Content-Type: application/json"   -X POST "http://localhost:8080/api/errors/events?limit=20&offset=0"   -d '{"from":"2026-02-24T00:00:00Z","to":"2027-02-25T00:00:00Z"}' | jq
```

Пример с фильтрами:
```bash
curl -sS   -H "Authorization: Bearer $ERRLOG_OWNER_JWT"   -H "Content-Type: application/json"   -X POST "http://localhost:8080/api/errors/events?limit=20&offset=0"   -d '{"filters":[{"field":"service","operation":"eq","values":["jerrgen-alpha"]},{"field":"level","operation":"in","values":["ERROR"]}]}' | jq
```

### Детальная информация по событию

```bash
curl -sS   -H "Authorization: Bearer $ERRLOG_OWNER_JWT"   "http://localhost:8080/api/errors/events/<eventId>" | jq
```

### Группы ошибок

```bash
curl -sS   -H "Authorization: Bearer $ERRLOG_OWNER_JWT"   -H "Content-Type: application/json"   -X POST "http://localhost:8080/api/errors/groups?limit=10&offset=0"   -d '{}' | jq
```

### Timeseries

Автоматический выбор размера бакета:
```bash
curl -sS   -H "Authorization: Bearer $ERRLOG_OWNER_JWT"   -H "Content-Type: application/json"   -X POST "http://localhost:8080/api/errors/timeseries"   -d '{}' | jq
```

Ручной выбор размера бакета:
```bash
curl -sS   -H "Authorization: Bearer $ERRLOG_OWNER_JWT"   -H "Content-Type: application/json"   -X POST "http://localhost:8080/api/errors/timeseries?bucket=1m"   -d '{}' | jq
```

## Пагинация событий

- Список событий сортируется по `timestamp DESC, eventId DESC`.
- Список групп событий сортируется по `groupCount DESC, groupLastSeen DESC, groupFingerprint DESC`.

Если поток живой и в систему продолжают приходить новые события, для стабильной offset-пагинации следует зафиксировать `to` на момент первого запроса и переиспользовать его на следующих страницах, меняя только `offset`.

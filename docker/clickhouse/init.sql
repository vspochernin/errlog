-- Создание базы.
CREATE DATABASE IF NOT EXISTS errlog_ch;

-- Создание таблицы.
CREATE TABLE IF NOT EXISTS errlog_ch.error_events
(
    -- Обязательные поля.
    event_id           UUID,
    timestamp          DateTime64(3, 'UTC'),
    source_type        LowCardinality(String),
    service            LowCardinality(String),
    level              LowCardinality(String),
    message_formatted  String,
    fingerprint        UInt64,
    fingerprint_source LowCardinality(String),

    -- Необязательные поля.
    instance           Nullable(String),
    service_version    Nullable(String),
    logger             Nullable(String),
    thread             Nullable(String),
    message_template   Nullable(String),
    exception_class    Nullable(String),
    exception_message  Nullable(String),
    stacktrace         Nullable(String)
)
    ENGINE = MergeTree
        PARTITION BY toYYYYMM(timestamp)
        ORDER BY (timestamp, service, fingerprint, event_id);
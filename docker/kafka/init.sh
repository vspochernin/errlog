#!/bin/sh

set -eu # Сразу падаем если любая команда завершилась ошибкой или была использована неинициализированная переменная.

/opt/kafka/bin/kafka-topics.sh \
      --bootstrap-server kafka:9092 \
      --create \
      --if-not-exists \
      --topic errors-raw \
      --partitions 1 \
      --replication-factor 1 \
      --config retention.ms=259200000 # Трое суток.

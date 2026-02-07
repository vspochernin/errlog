# vslog

Выпускная квалификационная работа «Сервис сбора и анализа ошибок информационных систем». 2-й семестр 2-го курса МИФИ
ИИКС РПО (2025-2026 уч. г).

## Проверка работы Kafka

1. Создать топик: `docker compose exec kafka /opt/kafka/bin/kafka-topics.sh --bootstrap-server localhost:9092 --create --if-not-exists --topic errors-raw --partitions 1 --replication-factor 1`..
2. Отправить в топик сообщение: `echo 123test-message123 | docker compose exec -T kafka /opt/kafka/bin/kafka-console-producer.sh --bootstrap-server localhost:9092 --topic errors-raw`.
3. Прочитать из топика одно сообщение `docker compose exec -T kafka /opt/kafka/bin/kafka-console-consumer.sh --bootstrap-server localhost:9092 --topic errors-raw --from-beginning --max-messages 1`.



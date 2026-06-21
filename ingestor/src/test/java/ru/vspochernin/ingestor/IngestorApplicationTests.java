package ru.vspochernin.ingestor;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * Полный контекст Spring Boot (@SpringBootTest) требует живую инфраструктуру:
 * Kafka (брокер, к которому подключается @KafkaListener при старте) и ClickHouse (DataSource).
 * Эти зависимости описаны в application.yaml через env-переменные без дефолтов, поэтому
 * контекст не поднимается в обычном `./mvnw test`.
 * <p>
 * Тест корректно проходит, когда поднят core-контур стенда и переданы env-переменные,
 * указывающие на localhost-порты контейнеров:
 * <p>
 * docker compose -f docker/docker-compose.core.yml up -d --build
 * <p>
 * KAFKA_BOOTSTRAP_SERVERS=localhost:9094 \
 * KAFKA_GROUP_ID=test-group \
 * KAFKA_TOPIC=errors-raw \
 * CLICKHOUSE_JDBC_URL=jdbc:clickhouse://localhost:8123/errlog_ch \
 * CLICKHOUSE_USER=errlog_ch_user \
 * CLICKHOUSE_PASSWORD=errlog_ch_password \
 * ./mvnw -Dtest=IngestorApplicationTests test
 * <p>
 * Бизнес-логика Ingestor покрыта отдельными юнит- и интеграционными тестами (см. TESTING.md),
 * поэтому этот smoke-тест отключён и не блокирует обычный прогон `./mvnw test`.
 */
@SpringBootTest
@Disabled("Требует поднятый core-контур стенда и env-переменные. См. javadoc выше.")
class IngestorApplicationTests {

    @Test
    void contextLoads() {
    }

}

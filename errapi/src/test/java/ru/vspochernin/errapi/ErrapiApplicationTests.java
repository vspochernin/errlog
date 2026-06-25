package ru.vspochernin.errapi;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * Полный контекст Spring Boot (@SpringBootTest) требует живую инфраструктуру:
 * PostgreSQL (JPA + Flyway, валидирует схему при старте) и ClickHouse (DataSource).
 * Эти зависимости и JWT-секрет/OWNER-учётка описаны в application.yaml и DataSourcesConfig
 * через env-переменные без дефолтов, поэтому контекст не поднимается в обычном `./mvnw test`.
 * <p>
 * Тест корректно проходит, когда поднят core-контур стенда и переданы env-переменные,
 * указывающие на localhost-порты контейнеров:
 * <p>
 * docker compose -f docker/docker-compose.core.yml up -d --build
 * <p>
 * POSTGRES_JDBC_URL=jdbc:postgresql://localhost:5432/errlog_pg \
 * POSTGRES_USER=errlog_pg_user \
 * POSTGRES_PASSWORD=errlog_pg_password \
 * CLICKHOUSE_JDBC_URL=jdbc:clickhouse://localhost:8123/errlog_ch \
 * CLICKHOUSE_USER=errlog_ch_user \
 * CLICKHOUSE_PASSWORD=errlog_ch_password \
 * JWT_SECRET=jwt-secret-that-should-be-more-than-32-symbols \
 * JWT_EXPIRES_SECONDS=3600 \
 * ERRLOG_OWNER_LOGIN=owner \
 * ERRLOG_OWNER_EMAIL=owner@example.com \
 * ERRLOG_OWNER_PASSWORD=owner_password \
 * ./mvnw -Dtest=ErrapiApplicationTests test
 * <p>
 * Бизнес-логика Errapi покрыта отдельными юнит- и интеграционными тестами (см. TESTING.md),
 * поэтому этот smoke-тест отключён и не блокирует обычный прогон `./mvnw test`.
 */
@SpringBootTest
@Disabled("Требует поднятый core-контур стенда и env-переменные. См. javadoc выше.")
class ErrapiApplicationTests {

    @Test
    void contextLoads() {
    }

}

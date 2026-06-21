package ru.vspochernin.errapi.integration;

import javax.sql.DataSource;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Testcontainers
class UserRepositoryIT {

    @Container
    static org.testcontainers.containers.PostgreSQLContainer<?> postgres =
            new org.testcontainers.containers.PostgreSQLContainer<>(
                    DockerImageName.parse("postgres:18.1"))
                    .withDatabaseName("errlog_pg")
                    .withUsername("errlog_pg_user")
                    .withPassword("errlog_pg_password")
                    .withReuse(true);

    private JdbcTemplate jdbc;

    @BeforeEach
    void setUp() {
        String jdbcUrl = postgres.getJdbcUrl();
        String user = postgres.getUsername();
        String password = postgres.getPassword();

        // Flyway миграция
        Flyway flyway = Flyway.configure()
                .dataSource(jdbcUrl, user, password)
                .locations("classpath:db/migration")
                .load();
        flyway.migrate();

        // Чистим таблицу
        DataSource ds = new DriverManagerDataSource(jdbcUrl, user, password);
        jdbc = new JdbcTemplate(ds);
        jdbc.execute("DELETE FROM users");
    }

    @Test
    void shouldCreateUserAndRetrieveByLogin() {
        jdbc.update(
                "INSERT INTO users (login, email, password_hash, role) VALUES (?, ?, ?, ?)",
                "test-user", "test@example.com", "hash", "READER");

        var rows = jdbc.queryForList("SELECT * FROM users WHERE login = ?", "test-user");
        assertThat(rows).hasSize(1);
        assertThat(rows.getFirst().get("role").toString()).isEqualTo("READER");
    }

    @Test
    void shouldEnforceUniqueLogin() {
        jdbc.update(
                "INSERT INTO users (login, email, password_hash, role) VALUES (?, ?, ?, ?)",
                "dup", "one@example.com", "hash", "NONE");

        // Повторный логин нарушает UNIQUE-ограничение.
        // Spring мапит это в DuplicateKeyException (SQLSTATE 23505) — специфичный подтип,
        // поэтому тест доказывает, что упало именно по дубликату ключа, а не по иной причине.
        assertThatThrownBy(() -> jdbc.update(
                "INSERT INTO users (login, email, password_hash, role) VALUES (?, ?, ?, ?)",
                "dup", "two@example.com", "hash", "NONE"))
                .isInstanceOf(DuplicateKeyException.class);
    }

    @Test
    void shouldEnforceRoleCheckConstraint() {
        // Несуществующая роль нарушает CHECK-ограничение. Spring не выделяет для CHECK
        // отдельный подтип, поэтому проверяем SQLSTATE 23514 (check_violation) в корневой
        // причине — это доказывает, что упало именно по CHECK, а не по другому constraint.
        assertThatThrownBy(() -> jdbc.update(
                "INSERT INTO users (login, email, password_hash, role) VALUES (?, ?, ?, ?)",
                "bad-role", "bad@example.com", "hash", "SUPERHERO"))
                .isInstanceOf(DataIntegrityViolationException.class)
                .rootCause()
                .isInstanceOf(java.sql.SQLException.class)
                .hasFieldOrPropertyWithValue("SQLState", "23514");
    }

    @Test
    void shouldCheckExistenceByRole() {
        jdbc.update(
                "INSERT INTO users (login, email, password_hash, role) VALUES (?, ?, ?, ?)",
                "owner1", "owner1@example.com", "hash", "OWNER");

        var count = jdbc.queryForObject(
                "SELECT count(*) FROM users WHERE role = 'OWNER'", Long.class);
        assertThat(count).isPositive();
    }
}

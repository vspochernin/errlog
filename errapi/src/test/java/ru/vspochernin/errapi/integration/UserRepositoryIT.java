package ru.vspochernin.errapi.integration;

import javax.sql.DataSource;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import ru.vspochernin.errapi.model.auth.UserRole;

import static org.assertj.core.api.Assertions.assertThat;

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

        try {
            jdbc.update(
                    "INSERT INTO users (login, email, password_hash, role) VALUES (?, ?, ?, ?)",
                    "dup", "two@example.com", "hash", "NONE");
            // Если не упало — constraint не сработал
            var rows = jdbc.queryForList("SELECT count(*) FROM users WHERE login = 'dup'", Long.class);
            assertThat(rows.getFirst()).isLessThanOrEqualTo(1);
        } catch (Exception e) {
            assertThat(e.getMessage()).contains("duplicate");
        }
    }

    @Test
    void shouldEnforceRoleCheckConstraint() {
        try {
            jdbc.update(
                    "INSERT INTO users (login, email, password_hash, role) VALUES (?, ?, ?, ?)",
                    "bad-role", "bad@example.com", "hash", "SUPERHERO");
            var rows = jdbc.queryForList("SELECT count(*) FROM users WHERE login = 'bad-role'", Long.class);
            assertThat(rows.getFirst()).isEqualTo(0);
        } catch (Exception e) {
            assertThat(e.getMessage()).contains("violates");
        }
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

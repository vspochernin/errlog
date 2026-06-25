package ru.vspochernin.ingestor.integration;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.testcontainers.clickhouse.ClickHouseContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import ru.vspochernin.ingestor.fingerprint.FingerprintResult;
import ru.vspochernin.ingestor.fingerprint.FingerprintSource;
import ru.vspochernin.ingestor.model.ErrorEvent;
import ru.vspochernin.ingestor.model.NormalizedErrorEvent;
import ru.vspochernin.ingestor.writer.ClickHouseEventWriter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

@Testcontainers
class ClickHouseEventWriterIT {

    private static final String DB = "errlog_ch";
    private static final String USER = "errlog_ch_user";
    private static final String PASSWORD = "errlog_ch_password";

    @Container
    static ClickHouseContainer clickhouse = new ClickHouseContainer(
            DockerImageName.parse("clickhouse/clickhouse-server:26.1"))
            .withUsername(USER)
            .withPassword(PASSWORD)
            .withReuse(true);

    private ClickHouseEventWriter writer;
    private JdbcTemplate jdbc;

    @BeforeAll
    static void setUpTables() throws Exception {
        String url = "jdbc:clickhouse://" + clickhouse.getHost() + ":" + clickhouse.getMappedPort(8123);
        try (Connection conn = DriverManager.getConnection(url, USER, PASSWORD)) {
            conn.createStatement().execute("CREATE DATABASE IF NOT EXISTS " + DB);
            conn.createStatement().execute("""
                    CREATE TABLE IF NOT EXISTS errlog_ch.error_events
                    (
                        event_id           UUID,
                        timestamp          DateTime64(3, 'UTC'),
                        source_type        LowCardinality(String),
                        service            LowCardinality(String),
                        level              LowCardinality(String),
                        message_formatted  String,
                        fingerprint        UInt64,
                        fingerprint_source LowCardinality(String),
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
                    ORDER BY (timestamp, service, fingerprint, event_id)
                    """);
        }
    }

    @BeforeEach
    void setUp() {
        jdbc = createJdbcTemplate();
        writer = new ClickHouseEventWriter(jdbc);
    }

    private JdbcTemplate createJdbcTemplate() {
        try {
            String jdbcUrl = "jdbc:clickhouse://" + clickhouse.getHost() + ":"
                    + clickhouse.getMappedPort(8123) + "/" + DB
                    + "?user=" + USER + "&password=" + PASSWORD;
            return new JdbcTemplate(new com.clickhouse.jdbc.ClickHouseDataSource(jdbcUrl));
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void shouldNotThrowOnNullList() {
        assertThatCode(() -> writer.write(null)).doesNotThrowAnyException();
    }

    @Test
    void shouldNotThrowOnEmptyList() {
        assertThatCode(() -> writer.write(List.of())).doesNotThrowAnyException();
    }

    @Test
    void shouldWriteSingleEvent() {
        var event = createTestEvent();
        writer.write(List.of(event));

        var rows = jdbc.queryForList(
                "SELECT event_id, service, level, fingerprint_source, toString(fingerprint) AS fp FROM errlog_ch.error_events");
        assertThat(rows).hasSize(1);
        var row = rows.getFirst();
        assertThat(row.get("service")).isEqualTo("test-svc");
        assertThat(row.get("level")).isEqualTo("ERROR");
        assertThat(row.get("fingerprint_source")).isEqualTo("STACKTRACE");
        assertThat(row.get("fp")).isNotNull();
    }

    @Test
    void fingerprintShouldBeRepeatedForSameBase() {
        var event1 = createTestEvent();
        var event2 = createTestEvent();

        writer.write(List.of(event1, event2));

        var fingerprints = jdbc.queryForList(
                "SELECT DISTINCT toString(fingerprint) AS fp FROM errlog_ch.error_events");
        assertThat(fingerprints).hasSize(1);
    }

    @Test
    void differentBasesShouldProduceDifferentFingerprints() {
        var event1 = createTestEvent();
        var event2 = new ErrorEvent(
                UUID.randomUUID(),
                new NormalizedErrorEvent(
                        Instant.EPOCH, "t", "other-svc", "WARN", "msg",
                        null, null, null, null, null, null, null, null),
                new FingerprintResult("different-base", FingerprintSource.MINIMAL));

        writer.write(List.of(event1, event2));

        var fingerprints = jdbc.queryForList(
                "SELECT DISTINCT toString(fingerprint) AS fp FROM errlog_ch.error_events");
        assertThat(fingerprints).hasSize(2);
    }

    private static ErrorEvent createTestEvent() {
        return new ErrorEvent(
                UUID.randomUUID(),
                new NormalizedErrorEvent(
                        Instant.EPOCH,
                        "java-spring-logback",
                        "test-svc",
                        "ERROR",
                        "formatted msg",
                        "instance-1",
                        "1.0.0",
                        "com.Logger",
                        "main",
                        "User {} not found",
                        "java.lang.RuntimeException",
                        "boom",
                        "at com.Foo.doIt(Foo.java:42)"),
                new FingerprintResult("fingerprint-base", FingerprintSource.STACKTRACE));
    }
}

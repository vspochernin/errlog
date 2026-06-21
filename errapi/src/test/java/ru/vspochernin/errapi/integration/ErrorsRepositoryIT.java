package ru.vspochernin.errapi.integration;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.testcontainers.clickhouse.ClickHouseContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import ru.vspochernin.errapi.dto.errors.ErrorsRequest;
import ru.vspochernin.errapi.mapper.ErrorEventRowMapper;
import ru.vspochernin.errapi.repository.ErrorsRepository;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
class ErrorsRepositoryIT {

    private static final String CH_USER = "errlog_ch_user";
    private static final String CH_PASSWORD = "errlog_ch_password";
    private static final String CH_DB = "errlog_ch";

    @Container
    static ClickHouseContainer clickhouse = new ClickHouseContainer(
            DockerImageName.parse("clickhouse/clickhouse-server:26.1"))
            .withUsername(CH_USER)
            .withPassword(CH_PASSWORD)
            .withReuse(true);

    private ErrorsRepository repository;

    @BeforeAll
    static void setUpTables() throws Exception {
        String url = "jdbc:clickhouse://" + clickhouse.getHost() + ":" + clickhouse.getMappedPort(8123);
        try (Connection conn = DriverManager.getConnection(url, CH_USER, CH_PASSWORD)) {
            conn.createStatement().execute("CREATE DATABASE IF NOT EXISTS " + CH_DB);
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
        NamedParameterJdbcTemplate jdbc = createJdbc();
        // Чистим таблицу перед каждым тестом
        jdbc.getJdbcTemplate().execute("TRUNCATE TABLE IF EXISTS errlog_ch.error_events");
        repository = new ErrorsRepository(jdbc, new ErrorEventRowMapper());
    }

    private NamedParameterJdbcTemplate createJdbc() {
        try {
            String jdbcUrl = "jdbc:clickhouse://" + clickhouse.getHost() + ":"
                    + clickhouse.getMappedPort(8123) + "/" + CH_DB
                    + "?user=" + CH_USER + "&password=" + CH_PASSWORD;
            return new NamedParameterJdbcTemplate(
                    new com.clickhouse.jdbc.ClickHouseDataSource(jdbcUrl));
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void shouldCountEvents() {
        seedEvents(3);

        var query = ru.vspochernin.errapi.model.errors.ErrorsQuery.parseFromErrorsRequest(
                ErrorsRequest.empty());
        assertThat(repository.countEvents(query)).isEqualTo(3);
    }

    @Test
    void shouldFindEventsWithPagination() {
        seedEvents(5);

        var query = ru.vspochernin.errapi.model.errors.ErrorsQuery.parseFromErrorsRequest(
                ErrorsRequest.empty());
        var events = repository.findEvents(query, 3, 0);
        assertThat(events).hasSize(3);

        var page2 = repository.findEvents(query, 3, 2);
        assertThat(page2).hasSize(3);
        // Вторая страница должна отличаться от первой
        assertThat(page2.getFirst().eventId()).isNotEqualTo(events.getFirst().eventId());
    }

    @Test
    void shouldFindEventById() {
        var id = UUID.randomUUID().toString();
        insertEvent(id, "find-by-id", "ERROR");

        var found = repository.findEventById(id);
        assertThat(found).isPresent();
        assertThat(found.get().service()).isEqualTo("find-by-id");
        assertThat(found.get().stacktrace()).isNotNull(); // полный stacktrace в детальном запросе
    }

    @Test
    void shouldReturnEmptyForMissingEventId() {
        assertThat(repository.findEventById("00000000-0000-0000-0000-000000000000")).isEmpty();
    }

    @Test
    void shouldCountGroups() {
        // Два события с разными базами -> две группы
        insertWithFingerprint("base-a", Instant.now(), "svc-a");
        insertWithFingerprint("base-b", Instant.now(), "svc-a");

        var query = ru.vspochernin.errapi.model.errors.ErrorsQuery.parseFromErrorsRequest(
                ErrorsRequest.empty());
        var totals = repository.countEventsAndGroupsTotals(query);
        assertThat(totals.eventsTotal()).isEqualTo(2);
        assertThat(totals.groupsTotal()).isEqualTo(2);
    }

    @Test
    void shouldFindGroups() {
        insertWithFingerprint("a", Instant.now().minusSeconds(3600), "svc");
        insertWithFingerprint("a", Instant.now(), "svc");
        insertWithFingerprint("b", Instant.now(), "svc");

        var query = ru.vspochernin.errapi.model.errors.ErrorsQuery.parseFromErrorsRequest(
                ErrorsRequest.empty());
        var groups = repository.findGroups(query, 10, 0);
        assertThat(groups).isNotEmpty();
    }

    @Test
    void shouldFindTimeseries() {
        seedEvents(10);

        var query = ru.vspochernin.errapi.model.errors.ErrorsQuery.parseFromErrorsRequest(
                ErrorsRequest.empty());
        var ts = repository.findTimeseries(query, ru.vspochernin.errapi.model.errors.TimeBucket.M1);
        assertThat(ts).isNotEmpty();
    }

    private void seedEvents(int count) {
        for (int i = 0; i < count; i++) {
            insertEvent(UUID.randomUUID().toString(), "seed-svc", "ERROR");
        }
    }

    private void insertEvent(String eventId, String service, String level) {
        try {
            String url = "jdbc:clickhouse://" + clickhouse.getHost() + ":"
                    + clickhouse.getMappedPort(8123) + "/" + CH_DB
                    + "?user=" + CH_USER + "&password=" + CH_PASSWORD;
            var jdbc = new JdbcTemplate(new com.clickhouse.jdbc.ClickHouseDataSource(url));
            jdbc.update("""
                    INSERT INTO errlog_ch.error_events
                    (event_id, timestamp, source_type, service, level, message_formatted,
                     fingerprint, fingerprint_source, stacktrace)
                    VALUES (toUUID(?), ?, 'java-spring-logback', ?, ?, ?,
                     xxh3(?), 'STACKTRACE', 'at com.Foo.doIt(Foo.java:42)')
                    """, eventId, Timestamp.from(Instant.now()), service, level, "msg " + eventId, eventId);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private void insertWithFingerprint(String base, Instant ts, String service) {
        try {
            String url = "jdbc:clickhouse://" + clickhouse.getHost() + ":"
                    + clickhouse.getMappedPort(8123) + "/" + CH_DB
                    + "?user=" + CH_USER + "&password=" + CH_PASSWORD;
            var jdbc = new JdbcTemplate(new com.clickhouse.jdbc.ClickHouseDataSource(url));
            jdbc.update("""
                    INSERT INTO errlog_ch.error_events
                    (event_id, timestamp, source_type, service, level, message_formatted,
                     fingerprint, fingerprint_source, stacktrace)
                    VALUES (toUUID(?), ?, 'java-spring-logback', ?, 'WARN', ?,
                     xxh3(?), 'MESSAGE_TEMPLATE', null)
                    """, UUID.randomUUID().toString(), Timestamp.from(ts), service, "msg:" + base, base);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}

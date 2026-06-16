package ru.vspochernin.ingestor.integration;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.List;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.kafka.support.Acknowledgment;
import org.testcontainers.clickhouse.ClickHouseContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import ru.vspochernin.ingestor.fingerprint.DefaultFingerprintBuilder;
import ru.vspochernin.ingestor.kafka.RawEventJsonKafkaListener;
import ru.vspochernin.ingestor.normalization.JavaSpringLogbackRawEventNormalizer;
import ru.vspochernin.ingestor.normalization.RawEventNormalizerRegistry;
import ru.vspochernin.ingestor.normalization.UnknownRawEventNormalizer;
import ru.vspochernin.ingestor.processing.DefaultRawEventJsonProcessor;
import ru.vspochernin.ingestor.writer.ClickHouseEventWriter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@Testcontainers
class RawEventJsonKafkaListenerIT {

    private static final String CH_USER = "errlog_ch_user";
    private static final String CH_PASSWORD = "errlog_ch_password";
    private static final String CH_DB = "errlog_ch";

    @Container
    static ClickHouseContainer clickhouse = new ClickHouseContainer(
            DockerImageName.parse("clickhouse/clickhouse-server:26.1"))
            .withUsername(CH_USER)
            .withPassword(CH_PASSWORD)
            .withReuse(true);

    private JdbcTemplate clickhouseJdbc;
    private RawEventJsonKafkaListener listener;

    private static final String VALID_JSON = """
            {"timestamp":1771000000000,"sourceType":"java-spring-logback","service":"kafka-test","level":"ERROR","loggerName":"L","message":"kafka message","formattedMessage":"kafka message"}
            """;

    private static final String UNKNOWN_JSON = """
            {"sourceType":"totally-unknown","timestamp":1771000000000,"service":"ghost","level":"ERROR","message":"should be skipped"}
            """;

    @BeforeAll
    static void setUpTables() throws Exception {
        String chUrl = "jdbc:clickhouse://" + clickhouse.getHost()
                + ":" + clickhouse.getMappedPort(8123);
        try (Connection conn = DriverManager.getConnection(chUrl, CH_USER, CH_PASSWORD)) {
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
        clickhouseJdbc = createJdbcTemplate();
        var writer = new ClickHouseEventWriter(clickhouseJdbc);
        var objectMapper = new ObjectMapper();
        var processor = new DefaultRawEventJsonProcessor(
                objectMapper,
                new RawEventNormalizerRegistry(List.of(
                        new JavaSpringLogbackRawEventNormalizer(objectMapper),
                        new UnknownRawEventNormalizer()
                )),
                new DefaultFingerprintBuilder());
        listener = new RawEventJsonKafkaListener(processor, writer);
    }

    private JdbcTemplate createJdbcTemplate() {
        try {
            String chJdbcUrl = "jdbc:clickhouse://" + clickhouse.getHost()
                    + ":" + clickhouse.getMappedPort(8123) + "/" + CH_DB
                    + "?user=" + CH_USER + "&password=" + CH_PASSWORD;
            return new JdbcTemplate(new com.clickhouse.jdbc.ClickHouseDataSource(chJdbcUrl));
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void shouldProcessWriteAndAck() {
        Acknowledgment ack = mock(Acknowledgment.class);
        listener.listen(List.of(VALID_JSON), ack);

        verify(ack).acknowledge();

        var rows = clickhouseJdbc.queryForList(
                "SELECT service, level, fingerprint_source FROM errlog_ch.error_events WHERE service = 'kafka-test'");
        assertThat(rows).hasSize(1);
        assertThat(rows.getFirst().get("level")).isEqualTo("ERROR");
    }

    @Test
    void unknownSourceTypeShouldBeSkipped() {
        Acknowledgment ack = mock(Acknowledgment.class);
        listener.listen(List.of(UNKNOWN_JSON), ack);

        verify(ack).acknowledge();

        var count = clickhouseJdbc.queryForObject(
                "SELECT count() FROM errlog_ch.error_events WHERE service = 'ghost'", Long.class);
        assertThat(count).isZero();
    }

    @Test
    void shouldNotAckWhenWriterFails() {
        // Закрываем ClickHouse контейнер чтобы writer упал
        clickhouse.stop();

        Acknowledgment ack = mock(Acknowledgment.class);
        try {
            listener.listen(List.of(VALID_JSON), ack);
        } catch (Exception ignored) {
        }

        verify(ack, never()).acknowledge();
    }
}

package ru.vspochernin.ingestor.writer;

import java.sql.Timestamp;
import java.util.List;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import ru.vspochernin.ingestor.model.ErrorEvent;

@Component
@RequiredArgsConstructor
public class ClickHouseEventWriter implements ErrorEventWriter {

    // Максимальный размер батча для вставки в ClickHouse.
    // Если количество значений на вставку будет больше, Spring разобъет их на соответствующие батчи.
    private static final int INSERT_BATCH_SIZE = 200;

    private final JdbcTemplate jdbcTemplate;

    private static final String INSERT_SQL = """
            INSERT INTO errlog_ch.error_events
            (
                event_id,
                timestamp,
                service,
                level,
                message_formatted,
                fingerprint,
                fingerprint_source,
                source_type,
                instance,
                service_version,
                logger,
                thread,
                message_template,
                exception_class,
                exception_message,
                stacktrace
            )
            VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)
            """;

    @Override
    public void writeBatch(List<ErrorEvent> events) {
        if (events == null || events.isEmpty()) {
            return;
        }

        jdbcTemplate.batchUpdate(
                INSERT_SQL,
                events,
                INSERT_BATCH_SIZE,
                (ps, e) -> {
                    ps.setObject(1, e.eventId());
                    ps.setTimestamp(2, Timestamp.from(e.timestamp()));

                    ps.setString(3, e.service());
                    ps.setString(4, e.level());
                    ps.setString(5, e.messageFormatted());
                    ps.setString(6, Long.toUnsignedString(e.fingerprint())); // UInt64 может не влезть в signed long.
                    ps.setString(7, e.fingerprintSource());
                    ps.setString(8, e.sourceType());

                    ps.setString(9, e.instance());
                    ps.setString(10, e.serviceVersion());
                    ps.setString(11, e.logger());
                    ps.setString(12, e.thread());
                    ps.setString(13, e.messageTemplate());

                    ps.setString(14, e.exceptionClass());
                    ps.setString(15, e.exceptionMessage());
                    ps.setString(16, e.stacktrace());
                }
        );
    }
}

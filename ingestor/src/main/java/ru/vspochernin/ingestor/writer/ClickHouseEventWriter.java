package ru.vspochernin.ingestor.writer;

import java.sql.Timestamp;
import java.util.List;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import ru.vspochernin.ingestor.fingerprint.FingerprintResult;
import ru.vspochernin.ingestor.model.ErrorEvent;
import ru.vspochernin.ingestor.model.NormalizedErrorEvent;

@Component
@RequiredArgsConstructor
public class ClickHouseEventWriter implements ErrorEventWriter {

    // Максимальный размер батча для вставки в ClickHouse.
    // Если количество значений на вставку будет больше, Spring разобъет их на соответствующие батчи.
    private static final int INSERT_BATCH_SIZE = 1000;

    private final JdbcTemplate jdbcTemplate;

    private static final String INSERT_SQL = """
            INSERT INTO errlog_ch.error_events
            (
                event_id,
                timestamp,
                source_type,
                service,
                level,
                message_formatted,
                fingerprint,
                fingerprint_source,
            
                instance,
                service_version,
                logger,
                thread,
                message_template,
                exception_class,
                exception_message,
                stacktrace
            )
            VALUES (?,?,?,?,?,?,xxh3(?),?,?,?,?,?,?,?,?,?)
            """;

    @Override
    public void write(List<ErrorEvent> events) {
        if (events == null || events.isEmpty()) {
            return;
        }

        jdbcTemplate.batchUpdate(
                INSERT_SQL,
                events,
                INSERT_BATCH_SIZE,
                (ps, errorEvent) -> {
                    NormalizedErrorEvent normalizedErrorEvent = errorEvent.normalizedErrorEvent();
                    FingerprintResult fingerprintResult = errorEvent.fingerprintResult();

                    ps.setObject(1, errorEvent.eventId());
                    ps.setTimestamp(2, Timestamp.from(normalizedErrorEvent.timestamp()));
                    ps.setString(3, normalizedErrorEvent.sourceType());
                    ps.setString(4, normalizedErrorEvent.service());
                    ps.setString(5, normalizedErrorEvent.level());
                    ps.setString(6, normalizedErrorEvent.messageFormatted());
                    ps.setString(7, fingerprintResult.fingerprintBase()); // Для xxh3().
                    ps.setString(8, fingerprintResult.fingerprintSource().name());

                    ps.setString(9, normalizedErrorEvent.instance());
                    ps.setString(10, normalizedErrorEvent.serviceVersion());
                    ps.setString(11, normalizedErrorEvent.logger());
                    ps.setString(12, normalizedErrorEvent.thread());
                    ps.setString(13, normalizedErrorEvent.messageTemplate());
                    ps.setString(14, normalizedErrorEvent.exceptionClass());
                    ps.setString(15, normalizedErrorEvent.exceptionMessage());
                    ps.setString(16, normalizedErrorEvent.stacktrace());
                }
        );
    }
}

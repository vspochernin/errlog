package ru.vspochernin.ingestor.writer;

import java.sql.PreparedStatement;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.ParameterizedPreparedStatementSetter;
import ru.vspochernin.ingestor.fingerprint.FingerprintResult;
import ru.vspochernin.ingestor.fingerprint.FingerprintSource;
import ru.vspochernin.ingestor.model.ErrorEvent;
import ru.vspochernin.ingestor.model.NormalizedErrorEvent;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ClickHouseEventWriterTest {

    @Mock
    private JdbcTemplate jdbcTemplate;

    private ClickHouseEventWriter writer;

    @BeforeEach
    void setUp() {
        writer = new ClickHouseEventWriter(jdbcTemplate);
    }

    @Test
    void shouldNotWriteNullList() {
        writer.write(null);
        verify(jdbcTemplate, never()).batchUpdate(any(), any(), anyInt(), any());
    }

    @Test
    void shouldNotWriteEmptyList() {
        writer.write(List.of());
        verify(jdbcTemplate, never()).batchUpdate(any(), any(), anyInt(), any());
    }

    @Test
    void shouldCallBatchUpdateForSingleEvent() throws Exception {
        var event = createTestEvent();

        writer.write(List.of(event));

        @SuppressWarnings("unchecked")
        ArgumentCaptor<ParameterizedPreparedStatementSetter<ErrorEvent>> captor =
                ArgumentCaptor.forClass(ParameterizedPreparedStatementSetter.class);

        verify(jdbcTemplate).batchUpdate(
                any(),
                eq(List.of(event)),
                eq(1000),
                captor.capture());

        // Проверяем что сеттер проставляет параметры через настоящий mock PreparedStatement
        PreparedStatement ps = mock(PreparedStatement.class);
        captor.getValue().setValues(ps, event);

        verify(ps).setObject(1, event.eventId());
        verify(ps).setTimestamp(2, Timestamp.from(event.normalizedErrorEvent().timestamp()));
        verify(ps).setString(3, "java-spring-logback");
        verify(ps).setString(4, "test-svc");
        verify(ps).setString(5, "ERROR");
        verify(ps).setString(6, "formatted msg");
        verify(ps).setString(7, "fingerprint-base"); // база, не хэш
        verify(ps).setString(8, "STACKTRACE");
        verify(ps).setString(9, "instance-1");
        verify(ps).setString(10, "1.0.0");
        verify(ps).setString(11, "com.Logger");
        verify(ps).setString(12, "main");
        verify(ps).setString(13, "User {} not found");
        verify(ps).setString(14, "java.lang.RuntimeException");
        verify(ps).setString(15, "boom");
        verify(ps).setString(16, "at com.Foo.doIt(Foo.java:42)");
    }

    @Test
    void shouldCallBatchUpdateForMultipleEvents() {
        var e1 = createTestEvent();
        var e2 = createTestEvent();

        writer.write(List.of(e1, e2));

        verify(jdbcTemplate).batchUpdate(any(), eq(List.of(e1, e2)), eq(1000), any());
    }

    private static ErrorEvent createTestEvent() {
        return new ErrorEvent(
                UUID.fromString("00000000-0000-0000-0000-000000000001"),
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

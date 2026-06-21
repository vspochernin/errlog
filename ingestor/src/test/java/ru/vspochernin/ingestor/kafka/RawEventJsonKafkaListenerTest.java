package ru.vspochernin.ingestor.kafka;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.support.Acknowledgment;
import ru.vspochernin.ingestor.fingerprint.FingerprintResult;
import ru.vspochernin.ingestor.fingerprint.FingerprintSource;
import ru.vspochernin.ingestor.model.ErrorEvent;
import ru.vspochernin.ingestor.model.NormalizedErrorEvent;
import ru.vspochernin.ingestor.processing.RawEventJsonProcessor;
import ru.vspochernin.ingestor.writer.ErrorEventWriter;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RawEventJsonKafkaListenerTest {

    @Mock
    private RawEventJsonProcessor processor;

    @Mock
    private ErrorEventWriter writer;

    @Mock
    private Acknowledgment ack;

    private RawEventJsonKafkaListener listener;

    private static final String JSON1 = "{\"service\":\"svc1\"}";
    private static final String JSON2 = "{\"service\":\"svc2\"}";

    @BeforeEach
    void setUp() {
        listener = new RawEventJsonKafkaListener(processor, writer);
    }

    @Test
    void shouldProcessAndWriteAndAckAllSuccessfulEvents() {
        var event1 = createEvent();
        var event2 = createEvent();
        when(processor.process(JSON1)).thenReturn(Optional.of(event1));
        when(processor.process(JSON2)).thenReturn(Optional.of(event2));

        listener.listen(List.of(JSON1, JSON2), ack);

        // Порядок важен: сначала write, потом ack
        InOrder inOrder = inOrder(writer, ack);
        inOrder.verify(writer).write(List.of(event1, event2));
        inOrder.verify(ack).acknowledge();
    }

    @Test
    void shouldSkipEmptyResultsAndStillAck() {
        when(processor.process(JSON1)).thenReturn(Optional.empty());
        when(processor.process(JSON2)).thenReturn(Optional.empty());

        listener.listen(List.of(JSON1, JSON2), ack);

        // Ничего не пишем, но ack всё равно вызываем
        verify(writer, never()).write(anyList());
        verify(ack).acknowledge();
    }

    @Test
    void shouldWriteOnlySuccessfulEvents() {
        var event1 = createEvent();
        when(processor.process(JSON1)).thenReturn(Optional.of(event1));
        when(processor.process(JSON2)).thenReturn(Optional.empty());

        listener.listen(List.of(JSON1, JSON2), ack);

        verify(writer).write(List.of(event1));
        verify(ack).acknowledge();
    }

    @Test
    void shouldNotAckWhenWriterThrowsAndPropagateException() {
        var event = createEvent();
        when(processor.process(JSON1)).thenReturn(Optional.of(event));
        doThrow(new RuntimeException("ClickHouse unavailable")).when(writer).write(anyList());

        // Исключение пробрасывается наружу, ack НЕ вызывается — at-least-once.
        assertThatThrownBy(() -> listener.listen(List.of(JSON1), ack))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("ClickHouse unavailable");

        verify(ack, never()).acknowledge();
    }

    @Test
    void shouldAckEvenWithEmptyBatch() {
        listener.listen(List.of(), ack);

        verify(writer, never()).write(anyList());
        verify(ack).acknowledge();
    }

    private static ErrorEvent createEvent() {
        return new ErrorEvent(
                UUID.randomUUID(),
                new NormalizedErrorEvent(
                        Instant.EPOCH, "t", "svc", "ERROR", "msg",
                        null, null, null, null, null, null, null, null),
                new FingerprintResult("base", FingerprintSource.MINIMAL));
    }
}

package ru.vspochernin.ingestor.processing;

import java.time.Instant;
import java.util.Optional;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.vspochernin.ingestor.fingerprint.FingerprintBuilder;
import ru.vspochernin.ingestor.fingerprint.FingerprintResult;
import ru.vspochernin.ingestor.fingerprint.FingerprintSource;
import ru.vspochernin.ingestor.model.NormalizedErrorEvent;
import ru.vspochernin.ingestor.normalization.RawEventNormalizer;
import ru.vspochernin.ingestor.normalization.RawEventNormalizerRegistry;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DefaultRawEventJsonProcessorTest {

    @Mock
    private RawEventNormalizerRegistry normalizerRegistry;

    @Mock
    private FingerprintBuilder fingerprintBuilder;

    private DefaultRawEventJsonProcessor processor;

    private static final String VALID_JSON = """
            {"sourceType":"java-spring-logback","timestamp":1771000000000,"service":"svc","level":"ERROR","message":"boom"}
            """;

    @BeforeEach
    void setUp() {
        processor = new DefaultRawEventJsonProcessor(
                new ObjectMapper(), normalizerRegistry, fingerprintBuilder);
    }

    @Test
    void shouldReturnEmptyForNullInput() {
        assertThat(processor.process(null)).isEmpty();
        verifyNoInteractions();
    }

    @Test
    void shouldReturnEmptyForEmptyString() {
        assertThat(processor.process("")).isEmpty();
        verifyNoInteractions();
    }

    @Test
    void shouldReturnEmptyForBlankString() {
        assertThat(processor.process("   ")).isEmpty();
        verifyNoInteractions();
    }

    @Test
    void shouldReturnEmptyForInvalidJson() {
        assertThat(processor.process("not a json")).isEmpty();
        verifyNoInteractions();
    }

    @Test
    void shouldReturnEmptyForUnknownSourceType() {
        var normalizer = new UnknownSourceNormalizer();
        when(normalizerRegistry.getNormalizer(any())).thenReturn(normalizer);

        // Неизвестный sourceType → UnknownRawEventNormalizer → empty → событие скипается
        assertThat(processor.process(VALID_JSON)).isEmpty();
    }

    @Test
    void shouldReturnEmptyWhenNormalizerThrowsException() {
        var normalizer = new ThrowingNormalizer();
        when(normalizerRegistry.getNormalizer(any())).thenReturn(normalizer);

        assertThat(processor.process(VALID_JSON)).isEmpty();
    }

    @Test
    void shouldReturnEmptyWhenNormalizerReturnsEmpty() {
        var normalizer = new EmptyNormalizer();
        when(normalizerRegistry.getNormalizer(any())).thenReturn(normalizer);

        assertThat(processor.process(VALID_JSON)).isEmpty();
        verify(fingerprintBuilder, never()).build(any());
    }

    @Test
    void shouldReturnEmptyWhenFingerprintBuilderThrows() {
        var normalizer = new HappyNormalizer();
        when(normalizerRegistry.getNormalizer(any())).thenReturn(normalizer);
        when(fingerprintBuilder.build(any())).thenThrow(new RuntimeException("fingerprint failed"));

        assertThat(processor.process(VALID_JSON)).isEmpty();
    }

    @Test
    void shouldReturnEventOnHappyPath() {
        var normalizer = new HappyNormalizer();
        when(normalizerRegistry.getNormalizer(any())).thenReturn(normalizer);
        var fingerprintResult = new FingerprintResult("base", FingerprintSource.STACKTRACE);
        when(fingerprintBuilder.build(any())).thenReturn(fingerprintResult);

        var result = processor.process(VALID_JSON);

        assertThat(result).isPresent();
        var event = result.get();
        // eventId случайный - не проверяем точное значение, только что не null
        assertThat(event.eventId()).isNotNull();
        assertThat(event.fingerprintResult()).isEqualTo(fingerprintResult);
        assertThat(event.normalizedErrorEvent().service()).isEqualTo("happy-svc");
        assertThat(event.normalizedErrorEvent().level()).isEqualTo("ERROR");
    }

    private void verifyNoInteractions() {
        // Ни registry, ни fingerprintBuilder не должны вызываться
        verify(normalizerRegistry, never()).getNormalizer(any());
        verify(fingerprintBuilder, never()).build(any());
    }

    // Stubs для тестовых сценариев

    private static class HappyNormalizer implements RawEventNormalizer {
        @Override
        public String sourceType() {
            return "java-spring-logback";
        }

        @Override
        public Optional<NormalizedErrorEvent> normalize(com.fasterxml.jackson.databind.JsonNode rawEvent) {
            return Optional.of(new NormalizedErrorEvent(
                    Instant.EPOCH, "java-spring-logback", "happy-svc", "ERROR", "test msg",
                    null, null, null, null, null, null, null, null));
        }
    }

    private static class EmptyNormalizer implements RawEventNormalizer {
        @Override
        public String sourceType() {
            return "java-spring-logback";
        }

        @Override
        public Optional<NormalizedErrorEvent> normalize(com.fasterxml.jackson.databind.JsonNode rawEvent) {
            return Optional.empty();
        }
    }

    private static class ThrowingNormalizer implements RawEventNormalizer {
        @Override
        public String sourceType() {
            return "java-spring-logback";
        }

        @Override
        public Optional<NormalizedErrorEvent> normalize(com.fasterxml.jackson.databind.JsonNode rawEvent) {
            throw new RuntimeException("normalization failed");
        }
    }

    private static class UnknownSourceNormalizer implements RawEventNormalizer {
        @Override
        public String sourceType() {
            return "__unknown__";
        }

        @Override
        public Optional<NormalizedErrorEvent> normalize(com.fasterxml.jackson.databind.JsonNode rawEvent) {
            return Optional.empty();
        }
    }
}

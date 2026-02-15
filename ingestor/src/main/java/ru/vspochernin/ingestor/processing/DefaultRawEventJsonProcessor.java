package ru.vspochernin.ingestor.processing;

import java.util.Optional;
import java.util.UUID;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.vspochernin.ingestor.fingerprint.FingerprintCalculator;
import ru.vspochernin.ingestor.fingerprint.FingerprintResult;
import ru.vspochernin.ingestor.model.ErrorEvent;
import ru.vspochernin.ingestor.model.NormalizedErrorEvent;
import ru.vspochernin.ingestor.normalization.RawEventNormalizer;
import ru.vspochernin.ingestor.normalization.RawEventNormalizerRegistry;

@Component
@RequiredArgsConstructor
@Slf4j
public class DefaultRawEventJsonProcessor implements RawEventJsonProcessor {

    private final ObjectMapper objectMapper;
    private final RawEventNormalizerRegistry normalizerRegistry;
    private final FingerprintCalculator fingerprintCalculator;

    @Override
    public Optional<ErrorEvent> process(String rawEventJson) {
        if (rawEventJson == null || rawEventJson.isBlank()) {
            return Optional.empty();
        }

        final JsonNode rawEvent;
        try {
            rawEvent = objectMapper.readTree(rawEventJson);
        } catch (Exception e) {
            log.warn("Skip rawEventJson={} because of {}", rawEventJson, e.getMessage());
            return Optional.empty();
        }

        String sourceType = rawEvent.path("sourceType").asText(null);
        RawEventNormalizer normalizer = normalizerRegistry.getNormalizer(sourceType);

        Optional<NormalizedErrorEvent> normalizedErrorEventO;
        try {
            normalizedErrorEventO = normalizer.normalize(rawEvent);
        } catch (Exception e) {
            log.warn("Skip rawEvent={} because of {}", rawEvent, e.getMessage());
            return Optional.empty();
        }

        if (normalizedErrorEventO.isEmpty()) {
            return Optional.empty();
        }
        NormalizedErrorEvent normalizedErrorEvent = normalizedErrorEventO.get();

        FingerprintResult fingerprintResult;
        try {
            fingerprintResult = fingerprintCalculator.calculate(normalizedErrorEvent);
        } catch (Exception e) {
            log.warn("Skip normalizedErrorEvent={} because of {}", normalizedErrorEvent, e.getMessage());
            return Optional.empty();
        }

        return Optional.of(new ErrorEvent(
                UUID.randomUUID(),
                normalizedErrorEvent,
                fingerprintResult));
    }
}

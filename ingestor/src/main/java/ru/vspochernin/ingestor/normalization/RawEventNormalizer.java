package ru.vspochernin.ingestor.normalization;

import java.util.Optional;

import com.fasterxml.jackson.databind.JsonNode;
import ru.vspochernin.ingestor.model.NormalizedErrorEvent;

public interface RawEventNormalizer {

    String sourceType();

    Optional<NormalizedErrorEvent> normalize(JsonNode rawEvent);
}

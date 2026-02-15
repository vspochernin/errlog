package ru.vspochernin.ingestor.normalization;

import java.util.Optional;

import com.fasterxml.jackson.databind.JsonNode;
import ru.vspochernin.ingestor.model.ErrorEvent;

public interface RawEventNormalizer {

    String sourceType();

    Optional<ErrorEvent> normalize(JsonNode rawEvent);
}

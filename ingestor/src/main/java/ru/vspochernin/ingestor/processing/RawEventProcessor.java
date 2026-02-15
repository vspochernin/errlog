package ru.vspochernin.ingestor.processing;

import java.util.Optional;

import com.fasterxml.jackson.databind.JsonNode;
import ru.vspochernin.ingestor.model.ErrorEvent;

public interface RawEventProcessor {

    String sourceType();

    Optional<ErrorEvent> process(JsonNode rawEvent);
}

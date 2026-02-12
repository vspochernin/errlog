package ru.vspochernin.ingestor.processing;

import java.util.Optional;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.vspochernin.ingestor.model.ErrorEvent;

@Component
@Slf4j
public class UnknownRawEventProcessor implements RawEventProcessor {

    @Override
    public String sourceType() {
        return RawEventProcessorRegistry.UNKNOWN_RAW_EVENT_PROCESSOR_SOURCE_TYPE;
    }

    @Override
    public Optional<ErrorEvent> processEvent(JsonNode rawEvent) {
        String sourceType = rawEvent.path("sourceType").asText(null);
        log.error("Unknown sourceType={}, drop event", sourceType);
        return Optional.empty();
    }
}

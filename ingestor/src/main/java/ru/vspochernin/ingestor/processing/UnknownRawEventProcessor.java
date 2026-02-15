package ru.vspochernin.ingestor.processing;

import java.util.Optional;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.vspochernin.ingestor.model.ErrorEvent;

@Component
@Slf4j
public class UnknownRawEventProcessor implements RawEventProcessor {

    // Фиктивный тип для неизвестных системе sourceType.
    static final String SOURCE_TYPE = "__unknown__";

    @Override
    public String sourceType() {
        return SOURCE_TYPE;
    }

    @Override
    public Optional<ErrorEvent> process(JsonNode rawEvent) {
        String sourceType = rawEvent.path("sourceType").asText(SOURCE_TYPE);
        log.error("Unknown sourceType={}, rawEvent={} drop event", sourceType, rawEvent);
        return Optional.empty();
    }
}

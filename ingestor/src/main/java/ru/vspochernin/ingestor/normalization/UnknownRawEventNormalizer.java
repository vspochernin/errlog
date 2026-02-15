package ru.vspochernin.ingestor.normalization;

import java.util.Optional;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.vspochernin.ingestor.model.ErrorEvent;

@Component
@Slf4j
public class UnknownRawEventNormalizer implements RawEventNormalizer {

    // Фиктивный тип для неизвестных системе sourceType.
    static final String SOURCE_TYPE = "__unknown__";

    @Override
    public String sourceType() {
        return SOURCE_TYPE;
    }

    @Override
    public Optional<ErrorEvent> normalize(JsonNode rawEvent) {
        String sourceType = rawEvent.path("sourceType").asText(SOURCE_TYPE);
        log.error("Unknown sourceType={}, skip rawEvent={}", sourceType, rawEvent);
        return Optional.empty();
    }
}

package ru.vspochernin.ingestor.processing;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.vspochernin.ingestor.dto.JavaSpringLogbackJsonRawEventDto;
import ru.vspochernin.ingestor.model.ErrorEvent;
import ru.vspochernin.ingestor.utils.StringUtils;

@Component
@RequiredArgsConstructor
@Slf4j
public class JavaSpringLogbackJsonRawEventProcessor implements RawEventProcessor {

    private final ObjectMapper objectMapper;

    @Override
    public String sourceType() {
        return "java-spring-logback-json";
    }

    @Override
    public Optional<ErrorEvent> process(JsonNode rawEvent) {
        JavaSpringLogbackJsonRawEventDto dto;
        try {
            dto = objectMapper.treeToValue(rawEvent, JavaSpringLogbackJsonRawEventDto.class);
        } catch (JsonProcessingException e) {
            log.error("Error processing rawEvent={} because of exception {}", rawEvent, e.getMessage());
            return Optional.empty();
        }

        return Optional.of(new ErrorEvent(
                UUID.randomUUID(),
                dto.timestamp() > 0 ? Instant.ofEpochMilli(dto.timestamp()) : Instant.now(),
                sourceType(),
                StringUtils.getOrDefault(dto.service(), "unknown-service"),
                StringUtils.getOrDefault(dto.level(), "UNKNOWN"),
                StringUtils.getFirstNonBlankOrDefault(dto.formattedMessage(), dto.message(), "empty message"),
                0L, // TODO: will be implemented in 6.
                "UNKNOWN", // TODO: will be implemented in 6.

                dto.instance(),
                dto.serviceVersion(),
                dto.loggerName(),
                dto.threadName(),
                dto.message(),
                dto.throwable() != null ? dto.throwable().className() : null,
                dto.throwable() != null ? dto.throwable().message() : null,
                dto.getStacktraceFormatted()));
    }
}

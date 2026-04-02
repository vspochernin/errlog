package ru.vspochernin.ingestor.normalization;

import java.time.Instant;
import java.util.Optional;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.vspochernin.ingestor.dto.JavaSpringLogbackRawEventDto;
import ru.vspochernin.ingestor.model.NormalizedErrorEvent;
import ru.vspochernin.ingestor.utils.StringUtils;

@Component
@RequiredArgsConstructor
@Slf4j
public class JavaSpringLogbackRawEventNormalizer implements RawEventNormalizer {

    private final ObjectMapper objectMapper;

    @Override
    public String sourceType() {
        return "java-spring-logback";
    }

    @Override
    public Optional<NormalizedErrorEvent> normalize(JsonNode rawEvent) {
        JavaSpringLogbackRawEventDto dto;
        try {
            dto = objectMapper.treeToValue(rawEvent, JavaSpringLogbackRawEventDto.class);
        } catch (JsonProcessingException e) {
            log.error("Json processing exception on rawEvent={} because of {}", rawEvent, e.getMessage());
            return Optional.empty();
        }

        long ts = dto.timestamp();
        if (ts <= 0) {
            log.error("Raw event timestamp is <= 0, ts={}, rawEvent={}", ts, rawEvent);
            return Optional.empty();
        }

        return Optional.of(new NormalizedErrorEvent(
                Instant.ofEpochMilli(ts),
                sourceType(),
                StringUtils.getOrDefault(dto.service(), "__unknown-service__"),
                StringUtils.getOrDefault(dto.level(), "__UNKNOWN__"),
                StringUtils.getFirstNonBlankOrDefault(dto.formattedMessage(), dto.message(), "__empty message__"),

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

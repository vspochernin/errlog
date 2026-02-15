package ru.vspochernin.ingestor.kafka;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;
import ru.vspochernin.ingestor.model.ErrorEvent;
import ru.vspochernin.ingestor.normalization.RawEventNormalizerRegistry;
import ru.vspochernin.ingestor.writer.ErrorEventWriter;

@Component
@RequiredArgsConstructor
@Slf4j
public class RawEventKafkaListener {

    private final ObjectMapper objectMapper;
    private final RawEventNormalizerRegistry normalizerRegistry;
    private final ErrorEventWriter eventWriter;

    @KafkaListener(topics = "${KAFKA_TOPIC}")
    public void listen(List<String> rawEvents, Acknowledgment ack) {
        List<ErrorEvent> batch = new ArrayList<>(rawEvents.size());
        AtomicInteger skipCount = new AtomicInteger();

        for (String rawEventStr : rawEvents) {
            try {
                JsonNode rawEvent = objectMapper.readTree(rawEventStr);
                String sourceType = rawEvent.path("sourceType").asText(null);

                normalizerRegistry.getNormalizer(sourceType)
                        .normalize(rawEvent)
                        .ifPresentOrElse(normalizedErrorEvent -> {
                            ErrorEvent ee = new ErrorEvent(
                                    UUID.randomUUID(),
                                    normalizedErrorEvent.timestamp(),
                                    normalizedErrorEvent.sourceType(),
                                    normalizedErrorEvent.service(),
                                    normalizedErrorEvent.level(),
                                    normalizedErrorEvent.messageFormatted(),
                                    0L,
                                    "UNKNOWN",
                                    normalizedErrorEvent.instance(),
                                    normalizedErrorEvent.serviceVersion(),
                                    normalizedErrorEvent.logger(),
                                    normalizedErrorEvent.messageTemplate(),
                                    normalizedErrorEvent.exceptionClass(),
                                    normalizedErrorEvent.exceptionMessage(),
                                    normalizedErrorEvent.exceptionMessage(),
                                    normalizedErrorEvent.stacktrace());
                            batch.add(ee);
                        }, skipCount::getAndIncrement);
            } catch (Exception e) {
                skipCount.getAndIncrement();
                log.warn("Skip raw event {} because of {}", rawEventStr, e.getMessage());
            }
        }

        if (!batch.isEmpty()) {
            eventWriter.write(batch);
        }

        // Если вставка упадет - ack не будет вызван и Kafka переотдаст эвенты.
        // Таким образом реализуется семантика (at-least-once).
        ack.acknowledge();
        log.info("Processed batch: total={}, inserted={}, skipped={}", rawEvents.size(), batch.size(), skipCount.get());
    }
}

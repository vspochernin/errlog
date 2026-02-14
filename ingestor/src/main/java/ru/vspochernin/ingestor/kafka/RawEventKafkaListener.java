package ru.vspochernin.ingestor.kafka;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;
import ru.vspochernin.ingestor.model.ErrorEvent;
import ru.vspochernin.ingestor.processing.RawEventProcessorRegistry;
import ru.vspochernin.ingestor.writer.ErrorEventWriter;

@Component
@RequiredArgsConstructor
@Slf4j
public class RawEventKafkaListener {

    private final ObjectMapper objectMapper;
    private final RawEventProcessorRegistry processorRegistry;
    private final ErrorEventWriter writer;

    @KafkaListener(topics = "${KAFKA_TOPIC}")
    public void onMessage(List<String> values, Acknowledgment ack) {
        List<ErrorEvent> batch = new ArrayList<>(values.size());
        AtomicInteger skipped = new AtomicInteger();

        for (String value : values) {
            try {
                JsonNode raw = objectMapper.readTree(value);
                String sourceType = raw.path("sourceType").asText(null);

                processorRegistry.getProcessor(sourceType)
                        .processEvent(raw)
                        .ifPresentOrElse(batch::add, skipped::getAndIncrement);
            } catch (Exception e) {
                skipped.getAndIncrement();
                log.warn("Skip raw event {} because of {}", value, e.getMessage());
            }
        }

        if (!batch.isEmpty()) {
            writer.writeBatch(batch);
        }

        // Если вставка упадет - ack не будет вызван и Kafka переотдаст батч.
        // Таким образом реализуем семантику (at-least-once).
        ack.acknowledge();
        log.info("Ingestor batch: total={}, inserted={}, skipped={}", values.size(), batch.size(), skipped.get());
    }
}

package ru.vspochernin.ingestor.kafka;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;
import ru.vspochernin.ingestor.model.ErrorEvent;
import ru.vspochernin.ingestor.processing.RawEventJsonProcessor;
import ru.vspochernin.ingestor.writer.ErrorEventWriter;

@Component
@RequiredArgsConstructor
@Slf4j
public class RawEventJsonKafkaListener {

    private final RawEventJsonProcessor rawEventJsonProcessor;
    private final ErrorEventWriter eventWriter;

    @KafkaListener(topics = "${KAFKA_TOPIC}")
    public void listen(List<String> rawEventJsons, Acknowledgment ack) {
        List<ErrorEvent> events = new ArrayList<>(rawEventJsons.size());
        AtomicInteger skipCount = new AtomicInteger();

        for (String rawEventJson : rawEventJsons) {
            rawEventJsonProcessor.process(rawEventJson)
                    .ifPresentOrElse(events::add, skipCount::incrementAndGet);
        }

        if (!events.isEmpty()) {
            try {
                eventWriter.write(events);
            } catch (Exception e) {
                log.error(
                        "ClickHouse insert failed, no ack (will retry), total={}, toInsert={}, skipped={}",
                        rawEventJsons.size(),
                        events.size(),
                        skipCount.get(),
                        e);
                throw e;
            }
        }

        // Если вставка упадет - ack не будет вызван и Kafka переотдаст эвенты.
        // Таким образом реализуется семантика (at-least-once).
        ack.acknowledge();
        log.info(
                "Processed batch of rawEventJsons: total={}, inserted={}, skipped={}",
                rawEventJsons.size(),
                events.size(),
                skipCount.get());
    }
}

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
        List<ErrorEvent> batch = new ArrayList<>(rawEventJsons.size());
        AtomicInteger skipCount = new AtomicInteger();

        for (String rawEventJson : rawEventJsons) {
            rawEventJsonProcessor.process(rawEventJson)
                    .ifPresentOrElse(batch::add, skipCount::incrementAndGet);
        }

        if (!batch.isEmpty()) {
            eventWriter.write(batch);
        }

        // Если вставка упадет - ack не будет вызван и Kafka переотдаст эвенты.
        // Таким образом реализуется семантика (at-least-once).
        ack.acknowledge();
        log.info(
                "Processed batch: total={}, inserted={}, skipped={}",
                rawEventJsons.size(),
                batch.size(),
                skipCount.get());
    }
}

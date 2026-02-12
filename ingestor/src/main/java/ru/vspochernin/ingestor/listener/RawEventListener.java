package ru.vspochernin.ingestor.listener;

import java.util.List;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;
import ru.vspochernin.ingestor.dto.JavaSpringLogbackJsonRawEventDto;

@Component
@Slf4j
@RequiredArgsConstructor
public class RawEventListener {

    private final ObjectMapper objectMapper;
    private static long SUM = 0;

    @KafkaListener(topics = "${KAFKA_TOPIC}")
    public void onMessage(List<String> values, Acknowledgment ack) {
        SUM += values.size();
        log.info("Received {} events", values.size());
        log.info("Sum is {}", SUM);
        try {
            for (val value : values) {
                JsonNode node = objectMapper.readTree(value);
                JavaSpringLogbackJsonRawEventDto eventDto =
                        objectMapper.treeToValue(node, JavaSpringLogbackJsonRawEventDto.class);
                log.info("ingestor got eventDto: {}", eventDto);
            }
            ack.acknowledge();
        } catch (JsonProcessingException e) {
            log.error("Can't read json tree: {} ", e.getMessage());
        }
    }
}

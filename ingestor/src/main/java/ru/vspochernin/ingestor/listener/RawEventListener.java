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

                String level = node.path("level").asText("UNKNOWN");
                String service = node.path("service").asText("unknown-service");
                String instance = node.path("instance").asText("unknown-instance");
                String sourceType = node.path("sourceType").asText("unknown-source-type");

                log.info("ingestor got event: level={} service={}, instance={} AsourceType={}", level, service, instance, sourceType);
            }

            ack.acknowledge();
        } catch (JsonProcessingException e) {
            log.error("Can't read json tree: {} ", e.getMessage());
        }
    }
}

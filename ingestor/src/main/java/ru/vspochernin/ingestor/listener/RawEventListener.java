package ru.vspochernin.ingestor.listener;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class RawEventListener {

    private final ObjectMapper objectMapper;

    @KafkaListener(topics = "${KAFKA_TOPIC}")
    public void onMessage(String value, Acknowledgment ack) {
        try {
            JsonNode node = objectMapper.readTree(value);

            String level = node.path("level").asText("UNKNOWN");
            String service = node.path("service").asText("unknown-service");
            String sourceType = node.path("sourceType").asText("unknown-source-type");

            log.info("ingestor got event: level={} service={} sourceType={}", level, service, sourceType);

            ack.acknowledge();
        } catch (JsonProcessingException e) {
            log.error("Can't read json tree: {} ", e.getMessage());
        }
    }
}

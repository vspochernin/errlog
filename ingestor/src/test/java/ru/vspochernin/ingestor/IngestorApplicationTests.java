package ru.vspochernin.ingestor;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
@Disabled("Требует инфраструктуру: Kafka + ClickHouse + env-переменные. " +
          "Запускайте этот тест только при поднятых Docker-контурах стенда.")
class IngestorApplicationTests {

    @Test
    void contextLoads() {
    }
}

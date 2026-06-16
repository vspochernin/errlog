package ru.vspochernin.errapi;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
@Disabled("Требует инфраструктуру: PostgreSQL + ClickHouse + env-переменные. " +
          "Запускайте этот тест только при поднятых Docker-контурах стенда.")
class ErrapiApplicationTests {

	@Test
	void contextLoads() {
	}

}

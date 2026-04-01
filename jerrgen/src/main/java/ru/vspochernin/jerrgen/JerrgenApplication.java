package ru.vspochernin.jerrgen;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class JerrgenApplication {

    public static void main(String[] args) {
        SpringApplication.run(JerrgenApplication.class, args);
    }
}

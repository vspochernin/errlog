package ru.vspochernin.jerrgen.generator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import ru.vspochernin.jerrgen.utils.CommonUtils;

@Component
public class SimpleGenerator {

    private static final Logger log = LoggerFactory.getLogger(SimpleGenerator.class);

    private static final int SLEEP_BEFORE_GENERATE_MILLIS = 5000;
    private static final int SCHEDULED_FIXED_RATE_MILLIS = 500;

    @Scheduled(fixedRate = SCHEDULED_FIXED_RATE_MILLIS)
    public void generateInfo() {
        CommonUtils.safeSleep(SLEEP_BEFORE_GENERATE_MILLIS);
        log.info("There is info with int: {} and double: {}", CommonUtils.getNextInt(), CommonUtils.getNextDouble());
    }

    @Scheduled(fixedRate = SCHEDULED_FIXED_RATE_MILLIS)
    public void generateWarn() {
        CommonUtils.safeSleep(SLEEP_BEFORE_GENERATE_MILLIS);
        log.warn("There is warn with int: {} and double: {}", CommonUtils.getNextInt(), CommonUtils.getNextDouble());
    }

    @Scheduled(fixedRate = SCHEDULED_FIXED_RATE_MILLIS)
    public void generateError() {
        CommonUtils.safeSleep(SLEEP_BEFORE_GENERATE_MILLIS);
        log.error("There is error with int: {} and double: {}", CommonUtils.getNextInt(), CommonUtils.getNextDouble());
    }

    @Scheduled(fixedRate = SCHEDULED_FIXED_RATE_MILLIS)
    public void generateException() {
        CommonUtils.safeSleep(SLEEP_BEFORE_GENERATE_MILLIS);
        log.error("There is exception", new IllegalAccessException("Exception message"));
    }

    @Scheduled(fixedRate = SCHEDULED_FIXED_RATE_MILLIS)
    public void generateExceptionLogMessageWithPlaceholders() {
        CommonUtils.safeSleep(SLEEP_BEFORE_GENERATE_MILLIS);
        log.error(
                "There is exception with int: {} and double: {}",
                CommonUtils.getNextInt(),
                CommonUtils.getNextDouble(),
                new IllegalAccessException("Exception message, but log message with placeholders"));
    }
}

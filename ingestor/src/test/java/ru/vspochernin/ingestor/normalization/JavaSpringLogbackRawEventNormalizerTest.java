package ru.vspochernin.ingestor.normalization;

import java.time.Instant;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ru.vspochernin.ingestor.model.NormalizedErrorEvent;

import static org.assertj.core.api.Assertions.assertThat;

class JavaSpringLogbackRawEventNormalizerTest {

    private JavaSpringLogbackRawEventNormalizer normalizer;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        normalizer = new JavaSpringLogbackRawEventNormalizer(objectMapper);
    }

    @Test
    void shouldReturnCorrectSourceType() {
        assertThat(normalizer.sourceType()).isEqualTo("java-spring-logback");
    }

    @Test
    void shouldNormalizeFullEvent() throws Exception {
        String json = """
                {
                    "timestamp": 1771000000000,
                    "service": "my-service",
                    "level": "ERROR",
                    "formattedMessage": "User 42 not found",
                    "message": "User {} not found",
                    "instance": "my-instance",
                    "serviceVersion": "1.2.3",
                    "loggerName": "com.example.Logger",
                    "threadName": "http-nio-8080-exec-1",
                    "sourceType": "java-spring-logback",
                    "throwable": {
                        "className": "java.lang.RuntimeException",
                        "message": "something broke",
                        "stepArray": [
                            {
                                "className": "com.example.Foo",
                                "methodName": "doIt",
                                "fileName": "Foo.java",
                                "lineNumber": 42
                            }
                        ]
                    }
                }
                """;
        var raw = objectMapper.readTree(json);
        var result = normalizer.normalize(raw);

        assertThat(result).isPresent();
        NormalizedErrorEvent e = result.get();
        assertThat(e.timestamp()).isEqualTo(Instant.ofEpochMilli(1771000000000L));
        assertThat(e.sourceType()).isEqualTo("java-spring-logback");
        assertThat(e.service()).isEqualTo("my-service");
        assertThat(e.level()).isEqualTo("ERROR");
        assertThat(e.messageFormatted()).isEqualTo("User 42 not found");
        assertThat(e.instance()).isEqualTo("my-instance");
        assertThat(e.serviceVersion()).isEqualTo("1.2.3");
        assertThat(e.logger()).isEqualTo("com.example.Logger");
        assertThat(e.thread()).isEqualTo("http-nio-8080-exec-1");
        assertThat(e.messageTemplate()).isEqualTo("User {} not found");
        assertThat(e.exceptionClass()).isEqualTo("java.lang.RuntimeException");
        assertThat(e.exceptionMessage()).isEqualTo("something broke");
        assertThat(e.stacktrace()).contains("java.lang.RuntimeException: something broke");
        assertThat(e.stacktrace()).contains("\tat com.example.Foo.doIt(Foo.java:42)");
    }

    @Test
    void shouldUseDefaultsForMissingFields() throws Exception {
        String json = """
                {
                    "timestamp": 1771000000000,
                    "level": "ERROR",
                    "message": "simple message"
                }
                """;
        var raw = objectMapper.readTree(json);
        var result = normalizer.normalize(raw);

        assertThat(result).isPresent();
        NormalizedErrorEvent e = result.get();
        assertThat(e.service()).isEqualTo("__unknown-service__");
        assertThat(e.messageFormatted()).isEqualTo("simple message");
        assertThat(e.instance()).isNull();
        assertThat(e.serviceVersion()).isNull();
        assertThat(e.logger()).isNull();
        assertThat(e.thread()).isNull();
    }

    @Test
    void levelDefaultShouldBeUnknown() throws Exception {
        String json = """
                {
                    "timestamp": 1771000000000,
                    "service": "svc",
                    "message": "msg"
                }
                """;
        var raw = objectMapper.readTree(json);
        var result = normalizer.normalize(raw);

        assertThat(result).isPresent();
        assertThat(result.get().level()).isEqualTo("__UNKNOWN__");
    }

    @Test
    void shouldPreferFormattedMessageOverMessage() throws Exception {
        String json = """
                {
                    "timestamp": 1771000000000,
                    "service": "svc",
                    "level": "WARN",
                    "formattedMessage": "formatted text",
                    "message": "template {}"
                }
                """;
        var raw = objectMapper.readTree(json);
        var result = normalizer.normalize(raw);

        assertThat(result).isPresent();
        assertThat(result.get().messageFormatted()).isEqualTo("formatted text");
    }

    @Test
    void shouldFallbackToMessageWhenFormattedMessageMissing() throws Exception {
        String json = """
                {
                    "timestamp": 1771000000000,
                    "service": "svc",
                    "level": "WARN",
                    "message": "only template"
                }
                """;
        var raw = objectMapper.readTree(json);
        var result = normalizer.normalize(raw);

        assertThat(result).isPresent();
        assertThat(result.get().messageFormatted()).isEqualTo("only template");
    }

    @Test
    void shouldUseDefaultMessageWhenBothMessageAndFormattedMessageMissing() throws Exception {
        String json = """
                {
                    "timestamp": 1771000000000,
                    "service": "svc",
                    "level": "WARN"
                }
                """;
        var raw = objectMapper.readTree(json);
        var result = normalizer.normalize(raw);

        assertThat(result).isPresent();
        assertThat(result.get().messageFormatted()).isEqualTo("__empty message__");
    }

    @Test
    void shouldReturnEmptyWhenTimestampIsZero() throws Exception {
        String json = """
                {
                    "timestamp": 0,
                    "service": "svc",
                    "level": "WARN",
                    "message": "msg"
                }
                """;
        var raw = objectMapper.readTree(json);
        var result = normalizer.normalize(raw);

        assertThat(result).isEmpty();
    }

    @Test
    void shouldReturnEmptyWhenTimestampIsNegative() throws Exception {
        String json = """
                {
                    "timestamp": -100,
                    "service": "svc",
                    "level": "WARN",
                    "message": "msg"
                }
                """;
        var raw = objectMapper.readTree(json);
        var result = normalizer.normalize(raw);

        assertThat(result).isEmpty();
    }

    @Test
    void shouldReturnEmptyForInvalidJson() {
        // Конструируем что-то, что не маппится в DTO - например, timestamp как строка
        ObjectNode raw = JsonNodeFactory.instance.objectNode();
        raw.put("timestamp", "not_a_number");

        var result = normalizer.normalize(raw);
        assertThat(result).isEmpty();
    }

    @Test
    void shouldReturnNullStacktraceWhenNoThrowable() throws Exception {
        String json = """
                {
                    "timestamp": 1771000000000,
                    "service": "svc",
                    "level": "WARN",
                    "message": "msg"
                }
                """;
        var raw = objectMapper.readTree(json);
        var result = normalizer.normalize(raw);

        assertThat(result).isPresent();
        NormalizedErrorEvent e = result.get();
        assertThat(e.exceptionClass()).isNull();
        assertThat(e.exceptionMessage()).isNull();
        assertThat(e.stacktrace()).isNull();
    }

    @Test
    void throwableWithoutStepArrayShouldStillHaveStacktraceHeader() throws Exception {
        String json = """
                {
                    "timestamp": 1771000000000,
                    "service": "svc",
                    "level": "WARN",
                    "message": "msg",
                    "throwable": {
                        "className": "java.lang.RuntimeException",
                        "message": "boom"
                    }
                }
                """;
        var raw = objectMapper.readTree(json);
        var result = normalizer.normalize(raw);

        assertThat(result).isPresent();
        NormalizedErrorEvent e = result.get();
        assertThat(e.exceptionClass()).isEqualTo("java.lang.RuntimeException");
        assertThat(e.exceptionMessage()).isEqualTo("boom");
        assertThat(e.stacktrace()).isEqualTo("java.lang.RuntimeException: boom");
    }

    @Test
    void nullFieldsShouldBePassedAsNull() throws Exception {
        String json = """
                {
                    "timestamp": 1771000000000,
                    "service": "svc",
                    "level": "WARN",
                    "message": "msg",
                    "instance": null,
                    "serviceVersion": null,
                    "loggerName": null,
                    "threadName": null
                }
                """;
        var raw = objectMapper.readTree(json);
        var result = normalizer.normalize(raw);

        assertThat(result).isPresent();
        NormalizedErrorEvent e = result.get();
        assertThat(e.instance()).isNull();
        assertThat(e.serviceVersion()).isNull();
        assertThat(e.logger()).isNull();
        assertThat(e.thread()).isNull();
    }
}

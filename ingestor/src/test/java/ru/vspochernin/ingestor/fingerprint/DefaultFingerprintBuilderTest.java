package ru.vspochernin.ingestor.fingerprint;

import java.time.Instant;

import org.junit.jupiter.api.Test;
import ru.vspochernin.ingestor.model.NormalizedErrorEvent;

import static org.assertj.core.api.Assertions.assertThat;

class DefaultFingerprintBuilderTest {

    private final DefaultFingerprintBuilder builder = new DefaultFingerprintBuilder();

    private static NormalizedErrorEvent event(String stacktrace, String exceptionClass,
                                              String exceptionMessage, String messageTemplate) {
        return new NormalizedErrorEvent(
                Instant.EPOCH, "java-spring-logback", "test-service", "ERROR",
                "formatted message",
                "test-instance", "1.0.0", "test-logger", "main",
                messageTemplate, exceptionClass, exceptionMessage, stacktrace);
    }

    @Test
    void shouldUseStacktraceBranchWhenStacktracePresent() {
        var event = event("java.lang.RuntimeException: boom\n\tat com.Foo.doIt(Foo.java:42)", null, null, null);
        var result = builder.build(event);

        assertThat(result.fingerprintSource()).isEqualTo(FingerprintSource.STACKTRACE);
        assertThat(result.fingerprintBase())
                .contains("test-service|test-logger|ERROR|")
                .doesNotContain("42");
    }

    @Test
    void shouldStripDigitsFromStacktrace() {
        var event = event("com.Foo.doIt(Foo.java:123)", null, null, null);
        var result = builder.build(event);

        assertThat(result.fingerprintSource()).isEqualTo(FingerprintSource.STACKTRACE);
        assertThat(result.fingerprintBase()).doesNotContain("123");
        assertThat(result.fingerprintBase()).contains("com.Foo.doIt(Foo.java:)");
    }

    @Test
    void identicalStacktraceExceptDigitsShouldProduceSameBase() {
        var e1 = event("at com.Foo.doIt(Foo.java:100)", null, null, null);
        var e2 = event("at com.Foo.doIt(Foo.java:999)", null, null, null);

        assertThat(builder.build(e1).fingerprintBase())
                .isEqualTo(builder.build(e2).fingerprintBase());
    }

    @Test
    void shouldUseExceptionBranchWhenStacktraceBlankAndExceptionPresent() {
        var event = event(null, "java.lang.NullPointerException", "npe message", null);
        var result = builder.build(event);

        assertThat(result.fingerprintSource()).isEqualTo(FingerprintSource.EXCEPTION);
        assertThat(result.fingerprintBase())
                .isEqualTo("test-service|test-logger|ERROR|java.lang.NullPointerException|npe message");
    }

    @Test
    void shouldUseMessageTemplateBranchWhenNoStacktraceAndNoExceptionButTemplatePresent() {
        var event = event(null, null, null, "User {} not found");
        var result = builder.build(event);

        assertThat(result.fingerprintSource()).isEqualTo(FingerprintSource.MESSAGE_TEMPLATE);
        assertThat(result.fingerprintBase())
                .isEqualTo("test-service|test-logger|ERROR|User {} not found");
    }

    @Test
    void shouldUseMinimalBranchWhenNothingElseAvailable() {
        var event = event(null, null, null, null);
        var result = builder.build(event);

        assertThat(result.fingerprintSource()).isEqualTo(FingerprintSource.MINIMAL);
        assertThat(result.fingerprintBase()).isEqualTo("test-service|test-logger|ERROR");
    }

    @Test
    void stacktraceBranchHasPriorityOverException() {
        // Даже если есть exceptionClass+message, stacktrace выигрывает
        var event = event("at com.Foo.doIt(Foo.java:1)", "java.lang.RuntimeException", "msg", null);
        var result = builder.build(event);

        assertThat(result.fingerprintSource()).isEqualTo(FingerprintSource.STACKTRACE);
    }

    @Test
    void exceptionBranchHasPriorityOverMessageTemplate() {
        var event = event(null, "java.lang.RuntimeException", "msg", "template {}");
        var result = builder.build(event);

        assertThat(result.fingerprintSource()).isEqualTo(FingerprintSource.EXCEPTION);
    }

    @Test
    void messageTemplateBranchHasPriorityOverMinimal() {
        var event = event(null, null, "msg", "template {}");
        // exceptionClass=null => EXCEPTION не сработает, идём дальше
        var result = builder.build(event);

        assertThat(result.fingerprintSource()).isEqualTo(FingerprintSource.MESSAGE_TEMPLATE);
    }

    @Test
    void digitsNotRemovedInExceptionBranch() {
        var event = event(null, "com.Class42", "error code 500", null);
        var result = builder.build(event);

        assertThat(result.fingerprintSource()).isEqualTo(FingerprintSource.EXCEPTION);
        assertThat(result.fingerprintBase()).contains("42").contains("500");
    }

    @Test
    void digitsNotRemovedInMessageTemplateBranch() {
        var event = event(null, null, null, "user 123 failed");
        var result = builder.build(event);

        assertThat(result.fingerprintSource()).isEqualTo(FingerprintSource.MESSAGE_TEMPLATE);
        assertThat(result.fingerprintBase()).contains("123");
    }

    @Test
    void nullLoggerBecomesEmptyString() {
        var event = new NormalizedErrorEvent(
                Instant.EPOCH, "t", "svc", "WARN", "msg",
                null, null, null, null,
                null, null, null, null);
        var result = builder.build(event);

        assertThat(result.fingerprintSource()).isEqualTo(FingerprintSource.MINIMAL);
        assertThat(result.fingerprintBase()).startsWith("svc||WARN");
    }
}

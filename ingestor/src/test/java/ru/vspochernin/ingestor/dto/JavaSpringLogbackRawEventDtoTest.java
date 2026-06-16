package ru.vspochernin.ingestor.dto;

import java.util.List;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class JavaSpringLogbackRawEventDtoTest {

    @Test
    void shouldReturnNullWhenThrowableIsNull() {
        var dto = new JavaSpringLogbackRawEventDto(
                null, null, null, null, null, null, null, null, null, null, 0L);

        assertThat(dto.getStacktraceFormatted()).isNull();
    }

    @Test
    void shouldReturnHeaderOnlyWhenNoStepArray() {
        var throwable = new JavaSpringLogbackRawEventDto.ThrowableDto(
                "java.lang.IllegalStateException", "bad state", null);
        var dto = new JavaSpringLogbackRawEventDto(
                null, null, null, null, null, null, null, null, null, throwable, 1000L);

        assertThat(dto.getStacktraceFormatted())
                .isEqualTo("java.lang.IllegalStateException: bad state");
    }

    @Test
    void shouldReturnHeaderOnlyWhenEmptyStepArray() {
        var throwable = new JavaSpringLogbackRawEventDto.ThrowableDto(
                "com.Example", "msg", List.of());
        var dto = new JavaSpringLogbackRawEventDto(
                null, null, null, null, null, null, null, null, null, throwable, 1000L);

        assertThat(dto.getStacktraceFormatted()).isEqualTo("com.Example: msg");
    }

    @Test
    void classNameWithoutMessageShouldNotHaveColon() {
        var throwable = new JavaSpringLogbackRawEventDto.ThrowableDto(
                "java.lang.RuntimeException", null, null);
        var dto = new JavaSpringLogbackRawEventDto(
                null, null, null, null, null, null, null, null, null, throwable, 1000L);

        assertThat(dto.getStacktraceFormatted()).isEqualTo("java.lang.RuntimeException");
    }

    @Test
    void classNameBlankAndMessagePresentShouldSkipHeader() {
        var throwable = new JavaSpringLogbackRawEventDto.ThrowableDto(
                "", "only message",
                List.of(new JavaSpringLogbackRawEventDto.ThrowableDto.StepDto(
                        "com.Example", "Example.java", 42, "run")));
        var dto = new JavaSpringLogbackRawEventDto(
                null, null, null, null, null, null, null, null, null, throwable, 1000L);

        assertThat(dto.getStacktraceFormatted())
                .contains("at com.Example.run(Example.java:42)");
    }

    @Test
    void shouldFormatFullStacktrace() {
        var throwable = new JavaSpringLogbackRawEventDto.ThrowableDto(
                "com.MainException", "main error",
                List.of(
                        new JavaSpringLogbackRawEventDto.ThrowableDto.StepDto(
                                "com.Foo", "Foo.java", 100, "doIt"),
                        new JavaSpringLogbackRawEventDto.ThrowableDto.StepDto(
                                "com.Bar", "Bar.java", 200, "call")));
        var dto = new JavaSpringLogbackRawEventDto(
                null, null, null, null, null, null, null, null, null, throwable, 1000L);

        String result = dto.getStacktraceFormatted();
        assertThat(result).startsWith("com.MainException: main error\n");
        assertThat(result).contains("\tat com.Foo.doIt(Foo.java:100)");
        assertThat(result).contains("\tat com.Bar.call(Bar.java:200)");
    }

    @Test
    void nullFileNameShouldBecomeUnknownSource() {
        var throwable = new JavaSpringLogbackRawEventDto.ThrowableDto(
                "com.X", "msg",
                List.of(new JavaSpringLogbackRawEventDto.ThrowableDto.StepDto(
                        "com.Example", null, 10, "run")));
        var dto = new JavaSpringLogbackRawEventDto(
                null, null, null, null, null, null, null, null, null, throwable, 1000L);

        assertThat(dto.getStacktraceFormatted()).contains("Unknown source");
    }

    @Test
    void stringNullFileNameShouldBecomeUnknownSource() {
        var throwable = new JavaSpringLogbackRawEventDto.ThrowableDto(
                "com.X", "msg",
                List.of(new JavaSpringLogbackRawEventDto.ThrowableDto.StepDto(
                        "com.Example", "null", 10, "run")));
        var dto = new JavaSpringLogbackRawEventDto(
                null, null, null, null, null, null, null, null, null, throwable, 1000L);

        assertThat(dto.getStacktraceFormatted()).contains("Unknown source");
    }

    @Test
    void blankFileNameShouldBecomeUnknownSource() {
        var throwable = new JavaSpringLogbackRawEventDto.ThrowableDto(
                "com.X", "msg",
                List.of(new JavaSpringLogbackRawEventDto.ThrowableDto.StepDto(
                        "com.Example", "   ", 10, "run")));
        var dto = new JavaSpringLogbackRawEventDto(
                null, null, null, null, null, null, null, null, null, throwable, 1000L);

        assertThat(dto.getStacktraceFormatted()).contains("Unknown source");
    }

    @Test
    void zeroLineNumberShouldOmitIt() {
        var throwable = new JavaSpringLogbackRawEventDto.ThrowableDto(
                "com.X", "msg",
                List.of(new JavaSpringLogbackRawEventDto.ThrowableDto.StepDto(
                        "com.Example", "Example.java", 0, "run")));
        var dto = new JavaSpringLogbackRawEventDto(
                null, null, null, null, null, null, null, null, null, throwable, 1000L);

        assertThat(dto.getStacktraceFormatted()).contains("(Example.java)");
        assertThat(dto.getStacktraceFormatted()).doesNotContain(":0");
    }

    @Test
    void negativeLineNumberShouldOmitIt() {
        var throwable = new JavaSpringLogbackRawEventDto.ThrowableDto(
                "com.X", "msg",
                List.of(new JavaSpringLogbackRawEventDto.ThrowableDto.StepDto(
                        "com.Example", "Example.java", -5, "run")));
        var dto = new JavaSpringLogbackRawEventDto(
                null, null, null, null, null, null, null, null, null, throwable, 1000L);

        assertThat(dto.getStacktraceFormatted()).contains("(Example.java)");
    }

    @Test
    void positiveLineNumberShouldAppear() {
        var throwable = new JavaSpringLogbackRawEventDto.ThrowableDto(
                "com.X", "msg",
                List.of(new JavaSpringLogbackRawEventDto.ThrowableDto.StepDto(
                        "com.Example", "Example.java", 42, "run")));
        var dto = new JavaSpringLogbackRawEventDto(
                null, null, null, null, null, null, null, null, null, throwable, 1000L);

        assertThat(dto.getStacktraceFormatted()).contains("Example.java:42");
    }

    @Test
    void nullStepClassNameShouldUseDefault() {
        var throwable = new JavaSpringLogbackRawEventDto.ThrowableDto(
                "com.X", "msg",
                List.of(new JavaSpringLogbackRawEventDto.ThrowableDto.StepDto(
                        null, "File.java", 10, "run")));
        var dto = new JavaSpringLogbackRawEventDto(
                null, null, null, null, null, null, null, null, null, throwable, 1000L);

        assertThat(dto.getStacktraceFormatted()).contains("Unknown className");
    }

    @Test
    void nullStepMethodNameShouldUseDefault() {
        var throwable = new JavaSpringLogbackRawEventDto.ThrowableDto(
                "com.X", "msg",
                List.of(new JavaSpringLogbackRawEventDto.ThrowableDto.StepDto(
                        "com.Example", "File.java", 10, null)));
        var dto = new JavaSpringLogbackRawEventDto(
                null, null, null, null, null, null, null, null, null, throwable, 1000L);

        assertThat(dto.getStacktraceFormatted()).contains("Unknown methodName");
    }

    @Test
    void emptyThrowableClassNameAndEmptyStepArrayShouldReturnNull() {
        var throwable = new JavaSpringLogbackRawEventDto.ThrowableDto(
                "", "msg", List.of());
        var dto = new JavaSpringLogbackRawEventDto(
                null, null, null, null, null, null, null, null, null, throwable, 1000L);

        assertThat(dto.getStacktraceFormatted()).isNull();
    }
}

package ru.vspochernin.ingestor.utils;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class StringUtilsTest {

    @Test
    void getOrDefaultShouldReturnValueWhenNotBlank() {
        assertThat(StringUtils.getOrDefault("hello", "default")).isEqualTo("hello");
    }

    @Test
    void getOrDefaultShouldReturnDefaultWhenNull() {
        assertThat(StringUtils.getOrDefault(null, "default")).isEqualTo("default");
    }

    @Test
    void getOrDefaultShouldReturnDefaultWhenEmpty() {
        assertThat(StringUtils.getOrDefault("", "default")).isEqualTo("default");
    }

    @Test
    void getOrDefaultShouldReturnDefaultWhenBlank() {
        assertThat(StringUtils.getOrDefault("   ", "default")).isEqualTo("default");
    }

    @Test
    void getFirstNonBlankOrDefaultShouldReturnFirstWhenNotBlank() {
        assertThat(StringUtils.getFirstNonBlankOrDefault("first", "second", "default"))
                .isEqualTo("first");
    }

    @Test
    void getFirstNonBlankOrDefaultShouldReturnSecondWhenFirstBlank() {
        assertThat(StringUtils.getFirstNonBlankOrDefault("", "second", "default"))
                .isEqualTo("second");
    }

    @Test
    void getFirstNonBlankOrDefaultShouldReturnSecondWhenFirstNull() {
        assertThat(StringUtils.getFirstNonBlankOrDefault(null, "second", "default"))
                .isEqualTo("second");
    }

    @Test
    void getFirstNonBlankOrDefaultShouldReturnDefaultWhenBothBlank() {
        assertThat(StringUtils.getFirstNonBlankOrDefault("", "   ", "default"))
                .isEqualTo("default");
    }

    @Test
    void getFirstNonBlankOrDefaultShouldReturnDefaultWhenBothNull() {
        assertThat(StringUtils.getFirstNonBlankOrDefault(null, null, "default"))
                .isEqualTo("default");
    }

    @Test
    void isNotBlankShouldReturnTrueForNonBlank() {
        assertThat(StringUtils.isNotBlank("a")).isTrue();
        assertThat(StringUtils.isNotBlank(" a ")).isTrue();
    }

    @Test
    void isNotBlankShouldReturnFalseForNull() {
        assertThat(StringUtils.isNotBlank(null)).isFalse();
    }

    @Test
    void isNotBlankShouldReturnFalseForEmpty() {
        assertThat(StringUtils.isNotBlank("")).isFalse();
    }

    @Test
    void isNotBlankShouldReturnFalseForBlank() {
        assertThat(StringUtils.isNotBlank("   ")).isFalse();
    }
}

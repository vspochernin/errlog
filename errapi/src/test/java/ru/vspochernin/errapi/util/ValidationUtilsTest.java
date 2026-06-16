package ru.vspochernin.errapi.util;

import org.junit.jupiter.api.Test;
import ru.vspochernin.errapi.exception.ErrapiException;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ValidationUtilsTest {

    @Test
    void shouldAcceptLimitInRange() {
        assertThatCode(() -> ValidationUtils.validateLimitOffset(1, 0))
                .doesNotThrowAnyException();
        assertThatCode(() -> ValidationUtils.validateLimitOffset(500, 0))
                .doesNotThrowAnyException();
        assertThatCode(() -> ValidationUtils.validateLimitOffset(250, 0))
                .doesNotThrowAnyException();
    }

    @Test
    void shouldThrowWhenLimitBelowMin() {
        assertThatThrownBy(() -> ValidationUtils.validateLimitOffset(0, 0))
                .isInstanceOf(ErrapiException.class)
                .hasMessageContaining("Limit must be");
        assertThatThrownBy(() -> ValidationUtils.validateLimitOffset(-1, 0))
                .isInstanceOf(ErrapiException.class);
    }

    @Test
    void shouldThrowWhenLimitAboveMax() {
        assertThatThrownBy(() -> ValidationUtils.validateLimitOffset(501, 0))
                .isInstanceOf(ErrapiException.class)
                .hasMessageContaining("Limit must be");
    }

    @Test
    void shouldAcceptZeroOffset() {
        assertThatCode(() -> ValidationUtils.validateLimitOffset(10, 0))
                .doesNotThrowAnyException();
    }

    @Test
    void shouldThrowForNegativeOffset() {
        assertThatThrownBy(() -> ValidationUtils.validateLimitOffset(10, -1))
                .isInstanceOf(ErrapiException.class)
                .hasMessageContaining("Offset can't be negative");
    }

    @Test
    void shouldAcceptValidUuid() {
        assertThatCode(() -> ValidationUtils.validateUuid("93305bb4-c952-4f65-8875-731da06e1077"))
                .doesNotThrowAnyException();
    }

    @Test
    void shouldThrowForInvalidUuid() {
        assertThatThrownBy(() -> ValidationUtils.validateUuid("not-a-uuid"))
                .isInstanceOf(ErrapiException.class)
                .hasMessageContaining("UUID");
    }
}

package ru.vspochernin.errapi.model.errors;

import java.util.Set;

import org.junit.jupiter.api.Test;
import ru.vspochernin.errapi.exception.ErrapiException;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FilterFieldTest {

    @Test
    void shouldAllowSupportedOperation() {
        var field = new FilterField("test", "test_col",
                Set.of(FilterOperation.EQ, FilterOperation.IN), "desc");

        assertThatCode(() -> field.checkOperationSupport(FilterOperation.EQ, 0))
                .doesNotThrowAnyException();
    }

    @Test
    void shouldThrowForUnsupportedOperation() {
        var field = new FilterField("test", "test_col",
                Set.of(FilterOperation.EQ), "desc");

        assertThatThrownBy(() -> field.checkOperationSupport(FilterOperation.LIKE, 0))
                .isInstanceOf(ErrapiException.class)
                .hasMessageContaining("unsupported");
    }
}

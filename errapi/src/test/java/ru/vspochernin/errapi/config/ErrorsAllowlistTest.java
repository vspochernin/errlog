package ru.vspochernin.errapi.config;

import org.junit.jupiter.api.Test;
import ru.vspochernin.errapi.exception.ErrapiException;
import ru.vspochernin.errapi.model.errors.FilterOperation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ErrorsAllowlistTest {

    @Test
    void shouldFindExistingField() {
        var field = ErrorsAllowlist.byName("service");
        assertThat(field.name()).isEqualTo("service");
        assertThat(field.column()).isEqualTo("service");
    }

    @Test
    void shouldThrowForUnknownField() {
        assertThatThrownBy(() -> ErrorsAllowlist.byName("nonexistent"))
                .isInstanceOf(ErrapiException.class)
                .hasMessageContaining("Unsupported field");
    }

    @Test
    void levelFieldShouldNotSupportLike() {
        var level = ErrorsAllowlist.byName("level");
        assertThat(level.operations()).doesNotContain(FilterOperation.LIKE);
    }
}

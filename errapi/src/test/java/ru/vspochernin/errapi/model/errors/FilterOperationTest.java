package ru.vspochernin.errapi.model.errors;

import org.junit.jupiter.api.Test;
import ru.vspochernin.errapi.exception.ErrapiException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FilterOperationTest {

    @Test
    void byNameShouldReturnCorrectOperation() {
        assertThat(FilterOperation.byName("eq")).isEqualTo(FilterOperation.EQ);
        assertThat(FilterOperation.byName("ne")).isEqualTo(FilterOperation.NE);
        assertThat(FilterOperation.byName("in")).isEqualTo(FilterOperation.IN);
        assertThat(FilterOperation.byName("like")).isEqualTo(FilterOperation.LIKE);
    }

    @Test
    void byNameShouldThrowForUnknown() {
        assertThatThrownBy(() -> FilterOperation.byName("gt"))
                .isInstanceOf(ErrapiException.class)
                .hasMessageContaining("Unknown operation");
    }

    @Test
    void inShouldAllowMultipleValues() {
        assertThat(FilterOperation.IN.isAllowsMultipleValues()).isTrue();
    }

    @Test
    void eqAndLikeShouldNotAllowMultipleValues() {
        assertThat(FilterOperation.EQ.isAllowsMultipleValues()).isFalse();
        assertThat(FilterOperation.NE.isAllowsMultipleValues()).isFalse();
        assertThat(FilterOperation.LIKE.isAllowsMultipleValues()).isFalse();
    }
}

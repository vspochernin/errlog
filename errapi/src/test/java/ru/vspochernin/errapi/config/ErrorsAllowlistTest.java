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
    void shouldMapCamelCaseNamesToSnakeCaseColumns() {
        // Нетривиальные маппинги: API-имя (camelCase) -> колонка ClickHouse (snake_case).
        assertThat(ErrorsAllowlist.byName("sourceType").column()).isEqualTo("source_type");
        assertThat(ErrorsAllowlist.byName("messageFormatted").column()).isEqualTo("message_formatted");
        assertThat(ErrorsAllowlist.byName("serviceVersion").column()).isEqualTo("service_version");
        assertThat(ErrorsAllowlist.byName("messageTemplate").column()).isEqualTo("message_template");
        assertThat(ErrorsAllowlist.byName("exceptionClass").column()).isEqualTo("exception_class");
        assertThat(ErrorsAllowlist.byName("exceptionMessage").column()).isEqualTo("exception_message");
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
        // При этом остальные операции level поддерживает
        assertThat(level.operations()).contains(FilterOperation.EQ, FilterOperation.NE, FilterOperation.IN);
    }
}

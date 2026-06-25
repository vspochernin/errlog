package ru.vspochernin.errapi.util;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import ru.vspochernin.errapi.dto.errors.ErrorsRequest;
import ru.vspochernin.errapi.exception.ErrapiException;
import ru.vspochernin.errapi.model.errors.FilterOperation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ErrorsFiltersParserTest {

    @Test
    void shouldReturnEmptyListForNull() {
        assertThat(ErrorsFiltersParser.parse(null)).isEmpty();
    }

    @Test
    void shouldReturnEmptyListForEmpty() {
        assertThat(ErrorsFiltersParser.parse(List.of())).isEmpty();
    }

    @Test
    void shouldParseSingleFilter() {
        var raw = List.of(new ErrorsRequest.Filter("service", "eq", List.of("my-svc")));

        var result = ErrorsFiltersParser.parse(raw);

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().field().name()).isEqualTo("service");
        assertThat(result.getFirst().operation()).isEqualTo(FilterOperation.EQ);
        assertThat(result.getFirst().values()).containsExactly("my-svc");
    }

    @Test
    void shouldParseMultipleFilters() {
        var raw = List.of(
                new ErrorsRequest.Filter("service", "eq", List.of("svc1")),
                new ErrorsRequest.Filter("level", "ne", List.of("INFO")));

        var result = ErrorsFiltersParser.parse(raw);

        assertThat(result).hasSize(2);
        // Проверяем содержимое обоих фильтров, а не только размер.
        assertThat(result.get(0).field().name()).isEqualTo("service");
        assertThat(result.get(0).operation()).isEqualTo(FilterOperation.EQ);
        assertThat(result.get(0).values()).containsExactly("svc1");
        assertThat(result.get(1).field().name()).isEqualTo("level");
        assertThat(result.get(1).operation()).isEqualTo(FilterOperation.NE);
        assertThat(result.get(1).values()).containsExactly("INFO");
    }

    @Test
    void shouldThrowWhenOperationIsBlank() {
        var raw = List.of(new ErrorsRequest.Filter("service", "   ", List.of("val")));
        assertThatThrownBy(() -> ErrorsFiltersParser.parse(raw))
                .isInstanceOf(ErrapiException.class)
                .hasMessageContaining("operation is null or blank");
    }

    @Test
    void shouldThrowWhenValuesIsNull() {
        var raw = List.of(new ErrorsRequest.Filter("service", "eq", null));
        assertThatThrownBy(() -> ErrorsFiltersParser.parse(raw))
                .isInstanceOf(ErrapiException.class)
                .hasMessageContaining("values is empty");
    }

    @Test
    void shouldThrowWhenFilterIsNull() {
        List<ErrorsRequest.Filter> raw = new ArrayList<>();
        raw.add(null);
        assertThatThrownBy(() -> ErrorsFiltersParser.parse(raw))
                .isInstanceOf(ErrapiException.class)
                .hasMessageContaining("is null");
    }

    @Test
    void shouldThrowWhenFieldIsBlank() {
        var raw = List.of(new ErrorsRequest.Filter("", "eq", List.of("val")));
        assertThatThrownBy(() -> ErrorsFiltersParser.parse(raw))
                .isInstanceOf(ErrapiException.class)
                .hasMessageContaining("field is blank");
    }

    @Test
    void shouldThrowWhenFieldIsUnknown() {
        var raw = List.of(new ErrorsRequest.Filter("nonexistent", "eq", List.of("val")));
        assertThatThrownBy(() -> ErrorsFiltersParser.parse(raw))
                .isInstanceOf(ErrapiException.class)
                .hasMessageContaining("Unsupported field");
    }

    @Test
    void shouldThrowWhenOperationIsUnknown() {
        var raw = List.of(new ErrorsRequest.Filter("service", "gt", List.of("val")));
        assertThatThrownBy(() -> ErrorsFiltersParser.parse(raw))
                .isInstanceOf(ErrapiException.class)
                .hasMessageContaining("Unknown operation");
    }

    @Test
    void shouldThrowWhenOperationNotSupportedForField() {
        // level не поддерживает LIKE
        var raw = List.of(new ErrorsRequest.Filter("level", "like", List.of("WARN")));
        assertThatThrownBy(() -> ErrorsFiltersParser.parse(raw))
                .isInstanceOf(ErrapiException.class)
                .hasMessageContaining("unsupported");
    }

    @Test
    void shouldThrowWhenValuesIsEmpty() {
        var raw = List.of(new ErrorsRequest.Filter("service", "eq", List.of()));
        assertThatThrownBy(() -> ErrorsFiltersParser.parse(raw))
                .isInstanceOf(ErrapiException.class)
                .hasMessageContaining("values is empty");
    }

    @Test
    void shouldThrowWhenOperationDoesNotAllowMultipleValues() {
        var raw = List.of(new ErrorsRequest.Filter("service", "eq", List.of("a", "b")));
        assertThatThrownBy(() -> ErrorsFiltersParser.parse(raw))
                .isInstanceOf(ErrapiException.class)
                .hasMessageContaining("must contain only one element");
    }

    @Test
    void inOperationShouldAllowMultipleValues() {
        var raw = List.of(new ErrorsRequest.Filter("service", "in", List.of("svc1", "svc2", "svc3")));
        var result = ErrorsFiltersParser.parse(raw);

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().values()).hasSize(3);
    }
}

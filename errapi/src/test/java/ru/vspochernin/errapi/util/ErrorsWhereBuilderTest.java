package ru.vspochernin.errapi.util;

import java.math.BigInteger;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.Test;
import ru.vspochernin.errapi.model.errors.ErrorsFilter;
import ru.vspochernin.errapi.model.errors.ErrorsQuery;
import ru.vspochernin.errapi.model.errors.FilterField;
import ru.vspochernin.errapi.model.errors.FilterOperation;
import ru.vspochernin.errapi.model.errors.TimeWindow;

import static org.assertj.core.api.Assertions.assertThat;

class ErrorsWhereBuilderTest {

    private static final TimeWindow WINDOW = new TimeWindow(
            Instant.parse("2026-01-01T00:00:00Z"),
            Instant.parse("2026-01-02T00:00:00Z"));

    @Test
    void shouldBuildBaseWhereWithTimestamp() {
        var query = new ErrorsQuery(WINDOW, Optional.empty(), List.of());
        var where = ErrorsWhereBuilder.buildWhere(query);

        assertThat(where.sql()).contains("timestamp >= :from AND timestamp < :to");
        assertThat(where.params().getValue("from")).isNotNull();
        assertThat(where.params().getValue("to")).isNotNull();
    }

    @Test
    void shouldAddFingerprintClause() {
        var query = new ErrorsQuery(WINDOW, Optional.of(BigInteger.valueOf(12345)), List.of());
        var where = ErrorsWhereBuilder.buildWhere(query);

        assertThat(where.sql()).contains("fingerprint = toUInt64(:fingerprint)");
        assertThat(where.params().getValue("fingerprint")).isEqualTo("12345");
    }

    @Test
    void shouldAddEqFilter() {
        var filter = new ErrorsFilter(
                new FilterField("service", "service", Set.of(FilterOperation.EQ), "d"),
                FilterOperation.EQ, List.of("my-svc"));
        var query = new ErrorsQuery(WINDOW, Optional.empty(), List.of(filter));
        var where = ErrorsWhereBuilder.buildWhere(query);

        assertThat(where.sql()).contains("service = :filter_0");
        assertThat(where.params().getValue("filter_0")).isEqualTo("my-svc");
    }

    @Test
    void shouldAddNeFilter() {
        var filter = new ErrorsFilter(
                new FilterField("level", "level", Set.of(FilterOperation.NE), "d"),
                FilterOperation.NE, List.of("INFO"));
        var query = new ErrorsQuery(WINDOW, Optional.empty(), List.of(filter));
        var where = ErrorsWhereBuilder.buildWhere(query);

        assertThat(where.sql()).contains("level != :filter_0");
    }

    @Test
    void shouldAddInFilter() {
        var filter = new ErrorsFilter(
                new FilterField("service", "service", Set.of(FilterOperation.IN), "d"),
                FilterOperation.IN, List.of("svc1", "svc2"));
        var query = new ErrorsQuery(WINDOW, Optional.empty(), List.of(filter));
        var where = ErrorsWhereBuilder.buildWhere(query);

        assertThat(where.sql()).contains("service IN (:filter_0)");
        // values передаются списком - JDBC сам развернёт
        @SuppressWarnings("unchecked")
        var values = (List<String>) where.params().getValue("filter_0");
        assertThat(values).containsExactly("svc1", "svc2");
    }

    @Test
    void shouldAddLikeFilter() {
        var filter = new ErrorsFilter(
                new FilterField("messageFormatted", "message_formatted", Set.of(FilterOperation.LIKE), "d"),
                FilterOperation.LIKE, List.of("%error%"));
        var query = new ErrorsQuery(WINDOW, Optional.empty(), List.of(filter));
        var where = ErrorsWhereBuilder.buildWhere(query);

        assertThat(where.sql()).contains("message_formatted LIKE :filter_0");
    }

    @Test
    void shouldUseColumnPrefix() {
        var filter = new ErrorsFilter(
                new FilterField("service", "service", Set.of(FilterOperation.EQ), "d"),
                FilterOperation.EQ, List.of("svc1"));
        var query = new ErrorsQuery(WINDOW, Optional.empty(), List.of(filter));
        var where = ErrorsWhereBuilder.buildWhere(query, "src.");

        // Префикс применяется и к timestamp, и к колонкам фильтров.
        assertThat(where.sql()).contains("src.timestamp >= :from AND src.timestamp < :to");
        assertThat(where.sql()).contains("src.service = :filter_0");
    }

    @Test
    void shouldHaveIncrementingParamNamesForMultipleFilters() {
        var filter1 = new ErrorsFilter(
                new FilterField("service", "service", Set.of(FilterOperation.EQ), "d"),
                FilterOperation.EQ, List.of("svc1"));
        var filter2 = new ErrorsFilter(
                new FilterField("level", "level", Set.of(FilterOperation.EQ), "d"),
                FilterOperation.EQ, List.of("ERROR"));
        var query = new ErrorsQuery(WINDOW, Optional.empty(), List.of(filter1, filter2));
        var where = ErrorsWhereBuilder.buildWhere(query);

        // Точные проверки имён параметров (contains матчил бы :filter_00 и т.п.).
        assertThat(where.sql()).contains("service = :filter_0").contains("level = :filter_1");
        assertThat(where.params().getValue("filter_0")).isEqualTo("svc1");
        assertThat(where.params().getValue("filter_1")).isEqualTo("ERROR");
    }
}

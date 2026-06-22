package ru.vspochernin.errapi.model.errors;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;
import ru.vspochernin.errapi.dto.errors.ErrorsRequest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

class ErrorsQueryTest {

    @Test
    void shouldParseFullRequest() {
        var request = new ErrorsRequest(
                Instant.parse("2026-01-01T00:00:00Z"),
                Instant.parse("2026-01-02T00:00:00Z"),
                "123456",
                List.of(new ErrorsRequest.Filter("service", "eq", List.of("svc1"))));

        var query = ErrorsQuery.parseFromErrorsRequest(request);

        assertThat(query.timeWindow().from()).isEqualTo(Instant.parse("2026-01-01T00:00:00Z"));
        assertThat(query.timeWindow().to()).isEqualTo(Instant.parse("2026-01-02T00:00:00Z"));
        // Проверяем значение fingerprint, а не только наличие.
        assertThat(query.fingerprintO()).hasValue(new java.math.BigInteger("123456"));
        assertThat(query.filters()).hasSize(1);
        assertThat(query.filters().getFirst().field().name()).isEqualTo("service");
        assertThat(query.filters().getFirst().operation()).isEqualTo(FilterOperation.EQ);
        assertThat(query.filters().getFirst().values()).containsExactly("svc1");
    }

    @Test
    void shouldParseEmptyRequest() {
        var request = ErrorsRequest.empty();
        var query = ErrorsQuery.parseFromErrorsRequest(request);

        assertThat(query.timeWindow().to()).isCloseTo(Instant.now(), within(Duration.ofSeconds(5)));
        assertThat(query.timeWindow().from())
                .isCloseTo(query.timeWindow().to().minus(Duration.ofHours(24)),
                        within(Duration.ofSeconds(5)));
        assertThat(query.fingerprintO()).isEmpty();
        assertThat(query.filters()).isEmpty();
    }
}

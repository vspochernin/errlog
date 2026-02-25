package ru.vspochernin.errapi.dto.errors;

import java.time.Instant;
import java.util.List;

public record ErrorsEventsRequest(
        Instant from,
        Instant to,
        String fingerprint,
        List<ErrorsFilterRequest> filters)
{
    public static ErrorsEventsRequest empty() {
        return new ErrorsEventsRequest(null, null, null, List.of());
    }
}

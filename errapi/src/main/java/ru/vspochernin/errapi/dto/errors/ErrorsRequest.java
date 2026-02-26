package ru.vspochernin.errapi.dto.errors;

import java.time.Instant;
import java.util.List;

public record ErrorsRequest(
        Instant from,
        Instant to,
        String fingerprint,
        List<Filter> filters)
{

    public static ErrorsRequest empty() {
        return new ErrorsRequest(null, null, null, List.of());
    }

    public record Filter(
            String field,
            String operation,
            List<String> values)
    {
    }
}

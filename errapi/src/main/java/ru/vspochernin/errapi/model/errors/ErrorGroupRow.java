package ru.vspochernin.errapi.model.errors;

import java.time.Instant;

public record ErrorGroupRow(
        String fingerprint,
        long count,
        Instant lastSeen,
        ErrorEventRow lastEvent
)
{
}

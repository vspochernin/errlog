package ru.vspochernin.errapi.model.errors;

import java.time.Instant;

public record ErrorGroupRow(
        String groupFingerprint,
        long groupCount,
        Instant groupLastSeen,
        ErrorEventRow lastEvent)
{
}

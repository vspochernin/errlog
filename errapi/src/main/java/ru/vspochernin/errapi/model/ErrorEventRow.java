package ru.vspochernin.errapi.model;

import java.time.Instant;

public record ErrorEventRow(
        String eventId,
        Instant timestamp,
        String sourceType,
        String service,
        String level,
        String messageFormatted,
        String fingerprint,
        String fingerprintSource,
        String instance,
        String serviceVersion,
        String logger,
        String thread,
        String messageTemplate,
        String exceptionClass,
        String exceptionMessage,
        String stacktrace)
{
}

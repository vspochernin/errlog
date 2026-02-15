package ru.vspochernin.ingestor.model;

import java.time.Instant;
import java.util.UUID;

public record ErrorEvent(

        UUID eventId,
        Instant timestamp,
        String sourceType,
        String service,
        String level,
        String messageFormatted,
        long fingerprint,
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

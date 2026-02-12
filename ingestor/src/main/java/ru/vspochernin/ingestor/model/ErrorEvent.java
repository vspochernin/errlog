package ru.vspochernin.ingestor.model;

import java.time.Instant;
import java.util.UUID;

public record ErrorEvent(
        UUID eventId,
        Instant timestamp,

        String service,
        String level,
        String message_formatted,
        long fingerprint,
        String fingerprintSource,
        String sourceType,

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

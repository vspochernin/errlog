package ru.vspochernin.ingestor.model;

import java.time.Instant;

public record NormalizedErrorEvent(

        Instant timestamp,
        String sourceType,
        String service,
        String level,
        String messageFormatted,

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

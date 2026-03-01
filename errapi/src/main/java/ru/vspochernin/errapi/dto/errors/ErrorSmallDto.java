package ru.vspochernin.errapi.dto.errors;

import java.time.Instant;

import ru.vspochernin.errapi.model.errors.ErrorEventRow;

public record ErrorSmallDto(
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
        String exceptionMessage)
{

    public static ErrorSmallDto fromRow(ErrorEventRow row) {
        return new ErrorSmallDto(
                row.eventId(),
                row.timestamp(),
                row.sourceType(),
                row.service(),
                row.level(),
                row.messageFormatted(),
                row.fingerprint(),
                row.fingerprintSource(),
                row.instance(),
                row.serviceVersion(),
                row.logger(),
                row.thread(),
                row.messageTemplate(),
                row.exceptionClass(),
                row.exceptionMessage());
    }
}

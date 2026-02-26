package ru.vspochernin.errapi.dto.errors;

import java.time.Instant;
import java.util.List;

import ru.vspochernin.errapi.model.errors.ErrorEventRow;

public record ErrorsEventsResponse(
        List<Item> items,
        long eventsTotal)
{

    public record Item(
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
        public static Item fromRow(ErrorEventRow r) {
            return new Item(
                    r.eventId(),
                    r.timestamp(),
                    r.sourceType(),
                    r.service(),
                    r.level(),
                    r.messageFormatted(),
                    r.fingerprint(),
                    r.fingerprintSource(),
                    r.instance(),
                    r.serviceVersion(),
                    r.logger(),
                    r.thread(),
                    r.messageTemplate(),
                    r.exceptionClass(),
                    r.exceptionMessage());
        }
    }
}

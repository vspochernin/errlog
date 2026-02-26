package ru.vspochernin.errapi.util;

import java.time.Duration;
import java.time.Instant;

import ru.vspochernin.errapi.exception.ErrapiErrorType;
import ru.vspochernin.errapi.exception.ErrapiException;
import ru.vspochernin.errapi.model.errors.ErrorsTimeWindow;

public class ErrorsTimeWindowParser {

    private static final Duration DEFAULT_TIME_WINDOW = Duration.ofHours(24);

    private ErrorsTimeWindowParser() {
    }

    public static ErrorsTimeWindow parse(Instant from, Instant to) {
        Instant resolvedTo = (to == null) ? Instant.now() : to;
        Instant resolvedFrom = (from == null) ? resolvedTo.minus(DEFAULT_TIME_WINDOW) : from;

        if (resolvedFrom.isAfter(resolvedTo)) {
            throw new ErrapiException(ErrapiErrorType.INCORRECT_TIME_BORDERS, "from must be <= to");
        }

        return new ErrorsTimeWindow(resolvedFrom, resolvedTo);
    }

    public static ErrorsTimeWindow parse(String fromRaw, String toRaw) {
        Instant to = (toRaw == null || toRaw.isBlank()) ? null : parseInstant(toRaw, "to");
        Instant from = (fromRaw == null || fromRaw.isBlank()) ? null : parseInstant(fromRaw, "from");
        return parse(from, to);
    }

    private static Instant parseInstant(String raw, String fieldName) {
        try {
            return Instant.parse(raw);
        } catch (Exception e) {
            throw new ErrapiException(
                    ErrapiErrorType.INCORRECT_TIME_BORDERS,
                    fieldName + ": " + raw + " must be ISO-8601 UTC");
        }
    }
}

package ru.vspochernin.errapi.util;

import java.time.Duration;
import java.time.Instant;

import ru.vspochernin.errapi.exception.ErrapiErrorType;
import ru.vspochernin.errapi.exception.ErrapiException;
import ru.vspochernin.errapi.model.errors.ErrorsQuery;

public class ErrorsQueryParser {

    private static final Duration DEFAULT_WINDOW = Duration.ofHours(24);

    private ErrorsQueryParser() {
    }

    public static ErrorsQuery parse(String fromRaw, String toRaw) {
        Instant to = (toRaw == null || toRaw.isBlank()) ? Instant.now() : parseInstant(toRaw);
        Instant from = (fromRaw == null || fromRaw.isBlank()) ? to.minus(DEFAULT_WINDOW) : parseInstant(fromRaw);

        if (from.isAfter(to)) {
            throw new ErrapiException(ErrapiErrorType.BAD_REQUEST, "from must be <= to");
        }
        return new ErrorsQuery(from, to);
    }

    private static Instant parseInstant(String raw) {
        try {
            return Instant.parse(raw);
        } catch (Exception e) {
            throw new ErrapiException(ErrapiErrorType.BAD_REQUEST, "Invalid raw instant" + raw);
        }
    }
}

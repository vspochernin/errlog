package ru.vspochernin.errapi.util;

import java.util.UUID;

import ru.vspochernin.errapi.exception.ErrapiErrorType;
import ru.vspochernin.errapi.exception.ErrapiException;

public class ValidationUtils {

    private static final int LIMIT_MIN = 1;
    private static final int LIMIT_MAX = 500;

    private ValidationUtils() {
    }

    public static void validateLimitOffset(int limit, long offset) {
        if (limit < LIMIT_MIN || limit > LIMIT_MAX) {
            throw new ErrapiException(
                    ErrapiErrorType.BAD_REQUEST,
                    "Limit must be from " + LIMIT_MIN + " to " + LIMIT_MAX);
        }
        if (offset < 0L) {
            throw new ErrapiException(ErrapiErrorType.BAD_REQUEST, "Offset can't be negative");
        }
    }

    public static void validateUuid(String uuid) {
        try {
            UUID.fromString(uuid);
        } catch (Exception e) {
            throw new ErrapiException(ErrapiErrorType.BAD_REQUEST, "eventId must be UUID");
        }
    }
}

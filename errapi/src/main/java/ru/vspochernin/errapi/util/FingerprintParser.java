package ru.vspochernin.errapi.util;

import java.math.BigInteger;
import java.util.Optional;

import ru.vspochernin.errapi.exception.ErrapiErrorType;
import ru.vspochernin.errapi.exception.ErrapiException;

public class FingerprintParser {

    // 2^64 - 1.
    private static final BigInteger MAX_UINT64 = BigInteger.ONE.shiftLeft(64).subtract(BigInteger.ONE);

    private FingerprintParser() {
    }

    public static Optional<BigInteger> parseOptional(String raw) {
        if (raw == null || raw.isBlank()) {
            return Optional.empty();
        }
        if (!raw.chars().allMatch(Character::isDigit)) {
            throw new ErrapiException(ErrapiErrorType.BAD_REQUEST, "fingerprint должен быть UInt64 строкой");
        }

        BigInteger value;
        try {
            value = new BigInteger(raw);
        } catch (Exception e) {
            throw new ErrapiException(ErrapiErrorType.BAD_REQUEST, "fingerprint должен быть UInt64 строкой");
        }

        if (value.signum() < 0 || value.compareTo(MAX_UINT64) > 0) {
            throw new ErrapiException(ErrapiErrorType.BAD_REQUEST, "fingerprint вне диапазона UInt64");
        }

        return Optional.of(value);
    }
}

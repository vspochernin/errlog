package ru.vspochernin.ingestor.fingerprint;

import ru.vspochernin.ingestor.model.NormalizedErrorEvent;

public interface FingerprintCalculator {

    FingerprintResult calculate(NormalizedErrorEvent normalizedErrorEvent);
}

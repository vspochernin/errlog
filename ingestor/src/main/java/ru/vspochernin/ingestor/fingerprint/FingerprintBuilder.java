package ru.vspochernin.ingestor.fingerprint;

import ru.vspochernin.ingestor.model.NormalizedErrorEvent;

public interface FingerprintBuilder {

    FingerprintResult build(NormalizedErrorEvent normalizedErrorEvent);
}

package ru.vspochernin.ingestor.model;

import java.util.UUID;

import ru.vspochernin.ingestor.fingerprint.FingerprintResult;

public record ErrorEvent(

        UUID eventId,
        NormalizedErrorEvent normalizedErrorEvent,
        FingerprintResult fingerprintResult)
{
}

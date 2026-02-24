package ru.vspochernin.errapi.model.errors;

import java.time.Instant;

public record ErrorsQuery(
        // TODO: может добавить fingerprint и список f.
        Instant from,
        Instant to)
{
}

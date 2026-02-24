package ru.vspochernin.errapi.model.errors;

import java.time.Instant;

public record ErrorsQuery(
        Instant from,
        Instant to) // TODO: Добавить fingerprint и список f.
{
}

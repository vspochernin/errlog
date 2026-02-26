package ru.vspochernin.errapi.model.errors;

import java.time.Instant;

public record ErrorsTimeWindow(
        Instant from,
        Instant to)
{
}

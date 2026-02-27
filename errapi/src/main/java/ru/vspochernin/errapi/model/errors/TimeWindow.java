package ru.vspochernin.errapi.model.errors;

import java.time.Instant;

public record TimeWindow(
        Instant from,
        Instant to)
{
}

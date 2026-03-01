package ru.vspochernin.errapi.model.errors;

import java.time.Instant;

public record ErrorTimeseriesRow(
        Instant bucketStart,
        long count)
{
}

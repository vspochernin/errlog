package ru.vspochernin.errapi.dto.errors;

import java.time.Instant;
import java.util.List;

import ru.vspochernin.errapi.model.errors.ErrorTimeseriesRow;

public record ErrorsTimeseriesResponse(
        List<Item> items,
        String bucketSize)
{

    public record Item(
            Instant bucketStart,
            long count)
    {

        public static Item fromRow(ErrorTimeseriesRow row) {
            return new Item(row.bucketStart(), row.count());
        }
    }
}

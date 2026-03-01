package ru.vspochernin.errapi.dto.errors;

import java.time.Instant;
import java.util.List;

import ru.vspochernin.errapi.model.errors.ErrorGroupRow;

public record ErrorsGroupsResponse(
        List<Item> items,
        long eventsTotal,
        long groupsTotal)
{

    public record Item(
            String groupFingerprint,
            long groupCount,
            Instant groupLastSeen,
            ErrorSmallDto lastEvent)
    {

        public static Item fromRow(ErrorGroupRow row) {
            return new Item(
                    row.groupFingerprint(),
                    row.groupCount(),
                    row.groupLastSeen(),
                    ErrorSmallDto.fromRow(row.lastEvent()));
        }
    }
}

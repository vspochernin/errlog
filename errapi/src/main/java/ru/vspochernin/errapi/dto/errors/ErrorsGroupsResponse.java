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
            String fingerprint,
            long count,
            Instant lastSeen,
            ErrorsEventsResponse.Item lastItem)
    {

        public static Item fromRow(ErrorGroupRow row) {
            return new Item(
                    row.fingerprint(),
                    row.count(),
                    row.lastSeen(),
                    ErrorsEventsResponse.Item.fromRow(row.lastEvent()));
        }
    }
}

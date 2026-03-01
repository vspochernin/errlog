package ru.vspochernin.errapi.dto.errors;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonUnwrapped;
import ru.vspochernin.errapi.model.errors.ErrorEventRow;

public record ErrorsEventsResponse(
        List<Item> items,
        long eventsTotal)
{

    public record Item(
            @JsonUnwrapped ErrorSmallDto dto)
    {
        public static Item fromRow(ErrorEventRow row) {
            return new Item(ErrorSmallDto.fromRow(row));
        }
    }
}

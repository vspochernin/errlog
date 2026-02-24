package ru.vspochernin.errapi.dto.errors;

import java.util.List;

public record ErrorsEventsResponse(
        List<ErrorsEventsResponseItemDto> items,
        long eventsTotal)
{
}

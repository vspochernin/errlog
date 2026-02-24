package ru.vspochernin.errapi.dto.errors;

import java.util.List;

public record ErrorsFiltersResponse(
        List<FilterFieldDto> fields)
{
}

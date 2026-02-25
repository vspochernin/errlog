package ru.vspochernin.errapi.dto.errors;

import java.util.List;

public record ErrorsFilterRequest(
        String field,
        String operation,
        List<String> values)
{
}

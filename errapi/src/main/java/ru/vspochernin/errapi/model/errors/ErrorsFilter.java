package ru.vspochernin.errapi.model.errors;

import java.util.List;

public record ErrorsFilter(
        FilterField field,
        FilterOperation operation,
        List<String> values)
{
}

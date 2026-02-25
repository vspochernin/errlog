package ru.vspochernin.errapi.model.errors;

import java.util.List;

public record ErrorsFilterCondition(
        FilterField field,
        FilterOp operation,
        List<String> values)
{
}

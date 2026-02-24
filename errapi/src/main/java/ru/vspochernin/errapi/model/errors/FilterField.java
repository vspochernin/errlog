package ru.vspochernin.errapi.model.errors;

import java.util.List;

import ru.vspochernin.errapi.dto.errors.FilterFieldDto;

public record FilterField(
        String name, // Имя для передачи в API.
        String column, // Название колонки в ClickHouse.
        List<FilterOp> ops,
        String description)
{
    public FilterFieldDto toDto()
    {
        List<String> opNames = ops.stream()
                .map(FilterOp::getName)
                .toList();
        return new FilterFieldDto(name, opNames, description);
    }
}

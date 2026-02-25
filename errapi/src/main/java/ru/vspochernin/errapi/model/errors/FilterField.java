package ru.vspochernin.errapi.model.errors;

import java.util.List;
import java.util.Set;

import ru.vspochernin.errapi.dto.errors.FilterFieldDto;

public record FilterField(
        String name, // Имя для передачи в API.
        String column, // Название соответствующей колонки в ClickHouse.
        Set<FilterOp> operations,
        String description)
{
    public FilterFieldDto toDto()
    {
        List<String> opNames = operations.stream()
                .map(FilterOp::getName)
                .toList();
        return new FilterFieldDto(name, opNames, description);
    }
}

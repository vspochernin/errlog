package ru.vspochernin.errapi.model.errors;

import java.util.Set;

public record FilterField(
        String name, // Имя фильтра для передачи в API.
        String column, // Название соответствующей колонки в ClickHouse.
        Set<FilterOp> operations,  // Допустимые для фильтра операции.
        String description)
{
}

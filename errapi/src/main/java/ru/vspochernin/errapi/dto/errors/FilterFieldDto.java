package ru.vspochernin.errapi.dto.errors;

import java.util.List;

public record FilterFieldDto(
        String name,
        List<String> ops, // Допустимые операции для фильтра.
        String description)
{
}

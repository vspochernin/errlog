package ru.vspochernin.errapi.model.errors;

import java.util.Set;

import ru.vspochernin.errapi.exception.ErrapiErrorType;
import ru.vspochernin.errapi.exception.ErrapiException;

public record FilterField(
        String name, // Имя фильтра для передачи в API.
        String column, // Название соответствующей колонки в ClickHouse.
        Set<FilterOperation> operations,  // Допустимые для фильтра операции.
        String description)
{

    public void checkOperationSupport(FilterOperation operation, int i) {
        if (!operations.contains(operation)) {
            throw new ErrapiException(
                    ErrapiErrorType.BAD_REQUEST,
                    "filters[" + i + "].operation " + operation.getName() + " unsupported for field " + name);
        }
    }
}

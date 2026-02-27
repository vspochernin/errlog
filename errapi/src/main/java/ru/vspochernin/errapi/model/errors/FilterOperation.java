package ru.vspochernin.errapi.model.errors;

import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import ru.vspochernin.errapi.exception.ErrapiErrorType;
import ru.vspochernin.errapi.exception.ErrapiException;

@Getter
@RequiredArgsConstructor
public enum FilterOperation {

    EQ("eq", false),
    NE("ne", false),
    IN("in", true),
    LIKE("like", false),
    ;

    private final String name; // Имя операции для передачи в API.
    private final boolean allowsMultipleValues;

    public static FilterOperation byName(String name) {
        return Optional.ofNullable(BY_NAME.get(name))
                .orElseThrow(() -> new ErrapiException(ErrapiErrorType.BAD_REQUEST, "Unknown operation: " + name));
    }

    private static final Map<String, FilterOperation> BY_NAME = Arrays.stream(FilterOperation.values())
            .collect(Collectors.toMap(FilterOperation::getName, Function.identity()));
}

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
public enum FilterOp {

    EQ("eq"),
    NE("ne"),
    IN("in"),
    LIKE("like"),
    ;

    private final String name; // Имя для передачи в API.

    public static FilterOp byName(String name) {
        return Optional.ofNullable(BY_NAME.get(name))
                .orElseThrow(() -> new ErrapiException(ErrapiErrorType.BAD_REQUEST, "Unknown operation: " + name));
    }

    private static final Map<String, FilterOp> BY_NAME = Arrays.stream(FilterOp.values())
            .collect(Collectors.toMap(FilterOp::getName, Function.identity()));
}

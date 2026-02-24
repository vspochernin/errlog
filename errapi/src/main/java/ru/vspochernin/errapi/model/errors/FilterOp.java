package ru.vspochernin.errapi.model.errors;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum FilterOp {

    EQ("eq"),
    NE("ne"),
    IN("in"),
    LIKE("like"),
    ;

    private final String name; // Имя для передачи в API.
}

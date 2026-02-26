package ru.vspochernin.errapi.config;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import ru.vspochernin.errapi.exception.ErrapiErrorType;
import ru.vspochernin.errapi.exception.ErrapiException;
import ru.vspochernin.errapi.model.errors.FilterField;
import ru.vspochernin.errapi.model.errors.FilterOp;

public class ErrorsAllowlist {

    private ErrorsAllowlist() {
    }

    public static final List<FilterField> FIELDS = List.of(
            new FilterField(
                    "sourceType",
                    "source_type",
                    Set.of(FilterOp.EQ, FilterOp.NE, FilterOp.IN, FilterOp.LIKE),
                    "Тип источника события"),

            new FilterField(
                    "service",
                    "service",
                    Set.of(FilterOp.EQ, FilterOp.NE, FilterOp.IN, FilterOp.LIKE),
                    "Название сервиса"),

            new FilterField(
                    "level",
                    "level",
                    Set.of(FilterOp.EQ, FilterOp.NE, FilterOp.IN),
                    "Уровень логирования"),

            new FilterField(
                    "messageFormatted",
                    "message_formatted",
                    Set.of(FilterOp.EQ, FilterOp.NE, FilterOp.IN, FilterOp.LIKE),
                    "Отформатированное сообщение (со вставленным текстом)"),

            new FilterField(
                    "instance",
                    "instance",
                    Set.of(FilterOp.EQ, FilterOp.NE, FilterOp.IN, FilterOp.LIKE),
                    "Название инстанса сервиса"),

            new FilterField(
                    "serviceVersion",
                    "service_version",
                    Set.of(FilterOp.EQ, FilterOp.NE, FilterOp.IN, FilterOp.LIKE),
                    "Версия сервиса"),

            new FilterField(
                    "logger",
                    "logger",
                    Set.of(FilterOp.EQ, FilterOp.NE, FilterOp.IN, FilterOp.LIKE),
                    "Название логгера"),

            new FilterField(
                    "thread",
                    "thread",
                    Set.of(FilterOp.EQ, FilterOp.NE, FilterOp.IN, FilterOp.LIKE),
                    "Название потока"),

            new FilterField(
                    "messageTemplate",
                    "message_template",
                    Set.of(FilterOp.EQ, FilterOp.NE, FilterOp.IN, FilterOp.LIKE),
                    "Шаблон сообщения (без вставленного текста)"),

            new FilterField(
                    "exceptionClass",
                    "exception_class",
                    Set.of(FilterOp.EQ, FilterOp.NE, FilterOp.IN, FilterOp.LIKE),
                    "Класс исключения"),

            new FilterField(
                    "exceptionMessage",
                    "exception_message",
                    Set.of(FilterOp.EQ, FilterOp.NE, FilterOp.IN, FilterOp.LIKE),
                    "Сообщение исключения"));

    public static FilterField byName(String name) {
        return Optional.ofNullable(BY_NAME.get(name))
                .orElseThrow(() -> new ErrapiException(ErrapiErrorType.BAD_REQUEST, "Unsupported field: " + name));
    }

    private static final Map<String, FilterField> BY_NAME = FIELDS.stream()
            .collect(Collectors.toUnmodifiableMap(FilterField::name, Function.identity()));
}

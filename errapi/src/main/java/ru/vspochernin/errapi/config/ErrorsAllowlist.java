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
import ru.vspochernin.errapi.model.errors.FilterOperation;

public class ErrorsAllowlist {

    private ErrorsAllowlist() {
    }

    public static final List<FilterField> FIELDS = List.of(
            new FilterField(
                    "sourceType",
                    "source_type",
                    Set.of(FilterOperation.EQ, FilterOperation.NE, FilterOperation.IN, FilterOperation.LIKE),
                    "Тип источника события"),

            new FilterField(
                    "service",
                    "service",
                    Set.of(FilterOperation.EQ, FilterOperation.NE, FilterOperation.IN, FilterOperation.LIKE),
                    "Название сервиса"),

            new FilterField(
                    "level",
                    "level",
                    Set.of(FilterOperation.EQ, FilterOperation.NE, FilterOperation.IN),
                    "Уровень логирования"),

            new FilterField(
                    "messageFormatted",
                    "message_formatted",
                    Set.of(FilterOperation.EQ, FilterOperation.NE, FilterOperation.IN, FilterOperation.LIKE),
                    "Отформатированное сообщение (со вставленным текстом)"),

            new FilterField(
                    "instance",
                    "instance",
                    Set.of(FilterOperation.EQ, FilterOperation.NE, FilterOperation.IN, FilterOperation.LIKE),
                    "Название инстанса сервиса"),

            new FilterField(
                    "serviceVersion",
                    "service_version",
                    Set.of(FilterOperation.EQ, FilterOperation.NE, FilterOperation.IN, FilterOperation.LIKE),
                    "Версия сервиса"),

            new FilterField(
                    "logger",
                    "logger",
                    Set.of(FilterOperation.EQ, FilterOperation.NE, FilterOperation.IN, FilterOperation.LIKE),
                    "Название логгера"),

            new FilterField(
                    "thread",
                    "thread",
                    Set.of(FilterOperation.EQ, FilterOperation.NE, FilterOperation.IN, FilterOperation.LIKE),
                    "Название потока"),

            new FilterField(
                    "messageTemplate",
                    "message_template",
                    Set.of(FilterOperation.EQ, FilterOperation.NE, FilterOperation.IN, FilterOperation.LIKE),
                    "Шаблон сообщения (без вставленного текста)"),

            new FilterField(
                    "exceptionClass",
                    "exception_class",
                    Set.of(FilterOperation.EQ, FilterOperation.NE, FilterOperation.IN, FilterOperation.LIKE),
                    "Класс исключения"),

            new FilterField(
                    "exceptionMessage",
                    "exception_message",
                    Set.of(FilterOperation.EQ, FilterOperation.NE, FilterOperation.IN, FilterOperation.LIKE),
                    "Сообщение исключения"));

    public static FilterField byName(String name) {
        return Optional.ofNullable(BY_NAME.get(name))
                .orElseThrow(() -> new ErrapiException(ErrapiErrorType.BAD_REQUEST, "Unsupported field: " + name));
    }

    private static final Map<String, FilterField> BY_NAME = FIELDS.stream()
            .collect(Collectors.toMap(FilterField::name, Function.identity()));
}

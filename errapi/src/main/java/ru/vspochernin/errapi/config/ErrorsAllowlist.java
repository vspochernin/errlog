package ru.vspochernin.errapi.config;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import ru.vspochernin.errapi.model.errors.FilterField;
import ru.vspochernin.errapi.model.errors.FilterOp;

public class ErrorsAllowlist {

    private ErrorsAllowlist() {
    }

    public static final List<FilterField> FIELDS = List.of(
            new FilterField(
                    "sourceType",
                    "source_type",
                    List.of(FilterOp.EQ, FilterOp.NE, FilterOp.IN, FilterOp.LIKE),
                    "Тип источника события"),

            new FilterField(
                    "service",
                    "service",
                    List.of(FilterOp.EQ, FilterOp.NE, FilterOp.IN, FilterOp.LIKE),
                    "Название сервиса"),

            new FilterField("level",
                    "level",
                    List.of(FilterOp.EQ, FilterOp.NE, FilterOp.IN),
                    "Уровень логирования"),

            new FilterField(
                    "messageFormatted",
                    "message_formatted",
                    List.of(FilterOp.EQ, FilterOp.NE, FilterOp.IN, FilterOp.LIKE),
                    "Отформатированное сообщение (со вставленным текстом)"),

            new FilterField(
                    "instance",
                    "instance",
                    List.of(FilterOp.EQ, FilterOp.NE, FilterOp.IN, FilterOp.LIKE),
                    "Название инстанса сервиса"),

            new FilterField(
                    "serviceVersion",
                    "service_version",
                    List.of(FilterOp.EQ, FilterOp.NE, FilterOp.IN, FilterOp.LIKE),
                    "Версия сервиса"),

            new FilterField(
                    "logger",
                    "logger",
                    List.of(FilterOp.EQ, FilterOp.NE, FilterOp.IN, FilterOp.LIKE),
                    "Название логгера"),

            new FilterField(
                    "thread",
                    "thread",
                    List.of(FilterOp.EQ, FilterOp.NE, FilterOp.IN, FilterOp.LIKE),
                    "Название потока"),

            new FilterField(
                    "messageTemplate",
                    "message_template",
                    List.of(FilterOp.EQ, FilterOp.NE, FilterOp.IN, FilterOp.LIKE),
                    "Шаблон сообщения (без вставленного текста)"),

            new FilterField(
                    "exceptionClass",
                    "exception_class",
                    List.of(FilterOp.EQ, FilterOp.NE, FilterOp.IN, FilterOp.LIKE),
                    "Класс исключения"),

            new FilterField(
                    "exceptionMessage",
                    "exception_message",
                    List.of(FilterOp.EQ, FilterOp.NE, FilterOp.IN, FilterOp.LIKE),
                    "Сообщение исключения"));

    public static final Map<String, FilterField> BY_NAME = FIELDS.stream()
            .collect(Collectors.toMap(FilterField::name, Function.identity()));
}

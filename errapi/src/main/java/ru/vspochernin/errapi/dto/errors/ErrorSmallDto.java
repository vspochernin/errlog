package ru.vspochernin.errapi.dto.errors;

import java.time.Instant;

import io.swagger.v3.oas.annotations.media.Schema;
import ru.vspochernin.errapi.model.errors.ErrorEventRow;

@Schema(description = "Краткая информация о событии ошибки")
public record ErrorSmallDto(

        @Schema(description = "Идентификатор события UUID", example = "9eefb744-0afb-49c1-93d1-ebb31aedcb5e")
        String eventId,

        @Schema(description = "Время события в UTC", example = "2026-03-29T11:56:13.800Z")
        Instant timestamp,

        @Schema(description = "Тип источника события", example = "java-spring-logback")
        String sourceType,

        @Schema(description = "Название сервиса-источника", example = "jerrgen-alpha")
        String service,

        @Schema(description = "Уровень логирования события", example = "ERROR")
        String level,

        @Schema(description = "Отформатированное сообщение события", example = "There is exception with int: -463855931 and double: 0.39060667043203356")
        String messageFormatted,

        @Schema(description = "Fingerprint события в десятичном представлении UInt64", example = "16018207979116224727")
        String fingerprint,

        @Schema(description = "Источник формирования fingerprint", example = "STACKTRACE")
        String fingerprintSource,

        @Schema(description = "Имя инстанса сервиса", example = "errlog-demo-jerrgen-alpha-2", nullable = true)
        String instance,

        @Schema(description = "Версия сервиса", example = "1.2.345", nullable = true)
        String serviceVersion,

        @Schema(description = "Имя логгера", example = "ru.vspochernin.jerrgen.generator.SimpleGenerator", nullable = true)
        String logger,

        @Schema(description = "Имя потока", example = "scheduling-1", nullable = true)
        String thread,

        @Schema(description = "Шаблон сообщения события", example = "There is exception with int: {} and double: {}", nullable = true)
        String messageTemplate,

        @Schema(description = "Класс исключения", example = "java.lang.IllegalAccessException", nullable = true)
        String exceptionClass,

        @Schema(description = "Сообщение исключения", example = "Exception message, but log message with placeholders", nullable = true)
        String exceptionMessage)
{

    public static ErrorSmallDto fromRow(ErrorEventRow row) {
        return new ErrorSmallDto(
                row.eventId(),
                row.timestamp(),
                row.sourceType(),
                row.service(),
                row.level(),
                row.messageFormatted(),
                row.fingerprint(),
                row.fingerprintSource(),
                row.instance(),
                row.serviceVersion(),
                row.logger(),
                row.thread(),
                row.messageTemplate(),
                row.exceptionClass(),
                row.exceptionMessage());
    }
}

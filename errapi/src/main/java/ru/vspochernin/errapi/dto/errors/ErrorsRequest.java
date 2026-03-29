package ru.vspochernin.errapi.dto.errors;

import java.time.Instant;
import java.util.List;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Базовый запрос фильтрации")
public record ErrorsRequest(

        @Schema(
                description = "Начало интервала времени (UTC), при отсутствии - 24 часа от to",
                example = "2026-02-18T10:00:00Z",
                nullable = true)
        Instant from,

        @Schema(
                description = "Конец интервала времени (UTC), при отсутствии - текущее время",
                example = "2027-02-18T12:00:00Z",
                nullable = true)
        Instant to,

        @Schema(
                description = "Fingerprint группы ошибок (строковое представление UInt64)",
                example = "6968451703662559529",
                nullable = true)
        String fingerprint,

        @ArraySchema(
                arraySchema = @Schema(description = "Список фильтров (объединяются логическим AND)"),
                schema = @Schema(implementation = Filter.class))
        List<Filter> filters)
{

    public static ErrorsRequest empty() {
        return new ErrorsRequest(null, null, null, List.of());
    }

    @Schema(description = "Фильтр")
    public record Filter(

            @Schema(description = "Имя поля для фильтрации", example = "service")
            String field,

            @Schema(description = "Операция для фильтрации", example = "eq")
            String operation,

            @ArraySchema(
                    arraySchema = @Schema(description = "Список значений фильтра"),
                    schema = @Schema(example = "jerrgen-alpha"))
            List<String> values)
    {
    }
}

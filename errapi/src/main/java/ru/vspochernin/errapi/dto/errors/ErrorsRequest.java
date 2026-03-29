package ru.vspochernin.errapi.dto.errors;

import java.time.Instant;
import java.util.List;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Общий запрос для поиска и аналитики ошибок")
public record ErrorsRequest(

        @Schema(
                description = "Начало интервала в UTC. Если не указано, используется последние 24 часа относительно to",
                example = "2026-02-18T10:00:00Z",
                nullable = true)
        Instant from,

        @Schema(
                description = "Конец интервала в UTC. Если не указано, используется текущее время на стороне API",
                example = "2027-02-18T12:00:00Z",
                nullable = true)
        Instant to,

        @Schema(
                description = "Fingerprint группы ошибок в десятичном строковом представлении UInt64",
                example = "6968451703662559529",
                nullable = true)
        String fingerprint,

        @ArraySchema(
                arraySchema = @Schema(description = "Список фильтров, объединяемых по AND"),
                schema = @Schema(implementation = Filter.class))
        List<Filter> filters)
{

    public static ErrorsRequest empty() {
        return new ErrorsRequest(null, null, null, List.of());
    }

    @Schema(description = "Конкретный фильтр запроса")
    public record Filter(

            @Schema(description = "Имя поля фильтра", example = "service")
            String field,

            @Schema(description = "Операция фильтрации", example = "eq")
            String operation,

            @ArraySchema(
                    arraySchema = @Schema(description = "Список значений фильтра"),
                    schema = @Schema(example = "jerrgen-alpha"))
            List<String> values)
    {
    }
}

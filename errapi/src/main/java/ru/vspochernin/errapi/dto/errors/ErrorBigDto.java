package ru.vspochernin.errapi.dto.errors;

import com.fasterxml.jackson.annotation.JsonUnwrapped;
import io.swagger.v3.oas.annotations.media.Schema;
import ru.vspochernin.errapi.model.errors.ErrorEventRow;

@Schema(description = "Полная информация о событии ошибки")
public record ErrorBigDto(

        @JsonUnwrapped
        ErrorSmallDto smallDto,

        @Schema(description = "Stacktrace события ошибки", nullable = true)
        String stacktrace)
{

    public static ErrorBigDto fromRow(ErrorEventRow row) {
        return new ErrorBigDto(
                ErrorSmallDto.fromRow(row),
                row.stacktrace());
    }
}

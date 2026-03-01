package ru.vspochernin.errapi.dto.errors;

import com.fasterxml.jackson.annotation.JsonUnwrapped;
import ru.vspochernin.errapi.model.errors.ErrorEventRow;

public record ErrorBigDto(
        @JsonUnwrapped ErrorSmallDto smallDto,
        String stacktrace)
{

    public static ErrorBigDto fromRow(ErrorEventRow row) {
        return new ErrorBigDto(
                ErrorSmallDto.fromRow(row),
                row.stacktrace());
    }
}

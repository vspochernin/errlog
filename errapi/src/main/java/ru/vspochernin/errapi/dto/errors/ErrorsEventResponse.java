package ru.vspochernin.errapi.dto.errors;


import com.fasterxml.jackson.annotation.JsonUnwrapped;
import ru.vspochernin.errapi.model.errors.ErrorEventRow;

public record ErrorsEventResponse(
        @JsonUnwrapped ErrorBigDto dto)
{

    public static ErrorsEventResponse fromRow(ErrorEventRow row) {
        return new ErrorsEventResponse(ErrorBigDto.fromRow(row));
    }
}

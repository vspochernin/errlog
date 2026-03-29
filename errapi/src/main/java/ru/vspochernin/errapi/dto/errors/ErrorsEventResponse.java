package ru.vspochernin.errapi.dto.errors;


import com.fasterxml.jackson.annotation.JsonUnwrapped;
import io.swagger.v3.oas.annotations.media.Schema;
import ru.vspochernin.errapi.model.errors.ErrorEventRow;

@Schema(description = "Ответ с информацией по конкретному событию ошибки")
public record ErrorsEventResponse(

        @JsonUnwrapped
        ErrorBigDto dto)
{

    public static ErrorsEventResponse fromRow(ErrorEventRow row) {
        return new ErrorsEventResponse(ErrorBigDto.fromRow(row));
    }
}

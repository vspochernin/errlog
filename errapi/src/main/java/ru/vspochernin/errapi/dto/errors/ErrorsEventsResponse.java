package ru.vspochernin.errapi.dto.errors;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonUnwrapped;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import ru.vspochernin.errapi.model.errors.ErrorEventRow;

@Schema(description = "Ответ списка событий ошибок")
public record ErrorsEventsResponse(

        @ArraySchema(
                arraySchema = @Schema(description = "Элементы списка"),
                schema = @Schema(implementation = Item.class))
        List<Item> items,

        @Schema(description = "Общее количество найденных событий ошибок", example = "123")
        long eventsTotal)
{

    @Schema(description = "Элемент списка")
    public record Item(
            @JsonUnwrapped
            ErrorSmallDto dto)
    {

        public static Item fromRow(ErrorEventRow row) {
            return new Item(ErrorSmallDto.fromRow(row));
        }
    }
}

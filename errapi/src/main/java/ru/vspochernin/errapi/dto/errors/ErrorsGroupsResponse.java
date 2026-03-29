package ru.vspochernin.errapi.dto.errors;

import java.time.Instant;
import java.util.List;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import ru.vspochernin.errapi.model.errors.ErrorGroupRow;

@Schema(description = "Ответ со списком групп ошибок")
public record ErrorsGroupsResponse(

        @ArraySchema(
                arraySchema = @Schema(description = "Элементы списка"),
                schema = @Schema(implementation = Item.class))
        List<Item> items,

        @Schema(description = "Общее количество событий, попавших в выборку", example = "125")
        long eventsTotal,

        @Schema(description = "Общее количество групп ошибок", example = "12")
        long groupsTotal)
{

    @Schema(description = "Элемент списка групп ошибок")
    public record Item(

            @Schema(description = "Fingerprint группы", example = "16018207979116224727")
            String groupFingerprint,

            @Schema(description = "Количество событий в группе", example = "18")
            long groupCount,

            @Schema(description = "Время последнего события в группе", example = "2026-03-29T12:18:18.933Z")
            Instant groupLastSeen,

            @Schema(description = "Последнее событие группы")
            ErrorSmallDto lastEvent)
    {

        public static Item fromRow(ErrorGroupRow row) {
            return new Item(
                    row.groupFingerprint(),
                    row.groupCount(),
                    row.groupLastSeen(),
                    ErrorSmallDto.fromRow(row.lastEvent()));
        }
    }
}

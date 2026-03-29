package ru.vspochernin.errapi.dto.errors;

import java.time.Instant;
import java.util.List;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import ru.vspochernin.errapi.model.errors.ErrorTimeseriesRow;

@Schema(description = "Ответ временного ряда количества ошибок")
public record ErrorsTimeseriesResponse(

        @ArraySchema(
                arraySchema = @Schema(description = "Элементы временного ряда"),
                schema = @Schema(implementation = Item.class))
        List<Item> items,

        @Schema(description = "Размер одного интервала времени", example = "5m")
        String bucketSize)
{

    @Schema(description = "Элемент временного ряда")
    public record Item(

            @Schema(description = "Начало интервала времени", example = "2026-03-29T00:00:00Z")
            Instant bucketStart,

            @Schema(description = "Количество событий в интервале", example = "7")
            long count)
    {

        public static Item fromRow(ErrorTimeseriesRow row) {
            return new Item(row.bucketStart(), row.count());
        }
    }
}

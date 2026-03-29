package ru.vspochernin.errapi.dto.errors;

import java.util.List;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import ru.vspochernin.errapi.model.errors.FilterField;
import ru.vspochernin.errapi.model.errors.FilterOperation;

@Schema(description = "Список поддерживаемых полей и операций фильтрации")
public record ErrorsFiltersResponse(

        @ArraySchema(
                arraySchema = @Schema(description = "Элементы списка"),
                schema = @Schema(implementation = Item.class))
        List<Item> items)
{

    @Schema(description = "Описание конкретного доступного поля фильтрации")
    public record Item(

            @Schema(description = "Имя поля в API", example = "service")
            String name,

            @ArraySchema(
                    arraySchema = @Schema(description = "Разрешенные операции"),
                    schema = @Schema(example = "eq"))
            List<String> operations,

            @Schema(description = "Описание поля", example = "Название сервиса")
            String description)
    {

        public static Item fromFilterField(FilterField filterField) {
            return new Item(
                    filterField.name(),
                    filterField.operations().stream()
                            .map(FilterOperation::getName)
                            .toList(),
                    filterField.description());
        }
    }
}

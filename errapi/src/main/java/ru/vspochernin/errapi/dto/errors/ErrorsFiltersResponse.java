package ru.vspochernin.errapi.dto.errors;

import java.util.List;

import ru.vspochernin.errapi.model.errors.FilterField;
import ru.vspochernin.errapi.model.errors.FilterOperation;

public record ErrorsFiltersResponse(
        List<Item> items)
{

    public record Item(
            String name,
            List<String> operations,
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

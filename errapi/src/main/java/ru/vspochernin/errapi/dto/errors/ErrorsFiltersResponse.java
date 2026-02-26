package ru.vspochernin.errapi.dto.errors;

import java.util.List;

import ru.vspochernin.errapi.model.errors.FilterField;
import ru.vspochernin.errapi.model.errors.FilterOp;

public record ErrorsFiltersResponse(
        List<Item> items)
{

    public record Item(
            String name,
            List<String> operations,
            String description)
    {

        public static Item fromFilter(FilterField filter) {
            return new Item(
                    filter.name(),
                    filter.operations().stream()
                            .map(FilterOp::getName)
                            .toList(),
                    filter.description());
        }
    }
}

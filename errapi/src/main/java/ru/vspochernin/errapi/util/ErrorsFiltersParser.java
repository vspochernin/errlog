package ru.vspochernin.errapi.util;

import java.util.ArrayList;
import java.util.List;

import ru.vspochernin.errapi.config.ErrorsAllowlist;
import ru.vspochernin.errapi.dto.errors.ErrorsFilterRequest;
import ru.vspochernin.errapi.exception.ErrapiErrorType;
import ru.vspochernin.errapi.exception.ErrapiException;
import ru.vspochernin.errapi.model.errors.ErrorsFilterCondition;
import ru.vspochernin.errapi.model.errors.FilterField;
import ru.vspochernin.errapi.model.errors.FilterOp;

public class ErrorsFiltersParser {

    private ErrorsFiltersParser() {
    }

    public static List<ErrorsFilterCondition> parse(List<ErrorsFilterRequest> rawFilters) {
        if (rawFilters == null || rawFilters.isEmpty()) {
            return List.of();
        }

        List<ErrorsFilterCondition> result = new ArrayList<>(rawFilters.size());

        for (int i = 0; i < rawFilters.size(); i++) {
            ErrorsFilterRequest rawFilter = rawFilters.get(i);
            if (rawFilter == null) {
                throw new ErrapiException(ErrapiErrorType.BAD_REQUEST, "filters[" + i + "] is null");
            }

            String fieldName = rawFilter.field();
            if (fieldName == null || fieldName.isBlank()) {
                throw new ErrapiException(ErrapiErrorType.BAD_REQUEST, "filters[" + i + "].field is blank");
            }
            FilterField field = ErrorsAllowlist.byName(fieldName);

            FilterOp operation = parseOperation(rawFilter.operation(), i);
            if (!field.operations().contains(operation)) {
                throw new ErrapiException(
                        ErrapiErrorType.BAD_REQUEST,
                        "filters[" + i + "].operation " + operation.getName() + " unsupported for field " +
                                field.name());
            }

            List<String> values = rawFilter.values();
            if (values == null || values.isEmpty()) {
                throw new ErrapiException(ErrapiErrorType.BAD_REQUEST, "filters[" + i + "].values is empty");
            }
            if (operation != FilterOp.IN && values.size() != 1) {
                throw new ErrapiException(
                        ErrapiErrorType.BAD_REQUEST,
                        "filters[" + i + "].values must contain exactly one element for operation " +
                                operation.getName());
            }

            result.add(new ErrorsFilterCondition(field, operation, List.copyOf(values)));
        }

        return result;
    }

    private static FilterOp parseOperation(String raw, int i) {
        if (raw == null || raw.isBlank()) {
            throw new ErrapiException(ErrapiErrorType.BAD_REQUEST, "filters[" + i + "].operation is null or blank");
        }
        return FilterOp.byName(raw);
    }
}

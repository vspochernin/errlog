package ru.vspochernin.errapi.util;

import java.util.ArrayList;
import java.util.List;

import ru.vspochernin.errapi.config.ErrorsAllowlist;
import ru.vspochernin.errapi.dto.errors.ErrorsRequest;
import ru.vspochernin.errapi.exception.ErrapiErrorType;
import ru.vspochernin.errapi.exception.ErrapiException;
import ru.vspochernin.errapi.model.errors.ErrorsFilter;
import ru.vspochernin.errapi.model.errors.FilterField;
import ru.vspochernin.errapi.model.errors.FilterOperation;

public class ErrorsFiltersParser {

    private ErrorsFiltersParser() {
    }

    public static List<ErrorsFilter> parse(List<ErrorsRequest.Filter> rawFilters) {
        if (rawFilters == null || rawFilters.isEmpty()) {
            return List.of();
        }

        List<ErrorsFilter> result = new ArrayList<>(rawFilters.size());
        int i = 0;
        for (ErrorsRequest.Filter rawFilter : rawFilters) {
            if (rawFilter == null) {
                throw new ErrapiException(ErrapiErrorType.BAD_REQUEST, "filters[" + i + "] is null");
            }

            String fieldName = rawFilter.field();
            if (fieldName == null || fieldName.isBlank()) {
                throw new ErrapiException(ErrapiErrorType.BAD_REQUEST, "filters[" + i + "].field is blank");
            }
            FilterField field = ErrorsAllowlist.byName(fieldName);

            String operationName = rawFilter.operation();
            if (operationName == null || operationName.isBlank()) {
                throw new ErrapiException(ErrapiErrorType.BAD_REQUEST, "filters[" + i + "].operation is null or blank");
            }
            FilterOperation operation = FilterOperation.byName(operationName);
            field.checkOperationSupport(operation, i);

            List<String> values = rawFilter.values();
            if (values == null || values.isEmpty()) {
                throw new ErrapiException(ErrapiErrorType.BAD_REQUEST, "filters[" + i + "].values is empty");
            }
            if (!operation.isAllowsMultipleValues() && values.size() > 1) {
                throw new ErrapiException(
                        ErrapiErrorType.BAD_REQUEST,
                        "filters[" + i + "].values must contain only one element for operation " + operation.getName());
            }

            result.add(new ErrorsFilter(field, operation, values));
            i++;
        }

        return result;
    }
}

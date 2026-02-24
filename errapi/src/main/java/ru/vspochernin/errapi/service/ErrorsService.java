package ru.vspochernin.errapi.service;

import java.util.List;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.vspochernin.errapi.config.ErrorsAllowlist;
import ru.vspochernin.errapi.dto.errors.ErrorsEventsResponse;
import ru.vspochernin.errapi.dto.errors.ErrorsEventsResponseItemDto;
import ru.vspochernin.errapi.dto.errors.ErrorsFiltersResponse;
import ru.vspochernin.errapi.exception.ErrapiErrorType;
import ru.vspochernin.errapi.exception.ErrapiException;
import ru.vspochernin.errapi.model.errors.ErrorsQuery;
import ru.vspochernin.errapi.model.errors.FilterField;
import ru.vspochernin.errapi.repository.ErrorsRepository;
import ru.vspochernin.errapi.util.ErrorsQueryParser;

@Service
@RequiredArgsConstructor
public class ErrorsService {

    private final ErrorsRepository errorsRepository;

    public ErrorsFiltersResponse getFilters() {
        return new ErrorsFiltersResponse(ErrorsAllowlist.FIELDS.stream()
                .map(FilterField::toDto)
                .toList());
    }

    public ErrorsEventsResponse getEvents(String from, String to, int limit, long offset) {
        validateLimitOffset(limit, offset);

        ErrorsQuery query = ErrorsQueryParser.parse(from, to);

        long total = errorsRepository.countEvents(query);
        List<ErrorsEventsResponseItemDto> items = errorsRepository.findEvents(query, limit, offset).stream()
                .map(ErrorsEventsResponseItemDto::fromRow)
                .toList();

        return new ErrorsEventsResponse(items, total);
    }

    private static void validateLimitOffset(int limit, long offset) {
        if (limit < 1 || limit > 500) {
            throw new ErrapiException(ErrapiErrorType.BAD_REQUEST, "Limit должен быть от 1 до 500");
        }
        if (offset < 0) {
            throw new ErrapiException(ErrapiErrorType.BAD_REQUEST, "Offset должен быть неотрицательным");
        }
    }
}

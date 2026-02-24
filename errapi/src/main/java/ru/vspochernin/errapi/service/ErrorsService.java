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

    private static final int LIMIT_MIN = 1;
    private static final int LIMIT_MAX = 500;

    private final ErrorsRepository errorsRepository;

    public ErrorsFiltersResponse getFilters() {
        return new ErrorsFiltersResponse(ErrorsAllowlist.FIELDS.stream()
                .map(FilterField::toDto)
                .toList());
    }

    public ErrorsEventsResponse getEvents(String from, String to, int limit, long offset) {
        validateLimitOffset(limit, offset);

        ErrorsQuery query = ErrorsQueryParser.parse(from, to);

        long eventsTotal = errorsRepository.countEvents(query);
        List<ErrorsEventsResponseItemDto> items = errorsRepository.findEvents(query, limit, offset).stream()
                .map(ErrorsEventsResponseItemDto::fromRow)
                .toList();

        return new ErrorsEventsResponse(items, eventsTotal);
    }

    private static void validateLimitOffset(int limit, long offset) {
        if (limit < LIMIT_MIN || limit > LIMIT_MAX) {
            throw new ErrapiException(ErrapiErrorType.BAD_REQUEST,
                    "Limit должен быть от " + LIMIT_MIN + " до " + LIMIT_MAX);
        }
        if (offset < 0L) {
            throw new ErrapiException(ErrapiErrorType.BAD_REQUEST, "Offset должен быть неотрицательным");
        }
    }
}

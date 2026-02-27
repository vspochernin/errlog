package ru.vspochernin.errapi.service;

import java.util.List;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.vspochernin.errapi.config.ErrorsAllowlist;
import ru.vspochernin.errapi.dto.errors.ErrorsRequest;
import ru.vspochernin.errapi.dto.errors.ErrorsEventsResponse;
import ru.vspochernin.errapi.dto.errors.ErrorsFiltersResponse;
import ru.vspochernin.errapi.model.errors.ErrorsQuery;
import ru.vspochernin.errapi.repository.ErrorsRepository;
import ru.vspochernin.errapi.util.ValidationUtils;

@Service
@RequiredArgsConstructor
public class ErrorsService {

    private final ErrorsRepository errorsRepository;

    public ErrorsFiltersResponse getFilters() {
        return new ErrorsFiltersResponse(ErrorsAllowlist.FIELDS.stream()
                .map(ErrorsFiltersResponse.Item::fromFilterField)
                .toList());
    }

    public ErrorsEventsResponse getEvents(ErrorsRequest request, int limit, long offset) {
        ValidationUtils.validateLimitOffset(limit, offset);

        ErrorsQuery query = ErrorsQuery.parseFromErrorsRequest(request);

        long eventsTotal = errorsRepository.countEvents(query);
        List<ErrorsEventsResponse.Item> items = errorsRepository.findEvents(query, limit, offset).stream()
                .map(ErrorsEventsResponse.Item::fromRow)
                .toList();

        return new ErrorsEventsResponse(items, eventsTotal);
    }
}

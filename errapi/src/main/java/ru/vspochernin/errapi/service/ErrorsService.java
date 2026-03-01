package ru.vspochernin.errapi.service;

import java.util.List;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.vspochernin.errapi.config.ErrorsAllowlist;
import ru.vspochernin.errapi.dto.errors.ErrorsEventResponse;
import ru.vspochernin.errapi.dto.errors.ErrorsGroupsResponse;
import ru.vspochernin.errapi.dto.errors.ErrorsRequest;
import ru.vspochernin.errapi.dto.errors.ErrorsEventsResponse;
import ru.vspochernin.errapi.dto.errors.ErrorsFiltersResponse;
import ru.vspochernin.errapi.dto.errors.ErrorsTimeseriesResponse;
import ru.vspochernin.errapi.exception.ErrapiErrorType;
import ru.vspochernin.errapi.exception.ErrapiException;
import ru.vspochernin.errapi.model.errors.ErrorsQuery;
import ru.vspochernin.errapi.model.errors.TimeBucket;
import ru.vspochernin.errapi.model.errors.EventsAndGroupsTotals;
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

    public ErrorsEventResponse getEventById(String eventId) {
        ValidationUtils.validateUuid(eventId);

        return errorsRepository.findEventById(eventId)
                .map(ErrorsEventResponse::fromRow)
                .orElseThrow(() -> new ErrapiException(ErrapiErrorType.NOT_FOUND, "Event not found"));
    }

    public ErrorsGroupsResponse getGroups(ErrorsRequest request, int limit, long offset) {
        ValidationUtils.validateLimitOffset(limit, offset);

        ErrorsQuery query = ErrorsQuery.parseFromErrorsRequest(request);

        EventsAndGroupsTotals totals = errorsRepository.countEventsAndGroupsTotals(query);

        List<ErrorsGroupsResponse.Item> items = errorsRepository.findGroups(query, limit, offset).stream()
                .map(ErrorsGroupsResponse.Item::fromRow)
                .toList();

        return new ErrorsGroupsResponse(items, totals.eventsTotal(), totals.groupsTotal());
    }

    public ErrorsTimeseriesResponse getTimeseries(ErrorsRequest request, String bucketRaw) {
        ErrorsQuery query = ErrorsQuery.parseFromErrorsRequest(request);

        TimeBucket bucket = (bucketRaw == null || bucketRaw.isBlank())
                ? TimeBucket.byTimeWindow(query.timeWindow())
                : TimeBucket.byName(bucketRaw);

        List<ErrorsTimeseriesResponse.Item> items = errorsRepository.findTimeseries(query, bucket).stream()
                .map(ErrorsTimeseriesResponse.Item::fromRow)
                .toList();

        return new ErrorsTimeseriesResponse(items, bucket.getName());
    }
}

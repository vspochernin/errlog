package ru.vspochernin.errapi.controller;

import java.util.Objects;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import ru.vspochernin.errapi.dto.errors.ErrorsEventResponse;
import ru.vspochernin.errapi.dto.errors.ErrorsGroupsResponse;
import ru.vspochernin.errapi.dto.errors.ErrorsRequest;
import ru.vspochernin.errapi.dto.errors.ErrorsEventsResponse;
import ru.vspochernin.errapi.dto.errors.ErrorsFiltersResponse;
import ru.vspochernin.errapi.dto.errors.ErrorsTimeseriesResponse;
import ru.vspochernin.errapi.service.ErrorsService;

@RestController
@RequestMapping("/api/errors")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('READER', 'ADMIN', 'OWNER')")
public class ErrorsController {

    private final ErrorsService errorsService;

    @GetMapping("/filters")
    public ResponseEntity<ErrorsFiltersResponse> filters() {
        ErrorsFiltersResponse response = errorsService.getFilters();
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(response);
    }

    @PostMapping("/events")
    public ResponseEntity<ErrorsEventsResponse> events(
            @RequestBody(required = false) ErrorsRequest requestOrNull,
            @RequestParam(value = "limit", defaultValue = "10") int limit,
            @RequestParam(value = "offset", defaultValue = "0") long offset)
    {
        ErrorsRequest request = Objects.requireNonNullElse(requestOrNull, ErrorsRequest.empty());
        ErrorsEventsResponse response = errorsService.getEvents(request, limit, offset);
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(response);
    }

    @GetMapping("/events/{eventId}")
    public ResponseEntity<ErrorsEventResponse> event(@PathVariable("eventId") String eventId) {
        ErrorsEventResponse response = errorsService.getEventById(eventId);
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(response);
    }

    @PostMapping("/groups")
    public ResponseEntity<ErrorsGroupsResponse> groups(
            @RequestBody(required = false) ErrorsRequest requestOrNull,
            @RequestParam(value = "limit", defaultValue = "10") int limit,
            @RequestParam(value = "offset", defaultValue = "0") long offset)
    {
        ErrorsRequest request = Objects.requireNonNullElse(requestOrNull, ErrorsRequest.empty());
        ErrorsGroupsResponse response = errorsService.getGroups(request, limit, offset);
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(response);
    }

    @PostMapping("/timeseries")
    public ResponseEntity<ErrorsTimeseriesResponse> timeseries(
            @RequestBody(required = false) ErrorsRequest requestOrNull,
            @RequestParam(value = "bucket", required = false) String bucket)
    {
        ErrorsRequest request = Objects.requireNonNullElse(requestOrNull, ErrorsRequest.empty());
        ErrorsTimeseriesResponse response = errorsService.getTimeseries(request, bucket);
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(response);
    }
}

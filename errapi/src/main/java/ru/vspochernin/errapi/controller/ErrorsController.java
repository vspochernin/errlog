package ru.vspochernin.errapi.controller;

import java.util.Objects;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import ru.vspochernin.errapi.dto.errors.ErrorsRequest;
import ru.vspochernin.errapi.dto.errors.ErrorsEventsResponse;
import ru.vspochernin.errapi.dto.errors.ErrorsFiltersResponse;
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
}

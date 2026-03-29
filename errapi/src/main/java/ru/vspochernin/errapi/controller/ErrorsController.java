package ru.vspochernin.errapi.controller;

import java.util.Objects;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import ru.vspochernin.errapi.dto.errors.ErrorsEventResponse;
import ru.vspochernin.errapi.dto.errors.ErrorsEventsResponse;
import ru.vspochernin.errapi.dto.errors.ErrorsFiltersResponse;
import ru.vspochernin.errapi.dto.errors.ErrorsGroupsResponse;
import ru.vspochernin.errapi.dto.errors.ErrorsRequest;
import ru.vspochernin.errapi.dto.errors.ErrorsTimeseriesResponse;
import ru.vspochernin.errapi.exception.ErrorMessage;
import ru.vspochernin.errapi.service.ErrorsService;

@RestController
@RequestMapping("/api/errors")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('READER', 'ADMIN', 'OWNER')")
@Tag(name = "Errors", description = "Поиск и аналитическая обработка событий ошибок")
public class ErrorsController {

    private final ErrorsService errorsService;

    @Operation(
            summary = "Получить список поддерживаемых полей операций для фильтрации",
            description = "Возвращает список поддерживаемых полей и операций для filters в ErrorsRequest")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Список фильтров получен",
                    content = @Content(schema = @Schema(implementation = ErrorsFiltersResponse.class))),
            @ApiResponse(responseCode = "401", description = "Требуется аутентификация",
                    content = @Content(schema = @Schema(implementation = ErrorMessage.class))),
            @ApiResponse(responseCode = "403", description = "Недостаточно прав",
                    content = @Content(schema = @Schema(implementation = ErrorMessage.class)))
    })
    @GetMapping("/filters")
    public ResponseEntity<ErrorsFiltersResponse> filters() {
        ErrorsFiltersResponse response = errorsService.getFilters();
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(response);
    }

    @Operation(
            summary = "Получить список событий ошибок",
            description = "Возвращает страницу событий ошибок. Тело запроса задает базовые параметры выборки, а limit/offset - параметры пагинации")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Список событий получен",
                    content = @Content(schema = @Schema(implementation = ErrorsEventsResponse.class))),
            @ApiResponse(responseCode = "400", description = "Некорректные параметры запроса",
                    content = @Content(schema = @Schema(implementation = ErrorMessage.class))),
            @ApiResponse(responseCode = "401", description = "Требуется аутентификация",
                    content = @Content(schema = @Schema(implementation = ErrorMessage.class))),
            @ApiResponse(responseCode = "403", description = "Недостаточно прав",
                    content = @Content(schema = @Schema(implementation = ErrorMessage.class)))
    })
    @PostMapping("/events")
    public ResponseEntity<ErrorsEventsResponse> events(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Общий запрос для фильтрации",
                    content = @Content(
                            schema = @Schema(implementation = ErrorsRequest.class),
                            examples = @ExampleObject(
                                    value = """
                                            {
                                              "filters": [
                                                {
                                                  "field": "service",
                                                  "operation": "in",
                                                  "values": ["jerrgen-alpha", "jerrgen-gamma"]
                                                },
                                                {
                                                  "field": "level",
                                                  "operation": "eq",
                                                  "values": ["ERROR"]
                                                }
                                              ]
                                            }
                                            """)))
            @RequestBody(required = false) ErrorsRequest requestOrNull,

            @Parameter(description = "Размер страницы", example = "10")
            @RequestParam(value = "limit", defaultValue = "10") int limit,

            @Parameter(description = "Смещение в выборке", example = "0")
            @RequestParam(value = "offset", defaultValue = "0") long offset)
    {
        ErrorsRequest request = Objects.requireNonNullElse(requestOrNull, ErrorsRequest.empty());
        ErrorsEventsResponse response = errorsService.getEvents(request, limit, offset);
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(response);
    }

    @Operation(
            summary = "Получить событие по eventId",
            description = "Возвращает полную информацию по одному событию, включая stacktrace")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Событие найдено",
                    content = @Content(schema = @Schema(implementation = ErrorsEventResponse.class))),
            @ApiResponse(responseCode = "400", description = "Некорректный eventId",
                    content = @Content(schema = @Schema(implementation = ErrorMessage.class))),
            @ApiResponse(responseCode = "404", description = "Событие не найдено",
                    content = @Content(schema = @Schema(implementation = ErrorMessage.class))),
            @ApiResponse(responseCode = "401", description = "Требуется аутентификация",
                    content = @Content(schema = @Schema(implementation = ErrorMessage.class))),
            @ApiResponse(responseCode = "403", description = "Недостаточно прав",
                    content = @Content(schema = @Schema(implementation = ErrorMessage.class)))
    })
    @GetMapping("/events/{eventId}")
    public ResponseEntity<ErrorsEventResponse> event(
            @Parameter(description = "UUID события", example = "93305bb4-c952-4f65-8875-731da06e1077")
            @PathVariable String eventId)
    {
        ErrorsEventResponse response = errorsService.getEventById(eventId);
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(response);
    }

    @Operation(
            summary = "Получить список групп ошибок",
            description = "Возвращает страницу групп ошибок, агрегированных по fingerprint")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Список групп получен",
                    content = @Content(schema = @Schema(implementation = ErrorsGroupsResponse.class))),
            @ApiResponse(responseCode = "400", description = "Некорректные параметры запроса",
                    content = @Content(schema = @Schema(implementation = ErrorMessage.class))),
            @ApiResponse(responseCode = "401", description = "Требуется аутентификация",
                    content = @Content(schema = @Schema(implementation = ErrorMessage.class))),
            @ApiResponse(responseCode = "403", description = "Недостаточно прав",
                    content = @Content(schema = @Schema(implementation = ErrorMessage.class)))
    })
    @PostMapping("/groups")
    public ResponseEntity<ErrorsGroupsResponse> groups(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Общий запрос для фильтрации",
                    content = @Content(
                            schema = @Schema(implementation = ErrorsRequest.class),
                            examples = @ExampleObject(
                                    value = """
                                            {
                                              "filters": [
                                                {
                                                  "field": "service",
                                                  "operation": "in",
                                                  "values": ["jerrgen-alpha", "jerrgen-gamma"]
                                                },
                                                {
                                                  "field": "level",
                                                  "operation": "eq",
                                                  "values": ["ERROR"]
                                                }
                                              ]
                                            }
                                            """)))
            @RequestBody(required = false) ErrorsRequest requestOrNull,

            @Parameter(description = "Размер страницы", example = "10")
            @RequestParam(value = "limit", defaultValue = "10") int limit,

            @Parameter(description = "Смещение в выборке", example = "0")
            @RequestParam(value = "offset", defaultValue = "0") long offset)
    {
        ErrorsRequest request = Objects.requireNonNullElse(requestOrNull, ErrorsRequest.empty());
        ErrorsGroupsResponse response = errorsService.getGroups(request, limit, offset);
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(response);
    }

    @Operation(
            summary = "Получить временной ряд по количеству ошибок",
            description = "Возвращает агрегированный временной ряд по количеству ошибок. Размер бакета можно передать явно, либо он будет выбран автоматически")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Временной ряд получен",
                    content = @Content(schema = @Schema(implementation = ErrorsTimeseriesResponse.class))),
            @ApiResponse(responseCode = "400", description = "Некорректные параметры запроса",
                    content = @Content(schema = @Schema(implementation = ErrorMessage.class))),
            @ApiResponse(responseCode = "401", description = "Требуется аутентификация",
                    content = @Content(schema = @Schema(implementation = ErrorMessage.class))),
            @ApiResponse(responseCode = "403", description = "Недостаточно прав",
                    content = @Content(schema = @Schema(implementation = ErrorMessage.class)))
    })
    @PostMapping("/timeseries")
    public ResponseEntity<ErrorsTimeseriesResponse> timeseries(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Общий запрос для фильтрации",
                    content = @Content(
                            schema = @Schema(implementation = ErrorsRequest.class),
                            examples = @ExampleObject(
                                    value = """
                                            {
                                              "filters": [
                                                {
                                                  "field": "service",
                                                  "operation": "in",
                                                  "values": ["jerrgen-alpha", "jerrgen-gamma"]
                                                },
                                                {
                                                  "field": "level",
                                                  "operation": "eq",
                                                  "values": ["ERROR"]
                                                }
                                              ]
                                            }
                                            """)))
            @RequestBody(required = false) ErrorsRequest requestOrNull,

            @Parameter(description = "Размер временного бакета. Например: 1m, 5m, 15m", example = "5m")
            @RequestParam(value = "bucket", required = false) String bucket)
    {
        ErrorsRequest request = Objects.requireNonNullElse(requestOrNull, ErrorsRequest.empty());
        ErrorsTimeseriesResponse response = errorsService.getTimeseries(request, bucket);
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(response);
    }
}

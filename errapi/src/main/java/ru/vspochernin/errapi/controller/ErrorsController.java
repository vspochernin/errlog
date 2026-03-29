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
            summary = "Получить список поддерживаемых полей и операций фильтрации",
            description = "Возвращает список поддерживаемых полей и операций для поля filters в ErrorsRequest")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Успешный ответ",
                    content = @Content(schema = @Schema(implementation = ErrorsFiltersResponse.class))),
            @ApiResponse(responseCode = "401", description = "Ошибка аутентификации",
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
            description = "Возвращает страницу списка событий ошибок (с пагинацией)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Успешный ответ",
                    content = @Content(schema = @Schema(implementation = ErrorsEventsResponse.class))),
            @ApiResponse(responseCode = "400", description = "Ошибка запроса",
                    content = @Content(schema = @Schema(implementation = ErrorMessage.class))),
            @ApiResponse(responseCode = "401", description = "Ошибка аутентификации",
                    content = @Content(schema = @Schema(implementation = ErrorMessage.class))),
            @ApiResponse(responseCode = "403", description = "Недостаточно прав",
                    content = @Content(schema = @Schema(implementation = ErrorMessage.class)))
    })
    @PostMapping("/events")
    public ResponseEntity<ErrorsEventsResponse> events(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Базовый запрос фильтрации",
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
            @RequestBody(required = false)
            ErrorsRequest requestOrNull,

            @Parameter(description = "Размер страницы", example = "10")
            @RequestParam(value = "limit", defaultValue = "10")
            int limit,

            @Parameter(description = "Смещение", example = "0")
            @RequestParam(value = "offset", defaultValue = "0")
            long offset)
    {
        ErrorsRequest request = Objects.requireNonNullElse(requestOrNull, ErrorsRequest.empty());
        ErrorsEventsResponse response = errorsService.getEvents(request, limit, offset);
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(response);
    }

    @Operation(
            summary = "Получить событие по eventId",
            description = "Возвращает полную информацию по событию")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Успешный ответ",
                    content = @Content(schema = @Schema(implementation = ErrorsEventResponse.class))),
            @ApiResponse(responseCode = "400", description = "Ошибка запроса",
                    content = @Content(schema = @Schema(implementation = ErrorMessage.class))),
            @ApiResponse(responseCode = "404", description = "Событие не найдено",
                    content = @Content(schema = @Schema(implementation = ErrorMessage.class))),
            @ApiResponse(responseCode = "401", description = "Ошибка аутентификации",
                    content = @Content(schema = @Schema(implementation = ErrorMessage.class))),
            @ApiResponse(responseCode = "403", description = "Недостаточно прав",
                    content = @Content(schema = @Schema(implementation = ErrorMessage.class)))
    })
    @GetMapping("/events/{eventId}")
    public ResponseEntity<ErrorsEventResponse> event(
            @Parameter(description = "UUID события", example = "93305bb4-c952-4f65-8875-731da06e1077")
            @PathVariable
            String eventId)
    {
        ErrorsEventResponse response = errorsService.getEventById(eventId);
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(response);
    }

    @Operation(
            summary = "Получить список групп ошибок",
            description = "Возвращает страницу списка групп ошибок (с пагинацией)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Успешный ответ",
                    content = @Content(schema = @Schema(implementation = ErrorsGroupsResponse.class))),
            @ApiResponse(responseCode = "400", description = "Ошибка запроса",
                    content = @Content(schema = @Schema(implementation = ErrorMessage.class))),
            @ApiResponse(responseCode = "401", description = "Ошибка аутентификации",
                    content = @Content(schema = @Schema(implementation = ErrorMessage.class))),
            @ApiResponse(responseCode = "403", description = "Недостаточно прав",
                    content = @Content(schema = @Schema(implementation = ErrorMessage.class)))
    })
    @PostMapping("/groups")
    public ResponseEntity<ErrorsGroupsResponse> groups(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Базовый запрос фильтрации",
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
            @RequestBody(required = false)
            ErrorsRequest requestOrNull,

            @Parameter(description = "Размер страницы", example = "10")
            @RequestParam(value = "limit", defaultValue = "10")
            int limit,

            @Parameter(description = "Смещение", example = "0")
            @RequestParam(value = "offset", defaultValue = "0")
            long offset)
    {
        ErrorsRequest request = Objects.requireNonNullElse(requestOrNull, ErrorsRequest.empty());
        ErrorsGroupsResponse response = errorsService.getGroups(request, limit, offset);
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(response);
    }

    @Operation(
            summary = "Получить временной ряд количества ошибок",
            description = "Возвращает временной ряд количества ошибок, размер интервала определяется автоматически или задается вручную")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Успешный ответ",
                    content = @Content(schema = @Schema(implementation = ErrorsTimeseriesResponse.class))),
            @ApiResponse(responseCode = "400", description = "Ошибка запроса",
                    content = @Content(schema = @Schema(implementation = ErrorMessage.class))),
            @ApiResponse(responseCode = "401", description = "Ошибка аутентификации",
                    content = @Content(schema = @Schema(implementation = ErrorMessage.class))),
            @ApiResponse(responseCode = "403", description = "Недостаточно прав",
                    content = @Content(schema = @Schema(implementation = ErrorMessage.class)))
    })
    @PostMapping("/timeseries")
    public ResponseEntity<ErrorsTimeseriesResponse> timeseries(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Базовый запрос фильтрации",
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
            @RequestBody(required = false)
            ErrorsRequest requestOrNull,

            @Parameter(description = "Размер интервала времени", example = "5m")
            @RequestParam(value = "bucket", required = false)
            String bucket)
    {
        ErrorsRequest request = Objects.requireNonNullElse(requestOrNull, ErrorsRequest.empty());
        ErrorsTimeseriesResponse response = errorsService.getTimeseries(request, bucket);
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(response);
    }
}

package ru.vspochernin.errapi.service;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.vspochernin.errapi.dto.errors.ErrorsRequest;
import ru.vspochernin.errapi.exception.ErrapiException;
import ru.vspochernin.errapi.model.errors.ErrorEventRow;
import ru.vspochernin.errapi.model.errors.ErrorGroupRow;
import ru.vspochernin.errapi.model.errors.ErrorTimeseriesRow;
import ru.vspochernin.errapi.model.errors.EventsAndGroupsTotals;
import ru.vspochernin.errapi.repository.ErrorsRepository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ErrorsServiceTest {

    @Mock
    private ErrorsRepository errorsRepository;

    private ErrorsService errorsService;

    private static final String EVENT_ID = "93305bb4-c952-4f65-8875-731da06e1077";

    @BeforeEach
    void setUp() {
        errorsService = new ErrorsService(errorsRepository);
    }

    @Test
    void getFiltersShouldReturnAllowlistFields() {
        var response = errorsService.getFilters();
        assertThat(response.items()).isNotEmpty();
        // should contain known fields
        assertThat(response.items()).anyMatch(i -> i.name().equals("service"));
        assertThat(response.items()).anyMatch(i -> i.name().equals("level"));
    }

    @Test
    void getEventsShouldReturnResponse() {
        var request = ErrorsRequest.empty();
        when(errorsRepository.countEvents(any())).thenReturn(10L);
        when(errorsRepository.findEvents(any(), eq(20), eq(0L)))
                .thenReturn(List.of(sampleRow()));

        var response = errorsService.getEvents(request, 20, 0);

        assertThat(response.eventsTotal()).isEqualTo(10L);
        assertThat(response.items()).hasSize(1);
        verify(errorsRepository).countEvents(any());
        verify(errorsRepository).findEvents(any(), eq(20), eq(0L));
    }

    @Test
    void getEventsWithInvalidLimitShouldThrow() {
        assertThatThrownBy(() -> errorsService.getEvents(ErrorsRequest.empty(), 0, 0))
                .isInstanceOf(ErrapiException.class)
                .hasMessageContaining("Limit must be");
    }

    @Test
    void getEventByIdShouldReturnEvent() {
        when(errorsRepository.findEventById(EVENT_ID)).thenReturn(Optional.of(sampleRow()));

        var response = errorsService.getEventById(EVENT_ID);

        // Проверяем содержимое ответа, а не только что он не null.
        // Структура: ErrorsEventResponse -> ErrorBigDto (smallDto + stacktrace) -> ErrorSmallDto (поля).
        assertThat(response).isNotNull();
        var bigDto = response.dto();
        var smallDto = bigDto.smallDto();
        assertThat(smallDto.eventId()).isEqualTo(EVENT_ID);
        assertThat(smallDto.service()).isEqualTo("test-svc");
        assertThat(smallDto.level()).isEqualTo("ERROR");
        assertThat(bigDto.stacktrace()).isEqualTo("at com.Foo.doIt(Foo.java:42)");
    }

    @Test
    void getEventByIdNotFoundShouldThrow() {
        when(errorsRepository.findEventById(EVENT_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> errorsService.getEventById(EVENT_ID))
                .isInstanceOf(ErrapiException.class)
                .hasMessageContaining("Event not found");
    }

    @Test
    void getEventByIdInvalidUuidShouldThrow() {
        assertThatThrownBy(() -> errorsService.getEventById("not-a-uuid"))
                .isInstanceOf(ErrapiException.class)
                .hasMessageContaining("UUID");
    }

    @Test
    void getGroupsShouldReturnResponse() {
        var request = ErrorsRequest.empty();
        when(errorsRepository.countEventsAndGroupsTotals(any()))
                .thenReturn(new EventsAndGroupsTotals(100L, 5L));
        when(errorsRepository.findGroups(any(), anyInt(), anyLong()))
                .thenReturn(List.of(new ErrorGroupRow("12345", 50L,
                        Instant.parse("2026-01-01T00:00:00Z"), sampleRow())));

        var response = errorsService.getGroups(request, 10, 0);

        assertThat(response.eventsTotal()).isEqualTo(100L);
        assertThat(response.groupsTotal()).isEqualTo(5L);
        assertThat(response.items()).hasSize(1);
    }

    @Test
    void getTimeseriesAutoBucketShouldInferFromWindow() {
        var request = ErrorsRequest.empty();
        when(errorsRepository.findTimeseries(any(), any()))
                .thenReturn(List.of(new ErrorTimeseriesRow(
                        Instant.parse("2026-01-01T00:00:00Z"), 42L)));

        var response = errorsService.getTimeseries(request, null);

        assertThat(response.items()).hasSize(1);
        // for default 24h window -> M15
        assertThat(response.bucketSize()).isEqualTo("15m");
    }

    @Test
    void getTimeseriesManualBucketShouldUseProvidedValue() {
        var request = ErrorsRequest.empty();
        when(errorsRepository.findTimeseries(any(), any()))
                .thenReturn(List.of());

        var response = errorsService.getTimeseries(request, "1m");

        assertThat(response.bucketSize()).isEqualTo("1m");
    }

    private static ErrorEventRow sampleRow() {
        return new ErrorEventRow(
                EVENT_ID, Instant.parse("2026-01-01T00:00:00Z"),
                "java-spring-logback", "test-svc", "ERROR", "msg",
                "12345", "STACKTRACE", "inst-1", "1.0", "logger",
                "main", "User {} not found", "RuntimeException", "boom",
                "at com.Foo.doIt(Foo.java:42)");
    }
}

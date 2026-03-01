package ru.vspochernin.errapi.model.errors;

import java.time.Duration;
import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import ru.vspochernin.errapi.exception.ErrapiErrorType;
import ru.vspochernin.errapi.exception.ErrapiException;

@Getter
@RequiredArgsConstructor
public enum TimeBucket {

    M1("1m", Duration.ofMinutes(1), "INTERVAL 1 MINUTE"),
    M5("5m", Duration.ofMinutes(5), "INTERVAL 5 MINUTE"),
    M15("15m", Duration.ofMinutes(15), "INTERVAL 15 MINUTE"),
    H1("1h", Duration.ofHours(1), "INTERVAL 1 HOUR"),
    H6("6h", Duration.ofHours(6), "INTERVAL 6 HOUR"),
    D1("1d", Duration.ofDays(1), "INTERVAL 1 DAY"),
    ;

    private final String name; // Имя бакета для передачи в API.
    private final Duration duration;
    private final String intervalSql;

    private static final Map<String, TimeBucket> BY_NAME = Arrays.stream(TimeBucket.values())
            .collect(Collectors.toMap(TimeBucket::getName, Function.identity()));

    public static TimeBucket byName(String name) {
        return Optional.ofNullable(BY_NAME.get(name))
                .orElseThrow(() -> new ErrapiException(
                        ErrapiErrorType.BAD_REQUEST,
                        "Unknown bucket: " + name + ". Allowed: " + String.join(", ", BY_NAME.keySet())));
    }

    public static TimeBucket byTimeWindow(TimeWindow timeWindow) {
        Duration d = Duration.between(timeWindow.from(), timeWindow.to());

        if (d.compareTo(Duration.ofHours(1)) <= 0) {
            return M1;
        }
        if (d.compareTo(Duration.ofHours(6)) <= 0) {
            return M5;
        }
        if (d.compareTo(Duration.ofHours(24)) <= 0) {
            return M15;
        }
        if (d.compareTo(Duration.ofDays(7)) <= 0) {
            return H1;
        }
        if (d.compareTo(Duration.ofDays(30)) <= 0) {
            return H6;
        }
        return D1;
    }
}

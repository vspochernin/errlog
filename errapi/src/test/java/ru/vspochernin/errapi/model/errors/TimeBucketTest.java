package ru.vspochernin.errapi.model.errors;

import java.time.Duration;
import java.time.Instant;

import org.junit.jupiter.api.Test;
import ru.vspochernin.errapi.exception.ErrapiException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TimeBucketTest {

    @Test
    void byNameShouldReturnCorrectBucket() {
        assertThat(TimeBucket.byName("1m")).isEqualTo(TimeBucket.M1);
        assertThat(TimeBucket.byName("5m")).isEqualTo(TimeBucket.M5);
        assertThat(TimeBucket.byName("15m")).isEqualTo(TimeBucket.M15);
        assertThat(TimeBucket.byName("1h")).isEqualTo(TimeBucket.H1);
        assertThat(TimeBucket.byName("6h")).isEqualTo(TimeBucket.H6);
        assertThat(TimeBucket.byName("1d")).isEqualTo(TimeBucket.D1);
    }

    @Test
    void byNameShouldThrowForUnknown() {
        assertThatThrownBy(() -> TimeBucket.byName("2m"))
                .isInstanceOf(ErrapiException.class)
                .hasMessageContaining("Unknown bucket");
    }

    @Test
    void byTimeWindowUpTo1HourShouldReturnM1() {
        Instant from = Instant.EPOCH;
        Instant to = from.plus(Duration.ofMinutes(59));
        assertThat(TimeBucket.byTimeWindow(new TimeWindow(from, to))).isEqualTo(TimeBucket.M1);
    }

    @Test
    void byTimeWindow1HourShouldReturnM1() {
        Instant from = Instant.EPOCH;
        Instant to = from.plus(Duration.ofHours(1));
        assertThat(TimeBucket.byTimeWindow(new TimeWindow(from, to))).isEqualTo(TimeBucket.M1);
    }

    @Test
    void byTimeWindowUpTo6HoursShouldReturnM5() {
        Instant from = Instant.EPOCH;
        Instant to = from.plus(Duration.ofHours(5));
        assertThat(TimeBucket.byTimeWindow(new TimeWindow(from, to))).isEqualTo(TimeBucket.M5);
    }

    @Test
    void byTimeWindow6HoursShouldReturnM5() {
        Instant from = Instant.EPOCH;
        Instant to = from.plus(Duration.ofHours(6));
        assertThat(TimeBucket.byTimeWindow(new TimeWindow(from, to))).isEqualTo(TimeBucket.M5);
    }

    @Test
    void byTimeWindowUpTo24HoursShouldReturnM15() {
        Instant from = Instant.EPOCH;
        Instant to = from.plus(Duration.ofHours(23));
        assertThat(TimeBucket.byTimeWindow(new TimeWindow(from, to))).isEqualTo(TimeBucket.M15);
    }

    @Test
    void byTimeWindowExactly24HoursShouldReturnM15() {
        // Ровно 24 часа - дефолтное окно системы. Граница <=: 24h попадает в M15, не в H1.
        Instant from = Instant.EPOCH;
        Instant to = from.plus(Duration.ofHours(24));
        assertThat(TimeBucket.byTimeWindow(new TimeWindow(from, to))).isEqualTo(TimeBucket.M15);
    }

    @Test
    void byTimeWindowUpTo7DaysShouldReturnH1() {
        Instant from = Instant.EPOCH;
        Instant to = from.plus(Duration.ofDays(6));
        assertThat(TimeBucket.byTimeWindow(new TimeWindow(from, to))).isEqualTo(TimeBucket.H1);
    }

    @Test
    void byTimeWindowExactly7DaysShouldReturnH1() {
        // Ровно 7 дней - граница <=: 7d попадает в H1, не в H6.
        Instant from = Instant.EPOCH;
        Instant to = from.plus(Duration.ofDays(7));
        assertThat(TimeBucket.byTimeWindow(new TimeWindow(from, to))).isEqualTo(TimeBucket.H1);
    }

    @Test
    void byTimeWindowUpTo30DaysShouldReturnH6() {
        Instant from = Instant.EPOCH;
        Instant to = from.plus(Duration.ofDays(29));
        assertThat(TimeBucket.byTimeWindow(new TimeWindow(from, to))).isEqualTo(TimeBucket.H6);
    }

    @Test
    void byTimeWindowExactly30DaysShouldReturnH6() {
        // Ровно 30 дней - граница <=: 30d попадает в H6, не в D1.
        Instant from = Instant.EPOCH;
        Instant to = from.plus(Duration.ofDays(30));
        assertThat(TimeBucket.byTimeWindow(new TimeWindow(from, to))).isEqualTo(TimeBucket.H6);
    }

    @Test
    void byTimeWindowOver30DaysShouldReturnD1() {
        Instant from = Instant.EPOCH;
        Instant to = from.plus(Duration.ofDays(31));
        assertThat(TimeBucket.byTimeWindow(new TimeWindow(from, to))).isEqualTo(TimeBucket.D1);
    }
}

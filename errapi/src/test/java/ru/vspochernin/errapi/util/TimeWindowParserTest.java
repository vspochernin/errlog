package ru.vspochernin.errapi.util;

import java.time.Duration;
import java.time.Instant;

import org.junit.jupiter.api.Test;
import ru.vspochernin.errapi.exception.ErrapiException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

class TimeWindowParserTest {

    @Test
    void shouldParseValidInstantRange() {
        Instant from = Instant.parse("2026-01-01T00:00:00Z");
        Instant to = Instant.parse("2026-01-02T00:00:00Z");

        var window = TimeWindowParser.parse(from, to);

        assertThat(window.from()).isEqualTo(from);
        assertThat(window.to()).isEqualTo(to);
    }

    @Test
    void nullToShouldDefaultToNow() {
        Instant from = Instant.parse("2026-01-01T00:00:00Z");

        var window = TimeWindowParser.parse(from, null);

        assertThat(window.from()).isEqualTo(from);
        assertThat(window.to()).isCloseTo(Instant.now(), within(Duration.ofSeconds(5)));
    }

    @Test
    void nullFromShouldDefaultTo24HoursBeforeTo() {
        Instant to = Instant.parse("2026-01-02T00:00:00Z");

        var window = TimeWindowParser.parse(null, to);

        assertThat(window.from()).isEqualTo(to.minus(Duration.ofHours(24)));
        assertThat(window.to()).isEqualTo(to);
    }

    @Test
    void bothNullShouldDefaultToNowMinus24hToNow() {
        var window = TimeWindowParser.parse(null, (Instant) null);

        assertThat(window.to()).isCloseTo(Instant.now(), within(Duration.ofSeconds(5)));
        assertThat(window.from()).isCloseTo(window.to().minus(Duration.ofHours(24)),
                within(Duration.ofSeconds(5)));
    }

    @Test
    void fromAfterToShouldThrow() {
        Instant from = Instant.parse("2026-02-01T00:00:00Z");
        Instant to = Instant.parse("2026-01-01T00:00:00Z");

        assertThatThrownBy(() -> TimeWindowParser.parse(from, to))
                .isInstanceOf(ErrapiException.class)
                .hasMessageContaining("from must be <= to");
    }

    @Test
    void shouldParseValidStringRange() {
        var window = TimeWindowParser.parse("2026-01-01T00:00:00Z", "2026-01-02T00:00:00Z");

        assertThat(window.from()).isEqualTo(Instant.parse("2026-01-01T00:00:00Z"));
        assertThat(window.to()).isEqualTo(Instant.parse("2026-01-02T00:00:00Z"));
    }

    @Test
    void invalidStringShouldThrow() {
        assertThatThrownBy(() -> TimeWindowParser.parse("not-a-date", "2026-01-02T00:00:00Z"))
                .isInstanceOf(ErrapiException.class)
                .hasMessageContaining("ISO-8601");
    }

    @Test
    void emptyStringsShouldBeTreatedAsNull() {
        var window = TimeWindowParser.parse("", "");

        assertThat(window.to()).isCloseTo(Instant.now(), within(Duration.ofSeconds(5)));
    }
}

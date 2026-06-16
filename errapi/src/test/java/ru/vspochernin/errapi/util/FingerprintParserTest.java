package ru.vspochernin.errapi.util;

import java.math.BigInteger;

import org.junit.jupiter.api.Test;
import ru.vspochernin.errapi.exception.ErrapiException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FingerprintParserTest {

    @Test
    void shouldReturnEmptyForNull() {
        assertThat(FingerprintParser.parseO(null)).isEmpty();
    }

    @Test
    void shouldReturnEmptyForBlank() {
        assertThat(FingerprintParser.parseO("   ")).isEmpty();
    }

    @Test
    void shouldReturnEmptyForEmptyString() {
        assertThat(FingerprintParser.parseO("")).isEmpty();
    }

    @Test
    void shouldThrowWhenContainsNonDigitChars() {
        assertThatThrownBy(() -> FingerprintParser.parseO("123abc"))
                .isInstanceOf(ErrapiException.class)
                .hasMessageContaining("fingerprint must be UInt64 string");
    }

    @Test
    void shouldParseZero() {
        assertThat(FingerprintParser.parseO("0")).hasValue(BigInteger.ZERO);
    }

    @Test
    void shouldParseMaxUInt64() {
        BigInteger max = BigInteger.ONE.shiftLeft(64).subtract(BigInteger.ONE);
        assertThat(FingerprintParser.parseO(max.toString())).hasValue(max);
    }

    @Test
    void shouldThrowWhenAboveMaxUInt64() {
        BigInteger above = BigInteger.ONE.shiftLeft(64);
        assertThatThrownBy(() -> FingerprintParser.parseO(above.toString()))
                .isInstanceOf(ErrapiException.class)
                .hasMessageContaining("out of range");
    }

    @Test
    void shouldThrowForNegativeNumber() {
        assertThatThrownBy(() -> FingerprintParser.parseO("-1"))
                .isInstanceOf(ErrapiException.class)
                .hasMessageContaining("UInt64 string");
    }
}

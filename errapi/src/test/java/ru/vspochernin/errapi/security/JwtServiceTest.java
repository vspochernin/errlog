package ru.vspochernin.errapi.security;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtServiceTest {

    private static final String SECRET_32 = "01234567890123456789012345678901"; // 32 байта

    @Test
    void shouldThrowWhenSecretTooShort() {
        assertThatThrownBy(() -> new JwtService("short", 3600))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("JWT_SECRET");
    }

    @Test
    void shouldConstructWithValidSecret() {
        var jwt = new JwtService(SECRET_32, 3600);
        assertThat(jwt).isNotNull();
    }

    @Test
    void shouldGenerateNonEmptyToken() {
        var jwt = new JwtService(SECRET_32, 3600);
        var token = jwt.generateToken("test-user");
        assertThat(token).isNotBlank();
    }

    @Test
    void shouldExtractLogin() {
        var jwt = new JwtService(SECRET_32, 3600);
        var token = jwt.generateToken("test-user");
        assertThat(jwt.extractLogin(token)).isEqualTo("test-user");
    }

    @Test
    void shouldValidateValidToken() {
        var jwt = new JwtService(SECRET_32, 3600);
        var token = jwt.generateToken("test-user");
        assertThat(jwt.isValid(token)).isTrue();
    }

    @Test
    void shouldInvalidateBogusToken() {
        var jwt = new JwtService(SECRET_32, 3600);
        assertThat(jwt.isValid("not.a.jwt.token")).isFalse();
    }
}

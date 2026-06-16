package ru.vspochernin.errapi.exception;

import java.util.List;
import java.util.NoSuchElementException;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import static org.assertj.core.api.Assertions.assertThat;

class ExceptionApiHandlerTest {

    private final ExceptionApiHandler handler = new ExceptionApiHandler();

    @Test
    void shouldHandleErrapiException() {
        var ex = new ErrapiException(ErrapiErrorType.NOT_FOUND, "User 5 not found");
        var response = handler.errapiException(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().errorType()).isEqualTo("NOT_FOUND");
        assertThat(response.getBody().additionalInfo()).isEqualTo("User 5 not found");
    }

    @Test
    void shouldHandleErrapiExceptionWithoutAdditionalInfo() {
        var ex = new ErrapiException(ErrapiErrorType.LOGIN_EXISTS);
        var response = handler.errapiException(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().errorType()).isEqualTo("LOGIN_EXISTS");
    }

    @Test
    void shouldHandleBadCredentialsException() {
        var ex = new BadCredentialsException("Bad credentials");
        var response = handler.badCredentialsException(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getBody().errorType()).isEqualTo("BAD_CREDENTIALS");
    }

    @Test
    void shouldHandleMethodArgumentNotValidWithFieldError() {
        var bindingResult = new BeanPropertyBindingResult(new Object(), "object");
        bindingResult.addError(new FieldError("object", "login", "must not be blank"));
        var ex = new MethodArgumentNotValidException(null, bindingResult);
        var response = handler.methodArgumentNotValidException(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().errorType()).isEqualTo("INVALID_LOGIN");
    }

    @Test
    void shouldHandleMethodArgumentNotValidWithoutFieldErrors() throws Exception {
        var bindingResult = new BeanPropertyBindingResult(new Object(), "object");
        var ex = new MethodArgumentNotValidException(null, bindingResult);
        var response = handler.methodArgumentNotValidException(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().errorType()).isEqualTo("BAD_REQUEST");
    }

    @Test
    void shouldMapEmailFieldToErrorType() {
        var bindingResult = new BeanPropertyBindingResult(new Object(), "object");
        bindingResult.addError(new FieldError("object", "email", "invalid"));
        var ex = new MethodArgumentNotValidException(null, bindingResult);
        var response = handler.methodArgumentNotValidException(ex);

        assertThat(response.getBody().errorType()).isEqualTo("INVALID_EMAIL");
    }

    @Test
    void shouldMapPasswordFieldToErrorType() {
        var bindingResult = new BeanPropertyBindingResult(new Object(), "object");
        bindingResult.addError(new FieldError("object", "password", "must not be blank"));
        var ex = new MethodArgumentNotValidException(null, bindingResult);
        var response = handler.methodArgumentNotValidException(ex);

        assertThat(response.getBody().errorType()).isEqualTo("INVALID_PASSWORD");
    }

    @Test
    void shouldHandleHttpMessageNotReadableException() {
        var ex = new HttpMessageNotReadableException("broken body");
        var response = handler.httpMessageNotReadableException(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void shouldHandleNumberFormatException() {
        var ex = new NumberFormatException("not a number");
        var response = handler.numberFormatException(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void shouldHandleNoSuchElementException() {
        var ex = new NoSuchElementException("not found");
        var response = handler.noSuchElementException(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void shouldHandleMethodArgumentTypeMismatchException() {
        var ex = new MethodArgumentTypeMismatchException("role", String.class, "role", null, null);
        var response = handler.methodArgumentTypeMismatchException(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void shouldHandleAuthorizationDeniedException() {
        var ex = new AuthorizationDeniedException("access denied");
        var response = handler.forbidden(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(response.getBody().errorType()).isEqualTo("FORBIDDEN");
    }

    @Test
    void shouldHandleAccessDeniedException() {
        var ex = new AccessDeniedException("access denied");
        var response = handler.forbidden(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void shouldHandleGenericException() {
        var ex = new RuntimeException("unexpected error");
        var response = handler.exception(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody().errorType()).isEqualTo("UNEXPECTED_ERROR");
    }
}

package ru.vspochernin.errapi.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrapiErrorType {

    UNEXPECTED_ERROR(0, "Unexpected error", HttpStatus.INTERNAL_SERVER_ERROR),

    LOGIN_EXISTS(1, "Login already exists", HttpStatus.BAD_REQUEST),
    EMAIL_EXISTS(2, "Email already exists", HttpStatus.BAD_REQUEST),

    INVALID_LOGIN(3, "Invalid login", HttpStatus.BAD_REQUEST),
    INVALID_EMAIL(4, "Invalid email", HttpStatus.BAD_REQUEST),
    INVALID_PASSWORD(5, "Invalid password", HttpStatus.BAD_REQUEST),

    PASSWORD_DOES_NOT_MATCH(6, "Entered password does not match the old one", HttpStatus.UNAUTHORIZED),
    INCORRECT_ROLE_CHANGE(7, "Incorrect role change", HttpStatus.FORBIDDEN),

    BAD_REQUEST(8, "Bad request", HttpStatus.BAD_REQUEST),
    NOT_FOUND(9, "Requested item not found", HttpStatus.NOT_FOUND),

    BAD_CREDENTIALS(10, "User with such login or password not found", HttpStatus.UNAUTHORIZED),
    USER_DOES_NOT_EXIST(11, "User does not exist", HttpStatus.UNAUTHORIZED),
    AUTH_REQUIRED(12, "Authentication required", HttpStatus.UNAUTHORIZED),
    INVALID_TOKEN(13, "Invalid token", HttpStatus.UNAUTHORIZED),
    FORBIDDEN(14, "Insufficient permissions", HttpStatus.FORBIDDEN),

    INCORRECT_TIME_BORDERS(15, "Invalid time borders", HttpStatus.BAD_REQUEST),
    ;

    private final int id;
    private final String description;
    private final HttpStatus httpStatus;
}

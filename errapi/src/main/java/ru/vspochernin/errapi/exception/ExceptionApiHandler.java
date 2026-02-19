package ru.vspochernin.errapi.exception;

import java.util.List;
import java.util.NoSuchElementException;

import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@RestControllerAdvice
public class ExceptionApiHandler {

    @ExceptionHandler(ErrapiException.class)
    public ResponseEntity<ErrorMessage> errapiException(ErrapiException exception) {
        return ResponseEntity
                .status(exception.getErrorType().getHttpStatus())
                .body(ErrorMessage.fromErrapiException(exception));
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ErrorMessage> badCredentialsException(BadCredentialsException exception) {
        ErrapiErrorType errorType = ErrapiErrorType.BAD_CREDENTIALS;
        return ResponseEntity
                .status(errorType.getHttpStatus())
                .body(ErrorMessage.fromErrorTypeWithAdditionalInfo(errorType, exception.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorMessage> methodArgumentNotValidException(MethodArgumentNotValidException exception) {
        List<FieldError> fieldErrors = exception.getBindingResult().getFieldErrors();

        ErrapiErrorType errorType = ErrapiErrorType.BAD_REQUEST_BODY;
        String additionalInfo = "Ошибка валидации полей";

        if (!fieldErrors.isEmpty()) {
            FieldError fieldError = fieldErrors.getFirst();
            errorType = mapFieldToErrorType(fieldError.getField());
            additionalInfo = fieldError.getField() + ": " + fieldError.getDefaultMessage();
        }

        return ResponseEntity
                .status(errorType.getHttpStatus())
                .body(ErrorMessage.fromErrorTypeWithAdditionalInfo(errorType, additionalInfo));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorMessage> httpMessageNotReadableException(HttpMessageNotReadableException exception) {
        ErrapiErrorType errorType = ErrapiErrorType.BAD_REQUEST_BODY;
        return ResponseEntity
                .status(errorType.getHttpStatus())
                .body(ErrorMessage.fromErrorTypeWithAdditionalInfo(errorType, exception.getMessage()));
    }

    @ExceptionHandler(NumberFormatException.class)
    public ResponseEntity<ErrorMessage> numberFormatException(NumberFormatException exception) {
        ErrapiErrorType errorType = ErrapiErrorType.BAD_REQUEST_BODY;
        return ResponseEntity
                .status(errorType.getHttpStatus())
                .body(ErrorMessage.fromErrorTypeWithAdditionalInfo(errorType, exception.getMessage()));
    }

    @ExceptionHandler(NoSuchElementException.class)
    public ResponseEntity<ErrorMessage> noSuchElementException(NoSuchElementException exception) {
        ErrapiErrorType errorType = ErrapiErrorType.NOT_FOUND;
        return ResponseEntity
                .status(errorType.getHttpStatus())
                .body(ErrorMessage.fromErrorTypeWithAdditionalInfo(errorType, exception.getMessage()));
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorMessage> methodArgumentTypeMismatchException(MethodArgumentTypeMismatchException ex)
    {
        ErrapiErrorType errorType = ErrapiErrorType.BAD_REQUEST_QUERY;
        return ResponseEntity
                .status(errorType.getHttpStatus())
                .body(ErrorMessage.fromErrorTypeWithAdditionalInfo(errorType, ex.getMessage()));
    }

    @ExceptionHandler({AccessDeniedException.class, AuthorizationDeniedException.class})
    public ResponseEntity<ErrorMessage> forbidden(Exception exception) {
        ErrapiErrorType errorType = ErrapiErrorType.FORBIDDEN;
        return ResponseEntity
                .status(errorType.getHttpStatus())
                .body(ErrorMessage.fromErrorTypeWithAdditionalInfo(errorType, exception.getMessage()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorMessage> exception(Exception exception) {
        ErrapiErrorType errorType = ErrapiErrorType.UNEXPECTED_ERROR;
        return ResponseEntity
                .status(errorType.getHttpStatus())
                .body(ErrorMessage.fromErrorTypeWithAdditionalInfo(errorType, exception.toString()));
    }

    private static ErrapiErrorType mapFieldToErrorType(String field) {
        return switch (field) {
            case "login" -> ErrapiErrorType.INVALID_LOGIN;
            case "email" -> ErrapiErrorType.INVALID_EMAIL;
            case "password" -> ErrapiErrorType.INVALID_PASSWORD;
            default -> ErrapiErrorType.BAD_REQUEST_BODY;
        };
    }
}

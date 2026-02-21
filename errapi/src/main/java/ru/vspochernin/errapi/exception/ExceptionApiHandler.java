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

    // Ошибки ErrapiException, выброшенные вручную.
    @ExceptionHandler(ErrapiException.class)
    public ResponseEntity<ErrorMessage> errapiException(ErrapiException ex) {
        return ResponseEntity
                .status(ex.getErrorType().getHttpStatus())
                .body(ErrorMessage.fromErrapiException(ex));
    }

    // Ошибки при некорректной аутентификаци.
    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ErrorMessage> badCredentialsException(BadCredentialsException ex) {
        ErrapiErrorType errorType = ErrapiErrorType.BAD_CREDENTIALS;
        return ResponseEntity
                .status(errorType.getHttpStatus())
                .body(ErrorMessage.fromErrorTypeWithAdditionalInfo(errorType, ex.getMessage()));
    }

    // Ошибки валидации параметров.
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorMessage> methodArgumentNotValidException(MethodArgumentNotValidException ex) {
        // По умолчанию (если не найдем ничего уточняющего).
        ErrapiErrorType errorType = ErrapiErrorType.BAD_REQUEST;
        String additionalInfo = "Ошибка валидации полей";

        List<FieldError> fieldErrors = ex.getBindingResult().getFieldErrors();
        if (!fieldErrors.isEmpty()) {
            FieldError fieldError = fieldErrors.getFirst(); // Ссылаемся на первое непрошедшее валидацию поле.
            errorType = mapFieldToErrorType(fieldError.getField());
            additionalInfo = fieldError.getField() + ": " + fieldError.getDefaultMessage();
        }

        return ResponseEntity
                .status(errorType.getHttpStatus())
                .body(ErrorMessage.fromErrorTypeWithAdditionalInfo(errorType, additionalInfo));
    }

    // Ошибки парсинга тела HTTP-запроса.
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorMessage> httpMessageNotReadableException(HttpMessageNotReadableException ex) {
        ErrapiErrorType errorType = ErrapiErrorType.BAD_REQUEST;
        return ResponseEntity
                .status(errorType.getHttpStatus())
                .body(ErrorMessage.fromErrorTypeWithAdditionalInfo(errorType, ex.getMessage()));
    }

    // Ошибки парсинга чисел.
    @ExceptionHandler(NumberFormatException.class)
    public ResponseEntity<ErrorMessage> numberFormatException(NumberFormatException ex) {
        ErrapiErrorType errorType = ErrapiErrorType.BAD_REQUEST;
        return ResponseEntity
                .status(errorType.getHttpStatus())
                .body(ErrorMessage.fromErrorTypeWithAdditionalInfo(errorType, ex.getMessage()));
    }

    // Ошибки ненахождения элемента.
    @ExceptionHandler(NoSuchElementException.class)
    public ResponseEntity<ErrorMessage> noSuchElementException(NoSuchElementException ex) {
        ErrapiErrorType errorType = ErrapiErrorType.NOT_FOUND;
        return ResponseEntity
                .status(errorType.getHttpStatus())
                .body(ErrorMessage.fromErrorTypeWithAdditionalInfo(errorType, ex.getMessage()));
    }

    // Ошибки конкертации параметров запроса.
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorMessage> methodArgumentTypeMismatchException(MethodArgumentTypeMismatchException ex)
    {
        ErrapiErrorType errorType = ErrapiErrorType.BAD_REQUEST;
        return ResponseEntity
                .status(errorType.getHttpStatus())
                .body(ErrorMessage.fromErrorTypeWithAdditionalInfo(errorType, ex.getMessage()));
    }

    // Ошибки запрета доступа.
    @ExceptionHandler({
            AuthorizationDeniedException.class, // Когда механизм авторизации выбросил исключение.
            AccessDeniedException.class // Когда пользователь аутентифицирован, но не имеет права доступа.
    })
    public ResponseEntity<ErrorMessage> forbidden(Exception ex) {
        ErrapiErrorType errorType = ErrapiErrorType.FORBIDDEN;
        return ResponseEntity
                .status(errorType.getHttpStatus())
                .body(ErrorMessage.fromErrorTypeWithAdditionalInfo(errorType, ex.getMessage()));
    }

    // Все остальные ошибки.
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorMessage> exception(Exception exception) {
        ErrapiErrorType errorType = ErrapiErrorType.UNEXPECTED_ERROR;
        return ResponseEntity
                .status(errorType.getHttpStatus())
                .body(ErrorMessage.fromErrorTypeWithAdditionalInfo(errorType, exception.toString()));
    }

    // Отображение названия поля, непрошедшего валидацию к типу ошибки.
    private static ErrapiErrorType mapFieldToErrorType(String field) {
        return switch (field) {
            case "login" -> ErrapiErrorType.INVALID_LOGIN;
            case "email" -> ErrapiErrorType.INVALID_EMAIL;
            case "password" -> ErrapiErrorType.INVALID_PASSWORD;
            default -> ErrapiErrorType.BAD_REQUEST;
        };
    }
}

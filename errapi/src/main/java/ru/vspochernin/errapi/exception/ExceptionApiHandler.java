package ru.vspochernin.errapi.exception;

import java.util.NoSuchElementException;

import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

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
        ErrapiErrorType errorType = ErrapiErrorType.BAD_REQUEST_BODY;
        return ResponseEntity
                .status(errorType.getHttpStatus())
                .body(ErrorMessage.fromErrorTypeWithAdditionalInfo(errorType, exception.getMessage()));
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
}

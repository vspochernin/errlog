package ru.vspochernin.errapi.exception;

import lombok.Getter;

@Getter
public class ErrapiException extends RuntimeException {

    private static final String DEFAULT_ADDITIONAL_INFO = "";

    private final ErrapiErrorType errorType;
    private final String additionalInfo;

    public ErrapiException(ErrapiErrorType errorType) {
        this.errorType = errorType;
        this.additionalInfo = DEFAULT_ADDITIONAL_INFO;
    }

    public ErrapiException(ErrapiErrorType errorType, String additionalInfo) {
        this.errorType = errorType;
        this.additionalInfo = additionalInfo;
    }
}

package ru.vspochernin.errapi.exception;

import lombok.Getter;

@Getter
public class ErrapiException extends RuntimeException {

    private final ErrapiErrorType errorType;
    private final String additionalInfo;

    public ErrapiException(ErrapiErrorType errorType) {
        super(errorType.getDescription());
        this.errorType = errorType;
        this.additionalInfo = "";
    }

    public ErrapiException(ErrapiErrorType errorType, String additionalInfo) {
        super(additionalInfo);
        this.errorType = errorType;
        this.additionalInfo = additionalInfo;
    }
}

package ru.vspochernin.errapi.exception;

public record ErrorMessage(
        int id,
        String description,
        String errorType,
        String additionalInfo)
{

    public static ErrorMessage fromErrorTypeWithAdditionalInfo(ErrapiErrorType errorType, String additionalInfo) {
        return new ErrorMessage(errorType.getId(), errorType.getDescription(), errorType.name(), additionalInfo);
    }

    public static ErrorMessage fromErrapiException(ErrapiException errapiException) {
        ErrapiErrorType errorType = errapiException.getErrorType();

        return new ErrorMessage(
                errorType.getId(),
                errorType.getDescription(),
                errorType.name(),
                errapiException.getAdditionalInfo());
    }
}

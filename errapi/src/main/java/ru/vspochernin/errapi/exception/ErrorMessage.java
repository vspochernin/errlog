package ru.vspochernin.errapi.exception;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Единый формат ошибки API")
public record ErrorMessage(

        @Schema(description = "Числовой идентификатор типа ошибки", example = "7")
        int id,

        @Schema(description = "Описание ошибки", example = "Error description")
        String description,

        @Schema(description = "Тип ошибки", example = "ERROR_TYPE")
        String errorType,

        @Schema(description = "Дополнительная информация об ошибке", example = "Error additional info")
        String additionalInfo)
{

    public static ErrorMessage fromErrorType(ErrapiErrorType errorType) {
        return new ErrorMessage(errorType.getId(), errorType.getDescription(), errorType.name(), "");
    }

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

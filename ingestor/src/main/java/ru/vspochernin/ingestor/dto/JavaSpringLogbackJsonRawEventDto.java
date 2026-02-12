package ru.vspochernin.ingestor.dto;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

public record JavaSpringLogbackJsonRawEventDto(

        @JsonProperty("formattedMessage")
        String formattedMessage,
        @JsonProperty("instance")
        String instance,
        @JsonProperty("level")
        String level,
        @JsonProperty("loggerName")
        String loggerName,
        @JsonProperty("message")
        String message,
        @JsonProperty("service")
        String service,
        @JsonProperty("serviceVersion")
        String serviceVersion,
        @JsonProperty("sourceType")
        String sourceType,
        @JsonProperty("threadName")
        String threadName,
        @JsonProperty("throwable")
        ThrowableDto throwable,
        @JsonProperty("timestamp")
        long timestamp)
{

    public record ThrowableDto(
            @JsonProperty("className")
            String className,
            @JsonProperty("message")
            String message,
            @JsonProperty("stepArray")
            List<StepDto> stepArray)
    {

        public record StepDto(
                @JsonProperty("className")
                String className,
                @JsonProperty("fileName")
                String fileName,
                @JsonProperty("lineNumber")
                int lineNumber,
                @JsonProperty("methodName")
                String methodName)
        {
        }
    }
}

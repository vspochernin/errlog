package ru.vspochernin.ingestor.dto;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;
import ru.vspochernin.ingestor.utils.StringUtils;

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

    public String getStacktraceFormatted() {
        if (throwable() == null) {
            return null;
        }
        ThrowableDto t = throwable();
        StringBuilder sb = new StringBuilder();

        if (StringUtils.isNotBlank(t.className())) {
            sb.append(t.className());
            if (StringUtils.isNotBlank(t.message())) {
                sb.append(": ").append(t.message());
            }
            sb.append('\n');
        }

        List<ThrowableDto.StepDto> steps = t.stepArray();
        if (steps == null || steps.isEmpty()) {
            return sb.isEmpty() ? null : sb.toString().trim();
        }

        for (ThrowableDto.StepDto step : steps) {
            sb.append("\tat ")
                    .append(StringUtils.getOrDefault(step.className(), "Unknown className"))
                    .append('.')
                    .append(StringUtils.getOrDefault(step.methodName(), "Unknown methodName"))
                    .append('(')
                    .append(formatLocation(step.fileName(), step.lineNumber()))
                    .append(')')
                    .append('\n');
        }

        return sb.toString().trim();
    }

    // В нашем случае fileName может быть строкой "null".
    private static String formatLocation(String fileName, int lineNumber) {
        if (fileName == null || fileName.isBlank() || fileName.equals("null")) {
            return "Unknown source";
        }

        if (lineNumber > 0) {
            return fileName + ":" + lineNumber;
        }

        return fileName;
    }
}

package ru.vspochernin.ingestor.fingerprint;

import java.util.regex.Pattern;

import org.springframework.stereotype.Component;
import ru.vspochernin.ingestor.model.NormalizedErrorEvent;
import ru.vspochernin.ingestor.utils.StringUtils;

@Component
public class DefaultFingerprintBuilder implements FingerprintBuilder {

    private static final Pattern DIGITS = Pattern.compile("\\d");

    @Override
    public FingerprintResult build(NormalizedErrorEvent event) {
        String service = StringUtils.getOrDefault(event.service(), "");
        String logger = StringUtils.getOrDefault(event.logger(), "");
        String level = StringUtils.getOrDefault(event.level(), "");

        String base;
        FingerprintSource source;

        if (StringUtils.isNotBlank(event.stacktrace())) {
            String exceptionClass = StringUtils.getOrDefault(event.exceptionClass(), "");
            String stacktraceNoDigits = DIGITS.matcher(event.stacktrace()).replaceAll("");

            base = String.join("|", service, logger, level, exceptionClass, stacktraceNoDigits);
            source = FingerprintSource.STACKTRACE;
        } else if (StringUtils.isNotBlank(event.messageTemplate())) {
            base = String.join("|", service, logger, level, event.messageTemplate());
            source = FingerprintSource.TEMPLATE;
        } else {
            base = String.join("|", service, logger, level);
            source = FingerprintSource.MINIMAL;
        }

        return new FingerprintResult(base, source);
    }
}

package ru.vspochernin.ingestor.fingerprint;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.regex.Pattern;

import org.springframework.stereotype.Component;
import ru.vspochernin.ingestor.model.NormalizedErrorEvent;
import ru.vspochernin.ingestor.utils.StringUtils;

@Component
public class DefaultFingerprintCalculator implements FingerprintCalculator {

    private static final Pattern DIGITS = Pattern.compile("\\d");

    @Override
    public FingerprintResult calculate(NormalizedErrorEvent event) {
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

        return new FingerprintResult(hashToLong(base), source);
    }

    private static long hashToLong(String value) {
        byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
        byte[] digest = sha256(bytes);
        return ByteBuffer.wrap(digest, 0, 8).getLong();
    }

    private static byte[] sha256(byte[] data) {
        try {
            return MessageDigest.getInstance("SHA-256").digest(data);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 algorithm can't be found", e);
        }
    }
}

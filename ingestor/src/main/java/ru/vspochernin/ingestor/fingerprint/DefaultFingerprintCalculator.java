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

        String prefix = service + "|" + logger + "|" + level;

        if (StringUtils.isNotBlank(event.stacktrace())) {
            String exceptionClass = StringUtils.getOrDefault(event.exceptionClass(), "");
            String stacktraceWithoutDigits = DIGITS.matcher(event.stacktrace()).replaceAll("");
            String base = prefix + "|" + exceptionClass + "|" + stacktraceWithoutDigits;
            return new FingerprintResult(hashToUInt64(base), FingerprintSource.STACKTRACE);
        }

        if (StringUtils.isNotBlank(event.messageTemplate())) {
            String base = prefix + "|" + event.messageTemplate();
            return new FingerprintResult(hashToUInt64(base), FingerprintSource.TEMPLATE);
        }

        return new FingerprintResult(hashToUInt64(prefix), FingerprintSource.MINIMAL);
    }

    private static long hashToUInt64(String base) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(base.getBytes(StandardCharsets.UTF_8));
            return ByteBuffer.wrap(digest, 0, 8).getLong(); // Первые 8 байт хэша.
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 algorithm not found", e);
        }
    }
}

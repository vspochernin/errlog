package ru.vspochernin.ingestor.utils;

public class StringUtils {

    private StringUtils() {
    }

    public static String getStringOrDefault(String value, String defaultValue) {
        if (value == null || value.isBlank()) {
            return defaultValue;
        }
        return value;
    }

    public static String getFirstNonBlankStringOrDefault(String value1, String value2, String defaultValue) {
        if (value1 != null && !value1.isBlank()) {
            return value1;
        }
        if (value2 != null && !value2.isBlank()) {
            return value2;
        }
        return defaultValue;
    }

    public static boolean isNotBlank(String value) {
        return value != null && !value.isBlank();
    }
}

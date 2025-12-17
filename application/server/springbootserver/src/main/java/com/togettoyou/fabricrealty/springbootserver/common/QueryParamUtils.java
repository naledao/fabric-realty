package com.togettoyou.fabricrealty.springbootserver.common;

public final class QueryParamUtils {
    private QueryParamUtils() {
    }

    public static int parseIntOrZero(String value) {
        if (value == null) {
            return 0;
        }
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException ignored) {
            return 0;
        }
    }

    public static int normalizePositive(int value, int defaultValue) {
        return value > 0 ? value : defaultValue;
    }
}


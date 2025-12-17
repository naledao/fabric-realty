package com.togettoyou.fabricrealty.springbootserver.fabric;

import io.grpc.Status;

public final class FabricErrorTranslator {
    private FabricErrorTranslator() {
    }

    public static String toUserMessage(Throwable error) {
        if (error == null) {
            return "";
        }

        String direct = safeMessage(error);
        if (looksLikeGoStyleMessage(direct)) {
            return direct;
        }

        Throwable current = error;
        while (current != null) {
            Status status = Status.fromThrowable(current);
            if (status != null && status.getCode() != Status.Code.UNKNOWN) {
                return formatGrpcStatus(status, current);
            }
            current = current.getCause();
        }

        Status status = Status.fromThrowable(error);
        if (status != null) {
            return formatGrpcStatus(status, error);
        }

        return safeMessage(error);
    }

    private static boolean looksLikeGoStyleMessage(String message) {
        if (message == null) {
            return false;
        }
        return message.startsWith("错误码:") && message.contains("消息:");
    }

    private static String formatGrpcStatus(Status status, Throwable original) {
        String code = toGoStyleCode(status.getCode());
        String description = status.getDescription();
        if (description == null || description.isBlank()) {
            description = safeMessage(original);
        }
        return "错误码: " + code + ", 消息: " + description;
    }

    private static String safeMessage(Throwable error) {
        String message = error.getMessage();
        if (message == null || message.isBlank()) {
            return error.getClass().getSimpleName();
        }
        return message;
    }

    private static String toGoStyleCode(Status.Code code) {
        if (code == null) {
            return "Unknown";
        }
        String name = code.name();
        StringBuilder sb = new StringBuilder();
        boolean upperNext = true;
        for (int i = 0; i < name.length(); i++) {
            char ch = name.charAt(i);
            if (ch == '_') {
                upperNext = true;
                continue;
            }
            if (upperNext) {
                sb.append(Character.toUpperCase(Character.toLowerCase(ch)));
                upperNext = false;
            } else {
                sb.append(Character.toLowerCase(ch));
            }
        }
        String result = sb.toString();
        return result.isBlank() ? "Unknown" : result;
    }
}

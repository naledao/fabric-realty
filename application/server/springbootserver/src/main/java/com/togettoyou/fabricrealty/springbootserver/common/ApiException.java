package com.togettoyou.fabricrealty.springbootserver.common;

public class ApiException extends RuntimeException {
    private final int code;

    public ApiException(int code, String message) {
        super(message);
        this.code = code;
    }

    public int getCode() {
        return code;
    }

    public static ApiException badRequest(String message) {
        return new ApiException(400, message);
    }
}


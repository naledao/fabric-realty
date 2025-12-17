package com.togettoyou.fabricrealty.springbootserver.common;

import com.fasterxml.jackson.annotation.JsonInclude;

public class ApiResponse<T> {
    private final int code;
    private final String message;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private final T data;

    public ApiResponse(int code, String message, T data) {
        this.code = code;
        this.message = message;
        this.data = data;
    }

    public int getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }

    public T getData() {
        return data;
    }

    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(200, "成功", data);
    }

    public static ApiResponse<Void> successMessage(String message) {
        return new ApiResponse<>(200, message, null);
    }

    public static <T> ApiResponse<T> successMessage(String message, T data) {
        return new ApiResponse<>(200, message, data);
    }

    public static ApiResponse<Void> error(int code, String message) {
        return new ApiResponse<>(code, message, null);
    }
}


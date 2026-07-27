package com.example.docsuriserver.common;

/**
 * Apidog 명세 공통 응답 래퍼: { "message": "...", "data": { ... } }
 */
public record ApiResponse<T>(String message, T data) {

    public static <T> ApiResponse<T> of(String message, T data) {
        return new ApiResponse<>(message, data);
    }
}

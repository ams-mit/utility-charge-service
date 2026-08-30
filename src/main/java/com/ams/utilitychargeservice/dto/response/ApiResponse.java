package com.ams.utilitychargeservice.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;

@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {

    private boolean success;
    private String message;
    private T data;
    private ErrorDetail error;
    private String timestamp;
    private String requestId;

    @Getter
    @Builder
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class ErrorDetail {
        private String code;
        private String details;
    }

    public static <T> ApiResponse<T> success(String message, T data, String requestId) {
        return ApiResponse.<T>builder()
                .success(true)
                .message(message)
                .data(data)
                .timestamp(Instant.now().toString())
                .requestId(requestId)
                .build();
    }

    public static <T> ApiResponse<T> error(String message, String code, String requestId) {
        return ApiResponse.<T>builder()
                .success(false)
                .message(message)
                .error(ErrorDetail.builder().code(code).build())
                .timestamp(Instant.now().toString())
                .requestId(requestId)
                .build();
    }
}
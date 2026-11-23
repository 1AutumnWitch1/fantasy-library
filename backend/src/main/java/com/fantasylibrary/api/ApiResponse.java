package com.fantasylibrary.api;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

public record ApiResponse<T>(
        Code code,
        String message,
        @JsonInclude(JsonInclude.Include.NON_NULL) T data,
        @JsonInclude(JsonInclude.Include.NON_EMPTY) List<FieldIssue> errors
) {
    public enum Code {
        OK, INVALID_ARGUMENT, WORK_NOT_FOUND, DOWNLOAD_NOT_FOUND, PLAYBACK_NOT_FOUND, ROUTE_NOT_FOUND, METHOD_NOT_ALLOWED, REQUEST_ERROR,
        DATABASE_UNAVAILABLE, CATALOG_NOT_INITIALIZED, INTERNAL_ERROR, MEDIA_NOT_FOUND, MEDIA_STORAGE_UNAVAILABLE,
        LOGIN_REQUIRED, INVALID_CREDENTIALS, USERNAME_TAKEN, FORBIDDEN, COMMUNITY_NOT_FOUND, COMMUNITY_CLOSED, COMMUNITY_STORAGE_UNAVAILABLE
    }

    public record FieldIssue(String field, String message) {
    }

    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(Code.OK, "请求成功", data, List.of());
    }

    public static ApiResponse<Void> error(Code code, String message, List<FieldIssue> errors) {
        return new ApiResponse<>(code, message, null, List.copyOf(errors));
    }
}

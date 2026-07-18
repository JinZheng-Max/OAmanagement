package com.wut.practicum.common;

import io.swagger.v3.oas.annotations.media.Schema;
import org.slf4j.MDC;

@Schema(description = "统一 API 响应")
public record ApiResult<T>(Integer code, String message, T data, String traceId) {
    public static <T> ApiResult<T> success(T data) {
        return new ApiResult<>(200, "操作成功", data, MDC.get("traceId"));
    }
    public static <T> ApiResult<T> success(T data, String message) {
        return new ApiResult<>(200, message, data, MDC.get("traceId"));
    }
    public static <T> ApiResult<T> error(int code, String message) {
        return new ApiResult<>(code, message, null, MDC.get("traceId"));
    }
}

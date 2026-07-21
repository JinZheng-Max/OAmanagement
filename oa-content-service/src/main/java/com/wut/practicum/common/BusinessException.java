package com.wut.practicum.common;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class BusinessException extends RuntimeException {
    private final int code;
    private final HttpStatus status;
    private final Object data;

    public BusinessException(int code, HttpStatus status, String message) {
        this(code, status, message, null);
    }

    public BusinessException(int code, HttpStatus status, String message, Object data) {
        super(message);
        this.code = code;
        this.status = status;
        this.data = data;
    }
}

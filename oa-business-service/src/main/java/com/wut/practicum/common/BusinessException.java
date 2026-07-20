package com.wut.practicum.common;

import org.springframework.http.HttpStatus;

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

    public int getCode() { return code; }
    public HttpStatus getStatus() { return status; }
    public Object getData() { return data; }
}

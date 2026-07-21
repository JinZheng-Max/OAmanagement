package com.wut.practicum.dto;

public record PageQuery(int page, int size) {
    public int offset() { return (page - 1) * size; }
}

package com.wut.practicum.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 通用分页查询参数
 *
 * Java 16+ 的 record 是一种特殊类，自动生成构造器、getter、equals、hashCode
 * 紧凑构造器（compact constructor）可以在赋值前校验/设置默认值
 *
 * @param page 页码（从1开始）
 * @param size 每页条数
 */
@Schema(description = "分页查询参数")
public record PageQuery(
        @Schema(description = "页码，从1开始", example = "1") Integer page,
        @Schema(description = "每页条数", example = "10") Integer size) {

    /** 紧凑构造器：参数的默认值处理 */
    public PageQuery {
        if (page == null || page < 1) page = 1;
        if (size == null || size < 1) size = 10;
        // 限制最大100条，防止有人恶意拉取大量数据
        if (size > 100) size = 100;
    }

    /** 获取分页偏移量（供 SQL 的 LIMIT OFFSET 使用） */
    public int offset() {
        return (page - 1) * size;
    }
}

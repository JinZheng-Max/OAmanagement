package com.wut.practicum.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

/**
 * 通用分页结果
 *
 * 对应 API 文档中的分页响应格式：
 * { "records": [...], "total": 100, "size": 10, "current": 1, "pages": 10 }
 *
 * @param <T> 列表中的数据类型
 */
@Schema(description = "分页结果")
public record PageResult<T>(
        @Schema(description = "数据列表") List<T> records,
        @Schema(description = "总记录数") long total,
        @Schema(description = "每页条数") int size,
        @Schema(description = "当前页码") int current,
        @Schema(description = "总页数") long pages) {

    /**
     * 工厂方法：根据查询结果构造分页对象
     *
     * @param records 当前页的数据
     * @param total   总记录数
     * @param query   原来的分页查询参数
     */
    public static <T> PageResult<T> of(List<T> records, long total, PageQuery query) {
        long pages = (total + query.size() - 1) / query.size(); // 向上取整计算总页数
        return new PageResult<>(records, total, query.size(), query.page(), pages);
    }
}

package com.wut.practicum.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ContentSaveDTO {
    private Long id;

    @NotBlank(message = "内容类型不能为空(ANNOUNCEMENT/POLICY)")
    private String type;

    @NotBlank(message = "标题不能为空")
    @Size(max = 200, message = "标题不能超过200字符")
    private String title;

    private String category;

    @NotBlank(message = "正文内容不能为空")
    private String body;

    private String scope = "ALL"; // ALL / DEPARTMENT

    private Long accessDepartmentId;
}

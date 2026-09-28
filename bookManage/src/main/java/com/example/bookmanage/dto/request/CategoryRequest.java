package com.example.bookmanage.dto.request;

import com.example.bookmanage.enums.CategoryStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 分类相关请求参数。
 */
public final class CategoryRequest {

    private CategoryRequest() {
    }

    public record Create(
            @NotBlank(message = "分类名称不能为空")
            @Size(max = 50, message = "分类名称长度不能超过 50 个字符")
            String name,

            @Size(max = 500, message = "分类描述长度不能超过 500 个字符")
            String description,

            Integer sortOrder,
            CategoryStatus status) {
    }

    /** 分类更新为全量覆盖，未传字段置空即可 */
    public record Update(
            @Size(max = 50, message = "分类名称长度不能超过 50 个字符")
            String name,

            @Size(max = 500, message = "分类描述长度不能超过 500 个字符")
            String description,

            Integer sortOrder,
            CategoryStatus status) {
    }
}

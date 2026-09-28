package com.example.bookmanage.dto.request;

import com.example.bookmanage.enums.BookStatus;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

/**
 * 图书相关请求参数。
 */
public final class BookRequest {

    private BookRequest() {
    }

    public record Create(
            @NotBlank(message = "书名不能为空")
            @Size(max = 200, message = "书名长度不能超过 200 个字符")
            String title,

            @NotBlank(message = "作者不能为空")
            @Size(max = 100, message = "作者长度不能超过 100 个字符")
            String author,

            @NotBlank(message = "ISBN 不能为空")
            @Size(min = 10, max = 20, message = "ISBN 长度需为 10 到 20 个字符")
            String isbn,

            @Size(max = 100) String publisher,
            LocalDate publishDate,
            @Size(max = 5000) String description,
            @Size(max = 500) String coverUrl,

            @NotNull(message = "必须指定图书分类")
            @Min(value = 1, message = "分类 ID 不合法")
            Long categoryId,

            @NotNull(message = "必须指定馆藏数量")
            @Min(value = 0, message = "馆藏数量不能为负数")
            @Max(value = 100000, message = "馆藏数量超出上限")
            Integer totalStock,

            BookStatus status) {
    }

    /** 书目信息更新不含库存：库存变化必须走库存调整接口并留痕 */
    public record Update(
            @Size(max = 200) String title,
            @Size(max = 100) String author,
            @Size(min = 10, max = 20) String isbn,
            @Size(max = 100) String publisher,
            LocalDate publishDate,
            @Size(max = 5000) String description,
            @Size(max = 500) String coverUrl,
            @Min(value = 1) Long categoryId,
            BookStatus status) {
    }

    /** change 为正表示入库、为负表示出库，不能为 0（0 在 Service 层校验并返回 422） */
    public record StockAdjust(
            @NotNull(message = "调整数量不能为空")
            @Min(value = -100000, message = "单次调整数量超出下限")
            @Max(value = 100000, message = "单次调整数量超出上限")
            Integer change,

            @NotBlank(message = "调整原因不能为空")
            @Size(min = 2, max = 200, message = "调整原因长度需为 2 到 200 个字符")
            String reason) {
    }
}

package com.example.bookmanage.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.OffsetDateTime;

/**
 * 借阅流程相关请求参数。
 */
public final class OrderRequest {

    private OrderRequest() {
    }

    public record Reserve(
            @NotNull(message = "必须指定图书")
            Long bookId,
            @Size(max = 200) String remark) {
    }

    /** 拒绝时 remark 必填，该规则在 Service 层校验，注解无法表达条件必填 */
    public record Audit(
            @NotBlank(message = "审核结论不能为空")
            String decision,
            @Size(max = 200) String remark) {
    }

    /** dueAt 为空时由系统按默认借阅天数计算应还时间 */
    public record Checkout(OffsetDateTime dueAt, @Size(max = 200) String remark) {
    }

    public record Return(@Size(max = 200) String remark) {
    }

    /** 损坏或遗失时 remark 必填，同样在 Service 层校验 */
    public record ConfirmReturn(
            @NotBlank(message = "验收结果不能为空")
            String condition,
            @Size(max = 500) String remark) {
    }
}

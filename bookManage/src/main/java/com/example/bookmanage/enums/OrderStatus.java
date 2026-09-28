package com.example.bookmanage.enums;

import java.util.Set;

/**
 * 借阅单状态。
 *
 * <p>OVERDUE 不落库：它由 BORROWED + due_at 过期推导得出，
 * 避免定时任务未执行时状态滞后于真实逾期情况。
 */
public enum OrderStatus {
    PENDING,
    APPROVED,
    REJECTED,
    BORROWED,
    RETURN_REQUESTED,
    RETURNED,
    CANCELLED,
    OVERDUE;

    /** 已终结状态：不再占用可借库存 */
    private static final Set<OrderStatus> FINISHED =
            Set.of(REJECTED, CANCELLED, RETURNED);

    /** 未终结状态：同一用户对同一本书只允许存在一个未终结借阅单 */
    private static final Set<OrderStatus> UNFINISHED =
            Set.of(PENDING, APPROVED, BORROWED, RETURN_REQUESTED, OVERDUE);

    public boolean isFinished() {
        return FINISHED.contains(this);
    }

    public boolean isUnfinished() {
        return UNFINISHED.contains(this);
    }
}

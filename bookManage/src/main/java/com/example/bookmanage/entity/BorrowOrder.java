package com.example.bookmanage.entity;

import com.example.bookmanage.enums.OrderStatus;
import com.example.bookmanage.enums.ReturnCondition;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 借阅单，对应表 b_borrow_order。
 */
@Data
public class BorrowOrder {

    private Long id;
    private String orderNo;
    private Long userId;
    private Long bookId;
    private OrderStatus status;
    private String remark;
    private String auditRemark;
    /** 读者发起归还时的说明 */
    private String returnRemark;
    private ReturnCondition returnCondition;
    private LocalDateTime reservedAt;
    private LocalDateTime approvedAt;
    private LocalDateTime borrowedAt;
    private LocalDateTime dueAt;
    private LocalDateTime returnedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

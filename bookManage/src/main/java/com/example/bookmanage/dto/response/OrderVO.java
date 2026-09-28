package com.example.bookmanage.dto.response;

import com.example.bookmanage.dto.response.BriefVO.BookBrief;
import com.example.bookmanage.dto.response.BriefVO.UserBrief;
import com.example.bookmanage.enums.OrderStatus;
import com.example.bookmanage.enums.ReturnCondition;

import java.time.LocalDateTime;

/**
 * 借阅单视图。
 */
public record OrderVO(Long id,
                      String orderNo,
                      UserBrief user,
                      BookBrief book,
                      OrderStatus status,
                      String remark,
                      String auditRemark,
                      ReturnCondition returnCondition,
                      LocalDateTime reservedAt,
                      LocalDateTime approvedAt,
                      LocalDateTime borrowedAt,
                      LocalDateTime dueAt,
                      LocalDateTime returnedAt,
                      LocalDateTime createdAt,
                      LocalDateTime updatedAt) {
}

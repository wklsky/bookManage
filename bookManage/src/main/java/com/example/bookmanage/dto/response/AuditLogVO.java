package com.example.bookmanage.dto.response;

import com.example.bookmanage.enums.AuditAction;
import com.example.bookmanage.enums.AuditTargetType;

import java.time.LocalDateTime;

/**
 * 管理员操作审计日志视图。
 *
 * <p>actionLabel 由后端枚举直接给出，前端列表无需再维护一份英文枚举到中文的映射。
 */
public record AuditLogVO(Long id,
                         Long operatorId,
                         String operatorName,
                         AuditAction action,
                         String actionLabel,
                         AuditTargetType targetType,
                         String targetId,
                         String summary,
                         String ip,
                         LocalDateTime createdAt) {
}

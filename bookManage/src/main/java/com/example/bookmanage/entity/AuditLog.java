package com.example.bookmanage.entity;

import com.example.bookmanage.enums.AuditAction;
import com.example.bookmanage.enums.AuditTargetType;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 管理员操作审计日志，对应表 sys_audit_log。
 */
@Data
public class AuditLog {

    private Long id;
    private Long operatorId;
    /** 操作人账号快照：账号改名或删除后仍能还原当时的操作人，故冗余存储而非联表查 */
    private String operatorName;
    private AuditAction action;
    private AuditTargetType targetType;
    private String targetId;
    private String summary;
    private String ip;
    private LocalDateTime createdAt;
}

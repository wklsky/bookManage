package com.example.bookmanage.service;

import com.example.bookmanage.common.PageResult;
import com.example.bookmanage.common.Paging;
import com.example.bookmanage.dto.response.AuditLogVO;
import com.example.bookmanage.entity.AuditLog;
import com.example.bookmanage.enums.AuditAction;
import com.example.bookmanage.enums.AuditTargetType;
import com.example.bookmanage.mapper.AuditLogMapper;
import com.example.bookmanage.security.SecurityUtils;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.List;

/**
 * 管理员操作留痕。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuditLogService {

    private final AuditLogMapper auditLogMapper;

    /**
     * 记录一条操作日志。
     *
     * <p>刻意使用默认事务传播：审计应与业务结果的成败保持一致，
     * 若业务回滚而审计单独提交（REQUIRES_NEW），就会留下一条"发生过但没生效"的假记录。
     *
     * <p>同时吞掉写入异常：审计是旁路能力，它失败不应该把正常的业务操作一起拖垮。
     */
    public void record(AuditAction action, AuditTargetType targetType, String targetId, String summary) {
        try {
            AuditLog entry = new AuditLog();
            var operator = SecurityUtils.currentUser();
            entry.setOperatorId(operator.getId());
            entry.setOperatorName(operator.getUsername());
            entry.setAction(action);
            entry.setTargetType(targetType);
            entry.setTargetId(targetId);
            entry.setSummary(summary);
            entry.setIp(resolveClientIp());
            auditLogMapper.insert(entry);
        } catch (RuntimeException ex) {
            log.error("写入审计日志失败，action={}, targetId={}", action, targetId, ex);
        }
    }

    public PageResult<AuditLogVO> list(int page, int size, String keyword, AuditAction action, Long operatorId) {
        Paging.check(page, size);
        long total = auditLogMapper.countPage(keyword, action, operatorId);
        List<AuditLog> logs = auditLogMapper.selectPage(keyword, action, operatorId,
                Paging.offset(page, size), size);
        return PageResult.of(page, size, total, logs.stream().map(this::toVO).toList());
    }

    private AuditLogVO toVO(AuditLog entry) {
        AuditAction action = entry.getAction();
        return new AuditLogVO(entry.getId(), entry.getOperatorId(), entry.getOperatorName(), action,
                action == null ? null : action.label(), entry.getTargetType(), entry.getTargetId(),
                entry.getSummary(), entry.getIp(), entry.getCreatedAt());
    }

    private String resolveClientIp() {
        // 审计也可能在非 Web 上下文被触发（例如后续接入定时任务），此时取不到请求，IP 留空即可
        if (!(RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attributes)) {
            return null;
        }
        HttpServletRequest request = attributes.getRequest();
        // 经过反向代理后 remoteAddr 只会是网关地址，真实来源在 X-Forwarded-For 首段。
        // 注意：该头可被客户端伪造，仅在可信代理之后才应采信；此处只用于留痕，不参与任何鉴权判断。
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",", 2)[0].trim();
        }
        return request.getRemoteAddr();
    }
}

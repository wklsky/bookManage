package com.example.bookmanage.controller;

import com.example.bookmanage.common.PageResult;
import com.example.bookmanage.common.R;
import com.example.bookmanage.dto.response.AuditLogVO;
import com.example.bookmanage.enums.AuditAction;
import com.example.bookmanage.service.AuditLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 管理后台：管理员操作审计日志查询。
 */
@RestController
@RequestMapping("/api/admin/audit-logs")
@RequiredArgsConstructor
public class AdminAuditLogsController {

    private final AuditLogService auditLogService;

    /**
     * @param keyword    匹配操作人账号、摘要或对象标识
     * @param operatorId 单独按操作人过滤，用于回溯某个管理员的全部操作
     */
    @GetMapping
    public R<PageResult<AuditLogVO>> list(@RequestParam(defaultValue = "1") int page,
                                          @RequestParam(defaultValue = "10") int size,
                                          @RequestParam(required = false) String keyword,
                                          @RequestParam(required = false) AuditAction action,
                                          @RequestParam(required = false) Long operatorId) {
        return R.ok(auditLogService.list(page, size, keyword, action, operatorId));
    }
}

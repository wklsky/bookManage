package com.example.bookmanage.enums;

/**
 * 需要留痕的管理员操作类型。
 *
 * <p>label 供管理后台列表直接展示，避免前端再维护一份枚举到中文的映射。
 */
public enum AuditAction {

    FEATURED_CREATE("新增推荐位"),
    FEATURED_UPDATE("修改推荐位"),
    FEATURED_DELETE("移除推荐位"),
    SITE_SETTING_UPDATE("修改站点展示配置"),
    USER_ACCESS_UPDATE("调整用户角色或状态"),
    USER_PASSWORD_RESET("重置用户密码"),
    BOOK_STATUS_UPDATE("变更图书展示状态");

    private final String label;

    AuditAction(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }
}

package com.example.bookmanage.enums;

/**
 * 审计日志的操作对象类型。
 */
public enum AuditTargetType {
    /** 馆藏图书 */
    BOOK,
    /** 首页推荐位 */
    FEATURED,
    /** 站点展示配置 */
    SITE,
    /** 系统用户 */
    USER
}

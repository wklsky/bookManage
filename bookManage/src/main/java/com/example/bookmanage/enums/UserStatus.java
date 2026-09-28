package com.example.bookmanage.enums;

/**
 * 用户账号状态。非 ACTIVE 的用户即使持有有效令牌也必须被拒绝访问。
 */
public enum UserStatus {
    ACTIVE,
    DISABLED,
    LOCKED
}

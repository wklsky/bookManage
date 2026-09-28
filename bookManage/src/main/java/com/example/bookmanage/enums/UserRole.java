package com.example.bookmanage.enums;

/**
 * 系统角色。与 swagger.json 的 x-roles 保持一致。
 */
public enum UserRole {
    /** 读者：检索、预约、归还 */
    READER,
    /** 图书管理员：馆藏与借阅流程维护 */
    LIBRARIAN,
    /** 系统管理员：图书管理员权限 + 用户管理 */
    ADMIN
}

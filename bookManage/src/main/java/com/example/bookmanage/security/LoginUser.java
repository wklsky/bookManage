package com.example.bookmanage.security;

import com.example.bookmanage.entity.SysUser;
import com.example.bookmanage.enums.UserRole;
import com.example.bookmanage.enums.UserStatus;
import lombok.Data;

/**
 * 当前登录主体，存放在 SecurityContext 中。
 */
@Data
public class LoginUser {

    private Long id;
    private String username;
    private UserRole role;
    private UserStatus status;

    public boolean isManager() {
        return role == UserRole.LIBRARIAN || role == UserRole.ADMIN;
    }

    public boolean isAdmin() {
        return role == UserRole.ADMIN;
    }

    public static LoginUser from(SysUser user) {
        LoginUser loginUser = new LoginUser();
        loginUser.setId(user.getId());
        loginUser.setUsername(user.getUsername());
        loginUser.setRole(user.getRole());
        loginUser.setStatus(user.getStatus());
        return loginUser;
    }
}

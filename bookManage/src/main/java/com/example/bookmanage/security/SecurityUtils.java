package com.example.bookmanage.security;

import com.example.bookmanage.exception.BizException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * 获取当前登录主体的便捷入口。
 */
public final class SecurityUtils {

    private SecurityUtils() {
    }

    public static LoginUser currentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof LoginUser loginUser)) {
            throw BizException.unauthorized("未登录或访问令牌已失效");
        }
        return loginUser;
    }

    public static Long currentUserId() {
        return currentUser().getId();
    }
}

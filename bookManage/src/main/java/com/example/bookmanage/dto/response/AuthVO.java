package com.example.bookmanage.dto.response;

/**
 * 登录与注册响应：令牌对 + 当前用户资料。
 */
public record AuthVO(String tokenType, String accessToken, String refreshToken, long expiresIn, UserVO user) {
}

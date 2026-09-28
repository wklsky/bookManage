package com.example.bookmanage.dto.response;

/**
 * 令牌对。expiresIn 为访问令牌剩余有效秒数。
 */
public record TokenVO(String tokenType, String accessToken, String refreshToken, long expiresIn) {
}

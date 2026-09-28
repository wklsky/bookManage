package com.example.bookmanage.entity;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 刷新令牌记录，对应表 sys_refresh_token。
 *
 * <p>退出登录需要让已签发的刷新令牌立即失效，纯无状态 JWT 无法满足，
 * 因此把 jti 落库，刷新时校验其存在性与撤销标记。
 */
@Data
public class RefreshToken {

    private Long id;
    private Long userId;
    private String tokenId;
    private Boolean revoked;
    private LocalDateTime expiresAt;
    private LocalDateTime createdAt;
}

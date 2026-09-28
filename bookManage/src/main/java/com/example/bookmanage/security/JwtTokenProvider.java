package com.example.bookmanage.security;

import com.example.bookmanage.config.BookProperties;
import com.example.bookmanage.enums.UserRole;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

/**
 * 访问令牌与刷新令牌的签发、解析。
 *
 * <p>两类令牌用 typ 声明区分：刷新令牌不能用于访问业务接口，
 * 访问令牌也不能用于刷新，避免令牌用途串用导致越权。
 */
@Component
public class JwtTokenProvider {

    private static final String CLAIM_TYPE = "typ";
    private static final String CLAIM_ROLE = "role";
    private static final String TYPE_ACCESS = "access";
    private static final String TYPE_REFRESH = "refresh";

    private final SecretKey key;
    private final BookProperties properties;

    public JwtTokenProvider(BookProperties properties) {
        this.properties = properties;
        this.key = Keys.hmacShaKeyFor(properties.getJwt().getSecret().getBytes(StandardCharsets.UTF_8));
    }

    public String createAccessToken(LoginUser user) {
        return buildToken(user, TYPE_ACCESS, null, properties.getJwt().getAccessTokenTtl());
    }

    public String createRefreshToken(LoginUser user) {
        return createRefreshToken(user, UUID.randomUUID().toString());
    }

    /** 指定 jti 的刷新令牌签发，签发方需要把 jti 落库以便后续撤销 */
    public String createRefreshToken(LoginUser user, String tokenId) {
        return buildToken(user, TYPE_REFRESH, tokenId, properties.getJwt().getRefreshTokenTtl());
    }

    /** 访问令牌剩余有效秒数，供前端判断何时需要静默刷新 */
    public long accessTokenExpiresIn() {
        return properties.getJwt().getAccessTokenTtl().toSeconds();
    }

    public long refreshTokenExpiresIn() {
        return properties.getJwt().getRefreshTokenTtl().toSeconds();
    }

    /** 解析访问令牌；过期、签名不符或类型不符均抛出 JwtException */
    public Claims parseAccessToken(String token) {
        Claims claims = parse(token);
        if (!TYPE_ACCESS.equals(claims.get(CLAIM_TYPE, String.class))) {
            throw new JwtException("令牌类型不匹配");
        }
        return claims;
    }

    /** 解析刷新令牌；过期、签名不符或类型不符均抛出 JwtException */
    public Claims parseRefreshToken(String token) {
        Claims claims = parse(token);
        if (!TYPE_REFRESH.equals(claims.get(CLAIM_TYPE, String.class))) {
            throw new JwtException("令牌类型不匹配");
        }
        return claims;
    }

    public Long extractUserId(Claims claims) {
        return Long.valueOf(claims.getSubject());
    }

    public UserRole extractRole(Claims claims) {
        String role = claims.get(CLAIM_ROLE, String.class);
        return role == null ? null : UserRole.valueOf(role);
    }

    private Claims parse(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private String buildToken(LoginUser user, String type, String tokenId, Duration ttl) {
        Instant now = Instant.now();
        var builder = Jwts.builder()
                .subject(String.valueOf(user.getId()))
                .claim(CLAIM_TYPE, type)
                .claim(CLAIM_ROLE, user.getRole() == null ? null : user.getRole().name())
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusMillis(ttl.toMillis())))
                .signWith(key);
        if (tokenId != null) {
            builder.id(tokenId);
        }
        return builder.compact();
    }
}

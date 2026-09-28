package com.example.bookmanage.service;

import com.example.bookmanage.config.BookProperties;
import com.example.bookmanage.dto.request.AuthRequest;
import com.example.bookmanage.dto.response.AuthVO;
import com.example.bookmanage.dto.response.TokenVO;
import com.example.bookmanage.entity.RefreshToken;
import com.example.bookmanage.entity.SysUser;
import com.example.bookmanage.enums.UserRole;
import com.example.bookmanage.enums.UserStatus;
import com.example.bookmanage.exception.BizException;
import com.example.bookmanage.mapper.RefreshTokenMapper;
import com.example.bookmanage.mapper.SysUserMapper;
import com.example.bookmanage.security.JwtTokenProvider;
import com.example.bookmanage.security.LoginUser;
import com.example.bookmanage.support.ViewAssembler;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 注册、登录、令牌刷新与退出登录。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private static final String TOKEN_TYPE = "Bearer";

    private final SysUserMapper userMapper;
    private final RefreshTokenMapper refreshTokenMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider tokenProvider;
    private final BookProperties properties;

    /** 注册账号固定为读者与启用状态，避免接口被利用来直接提权 */
    @Transactional
    public AuthVO register(AuthRequest.Register request) {
        if (userMapper.countByUsername(request.username()) > 0) {
            throw BizException.conflict("用户名已被占用");
        }
        if (userMapper.countByEmail(request.email()) > 0) {
            throw BizException.conflict("邮箱已被占用");
        }

        SysUser user = new SysUser();
        user.setUsername(request.username());
        user.setPassword(passwordEncoder.encode(request.password()));
        user.setEmail(request.email());
        user.setNickname(request.nickname());
        user.setPhone(request.phone());
        user.setRole(UserRole.READER);
        user.setStatus(UserStatus.ACTIVE);
        userMapper.insert(user);

        return issueTokens(user);
    }

    /** 登录同样要签发并落库刷新令牌，需要事务保证写入，与 register / refresh 保持一致 */
    @Transactional
    public AuthVO login(AuthRequest.Login request) {
        SysUser user = userMapper.selectByUsernameOrEmail(request.account());
        // 账号不存在与密码错误返回同一提示，避免暴露账号是否注册
        if (user == null || !passwordEncoder.matches(request.password(), user.getPassword())) {
            throw BizException.unauthorized("账号或密码错误");
        }
        if (user.getStatus() == UserStatus.DISABLED) {
            throw BizException.forbidden("账号已被禁用，请联系管理员");
        }
        if (user.getStatus() == UserStatus.LOCKED) {
            throw BizException.forbidden("账号已被锁定，请联系管理员");
        }
        return issueTokens(user);
    }

    @Transactional
    public TokenVO refresh(AuthRequest.Refresh request) {
        Claims claims;
        try {
            claims = tokenProvider.parseRefreshToken(request.refreshToken());
        } catch (JwtException ex) {
            throw BizException.unauthorized("刷新令牌无效或已过期");
        }

        RefreshToken stored = refreshTokenMapper.selectByTokenId(claims.getId());
        if (stored == null || Boolean.TRUE.equals(stored.getRevoked())
                || stored.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw BizException.unauthorized("刷新令牌无效或已过期");
        }

        SysUser user = userMapper.selectById(stored.getUserId());
        if (user == null || user.getStatus() != UserStatus.ACTIVE) {
            throw BizException.unauthorized("账号状态异常，请重新登录");
        }

        // 刷新令牌一次性使用：轮换后旧令牌立即失效，降低泄漏后被重放的风险
        refreshTokenMapper.revokeByTokenId(claims.getId());

        LoginUser loginUser = LoginUser.from(user);
        String tokenId = UUID.randomUUID().toString();
        String refreshToken = tokenProvider.createRefreshToken(loginUser, tokenId);
        persistRefreshToken(user.getId(), tokenId);

        return new TokenVO(TOKEN_TYPE, tokenProvider.createAccessToken(loginUser), refreshToken,
                tokenProvider.accessTokenExpiresIn());
    }

    /** 退出登录需幂等：令牌本身非法时也要返回成功，否则前端无法完成清理 */
    @Transactional
    public void logout(AuthRequest.Logout request) {
        try {
            Claims claims = tokenProvider.parseRefreshToken(request.refreshToken());
            refreshTokenMapper.revokeByTokenId(claims.getId());
        } catch (JwtException ex) {
            log.debug("退出登录时刷新令牌无法解析，视为已失效: {}", ex.getMessage());
        }
    }

    private AuthVO issueTokens(SysUser user) {
        LoginUser loginUser = LoginUser.from(user);
        String tokenId = UUID.randomUUID().toString();
        String refreshToken = tokenProvider.createRefreshToken(loginUser, tokenId);
        persistRefreshToken(user.getId(), tokenId);

        return new AuthVO(TOKEN_TYPE, tokenProvider.createAccessToken(loginUser), refreshToken,
                tokenProvider.accessTokenExpiresIn(), ViewAssembler.toUserVO(user));
    }

    private void persistRefreshToken(Long userId, String tokenId) {
        RefreshToken record = new RefreshToken();
        record.setUserId(userId);
        record.setTokenId(tokenId);
        record.setRevoked(false);
        record.setExpiresAt(LocalDateTime.now().plus(properties.getJwt().getRefreshTokenTtl()));
        refreshTokenMapper.insert(record);
    }
}

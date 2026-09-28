package com.example.bookmanage.security;

import com.example.bookmanage.entity.SysUser;
import com.example.bookmanage.enums.UserStatus;
import com.example.bookmanage.mapper.SysUserMapper;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * 解析 Bearer 令牌并装载登录主体。
 *
 * <p>每次请求都回查用户：角色与状态可能在令牌签发后被管理员修改，
 * 只信任令牌里的声明会让停用操作延迟到令牌过期才生效。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtTokenProvider tokenProvider;
    private final SysUserMapper userMapper;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (header != null && header.startsWith(BEARER_PREFIX)) {
            String token = header.substring(BEARER_PREFIX.length());
            try {
                Claims claims = tokenProvider.parseAccessToken(token);
                Long userId = tokenProvider.extractUserId(claims);
                SysUser user = userMapper.selectById(userId);
                if (user != null && user.getStatus() == UserStatus.ACTIVE) {
                    authenticate(user);
                }
            } catch (JwtException | NumberFormatException ex) {
                // 令牌无效时保持匿名：交由后续鉴权决定是否返回 401，
                // 这样 permitAll 的接口不会因为携带了过期令牌而被拒绝。
                log.debug("访问令牌解析失败: {}", ex.getMessage());
            }
        }
        filterChain.doFilter(request, response);
    }

    private void authenticate(SysUser user) {
        LoginUser loginUser = LoginUser.from(user);
        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                loginUser, null, List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name())));
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);
    }
}

package com.example.bookmanage.controller;

import com.example.bookmanage.common.R;
import com.example.bookmanage.dto.request.AuthRequest;
import com.example.bookmanage.dto.response.AuthVO;
import com.example.bookmanage.dto.response.TokenVO;
import com.example.bookmanage.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 认证接口：注册、登录、令牌刷新与退出登录，均不需要携带访问令牌。
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<R<AuthVO>> register(@Valid @RequestBody AuthRequest.Register request) {
        return ResponseEntity.status(201).body(R.created(authService.register(request)));
    }

    @PostMapping("/login")
    public R<AuthVO> login(@Valid @RequestBody AuthRequest.Login request) {
        return R.ok(authService.login(request));
    }

    /** 前端在收到 401 时用该接口静默换发访问令牌 */
    @PostMapping("/refresh")
    public R<TokenVO> refresh(@Valid @RequestBody AuthRequest.Refresh request) {
        return R.ok(authService.refresh(request));
    }

    @PostMapping("/logout")
    public R<Void> logout(@Valid @RequestBody AuthRequest.Logout request) {
        authService.logout(request);
        return R.ok(null);
    }
}

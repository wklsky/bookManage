package com.example.bookmanage.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 认证相关请求参数。字段名与 swagger.json 的定义一一对应。
 */
public final class AuthRequest {

    private AuthRequest() {
    }

    public record Register(
            @NotBlank(message = "用户名不能为空")
            @Size(min = 4, max = 32, message = "用户名长度需为 4 到 32 个字符")
            @Pattern(regexp = "^[A-Za-z0-9_]+$", message = "用户名只能包含字母、数字和下划线")
            String username,

            @NotBlank(message = "密码不能为空")
            @Size(min = 8, max = 64, message = "密码长度需为 8 到 64 个字符")
            String password,

            @NotBlank(message = "邮箱不能为空")
            @Email(message = "邮箱格式不正确")
            @Size(max = 100, message = "邮箱长度不能超过 100 个字符")
            String email,

            @Size(min = 1, max = 50, message = "昵称长度需为 1 到 50 个字符")
            String nickname,

            @Size(max = 20, message = "手机号长度不能超过 20 个字符")
            String phone) {
    }

    /** account 既可能是用户名也可能是邮箱，后端需同时匹配 */
    public record Login(
            @NotBlank(message = "登录账号不能为空")
            @Size(min = 4, max = 100, message = "登录账号长度不合法")
            String account,

            @NotBlank(message = "密码不能为空")
            @Size(min = 8, max = 64, message = "密码长度不合法")
            String password) {
    }

    public record Refresh(
            @NotBlank(message = "刷新令牌不能为空")
            @Size(min = 20, message = "刷新令牌格式不合法")
            String refreshToken) {
    }

    public record Logout(
            @NotBlank(message = "刷新令牌不能为空")
            @Size(min = 20, message = "刷新令牌格式不合法")
            String refreshToken) {
    }
}

package com.example.bookmanage.dto.request;

import com.example.bookmanage.enums.UserRole;
import com.example.bookmanage.enums.UserStatus;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 用户相关请求参数。
 */
public final class UserRequest {

    private UserRequest() {
    }

    /** 只允许修改资料字段，用户名 / 角色 / 状态不可由本人修改 */
    public record ProfileUpdate(
            @Size(min = 1, max = 50, message = "昵称长度需为 1 到 50 个字符")
            String nickname,

            @Email(message = "邮箱格式不正确")
            @Size(max = 100, message = "邮箱长度不能超过 100 个字符")
            String email,

            @Size(max = 20, message = "手机号长度不能超过 20 个字符")
            String phone,

            @Size(max = 500, message = "头像地址长度不能超过 500 个字符")
            String avatarUrl) {
    }

    public record PasswordChange(
            @NotBlank(message = "当前密码不能为空")
            @Size(min = 8, max = 64, message = "当前密码长度不合法")
            String oldPassword,

            @NotBlank(message = "新密码不能为空")
            @Size(min = 8, max = 64, message = "新密码长度需为 8 到 64 个字符")
            String newPassword) {
    }

    public record AccessUpdate(UserRole role, UserStatus status,
                               @Size(max = 200, message = "备注长度不能超过 200 个字符") String remark) {
    }
}

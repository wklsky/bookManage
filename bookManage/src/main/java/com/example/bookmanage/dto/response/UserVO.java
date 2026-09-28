package com.example.bookmanage.dto.response;

import com.example.bookmanage.enums.UserRole;
import com.example.bookmanage.enums.UserStatus;

import java.time.LocalDateTime;

/**
 * 用户视图。刻意不包含 password 字段，避免任何路径下泄露密码散列。
 */
public record UserVO(Long id,
                     String username,
                     String nickname,
                     String email,
                     String phone,
                     String avatarUrl,
                     UserRole role,
                     UserStatus status,
                     LocalDateTime createdAt,
                     LocalDateTime updatedAt) {
}

package com.example.bookmanage.entity;

import com.example.bookmanage.enums.UserRole;
import com.example.bookmanage.enums.UserStatus;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 系统用户，对应表 sys_user。
 */
@Data
public class SysUser {

    private Long id;
    private String username;
    /** BCrypt 密文，永不向外序列化 */
    private String password;
    private String email;
    private String nickname;
    private String phone;
    private String avatarUrl;
    private UserRole role;
    private UserStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

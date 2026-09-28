package com.example.bookmanage.controller;

import com.example.bookmanage.common.PageResult;
import com.example.bookmanage.common.R;
import com.example.bookmanage.dto.request.UserRequest;
import com.example.bookmanage.dto.response.UserVO;
import com.example.bookmanage.enums.UserRole;
import com.example.bookmanage.enums.UserStatus;
import com.example.bookmanage.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 个人中心与用户管理接口。
 */
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping("/profile")
    public R<UserVO> profile() {
        return R.ok(userService.currentProfile());
    }

    @PutMapping("/profile")
    public R<UserVO> updateProfile(@Valid @RequestBody UserRequest.ProfileUpdate request) {
        return R.ok(userService.updateProfile(request));
    }

    @PutMapping("/profile/password")
    public R<Void> changePassword(@Valid @RequestBody UserRequest.PasswordChange request) {
        userService.changePassword(request);
        return R.ok(null);
    }

    @GetMapping
    public R<PageResult<UserVO>> list(@RequestParam(defaultValue = "1") int page,
                          @RequestParam(defaultValue = "10") int size,
                          @RequestParam(required = false) String keyword,
                          @RequestParam(required = false) UserRole role,
                          @RequestParam(required = false) UserStatus status,
                          @RequestParam(required = false) String sort) {
        return R.ok(userService.list(page, size, keyword, role, status, sort));
    }

    @GetMapping("/{id}")
    public R<UserVO> detail(@PathVariable Long id) {
        return R.ok(userService.getById(id));
    }

    @PutMapping("/{id}/status")
    public R<UserVO> updateAccess(@PathVariable Long id,
                                  @Valid @RequestBody UserRequest.AccessUpdate request) {
        return R.ok(userService.updateAccess(id, request));
    }
}

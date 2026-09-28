package com.example.bookmanage.controller;

import com.example.bookmanage.common.PageResult;
import com.example.bookmanage.common.R;
import com.example.bookmanage.dto.request.UserRequest;
import com.example.bookmanage.dto.response.OrderVO;
import com.example.bookmanage.enums.OrderStatus;
import com.example.bookmanage.service.OrderService;
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
 * 管理后台增强的用户管理能力。
 *
 * <p>单独挂在 /api/admin/users 下而不是复用 /api/users/{id}/... ：
 * {@code /api/users/profile/password}（本人改密）与 {@code /api/users/{id}/password}
 * 的模式会互相歧义，拆开后两条规则各自明确，也便于分别授予 ADMIN 权限。
 */
@RestController
@RequestMapping("/api/admin/users")
@RequiredArgsConstructor
public class AdminUserController {

    private final UserService userService;
    private final OrderService orderService;

    @PutMapping("/{id}/password")
    public R<Void> resetPassword(@PathVariable Long id,
                                 @Valid @RequestBody UserRequest.PasswordReset request) {
        userService.resetPassword(id, request);
        return R.ok(null);
    }

    /** 处理申诉或核对在借情况时，需要直接看某个读者名下全部借阅单 */
    @GetMapping("/{id}/orders")
    public R<PageResult<OrderVO>> listOrders(@PathVariable Long id,
                                             @RequestParam(defaultValue = "1") int page,
                                             @RequestParam(defaultValue = "10") int size,
                                             @RequestParam(required = false) OrderStatus status,
                                             @RequestParam(required = false) String keyword) {
        return R.ok(orderService.listByUser(page, size, id, status, keyword));
    }
}

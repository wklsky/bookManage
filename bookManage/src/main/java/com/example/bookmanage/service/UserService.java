package com.example.bookmanage.service;

import com.example.bookmanage.common.PageResult;
import com.example.bookmanage.common.Paging;
import com.example.bookmanage.common.SqlSort;
import com.example.bookmanage.dto.request.UserRequest;
import com.example.bookmanage.dto.response.UserVO;
import com.example.bookmanage.entity.SysUser;
import com.example.bookmanage.enums.AuditAction;
import com.example.bookmanage.enums.AuditTargetType;
import com.example.bookmanage.enums.UserRole;
import com.example.bookmanage.enums.UserStatus;
import com.example.bookmanage.exception.BizException;
import com.example.bookmanage.mapper.RefreshTokenMapper;
import com.example.bookmanage.mapper.SysUserMapper;
import com.example.bookmanage.security.SecurityUtils;
import com.example.bookmanage.support.ViewAssembler;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

/**
 * 个人中心与用户管理。
 */
@Service
@RequiredArgsConstructor
public class UserService {

    /** 排序字段白名单：key 为对外字段名，value 为数据库列名 */
    private static final Map<String, String> SORT_FIELDS =
            Map.of("createdAt", "created_at", "username", "username");

    private final SysUserMapper userMapper;
    private final RefreshTokenMapper refreshTokenMapper;
    private final PasswordEncoder passwordEncoder;
    private final AuditLogService auditLogService;

    public UserVO currentProfile() {
        return ViewAssembler.toUserVO(requireUser(SecurityUtils.currentUserId()));
    }

    /** 资料更新为部分更新：未传字段保持原值，便于前端只提交改动项 */
    @Transactional
    public UserVO updateProfile(UserRequest.ProfileUpdate request) {
        SysUser user = requireUser(SecurityUtils.currentUserId());

        if (request.email() != null && !request.email().equalsIgnoreCase(user.getEmail())
                && userMapper.countByEmail(request.email()) > 0) {
            throw BizException.conflict("邮箱已被占用");
        }
        if (request.nickname() != null) {
            user.setNickname(request.nickname());
        }
        if (request.email() != null) {
            user.setEmail(request.email());
        }
        if (request.phone() != null) {
            user.setPhone(request.phone());
        }
        if (request.avatarUrl() != null) {
            user.setAvatarUrl(request.avatarUrl());
        }
        userMapper.updateProfile(user);
        return ViewAssembler.toUserVO(user);
    }

    /** 改密后撤销该用户全部刷新令牌，强制其他设备重新登录 */
    @Transactional
    public void changePassword(UserRequest.PasswordChange request) {
        SysUser user = requireUser(SecurityUtils.currentUserId());
        if (!passwordEncoder.matches(request.oldPassword(), user.getPassword())) {
            throw BizException.badRequest("当前密码不正确");
        }
        if (passwordEncoder.matches(request.newPassword(), user.getPassword())) {
            throw BizException.badRequest("新密码不能与当前密码相同");
        }
        userMapper.updatePassword(user.getId(), passwordEncoder.encode(request.newPassword()));
        refreshTokenMapper.revokeByUserId(user.getId());
    }

    public PageResult<UserVO> list(int page, int size, String keyword, UserRole role, UserStatus status,
                                   String sort) {
        Paging.check(page, size);
        String orderBy = SqlSort.resolve(sort, SORT_FIELDS, "createdAt");
        long total = userMapper.countPage(keyword, role, status);
        List<SysUser> users = userMapper.selectPage(keyword, role, status, orderBy,
                Paging.offset(page, size), size);
        return PageResult.of(page, size, total, users.stream().map(ViewAssembler::toUserVO).toList());
    }

    public UserVO getById(Long id) {
        return ViewAssembler.toUserVO(requireUser(id));
    }

    @Transactional
    public UserVO updateAccess(Long id, UserRequest.AccessUpdate request) {
        Long operatorId = SecurityUtils.currentUserId();
        if (operatorId.equals(id)) {
            throw BizException.conflict("不能修改当前登录账号的角色或状态");
        }
        SysUser target = requireUser(id);

        UserRole nextRole = request.role() != null ? request.role() : target.getRole();
        UserStatus nextStatus = request.status() != null ? request.status() : target.getStatus();

        // 只有当目标账号当前就是启用管理员、且本次调整会取消该身份时，才需要校验"最后一个管理员"。
        // 若目标账号本就不是启用管理员（例如已停用的管理员），它与保留管理员无关，不应拦截。
        boolean wasActiveAdmin = target.getRole() == UserRole.ADMIN && target.getStatus() == UserStatus.ACTIVE;
        boolean stillActiveAdmin = nextRole == UserRole.ADMIN && nextStatus == UserStatus.ACTIVE;
        if (wasActiveAdmin && !stillActiveAdmin && userMapper.countActiveAdminExcluding(id) == 0) {
            throw BizException.conflict("系统中必须保留至少一个启用状态的管理员");
        }

        userMapper.updateAccess(id, nextRole, nextStatus);
        // swagger 约定 remark 记录调整原因，而用户表没有预留该字段，因此并入审计摘要留存
        auditLogService.record(AuditAction.USER_ACCESS_UPDATE, AuditTargetType.USER, String.valueOf(id),
                "role=" + nextRole + "，status=" + nextStatus
                        + (request.remark() == null ? "" : "，原因：" + request.remark()));

        target.setRole(nextRole);
        target.setStatus(nextStatus);
        return ViewAssembler.toUserVO(target);
    }

    /**
     * 管理员重置他人密码。
     *
     * <p>与本人改密的关键差异：管理员无从得知他人旧密码，所以不做旧密码校验，
     * 安全边界完全由"只有 ADMIN 能调用该接口"来兜底——因此必须同时吊销目标用户
     * 已签发的全部刷新令牌，否则被重置的账号仍能用旧会话继续续期。
     */
    @Transactional
    public void resetPassword(Long id, UserRequest.PasswordReset request) {
        Long operatorId = SecurityUtils.currentUserId();
        if (operatorId.equals(id)) {
            throw BizException.conflict("重置本人密码请使用个人中心的修改密码");
        }
        SysUser target = requireUser(id);
        if (passwordEncoder.matches(request.newPassword(), target.getPassword())) {
            throw BizException.badRequest("新密码不能与当前密码相同");
        }

        userMapper.updatePassword(id, passwordEncoder.encode(request.newPassword()));
        refreshTokenMapper.revokeByUserId(id);
        auditLogService.record(AuditAction.USER_PASSWORD_RESET, AuditTargetType.USER, String.valueOf(id),
                "重置 " + target.getUsername() + " 的密码"
                        + (request.remark() == null ? "" : "，原因：" + request.remark()));
    }

    private SysUser requireUser(Long id) {
        SysUser user = userMapper.selectById(id);
        if (user == null) {
            throw BizException.notFound("用户不存在");
        }
        return user;
    }
}

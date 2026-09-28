package com.example.bookmanage.service;

import com.example.bookmanage.common.PageResult;
import com.example.bookmanage.common.Paging;
import com.example.bookmanage.config.BookProperties;
import com.example.bookmanage.dto.request.OrderRequest;
import com.example.bookmanage.dto.response.BriefVO;
import com.example.bookmanage.dto.response.OrderVO;
import com.example.bookmanage.entity.Book;
import com.example.bookmanage.entity.BorrowOrder;
import com.example.bookmanage.entity.SysUser;
import com.example.bookmanage.enums.BookStatus;
import com.example.bookmanage.enums.OrderStatus;
import com.example.bookmanage.enums.ReturnCondition;
import com.example.bookmanage.exception.BizException;
import com.example.bookmanage.mapper.BookMapper;
import com.example.bookmanage.mapper.BorrowOrderMapper;
import com.example.bookmanage.mapper.SysUserMapper;
import com.example.bookmanage.security.LoginUser;
import com.example.bookmanage.security.SecurityUtils;
import com.example.bookmanage.support.ViewAssembler;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

/**
 * 借阅流程：预约 → 审核 → 借出 → 发起还书 → 验收归还。
 *
 * <p>所有状态流转与库存变更都放在同一个事务内，并在扣减库存前对图书行加锁，
 * 保证并发预约时可用库存不会被击穿。
 */
@Service
@RequiredArgsConstructor
public class OrderService {

    private static final DateTimeFormatter ORDER_NO_TIMESTAMP =
            DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    private final BorrowOrderMapper orderMapper;
    private final BookMapper bookMapper;
    private final SysUserMapper userMapper;
    private final BookProperties properties;

    public PageResult<OrderVO> listAll(int page, int size, OrderStatus status, String keyword,
                                       String from, String to) {
        Paging.check(page, size);
        boolean overdueOnly = status == OrderStatus.OVERDUE;
        LocalDateTime fromTime = parseDateTime(from, "from");
        LocalDateTime toTime = parseDateTime(to, "to");

        long total = orderMapper.countPage(null, status, overdueOnly, keyword, fromTime, toTime);
        List<BorrowOrder> orders = orderMapper.selectPage(null, status, overdueOnly, keyword, fromTime, toTime,
                Paging.offset(page, size), size);
        return PageResult.of(page, size, total, assemble(orders));
    }

    public PageResult<OrderVO> listMine(int page, int size, OrderStatus status, String keyword) {
        Paging.check(page, size);
        Long userId = SecurityUtils.currentUserId();
        boolean overdueOnly = status == OrderStatus.OVERDUE;

        long total = orderMapper.countPage(userId, status, overdueOnly, keyword, null, null);
        List<BorrowOrder> orders = orderMapper.selectPage(userId, status, overdueOnly, keyword, null, null,
                Paging.offset(page, size), size);
        return PageResult.of(page, size, total, assemble(orders));
    }

    public OrderVO getById(Long id) {
        BorrowOrder order = requireOrder(id);
        LoginUser user = SecurityUtils.currentUser();
        if (!Objects.equals(order.getUserId(), user.getId()) && !user.isManager()) {
            throw BizException.forbidden("无权查看他人的借阅单");
        }
        return toVO(order);
    }

    @Transactional
    public OrderVO reserve(OrderRequest.Reserve request) {
        Long userId = SecurityUtils.currentUserId();
        if (orderMapper.countUnfinished(userId, request.bookId()) > 0) {
            throw BizException.conflict("该图书已存在未完成的借阅记录");
        }

        Book book = bookMapper.selectByIdForUpdate(request.bookId());
        if (book == null || Boolean.TRUE.equals(book.getIsDeleted())) {
            throw BizException.notFound("图书不存在");
        }
        if (book.getStatus() != BookStatus.ACTIVE) {
            throw BizException.unprocessable("该图书已下架，无法预约");
        }
        // 条件更新：可用库存为 0 时影响行数为 0，即代表无库存可预约
        if (bookMapper.decreaseAvailable(book.getId()) == 0) {
            throw BizException.unprocessable("该图书当前无可借库存");
        }

        BorrowOrder order = new BorrowOrder();
        order.setOrderNo(generateOrderNo());
        order.setUserId(userId);
        order.setBookId(book.getId());
        order.setStatus(OrderStatus.PENDING);
        order.setRemark(request.remark());
        order.setReservedAt(LocalDateTime.now());
        orderMapper.insert(order);

        return toVO(order);
    }

    @Transactional
    public OrderVO cancel(Long id) {
        BorrowOrder order = requireOrder(id);
        LoginUser user = SecurityUtils.currentUser();
        if (!Objects.equals(order.getUserId(), user.getId())) {
            throw BizException.forbidden("只能取消本人的借阅单");
        }
        if (order.getStatus() != OrderStatus.PENDING && order.getStatus() != OrderStatus.APPROVED) {
            throw BizException.conflict("当前状态的借阅单不可取消");
        }

        order.setStatus(OrderStatus.CANCELLED);
        orderMapper.update(order);
        bookMapper.increaseAvailable(order.getBookId());
        return toVO(order);
    }

    @Transactional
    public OrderVO audit(Long id, OrderRequest.Audit request) {
        BorrowOrder order = requireOrder(id);
        if (order.getStatus() != OrderStatus.PENDING) {
            throw BizException.conflict("只有待审核的预约可以审核");
        }

        boolean approve = "APPROVE".equalsIgnoreCase(request.decision());
        if (!approve && !"REJECT".equalsIgnoreCase(request.decision())) {
            throw BizException.badRequest("审核结论只能为 APPROVE 或 REJECT");
        }
        if (!approve && isBlank(request.remark())) {
            throw BizException.badRequest("拒绝预约时必须填写备注");
        }

        order.setAuditRemark(request.remark());
        if (approve) {
            order.setStatus(OrderStatus.APPROVED);
            order.setApprovedAt(LocalDateTime.now());
        } else {
            order.setStatus(OrderStatus.REJECTED);
            bookMapper.increaseAvailable(order.getBookId());
        }
        orderMapper.update(order);
        return toVO(order);
    }

    @Transactional
    public OrderVO checkout(Long id, OrderRequest.Checkout request) {
        BorrowOrder order = requireOrder(id);
        if (order.getStatus() != OrderStatus.APPROVED) {
            throw BizException.conflict("只有审核通过的预约可以确认借出");
        }

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime dueAt;
        if (request.dueAt() != null) {
            // 前端传的是带时区的 ISO 时间，需换算回服务端所在时区再落库
            dueAt = LocalDateTime.ofInstant(request.dueAt().toInstant(), ZoneId.systemDefault());
            if (!dueAt.isAfter(now)) {
                throw BizException.badRequest("应还时间必须晚于当前时间");
            }
        } else {
            dueAt = now.plusDays(properties.getBorrow().getDefaultDays());
        }

        order.setStatus(OrderStatus.BORROWED);
        order.setBorrowedAt(now);
        order.setDueAt(dueAt);
        if (!isBlank(request.remark())) {
            order.setAuditRemark(request.remark());
        }
        orderMapper.update(order);
        return toVO(order);
    }

    @Transactional
    public OrderVO requestReturn(Long id, OrderRequest.Return request) {
        BorrowOrder order = requireOrder(id);
        LoginUser user = SecurityUtils.currentUser();
        if (!Objects.equals(order.getUserId(), user.getId())) {
            throw BizException.forbidden("只能归还本人的借阅单");
        }
        // 逾期（OVERDUE）是 BORROWED 的派生状态，此处必须一并放行，
        // 否则已逾期的图书将永远无法发起还书。
        if (order.getStatus() != OrderStatus.BORROWED && order.getStatus() != OrderStatus.OVERDUE) {
            throw BizException.conflict("当前状态的借阅单无法发起还书");
        }

        order.setStatus(OrderStatus.RETURN_REQUESTED);
        order.setReturnRemark(request.remark());
        orderMapper.update(order);
        return toVO(order);
    }

    @Transactional
    public OrderVO confirmReturn(Long id, OrderRequest.ConfirmReturn request) {
        BorrowOrder order = requireOrder(id);
        if (order.getStatus() != OrderStatus.RETURN_REQUESTED) {
            throw BizException.conflict("只有待验收的借阅单可以验收归还");
        }

        ReturnCondition condition;
        try {
            condition = ReturnCondition.valueOf(request.condition());
        } catch (IllegalArgumentException | NullPointerException ex) {
            throw BizException.badRequest("验收结果只能为 GOOD、DAMAGED 或 LOST");
        }
        if (condition != ReturnCondition.GOOD && isBlank(request.remark())) {
            throw BizException.badRequest("图书损坏或遗失时必须填写备注");
        }

        order.setStatus(OrderStatus.RETURNED);
        order.setReturnCondition(condition);
        order.setReturnedAt(LocalDateTime.now());
        order.setAuditRemark(request.remark());
        orderMapper.update(order);

        // 遗失的图书无法再借出，因此不回补可用库存
        if (condition != ReturnCondition.LOST) {
            bookMapper.increaseAvailable(order.getBookId());
        }
        return toVO(order);
    }

    private BorrowOrder requireOrder(Long id) {
        BorrowOrder order = orderMapper.selectById(id);
        if (order == null) {
            throw BizException.notFound("借阅单不存在");
        }
        deriveOverdue(order);
        return order;
    }

    /** 逾期是派生状态：借阅中且已过应还时间即视为逾期，不落库以免状态滞后 */
    private void deriveOverdue(BorrowOrder order) {
        if (order.getStatus() == OrderStatus.BORROWED
                && order.getDueAt() != null
                && order.getDueAt().isBefore(LocalDateTime.now())) {
            order.setStatus(OrderStatus.OVERDUE);
        }
    }

    private List<OrderVO> assemble(List<BorrowOrder> orders) {
        orders.forEach(this::deriveOverdue);

        Set<Long> userIds = orders.stream().map(BorrowOrder::getUserId).filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Set<Long> bookIds = orders.stream().map(BorrowOrder::getBookId).filter(Objects::nonNull)
                .collect(Collectors.toSet());

        Map<Long, BriefVO.UserBrief> users = userIds.isEmpty() ? Map.of()
                : userMapper.selectBriefByIds(userIds).stream()
                        .collect(Collectors.toMap(SysUser::getId, ViewAssembler::toUserBrief, (a, b) -> a));
        Map<Long, BriefVO.BookBrief> books = bookIds.isEmpty() ? Map.of()
                : bookMapper.selectBriefByIds(bookIds).stream()
                        .collect(Collectors.toMap(Book::getId, ViewAssembler::toBookBrief, (a, b) -> a));

        return orders.stream()
                .map(order -> ViewAssembler.toOrderVO(order, users.get(order.getUserId()),
                        books.get(order.getBookId())))
                .toList();
    }

    private OrderVO toVO(BorrowOrder order) {
        SysUser user = userMapper.selectById(order.getUserId());
        Book book = bookMapper.selectById(order.getBookId());
        return ViewAssembler.toOrderVO(order, ViewAssembler.toUserBrief(user), ViewAssembler.toBookBrief(book));
    }

    private String generateOrderNo() {
        return "BOR" + LocalDateTime.now().format(ORDER_NO_TIMESTAMP)
                + String.format("%04d", ThreadLocalRandom.current().nextInt(10000));
    }

    /** 前端时间筛选可能是不带时区的本地时间，也可能是 ISO 带时区时间，两种都要兼容 */
    private LocalDateTime parseDateTime(String value, String field) {
        if (isBlank(value)) {
            return null;
        }
        try {
            return LocalDateTime.parse(value);
        } catch (DateTimeParseException ignored) {
            try {
                return OffsetDateTime.parse(value).toLocalDateTime();
            } catch (DateTimeParseException ex) {
                throw BizException.badRequest(field + " 时间格式不合法");
            }
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}

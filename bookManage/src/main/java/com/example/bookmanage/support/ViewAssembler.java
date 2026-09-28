package com.example.bookmanage.support;

import com.example.bookmanage.dto.response.BookVO;
import com.example.bookmanage.dto.response.BriefVO;
import com.example.bookmanage.dto.response.CategoryVO;
import com.example.bookmanage.dto.response.OrderVO;
import com.example.bookmanage.dto.response.UserVO;
import com.example.bookmanage.entity.Book;
import com.example.bookmanage.entity.BookCategory;
import com.example.bookmanage.entity.BorrowOrder;
import com.example.bookmanage.entity.SysUser;

/**
 * 实体到视图对象的转换。集中在此处，避免各 Service 重复拼装且口径不一致。
 */
public final class ViewAssembler {

    private ViewAssembler() {
    }

    public static UserVO toUserVO(SysUser user) {
        if (user == null) {
            return null;
        }
        return new UserVO(user.getId(), user.getUsername(), user.getNickname(), user.getEmail(),
                user.getPhone(), user.getAvatarUrl(), user.getRole(), user.getStatus(),
                user.getCreatedAt(), user.getUpdatedAt());
    }

    public static BriefVO.UserBrief toUserBrief(SysUser user) {
        if (user == null) {
            return null;
        }
        return new BriefVO.UserBrief(user.getId(), user.getUsername(), user.getNickname());
    }

    public static CategoryVO toCategoryVO(BookCategory category) {
        if (category == null) {
            return null;
        }
        return new CategoryVO(category.getId(), category.getName(), category.getDescription(),
                category.getSortOrder(), category.getStatus(), category.getBookCount(),
                category.getCreatedAt(), category.getUpdatedAt());
    }

    public static BriefVO.CategoryBrief toCategoryBrief(BookCategory category) {
        if (category == null) {
            return null;
        }
        return new BriefVO.CategoryBrief(category.getId(), category.getName());
    }

    public static BriefVO.BookBrief toBookBrief(Book book) {
        if (book == null) {
            return null;
        }
        return new BriefVO.BookBrief(book.getId(), book.getTitle(), book.getIsbn(), book.getCoverUrl());
    }

    public static BookVO toBookVO(Book book, BriefVO.CategoryBrief category) {
        if (book == null) {
            return null;
        }
        return new BookVO(book.getId(), book.getTitle(), book.getAuthor(), book.getIsbn(),
                book.getPublisher(), book.getPublishDate(), book.getDescription(), book.getCoverUrl(),
                category, book.getTotalStock(), book.getAvailableStock(), book.getStatus(),
                book.getCreatedAt(), book.getUpdatedAt());
    }

    public static OrderVO toOrderVO(BorrowOrder order, BriefVO.UserBrief user, BriefVO.BookBrief book) {
        if (order == null) {
            return null;
        }
        return new OrderVO(order.getId(), order.getOrderNo(), user, book, order.getStatus(),
                order.getRemark(), order.getAuditRemark(), order.getReturnCondition(),
                order.getReservedAt(), order.getApprovedAt(), order.getBorrowedAt(), order.getDueAt(),
                order.getReturnedAt(), order.getCreatedAt(), order.getUpdatedAt());
    }
}

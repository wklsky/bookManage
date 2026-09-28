package com.example.bookmanage.service;

import com.example.bookmanage.common.PageResult;
import com.example.bookmanage.common.Paging;
import com.example.bookmanage.common.SqlSort;
import com.example.bookmanage.dto.request.BookRequest;
import com.example.bookmanage.dto.response.BookVO;
import com.example.bookmanage.dto.response.BriefVO;
import com.example.bookmanage.entity.Book;
import com.example.bookmanage.entity.BookCategory;
import com.example.bookmanage.entity.BookStockLog;
import com.example.bookmanage.enums.AuditAction;
import com.example.bookmanage.enums.AuditTargetType;
import com.example.bookmanage.enums.BookStatus;
import com.example.bookmanage.exception.BizException;
import com.example.bookmanage.mapper.BookCategoryMapper;
import com.example.bookmanage.mapper.BookMapper;
import com.example.bookmanage.mapper.BookStockLogMapper;
import com.example.bookmanage.mapper.FeaturedBookMapper;
import com.example.bookmanage.security.SecurityUtils;
import com.example.bookmanage.support.ViewAssembler;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 图书检索、馆藏维护与库存调整。
 */
@Service
@RequiredArgsConstructor
public class BookService {

    /** 排序字段白名单：key 为对外字段名，value 为数据库列名 */
    private static final Map<String, String> SORT_FIELDS = Map.of(
            "createdAt", "created_at",
            "title", "title",
            "availableStock", "available_stock");

    private final BookMapper bookMapper;
    private final BookCategoryMapper categoryMapper;
    private final BookStockLogMapper stockLogMapper;
    private final FeaturedBookMapper featuredBookMapper;
    private final AuditLogService auditLogService;

    /**
     * @param manager 是否管理端调用。普通读者只能检索已上架图书，避免下架书仍然出现在检索结果中
     */
    public PageResult<BookVO> list(int page, int size, String keyword, Long categoryId, String availability,
                                   BookStatus requestedStatus, String sort, boolean manager) {
        Paging.check(page, size);

        BookStatus status = manager ? requestedStatus : BookStatus.ACTIVE;
        boolean availableOnly = "AVAILABLE".equalsIgnoreCase(availability);
        boolean unavailableOnly = "UNAVAILABLE".equalsIgnoreCase(availability);
        String orderBy = SqlSort.resolve(sort, SORT_FIELDS, "createdAt");

        long total = bookMapper.countPage(keyword, categoryId, status, availableOnly, unavailableOnly);
        List<Book> books = bookMapper.selectPage(keyword, categoryId, status, availableOnly, unavailableOnly,
                orderBy, Paging.offset(page, size), size);
        Map<Long, BriefVO.CategoryBrief> categories = loadCategories(books);

        return PageResult.of(page, size, total, books.stream()
                .map(book -> ViewAssembler.toBookVO(book, categories.get(book.getCategoryId())))
                .toList());
    }

    public BookVO getById(Long id) {
        Book book = requireBook(id);
        return ViewAssembler.toBookVO(book, loadCategoryBrief(book.getCategoryId()));
    }

    @Transactional
    public BookVO create(BookRequest.Create request) {
        BookCategory category = requireCategory(request.categoryId());
        if (bookMapper.countByIsbn(request.isbn(), null) > 0) {
            throw BizException.conflict("该 ISBN 已存在");
        }

        Book book = new Book();
        book.setTitle(request.title());
        book.setAuthor(request.author());
        book.setIsbn(request.isbn());
        book.setPublisher(request.publisher());
        book.setPublishDate(request.publishDate());
        book.setDescription(request.description());
        book.setCoverUrl(request.coverUrl());
        book.setCategoryId(category.getId());
        book.setTotalStock(request.totalStock());
        // 新书不存在借出记录，可借数量与馆藏总量一致
        book.setAvailableStock(request.totalStock());
        book.setStatus(request.status() == null ? BookStatus.ACTIVE : request.status());
        book.setIsDeleted(false);
        bookMapper.insert(book);

        return ViewAssembler.toBookVO(book, ViewAssembler.toCategoryBrief(category));
    }

    /** 书目更新不触碰库存：库存变化必须走库存接口，以便留下流水 */
    @Transactional
    public BookVO update(Long id, BookRequest.Update request) {
        Book book = requireBook(id);
        if (request.categoryId() != null) {
            requireCategory(request.categoryId());
            book.setCategoryId(request.categoryId());
        }
        if (request.isbn() != null && !request.isbn().equals(book.getIsbn())) {
            if (bookMapper.countByIsbn(request.isbn(), id) > 0) {
                throw BizException.conflict("该 ISBN 已存在");
            }
            book.setIsbn(request.isbn());
        }
        if (request.title() != null) {
            book.setTitle(request.title());
        }
        if (request.author() != null) {
            book.setAuthor(request.author());
        }
        if (request.publisher() != null) {
            book.setPublisher(request.publisher());
        }
        if (request.publishDate() != null) {
            book.setPublishDate(request.publishDate());
        }
        if (request.description() != null) {
            book.setDescription(request.description());
        }
        if (request.coverUrl() != null) {
            book.setCoverUrl(request.coverUrl());
        }
        if (request.status() != null && request.status() != book.getStatus()) {
            // 上下架直接决定图书是否对读者可见，属于前台展示控制，必须留痕以便追溯
            auditLogService.record(AuditAction.BOOK_STATUS_UPDATE, AuditTargetType.BOOK, String.valueOf(id),
                    "《" + book.getTitle() + "》" + book.getStatus() + " → " + request.status());
            book.setStatus(request.status());
        }
        bookMapper.update(book);
        return ViewAssembler.toBookVO(book, loadCategoryBrief(book.getCategoryId()));
    }

    /** 软删除：存在未完成借阅单时拒绝下架，否则借阅流程会指向已删除的书目 */
    @Transactional
    public void delete(Long id) {
        Book book = requireBook(id);
        if (bookMapper.countUnfinishedOrders(id) > 0) {
            throw BizException.conflict("该图书仍有未完成的借阅记录，无法删除");
        }
        bookMapper.softDelete(id);
        // 图书是软删除，外键 ON DELETE CASCADE 不会触发，推荐位必须由这里主动清理，
        // 否则前台首页会残留指向已下架图书的展示位，读者点击即 404
        featuredBookMapper.deleteByBookId(id);
        auditLogService.record(AuditAction.BOOK_STATUS_UPDATE, AuditTargetType.BOOK, String.valueOf(id),
                "下架并删除《" + book.getTitle() + "》");
    }

    @Transactional
    public BookVO adjustStock(Long id, BookRequest.StockAdjust request) {
        if (request.change() == null || request.change() == 0) {
            throw BizException.unprocessable("调整数量不能为 0");
        }
        // requireBook 同时承担"图书存在性"校验，库存调整不能落到已下架的书上
        requireBook(id);
        // 行锁 + 条件更新：并发调整时由影响行数判定是否越界
        bookMapper.selectByIdForUpdate(id);
        if (bookMapper.adjustStock(id, request.change()) == 0) {
            throw BizException.unprocessable("调整后可用库存为负，请先回收已借出的图书");
        }

        BookStockLog log = new BookStockLog();
        log.setBookId(id);
        log.setOperatorId(SecurityUtils.currentUserId());
        log.setChangeAmount(request.change());
        log.setReason(request.reason());
        log.setCreatedAt(LocalDateTime.now());
        stockLogMapper.insert(log);

        Book updated = requireBook(id);
        return ViewAssembler.toBookVO(updated, loadCategoryBrief(updated.getCategoryId()));
    }

    private Book requireBook(Long id) {
        Book book = bookMapper.selectById(id);
        if (book == null || Boolean.TRUE.equals(book.getIsDeleted())) {
            throw BizException.notFound("图书不存在");
        }
        return book;
    }

    private BookCategory requireCategory(Long categoryId) {
        BookCategory category = categoryMapper.selectById(categoryId);
        if (category == null) {
            throw BizException.notFound("指定的图书分类不存在");
        }
        return category;
    }

    private BriefVO.CategoryBrief loadCategoryBrief(Long categoryId) {
        if (categoryId == null) {
            return null;
        }
        return ViewAssembler.toCategoryBrief(categoryMapper.selectById(categoryId));
    }

    /** 批量加载分类，避免列表页逐条回查 */
    private Map<Long, BriefVO.CategoryBrief> loadCategories(List<Book> books) {
        Set<Long> ids = books.stream()
                .map(Book::getCategoryId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        if (ids.isEmpty()) {
            return Map.of();
        }
        return categoryMapper.selectBriefByIds(ids).stream()
                .collect(Collectors.toMap(BookCategory::getId, ViewAssembler::toCategoryBrief, (a, b) -> a));
    }
}

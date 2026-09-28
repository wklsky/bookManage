package com.example.bookmanage.service;

import com.example.bookmanage.common.PageResult;
import com.example.bookmanage.common.Paging;
import com.example.bookmanage.dto.request.AdminRequest;
import com.example.bookmanage.dto.response.FeaturedBookVO;
import com.example.bookmanage.entity.Book;
import com.example.bookmanage.entity.FeaturedBook;
import com.example.bookmanage.enums.AuditAction;
import com.example.bookmanage.enums.AuditTargetType;
import com.example.bookmanage.enums.BookStatus;
import com.example.bookmanage.exception.BizException;
import com.example.bookmanage.mapper.BookMapper;
import com.example.bookmanage.mapper.FeaturedBookMapper;
import com.example.bookmanage.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 前台首页推荐位维护。
 */
@Service
@RequiredArgsConstructor
public class FeaturedBookService {

    /** 前台一次最多展示的推荐位数量，防止后台误配过多把首页挤爆 */
    private static final int VISIBLE_LIMIT = 20;

    private final FeaturedBookMapper featuredBookMapper;
    private final BookMapper bookMapper;
    private final AuditLogService auditLogService;

    /** 前台读取：只返回已启用且图书处于上架状态的推荐位 */
    public List<FeaturedBookVO> listVisible() {
        return featuredBookMapper.selectVisible(VISIBLE_LIMIT).stream()
                .map(this::toVO)
                .toList();
    }

    /** 后台列表：enabledOnly 为 true 时只看已启用的，用于快速核对前台实际展示内容 */
    public PageResult<FeaturedBookVO> list(int page, int size, boolean enabledOnly) {
        Paging.check(page, size);
        long total = featuredBookMapper.countPage(enabledOnly);
        List<FeaturedBook> rows = featuredBookMapper.selectPage(enabledOnly, Paging.offset(page, size), size);
        return PageResult.of(page, size, total, rows.stream().map(this::toVO).toList());
    }

    @Transactional
    public FeaturedBookVO create(AdminRequest.FeaturedCreate request) {
        Book book = requireOnShelfBook(request.bookId());
        if (featuredBookMapper.countByBookId(request.bookId()) > 0) {
            throw BizException.conflict("该图书已在推荐位中");
        }

        FeaturedBook featured = new FeaturedBook();
        featured.setBookId(request.bookId());
        // 未指定权重时追加到末尾；+1 保证与现有最大权重相同时仍按加入先后稳定排序
        featured.setPosition(request.position() == null
                ? featuredBookMapper.maxPosition() + 1
                : request.position());
        featured.setEnabled(request.enabled() == null || request.enabled());
        featured.setRemark(request.remark());
        featured.setCreatedBy(SecurityUtils.currentUserId());
        featuredBookMapper.insert(featured);

        fillBookSnapshot(featured, book);
        auditLogService.record(AuditAction.FEATURED_CREATE, AuditTargetType.FEATURED,
                String.valueOf(featured.getId()), "推荐《" + book.getTitle() + "》");
        return toVO(featured);
    }

    @Transactional
    public FeaturedBookVO update(Long id, AdminRequest.FeaturedUpdate request) {
        FeaturedBook featured = requireFeatured(id);
        if (request.position() != null) {
            featured.setPosition(request.position());
        }
        if (request.enabled() != null) {
            featured.setEnabled(request.enabled());
        }
        if (request.remark() != null) {
            featured.setRemark(request.remark());
        }
        if (featuredBookMapper.update(featured) == 0) {
            throw BizException.notFound("推荐位不存在");
        }

        auditLogService.record(AuditAction.FEATURED_UPDATE, AuditTargetType.FEATURED, String.valueOf(id),
                "权重=" + featured.getPosition() + "，展示=" + featured.isEnabled());
        return toVO(loadWithBook(id));
    }

    @Transactional
    public void delete(Long id) {
        FeaturedBook featured = requireFeatured(id);
        featuredBookMapper.delete(id);
        auditLogService.record(AuditAction.FEATURED_DELETE, AuditTargetType.FEATURED, String.valueOf(id),
                "移除推荐位，图书ID=" + featured.getBookId());
    }

    private FeaturedBook requireFeatured(Long id) {
        FeaturedBook featured = featuredBookMapper.selectById(id);
        if (featured == null) {
            throw BizException.notFound("推荐位不存在");
        }
        return featured;
    }

    /**
     * 只有上架图书能进推荐位：前台展示位若指向已下架或已删除的图书，
     * 读者点击后只会得到 404，属于后台可以直接避免的脏数据。
     */
    private Book requireOnShelfBook(Long bookId) {
        Book book = bookMapper.selectById(bookId);
        if (book == null || Boolean.TRUE.equals(book.getIsDeleted())) {
            throw BizException.notFound("图书不存在");
        }
        if (book.getStatus() != BookStatus.ACTIVE) {
            throw BizException.conflict("仅上架状态的图书可加入推荐位");
        }
        return book;
    }

    private FeaturedBook loadWithBook(Long id) {
        FeaturedBook featured = requireFeatured(id);
        Book book = bookMapper.selectById(featured.getBookId());
        if (book != null) {
            fillBookSnapshot(featured, book);
        }
        return featured;
    }

    private void fillBookSnapshot(FeaturedBook featured, Book book) {
        featured.setTitle(book.getTitle());
        featured.setAuthor(book.getAuthor());
        featured.setIsbn(book.getIsbn());
        featured.setCoverUrl(book.getCoverUrl());
        featured.setStatus(book.getStatus());
        featured.setTotalStock(book.getTotalStock());
        featured.setAvailableStock(book.getAvailableStock());
    }

    private FeaturedBookVO toVO(FeaturedBook featured) {
        return new FeaturedBookVO(featured.getId(), featured.getBookId(), featured.getTitle(),
                featured.getAuthor(), featured.getIsbn(), featured.getCoverUrl(), featured.getStatus(),
                featured.getTotalStock(), featured.getAvailableStock(), featured.getPosition(),
                featured.isEnabled(), featured.getRemark(), featured.getCreatedBy(),
                featured.getCreatedAt(), featured.getUpdatedAt());
    }
}

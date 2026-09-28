package com.example.bookmanage.service;

import com.example.bookmanage.dto.request.CategoryRequest;
import com.example.bookmanage.dto.response.CategoryVO;
import com.example.bookmanage.entity.BookCategory;
import com.example.bookmanage.enums.CategoryStatus;
import com.example.bookmanage.exception.BizException;
import com.example.bookmanage.mapper.BookCategoryMapper;
import com.example.bookmanage.support.ViewAssembler;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 图书分类维护。
 */
@Service
@RequiredArgsConstructor
public class CategoryService {

    /** 列表默认按排序权重升序、创建时间降序 */
    private static final String DEFAULT_ORDER = "sort_order ASC, created_at DESC";

    private final BookCategoryMapper categoryMapper;

    public List<CategoryVO> list(String keyword, boolean includeDisabled) {
        return categoryMapper.selectList(keyword, includeDisabled, DEFAULT_ORDER).stream()
                .map(ViewAssembler::toCategoryVO)
                .toList();
    }

    public CategoryVO getById(Long id) {
        return ViewAssembler.toCategoryVO(requireCategory(id));
    }

    @Transactional
    public CategoryVO create(CategoryRequest.Create request) {
        BookCategory category = new BookCategory();
        category.setName(request.name());
        category.setDescription(request.description());
        category.setSortOrder(request.sortOrder() == null ? 0 : request.sortOrder());
        category.setStatus(request.status() == null ? CategoryStatus.ACTIVE : request.status());
        categoryMapper.insert(category);
        category.setBookCount(0L);
        return ViewAssembler.toCategoryVO(category);
    }

    @Transactional
    public CategoryVO update(Long id, CategoryRequest.Update request) {
        BookCategory category = requireCategory(id);
        if (request.name() != null) {
            category.setName(request.name());
        }
        if (request.description() != null) {
            category.setDescription(request.description());
        }
        if (request.sortOrder() != null) {
            category.setSortOrder(request.sortOrder());
        }
        if (request.status() != null) {
            category.setStatus(request.status());
        }
        if (categoryMapper.update(category) == 0) {
            throw BizException.notFound("分类不存在");
        }
        return ViewAssembler.toCategoryVO(category);
    }

    /** 分类下仍有图书时拒绝删除，避免出现无归属的馆藏数据 */
    @Transactional
    public void delete(Long id) {
        BookCategory category = requireCategory(id);
        if (categoryMapper.countBooks(id) > 0) {
            throw BizException.conflict("该分类下仍有图书，无法删除");
        }
        categoryMapper.delete(id);
    }

    private BookCategory requireCategory(Long id) {
        BookCategory category = categoryMapper.selectById(id);
        if (category == null) {
            throw BizException.notFound("分类不存在");
        }
        return category;
    }
}

package com.example.bookmanage.controller;

import com.example.bookmanage.common.R;
import com.example.bookmanage.dto.request.CategoryRequest;
import com.example.bookmanage.dto.response.CategoryVO;
import com.example.bookmanage.service.CategoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 图书分类接口。
 */
@RestController
@RequestMapping("/api/categories")
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryService categoryService;

    /** includeDisabled 供管理端查看停用分类；读者侧默认只看启用分类 */
    @GetMapping
    public R<List<CategoryVO>> list(@RequestParam(required = false) String keyword,
                                    @RequestParam(defaultValue = "false") boolean includeDisabled) {
        return R.ok(categoryService.list(keyword, includeDisabled));
    }

    @GetMapping("/{id}")
    public R<CategoryVO> detail(@PathVariable Long id) {
        return R.ok(categoryService.getById(id));
    }

    @PostMapping
    public ResponseEntity<R<CategoryVO>> create(@Valid @RequestBody CategoryRequest.Create request) {
        return ResponseEntity.status(201).body(R.created(categoryService.create(request)));
    }

    @PutMapping("/{id}")
    public R<CategoryVO> update(@PathVariable Long id,
                                @Valid @RequestBody CategoryRequest.Update request) {
        return R.ok(categoryService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable Long id) {
        categoryService.delete(id);
        return R.ok(null);
    }
}

package com.example.bookmanage.controller;

import com.example.bookmanage.common.PageResult;
import com.example.bookmanage.common.R;
import com.example.bookmanage.dto.request.AdminRequest;
import com.example.bookmanage.dto.response.FeaturedBookVO;
import com.example.bookmanage.service.FeaturedBookService;
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

/**
 * 管理后台：前台首页推荐位维护。
 */
@RestController
@RequestMapping("/api/admin/featured-books")
@RequiredArgsConstructor
public class AdminFeaturedBooksController {

    private final FeaturedBookService featuredBookService;

    /** enabledOnly 用于快速核对前台当前实际会展示哪些书 */
    @GetMapping
    public R<PageResult<FeaturedBookVO>> list(@RequestParam(defaultValue = "1") int page,
                                              @RequestParam(defaultValue = "10") int size,
                                              @RequestParam(defaultValue = "false") boolean enabledOnly) {
        return R.ok(featuredBookService.list(page, size, enabledOnly));
    }

    @PostMapping
    public ResponseEntity<R<FeaturedBookVO>> create(@Valid @RequestBody AdminRequest.FeaturedCreate request) {
        return ResponseEntity.status(201).body(R.created(featuredBookService.create(request)));
    }

    @PutMapping("/{id}")
    public R<FeaturedBookVO> update(@PathVariable Long id,
                                    @Valid @RequestBody AdminRequest.FeaturedUpdate request) {
        return R.ok(featuredBookService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable Long id) {
        featuredBookService.delete(id);
        return R.ok(null);
    }
}

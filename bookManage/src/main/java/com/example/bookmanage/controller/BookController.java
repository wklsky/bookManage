package com.example.bookmanage.controller;

import com.example.bookmanage.common.PageResult;
import com.example.bookmanage.common.R;
import com.example.bookmanage.dto.request.BookRequest;
import com.example.bookmanage.dto.response.BookVO;
import com.example.bookmanage.enums.BookStatus;
import com.example.bookmanage.security.SecurityUtils;
import com.example.bookmanage.service.BookService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 图书检索与馆藏维护接口。
 */
@RestController
@RequestMapping("/api/books")
@RequiredArgsConstructor
public class BookController {

    private final BookService bookService;

    /**
     * @param availability AVAILABLE 表示只看有库存的，UNAVAILABLE 表示只看无库存的
     * @param status       普通读者只能检索已上架图书，该限制在 Service 层强制生效
     */
    @GetMapping
    public R<PageResult<BookVO>> list(@RequestParam(defaultValue = "1") int page,
                          @RequestParam(defaultValue = "10") int size,
                          @RequestParam(required = false) String keyword,
                          @RequestParam(required = false) Long categoryId,
                          @RequestParam(required = false) String availability,
                          @RequestParam(required = false) BookStatus status,
                          @RequestParam(required = false) String sort) {
        return R.ok(bookService.list(page, size, keyword, categoryId, availability, status, sort,
                SecurityUtils.currentUser().isManager()));
    }

    @GetMapping("/{id}")
    public R<BookVO> detail(@PathVariable Long id) {
        return R.ok(bookService.getById(id));
    }

    @PostMapping
    public ResponseEntity<R<BookVO>> create(@Valid @RequestBody BookRequest.Create request) {
        return ResponseEntity.status(201).body(R.created(bookService.create(request)));
    }

    @PutMapping("/{id}")
    public R<BookVO> update(@PathVariable Long id, @Valid @RequestBody BookRequest.Update request) {
        return R.ok(bookService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable Long id) {
        bookService.delete(id);
        return R.ok(null);
    }

    @PatchMapping("/{id}/stock")
    public R<BookVO> adjustStock(@PathVariable Long id,
                                 @Valid @RequestBody BookRequest.StockAdjust request) {
        return R.ok(bookService.adjustStock(id, request));
    }
}

package com.example.bookmanage.controller;

import com.example.bookmanage.common.PageResult;
import com.example.bookmanage.common.R;
import com.example.bookmanage.dto.request.OrderRequest;
import com.example.bookmanage.dto.response.OrderVO;
import com.example.bookmanage.enums.OrderStatus;
import com.example.bookmanage.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 借阅流程接口。
 */
@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    /** 管理端查看全部借阅单，支持按创建时间区间筛选 */
    @GetMapping
    public R<PageResult<OrderVO>> list(@RequestParam(defaultValue = "1") int page,
                          @RequestParam(defaultValue = "10") int size,
                          @RequestParam(required = false) OrderStatus status,
                          @RequestParam(required = false) String keyword,
                          @RequestParam(required = false) String from,
                          @RequestParam(required = false) String to) {
        return R.ok(orderService.listAll(page, size, status, keyword, from, to));
    }

    @GetMapping("/mine")
    public R<PageResult<OrderVO>> listMine(@RequestParam(defaultValue = "1") int page,
                              @RequestParam(defaultValue = "10") int size,
                              @RequestParam(required = false) OrderStatus status,
                              @RequestParam(required = false) String keyword) {
        return R.ok(orderService.listMine(page, size, status, keyword));
    }

    @PostMapping("/reserve")
    public ResponseEntity<R<OrderVO>> reserve(@Valid @RequestBody OrderRequest.Reserve request) {
        return ResponseEntity.status(201).body(R.created(orderService.reserve(request)));
    }

    @GetMapping("/{id}")
    public R<OrderVO> detail(@PathVariable Long id) {
        return R.ok(orderService.getById(id));
    }

    @PutMapping("/{id}/cancel")
    public R<OrderVO> cancel(@PathVariable Long id) {
        return R.ok(orderService.cancel(id));
    }

    @PutMapping("/{id}/audit")
    public R<OrderVO> audit(@PathVariable Long id, @Valid @RequestBody OrderRequest.Audit request) {
        return R.ok(orderService.audit(id, request));
    }

    @PutMapping("/{id}/checkout")
    public R<OrderVO> checkout(@PathVariable Long id, @Valid @RequestBody OrderRequest.Checkout request) {
        return R.ok(orderService.checkout(id, request));
    }

    @PutMapping("/{id}/return")
    public R<OrderVO> requestReturn(@PathVariable Long id, @RequestBody(required = false) OrderRequest.Return request) {
        return R.ok(orderService.requestReturn(id, request == null ? new OrderRequest.Return(null) : request));
    }

    @PutMapping("/{id}/confirm-return")
    public R<OrderVO> confirmReturn(@PathVariable Long id,
                                    @Valid @RequestBody OrderRequest.ConfirmReturn request) {
        return R.ok(orderService.confirmReturn(id, request));
    }
}

package com.example.bookmanage.entity;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 库存变更流水，对应表 b_book_stock_log。
 */
@Data
public class BookStockLog {

    private Long id;
    private Long bookId;
    private Long operatorId;
    /** 正数入库、负数出库 */
    private Integer changeAmount;
    private String reason;
    private LocalDateTime createdAt;
}

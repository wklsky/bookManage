package com.example.bookmanage.entity;

import com.example.bookmanage.enums.BookStatus;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 图书，对应表 b_book。
 *
 * <p>totalStock 为馆藏总量，availableStock 为当前可借数量，
 * 两者之差即已借出未归还的册数，库存调整时必须保证差值不被击穿。
 */
@Data
public class Book {

    private Long id;
    private String title;
    private String author;
    private String isbn;
    private String publisher;
    private LocalDate publishDate;
    private String description;
    private String coverUrl;
    private Long categoryId;
    private Integer totalStock;
    private Integer availableStock;
    private BookStatus status;
    /** 软删除标记：0-正常，1-已下架删除。对应列 is_deleted，字段名需与下划线转驼峰后的结果一致 */
    private Boolean isDeleted;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

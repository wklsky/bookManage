package com.example.bookmanage.entity;

import com.example.bookmanage.enums.CategoryStatus;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 图书分类，对应表 b_category。
 */
@Data
public class BookCategory {

    private Long id;
    private String name;
    private String description;
    private Integer sortOrder;
    private CategoryStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    /** 关联图书数量，由查询统计后回填，不落库 */
    private Long bookCount;
}

package com.example.bookmanage.entity;

import com.example.bookmanage.enums.BookStatus;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 前台首页推荐位，对应表 b_featured_book。
 */
@Data
public class FeaturedBook {

    private Long id;
    private Long bookId;
    /** 展示排序权重，数值越小越靠前 */
    private int position;
    /** 是否对前台露出；false 表示后台保留配置但前台不展示 */
    private boolean enabled;
    private String remark;
    private Long createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // 以下为联表查询时填充的图书快照。推荐位自身字段很少，单独建投影类会让「插入用的实体」
    // 与「查询用的实体」割裂且容易写错，因此合并进同一实体；仅联表查询中有值，插入时保持为 null。
    private String title;
    private String author;
    private String isbn;
    private String coverUrl;
    private BookStatus status;
    private Integer totalStock;
    private Integer availableStock;
}

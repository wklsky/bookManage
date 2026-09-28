package com.example.bookmanage.dto.response;

import com.example.bookmanage.enums.BookStatus;

import java.time.LocalDateTime;

/**
 * 首页推荐位视图。除推荐位自身配置外，冗余图书快照，避免后台列表逐条再查图书。
 */
public record FeaturedBookVO(Long id,
                             Long bookId,
                             String title,
                             String author,
                             String isbn,
                             String coverUrl,
                             BookStatus bookStatus,
                             Integer totalStock,
                             Integer availableStock,
                             Integer position,
                             Boolean enabled,
                             String remark,
                             Long createdBy,
                             LocalDateTime createdAt,
                             LocalDateTime updatedAt) {
}

package com.example.bookmanage.dto.response;

import com.example.bookmanage.enums.CategoryStatus;

import java.time.LocalDateTime;

/**
 * 分类视图，bookCount 为关联图书数量。
 */
public record CategoryVO(Long id,
                         String name,
                         String description,
                         Integer sortOrder,
                         CategoryStatus status,
                         Long bookCount,
                         LocalDateTime createdAt,
                         LocalDateTime updatedAt) {
}

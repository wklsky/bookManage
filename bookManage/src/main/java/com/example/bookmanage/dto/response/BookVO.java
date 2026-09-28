package com.example.bookmanage.dto.response;

import com.example.bookmanage.dto.response.BriefVO.CategoryBrief;
import com.example.bookmanage.enums.BookStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 图书视图。
 */
public record BookVO(Long id,
                     String title,
                     String author,
                     String isbn,
                     String publisher,
                     LocalDate publishDate,
                     String description,
                     String coverUrl,
                     CategoryBrief category,
                     Integer totalStock,
                     Integer availableStock,
                     BookStatus status,
                     LocalDateTime createdAt,
                     LocalDateTime updatedAt) {
}

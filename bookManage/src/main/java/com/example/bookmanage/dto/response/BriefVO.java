package com.example.bookmanage.dto.response;

/**
 * 关联对象的精简视图，用于避免在列表接口中返回完整嵌套对象。
 */
public final class BriefVO {

    private BriefVO() {
    }

    public record CategoryBrief(Long id, String name) {
    }

    public record UserBrief(Long id, String username, String nickname) {
    }

    public record BookBrief(Long id, String title, String isbn, String coverUrl) {
    }
}

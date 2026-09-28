package com.example.bookmanage.dto.response;

import java.time.LocalDateTime;

/**
 * 站点前台展示配置视图。
 *
 * <p>对外是具名字段而不是 Map：后台表单按字段渲染，类型与长度校验也更明确，
 * 落库时再展开为键值行。
 */
public record SiteSettingsVO(String siteName,
                             String slogan,
                             String announcement,
                             Boolean announcementEnabled,
                             String bannerImage,
                             String theme,
                             LocalDateTime updatedAt) {
}

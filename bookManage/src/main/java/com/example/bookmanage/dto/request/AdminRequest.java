package com.example.bookmanage.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * 管理后台专属请求参数：首页推荐位与站点展示配置。
 *
 * <p>接口不在 swagger.json 中定义，属于管理后台独立能力，字段约束沿用项目既有风格。
 */
public final class AdminRequest {

    private AdminRequest() {
    }

    /** position、enabled 均允许为空：为空时分别取「排到最后」与「前台可见」 */
    public record FeaturedCreate(
            @NotNull(message = "必须指定图书")
            Long bookId,

            Integer position,

            @Size(max = 200, message = "推荐语长度不能超过 200 个字符")
            String remark,

            Boolean enabled) {
    }

    /** 推荐位为部分更新：未传字段保持原值，便于后台只调整排序或显隐 */
    public record FeaturedUpdate(
            Integer position,

            @Size(max = 200, message = "推荐语长度不能超过 200 个字符")
            String remark,

            Boolean enabled) {
    }

    public record SiteSettingsUpdate(
            @Size(max = 50, message = "站点名称长度不能超过 50 个字符")
            String siteName,

            @Size(max = 100, message = "站点标语长度不能超过 100 个字符")
            String slogan,

            @Size(max = 500, message = "公告内容长度不能超过 500 个字符")
            String announcement,

            Boolean announcementEnabled,

            @Size(max = 500, message = "横幅图片地址长度不能超过 500 个字符")
            String bannerImage,

            String theme) {
    }
}

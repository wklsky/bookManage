package com.example.bookmanage.common;

import java.util.Set;

/**
 * 站点展示配置允许写入的配置键白名单。
 *
 * <p>配置表是键值结构，如果不收敛键名，接口就能被当成任意键值存储来写脏数据。
 * 这里集中定义键名常量，Service 层据此校验后再落库。
 */
public final class SiteSettingKeys {

    public static final String SITE_NAME = "site.name";
    public static final String SLOGAN = "site.slogan";
    public static final String ANNOUNCEMENT = "site.announcement";
    public static final String ANNOUNCEMENT_ENABLED = "site.announcement.enabled";
    public static final String BANNER_IMAGE = "site.banner.image";
    public static final String THEME = "site.theme";

    public static final Set<String> ALL =
            Set.of(SITE_NAME, SLOGAN, ANNOUNCEMENT, ANNOUNCEMENT_ENABLED, BANNER_IMAGE, THEME);

    /** 可选主题。前端据此渲染不同配色，后端只做取值校验 */
    public static final Set<String> THEMES = Set.of("DEFAULT", "DARK", "WARM");

    private SiteSettingKeys() {
    }
}

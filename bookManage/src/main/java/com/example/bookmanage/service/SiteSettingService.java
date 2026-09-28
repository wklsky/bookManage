package com.example.bookmanage.service;

import com.example.bookmanage.common.SiteSettingKeys;
import com.example.bookmanage.dto.request.AdminRequest;
import com.example.bookmanage.dto.response.SiteSettingsVO;
import com.example.bookmanage.entity.SiteSetting;
import com.example.bookmanage.enums.AuditAction;
import com.example.bookmanage.enums.AuditTargetType;
import com.example.bookmanage.exception.BizException;
import com.example.bookmanage.mapper.SiteSettingMapper;
import com.example.bookmanage.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 站点前台展示配置。
 *
 * <p>对外是具名字段，落库是键值行：新增展示项时只改这里与 SiteSettingKeys，无需改表。
 */
@Service
@RequiredArgsConstructor
public class SiteSettingService {

    /** 未配置过时的兜底值，保证前台拿到的永远是可渲染的字符串而不是 null */
    private static final Map<String, String> DEFAULTS = Map.of(
            SiteSettingKeys.SITE_NAME, "阅界图书馆",
            SiteSettingKeys.SLOGAN, "让每一本书都被需要的人遇见",
            SiteSettingKeys.ANNOUNCEMENT, "",
            SiteSettingKeys.ANNOUNCEMENT_ENABLED, "false",
            SiteSettingKeys.BANNER_IMAGE, "",
            SiteSettingKeys.THEME, "DEFAULT");

    private static final Map<String, String> REMARKS = Map.of(
            SiteSettingKeys.SITE_NAME, "站点名称",
            SiteSettingKeys.SLOGAN, "站点标语",
            SiteSettingKeys.ANNOUNCEMENT, "首页公告",
            SiteSettingKeys.ANNOUNCEMENT_ENABLED, "公告是否启用",
            SiteSettingKeys.BANNER_IMAGE, "首页横幅图片",
            SiteSettingKeys.THEME, "站点主题");

    private final SiteSettingMapper siteSettingMapper;
    private final AuditLogService auditLogService;

    public SiteSettingsVO get() {
        List<SiteSetting> rows = siteSettingMapper.selectAll();
        Map<String, String> values = new HashMap<>(DEFAULTS);
        rows.forEach(setting -> {
            if (setting.getSettingValue() != null) {
                values.put(setting.getSettingKey(), setting.getSettingValue());
            }
        });
        return new SiteSettingsVO(
                values.get(SiteSettingKeys.SITE_NAME),
                values.get(SiteSettingKeys.SLOGAN),
                values.get(SiteSettingKeys.ANNOUNCEMENT),
                Boolean.parseBoolean(values.get(SiteSettingKeys.ANNOUNCEMENT_ENABLED)),
                values.get(SiteSettingKeys.BANNER_IMAGE),
                values.get(SiteSettingKeys.THEME),
                latestUpdatedAt(rows));
    }

    /** 配置为部分更新：只提交改动项，未提交的字段保持库中现值 */
    @Transactional
    public SiteSettingsVO update(AdminRequest.SiteSettingsUpdate request) {
        if (request.theme() != null && !SiteSettingKeys.THEMES.contains(request.theme())) {
            throw BizException.badRequest("不支持的主题取值：" + request.theme());
        }
        Long operatorId = SecurityUtils.currentUserId();
        upsert(SiteSettingKeys.SITE_NAME, request.siteName(), operatorId);
        upsert(SiteSettingKeys.SLOGAN, request.slogan(), operatorId);
        upsert(SiteSettingKeys.ANNOUNCEMENT, request.announcement(), operatorId);
        upsert(SiteSettingKeys.ANNOUNCEMENT_ENABLED,
                request.announcementEnabled() == null ? null : String.valueOf(request.announcementEnabled()),
                operatorId);
        upsert(SiteSettingKeys.BANNER_IMAGE, request.bannerImage(), operatorId);
        upsert(SiteSettingKeys.THEME, request.theme(), operatorId);

        auditLogService.record(AuditAction.SITE_SETTING_UPDATE, AuditTargetType.SITE, null, summarize(request));
        return get();
    }

    private void upsert(String key, String value, Long operatorId) {
        if (value == null) {
            return;
        }
        SiteSetting setting = new SiteSetting();
        setting.setSettingKey(key);
        setting.setSettingValue(value);
        setting.setRemark(REMARKS.get(key));
        setting.setUpdatedBy(operatorId);
        siteSettingMapper.upsert(setting);
    }

    private LocalDateTime latestUpdatedAt(List<SiteSetting> rows) {
        return rows.stream()
                .map(SiteSetting::getUpdatedAt)
                .filter(Objects::nonNull)
                .max(LocalDateTime::compareTo)
                .orElse(null);
    }

    private String summarize(AdminRequest.SiteSettingsUpdate request) {
        List<String> changed = new ArrayList<>();
        if (request.siteName() != null) {
            changed.add("站点名称=" + request.siteName());
        }
        if (request.slogan() != null) {
            changed.add("标语");
        }
        if (request.announcement() != null) {
            changed.add("公告");
        }
        if (request.announcementEnabled() != null) {
            changed.add("公告开关=" + request.announcementEnabled());
        }
        if (request.bannerImage() != null) {
            changed.add("横幅");
        }
        if (request.theme() != null) {
            changed.add("主题=" + request.theme());
        }
        return changed.isEmpty() ? "无变更" : String.join("，", changed);
    }
}

package com.example.bookmanage.entity;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 站点前台展示配置项，对应表 sys_site_setting。
 */
@Data
public class SiteSetting {

    /** 配置键，取值受 SiteSettingKeys 白名单约束 */
    private String settingKey;
    private String settingValue;
    private String remark;
    private Long updatedBy;
    private LocalDateTime updatedAt;
}

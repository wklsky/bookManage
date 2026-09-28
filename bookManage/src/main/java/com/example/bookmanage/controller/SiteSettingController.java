package com.example.bookmanage.controller;

import com.example.bookmanage.common.R;
import com.example.bookmanage.dto.response.SiteSettingsVO;
import com.example.bookmanage.service.SiteSettingService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 前台读取站点展示配置（站点名、标语、公告、横幅、主题）。
 *
 * <p>与后台的 {@code /api/admin/site-settings} 分开，避免把「谁能改」和「谁能看」混在一条规则里。
 */
@RestController
@RequestMapping("/api/site-settings")
@RequiredArgsConstructor
public class SiteSettingController {

    private final SiteSettingService siteSettingService;

    @GetMapping
    public R<SiteSettingsVO> detail() {
        return R.ok(siteSettingService.get());
    }
}

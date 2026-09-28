package com.example.bookmanage.controller;

import com.example.bookmanage.common.R;
import com.example.bookmanage.dto.request.AdminRequest;
import com.example.bookmanage.dto.response.SiteSettingsVO;
import com.example.bookmanage.service.SiteSettingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 管理后台：站点前台展示配置维护。
 */
@RestController
@RequestMapping("/api/admin/site-settings")
@RequiredArgsConstructor
public class AdminSiteSettingsController {

    private final SiteSettingService siteSettingService;

    @GetMapping
    public R<SiteSettingsVO> detail() {
        return R.ok(siteSettingService.get());
    }

    @PutMapping
    public R<SiteSettingsVO> update(@Valid @RequestBody AdminRequest.SiteSettingsUpdate request) {
        return R.ok(siteSettingService.update(request));
    }
}

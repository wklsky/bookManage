package com.example.bookmanage.controller;

import com.example.bookmanage.common.R;
import com.example.bookmanage.dto.response.DashboardVO;
import com.example.bookmanage.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 管理端统计概览接口。
 */
@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping("/summary")
    public R<DashboardVO> summary() {
        return R.ok(dashboardService.summary());
    }
}

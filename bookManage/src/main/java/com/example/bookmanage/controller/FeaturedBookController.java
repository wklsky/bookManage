package com.example.bookmanage.controller;

import com.example.bookmanage.common.R;
import com.example.bookmanage.dto.response.FeaturedBookVO;
import com.example.bookmanage.service.FeaturedBookService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 前台首页推荐位读取接口。
 *
 * <p>与后台管理接口分开：前台只能读到「已启用 + 图书已上架」的条目，
 * 且不带分页——首页一次性取完即可，分页对首页没有意义。
 */
@RestController
@RequestMapping("/api/featured-books")
@RequiredArgsConstructor
public class FeaturedBookController {

    private final FeaturedBookService featuredBookService;

    @GetMapping
    public R<List<FeaturedBookVO>> list() {
        return R.ok(featuredBookService.listVisible());
    }
}

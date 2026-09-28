package com.example.bookmanage.common;

import lombok.Data;

import java.util.Collections;
import java.util.List;

/**
 * 分页结果，字段与 swagger.json 的 XxxPageData 一致（page/size/total/pages/records）。
 *
 * <p>页码从 1 开始，与前端 PaginationBar 及 swagger 的 PageParam 约定保持一致。
 */
@Data
public class PageResult<T> {

    private long page;
    private long size;
    private long total;
    private long pages;
    private List<T> records;

    public static <T> PageResult<T> of(long page, long size, long total, List<T> records) {
        PageResult<T> result = new PageResult<>();
        result.page = page;
        result.size = size;
        result.total = total;
        result.pages = size <= 0 ? 0 : (total + size - 1) / size;
        result.records = records == null ? Collections.emptyList() : records;
        return result;
    }

    /** 前端直接取用 data，无匹配数据时返回空页而不是 null */
    public static <T> PageResult<T> empty(long page, long size) {
        return of(page, size, 0, Collections.emptyList());
    }
}

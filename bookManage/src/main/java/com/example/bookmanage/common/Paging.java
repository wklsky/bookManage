package com.example.bookmanage.common;

import com.example.bookmanage.exception.BizException;

/**
 * 分页参数校验与偏移量计算。
 *
 * <p>约定与 swagger.json 的 PageParam / SizeParam 一致：页码从 1 开始，每页最多 100 条。
 */
public final class Paging {

    public static final int DEFAULT_PAGE = 1;
    public static final int DEFAULT_SIZE = 10;
    public static final int MAX_SIZE = 100;

    private Paging() {
    }

    public static void check(int page, int size) {
        if (page < 1) {
            throw BizException.badRequest("页码必须不小于 1");
        }
        if (size < 1 || size > MAX_SIZE) {
            throw BizException.badRequest("每页数量需在 1 到 " + MAX_SIZE + " 之间");
        }
    }

    public static long offset(int page, int size) {
        return (long) (page - 1) * size;
    }
}

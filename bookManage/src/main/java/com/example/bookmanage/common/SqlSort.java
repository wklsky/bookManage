package com.example.bookmanage.common;

import java.util.Map;

/**
 * 排序子句解析器。
 *
 * <p>排序字段只能来自白名单再拼接进 SQL，绝不把前端传入的 sort 直接拼串，
 * 避免 ${} 占位符带来的 SQL 注入风险。
 */
public final class SqlSort {

    private SqlSort() {
    }

    /**
     * @param sort     形如 {@code createdAt,desc}
     * @param allowed  字段白名单：key 为对外字段名，value 为数据库列名
     * @param fallback 未指定或非法时使用的白名单 key
     * @return 可直接拼接的 {@code ORDER BY} 子句内容，如 {@code created_at DESC}
     */
    public static String resolve(String sort, Map<String, String> allowed, String fallback) {
        String key = fallback;
        String direction = "DESC";
        if (sort != null && !sort.isBlank()) {
            String[] parts = sort.split(",", 2);
            String candidate = parts[0].trim();
            if (allowed.containsKey(candidate)) {
                key = candidate;
            }
            if (parts.length > 1 && "asc".equalsIgnoreCase(parts[1].trim())) {
                direction = "ASC";
            }
        }
        return allowed.get(key) + " " + direction;
    }
}

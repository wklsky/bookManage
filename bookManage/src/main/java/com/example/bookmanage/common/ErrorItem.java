package com.example.bookmanage.common;

/**
 * 字段级错误明细，对应 swagger.json 的 ErrorDetail。
 */
public record ErrorItem(String field, String message) {
}

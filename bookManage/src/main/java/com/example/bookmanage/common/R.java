package com.example.bookmanage.common;

import lombok.Data;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * 统一响应包装。
 *
 * <p>成功结构为 {@code {code, message, data}}，失败结构在成功结构之外补充
 * {@code errors / requestId / timestamp}，与 swagger.json 的 BaseResponse、
 * ErrorResponse 一致。失败时 data 恒为 null。
 *
 * <p>空字段不序列化的策略由 spring.jackson.default-property-inclusion=non_null 全局控制，
 * 不在此处使用 Jackson 注解，避免与 Spring Boot 4 内置的 Jackson 3 注解坐标耦合。
 */
@Data
public class R<T> {

    private int code;
    private String message;
    private T data;
    private List<ErrorItem> errors;
    private String requestId;
    private String timestamp;

    public static <T> R<T> ok(T data) {
        return ok(200, "success", data);
    }

    public static <T> R<T> created(T data) {
        return ok(201, "success", data);
    }

    public static <T> R<T> ok(int code, String message, T data) {
        R<T> r = new R<>();
        r.code = code;
        r.message = message;
        r.data = data;
        return r;
    }

    public static <T> R<T> fail(int code, String message) {
        return fail(code, message, null, null);
    }

    public static <T> R<T> fail(int code, String message, List<ErrorItem> errors, String requestId) {
        R<T> r = new R<>();
        r.code = code;
        r.message = message;
        r.data = null;
        r.errors = errors;
        r.requestId = requestId;
        r.timestamp = OffsetDateTime.now().toString();
        return r;
    }
}

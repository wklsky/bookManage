package com.example.bookmanage.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * 业务异常。status 直接采用 HTTP 状态码，便于前端按 code 分支处理。
 */
@Getter
public class BizException extends RuntimeException {

    private final int status;

    public BizException(int status, String message) {
        super(message);
        this.status = status;
    }

    public static BizException badRequest(String message) {
        return new BizException(HttpStatus.BAD_REQUEST.value(), message);
    }

    public static BizException unauthorized(String message) {
        return new BizException(HttpStatus.UNAUTHORIZED.value(), message);
    }

    public static BizException forbidden(String message) {
        return new BizException(HttpStatus.FORBIDDEN.value(), message);
    }

    public static BizException notFound(String message) {
        return new BizException(HttpStatus.NOT_FOUND.value(), message);
    }

    public static BizException conflict(String message) {
        return new BizException(HttpStatus.CONFLICT.value(), message);
    }

    public static BizException unprocessable(String message) {
        return new BizException(HttpStatus.UNPROCESSABLE_ENTITY.value(), message);
    }
}

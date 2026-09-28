package com.example.bookmanage.exception;

import com.example.bookmanage.common.ErrorItem;
import com.example.bookmanage.common.R;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.List;
import java.util.UUID;

/**
 * 全局异常处理。
 *
 * <p>所有异常都被翻译成 swagger.json 约定的 ErrorResponse 结构，
 * 并带 requestId 便于前后端对照日志定位问题。
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BizException.class)
    public ResponseEntity<R<Void>> handleBiz(BizException ex) {
        String requestId = newRequestId();
        log.warn("业务异常 requestId={} status={} message={}", requestId, ex.getStatus(), ex.getMessage());
        return ResponseEntity.status(ex.getStatus())
                .body(R.fail(ex.getStatus(), ex.getMessage(), null, requestId));
    }

    /** 请求体校验失败：逐字段回传错误，前端可直接定位到表单项 */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<R<Void>> handleBodyValidation(MethodArgumentNotValidException ex) {
        List<ErrorItem> errors = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> new ErrorItem(error.getField(), defaultMessage(error)))
                .toList();
        String requestId = newRequestId();
        log.warn("参数校验失败 requestId={} errors={}", requestId, errors);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(R.fail(HttpStatus.BAD_REQUEST.value(), "请求参数校验失败", errors, requestId));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<R<Void>> handleConstraintViolation(ConstraintViolationException ex) {
        List<ErrorItem> errors = ex.getConstraintViolations().stream()
                .map(violation -> new ErrorItem(String.valueOf(violation.getPropertyPath()), violation.getMessage()))
                .toList();
        String requestId = newRequestId();
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(R.fail(HttpStatus.BAD_REQUEST.value(), "请求参数校验失败", errors, requestId));
    }

    /**
     * 请求体无法反序列化或参数类型不匹配。
     *
     * <p>必须记日志：这类错误在开发期极常见（字段名写错、JSON 格式错误、枚举值非法），
     * 若只返回一句笼统提示而不留痕，排查时无从下手。
     */
    @ExceptionHandler({HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class})
    public ResponseEntity<R<Void>> handleMalformedRequest(Exception ex) {
        String requestId = newRequestId();
        log.warn("请求无法解析 requestId={} reason={}", requestId, ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(R.fail(HttpStatus.BAD_REQUEST.value(), "请求格式错误或参数类型不匹配", null, requestId));
    }

    /** 兜底：日志打全量堆栈，响应只暴露 requestId，避免泄漏内部实现 */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<R<Void>> handleUnexpected(Exception ex) {
        String requestId = newRequestId();
        log.error("未处理异常 requestId={}", requestId, ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(R.fail(HttpStatus.INTERNAL_SERVER_ERROR.value(), "服务器内部错误", null, requestId));
    }

    private String defaultMessage(FieldError error) {
        String message = error.getDefaultMessage();
        return message == null ? "参数不合法" : message;
    }

    private String newRequestId() {
        return "req_" + UUID.randomUUID();
    }
}

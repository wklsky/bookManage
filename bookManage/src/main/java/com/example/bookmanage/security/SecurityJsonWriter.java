package com.example.bookmanage.security;

import com.example.bookmanage.common.R;
import tools.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

/**
 * 向过滤器层（而非 Controller 层）发生的异常写出统一错误响应。
 *
 * <p>认证与鉴权失败发生在 DispatcherServlet 之前，@RestControllerAdvice 无法覆盖，
 * 只能由 Spring Security 的入口点与拒绝处理器直接写响应体。
 */
public final class SecurityJsonWriter {

    private SecurityJsonWriter() {
    }

    public static void write(HttpServletResponse response, ObjectMapper objectMapper, int status, String message)
            throws IOException {
        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        R<Void> body = R.fail(status, message, null, "req_" + UUID.randomUUID());
        objectMapper.writeValue(response.getOutputStream(), body);
    }
}

package com.example.bookmanage.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;

/**
 * 项目自定义配置项，前缀 book.*。
 */
@Data
@Component
@ConfigurationProperties(prefix = "book")
public class BookProperties {

    private Jwt jwt = new Jwt();
    private Borrow borrow = new Borrow();
    private Cors cors = new Cors();

    @Data
    public static class Jwt {
        /** HS256 签名密钥，长度不低于 32 字节 */
        private String secret;
        private Duration accessTokenTtl = Duration.ofMinutes(30);
        private Duration refreshTokenTtl = Duration.ofDays(7);
    }

    @Data
    public static class Borrow {
        /** 借出时未指定 dueAt 的默认借阅天数 */
        private int defaultDays = 30;
    }

    @Data
    public static class Cors {
        /** 允许跨域的前端来源白名单 */
        private List<String> allowedOrigins = List.of("http://localhost:5173", "http://127.0.0.1:5173");
    }
}

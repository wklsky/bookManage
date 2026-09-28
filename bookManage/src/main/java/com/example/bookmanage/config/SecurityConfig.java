package com.example.bookmanage.config;

import com.example.bookmanage.security.JsonAccessDeniedHandler;
import com.example.bookmanage.security.JsonAuthenticationEntryPoint;
import com.example.bookmanage.security.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfigurationSource;

/**
 * 安全配置：无状态 + JWT + 基于角色的 URL 鉴权。
 *
 * <p>角色与 URL 的对应关系来自 swagger.json 的 x-roles 声明。
 * 读者与管理端接口在数据归属上的二次校验（如只能看自己的借阅单）放在 Service 层，
 * 因为那需要读取请求数据，URL 规则表达不了。
 */
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final JsonAuthenticationEntryPoint authenticationEntryPoint;
    private final JsonAccessDeniedHandler accessDeniedHandler;
    private final CorsConfigurationSource corsConfigurationSource;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(cors -> cors.configurationSource(corsConfigurationSource))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(handler -> handler
                        .authenticationEntryPoint(authenticationEntryPoint)
                        .accessDeniedHandler(accessDeniedHandler))
                .authorizeHttpRequests(auth -> auth
                        // 认证入口对匿名开放，刷新令牌也需要匿名访问
                        .requestMatchers(HttpMethod.POST,
                                "/api/auth/register",
                                "/api/auth/login",
                                "/api/auth/refresh").permitAll()

                        // 个人中心：所有已登录角色
                        .requestMatchers(HttpMethod.GET, "/api/users/profile").authenticated()
                        .requestMatchers(HttpMethod.PUT, "/api/users/profile").authenticated()
                        .requestMatchers(HttpMethod.PUT, "/api/users/profile/password").authenticated()

                        // 用户管理：仅系统管理员。profile 必须排在 {id} 之前，否则会被 ADMIN 规则拦截
                        .requestMatchers(HttpMethod.GET, "/api/users").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/users/{id}").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/users/{id}/status").hasRole("ADMIN")

                        // 分类：查询对已登录用户开放，写操作限管理员与图书管理员
                        .requestMatchers(HttpMethod.GET, "/api/categories").authenticated()
                        .requestMatchers(HttpMethod.GET, "/api/categories/{id}").authenticated()
                        .requestMatchers(HttpMethod.POST, "/api/categories").hasAnyRole("LIBRARIAN", "ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/categories/{id}").hasAnyRole("LIBRARIAN", "ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/categories/{id}").hasAnyRole("LIBRARIAN", "ADMIN")

                        // 图书：同上
                        .requestMatchers(HttpMethod.GET, "/api/books").authenticated()
                        .requestMatchers(HttpMethod.GET, "/api/books/{id}").authenticated()
                        .requestMatchers(HttpMethod.POST, "/api/books").hasAnyRole("LIBRARIAN", "ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/books/{id}").hasAnyRole("LIBRARIAN", "ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/books/{id}").hasAnyRole("LIBRARIAN", "ADMIN")
                        .requestMatchers(HttpMethod.PATCH, "/api/books/{id}/stock").hasAnyRole("LIBRARIAN", "ADMIN")

                        // 借阅单：全部记录与状态流转动作限管理端，mine / 预约 / 取消 / 还书归读者
                        .requestMatchers(HttpMethod.GET, "/api/orders").hasAnyRole("LIBRARIAN", "ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/orders/mine").authenticated()
                        .requestMatchers(HttpMethod.POST, "/api/orders/reserve").authenticated()
                        .requestMatchers(HttpMethod.GET, "/api/orders/{id}").authenticated()
                        .requestMatchers(HttpMethod.PUT, "/api/orders/{id}/cancel").authenticated()
                        .requestMatchers(HttpMethod.PUT, "/api/orders/{id}/return").authenticated()
                        .requestMatchers(HttpMethod.PUT, "/api/orders/{id}/audit").hasAnyRole("LIBRARIAN", "ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/orders/{id}/checkout").hasAnyRole("LIBRARIAN", "ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/orders/{id}/confirm-return")
                        .hasAnyRole("LIBRARIAN", "ADMIN")

                        .requestMatchers(HttpMethod.GET, "/api/dashboard/summary").hasAnyRole("LIBRARIAN", "ADMIN")

                        // 前台展示内容与推荐位：登录用户可读，写入一律归管理端
                        .requestMatchers(HttpMethod.GET, "/api/site-settings").authenticated()
                        .requestMatchers(HttpMethod.GET, "/api/featured-books").authenticated()

                        // 推荐位维护属于馆藏运营，图书管理员即可操作
                        .requestMatchers(HttpMethod.GET, "/api/admin/featured-books").hasAnyRole("LIBRARIAN", "ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/admin/featured-books").hasAnyRole("LIBRARIAN", "ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/admin/featured-books/{id}")
                        .hasAnyRole("LIBRARIAN", "ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/admin/featured-books/{id}")
                        .hasAnyRole("LIBRARIAN", "ADMIN")

                        // 站点文案与主题会影响整站观感，只交给系统管理员，避免图书管理员误改全局
                        .requestMatchers(HttpMethod.GET, "/api/admin/site-settings").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/admin/site-settings").hasRole("ADMIN")

                        // 审计日志可回溯他人操作，仅系统管理员可见
                        .requestMatchers(HttpMethod.GET, "/api/admin/audit-logs").hasRole("ADMIN")

                        // 重置他人密码会强制对方全部设备下线，风险高，仅系统管理员
                        .requestMatchers(HttpMethod.PUT, "/api/admin/users/{id}/password").hasRole("ADMIN")
                        // 查看某读者的借阅记录属于日常运营，图书管理员即可
                        .requestMatchers(HttpMethod.GET, "/api/admin/users/{id}/orders")
                        .hasAnyRole("LIBRARIAN", "ADMIN")

                        .anyRequest().authenticated())
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}

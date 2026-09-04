package com.example.test.config;

import com.example.test.security.PermissionInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web 配置：CORS 与权限拦截器，对应 Express 侧 cors 中间件 + authorize 中间件
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final AppProperties appProperties;
    private final PermissionInterceptor permissionInterceptor;

    public WebConfig(AppProperties appProperties, PermissionInterceptor permissionInterceptor) {
        this.appProperties = appProperties;
        this.permissionInterceptor = permissionInterceptor;
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        String[] origins = appProperties.getCors().getOrigins().split(",");
        String[] trimmed = java.util.Arrays.stream(origins).map(String::trim).filter(s -> !s.isEmpty()).toArray(String[]::new);
        registry.addMapping("/**")
                .allowedOriginPatterns(trimmed)
                .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true)
                .maxAge(3600);
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(permissionInterceptor).addPathPatterns("/api/v1/**");
    }
}

package com.techhub.config;

import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.filter.CharacterEncodingFilter;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web MVC 配置
 * 跨域（CORS）+ 字符编码（UTF-8）+ 静态文件映射
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Value("${UPLOAD_PATH:/home/Lsk/Documents/Code/Projects/TechHubBBS/backend/uploads/}")
    private String uploadPath;

    @Value("${cors.allowed-origins}")
    private List<String> allowedOrigins;

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOrigins(allowedOrigins.toArray(new String[0]))
                .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true);
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/file/**")
                .addResourceLocations("file:" + uploadPath);
    }

    /**
     * 强制 UTF-8 编码，防止中文响应乱码
     * 优先级最高，确保在 Security Filter Chain 之前生效
     */
    @Bean
    public FilterRegistrationBean<CharacterEncodingFilter> customCharacterEncodingFilter() {
        CharacterEncodingFilter filter = new CharacterEncodingFilter();
        filter.setEncoding("UTF-8");
        filter.setForceEncoding(true);

        FilterRegistrationBean<CharacterEncodingFilter> registration = new FilterRegistrationBean<>();
        registration.setFilter(filter);
        registration.addUrlPatterns("/*");
        registration.setOrder(Integer.MIN_VALUE);
        return registration;
    }
}

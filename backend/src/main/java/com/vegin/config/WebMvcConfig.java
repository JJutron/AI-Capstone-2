package com.vegin.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // favicon.ico 요청을 무시하도록 설정
        registry.addResourceHandler("/favicon.ico")
                .addResourceLocations("classpath:/static/")
                .resourceChain(false);
        
        // 빈 경로나 잘못된 정적 리소스 요청 방지
        // Spring Security가 처리하도록 함
    }
}
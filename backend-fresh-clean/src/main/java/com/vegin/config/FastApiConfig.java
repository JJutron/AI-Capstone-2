package com.vegin.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Slf4j
@Configuration
public class FastApiConfig {

    @Bean
    public RestClient fastApiRestClient(@Value("${ai.fastapi.base-url}") String baseUrl) {
        if (baseUrl == null || baseUrl.isBlank()) {
            log.warn("[FastAPI] ai.fastapi.base-url 이 비어 있습니다. application.yml 을 확인하세요.");
        } else {
            log.info("[FastAPI] baseUrl={}", baseUrl);
        }

        return RestClient.builder()
                .baseUrl(baseUrl)
                .build();
    }
}
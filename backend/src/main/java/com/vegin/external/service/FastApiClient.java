package com.vegin.external.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vegin.external.dto.FastApiResponseDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.MultiValueMap;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;

import java.util.Map;

@Component
@Slf4j
public class FastApiClient {

    private final RestClient restClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public FastApiClient(@Value("${ai.fastapi.base-url}") String baseUrl) {
        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .build();
        log.info("[FastAPI] baseUrl={}", baseUrl);
    }

    /**
     * 1) 개발용: 파일 그대로 FastAPI(image_file)로 보냄
     */
    public FastApiResponseDto analyzeWithImageFile(MultipartFile file, String surveyJson) {
        try {
            log.info("[FastAPI] /analyze-and-recommend (image_file) 호출 시작, filename={}", file.getOriginalFilename());

            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();

            ByteArrayResource imageResource = new ByteArrayResource(file.getBytes()) {
                @Override
                public String getFilename() {
                    return file.getOriginalFilename();
                }
            };

            HttpHeaders imageHeaders = new HttpHeaders();
            imageHeaders.setContentType(MediaType.parseMediaType(
                    file.getContentType() != null ? file.getContentType() : "image/jpeg"
            ));
            HttpEntity<ByteArrayResource> imagePart = new HttpEntity<>(imageResource, imageHeaders);

            HttpHeaders surveyHeaders = new HttpHeaders();
            surveyHeaders.setContentType(MediaType.TEXT_PLAIN);
            HttpEntity<String> surveyPart = new HttpEntity<>(surveyJson != null ? surveyJson : "{}", surveyHeaders);

            body.add("image_file", imagePart);
            body.add("survey", surveyPart);

            // ★ 원본 JSON 문자열로 먼저 받기
            String raw = restClient.post()
                    .uri("/analyze-and-recommend")
                    .contentType(MediaType.MULTIPART_FORM_DATA)
                    .body(body)
                    .retrieve()
                    .body(String.class);

            log.info("[FastAPI] raw response (image_file) = {}", raw);

            if (raw == null || raw.isBlank()) {
                throw new RuntimeException("FastAPI 응답이 비어 있습니다.");
            }

            return objectMapper.readValue(raw, FastApiResponseDto.class);

        } catch (Exception e) {
            log.error("[FastAPI] /analyze-and-recommend (image_file) 호출 실패", e);
            throw new RuntimeException("FastAPI 호출 실패 (image_file)", e);
        }
    }

    /**
     * 2) 실서비스용: S3 image_url 로 전송
     */
    public FastApiResponseDto analyzeWithImageUrl(String imageUrl, String surveyJson) {
        try {
            log.info("[FastAPI] /analyze-and-recommend (image_url) 호출 시작, url={}", imageUrl);
            
            // ✅ surveyJson 검증 및 로깅
            log.info("[FastAPI] surveyJson (raw) = {}", surveyJson);
            
            if (surveyJson == null || surveyJson.isBlank() || surveyJson.equals("{}")) {
                log.warn("[FastAPI] ⚠️ surveyJson이 비어있거나 유효하지 않습니다!");
                surveyJson = "{}";
            } else {
                // ✅ JSON 파싱해서 키 확인
                try {
                    Map<String, Object> surveyMap = objectMapper.readValue(surveyJson, Map.class);
                    log.info("[FastAPI] surveyJson 파싱 성공. 키 개수: {}, 키 목록: {}", 
                            surveyMap.size(), surveyMap.keySet());
                    
                    // ✅ q1~q10 키 확인
                    boolean hasQ1 = surveyMap.containsKey("q1");
                    if (!hasQ1) {
                        log.error("[FastAPI] ⚠️ surveyJson에 'q1' 키가 없습니다! 전체 내용: {}", surveyJson);
                        log.error("[FastAPI] ⚠️ FastAPI에서 KeyError: 'q1' 에러가 발생할 수 있습니다!");
                    }
                    
                    // ✅ 모든 q1~q10 키 확인
                    for (int i = 1; i <= 10; i++) {
                        String key = "q" + i;
                        if (!surveyMap.containsKey(key)) {
                            log.warn("[FastAPI] ⚠️ surveyJson에 '{}' 키가 없습니다.", key);
                        }
                    }
                } catch (Exception e) {
                    log.error("[FastAPI] surveyJson 파싱 실패: {}", surveyJson, e);
                }
            }

            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();

            HttpHeaders urlHeaders = new HttpHeaders();
            urlHeaders.setContentType(MediaType.TEXT_PLAIN);
            HttpEntity<String> urlPart = new HttpEntity<>(imageUrl, urlHeaders);

            HttpHeaders surveyHeaders = new HttpHeaders();
            surveyHeaders.setContentType(MediaType.TEXT_PLAIN);
            HttpEntity<String> surveyPart = new HttpEntity<>(surveyJson, surveyHeaders);

            body.add("image_url", urlPart);
            body.add("survey", surveyPart);

            try {
                String raw = restClient.post()
                        .uri("/analyze-and-recommend")
                        .contentType(MediaType.MULTIPART_FORM_DATA)
                        .body(body)
                        .retrieve()
                        .body(String.class);

                log.info("[FastAPI] raw response (image_url) = {}", raw);

                if (raw == null || raw.isBlank()) {
                    throw new RuntimeException("FastAPI 응답이 비어 있습니다.");
                }

                return objectMapper.readValue(raw, FastApiResponseDto.class);

            } catch (HttpServerErrorException e) {
                // ✅ 500 에러 상세 로깅
                String errorBody = e.getResponseBodyAsString();
                log.error("[FastAPI] ⚠️ 500 에러 발생!");
                log.error("[FastAPI] 요청 URL: {}", imageUrl);
                log.error("[FastAPI] 요청 Survey: {}", surveyJson);
                log.error("[FastAPI] 에러 응답 본문: {}", errorBody);
                log.error("[FastAPI] 에러 상태 코드: {}", e.getStatusCode());
                throw new RuntimeException("FastAPI 500 에러: " + errorBody, e);
            }

        } catch (Exception e) {
            log.error("[FastAPI] /analyze-and-recommend (image_url) 호출 실패, url={}", imageUrl, e);
            throw new RuntimeException("FastAPI 호출 실패 (image_url): " + e.getMessage(), e);
        }
    }
}
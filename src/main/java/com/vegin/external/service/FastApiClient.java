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
import org.springframework.web.multipart.MultipartFile;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;

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

            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();

            HttpHeaders urlHeaders = new HttpHeaders();
            urlHeaders.setContentType(MediaType.TEXT_PLAIN);
            HttpEntity<String> urlPart = new HttpEntity<>(imageUrl, urlHeaders);

            HttpHeaders surveyHeaders = new HttpHeaders();
            surveyHeaders.setContentType(MediaType.TEXT_PLAIN);
            HttpEntity<String> surveyPart = new HttpEntity<>(surveyJson != null ? surveyJson : "{}", surveyHeaders);

            body.add("image_url", urlPart);
            body.add("survey", surveyPart);

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

        } catch (Exception e) {
            log.error("[FastAPI] /analyze-and-recommend (image_url) 호출 실패", e);
            throw new RuntimeException("FastAPI 호출 실패 (image_url)", e);
        }
    }
}
//package com.vegin.external.service;
//
//import com.vegin.external.dto.FastApiResponseDto;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.core.io.ByteArrayResource;
//import org.springframework.http.MediaType;
//import org.springframework.http.client.MultipartBodyBuilder;
//import org.springframework.stereotype.Component;
//import org.springframework.util.MultiValueMap;
//import org.springframework.web.client.RestClient;
//import org.springframework.web.multipart.MultipartFile;
//import lombok.RequiredArgsConstructor;
//
//@Slf4j
//@Component
//@RequiredArgsConstructor
//public class FastApiClient {
//
//    // FastApiConfig 에서 만든 빈
//    private final RestClient fastApiRestClient;
//
//    /**
//     * FastAPI의 /analyze-and-recommend 동기 호출
//     *
//     * @param file       업로드된 피부 이미지
//     * @param surveyJson 설문 JSON 문자열
//     * @return FastAPI에서 내려준 원본 JSON 문자열
//     */
//    public String analyzeAndRecommend(MultipartFile file, String surveyJson) {
//        if (file == null || file.isEmpty()) {
//            throw new IllegalArgumentException("FastAPI 호출 실패: 이미지 파일이 비었습니다.");
//        }
//
//        try {
//            MultipartBodyBuilder bodyBuilder = new MultipartBodyBuilder();
//
//            // image 파트
//            ByteArrayResource imageResource = new ByteArrayResource(file.getBytes()) {
//                @Override
//                public String getFilename() {
//                    return file.getOriginalFilename();
//                }
//            };
//
//            bodyBuilder
//                    .part("image_file", imageResource)
//                    .filename(file.getOriginalFilename())
//                    .contentType(MediaType.parseMediaType(
//                            file.getContentType() != null ? file.getContentType() : "image/jpeg"
//                    ));
//
//            // survey 파트- FastAPI Form(...)
//            bodyBuilder
//                    .part("survey", surveyJson != null ? surveyJson : "{}")
//                    .contentType(MediaType.TEXT_PLAIN);
//
//            log.info("[FastAPI] /analyze-and-recommend 요청 시작");
//
//            String responseBody = fastApiRestClient.post()
//                    .uri("/analyze-and-recommend")
//                    .contentType(MediaType.MULTIPART_FORM_DATA)
//                    .body(bodyBuilder.build())
//                    .retrieve()
//                    .body(String.class);
//
//            log.info("[FastAPI] /analyze-and-recommend 응답 수신 완료");
//
//            return responseBody;
//
//        } catch (Exception e) {
//            log.error("[FastAPI] /analyze-and-recommend 호출 실패: {}", e.getMessage(), e);
//            throw new RuntimeException("FastAPI 호출 실패", e);
//        }
//    }
//}
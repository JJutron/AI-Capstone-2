package com.vegin.module.analysis.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vegin.common.S3Service;
import com.vegin.dto.response.AnalysisUploadResponse;

import com.vegin.external.dto.FastApiResponseDto;
import com.vegin.external.service.FastApiClient;
import com.vegin.module.analysis.domain.SkinAnalysis;
import com.vegin.module.analysis.repository.SkinAnalysisRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class SkinAnalysisService {

    private final S3Service s3;
    private final SkinAnalysisRepository analyses;
    private final FastApiClient fastApiClient;
    private final ObjectMapper objectMapper;   // FastAPI 응답을 JSON 문자열로 저장하기 위함

    /**
     * 동기 플로우:
     * 1) S3 업로드
     * 2) DB에 PENDING 상태로 insert
     * 3) FastAPI 동기 호출 (/analyze-and-recommend, image_url 사용)
     * 4) 결과 JSON + DONE 상태로 업데이트
     * 5) FE에 analysisId, imageUrl 반환
     */
    @Transactional
    public AnalysisUploadResponse uploadAndAnalyze(Long userId,
                                                   MultipartFile file,
                                                   String surveyJson) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("이미지 파일이 비어 있습니다.");
        }

        // 1) S3 Object Key 생성
        String key = generateKey(userId, file.getOriginalFilename());

        // 2) S3 업로드
        log.info("[S3] upload start. key={}", key);
        s3.upload(file, key);
        String imageUrl = s3.getUrl(key);
        log.info("[S3] upload done. url={}", imageUrl);

        // 3) DB에 PENDING 상태로 저장
        SkinAnalysis entity = SkinAnalysis.builder()
                .userId(userId)
                .s3Key(key)
                .userInput(surveyJson)   // 설문 JSON 문자열 그대로 저장
                .status("PENDING")
                .build();

        entity = analyses.save(entity); // PK 발급
        log.info("[SkinAnalysis] saved PENDING. id={}", entity.getId());

        // 4) FastAPI 동기 호출 (image_url 방식)
        FastApiResponseDto fastApiRes =
                fastApiClient.analyzeWithImageUrl(imageUrl, surveyJson);

        String fastApiResultJson = toJsonSafely(fastApiRes);

        // 5) 결과 반영 (DONE + result JSON)
        SkinAnalysis updated = SkinAnalysis.builder()
                .id(entity.getId())
                .userId(entity.getUserId())
                .s3Key(entity.getS3Key())
                .userInput(entity.getUserInput())
                .status("DONE")
                .result(fastApiResultJson)
                .createdAt(entity.getCreatedAt()) // 기존 createdAt 유지
                .build();

        analyses.save(updated);
        log.info("[SkinAnalysis] updated DONE. id={}", updated.getId());

        // 6) FE용 응답 (일단 최소 정보만)
        return new AnalysisUploadResponse(updated.getId(), imageUrl);
    }

    /**
     * (옵션) 로컬/백엔드 테스트용:
     * S3를 거치지 않고 파일 자체를 FastAPI에 보내고 싶은 경우 사용할 수 있는 유틸.
     * DB에는 저장하지 않음.
     */
    @Transactional(readOnly = true)
    public FastApiResponseDto analyzeFileOnlyForTest(MultipartFile file,
                                                     String surveyJson) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("이미지 파일이 비어 있습니다.");
        }
        log.info("[SkinAnalysis] TEST ONLY - 직접 파일로 FastAPI 호출 시작");
        return fastApiClient.analyzeWithImageFile(file, surveyJson);
    }

    // ================== 내부 유틸 ==================

    private String generateKey(Long userId, String originalName) {
        String ext = "";
        if (originalName != null) {
            int idx = originalName.lastIndexOf('.');
            if (idx >= 0) ext = originalName.substring(idx);
        }
        return "analysis/%d/%d_%s%s".formatted(
                userId,
                System.currentTimeMillis(),
                UUID.randomUUID().toString().substring(0, 8),
                ext
        );
    }

    private String toJsonSafely(FastApiResponseDto dto) {
        try {
            return objectMapper.writeValueAsString(dto);
        } catch (JsonProcessingException e) {
            log.error("[SkinAnalysis] FastAPI 응답 직렬화 실패", e);
            // 필요하면 별도 예외 타입
            throw new RuntimeException("FastAPI 응답 JSON 직렬화 실패", e);
        }
    }
}
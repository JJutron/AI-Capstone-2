package com.vegin.module.analysis.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vegin.common.S3Service;
import com.vegin.domain.BSTInfo;
import com.vegin.dto.response.AnalysisUploadResponse;
import com.vegin.dto.response.CategoryRecommendationResponse;
import com.vegin.external.dto.FastApiResponseDto;
import com.vegin.external.service.FastApiClient;
import com.vegin.module.analysis.AnalysisResultResponse;
import com.vegin.module.analysis.domain.SkinAnalysis;
import com.vegin.module.analysis.repository.SkinAnalysisRepository;
import com.vegin.module.users.Entity.User;
import com.vegin.module.users.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class SkinAnalysisService {

    private final UserRepository userRepo;
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

    @Transactional(readOnly = true)
    public AnalysisResultResponse getAnalysisResult(Long analysisId, Long userId) {
        SkinAnalysis analysis = analyses.findByIdAndUserId(analysisId, userId)
                .orElseThrow(() -> new EntityNotFoundException("분석 결과를 찾을 수 없습니다."));

        if (analysis.getResult() == null || analysis.getResult().isEmpty()) {
            throw new IllegalArgumentException("분석 결과가 아직 준비되지 않았습니다.");
        }

        // JSON 문자열을 FastApiResponseDto로 파싱
        FastApiResponseDto fastApiResponse;
        try {
            fastApiResponse = objectMapper.readValue(analysis.getResult(), FastApiResponseDto.class);
        } catch (JsonProcessingException e) {
            log.error("[SkinAnalysis] JSON 파싱 실패. analysisId={}", analysisId, e);
            throw new RuntimeException("분석 결과를 읽는 중 오류가 발생했습니다.", e);
        }

        if (fastApiResponse.fusion() == null) {
            throw new IllegalArgumentException("분석 결과에 fusion 데이터가 없습니다.");
        }

        Map<String, Object> fusion = fastApiResponse.fusion();
        String skinType = (String) fusion.get("skin_type");
        String mbti = (String) fusion.get("skin_mbti");
        Map<String, Object> indices = (Map<String, Object>) fusion.get("indices");
        Map<String, Object> visionRaw = (Map<String, Object>) fusion.get("vision_raw");

        BSTInfo bst = BSTInfo.fromCode(mbti);
        if (bst == null) {
            log.warn("[SkinAnalysis] 알 수 없는 MBTI 코드: {}", mbti);
        }

        User user = userRepo.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("사용자를 찾을 수 없습니다."));

        Map<String, Boolean> actions = new HashMap<>();
        actions.put("canRetake", true);
        actions.put("canShare", true);
        actions.put("canSave", true);

        // recommendations 파싱 및 변환
        List<CategoryRecommendationResponse> recommendationsList = parseRecommendations(fastApiResponse.recommendations());

        return AnalysisResultResponse.builder()
                .userName(user.getNickname())
                .skinMbtiType(mbti)
                .skinDisplayName(skinType != null ? skinType : "")
                .headline(bst != null ? bst.getHeadline() : "")
                .skinDescription(bst != null ? bst.getDescription() : "")
                .whiteListIngredients(bst != null ? bst.getWhiteListIngredients() : java.util.Collections.emptyList())
                .whiteListRecommendation(bst != null ? bst.getWhiteListRecommendation() : "")
                .blackListIngredients(bst != null ? bst.getBlackListIngredients() : java.util.Collections.emptyList())
                .axis(indices != null ? indices : new HashMap<>())
                .concerns(visionRaw != null ? visionRaw : new HashMap<>())
                .actions(actions)
                .recommendations(recommendationsList)
                .build();
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

    /**
     * FastAPI recommendations 배열을 CategoryRecommendationResponse 리스트로 변환
     */
    private List<CategoryRecommendationResponse> parseRecommendations(List<Map<String, Object>> recommendations) {
        if (recommendations == null || recommendations.isEmpty()) {
            return new ArrayList<>();
        }

        return recommendations.stream()
                .map(product -> {
                    try {
                        String productId = (String) product.get("productId");
                        String productName = (String) product.get("productName");
                        String brand = (String) product.get("brand");
                        String category = (String) product.get("category");
                        String imageUrl = (String) product.get("image_url");
                        
                        // 숫자 필드 파싱 (null-safe)
                        Integer salePrice = null;
                        if (product.get("salePrice") != null) {
                            if (product.get("salePrice") instanceof Number) {
                                salePrice = ((Number) product.get("salePrice")).intValue();
                            }
                        }
                        
                        Double averageReviewScore = null;
                        if (product.get("averageReviewScore") != null) {
                            if (product.get("averageReviewScore") instanceof Number) {
                                averageReviewScore = ((Number) product.get("averageReviewScore")).doubleValue();
                            }
                        }
                        
                        Integer totalReviewCount = null;
                        if (product.get("totalReviewCount") != null) {
                            if (product.get("totalReviewCount") instanceof Number) {
                                totalReviewCount = ((Number) product.get("totalReviewCount")).intValue();
                            }
                        }
                        
                        // xai_keywords 파싱
                        List<String> xaiKeywords = new ArrayList<>();
                        Object keywordsObj = product.get("xai_keywords");
                        if (keywordsObj instanceof List) {
                            xaiKeywords = ((List<?>) keywordsObj).stream()
                                    .map(Object::toString)
                                    .collect(Collectors.toList());
                        }

                        return CategoryRecommendationResponse.builder()
                                .productId(productId)
                                .productName(productName != null ? productName : "")
                                .brand(brand)
                                .salePrice(salePrice)
                                .averageReviewScore(averageReviewScore)
                                .totalReviewCount(totalReviewCount)
                                .category(category != null ? category : "")
                                .imageUrl(imageUrl != null ? imageUrl : "")
                                .xaiKeywords(xaiKeywords)
                                .tags(xaiKeywords) // 프론트엔드 호환성을 위해 tags도 설정
                                .build();
                    } catch (Exception e) {
                        log.warn("[SkinAnalysis] 추천 제품 파싱 실패. product={}", product, e);
                        return null;
                    }
                })
                .filter(item -> item != null)
                .collect(Collectors.toList());
    }
}
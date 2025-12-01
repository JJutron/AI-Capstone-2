package com.vegin.module.users.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vegin.common.S3Service;
import com.vegin.dto.request.ProfileUpdateRequest;
import com.vegin.dto.response.CategoryRecommendationResponse;
import com.vegin.dto.response.ProfileResponse;
import com.vegin.external.dto.FastApiResponseDto;
import com.vegin.module.analysis.domain.Recommendation;
import com.vegin.module.analysis.domain.SkinAnalysis;
import com.vegin.module.analysis.repository.RecommendationRepository;
import com.vegin.module.analysis.repository.SkinAnalysisRepository;
import com.vegin.module.users.Entity.SkinProfile;
import com.vegin.module.users.repository.SkinProfileRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class SkinProfileService {

    private final SkinProfileRepository profiles;
    private final SkinAnalysisRepository analyses;
    private final RecommendationRepository recommendations;
    private final ObjectMapper objectMapper;
    private final S3Service s3Service;

    /**
     * 마이페이지 조회용
     */
    @Transactional(readOnly = true)
    public Optional<SkinProfile> get(Long userId) {
        return profiles.findByUserId(userId);
    }

    /**
     * 프로필 조회 (프로필 정보 + 최근 분석 기록)
     */
    @Transactional
    public ProfileResponse getProfile(Long userId) {
        SkinProfile profile = profiles.findByUserId(userId).orElse(null);

        // concerns 파싱
        List<String> concernsList = Collections.emptyList();
        if (profile != null && profile.getConcerns() != null && !profile.getConcerns().isEmpty()) {
            try {
                concernsList = objectMapper.readValue(profile.getConcerns(), new TypeReference<List<String>>() {});
            } catch (Exception e) {
                log.warn("[SkinProfile] concerns JSON 파싱 실패. userId={}", userId, e);
            }
        }

        // 최근 분석 기록 조회 (최대 10개)
        List<SkinAnalysis> recentAnalyses = analyses.findByUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .limit(10)
                .collect(Collectors.toList());

        // 마지막 분석 정보
        ProfileResponse.LastAnalysis lastAnalysis = null;
        Long latestAnalysisId = null;
        String latestAnalysisSkinType = null;
        if (!recentAnalyses.isEmpty()) {
            SkinAnalysis latest = recentAnalyses.get(0);
            latestAnalysisId = latest.getId();
            if (latest.getResult() != null && !latest.getResult().isEmpty()) {
                try {
                    FastApiResponseDto dto = objectMapper.readValue(
                            latest.getResult(),
                            FastApiResponseDto.class
                    );
                    if (dto.fusion() != null) {
                        String mbti = (String) dto.fusion().get("skin_mbti");
                        latestAnalysisSkinType = (String) dto.fusion().get("skin_type");
                        
                        // concerns 추출 (vision_raw)
                        Map<String, Object> concernsMap = new HashMap<>();
                        Map<String, Object> visionRaw = (Map<String, Object>) dto.fusion().get("vision_raw");
                        if (visionRaw != null) {
                            concernsMap = visionRaw;
                        }
                        
                        // recommendations 파싱
                        List<CategoryRecommendationResponse> recommendationsList = parseRecommendations(dto.recommendations());
                        
                        lastAnalysis = ProfileResponse.LastAnalysis.builder()
                                .analysisId(latestAnalysisId)
                                .mbti(mbti)
                                .skinType(latestAnalysisSkinType)
                                .date(latest.getCreatedAt())
                                .concerns(concernsMap)
                                .recommendations(recommendationsList)
                                .build();
                    }
                } catch (Exception e) {
                    log.warn("[SkinProfile] 마지막 분석 결과 파싱 실패. analysisId={}", latest.getId(), e);
                }
            }
        }

        // skinType이 null이면 최신 분석 결과의 skinType 사용
        String finalSkinType = profile != null ? profile.getSkinType() : null;
        if (finalSkinType == null && latestAnalysisSkinType != null) {
            finalSkinType = latestAnalysisSkinType;
            // DB에도 업데이트 (기본 skinType으로 설정)
            if (profile != null) {
                SkinProfile updated = SkinProfile.builder()
                        .id(profile.getId())
                        .userId(profile.getUserId())
                        .skinType(finalSkinType)
                        .concerns(profile.getConcerns())
                        .mbti(profile.getMbti())
                        .tone(profile.getTone())
                        .profileImageUrl(profile.getProfileImageUrl())
                        .build();
                profiles.save(updated);
            } else {
                // profile이 없으면 새로 생성
                SkinProfile newProfile = SkinProfile.builder()
                        .userId(userId)
                        .skinType(finalSkinType)
                        .concerns("[]")
                        .build();
                profiles.save(newProfile);
            }
        }

        // 분석 히스토리
        List<ProfileResponse.AnalysisHistory> history = recentAnalyses.stream()
                .map(analysis -> {
                    String imageUrl = null;
                    if (analysis.getS3Key() != null) {
                        imageUrl = s3Service.getUrl(analysis.getS3Key());
                    }
                    
                    // concerns와 recommendations 추출
                    Map<String, Object> concernsMap = new HashMap<>();
                    List<CategoryRecommendationResponse> recommendationsList = new ArrayList<>();
                    
                    if (analysis.getResult() != null && !analysis.getResult().isEmpty()) {
                        try {
                            FastApiResponseDto dto = objectMapper.readValue(
                                    analysis.getResult(),
                                    FastApiResponseDto.class
                            );
                            if (dto.fusion() != null) {
                                Map<String, Object> visionRaw = (Map<String, Object>) dto.fusion().get("vision_raw");
                                if (visionRaw != null) {
                                    concernsMap = visionRaw;
                                }
                            }
                            recommendationsList = parseRecommendations(dto.recommendations());
                        } catch (Exception e) {
                            log.warn("[SkinProfile] 분석 히스토리 파싱 실패. analysisId={}", analysis.getId(), e);
                        }
                    }
                    
                    return ProfileResponse.AnalysisHistory.builder()
                            .analysisId(analysis.getId())
                            .imageUrl(imageUrl)
                            .createdAt(analysis.getCreatedAt())
                            .concerns(concernsMap)
                            .recommendations(recommendationsList)
                            .build();
                })
                .collect(Collectors.toList());

        // 추천 화장품 조회 (가장 최신 분석 기록의 analysisId 사용)
        List<ProfileResponse.RecommendationItem> recommendationItems = Collections.emptyList();
        if (latestAnalysisId != null) {
            try {
                List<Recommendation> recommendationsList = recommendations.findByAnalysisId(latestAnalysisId);
                recommendationItems = recommendationsList.stream()
                        .map(rec -> ProfileResponse.RecommendationItem.builder()
                                .id(rec.getId())
                                .analysisId(rec.getAnalysisId())
                                .items(rec.getItems())
                                .createdAt(rec.getCreatedAt())
                                .build())
                        .collect(Collectors.toList());
            } catch (Exception e) {
                log.warn("[SkinProfile] 추천 화장품 조회 실패. analysisId={}", latestAnalysisId, e);
            }
        }

        return ProfileResponse.builder()
                .profileImageUrl(profile != null ? profile.getProfileImageUrl() : null)
                .skinType(finalSkinType)
                .concerns(concernsList)
                .lastAnalysis(lastAnalysis)
                .history(history)
                .recommendations(recommendationItems)
                .build();
    }

    /**
     * 프로필 정보 upsert
     * - 설문/피부타입/관심고민 등 텍스트/선택값 업데이트
     * - 프로필 이미지 URL은 여기서 건드리지 않고 그대로 유지
     */
    @Transactional
    public void upsert(Long userId, ProfileUpdateRequest req) {
        // concerns 리스트를 JSON 문자열로 저장
        String concernsJson = "[]";
        if (req.concerns() != null && !req.concerns().isEmpty()) {
            try {
                concernsJson = objectMapper.writeValueAsString(req.concerns());
            } catch (Exception e) {
                log.error("[SkinProfile] concerns JSON 직렬화 실패", e);
            }
        }

        SkinProfile existing = profiles.findByUserId(userId).orElse(null);

        SkinProfile updated = SkinProfile.builder()
                .id(existing != null ? existing.getId() : null)
                .userId(userId)
                .skinType(req.skinType())
                .mbti(req.mbti())
                .tone(req.tone())
                .concerns(concernsJson)
                .profileImageUrl(existing != null ? existing.getProfileImageUrl() : null)
                .build();

        profiles.save(updated);
    }

    /**
     * 프로필 이미지 URL만 따로 갱신
     * - S3 업로드 이후, 최종 URL을 저장하는 용도
     */
    @Transactional
    public String updateProfileImage(Long userId, String imageUrl) {
        SkinProfile existing = profiles.findByUserId(userId).orElse(null);

        SkinProfile updated = SkinProfile.builder()
                .id(existing != null ? existing.getId() : null)
                .userId(userId)
                .skinType(existing != null ? existing.getSkinType() : null)
                .concerns(existing != null ? existing.getConcerns() : null)
                .mbti(existing != null ? existing.getMbti() : null)
                .tone(existing != null ? existing.getTone() : null)
                .profileImageUrl(imageUrl)
                .build();

        profiles.save(updated);
        return imageUrl;
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
                        log.warn("[SkinProfile] 추천 제품 파싱 실패. product={}", product, e);
                        return null;
                    }
                })
                .filter(item -> item != null)
                .collect(Collectors.toList());
    }
}
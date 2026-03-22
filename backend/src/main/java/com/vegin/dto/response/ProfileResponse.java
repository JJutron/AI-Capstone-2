package com.vegin.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

@Getter
@Builder
public class ProfileResponse {
    private String profileImageUrl;
    private String skinType;
    private List<String> concerns;
    private LastAnalysis lastAnalysis;
    private List<AnalysisHistory> history;
    private List<RecommendationItem> recommendations;

    @Getter
    @Builder
    public static class LastAnalysis {
        private Long analysisId;
        private String mbti;
        private String skinType;
        private OffsetDateTime date;
        private Map<String, Object> concerns; // acne, redness, melasma_darkspots 객체 포함
        private List<CategoryRecommendationResponse> recommendations; // 카테고리별 추천 화장품
    }
    
    @Getter
    @Builder
    public static class RecommendationItem {
        private Long id;
        private Long analysisId;
        private String items; // JSON string
        private OffsetDateTime createdAt;
    }

    @Getter
    @Builder
    public static class AnalysisHistory {
        private Long analysisId;
        private String imageUrl;
        private OffsetDateTime createdAt;
        private Map<String, Object> concerns; // acne, redness, melasma_darkspots 객체 포함
        private List<CategoryRecommendationResponse> recommendations; // 카테고리별 추천 화장품
    }
}


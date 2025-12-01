package com.vegin.module.analysis;

import com.vegin.dto.response.CategoryRecommendationResponse;
import lombok.Builder;
import lombok.Getter;

import java.util.Map;
import java.util.List;

@Getter
@Builder
public class AnalysisResultResponse {

    private String userName;

    private String skinMbtiType;           // DSRW
    private String skinDisplayName;        // 건성
    private String headline;               // 당신이 느끼는 건성 피부
    private String skinDescription;

    private List<String> whiteListIngredients;
    private String whiteListRecommendation;
    private List<String> blackListIngredients;

    private Map<String, Object> axis;      // 축별 결과
    private Map<String, Object> concerns;  // 고민 결과

    private Map<String, Boolean> actions;  // 버튼 활성화
    
    private List<CategoryRecommendationResponse> recommendations; // 카테고리별 추천 화장품
}
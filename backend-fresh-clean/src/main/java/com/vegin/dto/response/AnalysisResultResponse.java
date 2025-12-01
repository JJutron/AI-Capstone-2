package com.vegin.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.util.List;
import java.util.Map;

@Getter
@Builder
public class AnalysisResultResponse {

    private String userName;
    private String skinMbtiType;

    private String skinDescription;
    private List<String> whiteListIngredients;
    private String whiteListRecommendation;
    private List<String> blackListIngredients;

    private Map<String, String> axis;     // oiliness, sensitivity, pigmentation, elasticity
    private Map<String, String> concerns; // acne, blemishes

    private Map<String, Boolean> actions; // recommendProducts, retryAnalysis
}
package com.vegin.external.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public record FastApiResponseDto(
        String status,
        Map<String, Object> fusion,
        List<Map<String, Object>> recommendations,
        String error
) {


    // ====== 편의를 위한 서브 DTO들 (Map -> 자바 객체 변환용) ======

    public record FastApiIndicesDto(
            double oil,
            double dry,
            double sensitivity,
            double wrinkle,
            double pigment
    ) { }

    public record FastApiVisionRawDto(
            FastApiScoreReasonDto acne,
            FastApiScoreReasonDto redness,
            FastApiScoreReasonDto melasmaDarkspots
    ) { }

    public record FastApiScoreReasonDto(
            int score,
            String reason
    ) { }

    public record FastApiProductDto(
            String productId,
            String productName,
            String brand,
            List<String> ingredients,
            int salePrice,
            double averageReviewScore,
            int totalReviewCount,
            String category,
            double scoreEs,
            double scoreLtr,
            String imageUrl
    ) { }
}
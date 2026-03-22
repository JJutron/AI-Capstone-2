package com.vegin.external.dto;

import com.vegin.domain.BSTInfo;
import lombok.Builder;
import lombok.Getter;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Getter
@Builder
public class SkinAnalysisResultDto {

    private final String imageUrl;

    private final String skinMbtiType;
    private final String skinType;
    private final String skinDescription;
    private final String headline;

    private final List<String> whiteListIngredients;
    private final String whiteListRecommendation;
    private final List<String> blackListIngredients;

    private final FastApiResponseDto.FastApiIndicesDto indices;
    private final FastApiResponseDto.FastApiVisionRawDto visionRaw;
    private final List<FastApiResponseDto.FastApiProductDto> recommendations;



    public static SkinAnalysisResultDto from(String imageUrl, FastApiResponseDto dto) {

        if (dto == null) {
            return empty(imageUrl)
                    .recommendations(mapProducts(dto.recommendations()))
                    .build();
        }

        Map<String, Object> fusion = dto.fusion();

// fusion 이 null 이면, MBTI 정보 없이 추천만 채워서 리턴
        if (fusion == null) {
            return empty(imageUrl)
                    .recommendations(mapProducts(dto.recommendations()))
                    .build();
        }

        String skinMbti = (String) fusion.get("skin_mbti");
        String skinType = (String) fusion.get("skin_type");

        BSTInfo bst = BSTInfo.fromCode(skinMbti);

        FastApiResponseDto.FastApiIndicesDto indices = extractIndices(fusion);
        FastApiResponseDto.FastApiVisionRawDto visionRaw = extractVisionRaw(fusion);
        List<FastApiResponseDto.FastApiProductDto> recos = mapProducts(dto.recommendations());

        return SkinAnalysisResultDto.builder()
                .imageUrl(imageUrl)

                .skinMbtiType(skinMbti)
                .skinType(skinType)
                .skinDescription(bst != null ? bst.getDescription() : null)
                .headline(bst != null ? bst.getHeadline() : null)

                .whiteListIngredients(
                        bst != null ? bst.getWhiteListIngredients() : Collections.emptyList()
                )
                .whiteListRecommendation(
                        bst != null ? bst.getWhiteListRecommendation() : null
                )
                .blackListIngredients(
                        bst != null ? bst.getBlackListIngredients() : Collections.emptyList()
                )

                .indices(indices)
                .visionRaw(visionRaw)
                .recommendations(recos)
                .build();
    }

    // ====== 헬퍼 메서드들 ======

    public static SkinAnalysisResultDtoBuilder empty(String imageUrl) {
        return SkinAnalysisResultDto.builder()
                .imageUrl(imageUrl)
                .skinMbtiType(null)
                .skinType(null)
                .skinDescription(null)
                .headline(null)
                .whiteListIngredients(Collections.emptyList())
                .whiteListRecommendation(null)
                .blackListIngredients(Collections.emptyList())
                .indices(null)
                .visionRaw(null)
                .recommendations(Collections.emptyList());
    }

    @SuppressWarnings("unchecked")
    private static FastApiResponseDto.FastApiIndicesDto extractIndices(Map<String, Object> fusion) {
        Object obj = fusion.get("indices");
        if (!(obj instanceof Map<?, ?> m)) return null;

        double oil = toDouble(m.get("oil"));
        double dry = toDouble(m.get("dry"));
        double sensitivity = toDouble(m.get("sensitivity"));
        double wrinkle = toDouble(m.get("wrinkle"));
        double pigment = toDouble(m.get("pigment"));

        return new FastApiResponseDto.FastApiIndicesDto(
                oil, dry, sensitivity, wrinkle, pigment
        );
    }

    @SuppressWarnings("unchecked")
    private static FastApiResponseDto.FastApiVisionRawDto extractVisionRaw(Map<String, Object> fusion) {
        Object obj = fusion.get("vision_raw");
        if (!(obj instanceof Map<?, ?> m)) return null;

        FastApiResponseDto.FastApiScoreReasonDto acne = toScoreReason(m.get("acne"));
        FastApiResponseDto.FastApiScoreReasonDto redness = toScoreReason(m.get("redness"));
        FastApiResponseDto.FastApiScoreReasonDto melasma = toScoreReason(m.get("melasma_darkspots"));

        return new FastApiResponseDto.FastApiVisionRawDto(acne, redness, melasma);
    }

    @SuppressWarnings("unchecked")
    private static FastApiResponseDto.FastApiScoreReasonDto toScoreReason(Object o) {

        if (o == null) return null;
        if (!(o instanceof Map)) return null;

        Map<String, Object> m;
        try {
            m = (Map<String, Object>) o;
        } catch (ClassCastException e) {
            return null;
        }

        Object scoreObj = m.get("score");
        int score = 0;
        if (scoreObj instanceof Number num) {
            score = num.intValue();
        }

        Object reasonObj = m.get("reason");
        String reason = "";
        if (reasonObj instanceof String s) {
            reason = s;
        }

        return new FastApiResponseDto.FastApiScoreReasonDto(score, reason);
    }

    private static List<FastApiResponseDto.FastApiProductDto> mapProducts(List<Map<String, Object>> rawList) {
        if (rawList == null) return Collections.emptyList();

        return rawList.stream()
                .map(SkinAnalysisResultDto::toProduct)
                .collect(Collectors.toList());
    }

    @SuppressWarnings("unchecked")
    private static FastApiResponseDto.FastApiProductDto toProduct(Map<String, Object> m) {
        if (m == null) return null;

        String productId = (String) m.get("product_id");
        String productName = (String) m.get("productName");
        String brand = (String) m.get("brand");
        List<String> ingredients = (List<String>) m.getOrDefault("ingredients", Collections.emptyList());
        int salePrice = ((Number) m.getOrDefault("salePrice", 0)).intValue();
        double avgScore = toDouble(m.get("averageReviewScore"));
        int reviewCount = ((Number) m.getOrDefault("totalReviewCount", 0)).intValue();
        String category = (String) m.get("category");
        double scoreEs = toDouble(m.get("score_es"));
        double scoreLtr = toDouble(m.get("score_ltr"));
        String imageUrl = (String) m.get("image_url");

        return new FastApiResponseDto.FastApiProductDto(
                productId,
                productName,
                brand,
                ingredients,
                salePrice,
                avgScore,
                reviewCount,
                category,
                scoreEs,
                scoreLtr,
                imageUrl
        );
    }

    private static double toDouble(Object v) {
        if (v == null) return 0.0;
        if (v instanceof Number n) return n.doubleValue();
        try {
            return Double.parseDouble(v.toString());
        } catch (Exception e) {
            return 0.0;
        }
    }
}
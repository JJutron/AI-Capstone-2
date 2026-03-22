package com.vegin.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class CategoryRecommendationResponse {
    private String productId;          // 제품 ID
    private String productName;        // 제품 이름
    private String brand;              // 브랜드명
    private Integer salePrice;         // 판매 가격
    private Double averageReviewScore; // 평균 리뷰 점수
    private Integer totalReviewCount;  // 총 리뷰 개수
    private String category;           // 화장품 카테고리 (e.g., "cream", "essence", "skintoner")
    private String imageUrl;           // 제품 이미지 URL
    private List<String> xaiKeywords; // 제품의 핵심 키워드 (e.g., "수부지 개선", "촉촉")
    private List<String> tags;        // xaiKeywords의 별칭 (프론트엔드 호환성)
}


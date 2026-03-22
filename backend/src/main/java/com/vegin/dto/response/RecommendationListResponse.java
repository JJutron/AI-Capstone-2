package com.vegin.dto.response;

import com.vegin.dto.request.RecommendationCard;

import java.util.List;

public record RecommendationListResponse(List<RecommendationCard> items) {}

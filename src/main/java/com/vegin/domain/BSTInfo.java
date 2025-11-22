package com.vegin.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.List;
@Getter
@RequiredArgsConstructor
public enum BSTInfo {

    DSPT(
            "DSPT",
            "당신이 느끼는 건조하고 민감한 피부",
            "수분 유지력이 낮고 장벽이 약해 염증과 색소침착이 반복될 수 있어요.",
            List.of(
                    "세라마이드",
                    "히알루론산",
                    "병풀 추출물",
                    "나이아신아마이드(저농도)",
                    "자외선 차단제"
            ),
            "장벽 강화, 진정, 보습, 그리고 미백을 위한 저자극 복합 케어를 추천드립니다.",
            List.of(
                    "알코올",
                    "고농도 AHA",
                    "레티놀 고농도"
            )
    );

    private final String displayName;               // "DSPT"
    private final String headline;                  // 상단 문구
    private final String description;               // 상세 설명
    private final List<String> whiteListIngredients;
    private final String whiteListRecommendation;
    private final List<String> blackListIngredients;

    public static BSTInfo fromCode(String code) {
        try {
            return BSTInfo.valueOf(code);
        } catch (Exception e) {
            return null;
        }
    }
}
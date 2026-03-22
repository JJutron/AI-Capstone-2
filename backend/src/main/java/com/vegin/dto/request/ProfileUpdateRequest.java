package com.vegin.dto.request;

import java.util.List;

public record ProfileUpdateRequest(
        String skinType,
        List<String> concerns,
        String mbti,
        String tone
) {}

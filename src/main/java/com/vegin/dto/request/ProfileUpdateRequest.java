package com.vegin.dto.request;

import java.util.Map;

public record ProfileUpdateRequest(String skinType, Map<String,Object> concerns) {}

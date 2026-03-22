package com.vegin.module.users.controller;

import com.vegin.auth.UserPrincipal;
import com.vegin.common.ApiResponse;
import com.vegin.dto.request.ProfileUpdateRequest;
import com.vegin.dto.response.ProfileResponse;
import com.vegin.module.users.service.SkinProfileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Profile", description = "프로필 관리 API")
@RestController
@RequestMapping({"/api/profile"})
@RequiredArgsConstructor
public class ProfileController {

    private final SkinProfileService profileService;

    @Operation(
            summary = "프로필 조회",
            description = "사용자 프로필 정보와 최근 분석 기록을 조회합니다.",
            security = { @SecurityRequirement(name = "bearerAuth") }
    )
    @GetMapping
    public ApiResponse<ProfileResponse> getProfile(Authentication auth) {
        Long userId = resolveUserId(auth);
        ProfileResponse response = profileService.getProfile(userId);
        return ApiResponse.ok(response);
    }

    @Operation(
            summary = "프로필 업데이트",
            description = "프로필 정보를 업데이트합니다. 기존 데이터가 있으면 업데이트, 없으면 생성합니다.",
            security = { @SecurityRequirement(name = "bearerAuth") }
    )
    @PutMapping
    public ApiResponse<Void> updateProfile(
            @Valid @RequestBody ProfileUpdateRequest req,
            Authentication auth
    ) {
        Long userId = resolveUserId(auth);
        profileService.upsert(userId, req);
        return ApiResponse.ok(null);
    }

    private Long resolveUserId(Authentication auth) {
        Object principal = auth.getPrincipal();
        if (principal instanceof UserPrincipal up) {
            return up.getId();
        }
        throw new IllegalStateException("지원하지 않는 principal 타입: " + principal);
    }
}


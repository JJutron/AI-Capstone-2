package com.vegin.module.users.service;

import com.google.gson.Gson;

import com.vegin.module.users.Entity.SkinProfile;
import com.vegin.dto.request.ProfileUpdateRequest;
import com.vegin.module.users.repository.SkinProfileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class SkinProfileService {

    private final SkinProfileRepository profiles;
    private final Gson gson = new Gson();

    /**
     * 마이페이지 조회용
     */
    @Transactional(readOnly = true)
    public Optional<SkinProfile> get(Long userId) {
        return profiles.findByUserId(userId);
    }

    /**
     * 프로필 정보 upsert
     * - 설문/피부타입/관심고민 등 텍스트/선택값 업데이트
     * - 프로필 이미지 URL은 여기서 건드리지 않고 그대로 유지
     */
    @Transactional
    public void upsert(Long userId, ProfileUpdateRequest req) {
        // concerns 리스트를 JSON 문자열로 저장
        String concernsJson = (req.concerns() == null)
                ? "{}"
                : gson.toJson(req.concerns());

        SkinProfile existing = profiles.findByUserId(userId).orElse(null);

        SkinProfile updated = SkinProfile.builder()
                .id(existing != null ? existing.getId() : null)
                .userId(userId)

                // 프로필 기본 정보 업데이트
                .skinType(req.skinType())
                // .mbti(req.mbti())
                // .tone(req.tone())
                .concerns(concernsJson)

                //기존에 저장된 프로필 이미지 URL은 유지
                .profileImageUrl(existing != null ? existing.getProfileImageUrl() : null)

                .build();

        profiles.save(updated);
    }

    /**
     * 프로필 이미지 URL만 따로 갱신
     * - S3 업로드 이후, 최종 URL을 저장하는 용도
     */
    @Transactional
    public String updateProfileImage(Long userId, String imageUrl) {
        SkinProfile existing = profiles.findByUserId(userId).orElse(null);

        SkinProfile updated = SkinProfile.builder()
                .id(existing != null ? existing.getId() : null)
                .userId(userId)
                .skinType(existing != null ? existing.getSkinType() : null)
                .concerns(existing != null ? existing.getConcerns() : null)
                // .mbti(existing != null ? existing.getMbti() : null)
                // .tone(existing != null ? existing.getTone() : null)
                .profileImageUrl(imageUrl)
                .build();

        profiles.save(updated);
        return imageUrl;
    }
}
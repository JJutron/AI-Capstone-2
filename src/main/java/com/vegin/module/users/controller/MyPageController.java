package com.vegin.module.users.controller;

import com.vegin.auth.UserPrincipal;
import com.vegin.common.ApiResponse;
import com.vegin.dto.request.ProfileUpdateRequest;
import com.vegin.module.analysis.repository.RecommendationRepository;
import com.vegin.module.analysis.repository.SkinAnalysisRepository;
import com.vegin.common.S3Service;
import com.vegin.module.users.service.SkinProfileService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequestMapping("/api/mypage")
@RequiredArgsConstructor
public class MyPageController {

    private final SkinProfileService profiles;
    private final SkinAnalysisRepository analyses;
    private final RecommendationRepository recos;
    private final S3Service s3Service;
    @GetMapping
    public ApiResponse<Map<String,Object>> get(Authentication auth){
        Long userId = ((UserPrincipal) auth.getPrincipal()).getId();
        var profile = profiles.get(userId).orElse(null);
        var history = analyses.findByUserIdOrderByCreatedAtDesc(userId);
        var reco = recos.findFirstByUserIdOrderByCreatedAtDesc(userId).orElse(null);
        return ApiResponse.ok(
                Map.of(
                        "profile", profile,
                        "analysis", history,
                        "recommendation", reco
                )
        );
    }

    @PutMapping("/profile")
    public ApiResponse<Void> update(
            @RequestBody ProfileUpdateRequest req,
            Authentication auth
    ){
        Long userId = ((UserPrincipal) auth.getPrincipal()).getId();
        profiles.upsert(userId, req);
        return ApiResponse.ok(null);
    }

    /**
     * 마이페이지 프로필 이미지 업로드
     */
    @PostMapping(
            value = "/profile/image",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<Map<String, String>> uploadProfileImage(
            @RequestPart("file") MultipartFile file,
            Authentication auth
    ) {
        Long userId = ((UserPrincipal) auth.getPrincipal()).getId();

        // 1) S3 키 생성
        String ext = getExt(file.getOriginalFilename());
        String key = "profile/%d/%d%s".formatted(
                userId,
                System.currentTimeMillis(),
                ext
        );

        // 2) S3 업로드
        s3Service.upload(file, key);

        // 3) URL 생성 (이미 구현해둔 메서드 있다고 가정)
        String url = s3Service.getUrl(key);

        // 4) 프로필에 URL 저장
        profiles.updateProfileImage(userId, url);

        // 5) FE에 돌려주기
        return ApiResponse.ok(Map.of("profileImageUrl", url));
    }

    private String getExt(String name) {
        if (name == null) return "";
        int i = name.lastIndexOf('.');
        return (i >= 0) ? name.substring(i) : "";
    }
}
//package com.vegin.users.controller;
//
//import com.vegin.auth.UserPrincipal;
//import com.vegin.common.ApiResponse;
//import com.vegin.users.dto.request.ProfileUpdateRequest;
//import com.vegin.users.repository.RecommendationRepository;
//import com.vegin.users.repository.SkinAnalysisRepository;
//import com.vegin.users.service.SkinProfileService;
//import lombok.RequiredArgsConstructor;
//import org.springframework.security.core.Authentication;
//import org.springframework.web.bind.annotation.*;
//
//import java.util.Map;
//
//@RestController
//@RequestMapping("/api/mypage")
//@RequiredArgsConstructor
//public class MyPageController {
//    private final SkinProfileService profiles;
//    private final SkinAnalysisRepository analyses;
//    private final RecommendationRepository recos;
//    @GetMapping
//    public ApiResponse<Map<String,Object>> get(Authentication auth){
//        Long userId = ((UserPrincipal) auth.getPrincipal()).getId();
//        var profile = profiles.get(userId).orElse(null);
//        var history = analyses.findByUserIdOrderByCreatedAtDesc(userId);
//        var reco = recos.findFirstByUserIdOrderByCreatedAtDesc(userId).orElse(null);
//        return ApiResponse.ok(Map.of("profile", profile, "analysis", history, "recommendation", reco));
//    }
//
//    @PutMapping("/profile")
//    public ApiResponse<Void> update(@RequestBody ProfileUpdateRequest req, Authentication auth){
//        Long userId = ((UserPrincipal) auth.getPrincipal()).getId();
//        profiles.upsert(userId, req);
//        return ApiResponse.ok(null);
//    }
//}

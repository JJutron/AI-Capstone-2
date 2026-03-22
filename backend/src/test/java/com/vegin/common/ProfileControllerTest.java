package com.vegin.common;

import com.vegin.auth.UserPrincipal;
import com.vegin.common.ApiResponse;
import com.vegin.dto.response.ProfileResponse;
import com.vegin.module.users.controller.ProfileController;
import com.vegin.module.users.service.SkinProfileService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;

import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ProfileController 단위 테스트")
class ProfileControllerTest {

    @Mock
    private SkinProfileService profileService;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private ProfileController profileController;

    @Test
    @DisplayName("TC-D01-N01: (정상) 본인 프로필 정보 조회 성공")
    void getProfile_ValidUser_Success() {
        // Given
        Long userId = 1L;
        UserPrincipal userPrincipal = new UserPrincipal(
                userId,
                "test@vegin.com",
                "테스트유저",
                Collections.emptyList()
        );

        ProfileResponse mockResponse = ProfileResponse.builder()
                .profileImageUrl("https://example.com/profile.jpg")
                .skinType("건성")
                .concerns(List.of("여드름", "홍조"))
                .lastAnalysis(null)
                .history(Collections.emptyList())
                .recommendations(Collections.emptyList())
                .build();

        when(authentication.getPrincipal()).thenReturn(userPrincipal);
        when(profileService.getProfile(userId)).thenReturn(mockResponse);

        // When
        ApiResponse<ProfileResponse> response = profileController.getProfile(authentication);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getCode()).isEqualTo(200);
        assertThat(response.getData()).isNotNull();
        assertThat(response.getData().getSkinType()).isEqualTo("건성");
        assertThat(response.getData().getConcerns()).hasSize(2);
        assertThat(response.getData().getProfileImageUrl()).isEqualTo("https://example.com/profile.jpg");

        // 검증: 프로필 서비스 호출 확인
        verify(profileService, times(1)).getProfile(userId);
    }
}
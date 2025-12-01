package com.vegin.auth;

import com.vegin.module.users.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;
import org.springframework.beans.factory.annotation.Value;

import java.io.IOException;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class OAuth2AuthenticationSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final JwtTokenProvider jwtTokenProvider;
    private final UserRepository userRepository;

    @Value("${app.frontend-url}")
    private String frontendUrl;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, 
                                        HttpServletResponse response,
                                        Authentication authentication) throws IOException {
        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();
        
        // OAuth2User의 원본 attributes에서 이메일 추출
        // Google OAuth2의 경우 원본 attributes에 email이 포함되어 있음
        Map<String, Object> attributes = oAuth2User.getAttributes();
        String email = (String) attributes.get("email");
        
        if (email == null) {
            log.error("Email not found in OAuth2 attributes");
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Email not found");
            return;
        }
        
        // 사용자 ID 조회
        Long userId = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found: " + email))
                .getId();
        
        // JWT 토큰 생성
        String token = jwtTokenProvider.generateToken(userId, email);
        long expiresInSec = jwtTokenProvider.getExpiration() / 1000L; // ms → s
        
        // 프론트엔드로 리다이렉트 (토큰을 쿼리 파라미터로 전달)
        String redirectUrl = UriComponentsBuilder.fromUriString(frontendUrl + "/index.html")
                .queryParam("token", token)
                .queryParam("expiresIn", expiresInSec)
                .queryParam("userId", userId)
                .queryParam("email", email)
                .build().toUriString();
        
        log.info("OAuth2 login success for user: {}. Redirecting to: {}", email, redirectUrl);
        getRedirectStrategy().sendRedirect(request, response, redirectUrl);
    }
}


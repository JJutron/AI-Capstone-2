package com.vegin.auth;

import com.vegin.module.users.Entity.User;
import com.vegin.module.users.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private final UserRepository userRepository;

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oAuth2User = super.loadUser(userRequest);

        String registrationId = userRequest.getClientRegistration().getRegistrationId();
        String userNameAttributeName = userRequest.getClientRegistration()
                .getProviderDetails()
                .getUserInfoEndpoint()
                .getUserNameAttributeName();

        OAuthAttributes attributes = OAuthAttributes.of(
                registrationId,
                userNameAttributeName,
                oAuth2User.getAttributes()
        );

        User user = saveOrUpdate(attributes);

        // 원본 attributes를 유지하되, email이 포함되도록 보장
        // OAuth2SuccessHandler에서 email을 가져올 수 있도록 함
        Map<String, Object> userAttributes = oAuth2User.getAttributes();
        
        return new DefaultOAuth2User(
                Collections.singleton(new SimpleGrantedAuthority("ROLE_USER")),
                userAttributes,
                userNameAttributeName
        );
    }

    private User saveOrUpdate(OAuthAttributes attributes) {
        return userRepository.findByEmail(attributes.getEmail())
                .map(existingUser -> {
                    // 기존 사용자 정보 업데이트
                    String newNickname = attributes.getName() != null 
                            ? attributes.getName() 
                            : existingUser.getNickname();
                    existingUser.changeNickname(newNickname);
                    
                    User updatedUser = User.builder()
                            .id(existingUser.getId())
                            .email(existingUser.getEmail())
                            .password(existingUser.getPassword())
                            .nickname(newNickname)
                            .gender(existingUser.getGender())
                            .birthDate(existingUser.getBirthDate())
                            .createdAt(existingUser.getCreatedAt())
                            .updatedAt(OffsetDateTime.now())
                            .build();
                    return userRepository.save(updatedUser);
                })
                .orElseGet(() -> {
                    // 새 사용자 생성
                    User newUser = attributes.toEntity();
                    return userRepository.save(newUser);
                });
    }
}


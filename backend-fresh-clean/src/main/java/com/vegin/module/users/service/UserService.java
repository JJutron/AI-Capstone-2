package com.vegin.module.users.service;








import com.vegin.auth.JwtTokenProvider;
import com.vegin.module.users.Entity.User;
import com.vegin.dto.request.LoginRequest;
import com.vegin.dto.request.SignupRequest;
import com.vegin.dto.response.LoginResponse;
import com.vegin.module.users.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;


@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final PasswordEncoder passwordEncoder;

    /**
     * 회원가입
     * - 이메일 중복 검사
     * - 비밀번호 암호화
     * - 성별 및 생년월일 저장
     */
    public Long signup(SignupRequest req) {

        // 1) 이메일 중복 체크
        if (userRepository.existsByEmail(req.getEmail())) {
            // GlobalExceptionHandler에서 400 처리하게
            throw new IllegalArgumentException("이미 사용 중인 이메일입니다.");
        }

        // 2) Gender 변환
        User.Gender genderEnum;
        try {
            genderEnum = User.Gender.valueOf(req.getGender().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("유효하지 않은 성별입니다. M 또는 F를 입력해주세요.");
        }

        // 3) 유저 생성
        User user = User.builder()
                .email(req.getEmail())
                .nickname(req.getNickname())
                .password(passwordEncoder.encode(req.getPassword()))
                .gender(genderEnum)
                .birthDate(req.getBirthDate())
                .createdAt(OffsetDateTime.now())
                .updatedAt(OffsetDateTime.now())
                .build();

        // 4) 저장
        userRepository.save(user);
        return user.getId();
    }

    /**
     * 로그인
     * - 이메일로 사용자 조회
     * - 비밀번호 검증
     * - JWT 발급
     */
    public LoginResponse login(LoginRequest req) {
        // 1) 이메일로 사용자 조회
        User user = userRepository.findByEmail(req.getEmail())
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 이메일입니다."));

        // 2) 비밀번호 검증
        if (!passwordEncoder.matches(req.getPassword(), user.getPassword())) {
            // Spring Security에서 자주 쓰는 예외 (401로 매핑하면 좋음)
            throw new BadCredentialsException("비밀번호가 일치하지 않습니다.");
        }

        // 3) JWT 토큰 생성
        String token = jwtTokenProvider.generateToken(user.getId(), user.getEmail());
        long expiresInSec = jwtTokenProvider.getExpiration() / 1000L; // ms → s

        // 4) 응답 DTO
        return LoginResponse.of(
                token,
                "Bearer",
                expiresInSec,
                user.getId(),
                user.getEmail()
        );
    }

    /**
     * 이메일로 userId 조회 (마이페이지 등에서 사용)
     */
    public Long getUserIdByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + email))
                .getId();
    }
}
//
//@Service
//@RequiredArgsConstructor
//public class UserService {
//
//    private final UserRepository userRepository;
//    private final JwtTokenProvider jwtTokenProvider;
//    private final PasswordEncoder passwordEncoder;
//
//    public Long signup(SignupRequest req) {
//        User user = User.builder()
//                .email(req.getEmail())
//                .nickname(req.getNickname())
//                .password(passwordEncoder.encode(req.getPassword()))
//                .build();
//        userRepository.save(user);
//        return user.getId();
//    }
//    public LoginResponse login(LoginRequest req) {
//        // 자격 증명 검증
//
//        User user = userRepository.findByEmail(req.getEmail())
//                .orElseThrow(() -> new UsernameNotFoundException("No user"));
//
//        String token = jwtTokenProvider.generateToken(user.getId(), user.getEmail());
//
//        return LoginResponse.of(
//                token,
//                "Bearer",
//                jwtTokenProvider.getExpiration() / 1000, // 초 단위
//                user.getId(),
//                user.getEmail()
//        );
//    }
//    public Long getUserIdByEmail(String email) {
//        return userRepository.findByEmail(email)
//                .orElseThrow(() -> new IllegalArgumentException("User not found: " + email))
//                .getId();
//    }
//}
//
//
//

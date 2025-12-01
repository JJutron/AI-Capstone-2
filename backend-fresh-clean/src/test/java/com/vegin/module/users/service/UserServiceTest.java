package com.vegin.module.users.service;

import com.vegin.auth.JwtTokenProvider;
import com.vegin.dto.request.LoginRequest;
import com.vegin.dto.request.SignupRequest;
import com.vegin.dto.response.LoginResponse;
import com.vegin.module.users.Entity.User;
import com.vegin.module.users.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.lang.reflect.Field;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserService 단위 테스트")
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserService userService;

    // Reflection을 사용하여 User의 id 필드 설정 헬퍼 메서드
    private void setId(User user, Long id) {
        try {
            Field idField = User.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(user, id);
        } catch (Exception e) {
            throw new RuntimeException("Failed to set ID", e);
        }
    }

    @Test
    @DisplayName("TC-A01-N01: (정상) 유효한 정보로 회원가입 성공")
    void signup_ValidRequest_Success() {
        // Given
        SignupRequest request = new SignupRequest(
                "test@vegin.com",
                "password123",
                "테스트유저",
                "M",
                LocalDate.of(1990, 1, 1)
        );

        when(userRepository.existsByEmail("test@vegin.com")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("$2a$10$encodedPasswordHash");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            setId(user, 1L); // Reflection으로 ID 설정
            return user;
        });

        // When
        Long userId = userService.signup(request);

        // Then
        assertThat(userId).isNotNull();
        assertThat(userId).isEqualTo(1L);

        // 검증: 이메일 중복 체크 호출 확인
        verify(userRepository, times(1)).existsByEmail("test@vegin.com");

        // 검증: 비밀번호 암호화 호출 확인
        verify(passwordEncoder, times(1)).encode("password123");

        // 검증: 사용자 저장 호출 확인
        verify(userRepository, times(1)).save(any(User.class));
    }

    @Test
    @DisplayName("TC-A02-N01: (정상) 유효한 자격증명으로 로그인 성공")
    void login_ValidCredentials_Success() {
        // Given
        LoginRequest request = new LoginRequest("test@vegin.com", "password123");

        User savedUser = User.builder()
                .id(1L)
                .email("test@vegin.com")
                .password("$2a$10$encodedPasswordHash")
                .nickname("테스트유저")
                .gender(User.Gender.M)
                .birthDate(LocalDate.of(1990, 1, 1))
                .createdAt(OffsetDateTime.now())
                .updatedAt(OffsetDateTime.now())
                .build();

        when(userRepository.findByEmail("test@vegin.com"))
                .thenReturn(Optional.of(savedUser));
        when(passwordEncoder.matches("password123", "$2a$10$encodedPasswordHash"))
                .thenReturn(true);
        when(jwtTokenProvider.generateToken(1L, "test@vegin.com"))
                .thenReturn("mock.jwt.token");
        when(jwtTokenProvider.getExpiration())
                .thenReturn(3600000L); // 1시간 (밀리초)

        // When
        LoginResponse response = userService.login(request);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getAccessToken()).isEqualTo("mock.jwt.token");
        assertThat(response.getTokenType()).isEqualTo("Bearer");
        assertThat(response.getExpiresIn()).isEqualTo(3600L); // 초 단위
        assertThat(response.getUserId()).isEqualTo(1L);
        assertThat(response.getEmail()).isEqualTo("test@vegin.com");

        // 검증: 이메일로 사용자 조회 호출 확인
        verify(userRepository, times(1)).findByEmail("test@vegin.com");

        // 검증: 비밀번호 검증 호출 확인
        verify(passwordEncoder, times(1))
                .matches("password123", "$2a$10$encodedPasswordHash");

        // 검증: JWT 토큰 생성 호출 확인
        verify(jwtTokenProvider, times(1)).generateToken(1L, "test@vegin.com");

        // 검증: 만료 시간 조회 호출 확인
        verify(jwtTokenProvider, times(1)).getExpiration();
    }

    @Test
    @DisplayName("TC-A02-E01: (예외) 잘못된 비밀번호로 로그인 실패")
    void login_InvalidPassword_ThrowsException() {
        // Given
        LoginRequest request = new LoginRequest("test@vegin.com", "wrong_password");

        User savedUser = User.builder()
                .id(1L)
                .email("test@vegin.com")
                .password("$2a$10$encodedPasswordHash")
                .nickname("테스트유저")
                .gender(User.Gender.M)
                .createdAt(OffsetDateTime.now())
                .updatedAt(OffsetDateTime.now())
                .build();

        when(userRepository.findByEmail("test@vegin.com"))
                .thenReturn(Optional.of(savedUser));
        when(passwordEncoder.matches("wrong_password", "$2a$10$encodedPasswordHash"))
                .thenReturn(false);

        // When & Then
        assertThatThrownBy(() -> userService.login(request))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessage("비밀번호가 일치하지 않습니다.");

        // 검증: 이메일로 사용자 조회 호출 확인
        verify(userRepository, times(1)).findByEmail("test@vegin.com");

        // 검증: 비밀번호 검증 호출 확인
        verify(passwordEncoder, times(1))
                .matches("wrong_password", "$2a$10$encodedPasswordHash");

        // 검증: JWT 토큰 생성은 호출되지 않음
        verify(jwtTokenProvider, never()).generateToken(anyLong(), anyString());
    }

    @Test
    @DisplayName("TC-A04-E01: (예외) 기가입된 이메일로 회원가입 시도")
    void signup_DuplicateEmail_ThrowsException() {
        // Given
        SignupRequest request = new SignupRequest(
                "test@vegin.com",
                "password123",
                "테스트유저",
                "M",
                LocalDate.of(1990, 1, 1)
        );

        when(userRepository.existsByEmail("test@vegin.com")).thenReturn(true);

        // When & Then
        assertThatThrownBy(() -> userService.signup(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("이미 사용 중인 이메일입니다.");

        // 검증: 이메일 중복 체크 호출 확인
        verify(userRepository, times(1)).existsByEmail("test@vegin.com");

        // 검증: 사용자 저장은 호출되지 않음
        verify(userRepository, never()).save(any(User.class));

        // 검증: 비밀번호 암호화도 호출되지 않음
        verify(passwordEncoder, never()).encode(anyString());
    }
}
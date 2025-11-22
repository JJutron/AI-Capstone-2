package com.vegin.module.users.service;


import com.vegin.auth.JwtTokenProvider;
import com.vegin.module.users.Entity.User;
import com.vegin.dto.request.LoginRequest;
import com.vegin.dto.request.SignupRequest;
import com.vegin.dto.response.LoginResponse;
import com.vegin.module.users.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserService {

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final PasswordEncoder passwordEncoder;

    public Long signup(SignupRequest req) {
        User user = User.builder()
                .email(req.getEmail())
                .nickname(req.getNickname())
                .password(passwordEncoder.encode(req.getPassword()))
                .build();
        userRepository.save(user);
        return user.getId();
    }
    public LoginResponse login(LoginRequest req) {
        // 자격 증명 검증
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(req.getEmail(), req.getPassword())
        );

        User user = userRepository.findByEmail(req.getEmail())
                .orElseThrow(() -> new UsernameNotFoundException("No user"));

        String token = jwtTokenProvider.generateToken(user.getId(), user.getEmail());

        return LoginResponse.of(
                token,
                "Bearer",
                jwtTokenProvider.getExpiration() / 1000, // 초 단위
                user.getId(),
                user.getEmail()
        );
    }
    public Long getUserIdByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + email))
                .getId();
    }
}




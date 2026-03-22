package com.vegin.module.users.controller;

import com.vegin.dto.request.LoginRequest;
import com.vegin.dto.request.SignupRequest;
import com.vegin.common.ApiResponse;
import com.vegin.dto.response.LoginResponse;
import com.vegin.dto.response.SignupResponse;
import com.vegin.module.users.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Auth", description = "회원가입 / 로그인 API")
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @Operation(summary = "회원가입", description = "새로운 사용자를 회원가입 처리합니다.")
    @PostMapping("/signup")
    public ResponseEntity<ApiResponse<SignupResponse>> signup(
            @Valid @RequestBody SignupRequest req
    ) {
        Long id = userService.signup(req);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(SignupResponse.created(id)));
    }

    @Operation(summary = "로그인", description = "이메일 + 비밀번호로 로그인하고 JWT 토큰을 발급받습니다.")
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponse>> login(
            @Valid @RequestBody LoginRequest req
    ) {
        LoginResponse res = userService.login(req);
        return ResponseEntity.ok(ApiResponse.ok(res));
    }
}

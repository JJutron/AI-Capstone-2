package com.vegin.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
@Getter
@AllArgsConstructor(staticName = "of")
public class LoginResponse {
    private String accessToken;
    private String tokenType;
    private long expiresIn;
    private Long userId;
    private String email;
}
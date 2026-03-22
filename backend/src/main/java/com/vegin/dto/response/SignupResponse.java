package com.vegin.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class SignupResponse {
    private Long userId;
    private String message;

    public static SignupResponse created(Long id) {
        return new SignupResponse(id, "register_success");

    }
}

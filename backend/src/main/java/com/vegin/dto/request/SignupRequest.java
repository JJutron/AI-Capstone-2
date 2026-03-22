package com.vegin.dto.request;


import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
public class SignupRequest {

    @NotBlank @Email
    private String email;
    @NotBlank
    private String password;
    @NotBlank @Size(min= 2, max= 30)
    private String nickname;
    
    @NotBlank
    private String gender;  // "M" 또는 "F"
    
    private LocalDate birthDate;  // nullable
}

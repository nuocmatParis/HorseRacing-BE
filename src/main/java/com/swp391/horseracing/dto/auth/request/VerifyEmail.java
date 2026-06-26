package com.swp391.horseracing.dto.auth.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class VerifyEmail {

    @Email(message = "Email is invalid")
    @NotBlank(message = "Email is required")
    String email;

    @NotBlank(message = "OTP code is required")
    String otpCode;
}

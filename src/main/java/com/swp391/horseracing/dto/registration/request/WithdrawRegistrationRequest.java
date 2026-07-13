package com.swp391.horseracing.dto.registration.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class WithdrawRegistrationRequest {
    @NotBlank(message = "Withdraw reason is required")
    @Size(max = 1000, message = "Withdraw reason must not exceed 1000 characters")
    private String reason;
}

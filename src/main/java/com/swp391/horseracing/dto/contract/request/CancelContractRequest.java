package com.swp391.horseracing.dto.contract.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CancelContractRequest {
    @NotBlank(message = "Cancel reason is required")
    @Size(max = 1000, message = "Cancel reason must not exceed 1000 characters")
    private String reason;
}

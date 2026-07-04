package com.swp391.horseracing.dto.registration.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.Data;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;

@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
public class RegisterJockeyRequest {
    
    @NotNull(message = "Hire fee is required")
    @DecimalMin(value = "0.0", message = "Hire fee must be at least 0")
    BigDecimal hireFee;
}

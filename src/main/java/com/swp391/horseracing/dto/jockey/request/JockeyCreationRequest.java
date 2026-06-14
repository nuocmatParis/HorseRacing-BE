package com.swp391.horseracing.dto.jockey.request;



import com.swp391.horseracing.enums.JockeyStatus;
import jakarta.validation.constraints.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class JockeyCreationRequest {

    UUID userId;

    @Positive(message = "Height must be positive")
    BigDecimal height;

    @Positive(message = "Weight must be positive")
    BigDecimal weight;

    @NotNull(message = "Experience year is required")
    @Min(value = 0, message = "Experience year must be at least 0")
    int experienceYears;

    @Size(max = 50,message = "License number must not exceed 50 character")
    String licenseNumber;
    @Size(max = 50,message = "Specialization must not exceed 50 character")
    String specialization;

    @DecimalMin(value = "1", message = "Minimum is 0")
    BigDecimal hireFee;

}

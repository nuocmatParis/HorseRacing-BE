package com.swp391.horseracing.dto.jockey.request;

import jakarta.validation.constraints.*;
import lombok.AccessLevel;
import lombok.Data;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;

@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
public class JockeyUpdateRequest {

    @Positive(message = "Height must be positive")
    float height;

    @Positive(message = "Weight must be positive")
    float weight;

    @NotNull(message = "Experience year is required")
    @Min(value = 0, message = "Experience year must be at least 0")
    int experienceYears;

    @Size(max = 50, message = "License number must not exceed 50 character")
    @Pattern(
            regexp = "^(JOC)-[A-Z0-9]{8}$",
            message = "License number must follow format JOC-XXXXXXXX"
    )
    String licenseNumber;

    @Size(max = 50, message = "Specialization must not exceed 50 character")
    String specialization;

}

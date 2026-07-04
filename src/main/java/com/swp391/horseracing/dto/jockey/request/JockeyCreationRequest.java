package com.swp391.horseracing.dto.jockey.request;



import com.swp391.horseracing.enums.JockeyStatus;
import jakarta.validation.Valid;
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

    @Positive(message = "Height must be positive")
    float height;

    @Positive(message = "Weight must be positive")
    float weight;

    @NotBlank(message = "License number is required")
    @Size(max = 50,message = "License number must not exceed 50 character")
    @Pattern(
            regexp = "^(JOC)-[A-Z0-9]{8}$",
            message = "License number must follow format JOC-XXXXXXXX"
    )
    String licenseNumber;

    @Size(max = 50,message = "Specialization must not exceed 50 character")
    String specialization;

}

package com.swp391.horseracing.dto.referee.request;

import jakarta.validation.constraints.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class RefereeCreationRequest {

    @NotNull(message = "User ID is required")
    UUID userId;

    @Size(max = 50, message = "Certification level must not exceed 50 characters")
    String certificationLevel;

    @Min(value = 0, message = "Years of service must be at least 0")
    int yearsOfService;
}

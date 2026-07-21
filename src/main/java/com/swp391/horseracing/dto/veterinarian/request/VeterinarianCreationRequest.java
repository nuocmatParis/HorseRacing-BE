package com.swp391.horseracing.dto.veterinarian.request;

import jakarta.validation.constraints.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class VeterinarianCreationRequest {

    @NotNull(message = "User ID is required")
    UUID userId;

    @Size(max = 100, message = "Specialization must not exceed 100 characters")
    String specialization;

    @Min(value = 0, message = "Years of service must be at least 0")
    int yearsOfService;
}

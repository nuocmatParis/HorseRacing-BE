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

    @NotBlank(message = "License number is required")
    @Size(max = 50, message = "License number must not exceed 50 characters")
    @Pattern(regexp = "^(VET)-[A-Z0-9]{8}$", message = "License number must follow format VET-XXXXXXXX")
    String licenseNumber;

    @Size(max = 100, message = "Specialization must not exceed 100 characters")
    String specialization;

    @Min(value = 0, message = "Years of service must be at least 0")
    int yearsOfService;
}

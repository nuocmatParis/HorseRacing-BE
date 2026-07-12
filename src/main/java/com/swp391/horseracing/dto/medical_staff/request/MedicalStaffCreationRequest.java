package com.swp391.horseracing.dto.medical_staff.request;

import jakarta.validation.constraints.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class MedicalStaffCreationRequest {

    @NotNull(message = "User ID is required")
    UUID userId;

    @Size(max = 100, message = "Certification must not exceed 100 characters")
    String certification;

    @Min(value = 0, message = "Years of service must be at least 0")
    int yearsOfService;
}

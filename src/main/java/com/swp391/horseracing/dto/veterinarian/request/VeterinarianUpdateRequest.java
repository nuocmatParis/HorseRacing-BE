package com.swp391.horseracing.dto.veterinarian.request;

import jakarta.validation.constraints.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
public class VeterinarianUpdateRequest {

    @Size(max = 100, message = "Specialization must not exceed 100 characters")
    String specialization;

    @Min(value = 0, message = "Years of service must be at least 0")
    int yearsOfService;
}

package com.swp391.horseracing.dto.medical_staff.request;

import jakarta.validation.constraints.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
public class MedicalStaffUpdateRequest {

    @Size(max = 100, message = "Certification must not exceed 100 characters")
    String certification;

    @Min(value = 0, message = "Years of service must be at least 0")
    int yearsOfService;
}

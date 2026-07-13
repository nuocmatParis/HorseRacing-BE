package com.swp391.horseracing.dto.medicalstaff.request;

import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AssignInspectionStaffRequest {
    @NotNull(message = "Veterinarian id is required")
    UUID veterinarianId;

    @NotNull(message = "Medical staff id is required")
    UUID medStaffId;
}

package com.swp391.horseracing.dto.medicalstaff.response;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class InspectionStaffAssignmentResponse {
    UUID assignmentId;

    UUID raceId;
    String raceName;

    UUID veterinarianId;
    String veterinarianName;

    UUID medStaffId;
    String medicalStaffName;

    String certification;
    Integer yearsOfService;

    UUID assignedById;
    String assignedByName;

    LocalDateTime assignedAt;
}

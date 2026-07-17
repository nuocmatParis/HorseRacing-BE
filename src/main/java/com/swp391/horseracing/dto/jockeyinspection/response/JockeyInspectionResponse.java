package com.swp391.horseracing.dto.jockeyinspection.response;

import com.swp391.horseracing.enums.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class JockeyInspectionResponse {
    UUID jockeyInspectionId;

    UUID entryId;

    UUID jockeyId;
    String jockeyName;

    UUID medStaffId;
    String medicalStaffName;

    String certification;

    InspectionResult result;

    String note;

    LocalDateTime inspectedAt;

    Float registeredWeight;

    Float actualWeight;

    Boolean dopingDetected;

    InspectionStatus status;
}

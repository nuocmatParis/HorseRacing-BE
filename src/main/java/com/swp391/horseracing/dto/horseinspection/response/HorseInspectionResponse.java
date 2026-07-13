package com.swp391.horseracing.dto.horseinspection.response;
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
public class HorseInspectionResponse {
    UUID horseInspectionId;

    UUID entryId;

    UUID horseId;
    String horseName;

    UUID veterinarianId;
    String veterinarianName;

    InspectionResult result;

    String note;

    LocalDateTime inspectedAt;

    Float handicapWeight;

    Boolean handicapConfirmed;

    LocalDateTime confirmedAt;

    InspectionStatus status;
}

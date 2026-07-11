package com.swp391.horseracing.dto.appeal_evidence.response;

import com.swp391.horseracing.enums.AppealEvidenceType;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AppealEvidenceResponse {

    UUID evidenceId;
    UUID appealId;
    AppealEvidenceType type;
    String fileUrl;
    String textContent;
    String description;
    LocalDateTime uploadedAt;
}

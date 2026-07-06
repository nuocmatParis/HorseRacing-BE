package com.swp391.horseracing.dto.appeal_evidence.request;

import com.swp391.horseracing.enums.AppealEvidenceType;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AddAppealEvidenceRequest {

    @NotNull(message = "Evidence type is required")
    AppealEvidenceType type;

    String fileUrl;

    String textContent;

    String description;
}

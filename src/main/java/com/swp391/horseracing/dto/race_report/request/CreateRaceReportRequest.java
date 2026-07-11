package com.swp391.horseracing.dto.race_report.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CreateRaceReportRequest {

    @NotNull(message = "Race ID is required")
    UUID raceId;

    @NotBlank(message = "Summary is required")
    String summary;

    String appealNote;
}

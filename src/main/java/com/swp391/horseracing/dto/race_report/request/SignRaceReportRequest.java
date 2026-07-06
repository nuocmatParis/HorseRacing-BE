package com.swp391.horseracing.dto.race_report.request;

import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class SignRaceReportRequest {

    @NotNull(message = "Referee ID is required")
    UUID refereeId;
}

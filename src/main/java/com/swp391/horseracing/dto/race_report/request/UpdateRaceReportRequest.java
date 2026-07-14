package com.swp391.horseracing.dto.race_report.request;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateRaceReportRequest {
    @Size(max = 10000, message = "Summary must not exceed 10000 characters")
    private String summary;

    @Size(max = 10000, message = "Appeal note must not exceed 10000 characters")
    private String appealNote;
}

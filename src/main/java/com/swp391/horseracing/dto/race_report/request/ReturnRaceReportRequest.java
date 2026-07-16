package com.swp391.horseracing.dto.race_report.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ReturnRaceReportRequest {
    @NotBlank
    @Size(max = 2000)
    private String reason;
}

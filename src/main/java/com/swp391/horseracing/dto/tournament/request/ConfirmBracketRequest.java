package com.swp391.horseracing.dto.tournament.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ConfirmBracketRequest {

    @NotNull(message = "Max approved entries is required")
    @Min(value = 8, message = "Max approved entries must be at least 8")
    private Integer maxApprovedEntries;

    @NotNull(message = "Expected bracket plan version is required")
    @Min(value = 1, message = "Expected bracket plan version must be at least 1")
    private Integer expectedPlanVersion;
}

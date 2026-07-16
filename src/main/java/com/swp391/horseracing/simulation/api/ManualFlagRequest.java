package com.swp391.horseracing.simulation.api;

import com.swp391.horseracing.simulation.domain.SimulationSeverity;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record ManualFlagRequest(
        @NotNull UUID entryId,
        @NotNull SimulationSeverity severity,
        double raceTimeSeconds,
        @NotBlank @Size(min = 5, max = 2000) String note) {
}

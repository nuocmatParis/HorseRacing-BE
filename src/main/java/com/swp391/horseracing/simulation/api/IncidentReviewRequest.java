package com.swp391.horseracing.simulation.api;

import jakarta.validation.constraints.Size;

public record IncidentReviewRequest(@Size(max = 2000) String note) {
}

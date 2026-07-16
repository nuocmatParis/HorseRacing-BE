package com.swp391.horseracing.simulation.api;

import java.util.UUID;

public record ViolationDraftResponse(
        UUID entryId,
        String suggestedType,
        String description,
        String suggestedPenaltyType) {
}

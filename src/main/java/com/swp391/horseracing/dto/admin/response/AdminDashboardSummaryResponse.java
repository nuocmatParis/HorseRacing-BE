package com.swp391.horseracing.dto.admin.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class AdminDashboardSummaryResponse {
    private long totalTournaments;
    private long pendingRegistrations;
    private long pendingContracts;
    private long activeContracts;
    private long scheduledRaces;
    private LocalDateTime generatedAt;
}

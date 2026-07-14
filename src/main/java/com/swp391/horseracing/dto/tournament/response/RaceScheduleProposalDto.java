package com.swp391.horseracing.dto.tournament.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RaceScheduleProposalDto {
    UUID raceId;
    String raceName;
    int sequenceOrder;
    LocalDateTime suggestedStartTime;
    LocalDateTime suggestedEndTime;
}

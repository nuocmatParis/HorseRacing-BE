package com.swp391.horseracing.dto.tournament.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RoundScheduleProposalDto {
    UUID roundId;
    String roundName;
    int sequenceOrder;
    LocalDateTime suggestedStartDate;
    LocalDateTime suggestedEndDate;
    List<RaceScheduleProposalDto> races;
}

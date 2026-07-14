package com.swp391.horseracing.dto.tournament.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RoundQualifierResponse {

    UUID sourceRoundId;
    UUID sourceRaceId;
    String sourceRaceName;
    Integer sourceRaceSequence;
    UUID sourceEntryId;
    Integer rank;
    UUID contractId;
    UUID horseId;
    String horseName;
    UUID jockeyId;
    String jockeyName;
    UUID nextRoundId;
    UUID nextRaceId;
    Integer nextLaneNumber;
}

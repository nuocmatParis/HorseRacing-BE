package com.swp391.horseracing.dto.tournament.response;

import com.swp391.horseracing.enums.TournamentPhase;
import com.swp391.horseracing.enums.TournamentStatus;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class TournamentResponse {

    UUID tournamentId;
    String name;
    String description;
    LocalDate startDate;
    LocalDate endDate;
    LocalDateTime finishedAt;
    String location;
    BigDecimal registrationFee;
    BigDecimal systemContractFee;
    BigDecimal totalPrizePool;
    String allowedBreed;
    String raceClass;
    String weightClass;
    int minHorseAge;
    int maxHorseAge;
    String tournamentDivision;
    String handicapRule;
    String predictionRewardRule;
    int predictionOpenMinutesBefore;
    int predictionCloseMinutesBefore;
    TournamentStatus status;
    TournamentPhase phase;
    LocalDateTime createdAt;
    LocalDateTime publishedAt;
    UUID createdById;
    String createdByName;
}

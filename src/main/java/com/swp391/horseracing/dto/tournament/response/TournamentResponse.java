package com.swp391.horseracing.dto.tournament.response;

import com.swp391.horseracing.enums.HorseBreed;
import com.swp391.horseracing.enums.RaceClass;
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
    HorseBreed allowedBreed;
    RaceClass raceClass;
    int minHorseAge;
    int maxHorseAge;
    int topWeightLbs;
    int minWeightLbs;
    double equipmentWeightKg;
    boolean handicapEnabled;
    int predictionTop1CorrectPoints;
    int predictionTop3ExactPositionPoints;
    int predictionTop3CorrectHorsePoints;
    int predictionTop3PerfectBonusPoints;
    int predictionOpenMinutesBefore;
    int predictionCloseMinutesBefore;
    Integer maxRounds;
    Integer maxApprovedHorses;
    Integer maxApprovedJockeys;
    TournamentStatus status;
    TournamentPhase phase;
    LocalDateTime createdAt;
    LocalDateTime publishedAt;
    LocalDateTime registrationOpenAt;
    LocalDateTime registrationCloseAt;
    LocalDateTime reviewDeadlineAt;
    LocalDateTime jockeyMatchingDeadlineAt;
    LocalDateTime schedulingDeadlineAt;
    UUID createdById;
    String createdByName;
    boolean overdue;
}

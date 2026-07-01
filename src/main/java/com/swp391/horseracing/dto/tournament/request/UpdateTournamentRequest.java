package com.swp391.horseracing.dto.tournament.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UpdateTournamentRequest {

    @Size(max = 150, message = "Tournament name must not exceed 150 characters")
    String name;

    @Size(max = 150, message = "Description must not exceed 150 characters")
    String description;

    LocalDate startDate;

    LocalDate endDate;

    @Size(max = 200, message = "Location must not exceed 200 characters")
    String location;

    @Min(value = 0, message = "Registration fee must be positive")
    BigDecimal registrationFee;

    @Min(value = 0, message = "System contract fee must be positive")
    BigDecimal systemContractFee;

    @Min(value = 0, message = "Total prize pool must be positive")
    BigDecimal totalPrizePool;

    @Size(max = 100, message = "Allowed breed must not exceed 100 characters")
    String allowedBreed;

    @Size(max = 50, message = "Race class must not exceed 50 characters")
    String raceClass;

    @Size(max = 50, message = "Weight class must not exceed 50 characters")
    String weightClass;

    @Min(value = 0, message = "Min horse age must be at least 0")
    Integer minHorseAge;

    @Min(value = 0, message = "Max horse age must be at least 0")
    Integer maxHorseAge;

    @Size(max = 100, message = "Tournament division must not exceed 100 characters")
    String tournamentDivision;

    String handicapRule;

    String predictionRewardRule;

    @Min(value = 1, message = "Prediction open minutes must be at least 1")
    Integer predictionOpenMinutesBefore;

    @Min(value = 0, message = "Prediction close minutes must be at least 0")
    Integer predictionCloseMinutesBefore;

    LocalDateTime registrationOpenAt;

    LocalDateTime registrationCloseAt;

    LocalDateTime reviewDeadlineAt;

    LocalDateTime jockeyMatchingDeadlineAt;

    LocalDateTime schedulingDeadlineAt;

    @Min(value = 1, message = "Max rounds must be at least 1")
    Integer maxRounds;
}

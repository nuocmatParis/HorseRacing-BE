package com.swp391.horseracing.dto.tournament.request;

import com.swp391.horseracing.enums.HorseBreed;
import com.swp391.horseracing.enums.RaceClass;
import com.swp391.horseracing.enums.RaceDistance;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

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

    HorseBreed allowedBreed;

    RaceClass raceClass;

    RaceDistance distance;

    @Size(max = 50, message = "Weight class must not exceed 50 characters")
    String weightClass;

    @Min(value = 0, message = "Min horse age must be at least 0")
    Integer minHorseAge;

    @Min(value = 0, message = "Max horse age must be at least 0")
    Integer maxHorseAge;

    @Min(value = 0, message = "Top weight must be at least 0")
    Integer topWeightLbs;

    @Min(value = 0, message = "Min weight must be at least 0")
    Integer minWeightLbs;

    Double equipmentWeightKg;

    Boolean handicapEnabled;

    @Min(value = 0, message = "Prediction TOP 1 correct points must be at least 0")
    Integer predictionTop1CorrectPoints;

    @Min(value = 0, message = "Prediction TOP 3 exact position points must be at least 0")
    Integer predictionTop3ExactPositionPoints;

    @Min(value = 0, message = "Prediction TOP 3 correct horse points must be at least 0")
    Integer predictionTop3CorrectHorsePoints;

    @Min(value = 0, message = "Prediction TOP 3 perfect bonus points must be at least 0")
    Integer predictionTop3PerfectBonusPoints;

    @Min(value = 1, message = "Prediction open minutes must be at least 1")
    Integer predictionOpenMinutesBefore;

    @Min(value = 0, message = "Prediction close minutes must be at least 0")
    Integer predictionCloseMinutesBefore;

    @Min(value = 1, message = "Prediction card open hours must be at least 1")
    Integer predictionCardOpenHoursBeforeFirstRace;

    @Min(value = 0, message = "Inspection open minutes must be at least 0")
    Integer inspectionOpenMinutesBefore;

    @Min(value = 0, message = "Inspection close minutes must be at least 0")
    Integer inspectionCloseMinutesBefore;

    @Min(value = 1, message = "Max races per day must be at least 1")
    @Max(value = 9, message = "Max races per day must be at most 9")
    Integer maxRacesPerDay;

    @Min(value = 1, message = "Min race interval minutes must be at least 1")
    @Max(value = 30, message = "Min race interval minutes must be at most 30")
    Integer minRaceIntervalMinutes;

    @Min(value = 0, message = "Start early tolerance minutes must be at least 0")
    Integer startEarlyToleranceMinutes;

    @Min(value = 0, message = "Start late tolerance minutes must be at least 0")
    Integer startLateToleranceMinutes;

    @Min(value = 1, message = "Default race operational minutes must be at least 1")
    Integer defaultRaceOperationalMinutes;

    LocalTime raceDayStartTime;

    LocalTime raceDayEndTime;

    Boolean applyBreakTime;

    LocalTime breakStartTime;

    LocalTime breakEndTime;

    LocalDateTime registrationOpenAt;

    LocalDateTime registrationCloseAt;

    LocalDateTime reviewDeadlineAt;

    LocalDateTime jockeyMatchingDeadlineAt;

    LocalDateTime schedulingDeadlineAt;

    @Min(value = 8, message = "Max approved entries must be at least 8")
    Integer maxApprovedEntries;

}

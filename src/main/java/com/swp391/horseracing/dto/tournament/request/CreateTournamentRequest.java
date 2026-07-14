package com.swp391.horseracing.dto.tournament.request;

import com.swp391.horseracing.enums.HorseBreed;
import com.swp391.horseracing.enums.RaceClass;
import com.swp391.horseracing.enums.RaceDistance;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
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
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CreateTournamentRequest {

    @NotBlank(message = "Tournament name is required")
    @Size(max = 150, message = "Tournament name must not exceed 150 characters")
    String name;

    @NotBlank(message = "Description is required")
    @Size(max = 150, message = "Description must not exceed 150 characters")
    String description;

    @NotNull(message = "Start date is required")
    @FutureOrPresent(message = "Start date must be today or in the future")
    LocalDate startDate;

    @NotNull(message = "End date is required")
    @FutureOrPresent(message = "End date must be today or in the future")
    LocalDate endDate;

    @NotBlank(message = "Location is required")
    @Size(max = 200, message = "Location must not exceed 200 characters")
    String location;

    @NotNull(message = "Registration fee is required")
    @Positive(message = "Registration fee must be positive")
    BigDecimal registrationFee;

    @NotNull(message = "System contract fee is required")
    @Positive(message = "System contract fee must be positive")
    BigDecimal systemContractFee;

    @NotNull(message = "Total prize pool is required")
    @Positive(message = "Total prize pool must be positive")
    BigDecimal totalPrizePool;

    @NotNull(message = "Allowed breed is required")
    HorseBreed allowedBreed;

    @NotNull(message = "Race class is required")
    RaceClass raceClass;

    @NotNull(message = "Distance is required")
    RaceDistance distance;

    @NotNull(message = "Min horse age is required")
    @Min(value = 0, message = "Min horse age must be at least 0")
    Integer minHorseAge;

    @NotNull(message = "Max horse age is required")
    @Min(value = 0, message = "Max horse age must be at least 0")
    Integer maxHorseAge;

    @Min(value = 0, message = "Top weight must be at least 0")
    Integer topWeightLbs;

    @Min(value = 0, message = "Min weight must be at least 0")
    Integer minWeightLbs;

    Double equipmentWeightKg;

    @NotNull(message = "Handicap enabled is required")
    Boolean handicapEnabled;

    @Min(value = 0, message = "Prediction TOP 1 correct points must be at least 0")
    Integer predictionTop1CorrectPoints = 100;

    @Min(value = 0, message = "Prediction TOP 3 exact position points must be at least 0")
    Integer predictionTop3ExactPositionPoints = 30;

    @Min(value = 0, message = "Prediction TOP 3 correct horse points must be at least 0")
    Integer predictionTop3CorrectHorsePoints = 10;

    @Min(value = 0, message = "Prediction TOP 3 perfect bonus points must be at least 0")
    Integer predictionTop3PerfectBonusPoints = 50;

    @Min(value = 7, message = "Min round gap days must be at least 7")
    Integer minRoundGapDays = 7;

    @NotNull(message = "Max approved entries is required")
    @Min(value = 8, message = "Max approved entries must be at least 8")
    Integer maxApprovedEntries;


    @Min(value = 1, message = "Prediction open minutes must be at least 1")
    Integer predictionOpenMinutesBefore = 120;

    @Min(value = 0, message = "Prediction close minutes must be at least 0")
    Integer predictionCloseMinutesBefore = 5;

    @Min(value = 1, message = "Prediction card open hours must be at least 1")
    Integer predictionCardOpenHoursBeforeFirstRace = 24;

    @Min(value = 0, message = "Inspection open minutes must be at least 0")
    Integer inspectionOpenMinutesBefore = 90;

    @Min(value = 0, message = "Inspection close minutes must be at least 0")
    Integer inspectionCloseMinutesBefore = 30;

    @Min(value = 1, message = "Max races per day must be at least 1")
    @Max(value = 9, message = "Max races per day must be at most 9")
    Integer maxRacesPerDay = 9;

    @Min(value = 30, message = "Min race interval minutes must be at least 30")
    @Max(value = 60, message = "Min race interval minutes must be at most 60")
    Integer minRaceIntervalMinutes = 35;

    @Min(value = 0, message = "Start early tolerance minutes must be at least 0")
    Integer startEarlyToleranceMinutes = 0;

    @Min(value = 0, message = "Start late tolerance minutes must be at least 0")
    Integer startLateToleranceMinutes = 30;

    @Min(value = 1, message = "Default race operational minutes must be at least 1")
    Integer defaultRaceOperationalMinutes = 30;

    LocalTime raceDayStartTime = LocalTime.of(8, 0);

    LocalTime raceDayEndTime = LocalTime.of(18, 0);

    Boolean applyBreakTime = false;

    LocalTime breakStartTime;

    LocalTime breakEndTime;

    @NotNull(message = "Registration open time is required")
    LocalDateTime registrationOpenAt;

    @NotNull(message = "Registration close time is required")
    LocalDateTime registrationCloseAt;

    @NotNull(message = "Review deadline is required")
    LocalDateTime reviewDeadlineAt;

    @NotNull(message = "Jockey matching deadline is required")
    LocalDateTime jockeyMatchingDeadlineAt;

    @NotNull(message = "Scheduling deadline is required")
    LocalDateTime schedulingDeadlineAt;
}

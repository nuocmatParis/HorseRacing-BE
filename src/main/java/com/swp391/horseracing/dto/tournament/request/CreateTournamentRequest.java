package com.swp391.horseracing.dto.tournament.request;

import jakarta.validation.constraints.FutureOrPresent;
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

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
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

    @NotBlank(message = "Allowed breed is required")
    @Size(max = 100, message = "Allowed breed must not exceed 100 characters")
    String allowedBreed;

    @NotBlank(message = "Race class is required")
    @Size(max = 50, message = "Race class must not exceed 50 characters")
    String raceClass;

    @NotBlank(message = "Weight class is required")
    @Size(max = 50, message = "Weight class must not exceed 50 characters")
    String weightClass;

    @NotNull(message = "Min horse age is required")
    @Min(value = 0, message = "Min horse age must be at least 0")
    Integer minHorseAge;

    @NotNull(message = "Max horse age is required")
    @Min(value = 0, message = "Max horse age must be at least 0")
    Integer maxHorseAge;

    @NotBlank(message = "Tournament division is required")
    @Size(max = 100, message = "Tournament division must not exceed 100 characters")
    String tournamentDivision;

    @NotBlank(message = "Handicap rule is required")
    String handicapRule;

    @NotBlank(message = "Prediction reward rule is required")
    String predictionRewardRule;

    @Builder.Default
    @Min(value = 1, message = "Prediction open minutes must be at least 1")
    Integer predictionOpenMinutesBefore = 120;

    @Builder.Default
    @Min(value = 0, message = "Prediction close minutes must be at least 0")
    Integer predictionCloseMinutesBefore = 5;

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

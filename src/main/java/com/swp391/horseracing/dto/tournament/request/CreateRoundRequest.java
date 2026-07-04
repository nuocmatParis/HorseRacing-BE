package com.swp391.horseracing.dto.tournament.request;

import com.swp391.horseracing.enums.PredictionType;
import com.swp391.horseracing.enums.RoundStatus;
import jakarta.validation.Valid;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CreateRoundRequest {

    @NotBlank(message = "Round name is required")
    @Size(max = 100, message = "Round name must not exceed 100 characters")
    String roundName;

    @Min(value = 1, message = "Sequence order must be at least 1")
    int sequenceOrder;

    Boolean isFinal;

    @NotNull(message = "Prediction type is required")
    PredictionType predictionType;

    @NotNull(message = "Advancement rule is required")
    String advancementRule;

    @NotNull(message = "Start date is required")
    @FutureOrPresent(message = "Start date must be today or in the future")
    LocalDateTime startDate;

    @NotNull(message = "End date is required")
    @FutureOrPresent(message = "End date must be today or in the future")
    LocalDateTime endDate;

    @NotNull(message = "Description is required")
    String description;

    @NotNull(message = "Max races is required")
    @Min(value = 1, message = "Max races must be at least 1")
    Integer maxRaces;

    @NotNull(message = "Status is required")
    RoundStatus status;

    UUID headRefereeId;

    @Valid
    List<CreateRaceRequest> races;
}

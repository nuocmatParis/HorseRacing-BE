package com.swp391.horseracing.dto.tournament.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class RescheduleRaceRequest {

    @NotNull(message = "New start time is required")
    LocalDateTime newStartTime;

    @NotBlank(message = "Reason is required")
    String reason;
}

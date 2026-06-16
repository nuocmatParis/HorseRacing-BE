package com.swp391.horseracing.dto.tournament.request;

import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class RegisterHorseRequest {

    @NotNull(message = "Horse ID is required")
    UUID horseId;
}

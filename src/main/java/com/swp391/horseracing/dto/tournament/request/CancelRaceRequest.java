package com.swp391.horseracing.dto.tournament.request;

import jakarta.validation.constraints.NotBlank;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CancelRaceRequest {

    @NotBlank(message = "Reason is required")
    String reason;
}

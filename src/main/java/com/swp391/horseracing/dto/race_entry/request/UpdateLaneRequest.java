package com.swp391.horseracing.dto.race_entry.request;

import jakarta.validation.constraints.Min;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UpdateLaneRequest {

    @Min(value = 1, message = "Lane number must be at least 1")
    Integer laneNumber;
}

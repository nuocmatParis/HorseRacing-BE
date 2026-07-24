package com.swp391.horseracing.dto.phasetiming.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class PhaseTimingConfigRequest {

    @NotBlank(message = "Phase name is required")
    @Size(max = 50, message = "Phase name must not exceed 50 characters")
    String phaseName;

    @NotNull(message = "Min capacity is required")
    @Min(value = 0, message = "Min capacity must be at least 0")
    Integer minCapacity;

    @NotNull(message = "Max capacity is required")
    @Min(value = 0, message = "Max capacity must be at least 0")
    Integer maxCapacity;

    @NotNull(message = "Duration days is required")
    @Min(value = 1, message = "Duration days must be at least 1")
    Integer durationDays;

    @Size(max = 255, message = "Description must not exceed 255 characters")
    String description;
}

package com.swp391.horseracing.dto.phasetiming.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class PhaseTimingConfigBatchRequest {

    @NotEmpty(message = "Config list must not be empty")
    @Valid
    List<PhaseTimingConfigRequest> configs;
}

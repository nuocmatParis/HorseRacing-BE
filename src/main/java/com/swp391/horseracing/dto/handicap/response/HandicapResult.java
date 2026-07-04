package com.swp391.horseracing.dto.handicap.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.AccessLevel;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class HandicapResult {
    double assignedWeightLbs;
    double assignedWeightKg;
    double ballastKg;
    String status;
}

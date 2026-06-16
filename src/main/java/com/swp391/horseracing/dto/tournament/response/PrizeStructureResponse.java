package com.swp391.horseracing.dto.tournament.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class PrizeStructureResponse {

    UUID prizeStructureId;
    int rank;
    Float percentage;
    BigDecimal fixedAmount;
    boolean isActive;
    UUID tournamentId;
}

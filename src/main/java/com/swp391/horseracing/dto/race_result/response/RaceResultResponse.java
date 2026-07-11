package com.swp391.horseracing.dto.race_result.response;

import com.swp391.horseracing.enums.PrizeStatus;
import com.swp391.horseracing.enums.RaceResultStatus;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class RaceResultResponse {

    UUID resultId;
    UUID raceId;
    UUID entryId;
    Float finishTime;
    int rank;
    BigDecimal prizeMoney;
    BigDecimal ownerPrizeAmount;
    BigDecimal jockeyPrizeAmount;
    PrizeStatus prizeStatus;
    boolean isPrizePaid;
    LocalDateTime prizePaidAt;
    RaceResultStatus status;
    UUID recordedById;
    LocalDateTime recordedAt;
    LocalDateTime updatedAt;
}

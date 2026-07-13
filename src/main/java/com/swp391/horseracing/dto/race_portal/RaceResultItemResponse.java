package com.swp391.horseracing.dto.race_portal;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.swp391.horseracing.enums.PrizeStatus;
import com.swp391.horseracing.enums.RaceResultStatus;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class RaceResultItemResponse {
    private UUID resultId;
    private RaceEntryViewResponse entry;
    private Float finishTime;
    private Integer rank;
    private RaceResultStatus status;
    private BigDecimal prizeMoney;
    private BigDecimal myPrizeAmount;
    private PrizeStatus prizeStatus;
    private boolean prizePaid;
}

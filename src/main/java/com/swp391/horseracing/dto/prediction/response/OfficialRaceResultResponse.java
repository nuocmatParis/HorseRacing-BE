package com.swp391.horseracing.dto.prediction.response;

import lombok.Builder;
import lombok.Data;

import java.util.UUID;

@Data
@Builder
public class OfficialRaceResultResponse {
    private UUID entryId;
    private UUID horseId;
    private String horseName;
    private int rank;
}

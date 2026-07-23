package com.swp391.horseracing.dto.tournament.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.AccessLevel;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class TournamentRatingConfigResponse {
    int firstMin;
    int firstMax;
    int secondMin;
    int secondMax;
    int thirdMin;
    int thirdMax;
    int fourthFifthMin;
    int fourthFifthMax;
    int otherMin;
    int otherMax;
    int disqualifiedMin;
    int disqualifiedMax;
    int policyVersion;
    boolean locked;
    LocalDateTime lockedAt;
}

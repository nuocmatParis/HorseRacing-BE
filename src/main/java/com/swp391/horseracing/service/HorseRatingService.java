package com.swp391.horseracing.service;

import com.swp391.horseracing.dto.horse.*;
import com.swp391.horseracing.entity.HorseRatingHistory;
import com.swp391.horseracing.enums.RaceResultStatus;

import java.util.List;
import java.util.UUID;

public interface HorseRatingService {
    RaceRatingPreviewResponse previewForRace(UUID raceId);
    RaceRatingChangesResponse getRatingChangesForRace(UUID raceId);
    List<HorseRatingHistoryResponse> getRatingHistoryForHorse(UUID horseId);
    RoundRatingSummaryResponse getRoundRatingSummary(UUID roundId);

    void validateRatingChange(RaceResultStatus status, Integer rank, Integer ratingChange);

    List<HorseRatingHistory> applyManualRatingsForPublish(UUID raceId);
}

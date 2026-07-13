package com.swp391.horseracing.service;

import com.swp391.horseracing.dto.horse.*;
import com.swp391.horseracing.entity.HorseRatingHistory;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public interface HorseRatingService {
    List<HorseRatingCalculation> calculateForRace(UUID raceId, Map<UUID, Integer> ratingSnapshot);

    RaceRatingPreviewResponse previewForRace(UUID raceId);
    RaceRatingChangesResponse getRatingChangesForRace(UUID raceId);
    List<HorseRatingHistoryResponse> getRatingHistoryForHorse(UUID horseId);
    RoundRatingSummaryResponse getRoundRatingSummary(UUID roundId);

    List<HorseRatingHistory> calculateAndApplyForPublish(UUID raceId);
}

package com.swp391.horseracing.service;

import com.swp391.horseracing.dto.prediction.response.AIPredictionAggregateResponse;

import java.util.UUID;

public interface AIPredictionService {

    AIPredictionAggregateResponse generatePredictions(UUID raceId, int topN);

    AIPredictionAggregateResponse getAdminPredictionsByRace(UUID raceId);

    AIPredictionAggregateResponse getPublishedPredictionsByRace(UUID raceId);

    AIPredictionAggregateResponse publishPredictions(UUID raceId);

    AIPredictionAggregateResponse unpublishPredictions(UUID raceId);
}

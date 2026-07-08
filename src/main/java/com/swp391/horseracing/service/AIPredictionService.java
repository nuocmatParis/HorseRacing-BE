package com.swp391.horseracing.service;

import com.swp391.horseracing.dto.prediction.response.AIPredictionResponse;

import java.util.List;
import java.util.UUID;

public interface AIPredictionService {

    List<AIPredictionResponse> generatePredictions(UUID raceId);

    List<AIPredictionResponse> getPredictionsByRace(UUID raceId);
}

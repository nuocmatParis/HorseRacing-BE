package com.swp391.horseracing.service;

import com.swp391.horseracing.dto.prediction.request.CreatePredictionRequest;
import com.swp391.horseracing.dto.prediction.request.UpdatePredictionRequest;
import com.swp391.horseracing.dto.prediction.response.PredictionResponse;

import java.util.List;
import java.util.UUID;

public interface PredictionService {

    PredictionResponse createPrediction(UUID raceId, CreatePredictionRequest request);

    PredictionResponse updatePrediction(UUID predictionId, UpdatePredictionRequest request);

    void cancelPrediction(UUID predictionId);

    List<PredictionResponse> getMyPredictions();

    PredictionResponse getPredictionDetail(UUID predictionId);

    PredictionResponse getMyPredictionByRace(UUID raceId);
}

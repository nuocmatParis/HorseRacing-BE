package com.swp391.horseracing.service.impl;

import com.swp391.horseracing.entity.*;
import com.swp391.horseracing.enums.PredictionDetailStatus;
import com.swp391.horseracing.enums.PredictionStatus;
import com.swp391.horseracing.enums.PredictionType;
import com.swp391.horseracing.enums.RaceResultStatus;
import com.swp391.horseracing.repository.*;
import com.swp391.horseracing.service.BusinessNotificationEventService;
import com.swp391.horseracing.service.ScoringService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequiredArgsConstructor
public class ScoringServiceImpl implements ScoringService {

    PredictionRepository predictionRepository;
    RaceResultRepository raceResultRepository;
    SpectatorRepository spectatorRepository;
    BusinessNotificationEventService notificationEventService;

    @Override
    @Transactional
    public void scoreRace(UUID raceId) {
        List<Prediction> predictions = predictionRepository.findByRace_RaceIdAndStatus(raceId, PredictionStatus.PENDING);
        if (predictions.isEmpty()) {
            return;
        }

        Tournament tournament = predictions.get(0).getRace().getRound().getTournament();

        List<RaceResult> results = raceResultRepository.findByRace_RaceId(raceId);
        Map<UUID, RaceResult> resultMap = new HashMap<>();
        for (RaceResult result : results) {
            resultMap.put(result.getEntry().getEntryId(), result);
        }

        for (Prediction prediction : predictions) {
            scorePrediction(prediction, resultMap, tournament);
        }

        predictionRepository.saveAll(predictions);
        for (Prediction prediction : predictions) {
            notificationEventService.predictionScored(prediction);
        }
    }

    private void scorePrediction(Prediction prediction, Map<UUID, RaceResult> resultMap, Tournament tournament) {
        int totalRewardPoints = 0;
        boolean allExactPosition = true;

        for (PredictionDetail detail : prediction.getPredictionDetails()) {
            int points = scoreDetail(detail, resultMap, prediction.getPredictionType(), tournament);
            if (points < 0) {
                points = 0;
            }
            detail.setAwardedPoints(points);
            totalRewardPoints += points;

            RaceResult result = resultMap.get(detail.getEntry().getEntryId());
            if (result == null 
                    || result.getStatus() == RaceResultStatus.DISQUALIFIED 
                    || result.getStatus() == RaceResultStatus.DID_NOT_FINISH
                    || result.getRank() == null 
                    || !result.getRank().equals(detail.getPredictedRank())) {
                allExactPosition = false;
            }
        }

        if (prediction.getPredictionType() == PredictionType.TOP3 && allExactPosition) {
            totalRewardPoints += tournament.getPredictionTop3PerfectBonusPoints();
        }

        prediction.setRewardPoints(totalRewardPoints);
        prediction.setStatus(PredictionStatus.SCORED);
        prediction.setScoredAt(LocalDateTime.now());

        Spectator spectator = prediction.getSpectator();
        spectator.setTotalPoints(spectator.getTotalPoints() + totalRewardPoints);
        spectatorRepository.save(spectator);

    }

    private int scoreDetail(PredictionDetail detail, Map<UUID, RaceResult> resultMap,
                            PredictionType type, Tournament tournament) {
        RaceResult result = resultMap.get(detail.getEntry().getEntryId());

        if (result == null 
                || result.getStatus() == RaceResultStatus.DISQUALIFIED 
                || result.getStatus() == RaceResultStatus.DID_NOT_FINISH
                || result.getRank() == null) {
            detail.setStatus(PredictionDetailStatus.INCORRECT);
            return -1;
        }

        int actualRank = result.getRank();

        if (type == PredictionType.TOP1) {
            if (actualRank == 1 && detail.getPredictedRank() == 1) {
                detail.setStatus(PredictionDetailStatus.CORRECT);
                return tournament.getPredictionTop1CorrectPoints();
            } else {
                detail.setStatus(PredictionDetailStatus.INCORRECT);
                return -1;
            }
        }

        // TOP3
        boolean inTop3 = actualRank >= 1 && actualRank <= 3;

        if (inTop3 && detail.getPredictedRank() == actualRank) {
            detail.setStatus(PredictionDetailStatus.CORRECT);
            return tournament.getPredictionTop3ExactPositionPoints();
        }

        if (inTop3) {
            detail.setStatus(PredictionDetailStatus.CORRECT);
            return tournament.getPredictionTop3CorrectHorsePoints();
        }

        detail.setStatus(PredictionDetailStatus.INCORRECT);
        return -1;
    }
}

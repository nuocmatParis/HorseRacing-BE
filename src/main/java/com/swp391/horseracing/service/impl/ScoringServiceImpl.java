package com.swp391.horseracing.service.impl;

import com.swp391.horseracing.entity.*;
import com.swp391.horseracing.enums.NotificationType;
import com.swp391.horseracing.enums.PredictionDetailStatus;
import com.swp391.horseracing.enums.PredictionStatus;
import com.swp391.horseracing.enums.PredictionType;
import com.swp391.horseracing.enums.RaceResultStatus;
import com.swp391.horseracing.repository.*;
import com.swp391.horseracing.service.NotificationService;
import com.swp391.horseracing.service.ScoringService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequiredArgsConstructor
public class ScoringServiceImpl implements ScoringService {

    PredictionRepository predictionRepository;
    RaceResultRepository raceResultRepository;
    SpectatorRepository spectatorRepository;
    NotificationService notificationService;

    @Override
    @Transactional
    public void scoreRace(UUID raceId) {
        List<Prediction> predictions = predictionRepository.findByRace_RaceIdAndStatus(raceId, PredictionStatus.PENDING);
        if (predictions.isEmpty()) {
            return;
        }

        Tournament tournament = predictions.get(0).getRace().getRound().getTournament();

        List<RaceResult> results = raceResultRepository.findByRace_RaceId(raceId);
        Map<UUID, RaceResult> resultMap = results.stream()
                .collect(Collectors.toMap(r -> r.getEntry().getEntryId(), r -> r));

        for (Prediction prediction : predictions) {
            scorePrediction(prediction, resultMap, tournament);
        }

        predictionRepository.saveAll(predictions);
    }

    private void scorePrediction(Prediction prediction, Map<UUID, RaceResult> resultMap, Tournament tournament) {
        int totalRewardPoints = 0;
        boolean allExactPosition = true;

        for (PredictionDetail detail : prediction.getPredictionDetails()) {
            int points = scoreDetail(detail, resultMap, prediction.getPredictionType(), tournament);
            if (points < 0) {
                allExactPosition = false;
                points = 0;
            }
            detail.setAwardedPoints(points);
            totalRewardPoints += points;
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

        notificationService.sendNotification(
                spectator.getUser().getUserId(),
                "Prediction Scored",
                "Your prediction for race has been scored! You earned " + totalRewardPoints + " points.",
                NotificationType.PredictionScored,
                "Prediction",
                prediction.getPredictionId()
        );
    }

    private int scoreDetail(PredictionDetail detail, Map<UUID, RaceResult> resultMap,
                            PredictionType type, Tournament tournament) {
        RaceResult result = resultMap.get(detail.getEntry().getEntryId());

        if (result == null || result.getStatus() == RaceResultStatus.Disqualified) {
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

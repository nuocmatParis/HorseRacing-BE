package com.swp391.horseracing.service.impl;

import com.swp391.horseracing.dto.prediction.request.CreatePredictionRequest;
import com.swp391.horseracing.dto.prediction.request.PredictionEntryRequest;
import com.swp391.horseracing.dto.prediction.request.UpdatePredictionRequest;
import com.swp391.horseracing.dto.prediction.response.AIPredictionResponse;
import com.swp391.horseracing.dto.prediction.response.PredictionResponse;
import com.swp391.horseracing.entity.*;
import com.swp391.horseracing.enums.AccountStatus;
import com.swp391.horseracing.enums.PredictionDetailStatus;
import com.swp391.horseracing.enums.PredictionStatus;
import com.swp391.horseracing.enums.PredictionType;
import com.swp391.horseracing.enums.RoleName;
import com.swp391.horseracing.exception.AppException;
import com.swp391.horseracing.exception.ErrorCode;
import com.swp391.horseracing.mapper.AIPredictionMapper;
import com.swp391.horseracing.mapper.PredictionMapper;
import com.swp391.horseracing.repository.AIPredictionRepository;
import com.swp391.horseracing.repository.PredictionRepository;
import com.swp391.horseracing.repository.RaceEntryRepository;
import com.swp391.horseracing.repository.RaceRepository;
import com.swp391.horseracing.repository.SpectatorRepository;
import com.swp391.horseracing.service.PredictionService;
import com.swp391.horseracing.service.UserCurrentService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

@Service
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequiredArgsConstructor
public class PredictionServiceImpl implements PredictionService {

    PredictionRepository predictionRepository;
    RaceRepository raceRepository;
    RaceEntryRepository raceEntryRepository;
    SpectatorRepository spectatorRepository;
    PredictionMapper predictionMapper;
    UserCurrentService userCurrentService;
    AIPredictionRepository aiPredictionRepository;
    AIPredictionMapper aiPredictionMapper;

    @Override
    @Transactional
    public PredictionResponse createPrediction(UUID raceId, CreatePredictionRequest request) {
        Spectator spectator = getCurrentSpectator();

        Race race = raceRepository.findById(raceId)
                .orElseThrow(() -> new AppException(ErrorCode.RACE_NOT_FOUND));

        validatePredictionWindow(race);
        validateRaceNotStarted(race);

        UUID spectatorId = spectator.getSpectatorId();
        if (predictionRepository.existsByRace_RaceIdAndSpectator_SpectatorIdAndStatusNot(
                raceId, spectatorId, PredictionStatus.CANCELLED)) {
            throw new AppException(ErrorCode.PREDICTION_ALREADY_EXISTS);
        }

        List<PredictionEntryRequest> entries = request.getEntries();
        validateEntries(race, entries, request.getPredictionType());

        Prediction prediction = Prediction.builder()
                .spectator(spectator)
                .race(race)
                .predictionType(request.getPredictionType())
                .status(PredictionStatus.PENDING)
                .build();

        List<PredictionDetail> details = entries.stream()
                .map(e -> {
                    RaceEntry raceEntry = raceEntryRepository.findById(e.getEntryId())
                            .orElseThrow(() -> new AppException(ErrorCode.RACE_ENTRY_NOT_FOUND));
                    return PredictionDetail.builder()
                            .prediction(prediction)
                            .entry(raceEntry)
                            .predictedRank(e.getPredictedRank())
                            .status(PredictionDetailStatus.UNSCORED)
                            .build();
                })
                .toList();

        prediction.setPredictionDetails(details);

        return predictionMapper.toPredictionResponse(predictionRepository.save(prediction));
    }

    @Override
    @Transactional
    public PredictionResponse updatePrediction(UUID predictionId, UpdatePredictionRequest request) {
        Spectator spectator = getCurrentSpectator();

        Prediction prediction = predictionRepository.findById(predictionId)
                .orElseThrow(() -> new AppException(ErrorCode.PREDICTION_NOT_FOUND));

        if (!prediction.getSpectator().getSpectatorId().equals(spectator.getSpectatorId())) {
            throw new AppException(ErrorCode.PREDICTION_NOT_BELONG_TO_USER);
        }

        if (prediction.getStatus() == PredictionStatus.SCORED) {
            throw new AppException(ErrorCode.PREDICTION_ALREADY_SCORED);
        }
        if (prediction.getStatus() == PredictionStatus.CANCELLED) {
            throw new AppException(ErrorCode.PREDICTION_CANCELLED);
        }

        Race race = prediction.getRace();
        validatePredictionWindow(race);

        List<PredictionEntryRequest> entries = request.getEntries();
        validateEntries(race, entries, prediction.getPredictionType());

        prediction.getPredictionDetails().clear();

        List<PredictionDetail> newDetails = entries.stream()
                .map(e -> {
                    RaceEntry raceEntry = raceEntryRepository.findById(e.getEntryId())
                            .orElseThrow(() -> new AppException(ErrorCode.RACE_ENTRY_NOT_FOUND));
                    return PredictionDetail.builder()
                            .prediction(prediction)
                            .entry(raceEntry)
                            .predictedRank(e.getPredictedRank())
                            .status(PredictionDetailStatus.UNSCORED)
                            .build();
                })
                .toList();

        newDetails.forEach(prediction.getPredictionDetails()::add);

        return predictionMapper.toPredictionResponse(predictionRepository.save(prediction));
    }

    @Override
    @Transactional
    public void cancelPrediction(UUID predictionId) {
        Spectator spectator = getCurrentSpectator();

        Prediction prediction = predictionRepository.findById(predictionId)
                .orElseThrow(() -> new AppException(ErrorCode.PREDICTION_NOT_FOUND));

        if (!prediction.getSpectator().getSpectatorId().equals(spectator.getSpectatorId())) {
            throw new AppException(ErrorCode.PREDICTION_NOT_BELONG_TO_USER);
        }

        if (prediction.getStatus() == PredictionStatus.SCORED) {
            throw new AppException(ErrorCode.PREDICTION_ALREADY_SCORED);
        }
        if (prediction.getStatus() == PredictionStatus.CANCELLED) {
            throw new AppException(ErrorCode.PREDICTION_CANCELLED);
        }

        Race race = prediction.getRace();
        if (LocalDateTime.now().isAfter(race.getPredictionCloseAt())) {
            throw new AppException(ErrorCode.PREDICTION_WINDOW_CLOSED);
        }

        prediction.setStatus(PredictionStatus.CANCELLED);
        predictionRepository.save(prediction);
    }

    @Override
    public List<PredictionResponse> getMyPredictions() {
        Spectator spectator = getCurrentSpectator();
        return predictionMapper.toPredictionResponseList(
                predictionRepository.findBySpectator_SpectatorIdOrderByPredictionTimeDesc(
                        spectator.getSpectatorId()));
    }

    @Override
    public PredictionResponse getPredictionDetail(UUID predictionId) {
        Spectator spectator = getCurrentSpectator();

        Prediction prediction = predictionRepository.findById(predictionId)
                .orElseThrow(() -> new AppException(ErrorCode.PREDICTION_NOT_FOUND));

        if (!prediction.getSpectator().getSpectatorId().equals(spectator.getSpectatorId())) {
            throw new AppException(ErrorCode.PREDICTION_NOT_BELONG_TO_USER);
        }

        return attachAiPredictions(predictionMapper.toPredictionResponse(prediction), prediction.getRace().getRaceId());
    }

    @Override
    public PredictionResponse getMyPredictionByRace(UUID raceId) {
        Spectator spectator = getCurrentSpectator();

        Prediction prediction = predictionRepository
                .findByRace_RaceIdAndSpectator_SpectatorId(raceId, spectator.getSpectatorId())
                .orElseThrow(() -> new AppException(ErrorCode.PREDICTION_NOT_FOUND));

        return attachAiPredictions(predictionMapper.toPredictionResponse(prediction), raceId);
    }

    private PredictionResponse attachAiPredictions(PredictionResponse response, UUID raceId) {
        List<AIPrediction> aiPredictions = aiPredictionRepository.findByEntry_Race_RaceId(raceId);
        if (!aiPredictions.isEmpty()) {
            aiPredictions.sort(Comparator.comparingInt(AIPrediction::getPredictedRank));
            response.setAiPredictions(aiPredictionMapper.toAIPredictionResponseList(aiPredictions));
        }
        return response;
    }

    private Spectator getCurrentSpectator() {
        User currentUser = userCurrentService.getCurrentUser();

        if (currentUser.getRole().getRoleName() != RoleName.SPECTATOR) {
            throw new AppException(ErrorCode.ACCESS_DENIED);
        }
        if (currentUser.getStatus() != AccountStatus.ACTIVE) {
            throw new AppException(ErrorCode.USER_INACTIVE);
        }

        return spectatorRepository.findByUser_UserId(currentUser.getUserId())
                .orElseThrow(() -> new AppException(ErrorCode.SPECTATOR_PROFILE_NOT_FOUND));
    }

    private void validatePredictionWindow(Race race) {
        LocalDateTime now = LocalDateTime.now();
        if (now.isBefore(race.getPredictionOpenAt())) {
            throw new AppException(ErrorCode.PREDICTION_WINDOW_NOT_OPEN);
        }
        if (now.isAfter(race.getPredictionCloseAt())) {
            throw new AppException(ErrorCode.PREDICTION_WINDOW_CLOSED);
        }
    }

    private void validateRaceNotStarted(Race race) {
        if (race.getStartedAt() != null) {
            throw new AppException(ErrorCode.RACE_HAS_NOT_STARTED);
        }
    }

    private void validateEntries(Race race, List<PredictionEntryRequest> entries, PredictionType type) {
        if (type == PredictionType.TOP1) {
            if (entries.size() != 1) {
                throw new AppException(ErrorCode.INVALID_TOP1_COUNT);
            }
            if (entries.get(0).getPredictedRank() != 1) {
                throw new AppException(ErrorCode.INVALID_PREDICTED_RANK);
            }
        } else if (type == PredictionType.TOP3) {
            if (entries.size() != 3) {
                throw new AppException(ErrorCode.INVALID_TOP3_COUNT);
            }

            Set<Integer> ranks = new HashSet<>();
            Set<UUID> entryIds = new HashSet<>();
            for (PredictionEntryRequest e : entries) {
                if (e.getPredictedRank() < 1 || e.getPredictedRank() > 3) {
                    throw new AppException(ErrorCode.INVALID_PREDICTED_RANK);
                }
                if (!ranks.add(e.getPredictedRank())) {
                    throw new AppException(ErrorCode.INVALID_PREDICTED_RANK);
                }
                if (!entryIds.add(e.getEntryId())) {
                    throw new AppException(ErrorCode.DUPLICATE_HORSE_IN_PREDICTION);
                }
            }
        }

        Set<UUID> seenEntryIds = new HashSet<>();
        for (PredictionEntryRequest e : entries) {
            if (!seenEntryIds.add(e.getEntryId())) {
                throw new AppException(ErrorCode.DUPLICATE_HORSE_IN_PREDICTION);
            }

            RaceEntry raceEntry = raceEntryRepository.findById(e.getEntryId())
                    .orElseThrow(() -> new AppException(ErrorCode.RACE_ENTRY_NOT_FOUND));

            if (!raceEntry.getRace().getRaceId().equals(race.getRaceId())) {
                throw new AppException(ErrorCode.HORSE_NOT_IN_THIS_RACE);
            }
        }
    }
}

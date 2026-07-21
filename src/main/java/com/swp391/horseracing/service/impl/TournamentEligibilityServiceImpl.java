package com.swp391.horseracing.service.impl;

import com.swp391.horseracing.dto.tournament.request.CreateEligibilityRequest;
import com.swp391.horseracing.dto.tournament.request.UpdateEligibilityRequest;
import com.swp391.horseracing.dto.tournament.response.TournamentEligibilityResponse;
import com.swp391.horseracing.entity.Tournament;
import com.swp391.horseracing.entity.TournamentEligibility;
import com.swp391.horseracing.enums.EligibilityCondition;
import com.swp391.horseracing.enums.EligibilityTargetType;
import com.swp391.horseracing.enums.HorseBreed;
import com.swp391.horseracing.enums.JockeyTier;
import com.swp391.horseracing.enums.TournamentStatus;
import com.swp391.horseracing.exception.AppException;
import com.swp391.horseracing.exception.ErrorCode;
import com.swp391.horseracing.mapper.TournamentEligibilityMapper;
import com.swp391.horseracing.repository.TournamentEligibilityRepository;
import com.swp391.horseracing.repository.TournamentRepository;
import com.swp391.horseracing.service.TournamentEligibilityService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequiredArgsConstructor
public class TournamentEligibilityServiceImpl implements TournamentEligibilityService {

    TournamentEligibilityRepository tournamentEligibilityRepository;
    TournamentRepository tournamentRepository;
     TournamentEligibilityMapper tournamentEligibilityMapper;

    @Override
    @Transactional
    public TournamentEligibilityResponse create(UUID tournamentId, CreateEligibilityRequest request) {
        Tournament tournament = tournamentRepository.findById(tournamentId)
                .orElseThrow(() -> new AppException(ErrorCode.TOURNAMENT_NOT_FOUND));

        if (tournament.getStatus() != TournamentStatus.DRAFT) {
            throw new AppException(ErrorCode.TOURNAMENT_NOT_IN_DRAFT);
        }

        if (tournamentEligibilityRepository.existsByTournament_TournamentIdAndConditionName(
                tournamentId, request.getConditionName())) {
            throw new AppException(ErrorCode.ELIGIBILITY_CONDITION_EXISTS);
        }

        validateConditionValue(request.getConditionName(), request.getTargetType(), request.getConditionValue());

        TournamentEligibility eligibility = tournamentEligibilityMapper.toEligibility(request);
        eligibility.setTournament(tournament);

        return tournamentEligibilityMapper.toEligibilityResponse(tournamentEligibilityRepository.save(eligibility));
    }

    @Override
    @Transactional
    public TournamentEligibilityResponse update(UUID eligibilityId, UpdateEligibilityRequest request) {
        TournamentEligibility eligibility = tournamentEligibilityRepository.findById(eligibilityId)
                .orElseThrow(() -> new AppException(ErrorCode.TOURNAMENT_ELIGIBILITY_NOT_FOUND));

        Tournament tournament = eligibility.getTournament();
        if (tournament.getStatus() != TournamentStatus.DRAFT) {
            throw new AppException(ErrorCode.TOURNAMENT_NOT_IN_DRAFT);
        }

        if (request.getConditionName() != null && !request.getConditionName().equals(eligibility.getConditionName())
                && tournamentEligibilityRepository.existsByTournament_TournamentIdAndConditionName(
                        tournament.getTournamentId(), request.getConditionName())) {
            throw new AppException(ErrorCode.ELIGIBILITY_CONDITION_EXISTS);
        }

        if (request.getConditionValue() != null) {
            EligibilityCondition condition = request.getConditionName() != null
                    ? request.getConditionName() : eligibility.getConditionName();
            EligibilityTargetType target = request.getTargetType() != null
                    ? request.getTargetType() : eligibility.getTargetType();
            validateConditionValue(condition, target, request.getConditionValue());
        }

        tournamentEligibilityMapper.updateEligibility(request, eligibility);
        return tournamentEligibilityMapper.toEligibilityResponse(tournamentEligibilityRepository.save(eligibility));
    }

    @Override
    @Transactional
    public void delete(UUID eligibilityId) {
        TournamentEligibility eligibility = tournamentEligibilityRepository.findById(eligibilityId)
                .orElseThrow(() -> new AppException(ErrorCode.TOURNAMENT_ELIGIBILITY_NOT_FOUND));

        if (eligibility.getTournament().getStatus() != TournamentStatus.DRAFT) {
            throw new AppException(ErrorCode.TOURNAMENT_NOT_IN_DRAFT);
        }

        tournamentEligibilityRepository.delete(eligibility);
    }

    @Override
    public List<TournamentEligibilityResponse> getByTournament(UUID tournamentId){
        tournamentRepository.findById(tournamentId)
                .orElseThrow(() -> new AppException(ErrorCode.TOURNAMENT_NOT_FOUND));
        return tournamentEligibilityRepository.findByTournament_TournamentId(tournamentId)
                .stream().map(tournamentEligibilityMapper :: toEligibilityResponse).collect(Collectors.toList());
    }

    private void validateConditionValue(EligibilityCondition conditionName, EligibilityTargetType targetType, String conditionValue) {
        try {
            switch (conditionName) {
                case AGE, EXPERIENCE_YEARS -> {
                    int v = Integer.parseInt(conditionValue);
                    if (v < 0) throw new AppException(ErrorCode.ELIGIBILITY_INVALID_VALUE);
                }
                case WEIGHT -> {
                    float v = Float.parseFloat(conditionValue);
                    if (v <= 0) throw new AppException(ErrorCode.ELIGIBILITY_INVALID_VALUE);
                    if (targetType == EligibilityTargetType.HORSE && (v < 400 || v > 600)) {
                        throw new AppException(ErrorCode.ELIGIBILITY_HORSE_WEIGHT_OUT_OF_RANGE);
                    }
                    if (targetType == EligibilityTargetType.JOCKEY && (v < 45 || v > 65)) {
                        throw new AppException(ErrorCode.ELIGIBILITY_JOCKEY_WEIGHT_OUT_OF_RANGE);
                    }
                }
                case WIN_RATE -> {
                    double v = Double.parseDouble(conditionValue);
                    if (v < 0 || v > 100) throw new AppException(ErrorCode.ELIGIBILITY_INVALID_VALUE);
                }
                case BREED -> HorseBreed.valueOf(conditionValue.toUpperCase());
                case JOCKEY_TIER -> JockeyTier.valueOf(conditionValue.toUpperCase());
            }
        } catch (IllegalArgumentException e) {
            throw new AppException(ErrorCode.ELIGIBILITY_INVALID_VALUE);
        }
    }
}

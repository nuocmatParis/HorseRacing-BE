package com.swp391.horseracing.service.impl;

import com.swp391.horseracing.dto.tournament.request.CreateEligibilityRequest;
import com.swp391.horseracing.dto.tournament.request.UpdateEligibilityRequest;
import com.swp391.horseracing.dto.tournament.response.TournamentEligibilityResponse;
import com.swp391.horseracing.entity.Tournament;
import com.swp391.horseracing.entity.TournamentEligibility;
import com.swp391.horseracing.enums.EligibilityCondition;
import com.swp391.horseracing.enums.EligibilityTargetType;
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

        if (tournamentEligibilityRepository.existsByTournament_TournamentIdAndTargetTypeAndConditionName(
                tournamentId, request.getTargetType(), request.getConditionName())) {
            throw new AppException(ErrorCode.ELIGIBILITY_CONDITION_EXISTS);
        }

        validateSupportedCondition(request.getTargetType(), request.getConditionName());
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

        EligibilityCondition updatedCondition = request.getConditionName() != null
                ? request.getConditionName() : eligibility.getConditionName();
        EligibilityTargetType updatedTarget = request.getTargetType() != null
                ? request.getTargetType() : eligibility.getTargetType();
        if (tournamentEligibilityRepository
                .existsByTournament_TournamentIdAndTargetTypeAndConditionNameAndEligibilityIdNot(
                        tournament.getTournamentId(), updatedTarget,
                        updatedCondition, eligibility.getEligibilityId())) {
            throw new AppException(ErrorCode.ELIGIBILITY_CONDITION_EXISTS);
        }

        validateSupportedCondition(updatedTarget, updatedCondition);
        String updatedValue = request.getConditionValue() != null
                ? request.getConditionValue() : eligibility.getConditionValue();
        validateConditionValue(updatedCondition, updatedTarget, updatedValue);

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
        List<TournamentEligibility> rules =
                tournamentEligibilityRepository.findByTournament_TournamentId(tournamentId);
        List<TournamentEligibilityResponse> responses = new java.util.ArrayList<>();
        for (TournamentEligibility rule : rules) {
            if (isSupportedCondition(rule.getTargetType(), rule.getConditionName())) {
                responses.add(tournamentEligibilityMapper.toEligibilityResponse(rule));
            }
        }
        return responses;
    }

    private void validateSupportedCondition(
            EligibilityTargetType targetType, EligibilityCondition conditionName) {
        if (!isSupportedCondition(targetType, conditionName)) {
            throw new AppException(ErrorCode.ELIGIBILITY_INVALID_VALUE);
        }
    }

    private boolean isSupportedCondition(
            EligibilityTargetType targetType, EligibilityCondition conditionName) {
        if (targetType == EligibilityTargetType.HORSE) {
            return conditionName == EligibilityCondition.AGE
                    || conditionName == EligibilityCondition.WEIGHT
                    || conditionName == EligibilityCondition.WIN_RATE;
        }
        if (targetType == EligibilityTargetType.JOCKEY) {
            return conditionName == EligibilityCondition.WEIGHT
                    || conditionName == EligibilityCondition.EXPERIENCE_YEARS;
        }
        return false;
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
                case BREED, JOCKEY_TIER -> throw new AppException(ErrorCode.ELIGIBILITY_INVALID_VALUE);
            }
        } catch (IllegalArgumentException e) {
            throw new AppException(ErrorCode.ELIGIBILITY_INVALID_VALUE);
        }
    }
}

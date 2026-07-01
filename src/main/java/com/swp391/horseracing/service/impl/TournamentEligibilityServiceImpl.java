package com.swp391.horseracing.service.impl;

import com.swp391.horseracing.dto.tournament.request.CreateEligibilityRequest;
import com.swp391.horseracing.dto.tournament.request.UpdateEligibilityRequest;
import com.swp391.horseracing.dto.tournament.response.TournamentEligibilityResponse;
import com.swp391.horseracing.entity.Tournament;
import com.swp391.horseracing.entity.TournamentEligibility;
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
}

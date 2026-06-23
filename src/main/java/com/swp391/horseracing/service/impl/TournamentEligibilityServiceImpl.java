package com.swp391.horseracing.service.impl;

import com.swp391.horseracing.dto.tournament.request.CreateEligibilityRequest;
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

        TournamentEligibility eligibility = tournamentEligibilityMapper.toEligibility(request);
        eligibility.setTournament(tournament);

        return tournamentEligibilityMapper.toEligibilityResponse(tournamentEligibilityRepository.save(eligibility));
    }

    @Override
    public List<TournamentEligibilityResponse> getByTournament(UUID tournamentId){
        tournamentRepository.findById(tournamentId)
                .orElseThrow(() -> new AppException(ErrorCode.TOURNAMENT_NOT_FOUND));
        return tournamentEligibilityRepository.findByTournament_TournamentId(tournamentId)
                .stream().map(tournamentEligibilityMapper :: toEligibilityResponse).collect(Collectors.toList());
    }
}

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

import java.util.UUID;

@Service
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequiredArgsConstructor
public class TournamentEligibilityServiceImpl implements TournamentEligibilityService {

    TournamentEligibilityRepository repository;
    TournamentRepository tournamentRepository;
    TournamentEligibilityMapper mapper;

    @Override
    @Transactional
    public TournamentEligibilityResponse create(UUID tournamentId, CreateEligibilityRequest request) {
        Tournament tournament = tournamentRepository.findById(tournamentId)
                .orElseThrow(() -> new AppException(ErrorCode.TOURNAMENT_NOT_FOUND));

        if (tournament.getStatus() != TournamentStatus.DRAFT) {
            throw new AppException(ErrorCode.TOURNAMENT_NOT_IN_DRAFT);
        }

        TournamentEligibility eligibility = mapper.toEligibility(request);
        eligibility.setTournament(tournament);

        return mapper.toEligibilityResponse(repository.save(eligibility));
    }
}

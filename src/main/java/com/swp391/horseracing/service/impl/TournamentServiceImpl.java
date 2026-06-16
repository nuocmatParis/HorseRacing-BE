package com.swp391.horseracing.service.impl;

import com.swp391.horseracing.dto.tournament.request.CreateTournamentRequest;
import com.swp391.horseracing.dto.tournament.response.TournamentResponse;
import com.swp391.horseracing.entity.Tournament;
import com.swp391.horseracing.entity.User;
import com.swp391.horseracing.enums.TournamentPhase;
import com.swp391.horseracing.enums.TournamentStatus;
import com.swp391.horseracing.exception.AppException;
import com.swp391.horseracing.exception.ErrorCode;
import com.swp391.horseracing.mapper.TournamentMapper;
import com.swp391.horseracing.repository.TournamentRepository;
import com.swp391.horseracing.repository.UserRepository;
import com.swp391.horseracing.service.TournamentService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequiredArgsConstructor
public class TournamentServiceImpl implements TournamentService {

    TournamentRepository tournamentRepository;
    UserRepository userRepository;
    TournamentMapper tournamentMapper;

    @Override
    @Transactional
    public TournamentResponse create(CreateTournamentRequest request) {
        if (request.getEndDate().isBefore(request.getStartDate())) {
            throw new AppException(ErrorCode.INVALID_TOURNAMENT_DATES);
        }

        User currentUser = getCurrentUser();

        Tournament tournament = tournamentMapper.toTournament(request);
        tournament.setCreatedBy(currentUser);
        tournament.setStatus(TournamentStatus.DRAFT);
        tournament.setPhase(TournamentPhase.REGISTRATION);

        return tournamentMapper.toTournamentResponse(tournamentRepository.save(tournament));
    }

    @Override
    public List<TournamentResponse> getAll() {
        return tournamentRepository.findAllByOrderByCreatedAtDesc()
                .stream()
                .map(tournamentMapper::toTournamentResponse)
                .collect(Collectors.toList());
    }

    @Override
    public TournamentResponse getById(UUID id) {
        Tournament tournament = tournamentRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.TOURNAMENT_NOT_FOUND));
        return tournamentMapper.toTournamentResponse(tournament);
    }

    private User getCurrentUser() {
        var context = SecurityContextHolder.getContext();
        String username = context.getAuthentication().getName();
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
    }
}

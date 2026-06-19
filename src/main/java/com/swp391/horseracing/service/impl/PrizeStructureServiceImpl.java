package com.swp391.horseracing.service.impl;

import com.swp391.horseracing.dto.tournament.request.CreatePrizeStructureRequest;
import com.swp391.horseracing.dto.tournament.response.PrizeStructureResponse;
import com.swp391.horseracing.entity.PrizeStructure;
import com.swp391.horseracing.entity.Tournament;
import com.swp391.horseracing.enums.TournamentStatus;
import com.swp391.horseracing.exception.AppException;
import com.swp391.horseracing.exception.ErrorCode;
import com.swp391.horseracing.mapper.PrizeStructureMapper;
import com.swp391.horseracing.repository.PrizeStructureRepository;
import com.swp391.horseracing.repository.TournamentRepository;
import com.swp391.horseracing.service.PrizeStructureService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

@Service
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequiredArgsConstructor
public class PrizeStructureServiceImpl implements PrizeStructureService {

    PrizeStructureRepository prizeStructureRepository;
    TournamentRepository tournamentRepository;
    PrizeStructureMapper prizeStructureMapper;

    @Override
    @Transactional
    public PrizeStructureResponse create(UUID tournamentId, CreatePrizeStructureRequest request) {
        if (request.getPercentage() == null && request.getFixedAmount() == null) {
            throw new AppException(ErrorCode.PRIZE_MISSING_VALUE);
        }

        if (request.getPercentage() != null && request.getPercentage() > 100) {
            throw new AppException(ErrorCode.PRIZE_PERCENTAGE_EXCEEDS_100);
        }

        Tournament tournament = tournamentRepository.findById(tournamentId)
                .orElseThrow(() -> new AppException(ErrorCode.TOURNAMENT_NOT_FOUND));

        if (tournament.getStatus() != TournamentStatus.DRAFT) {
            throw new AppException(ErrorCode.TOURNAMENT_NOT_IN_DRAFT);
        }

        if (prizeStructureRepository.existsByTournament_TournamentIdAndRank(tournamentId, request.getRank())) {
            throw new AppException(ErrorCode.DUPLICATE_PRIZE_RANK);
        }

        if (request.getPercentage() != null) {
            BigDecimal existingTotal = prizeStructureRepository
                    .findByTournament_TournamentId(tournamentId)
                    .stream()
                    .filter(ps -> ps.getPercentage() != null)
                    .map(ps -> BigDecimal.valueOf(ps.getPercentage()))
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal newTotal = existingTotal.add(BigDecimal.valueOf(request.getPercentage()));
            if (newTotal.compareTo(BigDecimal.valueOf(100)) > 0) {
                throw new AppException(ErrorCode.PRIZE_PERCENTAGE_EXCEEDS_100);
            }
        }

        PrizeStructure prizeStructure = prizeStructureMapper.toPrizeStructure(request);
        prizeStructure.setTournament(tournament);

        return prizeStructureMapper.toPrizeStructureResponse(prizeStructureRepository.save(prizeStructure));
    }
}

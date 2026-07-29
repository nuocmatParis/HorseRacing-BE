package com.swp391.horseracing.service.impl;

import com.swp391.horseracing.dto.tournament.request.CreatePrizeStructureRequest;
import com.swp391.horseracing.dto.tournament.request.UpdatePrizeStructureRequest;
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
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

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

        int maxAllowedRank = tournament.getMaxApprovedHorses() != null && tournament.getMaxApprovedHorses() > 0
                ? tournament.getMaxApprovedHorses()
                : tournament.getMaxEntriesPerRace();
        if (request.getRank() > maxAllowedRank) {
            throw new AppException(ErrorCode.PRIZE_RANK_EXCEEDS_HORSE_COUNT);
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
            validateRankHierarchyForCreationOrUpdate(tournamentId, null, request.getRank(), request.getPercentage());
        }

        PrizeStructure prizeStructure = prizeStructureMapper.toPrizeStructure(request);
        prizeStructure.setTournament(tournament);

        return prizeStructureMapper.toPrizeStructureResponse(prizeStructureRepository.save(prizeStructure));
    }

    @Override
    @Transactional
    public PrizeStructureResponse update(UUID prizeStructureId, UpdatePrizeStructureRequest request) {
        PrizeStructure prizeStructure = prizeStructureRepository.findById(prizeStructureId)
                .orElseThrow(() -> new AppException(ErrorCode.PRIZE_STRUCTURE_NOT_FOUND));

        Tournament tournament = prizeStructure.getTournament();
        if (tournament.getStatus() != TournamentStatus.DRAFT) {
            throw new AppException(ErrorCode.TOURNAMENT_NOT_IN_DRAFT);
        }

        if (request.getRank() != null && request.getRank() != prizeStructure.getRank()
                && prizeStructureRepository.existsByTournament_TournamentIdAndRank(tournament.getTournamentId(), request.getRank())) {
            throw new AppException(ErrorCode.DUPLICATE_PRIZE_RANK);
        }

        if (request.getPercentage() != null && request.getPercentage() > 100) {
            throw new AppException(ErrorCode.PRIZE_PERCENTAGE_EXCEEDS_100);
        }

        int targetRank = request.getRank() != null ? request.getRank() : prizeStructure.getRank();
        Float targetPercentage = request.getPercentage() != null ? request.getPercentage() : prizeStructure.getPercentage();

        int maxAllowedRank = tournament.getMaxApprovedHorses() != null && tournament.getMaxApprovedHorses() > 0
                ? tournament.getMaxApprovedHorses()
                : tournament.getMaxEntriesPerRace();
        if (targetRank > maxAllowedRank) {
            throw new AppException(ErrorCode.PRIZE_RANK_EXCEEDS_HORSE_COUNT);
        }

        if (targetPercentage != null) {
            BigDecimal existingTotal = prizeStructureRepository
                    .findByTournament_TournamentId(tournament.getTournamentId())
                    .stream()
                    .filter(ps -> !ps.getPrizeStructureId().equals(prizeStructureId) && ps.getPercentage() != null)
                    .map(ps -> BigDecimal.valueOf(ps.getPercentage()))
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal newTotal = existingTotal.add(BigDecimal.valueOf(targetPercentage));
            if (newTotal.compareTo(BigDecimal.valueOf(100)) > 0) {
                throw new AppException(ErrorCode.PRIZE_PERCENTAGE_EXCEEDS_100);
            }
            validateRankHierarchyForCreationOrUpdate(tournament.getTournamentId(), prizeStructureId, targetRank, targetPercentage);
        }

        prizeStructureMapper.updatePrizeStructure(request, prizeStructure);
        return prizeStructureMapper.toPrizeStructureResponse(prizeStructureRepository.save(prizeStructure));
    }

    @Override
    @Transactional
    public void delete(UUID prizeStructureId) {
        PrizeStructure prizeStructure = prizeStructureRepository.findById(prizeStructureId)
                .orElseThrow(() -> new AppException(ErrorCode.PRIZE_STRUCTURE_NOT_FOUND));

        if (prizeStructure.getTournament().getStatus() != TournamentStatus.DRAFT) {
            throw new AppException(ErrorCode.TOURNAMENT_NOT_IN_DRAFT);
        }

        prizeStructureRepository.delete(prizeStructure);
    }

    @Override
    public List<PrizeStructureResponse> getByTournament(UUID tournamentId) {
        tournamentRepository.findById(tournamentId)
                .orElseThrow(() -> new AppException(ErrorCode.TOURNAMENT_NOT_FOUND));
        return prizeStructureRepository.findByTournament_TournamentId(tournamentId)
                .stream().map(prizeStructureMapper::toPrizeStructureResponse).collect(Collectors.toList());
    }

    @Override
    public void validatePrizeStructuresForTournament(UUID tournamentId) {
        List<PrizeStructure> prizeStructures = prizeStructureRepository.findByTournament_TournamentId(tournamentId);

        if (prizeStructures.isEmpty()) {
            throw new AppException(ErrorCode.TOURNAMENT_MISSING_PRIZE);
        }

        List<PrizeStructure> activePercentagePrizes = prizeStructures.stream()
                .filter(ps -> ps.getPercentage() != null)
                .sorted(Comparator.comparingInt(PrizeStructure::getRank))
                .collect(Collectors.toList());

        if (!activePercentagePrizes.isEmpty()) {
            BigDecimal totalPercentage = activePercentagePrizes.stream()
                    .map(ps -> BigDecimal.valueOf(ps.getPercentage()))
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            if (totalPercentage.compareTo(BigDecimal.valueOf(100)) != 0) {
                throw new AppException(ErrorCode.INVALID_TOTAL_PRIZE_PERCENTAGE);
            }
        }
    }

    private void validateRankHierarchyForCreationOrUpdate(UUID tournamentId, UUID currentPrizeStructureId, int targetRank, Float targetPercentage) {
        if (targetPercentage == null) {
            return;
        }

        List<PrizeStructure> existing = prizeStructureRepository.findByTournament_TournamentId(tournamentId);

        List<PrizeRankEntry> simulatedList = new ArrayList<>();
        for (PrizeStructure ps : existing) {
            if (currentPrizeStructureId != null && ps.getPrizeStructureId().equals(currentPrizeStructureId)) {
                continue;
            }
            if (ps.getPercentage() != null) {
                simulatedList.add(new PrizeRankEntry(ps.getRank(), ps.getPercentage()));
            }
        }
        simulatedList.add(new PrizeRankEntry(targetRank, targetPercentage));

        simulatedList.sort(Comparator.comparingInt(PrizeRankEntry::rank));

        for (int i = 0; i < simulatedList.size() - 1; i++) {
            PrizeRankEntry current = simulatedList.get(i);
            PrizeRankEntry next = simulatedList.get(i + 1);

            if (current.percentage() <= next.percentage()) {
                throw new AppException(ErrorCode.INVALID_PRIZE_RANK_HIERARCHY);
            }
        }
    }

    private record PrizeRankEntry(int rank, float percentage) {}

}

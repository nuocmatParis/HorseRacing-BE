package com.swp391.horseracing.service.impl;

import com.swp391.horseracing.dto.bracket.BracketPreviewResponse;
import com.swp391.horseracing.dto.bracket.BracketStructure;
import com.swp391.horseracing.dto.bracket.RoundPlan;
import com.swp391.horseracing.entity.*;
import com.swp391.horseracing.enums.PredictionType;
import com.swp391.horseracing.enums.RoundStatus;
import com.swp391.horseracing.enums.RoundTransitionStatus;
import com.swp391.horseracing.enums.TournamentPhase;
import com.swp391.horseracing.enums.TournamentStatus;
import com.swp391.horseracing.exception.AppException;
import com.swp391.horseracing.exception.ErrorCode;
import com.swp391.horseracing.policy.BracketCalculator;
import com.swp391.horseracing.policy.RaceScheduleCalculator;
import com.swp391.horseracing.policy.TournamentTimelinePolicy;
import com.swp391.horseracing.repository.*;
import com.swp391.horseracing.service.BracketService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequiredArgsConstructor
public class BracketServiceImpl implements BracketService {

    TournamentRepository tournamentRepository;
    RoundRepository roundRepository;
    RaceRepository raceRepository;
    UserRepository userRepository;
    TournamentPhaseConfigRepository tournamentPhaseConfigRepository;
    PhaseTimingConfigRepository phaseTimingConfigRepository;
    @Override
    public BracketPreviewResponse preview(UUID tournamentId, int actualEntries) {
        Tournament tournament = tournamentRepository.findById(tournamentId)
                .orElseThrow(() -> new AppException(ErrorCode.TOURNAMENT_NOT_FOUND));

        if (tournament.getStatus() != TournamentStatus.DRAFT) {
            throw new AppException(ErrorCode.TOURNAMENT_NOT_IN_DRAFT);
        }

        TournamentTimelinePolicy.validateMaxApprovedEntries(actualEntries);

        int maxEntriesPerRace = tournament.getMaxEntriesPerRace();
        int qualifiersPerRace = tournament.getQualifiersPerRace();

        BracketStructure bracket = BracketCalculator.calculate(
                actualEntries, maxEntriesPerRace, qualifiersPerRace);

        int preRaceBufferDays = phaseTimingConfigRepository
                .findByPhaseNameAndCapacity("PRE_RACE_BUFFER", actualEntries)
                .map(PhaseTimingConfig::getDurationDays)
                .orElse(0);

        List<RoundPlan> scheduledRounds = RaceScheduleCalculator.scheduleRounds(
                bracket.getRounds(), tournament, preRaceBufferDays);

        bracket.setRounds(scheduledRounds);

        Map<String, Integer> phaseConfigs = tournamentPhaseConfigRepository
                .findByTournamentTournamentId(tournamentId)
                .stream()
                .collect(Collectors.toMap(
                        TournamentPhaseConfig::getPhaseName,
                        TournamentPhaseConfig::getDurationDays
                ));

        return BracketPreviewResponse.builder()
                .bracket(bracket)
                .phaseConfigs(phaseConfigs)
                .build();
    }

    @Override
    @Transactional
    public void confirm(UUID tournamentId) {
        Tournament tournament = tournamentRepository.findById(tournamentId)
                .orElseThrow(() -> new AppException(ErrorCode.TOURNAMENT_NOT_FOUND));

        if (tournament.getStatus() != TournamentStatus.DRAFT) {
            throw new AppException(ErrorCode.TOURNAMENT_NOT_IN_DRAFT);
        }

        int actualEntries = tournament.getMaxApprovedEntries();
        int maxEntriesPerRace = tournament.getMaxEntriesPerRace();
        int qualifiersPerRace = tournament.getQualifiersPerRace();

        BracketStructure bracket = BracketCalculator.calculate(
                actualEntries, maxEntriesPerRace, qualifiersPerRace);

        int preRaceBufferDays = phaseTimingConfigRepository
                .findByPhaseNameAndCapacity("PRE_RACE_BUFFER", actualEntries)
                .map(PhaseTimingConfig::getDurationDays)
                .orElse(0);

        List<RoundPlan> scheduledRounds = RaceScheduleCalculator.scheduleRounds(
                bracket.getRounds(), tournament, preRaceBufferDays);

        deleteExistingRounds(tournamentId);
        User currentUser = getCurrentUser();
        List<Round> createdRounds = new ArrayList<>();

        for (RoundPlan roundPlan : scheduledRounds) {
            Round round = Round.builder()
                    .roundName(roundPlan.getRoundName())
                    .sequenceOrder(roundPlan.getSequenceOrder())
                    .isFinal(roundPlan.isFinal())
                    .predictionType(PredictionType.TOP3)
                    .advancementRule(roundPlan.isFinal()
                            ? "Chung kết"
                            : "Top " + tournament.getQualifiersPerRace() + " mỗi race đi tiếp")
                    .startDate(roundPlan.getEstimatedStartDate())
                    .endDate(roundPlan.getEstimatedEndDate())
                    .description(roundPlan.getRoundName()
                            + " - " + roundPlan.getRaceCount() + " race")
                    .maxRaces(roundPlan.getRaceCount())
                    .maxEntries(maxEntriesPerRace)
                    .minEntries(tournament.getMinEntriesPerRace())
                    .qualifiersPerRace(roundPlan.getQualifiersPerRace())
                    .status(RoundStatus.SCHEDULING)
                    .transitionStatus(RoundTransitionStatus.NOT_READY)
                    .tournament(tournament)
                    .createdBy(currentUser)
                    .createdAt(LocalDateTime.now())
                    .build();

            Round savedRound = roundRepository.save(round);

            for (int seq = 1; seq <= roundPlan.getRaces().size(); seq++) {
                var racePlan = roundPlan.getRaces().get(seq - 1);
                Race race = Race.builder()
                        .name(roundPlan.getRoundName() + " - Race " + seq)
                        .startTime(racePlan.getStartTime())
                        .endTime(racePlan.getEndTime())
                        .trackCondition("Tốt")
                        .distance(tournament.getDistance())
                        .sequenceOrder(seq)
                        .status(RoundStatus.SCHEDULING)
                        .round(savedRound)
                        .createdBy(currentUser)
                        .build();
                raceRepository.save(race);
            }

            createdRounds.add(savedRound);
        }
    }

    @Override
    @Transactional
    public void recalculate(UUID tournamentId, int actualEntries) {
        Tournament tournament = tournamentRepository.findById(tournamentId)
                .orElseThrow(() -> new AppException(ErrorCode.TOURNAMENT_NOT_FOUND));

        if (tournament.getStatus() != TournamentStatus.DRAFT) {
            throw new AppException(ErrorCode.TOURNAMENT_NOT_IN_DRAFT);
        }

        TournamentTimelinePolicy.validateMaxApprovedEntries(actualEntries);
        deleteExistingRounds(tournamentId);
        tournament.setMaxApprovedEntries(actualEntries);
        tournament.setMaxApprovedHorses(actualEntries);
        tournamentRepository.save(tournament);

        confirm(tournamentId);
    }

    private void deleteExistingRounds(UUID tournamentId) {
        List<Round> existingRounds = roundRepository
                .findByTournament_TournamentIdOrderBySequenceOrderAsc(tournamentId);
        for (Round round : existingRounds) {
            List<Race> races = raceRepository.findByRound_RoundId(round.getRoundId());
            if (!races.isEmpty()) {
                raceRepository.deleteAll(races);
            }
        }
        if (!existingRounds.isEmpty()) {
            roundRepository.deleteAll(existingRounds);
        }
    }

    private User getCurrentUser() {
        var context = SecurityContextHolder.getContext();
        String username = context.getAuthentication().getName();
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
    }
}

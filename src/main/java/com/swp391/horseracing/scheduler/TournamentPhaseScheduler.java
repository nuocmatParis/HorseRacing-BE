package com.swp391.horseracing.scheduler;

import com.swp391.horseracing.entity.Race;
import com.swp391.horseracing.entity.Round;
import com.swp391.horseracing.entity.Tournament;
import com.swp391.horseracing.enums.RoundStatus;
import com.swp391.horseracing.enums.TournamentPhase;
import com.swp391.horseracing.enums.TournamentStatus;
import com.swp391.horseracing.repository.RaceRepository;
import com.swp391.horseracing.repository.RoundRepository;
import com.swp391.horseracing.repository.TournamentRepository;
import com.swp391.horseracing.repository.RaceReportRepository;
import com.swp391.horseracing.enums.ReportStatus;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class TournamentPhaseScheduler {

    TournamentRepository tournamentRepository;
    RoundRepository roundRepository;
    RaceRepository raceRepository;
    RaceReportRepository raceReportRepository;

    @Transactional
    @Scheduled(fixedRate = 60_000)
    public void autoTransitionPhases() {
        autoStartReview();
        autoFinishRacing();
    }

    private void autoStartReview() {
        List<Tournament> tournaments = tournamentRepository.findByPhase(TournamentPhase.REGISTRATION_OPEN);
        LocalDateTime now = LocalDateTime.now();

        for (Tournament tournament : tournaments) {
            if (now.isAfter(tournament.getRegistrationCloseAt())) {
                tournament.setPhase(TournamentPhase.REGISTRATION_REVIEW);
                tournament.setStatus(TournamentStatus.OPEN);
                tournamentRepository.save(tournament);
                log.info("Tournament {} auto-transitioned: REGISTRATION_OPEN → REGISTRATION_REVIEW",
                        tournament.getTournamentId());
            }
        }
    }

    private void autoFinishRacing() {
        List<Tournament> tournaments = tournamentRepository.findByPhase(TournamentPhase.RACING);

        for (Tournament tournament : tournaments) {
            List<Round> rounds = roundRepository
                    .findByTournament_TournamentIdOrderBySequenceOrderAsc(tournament.getTournamentId());

            if (rounds.isEmpty()) continue;

            Round currentRound = findCurrentRound(tournament, rounds);

            // Intermediate-round advancement belongs to RaceReportService after every
            // report is published and all Top 4 qualifiers are validated atomically.
            if (!currentRound.isFinal()) {
                continue;
            }

            boolean allFinished = true;
            List<Race> finalRaces = raceRepository.findByRound_RoundId(currentRound.getRoundId());
            if (finalRaces.isEmpty()) {
                allFinished = false;
            }
            for (Race race : finalRaces) {
                if (race.getStatus() != RoundStatus.COMPLETED
                        || raceReportRepository.findByRace_RaceId(race.getRaceId())
                        .filter(report -> report.getStatus() == ReportStatus.Published)
                        .isEmpty()) {
                    allFinished = false;
                    break;
                }
            }

            if (!allFinished) continue;

            int currentIndex = -1;
            for (int i = 0; i < rounds.size(); i++) {
                if (rounds.get(i).getRoundId().equals(currentRound.getRoundId())) {
                    currentIndex = i;
                    break;
                }
            }
            int nextIndex = currentIndex + 1;

            if (nextIndex < rounds.size()) {
                Round nextRound = rounds.get(nextIndex);
                tournament.setCurrentRoundName(nextRound.getRoundName());
                tournamentRepository.save(tournament);
                log.info("Tournament {} round complete: '{}' → '{}'",
                        tournament.getTournamentId(),
                        currentRound.getRoundName(),
                        nextRound.getRoundName());
            } else {
                tournament.setPhase(TournamentPhase.RESULT_PENDING);
                tournament.setStatus(TournamentStatus.ONGOING);
                tournamentRepository.save(tournament);
                log.info("Tournament {} auto-transitioned: RACING → RESULT_PENDING",
                        tournament.getTournamentId());
            }
        }
    }

    private Round findCurrentRound(Tournament tournament, List<Round> rounds) {
        String currentRoundName = tournament.getCurrentRoundName();
        if (currentRoundName != null) {
            for (Round round : rounds) {
                if (round.getRoundName().equals(currentRoundName)) {
                    return round;
                }
            }
        }
        return rounds.get(0);
    }
}

package com.swp391.horseracing.scheduler;

import com.swp391.horseracing.entity.Round;
import com.swp391.horseracing.entity.Tournament;
import com.swp391.horseracing.enums.TournamentPhase;
import com.swp391.horseracing.enums.TournamentStatus;
import com.swp391.horseracing.repository.RaceRepository;
import com.swp391.horseracing.repository.RoundRepository;
import com.swp391.horseracing.repository.TournamentRepository;
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

            boolean allFinished = rounds.stream()
                    .flatMap(round -> raceRepository.findByRound_RoundId(round.getRoundId()).stream())
                    .allMatch(race -> race.getFinishedAt() != null);

            if (allFinished && !rounds.isEmpty()) {
                tournament.setPhase(TournamentPhase.RESULT_PENDING);
                tournament.setStatus(TournamentStatus.ONGOING);
                tournamentRepository.save(tournament);
                log.info("Tournament {} auto-transitioned: RACING → RESULT_PENDING",
                        tournament.getTournamentId());
            }
        }
    }
}

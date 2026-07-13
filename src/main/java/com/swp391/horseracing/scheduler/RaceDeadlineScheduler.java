package com.swp391.horseracing.scheduler;

import com.swp391.horseracing.entity.Race;
import com.swp391.horseracing.enums.RoundStatus;
import com.swp391.horseracing.repository.RaceRepository;
import com.swp391.horseracing.service.RaceService;
import com.swp391.horseracing.service.PredictionService;
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
public class RaceDeadlineScheduler {

    RaceRepository raceRepository;
    RaceService raceService;
    PredictionService predictionService;

    @Scheduled(fixedRate = 30000) // Runs every 30 seconds
    @Transactional(readOnly = true)
    public void checkDeadlineAndFinalize() {
        LocalDateTime now = LocalDateTime.now();
        List<Race> races = raceRepository.findAll();
        for (Race race : races) {
            if (race.getStatus() == RoundStatus.SCHEDULED) {
                // 1. Entry finalization
                if (race.getInspectionFinalizedAt() == null) {
                    int closeMin = race.getRound().getTournament().getInspectionCloseMinutesBefore();
                    LocalDateTime closeTime = race.getStartTime().minusMinutes(closeMin);
                    if (now.isAfter(closeTime) || now.isEqual(closeTime)) {
                        try {
                            log.info("Auto-finalizing entries for race: {} (ID: {})", race.getName(), race.getRaceId());
                            raceService.finalizeRaceEntries(race.getRaceId());
                        } catch (Exception e) {
                            log.error("Failed to auto-finalize race entry for race: " + race.getRaceId(), e);
                        }
                    }
                }

                // 2. Voiding invalid predictions at close time
                if (now.isAfter(race.getPredictionCloseAt()) || now.isEqual(race.getPredictionCloseAt())) {
                    try {
                        predictionService.voidInvalidPredictionsForRace(race.getRaceId());
                    } catch (Exception e) {
                        log.error("Failed to void predictions for race: " + race.getRaceId(), e);
                    }
                }
            }
        }
    }
}

package com.swp391.horseracing.service;

import com.swp391.horseracing.dto.horse.RaceRatingPreviewResponse;
import com.swp391.horseracing.entity.Horse;
import com.swp391.horseracing.entity.JockeyHorseContract;
import com.swp391.horseracing.entity.Race;
import com.swp391.horseracing.entity.RaceEntry;
import com.swp391.horseracing.entity.RaceReport;
import com.swp391.horseracing.entity.RaceResult;
import com.swp391.horseracing.entity.Round;
import com.swp391.horseracing.entity.Tournament;
import com.swp391.horseracing.enums.RaceResultStatus;
import com.swp391.horseracing.enums.ReportStatus;
import com.swp391.horseracing.exception.AppException;
import com.swp391.horseracing.exception.ErrorCode;
import com.swp391.horseracing.repository.HorseOwnerRepository;
import com.swp391.horseracing.repository.HorseRatingHistoryRepository;
import com.swp391.horseracing.repository.HorseRepository;
import com.swp391.horseracing.repository.RaceReportRepository;
import com.swp391.horseracing.repository.RaceRepository;
import com.swp391.horseracing.repository.RaceResultRepository;
import com.swp391.horseracing.repository.RoundRepository;
import com.swp391.horseracing.service.impl.HorseRatingServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ManualHorseRatingServiceTest {

    @Mock RaceRepository raceRepository;
    @Mock RaceResultRepository raceResultRepository;
    @Mock HorseRepository horseRepository;
    @Mock HorseRatingHistoryRepository ratingHistoryRepository;
    @Mock RaceReportRepository raceReportRepository;
    @Mock RoundRepository roundRepository;
    @Mock UserCurrentService userCurrentService;
    @Mock HorseOwnerRepository horseOwnerRepository;
    @InjectMocks HorseRatingServiceImpl service;

    Race ratingRace;

    @BeforeEach
    void configureRanges() {
        Tournament tournament = createRatingTournament();
        Round round = new Round();
        round.setTournament(tournament);
        ratingRace = new Race();
        ratingRace.setRound(round);
    }

    @Test
    void validatesManualRatingByOfficialResult() {
        service.validateRatingChange(ratingRace, RaceResultStatus.FINISHED, 1, 6);
        service.validateRatingChange(ratingRace, RaceResultStatus.FINISHED, 1, 12);
        service.validateRatingChange(ratingRace, RaceResultStatus.FINISHED, 5, 2);
        service.validateRatingChange(ratingRace, RaceResultStatus.FINISHED, 6, -8);
        service.validateRatingChange(
                ratingRace, RaceResultStatus.DISQUALIFIED, null, -8);
    }

    @Test
    void rejectsMissingOrOutOfRangeManualRating() {
        AppException missing = assertThrows(AppException.class,
                () -> service.validateRatingChange(
                        ratingRace, RaceResultStatus.FINISHED, 1, null));
        assertEquals(ErrorCode.HORSE_RATING_CHANGE_REQUIRED, missing.getErrorCode());

        AppException invalidWinner = assertThrows(AppException.class,
                () -> service.validateRatingChange(
                        ratingRace, RaceResultStatus.FINISHED, 1, 5));
        assertEquals(ErrorCode.HORSE_RATING_CHANGE_OUT_OF_RANGE, invalidWinner.getErrorCode());

        AppException invalidSixth = assertThrows(AppException.class,
                () -> service.validateRatingChange(
                        ratingRace, RaceResultStatus.FINISHED, 6, 1));
        assertEquals(ErrorCode.HORSE_RATING_CHANGE_OUT_OF_RANGE, invalidSixth.getErrorCode());
    }

    @Test
    void validatesAgainstTournamentRatingConfigInsteadOfGlobalDefaults() {
        Tournament tournament = ratingRace.getRound().getTournament();
        tournament.setRatingFirstMin(20);
        tournament.setRatingFirstMax(25);

        service.validateRatingChange(
                ratingRace, RaceResultStatus.FINISHED, 1, 20);

        AppException oldDefault = assertThrows(AppException.class,
                () -> service.validateRatingChange(
                        ratingRace, RaceResultStatus.FINISHED, 1, 12));
        assertEquals(ErrorCode.HORSE_RATING_CHANGE_OUT_OF_RANGE, oldDefault.getErrorCode());
    }

    @Test
    void signedPreviewUsesStoredRefereeRatingWithoutCalculatingBonus() {
        UUID raceId = UUID.randomUUID();
        Race race = new Race();
        race.setRaceId(raceId);
        race.setRound(ratingRace.getRound());
        RaceReport report = new RaceReport();
        report.setStatus(ReportStatus.SIGNED);

        Horse horse = new Horse();
        horse.setHorseId(UUID.randomUUID());
        horse.setName("Manual Rating Horse");
        horse.setCurrentRating(50);
        JockeyHorseContract contract = new JockeyHorseContract();
        contract.setHorse(horse);
        RaceEntry entry = new RaceEntry();
        entry.setContract(contract);
        RaceResult result = new RaceResult();
        result.setRace(race);
        result.setEntry(entry);
        result.setStatus(RaceResultStatus.FINISHED);
        result.setRank(1);
        result.setRatingChange(10);

        when(raceRepository.findById(raceId)).thenReturn(Optional.of(race));
        when(raceReportRepository.findByRace_RaceId(raceId)).thenReturn(Optional.of(report));
        when(raceResultRepository.findByRace_RaceIdOrderByRankAsc(raceId)).thenReturn(List.of(result));
        RaceRatingPreviewResponse response = service.previewForRace(raceId);

        assertEquals(1, response.getChanges().size());
        assertEquals(10, response.getChanges().get(0).getFinalChange());
        assertEquals(60, response.getChanges().get(0).getNewRating());
        assertEquals(6, response.getChanges().get(0).getMinimumAllowedChange());
        assertEquals(12, response.getChanges().get(0).getMaximumAllowedChange());
    }

    private Tournament createRatingTournament() {
        Tournament tournament = new Tournament();
        tournament.setRatingFirstMin(6);
        tournament.setRatingFirstMax(12);
        tournament.setRatingSecondMin(2);
        tournament.setRatingSecondMax(5);
        tournament.setRatingThirdMin(1);
        tournament.setRatingThirdMax(4);
        tournament.setRatingFourthFifthMin(0);
        tournament.setRatingFourthFifthMax(2);
        tournament.setRatingOtherMin(-8);
        tournament.setRatingOtherMax(0);
        tournament.setRatingDisqualifiedMin(-8);
        tournament.setRatingDisqualifiedMax(0);
        tournament.setRatingPolicyVersion(1);
        return tournament;
    }
}

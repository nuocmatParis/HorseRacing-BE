package com.swp391.horseracing.service;

import com.swp391.horseracing.config.HorseRatingProperties;
import com.swp391.horseracing.dto.horse.RaceRatingPreviewResponse;
import com.swp391.horseracing.entity.Horse;
import com.swp391.horseracing.entity.JockeyHorseContract;
import com.swp391.horseracing.entity.Race;
import com.swp391.horseracing.entity.RaceEntry;
import com.swp391.horseracing.entity.RaceReport;
import com.swp391.horseracing.entity.RaceResult;
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
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ManualHorseRatingServiceTest {

    @Mock HorseRatingProperties properties;
    @Mock RaceRepository raceRepository;
    @Mock RaceResultRepository raceResultRepository;
    @Mock HorseRepository horseRepository;
    @Mock HorseRatingHistoryRepository ratingHistoryRepository;
    @Mock RaceReportRepository raceReportRepository;
    @Mock RoundRepository roundRepository;
    @Mock UserCurrentService userCurrentService;
    @Mock HorseOwnerRepository horseOwnerRepository;
    @InjectMocks HorseRatingServiceImpl service;

    @BeforeEach
    void configureRanges() {
        lenient().when(properties.getFirstMin()).thenReturn(6);
        lenient().when(properties.getFirstMax()).thenReturn(12);
        lenient().when(properties.getSecondMin()).thenReturn(2);
        lenient().when(properties.getSecondMax()).thenReturn(5);
        lenient().when(properties.getThirdMin()).thenReturn(1);
        lenient().when(properties.getThirdMax()).thenReturn(4);
        lenient().when(properties.getFourthFifthMin()).thenReturn(0);
        lenient().when(properties.getFourthFifthMax()).thenReturn(2);
        lenient().when(properties.getOtherMin()).thenReturn(-8);
        lenient().when(properties.getOtherMax()).thenReturn(0);
        lenient().when(properties.getDnfMin()).thenReturn(-8);
        lenient().when(properties.getDnfMax()).thenReturn(0);
        lenient().when(properties.getDisqualifiedMin()).thenReturn(-8);
        lenient().when(properties.getDisqualifiedMax()).thenReturn(0);
    }

    @Test
    void validatesManualRatingByOfficialResult() {
        service.validateRatingChange(RaceResultStatus.FINISHED, 1, 6);
        service.validateRatingChange(RaceResultStatus.FINISHED, 1, 12);
        service.validateRatingChange(RaceResultStatus.FINISHED, 5, 2);
        service.validateRatingChange(RaceResultStatus.FINISHED, 6, -8);
        service.validateRatingChange(RaceResultStatus.DID_NOT_FINISH, null, 0);
        service.validateRatingChange(RaceResultStatus.DISQUALIFIED, null, -8);
    }

    @Test
    void rejectsMissingOrOutOfRangeManualRating() {
        AppException missing = assertThrows(AppException.class,
                () -> service.validateRatingChange(RaceResultStatus.FINISHED, 1, null));
        assertEquals(ErrorCode.HORSE_RATING_CHANGE_REQUIRED, missing.getErrorCode());

        AppException invalidWinner = assertThrows(AppException.class,
                () -> service.validateRatingChange(RaceResultStatus.FINISHED, 1, 5));
        assertEquals(ErrorCode.HORSE_RATING_CHANGE_OUT_OF_RANGE, invalidWinner.getErrorCode());

        AppException invalidSixth = assertThrows(AppException.class,
                () -> service.validateRatingChange(RaceResultStatus.FINISHED, 6, 1));
        assertEquals(ErrorCode.HORSE_RATING_CHANGE_OUT_OF_RANGE, invalidSixth.getErrorCode());
    }

    @Test
    void signedPreviewUsesStoredRefereeRatingWithoutCalculatingBonus() {
        UUID raceId = UUID.randomUUID();
        Race race = new Race();
        race.setRaceId(raceId);
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
        lenient().when(properties.getPolicyVersion()).thenReturn(1);

        RaceRatingPreviewResponse response = service.previewForRace(raceId);

        assertEquals(1, response.getChanges().size());
        assertEquals(10, response.getChanges().get(0).getFinalChange());
        assertEquals(60, response.getChanges().get(0).getNewRating());
        assertEquals(6, response.getChanges().get(0).getMinimumAllowedChange());
        assertEquals(12, response.getChanges().get(0).getMaximumAllowedChange());
    }
}

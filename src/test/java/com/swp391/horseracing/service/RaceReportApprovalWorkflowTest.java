package com.swp391.horseracing.service;

import com.swp391.horseracing.entity.*;
import com.swp391.horseracing.enums.*;
import com.swp391.horseracing.exception.AppException;
import com.swp391.horseracing.exception.ErrorCode;
import com.swp391.horseracing.mapper.RaceReportMapper;
import com.swp391.horseracing.mapper.RaceResultMapper;
import com.swp391.horseracing.repository.*;
import com.swp391.horseracing.service.impl.RaceReportServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RaceReportApprovalWorkflowTest {
    @Mock RaceReportRepository raceReportRepository;
    @Mock RaceRepository raceRepository;
    @Mock RaceResultRepository raceResultRepository;
    @Mock RaceEntryRepository raceEntryRepository;
    @Mock RaceRefereeRepository raceRefereeRepository;
    @Mock RefereeRepository refereeRepository;
    @Mock AppealRepository appealRepository;
    @Mock RaceReportMapper raceReportMapper;
    @Mock RaceResultMapper raceResultMapper;
    @Mock UserCurrentService userCurrentService;
    @Mock RoundRepository roundRepository;
    @Mock BusinessNotificationEventService notificationEventService;
    @Mock ScoringService scoringService;
    @Mock PrizeStructureRepository prizeStructureRepository;
    @Mock WalletRepository walletRepository;
    @Mock WalletTransactionRepository walletTransactionRepository;
    @Mock ContractService contractService;
    @Mock JockeyHorseContractRepository jockeyHorseContractRepository;
    @Mock HorseRatingService horseRatingService;
    @Mock TournamentRepository tournamentRepository;
    @InjectMocks RaceReportServiceImpl service;

    @Test
    void raceRefereeCannotSignReport() {
        Fixture fixture = fixture(false);
        mockCurrentReferee(fixture);
        when(raceRepository.findById(fixture.race.getRaceId())).thenReturn(Optional.of(fixture.race));

        AppException exception = assertThrows(AppException.class,
                () -> service.signReport(fixture.race.getRaceId()));

        assertEquals(ErrorCode.ACCESS_DENIED, exception.getErrorCode());
    }

    @Test
    void headRefereeCannotSignDraftReport() {
        Fixture fixture = fixture(true);
        mockCurrentReferee(fixture);
        mockValidResults(fixture);
        when(raceRepository.findById(fixture.race.getRaceId())).thenReturn(Optional.of(fixture.race));
        when(appealRepository.existsByEntry_Race_RaceIdAndStatus(
                fixture.race.getRaceId(), AppealStatus.Pending)).thenReturn(false);
        fixture.report.setStatus(ReportStatus.DRAFT);
        when(raceReportRepository.findForUpdateByRace_RaceId(fixture.race.getRaceId()))
                .thenReturn(Optional.of(fixture.report));

        AppException exception = assertThrows(AppException.class,
                () -> service.signReport(fixture.race.getRaceId()));

        assertEquals(ErrorCode.RACE_REPORT_NOT_SUBMITTED, exception.getErrorCode());
    }

    @Test
    void headRefereeCannotSignWhileAppealPending() {
        Fixture fixture = fixture(true);
        mockCurrentReferee(fixture);
        mockValidResults(fixture);
        when(raceRepository.findById(fixture.race.getRaceId())).thenReturn(Optional.of(fixture.race));
        when(appealRepository.existsByEntry_Race_RaceIdAndStatus(
                fixture.race.getRaceId(), AppealStatus.Pending)).thenReturn(true);

        AppException exception = assertThrows(AppException.class,
                () -> service.signReport(fixture.race.getRaceId()));

        assertEquals(ErrorCode.RACE_REPORT_PENDING_APPEAL, exception.getErrorCode());
    }

    @Test
    void adminCannotPublishUnsignedReport() {
        Fixture fixture = fixture(true);
        fixture.race.setStatus(RoundStatus.FINISHED);
        fixture.report.setStatus(ReportStatus.SUBMITTED_TO_HEAD);
        when(raceRepository.findById(fixture.race.getRaceId())).thenReturn(Optional.of(fixture.race));
        when(raceReportRepository.findForUpdateByRace_RaceId(fixture.race.getRaceId()))
                .thenReturn(Optional.of(fixture.report));

        AppException exception = assertThrows(AppException.class,
                () -> service.publishReport(fixture.race.getRaceId()));

        assertEquals(ErrorCode.RACE_REPORT_NOT_SIGNED, exception.getErrorCode());
    }

    @Test
    void publishingSignedReportRunsScoringAndRatingOnce() {
        Fixture fixture = fixture(true);
        fixture.race.setStatus(RoundStatus.FINISHED);
        fixture.round.setFinal(true);
        fixture.round.setRaces(List.of(fixture.race));
        fixture.report.setStatus(ReportStatus.SIGNED);
        when(raceRepository.findById(fixture.race.getRaceId())).thenReturn(Optional.of(fixture.race));
        when(raceReportRepository.findForUpdateByRace_RaceId(fixture.race.getRaceId()))
                .thenReturn(Optional.of(fixture.report));
        when(userCurrentService.getCurrentUser()).thenReturn(fixture.user);
        when(prizeStructureRepository.findByTournament_TournamentId(fixture.tournament.getTournamentId()))
                .thenReturn(new ArrayList<>());
        when(raceResultRepository.findForUpdateByRace_RaceId(fixture.race.getRaceId()))
                .thenReturn(new ArrayList<>());
        when(jockeyHorseContractRepository.findByTournament_TournamentIdAndStatusAndEscrowStatus(
                fixture.tournament.getTournamentId(), ContractStatus.APPROVED, EscrowStatus.PARTIALLY_RELEASED))
                .thenReturn(new ArrayList<>());
        when(roundRepository.findForUpdateByRoundId(fixture.round.getRoundId()))
                .thenReturn(Optional.of(fixture.round));
        when(raceRepository.findByRound_RoundIdOrderBySequenceOrderAsc(fixture.round.getRoundId()))
                .thenReturn(List.of(fixture.race));
        when(raceReportRepository.findByRace_RaceId(fixture.race.getRaceId()))
                .thenReturn(Optional.of(fixture.report));

        service.publishReport(fixture.race.getRaceId());

        verify(scoringService, times(1)).scoreRace(fixture.race.getRaceId());
        verify(horseRatingService, times(1)).calculateAndApplyForPublish(fixture.race.getRaceId());
        assertEquals(ReportStatus.PUBLISHED, fixture.report.getStatus());
    }

    private void mockCurrentReferee(Fixture fixture) {
        when(userCurrentService.getCurrentUser()).thenReturn(fixture.user);
        when(refereeRepository.findByUser_UserId(fixture.user.getUserId()))
                .thenReturn(Optional.of(fixture.referee));
    }

    private void mockValidResults(Fixture fixture) {
        when(raceEntryRepository.findByRace_RaceIdOrderByLaneNumberAsc(fixture.race.getRaceId()))
                .thenReturn(List.of(fixture.entry));
        when(raceResultRepository.findByRace_RaceId(fixture.race.getRaceId()))
                .thenReturn(List.of(fixture.result));
    }

    private Fixture fixture(boolean currentIsHead) {
        User user = new User();
        user.setUserId(UUID.randomUUID());
        user.setFullName("Referee");
        Referee referee = new Referee();
        referee.setRefereeId(UUID.randomUUID());
        referee.setUser(user);
        Referee otherHead = new Referee();
        otherHead.setRefereeId(UUID.randomUUID());

        Tournament tournament = new Tournament();
        tournament.setTournamentId(UUID.randomUUID());
        Round round = new Round();
        round.setRoundId(UUID.randomUUID());
        round.setTournament(tournament);
        round.setHeadReferee(currentIsHead ? referee : otherHead);
        Race race = new Race();
        race.setRaceId(UUID.randomUUID());
        race.setRound(round);
        race.setStatus(RoundStatus.ONGOING);

        Horse horse = new Horse();
        horse.setHorseId(UUID.randomUUID());
        Jockey jockey = new Jockey();
        jockey.setJockeyId(UUID.randomUUID());
        User jockeyUser = new User();
        jockeyUser.setFullName("Jockey");
        jockey.setUser(jockeyUser);
        JockeyHorseContract contract = new JockeyHorseContract();
        contract.setHorse(horse);
        contract.setJockey(jockey);
        RaceEntry entry = new RaceEntry();
        entry.setEntryId(UUID.randomUUID());
        entry.setRace(race);
        entry.setContract(contract);
        entry.setStatus(RaceEntryStatus.CONFIRMED);
        RaceResult result = new RaceResult();
        result.setEntry(entry);
        result.setRace(race);
        result.setStatus(RaceResultStatus.FINISHED);
        result.setRank(1);
        result.setFinishTime(60F);
        RaceReport report = new RaceReport();
        report.setRace(race);
        report.setReferee(referee);
        report.setStatus(ReportStatus.SUBMITTED_TO_HEAD);
        return new Fixture(user, referee, tournament, round, race, entry, result, report);
    }

    private record Fixture(User user, Referee referee, Tournament tournament, Round round,
                           Race race, RaceEntry entry, RaceResult result, RaceReport report) {
    }
}

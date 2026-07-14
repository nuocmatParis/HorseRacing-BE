package com.swp391.horseracing.service;

import com.swp391.horseracing.entity.Horse;
import com.swp391.horseracing.entity.Jockey;
import com.swp391.horseracing.entity.JockeyHorseContract;
import com.swp391.horseracing.entity.Race;
import com.swp391.horseracing.entity.RaceEntry;
import com.swp391.horseracing.entity.RaceReport;
import com.swp391.horseracing.entity.RaceResult;
import com.swp391.horseracing.entity.Round;
import com.swp391.horseracing.entity.Tournament;
import com.swp391.horseracing.entity.User;
import com.swp391.horseracing.enums.RaceResultStatus;
import com.swp391.horseracing.enums.ReportStatus;
import com.swp391.horseracing.enums.RoundStatus;
import com.swp391.horseracing.enums.RoundTransitionStatus;
import com.swp391.horseracing.enums.TournamentPhase;
import com.swp391.horseracing.mapper.RaceReportMapper;
import com.swp391.horseracing.mapper.RaceResultMapper;
import com.swp391.horseracing.repository.AppealRepository;
import com.swp391.horseracing.repository.JockeyHorseContractRepository;
import com.swp391.horseracing.repository.PrizeStructureRepository;
import com.swp391.horseracing.repository.RaceEntryRepository;
import com.swp391.horseracing.repository.RaceRefereeRepository;
import com.swp391.horseracing.repository.RaceReportRepository;
import com.swp391.horseracing.repository.RaceRepository;
import com.swp391.horseracing.repository.RaceResultRepository;
import com.swp391.horseracing.repository.RefereeRepository;
import com.swp391.horseracing.repository.RoundRepository;
import com.swp391.horseracing.repository.TournamentRepository;
import com.swp391.horseracing.repository.WalletRepository;
import com.swp391.horseracing.repository.WalletTransactionRepository;
import com.swp391.horseracing.service.impl.RaceReportServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RoundTransitionBusinessLogicTest {
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
    @Mock JockeyHorseContractRepository contractRepository;
    @Mock HorseRatingService horseRatingService;
    @Mock TournamentRepository tournamentRepository;

    @InjectMocks RaceReportServiceImpl raceReportService;

    @Test
    void allRacesWithTopFourAdvanceAtomically() throws Exception {
        TransitionFixture fixture = createFixture();
        mockCommonTransitionData(fixture);
        when(raceResultRepository.findByRace_RaceIdOrderByRankAsc(fixture.firstRace.getRaceId()))
                .thenReturn(createResults(fixture.firstRace, 4, 0));
        when(raceResultRepository.findByRace_RaceIdOrderByRankAsc(fixture.secondRace.getRaceId()))
                .thenReturn(createResults(fixture.secondRace, 4, 4));

        invokeAdvance(fixture.currentRound);

        verify(raceEntryRepository, times(8)).save(any(RaceEntry.class));
        assertNotNull(fixture.currentRound.getAdvancedAt());
        assertEquals(RoundTransitionStatus.COMPLETED, fixture.currentRound.getTransitionStatus());
        assertEquals(TournamentPhase.SCHEDULING, fixture.tournament.getPhase());
        assertEquals(fixture.nextRound.getRoundName(), fixture.tournament.getCurrentRoundName());
        verify(notificationEventService, never()).roundTransitionBlocked(any(Round.class));
    }

    @Test
    void oneRaceMissingTopFourCreatesNoNextRoundEntry() throws Exception {
        TransitionFixture fixture = createFixture();
        mockCommonTransitionData(fixture);
        when(raceResultRepository.findByRace_RaceIdOrderByRankAsc(fixture.firstRace.getRaceId()))
                .thenReturn(createResults(fixture.firstRace, 4, 0));
        List<RaceResult> secondRaceResults = createResults(fixture.secondRace, 3, 4);
        RaceResult didNotFinish = createResults(fixture.secondRace, 1, 7).get(0);
        didNotFinish.setRank(null);
        didNotFinish.setStatus(RaceResultStatus.DID_NOT_FINISH);
        secondRaceResults.add(didNotFinish);
        when(raceResultRepository.findByRace_RaceIdOrderByRankAsc(fixture.secondRace.getRaceId()))
                .thenReturn(secondRaceResults);

        invokeAdvance(fixture.currentRound);

        verify(raceEntryRepository, never()).save(any(RaceEntry.class));
        assertEquals(RoundTransitionStatus.BLOCKED_NOT_ENOUGH_QUALIFIERS,
                fixture.currentRound.getTransitionStatus());
        verify(notificationEventService).roundTransitionBlocked(fixture.currentRound);
    }

    @Test
    void completedTransitionIsIdempotent() throws Exception {
        TransitionFixture fixture = createFixture();
        fixture.currentRound.setAdvancedAt(java.time.LocalDateTime.now());
        fixture.currentRound.setTransitionStatus(RoundTransitionStatus.COMPLETED);
        when(roundRepository.findForUpdateByRoundId(fixture.currentRound.getRoundId()))
                .thenReturn(Optional.of(fixture.currentRound));

        invokeAdvance(fixture.currentRound);

        verify(raceEntryRepository, never()).save(any(RaceEntry.class));
        verify(raceRepository, never()).findByRound_RoundIdOrderBySequenceOrderAsc(any(UUID.class));
    }

    private void mockCommonTransitionData(TransitionFixture fixture) {
        when(roundRepository.findForUpdateByRoundId(fixture.currentRound.getRoundId()))
                .thenReturn(Optional.of(fixture.currentRound));
        when(raceRepository.findByRound_RoundIdOrderBySequenceOrderAsc(fixture.currentRound.getRoundId()))
                .thenReturn(List.of(fixture.firstRace, fixture.secondRace));
        when(roundRepository.findByTournament_TournamentIdAndSequenceOrder(
                fixture.tournament.getTournamentId(), 2)).thenReturn(Optional.of(fixture.nextRound));
        when(raceRepository.findByRound_RoundIdOrderBySequenceOrderAsc(fixture.nextRound.getRoundId()))
                .thenReturn(List.of(fixture.targetRace));
        when(raceEntryRepository.countByRace_Round_RoundId(fixture.nextRound.getRoundId())).thenReturn(0);
        when(raceReportRepository.findByRace_RaceId(fixture.firstRace.getRaceId()))
                .thenReturn(Optional.of(publishedReport(fixture.firstRace)));
        when(raceReportRepository.findByRace_RaceId(fixture.secondRace.getRaceId()))
                .thenReturn(Optional.of(publishedReport(fixture.secondRace)));
    }

    private TransitionFixture createFixture() {
        User admin = new User();
        admin.setUserId(UUID.randomUUID());

        Tournament tournament = new Tournament();
        tournament.setTournamentId(UUID.randomUUID());
        tournament.setPhase(TournamentPhase.RACING);

        Round currentRound = new Round();
        currentRound.setRoundId(UUID.randomUUID());
        currentRound.setRoundName("Round 1");
        currentRound.setSequenceOrder(1);
        currentRound.setFinal(false);
        currentRound.setTournament(tournament);
        currentRound.setCreatedBy(admin);
        currentRound.setTransitionStatus(RoundTransitionStatus.NOT_READY);

        Round nextRound = new Round();
        nextRound.setRoundId(UUID.randomUUID());
        nextRound.setRoundName("Round 2 (Final)");
        nextRound.setSequenceOrder(2);
        nextRound.setFinal(true);
        nextRound.setTournament(tournament);

        Race firstRace = completedRace(currentRound, 1);
        Race secondRace = completedRace(currentRound, 2);
        Race targetRace = completedRace(nextRound, 1);
        targetRace.setStatus(RoundStatus.SCHEDULING);
        return new TransitionFixture(tournament, currentRound, nextRound,
                firstRace, secondRace, targetRace);
    }

    private Race completedRace(Round round, int sequence) {
        Race race = new Race();
        race.setRaceId(UUID.randomUUID());
        race.setRound(round);
        race.setSequenceOrder(sequence);
        race.setStatus(RoundStatus.COMPLETED);
        return race;
    }

    private RaceReport publishedReport(Race race) {
        RaceReport report = new RaceReport();
        report.setRace(race);
        report.setStatus(ReportStatus.Published);
        return report;
    }

    private List<RaceResult> createResults(Race race, int count, int identifierOffset) {
        List<RaceResult> results = new ArrayList<>();
        for (int index = 0; index < count; index++) {
            Horse horse = new Horse();
            horse.setHorseId(UUID.randomUUID());
            Jockey jockey = new Jockey();
            jockey.setJockeyId(UUID.randomUUID());
            JockeyHorseContract contract = new JockeyHorseContract();
            contract.setContractId(UUID.randomUUID());
            contract.setHorse(horse);
            contract.setJockey(jockey);

            RaceEntry entry = new RaceEntry();
            entry.setEntryId(UUID.randomUUID());
            entry.setRace(race);
            entry.setContract(contract);

            RaceResult result = new RaceResult();
            result.setResultId(UUID.randomUUID());
            result.setRace(race);
            result.setEntry(entry);
            result.setRank(identifierOffset + index + 1);
            result.setStatus(RaceResultStatus.FINISHED);
            results.add(result);
        }
        return results;
    }

    private void invokeAdvance(Round round) throws Exception {
        Method method = RaceReportServiceImpl.class.getDeclaredMethod("advanceRoundIfPossible", Round.class);
        method.setAccessible(true);
        method.invoke(raceReportService, round);
    }

    private record TransitionFixture(Tournament tournament,
                                     Round currentRound,
                                     Round nextRound,
                                     Race firstRace,
                                     Race secondRace,
                                     Race targetRace) {
    }
}

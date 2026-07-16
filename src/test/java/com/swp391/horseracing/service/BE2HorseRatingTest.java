package com.swp391.horseracing.service;

import com.swp391.horseracing.config.HorseRatingProperties;
import com.swp391.horseracing.dto.horse.*;
import com.swp391.horseracing.entity.*;
import com.swp391.horseracing.enums.*;
import com.swp391.horseracing.exception.AppException;
import com.swp391.horseracing.exception.ErrorCode;
import com.swp391.horseracing.repository.*;
import com.swp391.horseracing.service.impl.HorseRatingServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class BE2HorseRatingTest {

    @Mock HorseRatingProperties properties;
    @Mock RaceRepository raceRepository;
    @Mock RaceResultRepository raceResultRepository;
    @Mock HorseRepository horseRepository;
    @Mock HorseRatingHistoryRepository ratingHistoryRepository;
    @Mock RaceReportRepository raceReportRepository;
    @Mock RoundRepository roundRepository;
    @Mock UserCurrentService userCurrentService;
    @Mock HorseOwnerRepository horseOwnerRepository;

    @InjectMocks
    HorseRatingServiceImpl horseRatingService;

    private UUID raceId;
    private Race race;

    @BeforeEach
    void setUp() {
        raceId = UUID.randomUUID();
        race = new Race();
        race.setRaceId(raceId);
        Round round = new Round();
        round.setRoundId(UUID.randomUUID());
        race.setRound(round);

        lenient().when(properties.getFirstBase()).thenReturn(6);
        lenient().when(properties.getFirstMax()).thenReturn(12);
        lenient().when(properties.getSecondBase()).thenReturn(2);
        lenient().when(properties.getSecondMax()).thenReturn(5);
        lenient().when(properties.getThirdBase()).thenReturn(1);
        lenient().when(properties.getThirdMax()).thenReturn(4);
        lenient().when(properties.getFourthFifthMax()).thenReturn(2);
        lenient().when(properties.getDnfChange()).thenReturn(-4);
        lenient().when(properties.getDisqualifiedChange()).thenReturn(-6);
        lenient().when(properties.getMaxDecrease()).thenReturn(-8);
        lenient().when(properties.getLargeFieldSize()).thenReturn(8);
        lenient().when(properties.getPolicyVersion()).thenReturn(1);

        // Opponent strength config
        lenient().when(properties.getStrongOpponentDifference()).thenReturn(20);
        lenient().when(properties.getMediumOpponentDifference()).thenReturn(10);
        lenient().when(properties.getWeakOpponentDifference()).thenReturn(1);
        lenient().when(properties.getFirstStrongOpponentBonus()).thenReturn(3);
        lenient().when(properties.getFirstMediumOpponentBonus()).thenReturn(2);
        lenient().when(properties.getFirstWeakOpponentBonus()).thenReturn(1);
        lenient().when(properties.getTopThreeStrongOpponentBonus()).thenReturn(2);
        lenient().when(properties.getTopThreeMediumOpponentBonus()).thenReturn(1);
        lenient().when(properties.getFourthFifthOpponentBonus()).thenReturn(1);

        // Gaps & Bonuses config
        lenient().when(properties.getWinnerLargeGapPercent()).thenReturn(2.0);
        lenient().when(properties.getWinnerMediumGapPercent()).thenReturn(1.0);
        lenient().when(properties.getTopThreeCloseGapPercent()).thenReturn(0.5);
        lenient().when(properties.getFourthFifthCloseGapPercent()).thenReturn(2.0);
        lenient().when(properties.getWinnerLargeGapBonus()).thenReturn(2);
        lenient().when(properties.getWinnerMediumGapBonus()).thenReturn(1);
        lenient().when(properties.getCloseFinishBonus()).thenReturn(1);
        lenient().when(properties.getLargeFieldBonus()).thenReturn(1);

        // Underperformance config
        lenient().when(properties.getSevereUnderperformanceGapPercent()).thenReturn(10.0);
        lenient().when(properties.getMediumUnderperformanceGapPercent()).thenReturn(6.0);
        lenient().when(properties.getSmallUnderperformanceGapPercent()).thenReturn(3.0);
        lenient().when(properties.getSevereUnderperformancePenalty()).thenReturn(-6);
        lenient().when(properties.getMediumUnderperformancePenalty()).thenReturn(-4);
        lenient().when(properties.getSmallUnderperformancePenalty()).thenReturn(-2);
        lenient().when(properties.getHighRatedUnderperformanceDifference()).thenReturn(20);
        lenient().when(properties.getHighRatedUnderperformanceExtraPenalty()).thenReturn(-2);
    }

    private RaceResult createFinishedResult(int rank, float finishTime, int rating, String name) {
        Horse h = new Horse();
        h.setHorseId(UUID.randomUUID());
        h.setName(name);
        h.setCurrentRating(rating);
        h.setRaceClass(RaceClass.fromRating(rating));

        JockeyHorseContract contract = new JockeyHorseContract();
        contract.setHorse(h);

        RaceEntry entry = new RaceEntry();
        entry.setStatus(RaceEntryStatus.FINISHED);
        entry.setContract(contract);

        RaceResult res = new RaceResult();
        res.setResultId(UUID.randomUUID());
        res.setRace(race);
        res.setEntry(entry);
        res.setStatus(RaceResultStatus.FINISHED);
        res.setRank(rank);
        res.setFinishTime(finishTime);
        return res;
    }

    private RaceResult createDnfResult(int rating, String name) {
        Horse h = new Horse();
        h.setHorseId(UUID.randomUUID());
        h.setName(name);
        h.setCurrentRating(rating);
        h.setRaceClass(RaceClass.fromRating(rating));

        JockeyHorseContract contract = new JockeyHorseContract();
        contract.setHorse(h);

        RaceEntry entry = new RaceEntry();
        entry.setStatus(RaceEntryStatus.DID_NOT_FINISH);
        entry.setContract(contract);

        RaceResult res = new RaceResult();
        res.setResultId(UUID.randomUUID());
        res.setRace(race);
        res.setEntry(entry);
        res.setStatus(RaceResultStatus.DID_NOT_FINISH);
        res.setRank(null);
        res.setFinishTime(null);
        return res;
    }

    private RaceResult createDqResult(int rating, String name) {
        Horse h = new Horse();
        h.setHorseId(UUID.randomUUID());
        h.setName(name);
        h.setCurrentRating(rating);
        h.setRaceClass(RaceClass.fromRating(rating));

        JockeyHorseContract contract = new JockeyHorseContract();
        contract.setHorse(h);

        RaceEntry entry = new RaceEntry();
        entry.setStatus(RaceEntryStatus.DISQUALIFIED);
        entry.setContract(contract);

        RaceResult res = new RaceResult();
        res.setResultId(UUID.randomUUID());
        res.setRace(race);
        res.setEntry(entry);
        res.setStatus(RaceResultStatus.DISQUALIFIED);
        res.setRank(null);
        res.setFinishTime(null);
        return res;
    }

    private Map<UUID, Integer> buildSnapshot(List<RaceResult> results) {
        Map<UUID, Integer> snapshot = new HashMap<>();
        for (RaceResult r : results) {
            snapshot.put(r.getEntry().getContract().getHorse().getHorseId(), r.getEntry().getContract().getHorse().getCurrentRating());
        }
        return snapshot;
    }

    @Test
    void testRank1WeakFieldCloseWin() {
        RaceResult r1 = createFinishedResult(1, 100.0f, 10, "H1");
        RaceResult r2 = createFinishedResult(2, 100.5f, 10, "H2");
        RaceResult r3 = createFinishedResult(3, 102.0f, 10, "H3");

        List<RaceResult> results = Arrays.asList(r1, r2, r3);
        when(raceRepository.findById(raceId)).thenReturn(Optional.of(race));
        when(raceResultRepository.findByRace_RaceId(raceId)).thenReturn(results);

        List<HorseRatingCalculation> calcs = horseRatingService.calculateForRace(raceId, buildSnapshot(results));
        
        HorseRatingCalculation h1Calc = calcs.stream().filter(c -> c.getHorseName().equals("H1")).findFirst().orElseThrow();
        assertEquals(6, h1Calc.getFinalChange());
        assertEquals(16, h1Calc.getNewRating());
    }

    @Test
    void testRank1StrongFieldGreatWin() {
        RaceResult r1 = createFinishedResult(1, 100.0f, 10, "Winner");
        RaceResult r2 = createFinishedResult(2, 102.5f, 100, "RunnerUp");
        
        List<RaceResult> results = new ArrayList<>();
        results.add(r1);
        results.add(r2);
        for (int i = 3; i <= 8; i++) {
            results.add(createFinishedResult(i, 105.0f + i, 100, "Horse" + i));
        }

        when(raceRepository.findById(raceId)).thenReturn(Optional.of(race));
        when(raceResultRepository.findByRace_RaceId(raceId)).thenReturn(results);

        List<HorseRatingCalculation> calcs = horseRatingService.calculateForRace(raceId, buildSnapshot(results));

        HorseRatingCalculation winnerCalc = calcs.stream().filter(c -> c.getHorseName().equals("Winner")).findFirst().orElseThrow();
        assertEquals(3, winnerCalc.getOpponentStrengthBonus());
        assertEquals(2, winnerCalc.getFinishPerformanceBonus());
        assertEquals(1, winnerCalc.getFieldSizeBonus());
        assertEquals(12, winnerCalc.getFinalChange());
        assertEquals(22, winnerCalc.getNewRating());
    }

    @Test
    void testRank2And3Limits() {
        RaceResult r1 = createFinishedResult(1, 100.0f, 10, "H1");
        RaceResult r2 = createFinishedResult(2, 100.1f, 10, "H2");
        RaceResult r3 = createFinishedResult(3, 100.1f, 10, "H3");

        List<RaceResult> results = Arrays.asList(r1, r2, r3);
        when(raceRepository.findById(raceId)).thenReturn(Optional.of(race));
        when(raceResultRepository.findByRace_RaceId(raceId)).thenReturn(results);

        List<HorseRatingCalculation> calcs = horseRatingService.calculateForRace(raceId, buildSnapshot(results));

        HorseRatingCalculation h2Calc = calcs.stream().filter(c -> c.getHorseName().equals("H2")).findFirst().orElseThrow();
        assertEquals(3, h2Calc.getFinalChange());

        HorseRatingCalculation h3Calc = calcs.stream().filter(c -> c.getHorseName().equals("H3")).findFirst().orElseThrow();
        assertEquals(2, h3Calc.getFinalChange());
    }

    @Test
    void testRank4And5Limits() {
        RaceResult r1 = createFinishedResult(1, 100.0f, 10, "H1");
        RaceResult r2 = createFinishedResult(2, 101.0f, 10, "H2");
        RaceResult r3 = createFinishedResult(3, 101.5f, 10, "H3");
        RaceResult r4 = createFinishedResult(4, 101.8f, 10, "H4"); // gap = 1.8% <= 2% => bonus = 1
        RaceResult r5 = createFinishedResult(5, 103.0f, 10, "H5"); // gap = 3.0% > 2% => bonus = 0

        List<RaceResult> results = Arrays.asList(r1, r2, r3, r4, r5);
        when(raceRepository.findById(raceId)).thenReturn(Optional.of(race));
        when(raceResultRepository.findByRace_RaceId(raceId)).thenReturn(results);

        List<HorseRatingCalculation> calcs = horseRatingService.calculateForRace(raceId, buildSnapshot(results));

        HorseRatingCalculation h4Calc = calcs.stream().filter(c -> c.getHorseName().equals("H4")).findFirst().orElseThrow();
        assertEquals(1, h4Calc.getFinalChange()); // Base 0 + Finish 1 = 1

        HorseRatingCalculation h5Calc = calcs.stream().filter(c -> c.getHorseName().equals("H5")).findFirst().orElseThrow();
        assertEquals(0, h5Calc.getFinalChange()); // Base 0 + Finish 0 = 0
    }

    @Test
    void testRank6PlusPenaltyAndClamp() {
        RaceResult r1 = createFinishedResult(1, 100.0f, 10, "Winner");
        RaceResult r2 = createFinishedResult(2, 101.0f, 10, "H2");
        RaceResult r3 = createFinishedResult(3, 102.0f, 10, "H3");
        RaceResult r4 = createFinishedResult(4, 103.0f, 10, "H4");
        RaceResult r5 = createFinishedResult(5, 104.0f, 10, "H5");
        RaceResult r6 = createFinishedResult(6, 112.0f, 100, "Underperformer"); 

        List<RaceResult> results = Arrays.asList(r1, r2, r3, r4, r5, r6);
        when(raceRepository.findById(raceId)).thenReturn(Optional.of(race));
        when(raceResultRepository.findByRace_RaceId(raceId)).thenReturn(results);

        List<HorseRatingCalculation> calcs = horseRatingService.calculateForRace(raceId, buildSnapshot(results));

        HorseRatingCalculation underCalc = calcs.stream().filter(c -> c.getHorseName().equals("Underperformer")).findFirst().orElseThrow();
        assertEquals(-8, underCalc.getUnderperformancePenalty());
        assertEquals(-8, underCalc.getFinalChange());
        assertEquals(92, underCalc.getNewRating());
    }

    @Test
    void testDnfAndDqFixedChange() {
        RaceResult r1 = createFinishedResult(1, 100.0f, 10, "Winner");
        RaceResult r2 = createDnfResult(20, "DnfHorse");
        RaceResult r3 = createDqResult(20, "DqHorse");

        List<RaceResult> results = Arrays.asList(r1, r2, r3);
        when(raceRepository.findById(raceId)).thenReturn(Optional.of(race));
        when(raceResultRepository.findByRace_RaceId(raceId)).thenReturn(results);

        List<HorseRatingCalculation> calcs = horseRatingService.calculateForRace(raceId, buildSnapshot(results));

        HorseRatingCalculation dnfCalc = calcs.stream().filter(c -> c.getHorseName().equals("DnfHorse")).findFirst().orElseThrow();
        assertEquals(-4, dnfCalc.getFinalChange());
        assertEquals(16, dnfCalc.getNewRating());

        HorseRatingCalculation dqCalc = calcs.stream().filter(c -> c.getHorseName().equals("DqHorse")).findFirst().orElseThrow();
        assertEquals(-6, dqCalc.getFinalChange());
        assertEquals(14, dqCalc.getNewRating());
    }

    @Test
    void testRatingNotBelowZero() {
        RaceResult r1 = createFinishedResult(1, 100.0f, 10, "Winner");
        RaceResult r2 = createDnfResult(2, "DnfHorse"); 

        List<RaceResult> results = Arrays.asList(r1, r2);
        when(raceRepository.findById(raceId)).thenReturn(Optional.of(race));
        when(raceResultRepository.findByRace_RaceId(raceId)).thenReturn(results);

        List<HorseRatingCalculation> calcs = horseRatingService.calculateForRace(raceId, buildSnapshot(results));

        HorseRatingCalculation dnfCalc = calcs.stream().filter(c -> c.getHorseName().equals("DnfHorse")).findFirst().orElseThrow();
        assertEquals(0, dnfCalc.getNewRating());
    }

    @Test
    void testRaceClassBoundaries() {
        assertEquals(RaceClass.CLASS_5, RaceClass.fromRating(39));
        assertEquals(RaceClass.CLASS_4, RaceClass.fromRating(40));
        assertEquals(RaceClass.CLASS_4, RaceClass.fromRating(59));
        assertEquals(RaceClass.CLASS_3, RaceClass.fromRating(60));
        assertEquals(RaceClass.CLASS_3, RaceClass.fromRating(79));
        assertEquals(RaceClass.CLASS_2, RaceClass.fromRating(80));
        assertEquals(RaceClass.CLASS_2, RaceClass.fromRating(99));
        assertEquals(RaceClass.CLASS_1, RaceClass.fromRating(100));
        assertEquals(RaceClass.CLASS_1, RaceClass.fromRating(119));
        assertEquals(RaceClass.G3, RaceClass.fromRating(120));
        assertEquals(RaceClass.G3, RaceClass.fromRating(135));
        assertEquals(RaceClass.G2, RaceClass.fromRating(136));
        assertEquals(RaceClass.G2, RaceClass.fromRating(149));
        assertEquals(RaceClass.G1, RaceClass.fromRating(150));
    }

    @Test
    void testPreviewSignedReportOnly() {
        RaceReport draftReport = new RaceReport();
        draftReport.setStatus(ReportStatus.DRAFT);
        when(raceRepository.findById(raceId)).thenReturn(Optional.of(race));
        when(raceReportRepository.findByRace_RaceId(raceId)).thenReturn(Optional.of(draftReport));

        assertThrows(AppException.class, () -> horseRatingService.previewForRace(raceId));

        RaceReport signedReport = new RaceReport();
        signedReport.setStatus(ReportStatus.SIGNED);
        when(raceReportRepository.findByRace_RaceId(raceId)).thenReturn(Optional.of(signedReport));
        when(raceResultRepository.findByRace_RaceId(raceId)).thenReturn(Collections.emptyList());

        RaceRatingPreviewResponse response = horseRatingService.previewForRace(raceId);
        assertNotNull(response);
        assertEquals("SIGNED", response.getReportStatus());
    }

    @Test
    void testPreviewDoesNotSave() {
        RaceReport report = new RaceReport();
        report.setStatus(ReportStatus.SIGNED);
        when(raceRepository.findById(raceId)).thenReturn(Optional.of(race));
        when(raceReportRepository.findByRace_RaceId(raceId)).thenReturn(Optional.of(report));
        when(raceResultRepository.findByRace_RaceId(raceId)).thenReturn(Collections.emptyList());

        horseRatingService.previewForRace(raceId);

        verify(ratingHistoryRepository, never()).save(any());
        verify(horseRepository, never()).save(any());
    }

    @Test
    void testRatingChangesOnlyForPublished() {
        RaceReport signedReport = new RaceReport();
        signedReport.setStatus(ReportStatus.SIGNED);
        when(raceRepository.findById(raceId)).thenReturn(Optional.of(race));
        when(raceReportRepository.findByRace_RaceId(raceId)).thenReturn(Optional.of(signedReport));

        assertThrows(AppException.class, () -> horseRatingService.getRatingChangesForRace(raceId));

        RaceReport publishedReport = new RaceReport();
        publishedReport.setStatus(ReportStatus.PUBLISHED);
        when(raceReportRepository.findByRace_RaceId(raceId)).thenReturn(Optional.of(publishedReport));
        when(ratingHistoryRepository.findByRace_RaceId(raceId)).thenReturn(Collections.emptyList());

        RaceRatingChangesResponse response = horseRatingService.getRatingChangesForRace(raceId);
        assertNotNull(response);
        assertEquals("PUBLISHED", response.getReportStatus());
    }

    @Test
    void testOwnerAuthorizationOnHorseHistory() {
        UUID horseId = UUID.randomUUID();
        Horse horse = new Horse();
        HorseOwner owner1 = new HorseOwner();
        owner1.setOwnerId(UUID.randomUUID());
        horse.setOwner(owner1);

        when(horseRepository.findById(horseId)).thenReturn(Optional.of(horse));

        // 1. Admin: Allowed
        User adminUser = new User();
        Role adminRole = new Role();
        adminRole.setRoleName(RoleName.ADMIN);
        adminUser.setRole(adminRole);
        when(userCurrentService.getCurrentUser()).thenReturn(adminUser);

        List<HorseRatingHistoryResponse> resAdmin = horseRatingService.getRatingHistoryForHorse(horseId);
        assertNotNull(resAdmin);

        // 2. Different Owner: Rejected
        User ownerUser2 = new User();
        Role ownerRole2 = new Role();
        ownerRole2.setRoleName(RoleName.HORSE_OWNER);
        ownerUser2.setRole(ownerRole2);
        ownerUser2.setUserId(UUID.randomUUID());
        when(userCurrentService.getCurrentUser()).thenReturn(ownerUser2);

        HorseOwner owner2 = new HorseOwner();
        owner2.setOwnerId(UUID.randomUUID());
        when(horseOwnerRepository.findByUser_UserId(ownerUser2.getUserId())).thenReturn(Optional.of(owner2));

        assertThrows(AppException.class, () -> horseRatingService.getRatingHistoryForHorse(horseId));

        // 3. Right Owner: Allowed
        User ownerUser1 = new User();
        ownerUser1.setRole(ownerRole2);
        ownerUser1.setUserId(UUID.randomUUID());
        when(userCurrentService.getCurrentUser()).thenReturn(ownerUser1);
        when(horseOwnerRepository.findByUser_UserId(ownerUser1.getUserId())).thenReturn(Optional.of(owner1));

        List<HorseRatingHistoryResponse> resOwner = horseRatingService.getRatingHistoryForHorse(horseId);
        assertNotNull(resOwner);
    }

    @Test
    void testHistoryReturnsSavedPolicyVersion() {
        UUID horseId = UUID.randomUUID();
        Horse horse = new Horse();
        HorseOwner owner = new HorseOwner();
        owner.setOwnerId(UUID.randomUUID());
        horse.setOwner(owner);

        User adminUser = new User();
        Role adminRole = new Role();
        adminRole.setRoleName(RoleName.ADMIN);
        adminUser.setRole(adminRole);
        when(userCurrentService.getCurrentUser()).thenReturn(adminUser);
        when(horseRepository.findById(horseId)).thenReturn(Optional.of(horse));

        RaceResult res = new RaceResult();
        res.setResultId(UUID.randomUUID());
        res.setRank(1);

        HorseRatingHistory hist = HorseRatingHistory.builder()
                .horse(horse)
                .race(race)
                .raceResult(res)
                .oldRating(10)
                .newRating(16)
                .oldRaceClass(RaceClass.CLASS_5)
                .newRaceClass(RaceClass.CLASS_5)
                .policyVersion(5) // Saved with version 5
                .calculatedAt(LocalDateTime.now())
                .build();

        when(ratingHistoryRepository.findByHorse_HorseIdOrderByCalculatedAtAsc(horseId))
                .thenReturn(Collections.singletonList(hist));

        List<HorseRatingHistoryResponse> historyList = horseRatingService.getRatingHistoryForHorse(horseId);
        assertEquals(1, historyList.size());
        assertEquals(5, historyList.get(0).getPolicyVersion());
    }

    @Test
    void testRoundSummaryThreeStates() {
        UUID roundId = UUID.randomUUID();
        Round round = new Round();
        round.setRoundId(roundId);

        Race race1 = new Race();
        race1.setRaceId(UUID.randomUUID());
        race1.setStatus(RoundStatus.ONGOING);
        race1.setRound(round);

        Race race2 = new Race();
        race2.setRaceId(UUID.randomUUID());
        race2.setStatus(RoundStatus.FINISHED);
        race2.setRound(round);

        round.setRaces(Arrays.asList(race1, race2));

        when(roundRepository.findById(roundId)).thenReturn(Optional.of(round));
        lenient().when(raceRepository.findById(race1.getRaceId())).thenReturn(Optional.of(race1));
        lenient().when(raceRepository.findById(race2.getRaceId())).thenReturn(Optional.of(race2));

        // 1. None processed -> NOT_STARTED
        when(raceReportRepository.findByRace_RaceId(any())).thenReturn(Optional.empty());
        RoundRatingSummaryResponse summary1 = horseRatingService.getRoundRatingSummary(roundId);
        assertEquals("NOT_STARTED", summary1.getSummaryStatus());

        // 2. One published -> PARTIAL
        RaceReport report2 = new RaceReport();
        report2.setStatus(ReportStatus.PUBLISHED);
        when(raceReportRepository.findByRace_RaceId(race2.getRaceId())).thenReturn(Optional.of(report2));
        when(ratingHistoryRepository.findByRace_RaceId(race2.getRaceId())).thenReturn(Collections.emptyList());

        RoundRatingSummaryResponse summary2 = horseRatingService.getRoundRatingSummary(roundId);
        assertEquals("PARTIAL", summary2.getSummaryStatus());

        // 3. All processed (Published / Cancelled) -> COMPLETED
        race1.setStatus(RoundStatus.CANCELLED);
        RoundRatingSummaryResponse summary3 = horseRatingService.getRoundRatingSummary(roundId);
        assertEquals("COMPLETED", summary3.getSummaryStatus());
    }

    @Test
    void testHorseRatingPropertiesValidation() {
        // 1. Valid Default Properties
        HorseRatingProperties props = new HorseRatingProperties();
        assertTrue(props.isValid());

        // 2. Strong opponent diff < medium opponent diff -> invalid
        props.setStrongOpponentDifference(5);
        props.setMediumOpponentDifference(10);
        assertFalse(props.isValid());

        // Reset to valid
        props.setStrongOpponentDifference(20);
        props.setMediumOpponentDifference(10);
        assertTrue(props.isValid());

        // 3. Severe gap < medium gap -> invalid
        props.setSevereUnderperformanceGapPercent(5.0);
        props.setMediumUnderperformanceGapPercent(6.0);
        assertFalse(props.isValid());

        // Reset
        props.setSevereUnderperformanceGapPercent(10.0);
        props.setMediumUnderperformanceGapPercent(6.0);
        assertTrue(props.isValid());

        // 4. Opponent bonus negative -> invalid
        props.setFirstStrongOpponentBonus(-1);
        assertFalse(props.isValid());

        // Reset
        props.setFirstStrongOpponentBonus(3);
        assertTrue(props.isValid());

        // 5. Penalty positive -> invalid
        props.setSmallUnderperformancePenalty(1);
        assertFalse(props.isValid());

        // Reset
        props.setSmallUnderperformancePenalty(-2);
        assertTrue(props.isValid());

        // 6. Policy version = 0 -> invalid
        props.setPolicyVersion(0);
        assertFalse(props.isValid());
    }

    @Test
    void checkFlywayOnClasspath() throws Exception {
        Class<?> clazz = Class.forName("org.flywaydb.core.Flyway");
        assertNotNull(clazz);
        System.out.println("Flyway class loaded: " + clazz.getName());
    }
}

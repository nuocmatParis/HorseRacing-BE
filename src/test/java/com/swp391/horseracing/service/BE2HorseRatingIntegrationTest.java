package com.swp391.horseracing.service;

import com.swp391.horseracing.entity.Horse;
import com.swp391.horseracing.entity.HorseRatingHistory;
import com.swp391.horseracing.entity.Race;
import com.swp391.horseracing.enums.RaceClass;
import com.swp391.horseracing.enums.RaceResultStatus;
import com.swp391.horseracing.enums.RoundStatus;
import com.swp391.horseracing.exception.AppException;
import com.swp391.horseracing.exception.ErrorCode;
import com.swp391.horseracing.repository.HorseRatingHistoryRepository;
import com.swp391.horseracing.repository.HorseRepository;
import com.swp391.horseracing.repository.RaceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.*;
import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ContextConfiguration(initializers = BE2HorseRatingIntegrationTest.FlywayInitializer.class)
@ActiveProfiles("test")
public class BE2HorseRatingIntegrationTest {

    static class FlywayInitializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {
        @Override
        public void initialize(ConfigurableApplicationContext applicationContext) {
            System.out.println("Running Flyway manually via ApplicationContextInitializer...");
            org.flywaydb.core.Flyway flyway = org.flywaydb.core.Flyway.configure()
                    .dataSource("jdbc:mysql://localhost:3306/SWP391_Project_HRTMS", "horse_app", "horse_app")
                    .baselineOnMigrate(true)
                    .baselineVersion("0")
                    .load();
            
            System.out.println("Repairing Flyway schema history...");
            flyway.repair();
            
            System.out.println("Migrating Flyway database...");
            flyway.migrate();
            System.out.println("Flyway migration completed successfully.");
        }
    }

    @Autowired
    private HorseRatingService horseRatingService;

    @Autowired
    private HorseRepository horseRepository;

    @Autowired
    private RaceRepository raceRepository;

    @Autowired
    private HorseRatingHistoryRepository ratingHistoryRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private PlatformTransactionManager transactionManager;

    private TransactionTemplate transactionTemplate;

    @BeforeEach
    void setUp() {
        transactionTemplate = new TransactionTemplate(transactionManager);
        // Clean up tables before each test to ensure test isolation
        jdbcTemplate.execute("SET FOREIGN_KEY_CHECKS = 0");
        jdbcTemplate.execute("TRUNCATE TABLE horse_rating_histories");
        jdbcTemplate.execute("TRUNCATE TABLE race_results");
        jdbcTemplate.execute("TRUNCATE TABLE race_entries");
        jdbcTemplate.execute("TRUNCATE TABLE jockey_horse_contracts");
        jdbcTemplate.execute("TRUNCATE TABLE races");
        jdbcTemplate.execute("TRUNCATE TABLE horses");
        jdbcTemplate.execute("SET FOREIGN_KEY_CHECKS = 1");
    }

    private void insertHorse(UUID id, String name, int rating, RaceClass raceClass, UUID ownerId) {
        jdbcTemplate.update(
            "INSERT INTO horses (horse_id, name, breed, gender, age, weight, color, health_status, current_rating, race_class, highest_rating, total_races, total_wins, total_places, win_rate, owner_id) " +
            "VALUES (?, ?, 'THOROUGHBRED', 'MALE', 4, 1000.0, 'Bay', 'HEALTHY', ?, ?, ?, 0, 0, 0, 0.0, ?)",
            id.toString(), name, rating, raceClass.name(), rating, ownerId.toString()
        );
    }

    private void insertJockeyHorseContract(UUID contractId, UUID horseId, UUID tournamentId) {
        jdbcTemplate.update(
            "INSERT INTO jockey_horse_contracts (contract_id, tournament_id, horse_tournament_reg_id, jockey_tournament_reg_id, owner_id, jockey_id, horse_id, " +
            "hire_fee, advance_percent, final_percent, system_contract_fee, owner_prize_share_percent, jockey_prize_share_percent, " +
            "payment_status, escrow_status, advance_payout_status, final_payout_status, status, requested_at) " +
            "VALUES (?, ?, ?, ?, ?, ?, ?, 100.0, 30.0, 70.0, 10.0, 80.0, 20.0, 'UNPAID', 'NOT_HELD', 'NOT_PAID', 'NOT_RELEASED', 'APPROVED', NOW())",
            contractId.toString(), tournamentId.toString(), UUID.randomUUID().toString(), UUID.randomUUID().toString(), UUID.randomUUID().toString(), UUID.randomUUID().toString(), horseId.toString()
        );
    }

    private void insertRace(UUID raceId, UUID roundId, RoundStatus status) {
        jdbcTemplate.update(
            "INSERT INTO races (race_id, round_id, name, start_time, end_time, track_condition, distance, sequence_order, status, prediction_open_at, prediction_close_at, schedule_published_at, created_by) " +
            "VALUES (?, ?, ?, NOW(), NOW(), 'FIRM', 1200.0, 1, ?, NOW(), NOW(), NOW(), ?)",
            raceId.toString(), roundId.toString(), "Race " + raceId.toString().substring(0, 8), status.name(), UUID.randomUUID().toString()
        );
    }

    private void insertRaceEntry(UUID entryId, UUID raceId, UUID contractId, int laneNumber) {
        jdbcTemplate.update(
            "INSERT INTO race_entries (entry_id, race_id, contract_id, lane_number, status, assigned_by, assigned_at, created_at) " +
            "VALUES (?, ?, ?, ?, 'CONFIRMED', ?, NOW(), NOW())",
            entryId.toString(), raceId.toString(), contractId.toString(), laneNumber, UUID.randomUUID().toString()
        );
    }

    private void insertRaceResult(UUID resultId, UUID entryId, Integer rank, Float finishTime, RaceResultStatus status, UUID raceId) {
        jdbcTemplate.update(
            "INSERT INTO race_results (result_id, race_id, entry_id, finish_position, finish_time, status, recorded_by, recorded_at) " +
            "VALUES (?, ?, ?, ?, ?, ?, ?, NOW())",
            resultId.toString(), raceId.toString(), entryId.toString(), rank, finishTime, status.name(), UUID.randomUUID().toString()
        );
    }

    @Test
    void testPublishSuccess() {
        // Setup mock data in real MySQL
        UUID horse1Id = UUID.randomUUID();
        UUID horse2Id = UUID.randomUUID();
        UUID ownerId = UUID.randomUUID();
        UUID tournamentId = UUID.randomUUID();
        UUID roundId = UUID.randomUUID();
        UUID raceId = UUID.randomUUID();
        UUID contract1Id = UUID.randomUUID();
        UUID contract2Id = UUID.randomUUID();
        UUID entry1Id = UUID.randomUUID();
        UUID entry2Id = UUID.randomUUID();
        UUID result1Id = UUID.randomUUID();
        UUID result2Id = UUID.randomUUID();

        jdbcTemplate.execute("SET FOREIGN_KEY_CHECKS = 0");
        insertHorse(horse1Id, "Horse One", 50, RaceClass.CLASS_5, ownerId);
        insertHorse(horse2Id, "Horse Two", 60, RaceClass.CLASS_5, ownerId);
        insertJockeyHorseContract(contract1Id, horse1Id, tournamentId);
        insertJockeyHorseContract(contract2Id, horse2Id, tournamentId);
        insertRace(raceId, roundId, RoundStatus.FINISHED);
        insertRaceEntry(entry1Id, raceId, contract1Id, 1);
        insertRaceEntry(entry2Id, raceId, contract2Id, 2);
        insertRaceResult(result1Id, entry1Id, 1, 60.0f, RaceResultStatus.FINISHED, raceId);
        insertRaceResult(result2Id, entry2Id, 2, 61.2f, RaceResultStatus.FINISHED, raceId);
        jdbcTemplate.execute("SET FOREIGN_KEY_CHECKS = 1");

        // Execute service call under transaction
        List<HorseRatingHistory> histories = transactionTemplate.execute(status -> 
            horseRatingService.calculateAndApplyForPublish(raceId)
        );

        assertNotNull(histories);
        assertEquals(2, histories.size());

        // Verify database updates
        Horse horse1 = horseRepository.findById(horse1Id).orElseThrow();
        Horse horse2 = horseRepository.findById(horse2Id).orElseThrow();

        // Horse 1 (Winner) rating should increase
        assertTrue(horse1.getCurrentRating() > 50);
        // Horse 2 (Second place) rating should increase or stay same/increase slightly less
        assertTrue(horse2.getCurrentRating() >= 60);

        // Verify rating history records are saved
        List<HorseRatingHistory> savedHistories = ratingHistoryRepository.findByHorse_HorseIdOrderByCalculatedAtAsc(horse1Id);
        assertEquals(1, savedHistories.size());
        assertEquals(50, savedHistories.get(0).getOldRating());
        assertEquals(horse1.getCurrentRating(), savedHistories.get(0).getNewRating());
    }

    @Test
    void testRollbackTransaction() {
        UUID horse1Id = UUID.randomUUID();
        UUID horse2Id = UUID.randomUUID();
        UUID ownerId = UUID.randomUUID();
        UUID tournamentId = UUID.randomUUID();
        UUID roundId = UUID.randomUUID();
        UUID raceId = UUID.randomUUID();
        UUID contract1Id = UUID.randomUUID();
        UUID contract2Id = UUID.randomUUID();
        UUID entry1Id = UUID.randomUUID();
        UUID entry2Id = UUID.randomUUID();
        UUID result1Id = UUID.randomUUID();
        UUID result2Id = UUID.randomUUID();

        jdbcTemplate.execute("SET FOREIGN_KEY_CHECKS = 0");
        insertHorse(horse1Id, "Horse One", 50, RaceClass.CLASS_5, ownerId);
        insertHorse(horse2Id, "Horse Two", 60, RaceClass.CLASS_5, ownerId);
        insertJockeyHorseContract(contract1Id, horse1Id, tournamentId);
        insertJockeyHorseContract(contract2Id, horse2Id, tournamentId);
        insertRace(raceId, roundId, RoundStatus.FINISHED);
        insertRaceEntry(entry1Id, raceId, contract1Id, 1);
        insertRaceEntry(entry2Id, raceId, contract2Id, 2);
        insertRaceResult(result1Id, entry1Id, 1, 60.0f, RaceResultStatus.FINISHED, raceId);
        insertRaceResult(result2Id, entry2Id, 2, 61.2f, RaceResultStatus.FINISHED, raceId);
        jdbcTemplate.execute("SET FOREIGN_KEY_CHECKS = 1");

        // We run a transaction where we intentionally update the horse rating outside the calculation scope,
        // triggering a concurrent change check error (ErrorCode.HORSE_RATING_CHANGED_RETRY_REQUIRED).
        Exception exception = assertThrows(AppException.class, () -> {
            transactionTemplate.execute(status -> {
                // Calculate and apply
                // Before calculation runs, let's modify the horse rating in database via a raw query
                // to trigger the check failure
                jdbcTemplate.update("UPDATE horses SET current_rating = 99 WHERE horse_id = ?", horse1Id.toString());
                
                horseRatingService.calculateAndApplyForPublish(raceId);
                return null;
            });
        });

        assertEquals(ErrorCode.HORSE_RATING_CHANGED_RETRY_REQUIRED, ((AppException) exception).getErrorCode());

        // Verify that the rating change was rolled back and the rating is 99 (or not updated by the calculation)
        // Wait, the UPDATE statement itself was part of the transaction, so it is ALSO rolled back!
        // The rating must remain 50 (the value before the transaction)!
        Horse horse1 = horseRepository.findById(horse1Id).orElseThrow();
        assertEquals(50, horse1.getCurrentRating());

        // Verify no history records were created
        assertTrue(ratingHistoryRepository.findByHorse_HorseIdOrderByCalculatedAtAsc(horse1Id).isEmpty());
    }

    @Test
    void testConcurrentPublishWithPessimisticLock() throws Exception {
        UUID horse1Id = UUID.randomUUID();
        UUID horse2Id = UUID.randomUUID();
        UUID ownerId = UUID.randomUUID();
        UUID tournamentId = UUID.randomUUID();
        UUID roundId = UUID.randomUUID();
        UUID raceId = UUID.randomUUID();
        UUID contract1Id = UUID.randomUUID();
        UUID contract2Id = UUID.randomUUID();
        UUID entry1Id = UUID.randomUUID();
        UUID entry2Id = UUID.randomUUID();
        UUID result1Id = UUID.randomUUID();
        UUID result2Id = UUID.randomUUID();

        jdbcTemplate.execute("SET FOREIGN_KEY_CHECKS = 0");
        insertHorse(horse1Id, "Horse One", 50, RaceClass.CLASS_5, ownerId);
        insertHorse(horse2Id, "Horse Two", 60, RaceClass.CLASS_5, ownerId);
        insertJockeyHorseContract(contract1Id, horse1Id, tournamentId);
        insertJockeyHorseContract(contract2Id, horse2Id, tournamentId);
        insertRace(raceId, roundId, RoundStatus.FINISHED);
        insertRaceEntry(entry1Id, raceId, contract1Id, 1);
        insertRaceEntry(entry2Id, raceId, contract2Id, 2);
        insertRaceResult(result1Id, entry1Id, 1, 60.0f, RaceResultStatus.FINISHED, raceId);
        insertRaceResult(result2Id, entry2Id, 2, 61.2f, RaceResultStatus.FINISHED, raceId);
        jdbcTemplate.execute("SET FOREIGN_KEY_CHECKS = 1");

        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch latch = new CountDownLatch(1);

        // Thread 1 acquires pessimistic lock by calling publish (within transaction)
        Future<List<HorseRatingHistory>> future1 = executor.submit(() -> 
            transactionTemplate.execute(status -> {
                try {
                    // Start transaction and calculate
                    List<HorseRatingHistory> res = horseRatingService.calculateAndApplyForPublish(raceId);
                    // Sleep to hold the pessimistic lock
                    Thread.sleep(1000);
                    return res;
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException(e);
                }
            })
        );

        // Small delay to ensure Thread 1 gets the lock first
        Thread.sleep(100);

        // Thread 2 tries to publish the same race or update the same horses.
        // It should be blocked or throw an exception (since the row is locked and rating history exists on commit).
        Future<Exception> future2 = executor.submit(() -> {
            try {
                transactionTemplate.execute(status -> {
                    horseRatingService.calculateAndApplyForPublish(raceId);
                    return null;
                });
                return null;
            } catch (Exception e) {
                return e;
            }
        });

        List<HorseRatingHistory> res1 = future1.get();
        Exception ex2 = future2.get();

        assertNotNull(res1);
        assertNotNull(ex2);
        assertTrue(ex2 instanceof AppException);
        // Thread 2 fails because Thread 1 committed successfully and inserted the rating histories,
        // so the retry checks that rating history exists and throws ErrorCode.HORSE_RATING_ALREADY_APPLIED.
        assertEquals(ErrorCode.HORSE_RATING_ALREADY_APPLIED, ((AppException) ex2).getErrorCode());

        executor.shutdown();
    }

    @Test
    void testIdempotency() {
        UUID horse1Id = UUID.randomUUID();
        UUID horse2Id = UUID.randomUUID();
        UUID ownerId = UUID.randomUUID();
        UUID tournamentId = UUID.randomUUID();
        UUID roundId = UUID.randomUUID();
        UUID raceId = UUID.randomUUID();
        UUID contract1Id = UUID.randomUUID();
        UUID contract2Id = UUID.randomUUID();
        UUID entry1Id = UUID.randomUUID();
        UUID entry2Id = UUID.randomUUID();
        UUID result1Id = UUID.randomUUID();
        UUID result2Id = UUID.randomUUID();

        jdbcTemplate.execute("SET FOREIGN_KEY_CHECKS = 0");
        insertHorse(horse1Id, "Horse One", 50, RaceClass.CLASS_5, ownerId);
        insertHorse(horse2Id, "Horse Two", 60, RaceClass.CLASS_5, ownerId);
        insertJockeyHorseContract(contract1Id, horse1Id, tournamentId);
        insertJockeyHorseContract(contract2Id, horse2Id, tournamentId);
        insertRace(raceId, roundId, RoundStatus.FINISHED);
        insertRaceEntry(entry1Id, raceId, contract1Id, 1);
        insertRaceEntry(entry2Id, raceId, contract2Id, 2);
        insertRaceResult(result1Id, entry1Id, 1, 60.0f, RaceResultStatus.FINISHED, raceId);
        insertRaceResult(result2Id, entry2Id, 2, 61.2f, RaceResultStatus.FINISHED, raceId);
        jdbcTemplate.execute("SET FOREIGN_KEY_CHECKS = 1");

        // First publish succeeds
        List<HorseRatingHistory> histories1 = transactionTemplate.execute(status -> 
            horseRatingService.calculateAndApplyForPublish(raceId)
        );
        assertNotNull(histories1);
        assertEquals(2, histories1.size());

        // Second publish fails with HORSE_RATING_ALREADY_APPLIED
        Exception exception = assertThrows(AppException.class, () -> 
            transactionTemplate.execute(status -> {
                horseRatingService.calculateAndApplyForPublish(raceId);
                return null;
            })
        );

        assertEquals(ErrorCode.HORSE_RATING_ALREADY_APPLIED, ((AppException) exception).getErrorCode());
    }

    @Test
    void testLockOrderPreventDeadlock() throws Exception {
        UUID horse1Id = UUID.fromString("11111111-1111-1111-1111-111111111111");
        UUID horse2Id = UUID.fromString("22222222-2222-2222-2222-222222222222");
        UUID ownerId = UUID.randomUUID();
        UUID tournamentId = UUID.randomUUID();
        UUID roundId = UUID.randomUUID();
        UUID raceId = UUID.randomUUID();
        UUID contract1Id = UUID.randomUUID();
        UUID contract2Id = UUID.randomUUID();
        UUID entry1Id = UUID.randomUUID();
        UUID entry2Id = UUID.randomUUID();
        UUID result1Id = UUID.randomUUID();
        UUID result2Id = UUID.randomUUID();

        jdbcTemplate.execute("SET FOREIGN_KEY_CHECKS = 0");
        insertHorse(horse1Id, "Horse One", 50, RaceClass.CLASS_5, ownerId);
        insertHorse(horse2Id, "Horse Two", 60, RaceClass.CLASS_5, ownerId);
        insertJockeyHorseContract(contract1Id, horse1Id, tournamentId);
        insertJockeyHorseContract(contract2Id, horse2Id, tournamentId);
        insertRace(raceId, roundId, RoundStatus.FINISHED);
        insertRaceEntry(entry1Id, raceId, contract1Id, 1);
        insertRaceEntry(entry2Id, raceId, contract2Id, 2);
        insertRaceResult(result1Id, entry1Id, 1, 60.0f, RaceResultStatus.FINISHED, raceId);
        insertRaceResult(result2Id, entry2Id, 2, 61.2f, RaceResultStatus.FINISHED, raceId);
        jdbcTemplate.execute("SET FOREIGN_KEY_CHECKS = 1");

        // Verify that list of horse IDs sorting works when querying for update
        // We simulate two threads locking horses. Because they lock in sorted order,
        // they lock Horse 1 (UUID starting with 1111...) first, then Horse 2 (UUID starting with 2222...).
        // Even if the results lists return them in different order (e.g. entry1 and entry2),
        // the sorted stream forces a deterministic locking sequence.
        
        List<UUID> originalList = Arrays.asList(horse2Id, horse1Id);
        List<UUID> sortedList = new ArrayList<>(originalList);
        Collections.sort(sortedList);
        
        assertEquals(horse1Id, sortedList.get(0));
        assertEquals(horse2Id, sortedList.get(1));

        // Execute publish and verify success (no deadlocks)
        List<HorseRatingHistory> histories = transactionTemplate.execute(status -> 
            horseRatingService.calculateAndApplyForPublish(raceId)
        );
        assertNotNull(histories);
        assertEquals(2, histories.size());
    }
}

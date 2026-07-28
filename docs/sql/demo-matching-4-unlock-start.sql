-- ============================================================================
-- HRTMS DEMO - MỞ KHÓA START CHO FINAL RACE 4 NGỰA
-- MySQL 8+
-- ============================================================================
-- Chỉ chạy file này SAU KHI:
--   1. Spectator đã tạo ít nhất 1 prediction PENDING.
--   2. Cả 4 ngựa đã có HorseInspection CONFIRMED + PASS.
--   3. Cả 4 Jockey đã có JockeyInspection CONFIRMED + PASS.
--   4. Cả 4 RaceEntry vẫn CONFIRMED.
--
-- File này KHÔNG tự tạo inspection, prediction hay tự Start race.
-- Nó chỉ dời start_time về NOW()-1 phút để Race Referee bấm Start qua FE/API.
-- Guard sẽ chặn nếu dữ liệu demo chưa sẵn sàng.
-- ============================================================================

USE SWP391_Project_HRTMS;
SET NAMES utf8mb4;

SET @now = NOW();
SET @tournament_id = 'e5000000-0000-0000-0000-000000000001';
SET @round_id      = 'e5010000-0000-0000-0000-000000000001';
SET @race_id       = 'e5020000-0000-0000-0000-000000000001';

-- ============================================================================
-- GUARD: không cho "mở Start" nếu demo chưa hoàn tất prediction + inspection.
-- ============================================================================

DROP TEMPORARY TABLE IF EXISTS demo_matching_start_guard;

CREATE TEMPORARY TABLE demo_matching_start_guard (
    scheduled_race_count INT NOT NULL CHECK (scheduled_race_count = 1),
    confirmed_entry_count INT NOT NULL CHECK (confirmed_entry_count = 4),
    horse_pass_count INT NOT NULL CHECK (horse_pass_count = 4),
    jockey_pass_count INT NOT NULL CHECK (jockey_pass_count = 4),
    pending_prediction_count INT NOT NULL CHECK (pending_prediction_count >= 1)
);

INSERT INTO demo_matching_start_guard (
    scheduled_race_count,
    confirmed_entry_count,
    horse_pass_count,
    jockey_pass_count,
    pending_prediction_count
)
SELECT
    (SELECT COUNT(*)
     FROM races
     WHERE race_id = @race_id
       AND status = 'SCHEDULED'
       AND started_at IS NULL),
    (SELECT COUNT(*)
     FROM race_entries
     WHERE race_id = @race_id
       AND status = 'CONFIRMED'),
    (SELECT COUNT(*)
     FROM horse_inspections hi
     JOIN race_entries re ON re.entry_id = hi.entry_id
     WHERE re.race_id = @race_id
       AND re.status = 'CONFIRMED'
       AND hi.status = 'CONFIRMED'
       AND hi.result = 'PASS'
       AND (
           hi.handicap_weight IS NULL
           OR hi.handicap_weight <= 0
           OR hi.is_handicap_confirmed = 1
       )),
    (SELECT COUNT(*)
     FROM jockey_inspections ji
     JOIN race_entries re ON re.entry_id = ji.entry_id
     WHERE re.race_id = @race_id
       AND re.status = 'CONFIRMED'
       AND ji.status = 'CONFIRMED'
       AND ji.result = 'PASS'),
    (SELECT COUNT(*)
     FROM predictions
     WHERE race_id = @race_id
       AND status = 'PENDING');

DROP TEMPORARY TABLE demo_matching_start_guard;

START TRANSACTION;

-- Start hợp lệ từ NOW()-1 phút đến NOW()+4 giờ 59 phút
-- vì start_late_tolerance_minutes của giải demo là 300.
SET @new_race_start = DATE_SUB(@now, INTERVAL 1 MINUTE);
SET @new_race_end = DATE_ADD(@now, INTERVAL 5 HOUR);

UPDATE races
SET start_time = @new_race_start,
    end_time = @new_race_end,
    status = 'SCHEDULED',
    prediction_close_at = DATE_SUB(@now, INTERVAL 2 MINUTE),
    inspection_finalized_at = NULL,
    started_at = NULL,
    started_by = NULL,
    finished_at = NULL
WHERE race_id = @race_id;

UPDATE rounds
SET start_date = @new_race_start,
    end_date = @new_race_end,
    status = 'SCHEDULED'
WHERE round_id = @round_id;

UPDATE tournaments
SET phase = 'RACING',
    status = 'ONGOING',
    start_date = DATE(@new_race_start),
    end_date = DATE(@new_race_end),
    competition_start_at = @new_race_start,
    current_round_name = 'Chung kết demo 4 ngựa',
    start_late_tolerance_minutes = 300
WHERE tournament_id = @tournament_id;

COMMIT;

-- ============================================================================
-- KIỂM TRA SAU KHI MỞ START
-- ============================================================================

SELECT
    race.race_id,
    race.name,
    race.status,
    race.start_time AS earliest_start,
    DATE_ADD(race.start_time, INTERVAL t.start_late_tolerance_minutes MINUTE)
        AS latest_start,
    NOW() AS server_now,
    COUNT(DISTINCT CASE WHEN re.status = 'CONFIRMED' THEN re.entry_id END)
        AS confirmed_entries,
    COUNT(DISTINCT CASE
        WHEN hi.status = 'CONFIRMED'
         AND hi.result = 'PASS'
         AND (
             hi.handicap_weight IS NULL
             OR hi.handicap_weight <= 0
             OR hi.is_handicap_confirmed = 1
         )
        THEN hi.horse_inspection_id END) AS horse_pass,
    COUNT(DISTINCT CASE
        WHEN ji.status = 'CONFIRMED'
         AND ji.result = 'PASS'
        THEN ji.jockey_inspection_id END) AS jockey_pass
FROM races race
JOIN rounds r ON r.round_id = race.round_id
JOIN tournaments t ON t.tournament_id = r.tournament_id
LEFT JOIN race_entries re ON re.race_id = race.race_id
LEFT JOIN horse_inspections hi ON hi.entry_id = re.entry_id
LEFT JOIN jockey_inspections ji ON ji.entry_id = re.entry_id
WHERE race.race_id = @race_id
GROUP BY
    race.race_id,
    race.name,
    race.status,
    race.start_time,
    t.start_late_tolerance_minutes;

SELECT
    'BƯỚC TIẾP THEO' AS note,
    'Đăng nhập frace_ref, gọi readiness rồi bấm Start. API: POST /api/referee/races/e5020000-0000-0000-0000-000000000001/start' AS action;

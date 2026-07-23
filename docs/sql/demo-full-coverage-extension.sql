-- ============================================================================
-- HRTMS DEMO FULL COVERAGE EXTENSION (MySQL 8+)
-- Chạy SAU docs/sql/demo-test-data.sql trên database local/test.
-- Mục tiêu: bổ sung bracket 32 entry để demo 2 race vòng loại -> 1 Final.
-- Mật khẩu tất cả tài khoản jockey9..jockey40: admin123
-- ============================================================================

USE SWP391_Project_HRTMS;

SET NAMES utf8mb4;
SET @now = NOW();
SET @demo_password = '$2a$12$AsBCvrsZ2yvqd7RyEflkfOGwfTewt8CSx40CKh0ZIZuD4WZ49wo4a';
SET @role_jockey = (SELECT role_id FROM roles WHERE role_name = 'JOCKEY' LIMIT 1);
SET @admin_user = '10000000-0000-0000-0000-000000000001';
SET @owner_id = '20000000-0000-0000-0000-000000000001';
SET @referee_id = '20000000-0000-0000-0000-000000000004';
SET @tournament_id = '50000000-0000-0000-0000-000000000004';
SET @round_1 = '80000000-0000-0000-0000-000000000041';
SET @round_2 = '80000000-0000-0000-0000-000000000042';
SET @race_a = '90000000-0000-0000-0000-000000000041';
SET @race_b = '90000000-0000-0000-0000-000000000042';
SET @race_final = '90000000-0000-0000-0000-000000000043';
SET @race_a_start = TIMESTAMP(DATE_SUB(CURRENT_DATE, INTERVAL 2 DAY), '08:00:00');
SET @race_a_end = DATE_ADD(@race_a_start, INTERVAL 30 MINUTE);
SET @race_b_start = DATE_ADD(@race_a_end, INTERVAL 35 MINUTE);
SET @race_b_end = DATE_ADD(@race_b_start, INTERVAL 30 MINUTE);
SET @final_start = DATE_ADD(@race_b_start, INTERVAL 7 DAY);
SET @final_end = DATE_ADD(@final_start, INTERVAL 30 MINUTE);
ALTER DATABASE `SWP391_Project_HRTMS` CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
-- Reset dữ liệu phát sinh nếu đã từng publish hai report và chuyển Top 4.
DELETE FROM horse_rating_histories
WHERE race_id IN (
                  @race_a COLLATE utf8mb4_unicode_ci,
                  @race_b COLLATE utf8mb4_unicode_ci
    );

DELETE FROM race_entries
WHERE race_id = @race_final COLLATE utf8mb4_unicode_ci;

DROP TEMPORARY TABLE IF EXISTS demo_numbers_32;
CREATE TEMPORARY TABLE demo_numbers_32 (n INT PRIMARY KEY);
INSERT INTO demo_numbers_32 (n) VALUES
    (1),(2),(3),(4),(5),(6),(7),(8),
    (9),(10),(11),(12),(13),(14),(15),(16),
    (17),(18),(19),(20),(21),(22),(23),(24),
    (25),(26),(27),(28),(29),(30),(31),(32);

-- 1. 32 tài khoản Kỵ sĩ độc lập cho Round có 32 entry.
INSERT INTO users
    (user_id, username, password, email, dob, gender, full_name, phone_number,
     image_url, status, created_at, last_login_at, role_id)
SELECT
    CONCAT('11000000-0000-0000-0000-', LPAD(n, 12, '0')),
    CONCAT('jockey', n + 8),
    @demo_password,
    CONCAT('demo.bracket.jock', LPAD(n, 2, '0'), '@hrtms.local'),
    DATE_ADD('1990-01-01', INTERVAL n MONTH),
    IF(MOD(n, 2) = 0, 'FEMALE', 'MALE'),
    CONCAT('Kỵ sĩ Bracket ', LPAD(n, 2, '0')),
    NULL, NULL, 'ACTIVE', @now, @now, @role_jockey
FROM demo_numbers_32
ON DUPLICATE KEY UPDATE
    username = VALUES(username),
    password = VALUES(password),
    status = 'ACTIVE';

INSERT INTO jockeys
    (jockey_id, user_id, height, weight, experience_years,
     specialization, status, total_races, total_wins, jockey_tier,
     tier_updated_at, last_race_at, created_at)
SELECT
    CONCAT('22000000-0000-0000-0000-', LPAD(n, 12, '0')),
    CONCAT('11000000-0000-0000-0000-', LPAD(n, 12, '0')),
    1.58 + MOD(n, 8) / 100,
    49 + MOD(n, 5),
    2 + MOD(n, 9),
    IF(MOD(n, 2) = 0, 'SPRINT', 'MILE'),
    'AVAILABLE', 10 + n, MOD(n, 8),
    IF(n <= 8, 'PROFESSIONAL', IF(n <= 20, 'JUNIOR', 'APPRENTICE')),
    @now, @race_b_end, @now
FROM demo_numbers_32
ON DUPLICATE KEY UPDATE status = 'AVAILABLE', total_races = VALUES(total_races);

INSERT INTO wallets
    (wallet_id, owner_type, balance, currency, status, created_at, updated_at,
     wallet_purpose, user_id)
SELECT
    CONCAT('31000000-0000-0000-0000-', LPAD(n, 12, '0')),
    'USER', 300000, 'VND', 'ACTIVE', @now, @now, 'USER_MAIN',
    CONCAT('11000000-0000-0000-0000-', LPAD(n, 12, '0'))
FROM demo_numbers_32
ON DUPLICATE KEY UPDATE balance = VALUES(balance), status = 'ACTIVE';

-- 2. 32 ngựa thuộc Owner demo.
INSERT INTO horses
    (horse_id, name, breed, gender, age, weight, color, image_url,
     health_status, current_rating, race_class, highest_rating,
     rating_updated_at, total_races, total_wins, total_places, win_rate,
     last_race_at, created_at, owner_id)
SELECT
    CONCAT('41000000-0000-0000-0000-', LPAD(n, 12, '0')),
    CONCAT('Ngựa Bracket ', LPAD(n, 2, '0')),
    'THOROUGHBRED', IF(MOD(n, 2) = 0, 'FEMALE', 'MALE'),
    3 + MOD(n, 4), 460 + MOD(n * 7, 35),
    IF(MOD(n, 3) = 0, 'Đen', IF(MOD(n, 3) = 1, 'Nâu', 'Xám')),
    NULL, 'HEALTHY', 60 + n, 'CLASS_3', 60 + n,
    @now, 5 + n, MOD(n, 6), MOD(n, 10), 10.0,
    @race_b_end, @now, @owner_id
FROM demo_numbers_32
ON DUPLICATE KEY UPDATE current_rating = VALUES(current_rating), health_status = 'HEALTHY';

-- 3. Tournament bracket 32: 2 race vòng loại, Top 4 mỗi race vào Final 8 entry.
INSERT INTO tournaments
    (tournament_id, name, description, start_date, end_date, finished_at,
     location, registration_fee, system_contract_fee, total_prize_pool,
     allowed_breed, min_horse_age, max_horse_age,
     prediction_top1_correct_points, prediction_top3_exact_position_points,
     prediction_top3_correct_horse_points, prediction_top3_perfect_bonus_points,
     prediction_open_minutes_before, prediction_close_minutes_before,
     prediction_card_open_hours_before_first_race,
     inspection_open_minutes_before, inspection_close_minutes_before,
     max_races_per_day, min_race_interval_minutes,
     start_early_tolerance_minutes, start_late_tolerance_minutes,
     default_race_operational_minutes, race_day_start_time, race_day_end_time,
      apply_break_time, break_start_time, break_end_time,
      status, phase, created_at, published_at,
     registration_open_at, registration_close_at, review_deadline_at,
     jockey_matching_deadline_at, scheduling_deadline_at, competition_start_at,
      current_round_name, race_class, distance,
     top_weight_lbs, min_weight_lbs, equipment_weight_kg, handicap_enabled,
     max_approved_horses, max_approved_jockeys, max_approved_entries,
     planned_round_count, planned_race_count, bracket_plan_status,
     bracket_plan_version, created_by)
VALUES
    (@tournament_id, 'DEMO 4 - Bracket 32 chuyển vòng',
     'Hai race vòng loại đã có result và Signed report; publish cả hai để demo Top 4 atomic sang Final.',
     DATE_SUB(CURRENT_DATE, INTERVAL 5 DAY), DATE_ADD(CURRENT_DATE, INTERVAL 14 DAY), NULL,
     'Trường đua Demo Bracket', 100000, 50000, 12000000,
     'THOROUGHBRED', 2, 12, 100, 30, 10, 50, 120, 5, 24,
     90, 30, 9, 35, 0, 30, 30, '08:00:00', '18:00:00',
      0, NULL, NULL, 'ONGOING', 'RESULT_PENDING', DATE_SUB(@now, INTERVAL 35 DAY),
     DATE_SUB(@now, INTERVAL 30 DAY), DATE_SUB(@now, INTERVAL 35 DAY),
     DATE_SUB(@now, INTERVAL 30 DAY), DATE_SUB(@now, INTERVAL 25 DAY),
     DATE_SUB(@now, INTERVAL 20 DAY), DATE_SUB(@now, INTERVAL 15 DAY), @race_a_start,
      'Vòng 1', 'CLASS_3', 'MILE_1600M', 135, 115, 1.5, 0,
     32, 32, 32, 2, 3, 'LOCKED', 1, @admin_user)
ON DUPLICATE KEY UPDATE phase = 'RESULT_PENDING', current_round_name = 'Vòng 1';

INSERT INTO horse_tournament_registrations
    (horse_tournament_reg_id, tournament_id, horse_id, owner_id, status,
     submitted_at, reviewed_by, reviewed_at, note,
     rating_at_registration, race_class_at_registration)
SELECT
    CONCAT('62000000-0000-0000-0000-', LPAD(n, 12, '0')),
    @tournament_id,
    CONCAT('41000000-0000-0000-0000-', LPAD(n, 12, '0')),
    @owner_id, 'APPROVED', DATE_SUB(@now, INTERVAL 30 DAY),
    @admin_user, DATE_SUB(@now, INTERVAL 25 DAY),
    'Hồ sơ đã duyệt cho bracket 32', 60 + n, 'CLASS_3'
FROM demo_numbers_32
ON DUPLICATE KEY UPDATE status = 'APPROVED';

INSERT INTO jockey_tournament_registrations
    (jockey_tournament_reg_id, tournament_id, jockey_id, status,
     submitted_at, reviewed_by, reviewed_at, note, hire_fee)
SELECT
    CONCAT('63000000-0000-0000-0000-', LPAD(n, 12, '0')),
    @tournament_id,
    CONCAT('22000000-0000-0000-0000-', LPAD(n, 12, '0')),
    'APPROVED', DATE_SUB(@now, INTERVAL 28 DAY), NULL,
    DATE_SUB(@now, INTERVAL 28 DAY), 'Kỵ sĩ tự động được chấp nhận', 1000000
FROM demo_numbers_32
ON DUPLICATE KEY UPDATE status = 'APPROVED';

INSERT INTO jockey_horse_contracts
    (contract_id, tournament_id, horse_tournament_reg_id,
     jockey_tournament_reg_id, owner_id, horse_id, jockey_id,
     hire_fee, advance_percent, final_percent, advance_paid_amount,
     escrow_amount, system_contract_fee, owner_prize_share_percent,
     jockey_prize_share_percent, payment_status, escrow_status,
     advance_payout_status, final_payout_status, status,
     advance_payout_at, final_payout_at, requested_at, responded_at,
     accepted_at, submitted_at, reviewed_by, reviewed_at, contract_note)
SELECT
    CONCAT('72000000-0000-0000-0000-', LPAD(n, 12, '0')),
    @tournament_id,
    CONCAT('62000000-0000-0000-0000-', LPAD(n, 12, '0')),
    CONCAT('63000000-0000-0000-0000-', LPAD(n, 12, '0')),
    @owner_id,
    CONCAT('41000000-0000-0000-0000-', LPAD(n, 12, '0')),
    CONCAT('22000000-0000-0000-0000-', LPAD(n, 12, '0')),
    1000000, 30, 70, 300000, 700000, 50000, 80, 20,
    'PAID', 'PARTIALLY_RELEASED', 'PAID', 'NOT_RELEASED', 'APPROVED',
    DATE_SUB(@now, INTERVAL 18 DAY), NULL, DATE_SUB(@now, INTERVAL 22 DAY),
    DATE_SUB(@now, INTERVAL 21 DAY), DATE_SUB(@now, INTERVAL 21 DAY),
    DATE_SUB(@now, INTERVAL 20 DAY), @admin_user, DATE_SUB(@now, INTERVAL 19 DAY),
    'Contract bracket 32 dùng demo chuyển vòng'
FROM demo_numbers_32
ON DUPLICATE KEY UPDATE status = 'APPROVED', final_payout_status = 'NOT_RELEASED';

INSERT INTO rounds
    (round_id, round_name, sequence_order, is_final, prediction_type,
     advancement_rule, start_date, end_date, description, max_races,
     max_entries, min_entries, status, head_referee_id,
     head_referee_assigned_at, expected_entries, planned_race_count,
     qualifiers_per_race, bracket_plan_version, advanced_at,
     transition_status, created_at, tournament_id, created_by)
VALUES
    (@round_1, 'Vòng 1', 1, 0, 'TOP3', 'Lấy Top 4 mỗi Race',
     @race_a_start, @race_b_end, 'Hai Race vòng loại, mỗi Race 16 entry',
     2, 16, 8, 'FINISHED', @referee_id, DATE_SUB(@now, INTERVAL 10 DAY),
     32, 2, 4, 1, NULL, 'READY', DATE_SUB(@now, INTERVAL 15 DAY),
     @tournament_id, @admin_user),
    (@round_2, 'Vòng 2 (Chung Kết)', 2, 1, 'TOP3', 'Xác định Top 3 chung cuộc',
     @final_start, @final_end, 'Final chờ hệ thống chuyển 8 Top 4 vào',
     1, 16, 8, 'SCHEDULING', @referee_id, DATE_SUB(@now, INTERVAL 10 DAY),
     8, 1, 0, 1, NULL, 'NOT_READY', DATE_SUB(@now, INTERVAL 15 DAY),
     @tournament_id, @admin_user)
ON DUPLICATE KEY UPDATE status = VALUES(status), transition_status = VALUES(transition_status), advanced_at = NULL;

INSERT INTO races
    (race_id, name, start_time, end_time, track_condition, distance,
     sequence_order, status, started_at, finished_at, schedule_published_at,
     prediction_open_at, prediction_close_at, round_id, created_by,
     started_by, inspection_finalized_at)
VALUES
    (@race_a, 'DEMO Bracket - Vòng 1 Race A', @race_a_start, @race_a_end,
     'TURF', 'MILE_1600M', 1, 'FINISHED', @race_a_start, @race_a_end,
     DATE_SUB(@race_a_start, INTERVAL 1 DAY), DATE_SUB(@race_a_start, INTERVAL 1 DAY),
     DATE_SUB(@race_a_start, INTERVAL 5 MINUTE), @round_1, @admin_user,
     @admin_user, DATE_SUB(@race_a_start, INTERVAL 30 MINUTE)),
    (@race_b, 'DEMO Bracket - Vòng 1 Race B', @race_b_start, @race_b_end,
     'TURF', 'MILE_1600M', 2, 'FINISHED', @race_b_start, @race_b_end,
     DATE_SUB(@race_b_start, INTERVAL 1 DAY), DATE_SUB(@race_b_start, INTERVAL 1 DAY),
     DATE_SUB(@race_b_start, INTERVAL 5 MINUTE), @round_1, @admin_user,
     @admin_user, DATE_SUB(@race_b_start, INTERVAL 30 MINUTE)),
    (@race_final, 'DEMO Bracket - Final', @final_start, @final_end,
     'TURF', 'MILE_1600M', 1, 'SCHEDULING', NULL, NULL, NULL,
     DATE_SUB(@final_start, INTERVAL 1 DAY), DATE_SUB(@final_start, INTERVAL 5 MINUTE),
     @round_2, @admin_user, NULL, NULL)
ON DUPLICATE KEY UPDATE status = VALUES(status), started_at = VALUES(started_at), finished_at = VALUES(finished_at);

INSERT INTO race_referees
    (race_referee_id, race_id, referee_id, assigned_by, assigned_at)
VALUES
    ('91000000-0000-0000-0000-000000000041', @race_a, @referee_id, @admin_user, DATE_SUB(@now, INTERVAL 10 DAY)),
    ('91000000-0000-0000-0000-000000000042', @race_b, @referee_id, @admin_user, DATE_SUB(@now, INTERVAL 10 DAY))
ON DUPLICATE KEY UPDATE assigned_at = VALUES(assigned_at);

INSERT INTO race_entries
    (entry_id, race_id, contract_id, lane_number, status,
     assigned_by, assigned_at, created_at)
SELECT
    CONCAT('ab000000-0000-0000-0000-', LPAD(n, 12, '0')),
    IF(n <= 16, @race_a, @race_b),
    CONCAT('72000000-0000-0000-0000-', LPAD(n, 12, '0')),
    IF(n <= 16, n, n - 16), 'FINISHED', @admin_user,
    DATE_SUB(@now, INTERVAL 10 DAY), DATE_SUB(@now, INTERVAL 10 DAY)
FROM demo_numbers_32
ON DUPLICATE KEY UPDATE status = 'FINISHED', lane_number = VALUES(lane_number);

INSERT INTO race_results
    (result_id, race_id, entry_id, finish_time, finish_position,
     prize_money, owner_prize_amount, jockey_prize_amount, prize_status,
     is_prize_paid, prize_paid_at, status, rating_change,
     recorded_by, recorded_at, updated_at)
SELECT
    CONCAT('cb000000-0000-0000-0000-', LPAD(n, 12, '0')),
    IF(n <= 16, @race_a, @race_b),
    CONCAT('ab000000-0000-0000-0000-', LPAD(n, 12, '0')),
    92 + IF(n <= 16, n, n - 16) / 10,
    IF(n <= 16, n, n - 16), 0, 0, 0, 'NotEligible', 0, NULL,
    'FINISHED',
    CASE IF(n <= 16, n, n - 16)
        WHEN 1 THEN 6 WHEN 2 THEN 2 WHEN 3 THEN 1 ELSE 0 END,
    @admin_user, IF(n <= 16, @race_a_end, @race_b_end),
    IF(n <= 16, @race_a_end, @race_b_end)
FROM demo_numbers_32
ON DUPLICATE KEY UPDATE
    finish_position = VALUES(finish_position),
    status = 'FINISHED',
    rating_change = VALUES(rating_change);

INSERT INTO race_reports
    (report_id, race_id, referee_id, summary, appeal_note, status,
     signed_by, signed_at, published_by, published_at, created_at)
VALUES
    ('db000000-0000-0000-0000-000000000041', @race_a, @referee_id,
     'Race A đã hoàn tất, Top 4 đủ điều kiện chuyển vòng.', 'Không có khiếu nại tồn đọng.',
     'Signed', @referee_id, DATE_ADD(@race_a_end, INTERVAL 30 MINUTE), NULL, NULL, @race_a_end),
    ('db000000-0000-0000-0000-000000000042', @race_b, @referee_id,
     'Race B đã hoàn tất, Top 4 đủ điều kiện chuyển vòng.', 'Không có khiếu nại tồn đọng.',
     'Signed', @referee_id, DATE_ADD(@race_b_end, INTERVAL 30 MINUTE), NULL, NULL, @race_b_end)
ON DUPLICATE KEY UPDATE status = 'Signed', published_by = NULL, published_at = NULL;

DROP TEMPORARY TABLE IF EXISTS demo_numbers_32;

SELECT 'Bracket jockey users' AS demo_group, COUNT(*) AS total
FROM users WHERE user_id LIKE '11000000-0000-0000-0000-%'
UNION ALL
SELECT 'Bracket horses', COUNT(*) FROM horses WHERE name LIKE 'Ngựa Bracket %'
UNION ALL
SELECT 'Bracket contracts', COUNT(*) FROM jockey_horse_contracts WHERE tournament_id = @tournament_id
UNION ALL
SELECT 'Bracket Round 1 entries', COUNT(*) FROM race_entries WHERE race_id IN (@race_a, @race_b)
UNION ALL
SELECT 'Bracket signed reports', COUNT(*) FROM race_reports WHERE race_id IN (@race_a, @race_b) AND status = 'Signed';

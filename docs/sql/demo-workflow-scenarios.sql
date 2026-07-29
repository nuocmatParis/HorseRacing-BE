-- ============================================================================
-- HRTMS WORKFLOW SCENARIOS (MySQL 8+)
-- Chạy SAU:
--   1) docs/sql/demo-test-data.sql
--   2) docs/sql/demo-full-coverage-extension.sql
-- Bổ sung giải theo phase: REGISTRATION_OPEN, JOCKEY_MATCHING, SCHEDULING
-- và thêm Referee/Vet/Medical Staff AVAILABLE để Admin phân công.
-- ============================================================================

USE SWP391_Project_HRTMS;

SET NAMES utf8mb4;
SET @now = NOW();
SET @demo_password = '$2a$12$AsBCvrsZ2yvqd7RyEflkfOGwfTewt8CSx40CKh0ZIZuD4WZ49wo4a';
SET @admin_user = '10000000-0000-0000-0000-000000000001';
SET @owner_id = '20000000-0000-0000-0000-000000000001';
SET @role_ref = (SELECT role_id FROM roles WHERE role_name = 'REFEREE' LIMIT 1);
SET @role_vet = (SELECT role_id FROM roles WHERE role_name = 'VETERINARIAN' LIMIT 1);
SET @role_med = (SELECT role_id FROM roles WHERE role_name = 'MEDICAL_STAFF' LIMIT 1);
SET @registration_tournament = '50000000-0000-0000-0000-000000000005';
SET @matching_tournament = '50000000-0000-0000-0000-000000000006';
SET @scheduling_tournament = '50000000-0000-0000-0000-000000000007';
SET @schedule_start = TIMESTAMP(DATE_ADD(CURRENT_DATE, INTERVAL 5 DAY), '08:00:00');
SET @schedule_end = DATE_ADD(@schedule_start, INTERVAL 30 MINUTE);

-- 1. Staff còn AVAILABLE để test phân công trên Scheduling Board.
INSERT INTO users
    (user_id, username, password, email, dob, gender, full_name, phone_number,
     image_url, status, created_at, last_login_at, role_id)
VALUES
    ('12000000-0000-0000-0000-000000000001', 'referee2', @demo_password, 'demo.ref2@hrtms.local', '1988-01-01', 'MALE', 'Trọng tài Available Demo', NULL, NULL, 'ACTIVE', @now, @now, @role_ref),
    ('12000000-0000-0000-0000-000000000002', 'vet2', @demo_password, 'demo.vet2@hrtms.local', '1987-02-02', 'FEMALE', 'Thú y Available Demo', NULL, NULL, 'ACTIVE', @now, @now, @role_vet),
    ('12000000-0000-0000-0000-000000000003', 'medical2', @demo_password, 'demo.med2@hrtms.local', '1989-03-03', 'MALE', 'Y tế Available Demo', NULL, NULL, 'ACTIVE', @now, @now, @role_med)
ON DUPLICATE KEY UPDATE
    username = VALUES(username),
    password = VALUES(password),
    status = 'ACTIVE';

INSERT INTO referees
    (referee_id, user_id, certification_level, years_of_service, status, created_at)
VALUES
    ('23000000-0000-0000-0000-000000000001', '12000000-0000-0000-0000-000000000001', 'REFEREE', 6, 'AVAILABLE', @now)
ON DUPLICATE KEY UPDATE status = 'AVAILABLE';

INSERT INTO veterinarians
    (vet_id, user_id, specialization, years_of_service, status, created_at)
VALUES
    ('23000000-0000-0000-0000-000000000002', '12000000-0000-0000-0000-000000000002', 'Equine Medicine', 5, 'AVAILABLE', @now)
ON DUPLICATE KEY UPDATE status = 'AVAILABLE';

INSERT INTO medical_staffs
    (med_staff_id, user_id, certification, years_of_service, status, created_at)
VALUES
    ('23000000-0000-0000-0000-000000000003', '12000000-0000-0000-0000-000000000003', 'Sports Medicine', 5, 'AVAILABLE', @now)
ON DUPLICATE KEY UPDATE status = 'AVAILABLE';

-- 2. Ba Tournament chuyên biệt cho từng phase nghiệp vụ.
INSERT INTO tournaments
    (tournament_id, name, description, start_date, end_date, finished_at,
     location, registration_fee, system_contract_fee, total_prize_pool,
     allowed_breed, min_horse_age, max_horse_age,
     prediction_top1_correct_points, prediction_top3_exact_position_points,
     prediction_top3_correct_horse_points, prediction_top3_perfect_bonus_points,
     prediction_open_minutes_before, prediction_close_minutes_before,
     prediction_card_open_hours_before_first_race,
     inspection_open_minutes_before, inspection_close_minutes_before,
     min_race_interval_minutes,
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
    (@registration_tournament,
     'DEMO 5 - Đang mở đăng ký',
     'Owner đăng ký ngựa và Jockey đăng ký trực tiếp, không cần Admin duyệt Jockey.',
     DATE_ADD(CURRENT_DATE, INTERVAL 15 DAY), DATE_ADD(CURRENT_DATE, INTERVAL 17 DAY), NULL,
     'Trường đua Demo Registration', 100000, 50000, 5000000,
     'THOROUGHBRED', 2, 12, 100, 30, 10, 50, 120, 5, 24,
     90, 30, 35, 0, 30, 30, '08:00:00', '18:00:00',
     0, NULL, NULL, 'OPEN', 'REGISTRATION_OPEN', DATE_SUB(@now, INTERVAL 2 DAY),
     DATE_SUB(@now, INTERVAL 1 DAY), DATE_SUB(@now, INTERVAL 1 DAY),
     DATE_ADD(@now, INTERVAL 3 DAY), DATE_ADD(@now, INTERVAL 7 DAY),
     DATE_ADD(@now, INTERVAL 11 DAY), DATE_ADD(@now, INTERVAL 13 DAY),
      DATE_ADD(@now, INTERVAL 15 DAY), NULL, 'CLASS_3', 'MILE_1600M',
     135, 115, 1.5, 0, 8, 8, 8, 1, 1, 'CONFIRMED', 1, @admin_user),

    (@matching_tournament,
     'DEMO 6 - Đang ghép Kỵ sĩ',
     'Owner tìm Kỵ sĩ, gửi lời mời; Kỵ sĩ chấp nhận hoặc từ chối; contract gửi Admin duyệt.',
     DATE_ADD(CURRENT_DATE, INTERVAL 12 DAY), DATE_ADD(CURRENT_DATE, INTERVAL 14 DAY), NULL,
     'Trường đua Demo Matching', 100000, 50000, 5000000,
     'THOROUGHBRED', 2, 12, 100, 30, 10, 50, 120, 5, 24,
     90, 30, 35, 0, 30, 30, '08:00:00', '18:00:00',
     0, NULL, NULL, 1, 'ONGOING', 'JOCKEY_MATCHING', DATE_SUB(@now, INTERVAL 12 DAY),
     DATE_SUB(@now, INTERVAL 10 DAY), DATE_SUB(@now, INTERVAL 12 DAY),
     DATE_SUB(@now, INTERVAL 8 DAY), DATE_SUB(@now, INTERVAL 4 DAY),
     DATE_ADD(@now, INTERVAL 3 DAY), DATE_ADD(@now, INTERVAL 7 DAY),
      DATE_ADD(@now, INTERVAL 12 DAY), NULL, 'CLASS_5', 'SPRINT_1200M',
     135, 115, 1.5, 0, 8, 8, 8, 1, 1, 'CONFIRMED', 1, @admin_user),

    (@scheduling_tournament,
     'DEMO 7 - Đang xếp lịch',
     'Admin auto-assign contract, random lane, phân công staff và publish schedule.',
     DATE_ADD(CURRENT_DATE, INTERVAL 5 DAY), DATE_ADD(CURRENT_DATE, INTERVAL 7 DAY), NULL,
     'Trường đua Demo Scheduling', 100000, 50000, 5000000,
     'THOROUGHBRED', 2, 12, 100, 30, 10, 50, 120, 5, 24,
     90, 30, 35, 0, 30, 30, '08:00:00', '18:00:00',
     0, NULL, NULL, 1, 'ONGOING', 'SCHEDULING', DATE_SUB(@now, INTERVAL 20 DAY),
     DATE_SUB(@now, INTERVAL 18 DAY), DATE_SUB(@now, INTERVAL 20 DAY),
     DATE_SUB(@now, INTERVAL 16 DAY), DATE_SUB(@now, INTERVAL 12 DAY),
     DATE_SUB(@now, INTERVAL 8 DAY), DATE_SUB(@now, INTERVAL 4 DAY),
      @schedule_start, 'Vòng 1 (Chung Kết)', 'CLASS_1', 'MILE_1600M',
     135, 115, 1.5, 0, 8, 8, 8, 1, 1, 'LOCKED', 1, @admin_user)
ON DUPLICATE KEY UPDATE phase = VALUES(phase), status = VALUES(status), current_round_name = VALUES(current_round_name);

-- 3. Matching scenario: một ngựa APPROVED và bốn Kỵ sĩ để Owner lựa chọn.
INSERT INTO horse_tournament_registrations
    (horse_tournament_reg_id, tournament_id, horse_id, owner_id, status,
     submitted_at, reviewed_by, reviewed_at, note,
     rating_at_registration, race_class_at_registration)
VALUES
    ('64000000-0000-0000-0000-000000000001', @matching_tournament,
     '40000000-0000-0000-0000-000000000109', @owner_id, 'APPROVED',
     DATE_SUB(@now, INTERVAL 8 DAY), @admin_user, DATE_SUB(@now, INTERVAL 4 DAY),
     'Ngựa sẵn sàng tìm Kỵ sĩ', 35, 'CLASS_5')
ON DUPLICATE KEY UPDATE status = 'APPROVED';

INSERT INTO jockey_tournament_registrations
    (jockey_tournament_reg_id, tournament_id, jockey_id, status,
     submitted_at, reviewed_by, reviewed_at, note, hire_fee)
VALUES
    ('65000000-0000-0000-0000-000000000001', @matching_tournament, '22000000-0000-0000-0000-000000000001', 'APPROVED', DATE_SUB(@now, INTERVAL 6 DAY), NULL, DATE_SUB(@now, INTERVAL 6 DAY), 'Tự động chấp nhận đăng ký', 1000000),
    ('65000000-0000-0000-0000-000000000002', @matching_tournament, '22000000-0000-0000-0000-000000000002', 'APPROVED', DATE_SUB(@now, INTERVAL 6 DAY), NULL, DATE_SUB(@now, INTERVAL 6 DAY), 'Tự động chấp nhận đăng ký', 1000000),
    ('65000000-0000-0000-0000-000000000003', @matching_tournament, '22000000-0000-0000-0000-000000000003', 'APPROVED', DATE_SUB(@now, INTERVAL 6 DAY), NULL, DATE_SUB(@now, INTERVAL 6 DAY), 'Tự động chấp nhận đăng ký', 1000000),
    ('65000000-0000-0000-0000-000000000004', @matching_tournament, '22000000-0000-0000-0000-000000000004', 'APPROVED', DATE_SUB(@now, INTERVAL 6 DAY), NULL, DATE_SUB(@now, INTERVAL 6 DAY), 'Tự động chấp nhận đăng ký', 1000000)
ON DUPLICATE KEY UPDATE status = 'APPROVED';

-- 4. Scheduling scenario: clone 8 registrations và contract APPROVED, chưa tạo RaceEntry.
INSERT INTO horse_tournament_registrations
    (horse_tournament_reg_id, tournament_id, horse_id, owner_id, status,
     submitted_at, reviewed_by, reviewed_at, note,
     rating_at_registration, race_class_at_registration)
SELECT
    REPLACE(horse_tournament_reg_id, '60000000-0000-0000-0000-0000000001', '66000000-0000-0000-0000-0000000001'),
    @scheduling_tournament, horse_id, owner_id, 'APPROVED',
    DATE_SUB(@now, INTERVAL 16 DAY), @admin_user, DATE_SUB(@now, INTERVAL 12 DAY),
    'Hồ sơ approved dùng Scheduling Board', rating_at_registration, race_class_at_registration
FROM horse_tournament_registrations
WHERE tournament_id = '50000000-0000-0000-0000-000000000001'
ON DUPLICATE KEY UPDATE status = 'APPROVED';

INSERT INTO jockey_tournament_registrations
    (jockey_tournament_reg_id, tournament_id, jockey_id, status,
     submitted_at, reviewed_by, reviewed_at, note, hire_fee)
SELECT
    REPLACE(jockey_tournament_reg_id, '61000000-0000-0000-0000-0000000001', '67000000-0000-0000-0000-0000000001'),
    @scheduling_tournament, jockey_id, 'APPROVED', DATE_SUB(@now, INTERVAL 14 DAY),
    NULL, DATE_SUB(@now, INTERVAL 14 DAY), 'Tự động chấp nhận đăng ký', hire_fee
FROM jockey_tournament_registrations
WHERE tournament_id = '50000000-0000-0000-0000-000000000001'
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
    REPLACE(contract_id, '70000000-0000-0000-0000-0000000001', '73000000-0000-0000-0000-0000000001'),
    @scheduling_tournament,
    REPLACE(horse_tournament_reg_id, '60000000-0000-0000-0000-0000000001', '66000000-0000-0000-0000-0000000001'),
    REPLACE(jockey_tournament_reg_id, '61000000-0000-0000-0000-0000000001', '67000000-0000-0000-0000-0000000001'),
    owner_id, horse_id, jockey_id, hire_fee, advance_percent, final_percent,
    advance_paid_amount, escrow_amount, system_contract_fee,
    owner_prize_share_percent, jockey_prize_share_percent,
    'PAID', 'PARTIALLY_RELEASED', 'PAID', 'NOT_RELEASED', 'APPROVED',
    DATE_SUB(@now, INTERVAL 10 DAY), NULL, DATE_SUB(@now, INTERVAL 14 DAY),
    DATE_SUB(@now, INTERVAL 13 DAY), DATE_SUB(@now, INTERVAL 13 DAY),
    DATE_SUB(@now, INTERVAL 12 DAY), @admin_user, DATE_SUB(@now, INTERVAL 11 DAY),
    'Contract approved chờ phân vào Race'
FROM jockey_horse_contracts
WHERE tournament_id = '50000000-0000-0000-0000-000000000001'
ON DUPLICATE KEY UPDATE status = 'APPROVED';

INSERT INTO rounds
    (round_id, round_name, sequence_order, is_final, prediction_type,
     advancement_rule, start_date, end_date, description, max_races,
     max_entries, min_entries, status, head_referee_id,
     head_referee_assigned_at, expected_entries, planned_race_count,
     qualifiers_per_race, bracket_plan_version, advanced_at,
     transition_status, created_at, tournament_id, created_by)
VALUES
    ('80000000-0000-0000-0000-000000000007', 'Vòng 1 (Chung Kết)', 1, 1,
     'TOP3', 'Xác định Top 3 chung cuộc', @schedule_start, @schedule_end,
     'Round chờ Admin xếp entry, lane và staff', 1, 16, 8, 'SCHEDULING',
     NULL, NULL, 8, 1, 0, 1, NULL, 'NOT_READY', DATE_SUB(@now, INTERVAL 5 DAY),
     @scheduling_tournament, @admin_user)
ON DUPLICATE KEY UPDATE status = 'SCHEDULING', head_referee_id = NULL;

INSERT INTO races
    (race_id, name, start_time, end_time, track_condition, distance,
     sequence_order, status, started_at, finished_at, schedule_published_at,
     prediction_open_at, prediction_close_at, round_id, created_by,
     started_by, inspection_finalized_at)
VALUES
    ('90000000-0000-0000-0000-000000000007', 'DEMO Scheduling Race',
     @schedule_start, @schedule_end, 'TURF', 'MILE_1600M', 1, 'SCHEDULING',
     NULL, NULL, NULL, DATE_SUB(@schedule_start, INTERVAL 1 DAY),
     DATE_SUB(@schedule_start, INTERVAL 5 MINUTE),
     '80000000-0000-0000-0000-000000000007', @admin_user, NULL, NULL)
ON DUPLICATE KEY UPDATE status = 'SCHEDULING', schedule_published_at = NULL;

-- Reset phần do Admin thao tác trên Scheduling Board để script có thể chạy lại.
DELETE FROM race_entries WHERE race_id = '90000000-0000-0000-0000-000000000007';
DELETE FROM race_referees WHERE race_id = '90000000-0000-0000-0000-000000000007';
DELETE FROM race_inspection_staff_assignments WHERE race_id = '90000000-0000-0000-0000-000000000007';

SELECT 'Registration-open tournaments' AS demo_group, COUNT(*) AS total
FROM tournaments WHERE phase = 'REGISTRATION_OPEN' AND tournament_id = @registration_tournament
UNION ALL
SELECT 'Jockey-matching tournaments', COUNT(*) FROM tournaments WHERE phase = 'JOCKEY_MATCHING' AND tournament_id = @matching_tournament
UNION ALL
SELECT 'Scheduling approved contracts', COUNT(*) FROM jockey_horse_contracts WHERE tournament_id = @scheduling_tournament AND status = 'APPROVED'
UNION ALL
SELECT 'Available staff',
    (SELECT COUNT(*) FROM referees WHERE status = 'AVAILABLE')
  + (SELECT COUNT(*) FROM veterinarians WHERE status = 'AVAILABLE')
  + (SELECT COUNT(*) FROM medical_staffs WHERE status = 'AVAILABLE');

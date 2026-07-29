-- ============================================================================
-- HRTMS - DEMO ADMIN LẬP LỊCH, PHÂN ENTRY, LANE VÀ NHÂN SỰ (MYSQL 8+)
-- ============================================================================
-- Chỉ dùng cho database LOCAL/TEST.
--
-- Trạng thái ban đầu:
--   - Tournament: ONGOING / SCHEDULING.
--   - 1 Final Round và 1 Race đang SCHEDULING.
--   - 8 contract APPROVED.
--   - CHƯA có RaceEntry.
--   - CHƯA có lane.
--   - CHƯA có Head Referee.
--   - CHƯA có Race Referee.
--   - CHƯA có Vet/Medical assignment.
--   - CHƯA publish schedule.
--
-- Admin phải tự thao tác:
--   1. Auto-assign 8 contract vào race.
--   2. Random lane.
--   3. Gán Head Referee cho Round.
--   4. Gán đúng 1 Race Referee cho Race.
--   5. Gán Veterinarian + Medical Staff.
--   6. Publish schedule.
--
-- Tài khoản Admin: dmadmin / 12345678
-- Nhân sự:
--   - Race Referee đề xuất: schref1 / 12345678
--   - Head Referee đề xuất: schref2 / 12345678
--   - Veterinarian: schvet1 / 12345678
--   - Medical Staff: schmed1 / 12345678
--
-- UUID cố định:
--   Tournament : e9000000-0000-0000-0000-000000000001
--   Round      : e9010000-0000-0000-0000-000000000001
--   Race       : e9020000-0000-0000-0000-000000000001
--
-- Có thể chạy lại toàn bộ file để reset riêng giải demo này.
-- ============================================================================

USE SWP391_Project_HRTMS;
SET NAMES utf8mb4;

SET @OLD_FOREIGN_KEY_CHECKS = @@FOREIGN_KEY_CHECKS;
SET @OLD_SQL_SAFE_UPDATES = @@SQL_SAFE_UPDATES;
SET FOREIGN_KEY_CHECKS = 0;
SET SQL_SAFE_UPDATES = 0;

SET @seed_now = NOW();
SET @created_at = DATE_SUB(@seed_now, INTERVAL 30 DAY);
SET @demo_password = '$2a$12$ZGrUyKDU0UvqY0kpswOtoO58uurKVC2yVAA0iTlcnYI4pmPb18mBS';

SET @tournament_id = 'e9000000-0000-0000-0000-000000000001';
SET @round_id = 'e9010000-0000-0000-0000-000000000001';
SET @race_id = 'e9020000-0000-0000-0000-000000000001';
SET @admin_seed_id = 'e0000000-0000-0000-0000-000000000001';

-- Race diễn ra ngày kia, từ 09:00 đến 09:30.
SET @race_start = TIMESTAMP(DATE_ADD(CURDATE(), INTERVAL 2 DAY), '09:00:00');
SET @race_end = DATE_ADD(@race_start, INTERVAL 30 MINUTE);

-- ============================================================================
-- 1. CLEANUP ĐÚNG PHẠM VI GIẢI DEMO
-- ============================================================================

DELETE FROM notification_deliveries
WHERE notification_id IN (
    SELECT notification_id
    FROM notifications
    WHERE related_id IN (@tournament_id, @round_id, @race_id)
);

DELETE FROM notifications
WHERE related_id IN (@tournament_id, @round_id, @race_id);

DELETE FROM notification_events
WHERE aggregate_id IN (@tournament_id, @round_id, @race_id);

DELETE FROM horse_rating_histories WHERE race_id = @race_id;

DELETE FROM prediction_detail
WHERE prediction_id IN (
    SELECT prediction_id FROM predictions WHERE race_id = @race_id
);
DELETE FROM predictions WHERE race_id = @race_id;

DELETE FROM appeal_evidences
WHERE appeal_id IN (
    SELECT appeal_id
    FROM appeals
    WHERE entry_id IN (
        SELECT entry_id FROM race_entries WHERE race_id = @race_id
    )
);
DELETE FROM appeals
WHERE entry_id IN (
    SELECT entry_id FROM race_entries WHERE race_id = @race_id
);
DELETE FROM violations
WHERE entry_id IN (
    SELECT entry_id FROM race_entries WHERE race_id = @race_id
);

DELETE FROM race_reports WHERE race_id = @race_id;
DELETE FROM race_results WHERE race_id = @race_id;
DELETE FROM horse_inspections
WHERE entry_id IN (
    SELECT entry_id FROM race_entries WHERE race_id = @race_id
);
DELETE FROM jockey_inspections
WHERE entry_id IN (
    SELECT entry_id FROM race_entries WHERE race_id = @race_id
);

DELETE FROM race_referees WHERE race_id = @race_id;
DELETE FROM race_inspection_staff_assignments WHERE race_id = @race_id;
DELETE FROM race_entries WHERE race_id = @race_id;
DELETE FROM races WHERE round_id = @round_id;
DELETE FROM rounds WHERE tournament_id = @tournament_id;

DELETE FROM wallet_transactions
WHERE contract_id IN (
    SELECT contract_id
    FROM jockey_horse_contracts
    WHERE tournament_id = @tournament_id
);

DELETE FROM invoices
WHERE contract_id IN (
        SELECT contract_id
        FROM jockey_horse_contracts
        WHERE tournament_id = @tournament_id
    )
   OR tournament_reg_id IN (
        SELECT horse_tournament_reg_id
        FROM horse_tournament_registrations
        WHERE tournament_id = @tournament_id
    )
   OR jockey_tournament_reg_id IN (
        SELECT jockey_tournament_reg_id
        FROM jockey_tournament_registrations
        WHERE tournament_id = @tournament_id
    );

DELETE FROM jockey_horse_contracts WHERE tournament_id = @tournament_id;
DELETE FROM horse_tournament_registrations WHERE tournament_id = @tournament_id;
DELETE FROM jockey_tournament_registrations WHERE tournament_id = @tournament_id;
DELETE FROM tournament_eligibility WHERE tournament_id = @tournament_id;
DELETE FROM tournament_phase_config WHERE tournament_id = @tournament_id;
DELETE FROM prize_structures WHERE tournament_id = @tournament_id;
DELETE FROM tournaments WHERE tournament_id = @tournament_id;

DELETE FROM horses
WHERE horse_id LIKE 'e9310000-0000-0000-0000-%';

-- ============================================================================
-- 2. ROLE VÀ TÀI KHOẢN
-- ============================================================================

INSERT INTO roles (role_id, role_name, description, is_active, created_at)
VALUES
    ('e0000000-0000-0000-0001-000000000001', 'ADMIN', 'Quản trị hệ thống', 1, @created_at),
    ('e0000000-0000-0000-0001-000000000002', 'HORSE_OWNER', 'Chủ ngựa', 1, @created_at),
    ('e0000000-0000-0000-0001-000000000003', 'JOCKEY', 'Kỵ sĩ', 1, @created_at),
    ('e0000000-0000-0000-0001-000000000005', 'REFEREE', 'Trọng tài', 1, @created_at),
    ('e0000000-0000-0000-0001-000000000006', 'VETERINARIAN', 'Bác sĩ thú y', 1, @created_at),
    ('e0000000-0000-0000-0001-000000000007', 'MEDICAL_STAFF', 'Nhân viên y tế', 1, @created_at)
ON DUPLICATE KEY UPDATE
    description = VALUES(description),
    is_active = 1;

SET @role_admin = (
    SELECT role_id FROM roles WHERE role_name = 'ADMIN' LIMIT 1
);
SET @role_owner = (
    SELECT role_id FROM roles WHERE role_name = 'HORSE_OWNER' LIMIT 1
);
SET @role_jockey = (
    SELECT role_id FROM roles WHERE role_name = 'JOCKEY' LIMIT 1
);
SET @role_referee = (
    SELECT role_id FROM roles WHERE role_name = 'REFEREE' LIMIT 1
);
SET @role_vet = (
    SELECT role_id FROM roles WHERE role_name = 'VETERINARIAN' LIMIT 1
);
SET @role_medical = (
    SELECT role_id FROM roles WHERE role_name = 'MEDICAL_STAFF' LIMIT 1
);

INSERT INTO users
    (user_id, username, password, email, dob, gender, full_name,
     phone_number, image_url, status, created_at, last_login_at, role_id)
VALUES
    (@admin_seed_id, 'dmadmin', @demo_password, 'dmadmin@hrtms.test',
     '1990-01-01', 'MALE', 'Admin Demo', NULL, NULL,
     'ACTIVE', @created_at, @seed_now, @role_admin),

    ('e9100000-0000-0000-0000-000000000001', 'schowner1', @demo_password,
     'schowner1@hrtms.test', '1981-01-01', 'MALE', 'Scheduling Owner 1',
     NULL, NULL, 'ACTIVE', @created_at, @seed_now, @role_owner),
    ('e9100000-0000-0000-0000-000000000002', 'schowner2', @demo_password,
     'schowner2@hrtms.test', '1982-02-02', 'FEMALE', 'Scheduling Owner 2',
     NULL, NULL, 'ACTIVE', @created_at, @seed_now, @role_owner),
    ('e9100000-0000-0000-0000-000000000003', 'schowner3', @demo_password,
     'schowner3@hrtms.test', '1983-03-03', 'MALE', 'Scheduling Owner 3',
     NULL, NULL, 'ACTIVE', @created_at, @seed_now, @role_owner),
    ('e9100000-0000-0000-0000-000000000004', 'schowner4', @demo_password,
     'schowner4@hrtms.test', '1984-04-04', 'FEMALE', 'Scheduling Owner 4',
     NULL, NULL, 'ACTIVE', @created_at, @seed_now, @role_owner),
    ('e9100000-0000-0000-0000-000000000005', 'schowner5', @demo_password,
     'schowner5@hrtms.test', '1985-05-05', 'MALE', 'Scheduling Owner 5',
     NULL, NULL, 'ACTIVE', @created_at, @seed_now, @role_owner),

    ('e9200000-0000-0000-0000-000000000001', 'schjockey1', @demo_password,
     'schjockey1@hrtms.test', '1995-01-01', 'MALE', 'Scheduling Jockey 1',
     NULL, NULL, 'ACTIVE', @created_at, @seed_now, @role_jockey),
    ('e9200000-0000-0000-0000-000000000002', 'schjockey2', @demo_password,
     'schjockey2@hrtms.test', '1995-02-02', 'FEMALE', 'Scheduling Jockey 2',
     NULL, NULL, 'ACTIVE', @created_at, @seed_now, @role_jockey),
    ('e9200000-0000-0000-0000-000000000003', 'schjockey3', @demo_password,
     'schjockey3@hrtms.test', '1995-03-03', 'MALE', 'Scheduling Jockey 3',
     NULL, NULL, 'ACTIVE', @created_at, @seed_now, @role_jockey),
    ('e9200000-0000-0000-0000-000000000004', 'schjockey4', @demo_password,
     'schjockey4@hrtms.test', '1995-04-04', 'FEMALE', 'Scheduling Jockey 4',
     NULL, NULL, 'ACTIVE', @created_at, @seed_now, @role_jockey),
    ('e9200000-0000-0000-0000-000000000005', 'schjockey5', @demo_password,
     'schjockey5@hrtms.test', '1995-05-05', 'MALE', 'Scheduling Jockey 5',
     NULL, NULL, 'ACTIVE', @created_at, @seed_now, @role_jockey),
    ('e9200000-0000-0000-0000-000000000006', 'schjockey6', @demo_password,
     'schjockey6@hrtms.test', '1995-06-06', 'FEMALE', 'Scheduling Jockey 6',
     NULL, NULL, 'ACTIVE', @created_at, @seed_now, @role_jockey),
    ('e9200000-0000-0000-0000-000000000007', 'schjockey7', @demo_password,
     'schjockey7@hrtms.test', '1995-07-07', 'MALE', 'Scheduling Jockey 7',
     NULL, NULL, 'ACTIVE', @created_at, @seed_now, @role_jockey),
    ('e9200000-0000-0000-0000-000000000008', 'schjockey8', @demo_password,
     'schjockey8@hrtms.test', '1995-08-08', 'FEMALE', 'Scheduling Jockey 8',
     NULL, NULL, 'ACTIVE', @created_at, @seed_now, @role_jockey),

    ('e9400000-0000-0000-0000-000000000001', 'schref1', @demo_password,
     'schref1@hrtms.test', '1980-01-10', 'MALE', 'Scheduling Race Referee',
     NULL, NULL, 'ACTIVE', @created_at, @seed_now, @role_referee),
    ('e9400000-0000-0000-0000-000000000002', 'schref2', @demo_password,
     'schref2@hrtms.test', '1978-02-20', 'FEMALE', 'Scheduling Head Referee',
     NULL, NULL, 'ACTIVE', @created_at, @seed_now, @role_referee),
    ('e9400000-0000-0000-0000-000000000003', 'schvet1', @demo_password,
     'schvet1@hrtms.test', '1982-03-15', 'FEMALE', 'Scheduling Veterinarian',
     NULL, NULL, 'ACTIVE', @created_at, @seed_now, @role_vet),
    ('e9400000-0000-0000-0000-000000000004', 'schmed1', @demo_password,
     'schmed1@hrtms.test', '1983-04-16', 'MALE', 'Scheduling Medical Staff',
     NULL, NULL, 'ACTIVE', @created_at, @seed_now, @role_medical)
ON DUPLICATE KEY UPDATE
    password = VALUES(password),
    full_name = VALUES(full_name),
    status = 'ACTIVE',
    role_id = VALUES(role_id);

SET @admin_user_id = (
    SELECT user_id FROM users WHERE username = 'dmadmin' LIMIT 1
);

-- ============================================================================
-- 3. PROFILE OWNER, JOCKEY VÀ NHÂN SỰ
-- ============================================================================

INSERT INTO horse_owners (owner_id, user_id, farm_name, address, created_at)
VALUES
    ('e9110000-0000-0000-0000-000000000001',
     'e9100000-0000-0000-0000-000000000001',
     'Scheduling Farm 1', 'TP.HCM', @created_at),
    ('e9110000-0000-0000-0000-000000000002',
     'e9100000-0000-0000-0000-000000000002',
     'Scheduling Farm 2', 'Hà Nội', @created_at),
    ('e9110000-0000-0000-0000-000000000003',
     'e9100000-0000-0000-0000-000000000003',
     'Scheduling Farm 3', 'Đà Nẵng', @created_at),
    ('e9110000-0000-0000-0000-000000000004',
     'e9100000-0000-0000-0000-000000000004',
     'Scheduling Farm 4', 'Huế', @created_at),
    ('e9110000-0000-0000-0000-000000000005',
     'e9100000-0000-0000-0000-000000000005',
     'Scheduling Farm 5', 'Cần Thơ', @created_at)
ON DUPLICATE KEY UPDATE
    farm_name = VALUES(farm_name),
    address = VALUES(address);

INSERT INTO jockeys
    (jockey_id, user_id, height, weight, experience_years, specialization,
     status, total_races, total_wins, jockey_tier, tier_updated_at, created_at)
VALUES
    ('e9210000-0000-0000-0000-000000000001',
     'e9200000-0000-0000-0000-000000000001',
     1.60, 48.0, 6, 'MILE', 'AVAILABLE', 20, 4, 'PROFESSIONAL',
     @created_at, @created_at),
    ('e9210000-0000-0000-0000-000000000002',
     'e9200000-0000-0000-0000-000000000002',
     1.61, 49.0, 5, 'MILE', 'AVAILABLE', 18, 3, 'PROFESSIONAL',
     @created_at, @created_at),
    ('e9210000-0000-0000-0000-000000000003',
     'e9200000-0000-0000-0000-000000000003',
     1.62, 50.0, 7, 'MILE', 'AVAILABLE', 28, 7, 'ELITE',
     @created_at, @created_at),
    ('e9210000-0000-0000-0000-000000000004',
     'e9200000-0000-0000-0000-000000000004',
     1.63, 51.0, 4, 'MILE', 'AVAILABLE', 14, 2, 'JUNIOR',
     @created_at, @created_at),
    ('e9210000-0000-0000-0000-000000000005',
     'e9200000-0000-0000-0000-000000000005',
     1.64, 52.0, 8, 'MILE', 'AVAILABLE', 35, 10, 'ELITE',
     @created_at, @created_at),
    ('e9210000-0000-0000-0000-000000000006',
     'e9200000-0000-0000-0000-000000000006',
     1.59, 48.5, 3, 'MILE', 'AVAILABLE', 10, 1, 'JUNIOR',
     @created_at, @created_at),
    ('e9210000-0000-0000-0000-000000000007',
     'e9200000-0000-0000-0000-000000000007',
     1.65, 53.0, 6, 'MILE', 'AVAILABLE', 24, 6, 'PROFESSIONAL',
     @created_at, @created_at),
    ('e9210000-0000-0000-0000-000000000008',
     'e9200000-0000-0000-0000-000000000008',
     1.58, 47.5, 5, 'MILE', 'AVAILABLE', 19, 4, 'PROFESSIONAL',
     @created_at, @created_at)
ON DUPLICATE KEY UPDATE
    status = 'AVAILABLE',
    weight = VALUES(weight),
    experience_years = VALUES(experience_years);

INSERT INTO referees
    (referee_id, user_id, certification_level, years_of_service,
     status, created_at)
VALUES
    ('e9300000-0000-0000-0000-000000000001',
     'e9400000-0000-0000-0000-000000000001',
     'RACE_REFEREE', 8, 'AVAILABLE', @created_at),
    ('e9300000-0000-0000-0000-000000000002',
     'e9400000-0000-0000-0000-000000000002',
     'HEAD_REFEREE', 12, 'AVAILABLE', @created_at)
ON DUPLICATE KEY UPDATE
    status = 'AVAILABLE',
    certification_level = VALUES(certification_level);

INSERT INTO veterinarians
    (vet_id, user_id, specialization, years_of_service, status, created_at)
VALUES
    ('e9300000-0000-0000-0000-000000000003',
     'e9400000-0000-0000-0000-000000000003',
     'Equine Medicine', 9, 'AVAILABLE', @created_at)
ON DUPLICATE KEY UPDATE status = 'AVAILABLE';

INSERT INTO medical_staffs
    (med_staff_id, user_id, certification, years_of_service,
     status, created_at)
VALUES
    ('e9300000-0000-0000-0000-000000000004',
     'e9400000-0000-0000-0000-000000000004',
     'SPORT_MEDICINE', 7, 'AVAILABLE', @created_at)
ON DUPLICATE KEY UPDATE status = 'AVAILABLE';

-- ============================================================================
-- 4. TOURNAMENT ĐANG Ở PHASE SCHEDULING
-- ============================================================================

INSERT INTO tournaments (
    tournament_id, name, description, start_date, end_date, finished_at,
    location, image_url, registration_fee, system_contract_fee,
    total_prize_pool, allowed_breed, min_horse_age, max_horse_age,
    prediction_top1_correct_points, prediction_top3_exact_position_points,
    prediction_top3_correct_horse_points, prediction_top3_perfect_bonus_points,
    rating_first_min, rating_first_max, rating_second_min, rating_second_max,
    rating_third_min, rating_third_max,
    rating_fourth_fifth_min, rating_fourth_fifth_max,
    rating_other_min, rating_other_max,
    rating_disqualified_min, rating_disqualified_max,
    rating_policy_version, rating_policy_locked_at,
    prediction_open_minutes_before, prediction_close_minutes_before,
    inspection_open_minutes_before, inspection_close_minutes_before,
    min_race_interval_minutes, start_late_tolerance_minutes,
    default_race_operational_minutes, race_day_start_time, race_day_end_time,
    status, phase, created_at, published_at,
    registration_open_at, registration_close_at, review_deadline_at,
    jockey_matching_deadline_at, scheduling_deadline_at,
    competition_start_at, current_round_name,
    race_class, distance, track_condition,
    top_weight_lbs, min_weight_lbs, equipment_weight_kg, handicap_enabled,
    max_approved_horses, max_approved_jockeys, max_approved_entries,
    qualifiers_per_race, max_entries_per_race, created_by
) VALUES (
    @tournament_id,
    'DEMO ADMIN - Đang lập lịch',
    'Giải có 8 contract APPROVED để Admin phân entry, lane và nhân sự.',
    DATE(@race_start),
    DATE(@race_end),
    NULL,
    'Scheduling Demo Track',
    NULL,
    500000,
    100000,
    10000000,
    'THOROUGHBRED',
    3,
    8,
    100, 30, 10, 50,
    6, 12, 2, 5, 1, 4, 0, 2, -8, 0, -8, 0,
    1,
    @seed_now,
    1440,
    5,
    90,
    30,
    35,
    30,
    30,
    '08:00:00',
    '17:00:00',
    'ONGOING',
    'SCHEDULING',
    DATE_SUB(@seed_now, INTERVAL 20 DAY),
    DATE_SUB(@seed_now, INTERVAL 15 DAY),
    DATE_SUB(@seed_now, INTERVAL 20 DAY),
    DATE_SUB(@seed_now, INTERVAL 17 DAY),
    DATE_SUB(@seed_now, INTERVAL 14 DAY),
    DATE_SUB(@seed_now, INTERVAL 10 DAY),
    DATE_SUB(@race_start, INTERVAL 1 DAY),
    @race_start,
    'Chung kết',
    'CLASS_4',
    'MILE_1600M',
    'TURF',
    135,
    115,
    1.5,
    0,
    8,
    8,
    8,
    4,
    8,
    @admin_user_id
);

INSERT INTO prize_structures
    (prize_structure_id, prize_rank, percentage,
     fixed_amount, is_active, tournament_id)
VALUES
    ('e9030000-0000-0000-0000-000000000001',
     1, 50, 0, 1, @tournament_id),
    ('e9030000-0000-0000-0000-000000000002',
     2, 30, 0, 1, @tournament_id),
    ('e9030000-0000-0000-0000-000000000003',
     3, 20, 0, 1, @tournament_id);

INSERT INTO tournament_eligibility
    (eligibility_id, target_type, condition_name, condition_operator,
     condition_value, is_active, tournament_id)
VALUES
    ('e9040000-0000-0000-0000-000000000001',
     'HORSE', 'AGE', 'GREATER_THAN_OR_EQUAL', '3', 1, @tournament_id),
    ('e9040000-0000-0000-0000-000000000002',
     'HORSE', 'AGE', 'LESS_THAN_OR_EQUAL', '8', 1, @tournament_id),
    ('e9040000-0000-0000-0000-000000000003',
     'HORSE', 'WEIGHT', 'GREATER_THAN_OR_EQUAL', '400', 1, @tournament_id),
    ('e9040000-0000-0000-0000-000000000004',
     'HORSE', 'WEIGHT', 'LESS_THAN_OR_EQUAL', '600', 1, @tournament_id),
    ('e9040000-0000-0000-0000-000000000005',
     'JOCKEY', 'WEIGHT', 'GREATER_THAN_OR_EQUAL', '45', 1, @tournament_id),
    ('e9040000-0000-0000-0000-000000000006',
     'JOCKEY', 'WEIGHT', 'LESS_THAN_OR_EQUAL', '65', 1, @tournament_id);

-- ============================================================================
-- 5. TÁM NGỰA, NĂM OWNER VÀ TÁM JOCKEY
-- ============================================================================

INSERT INTO horses
    (horse_id, name, breed, gender, age, weight, color, image_url,
     health_status, current_rating, race_class, highest_rating,
     rating_updated_at, total_races, total_wins, total_places,
     win_rate, last_race_at, created_at, owner_id)
VALUES
    ('e9310000-0000-0000-0000-000000000001',
     'Schedule Horse 1', 'THOROUGHBRED', 'MALE', 4, 445, 'BAY', NULL,
     'HEALTHY', 40, 'CLASS_4', 45, @created_at, 8, 1, 3, 12, NULL,
     @created_at, 'e9110000-0000-0000-0000-000000000001'),
    ('e9310000-0000-0000-0000-000000000002',
     'Schedule Horse 2', 'THOROUGHBRED', 'FEMALE', 4, 450, 'BLACK', NULL,
     'HEALTHY', 42, 'CLASS_4', 47, @created_at, 9, 2, 4, 22, NULL,
     @created_at, 'e9110000-0000-0000-0000-000000000002'),
    ('e9310000-0000-0000-0000-000000000003',
     'Schedule Horse 3', 'THOROUGHBRED', 'MALE', 5, 455, 'CHESTNUT', NULL,
     'HEALTHY', 44, 'CLASS_4', 49, @created_at, 10, 2, 5, 20, NULL,
     @created_at, 'e9110000-0000-0000-0000-000000000003'),
    ('e9310000-0000-0000-0000-000000000004',
     'Schedule Horse 4', 'THOROUGHBRED', 'FEMALE', 3, 440, 'GREY', NULL,
     'HEALTHY', 46, 'CLASS_4', 50, @created_at, 7, 1, 3, 14, NULL,
     @created_at, 'e9110000-0000-0000-0000-000000000004'),
    ('e9310000-0000-0000-0000-000000000005',
     'Schedule Horse 5', 'THOROUGHBRED', 'MALE', 6, 460, 'BAY', NULL,
     'HEALTHY', 48, 'CLASS_4', 53, @created_at, 11, 3, 6, 27, NULL,
     @created_at, 'e9110000-0000-0000-0000-000000000005'),
    ('e9310000-0000-0000-0000-000000000006',
     'Schedule Horse 6', 'THOROUGHBRED', 'FEMALE', 4, 448, 'BLACK', NULL,
     'HEALTHY', 50, 'CLASS_4', 55, @created_at, 12, 3, 6, 25, NULL,
     @created_at, 'e9110000-0000-0000-0000-000000000001'),
    ('e9310000-0000-0000-0000-000000000007',
     'Schedule Horse 7', 'THOROUGHBRED', 'MALE', 5, 452, 'CHESTNUT', NULL,
     'HEALTHY', 52, 'CLASS_4', 57, @created_at, 13, 4, 7, 31, NULL,
     @created_at, 'e9110000-0000-0000-0000-000000000002'),
    ('e9310000-0000-0000-0000-000000000008',
     'Schedule Horse 8', 'THOROUGHBRED', 'FEMALE', 4, 442, 'GREY', NULL,
     'HEALTHY', 54, 'CLASS_4', 59, @created_at, 14, 4, 8, 29, NULL,
     @created_at, 'e9110000-0000-0000-0000-000000000003');

INSERT INTO horse_tournament_registrations
    (horse_tournament_reg_id, tournament_id, horse_id, owner_id,
     status, submitted_at, reviewed_by, reviewed_at,
     rating_at_registration, race_class_at_registration, note)
VALUES
    ('e9320000-0000-0000-0000-000000000001', @tournament_id,
     'e9310000-0000-0000-0000-000000000001',
     'e9110000-0000-0000-0000-000000000001',
     'APPROVED', DATE_SUB(@seed_now, INTERVAL 16 DAY),
     @admin_user_id, DATE_SUB(@seed_now, INTERVAL 14 DAY),
     40, 'CLASS_4', 'Scheduling demo approved'),
    ('e9320000-0000-0000-0000-000000000002', @tournament_id,
     'e9310000-0000-0000-0000-000000000002',
     'e9110000-0000-0000-0000-000000000002',
     'APPROVED', DATE_SUB(@seed_now, INTERVAL 16 DAY),
     @admin_user_id, DATE_SUB(@seed_now, INTERVAL 14 DAY),
     42, 'CLASS_4', 'Scheduling demo approved'),
    ('e9320000-0000-0000-0000-000000000003', @tournament_id,
     'e9310000-0000-0000-0000-000000000003',
     'e9110000-0000-0000-0000-000000000003',
     'APPROVED', DATE_SUB(@seed_now, INTERVAL 16 DAY),
     @admin_user_id, DATE_SUB(@seed_now, INTERVAL 14 DAY),
     44, 'CLASS_4', 'Scheduling demo approved'),
    ('e9320000-0000-0000-0000-000000000004', @tournament_id,
     'e9310000-0000-0000-0000-000000000004',
     'e9110000-0000-0000-0000-000000000004',
     'APPROVED', DATE_SUB(@seed_now, INTERVAL 16 DAY),
     @admin_user_id, DATE_SUB(@seed_now, INTERVAL 14 DAY),
     46, 'CLASS_4', 'Scheduling demo approved'),
    ('e9320000-0000-0000-0000-000000000005', @tournament_id,
     'e9310000-0000-0000-0000-000000000005',
     'e9110000-0000-0000-0000-000000000005',
     'APPROVED', DATE_SUB(@seed_now, INTERVAL 16 DAY),
     @admin_user_id, DATE_SUB(@seed_now, INTERVAL 14 DAY),
     48, 'CLASS_4', 'Scheduling demo approved'),
    ('e9320000-0000-0000-0000-000000000006', @tournament_id,
     'e9310000-0000-0000-0000-000000000006',
     'e9110000-0000-0000-0000-000000000001',
     'APPROVED', DATE_SUB(@seed_now, INTERVAL 16 DAY),
     @admin_user_id, DATE_SUB(@seed_now, INTERVAL 14 DAY),
     50, 'CLASS_4', 'Scheduling demo approved'),
    ('e9320000-0000-0000-0000-000000000007', @tournament_id,
     'e9310000-0000-0000-0000-000000000007',
     'e9110000-0000-0000-0000-000000000002',
     'APPROVED', DATE_SUB(@seed_now, INTERVAL 16 DAY),
     @admin_user_id, DATE_SUB(@seed_now, INTERVAL 14 DAY),
     52, 'CLASS_4', 'Scheduling demo approved'),
    ('e9320000-0000-0000-0000-000000000008', @tournament_id,
     'e9310000-0000-0000-0000-000000000008',
     'e9110000-0000-0000-0000-000000000003',
     'APPROVED', DATE_SUB(@seed_now, INTERVAL 16 DAY),
     @admin_user_id, DATE_SUB(@seed_now, INTERVAL 14 DAY),
     54, 'CLASS_4', 'Scheduling demo approved');

INSERT INTO jockey_tournament_registrations
    (jockey_tournament_reg_id, tournament_id, jockey_id, status,
     submitted_at, reviewed_by, reviewed_at, hire_fee, note)
VALUES
    ('e9330000-0000-0000-0000-000000000001', @tournament_id,
     'e9210000-0000-0000-0000-000000000001', 'APPROVED',
     DATE_SUB(@seed_now, INTERVAL 16 DAY), @admin_user_id,
     DATE_SUB(@seed_now, INTERVAL 14 DAY), 2000000,
     'Scheduling demo approved'),
    ('e9330000-0000-0000-0000-000000000002', @tournament_id,
     'e9210000-0000-0000-0000-000000000002', 'APPROVED',
     DATE_SUB(@seed_now, INTERVAL 16 DAY), @admin_user_id,
     DATE_SUB(@seed_now, INTERVAL 14 DAY), 2000000,
     'Scheduling demo approved'),
    ('e9330000-0000-0000-0000-000000000003', @tournament_id,
     'e9210000-0000-0000-0000-000000000003', 'APPROVED',
     DATE_SUB(@seed_now, INTERVAL 16 DAY), @admin_user_id,
     DATE_SUB(@seed_now, INTERVAL 14 DAY), 2000000,
     'Scheduling demo approved'),
    ('e9330000-0000-0000-0000-000000000004', @tournament_id,
     'e9210000-0000-0000-0000-000000000004', 'APPROVED',
     DATE_SUB(@seed_now, INTERVAL 16 DAY), @admin_user_id,
     DATE_SUB(@seed_now, INTERVAL 14 DAY), 2000000,
     'Scheduling demo approved'),
    ('e9330000-0000-0000-0000-000000000005', @tournament_id,
     'e9210000-0000-0000-0000-000000000005', 'APPROVED',
     DATE_SUB(@seed_now, INTERVAL 16 DAY), @admin_user_id,
     DATE_SUB(@seed_now, INTERVAL 14 DAY), 2000000,
     'Scheduling demo approved'),
    ('e9330000-0000-0000-0000-000000000006', @tournament_id,
     'e9210000-0000-0000-0000-000000000006', 'APPROVED',
     DATE_SUB(@seed_now, INTERVAL 16 DAY), @admin_user_id,
     DATE_SUB(@seed_now, INTERVAL 14 DAY), 2000000,
     'Scheduling demo approved'),
    ('e9330000-0000-0000-0000-000000000007', @tournament_id,
     'e9210000-0000-0000-0000-000000000007', 'APPROVED',
     DATE_SUB(@seed_now, INTERVAL 16 DAY), @admin_user_id,
     DATE_SUB(@seed_now, INTERVAL 14 DAY), 2000000,
     'Scheduling demo approved'),
    ('e9330000-0000-0000-0000-000000000008', @tournament_id,
     'e9210000-0000-0000-0000-000000000008', 'APPROVED',
     DATE_SUB(@seed_now, INTERVAL 16 DAY), @admin_user_id,
     DATE_SUB(@seed_now, INTERVAL 14 DAY), 2000000,
     'Scheduling demo approved');

-- Contract đã hợp lệ nhưng chưa được phân vào race.
INSERT INTO jockey_horse_contracts (
    contract_id, tournament_id,
    horse_tournament_reg_id, jockey_tournament_reg_id,
    owner_id, horse_id, jockey_id,
    hire_fee, advance_percent, final_percent,
    advance_paid_amount, escrow_amount, system_contract_fee,
    owner_prize_share_percent, jockey_prize_share_percent,
    payment_status, escrow_status,
    advance_payout_status, final_payout_status,
    status, advance_payout_at, requested_at, responded_at,
    accepted_at, submitted_at, contract_note
) VALUES
    ('e9340000-0000-0000-0000-000000000001', @tournament_id,
     'e9320000-0000-0000-0000-000000000001',
     'e9330000-0000-0000-0000-000000000001',
     'e9110000-0000-0000-0000-000000000001',
     'e9310000-0000-0000-0000-000000000001',
     'e9210000-0000-0000-0000-000000000001',
     2000000, 30, 70, 600000, 1400000, 100000, 80, 20,
     'PAID', 'PARTIALLY_RELEASED', 'PAID', 'NOT_RELEASED', 'APPROVED',
     DATE_SUB(@seed_now, INTERVAL 8 DAY),
     DATE_SUB(@seed_now, INTERVAL 10 DAY),
     DATE_SUB(@seed_now, INTERVAL 9 DAY),
     DATE_SUB(@seed_now, INTERVAL 9 DAY),
     DATE_SUB(@seed_now, INTERVAL 8 DAY), 'Scheduling contract 1'),
    ('e9340000-0000-0000-0000-000000000002', @tournament_id,
     'e9320000-0000-0000-0000-000000000002',
     'e9330000-0000-0000-0000-000000000002',
     'e9110000-0000-0000-0000-000000000002',
     'e9310000-0000-0000-0000-000000000002',
     'e9210000-0000-0000-0000-000000000002',
     2000000, 30, 70, 600000, 1400000, 100000, 80, 20,
     'PAID', 'PARTIALLY_RELEASED', 'PAID', 'NOT_RELEASED', 'APPROVED',
     DATE_SUB(@seed_now, INTERVAL 8 DAY),
     DATE_SUB(@seed_now, INTERVAL 10 DAY),
     DATE_SUB(@seed_now, INTERVAL 9 DAY),
     DATE_SUB(@seed_now, INTERVAL 9 DAY),
     DATE_SUB(@seed_now, INTERVAL 8 DAY), 'Scheduling contract 2'),
    ('e9340000-0000-0000-0000-000000000003', @tournament_id,
     'e9320000-0000-0000-0000-000000000003',
     'e9330000-0000-0000-0000-000000000003',
     'e9110000-0000-0000-0000-000000000003',
     'e9310000-0000-0000-0000-000000000003',
     'e9210000-0000-0000-0000-000000000003',
     2000000, 30, 70, 600000, 1400000, 100000, 80, 20,
     'PAID', 'PARTIALLY_RELEASED', 'PAID', 'NOT_RELEASED', 'APPROVED',
     DATE_SUB(@seed_now, INTERVAL 8 DAY),
     DATE_SUB(@seed_now, INTERVAL 10 DAY),
     DATE_SUB(@seed_now, INTERVAL 9 DAY),
     DATE_SUB(@seed_now, INTERVAL 9 DAY),
     DATE_SUB(@seed_now, INTERVAL 8 DAY), 'Scheduling contract 3'),
    ('e9340000-0000-0000-0000-000000000004', @tournament_id,
     'e9320000-0000-0000-0000-000000000004',
     'e9330000-0000-0000-0000-000000000004',
     'e9110000-0000-0000-0000-000000000004',
     'e9310000-0000-0000-0000-000000000004',
     'e9210000-0000-0000-0000-000000000004',
     2000000, 30, 70, 600000, 1400000, 100000, 80, 20,
     'PAID', 'PARTIALLY_RELEASED', 'PAID', 'NOT_RELEASED', 'APPROVED',
     DATE_SUB(@seed_now, INTERVAL 8 DAY),
     DATE_SUB(@seed_now, INTERVAL 10 DAY),
     DATE_SUB(@seed_now, INTERVAL 9 DAY),
     DATE_SUB(@seed_now, INTERVAL 9 DAY),
     DATE_SUB(@seed_now, INTERVAL 8 DAY), 'Scheduling contract 4'),
    ('e9340000-0000-0000-0000-000000000005', @tournament_id,
     'e9320000-0000-0000-0000-000000000005',
     'e9330000-0000-0000-0000-000000000005',
     'e9110000-0000-0000-0000-000000000005',
     'e9310000-0000-0000-0000-000000000005',
     'e9210000-0000-0000-0000-000000000005',
     2000000, 30, 70, 600000, 1400000, 100000, 80, 20,
     'PAID', 'PARTIALLY_RELEASED', 'PAID', 'NOT_RELEASED', 'APPROVED',
     DATE_SUB(@seed_now, INTERVAL 8 DAY),
     DATE_SUB(@seed_now, INTERVAL 10 DAY),
     DATE_SUB(@seed_now, INTERVAL 9 DAY),
     DATE_SUB(@seed_now, INTERVAL 9 DAY),
     DATE_SUB(@seed_now, INTERVAL 8 DAY), 'Scheduling contract 5'),
    ('e9340000-0000-0000-0000-000000000006', @tournament_id,
     'e9320000-0000-0000-0000-000000000006',
     'e9330000-0000-0000-0000-000000000006',
     'e9110000-0000-0000-0000-000000000001',
     'e9310000-0000-0000-0000-000000000006',
     'e9210000-0000-0000-0000-000000000006',
     2000000, 30, 70, 600000, 1400000, 100000, 80, 20,
     'PAID', 'PARTIALLY_RELEASED', 'PAID', 'NOT_RELEASED', 'APPROVED',
     DATE_SUB(@seed_now, INTERVAL 8 DAY),
     DATE_SUB(@seed_now, INTERVAL 10 DAY),
     DATE_SUB(@seed_now, INTERVAL 9 DAY),
     DATE_SUB(@seed_now, INTERVAL 9 DAY),
     DATE_SUB(@seed_now, INTERVAL 8 DAY), 'Scheduling contract 6'),
    ('e9340000-0000-0000-0000-000000000007', @tournament_id,
     'e9320000-0000-0000-0000-000000000007',
     'e9330000-0000-0000-0000-000000000007',
     'e9110000-0000-0000-0000-000000000002',
     'e9310000-0000-0000-0000-000000000007',
     'e9210000-0000-0000-0000-000000000007',
     2000000, 30, 70, 600000, 1400000, 100000, 80, 20,
     'PAID', 'PARTIALLY_RELEASED', 'PAID', 'NOT_RELEASED', 'APPROVED',
     DATE_SUB(@seed_now, INTERVAL 8 DAY),
     DATE_SUB(@seed_now, INTERVAL 10 DAY),
     DATE_SUB(@seed_now, INTERVAL 9 DAY),
     DATE_SUB(@seed_now, INTERVAL 9 DAY),
     DATE_SUB(@seed_now, INTERVAL 8 DAY), 'Scheduling contract 7'),
    ('e9340000-0000-0000-0000-000000000008', @tournament_id,
     'e9320000-0000-0000-0000-000000000008',
     'e9330000-0000-0000-0000-000000000008',
     'e9110000-0000-0000-0000-000000000003',
     'e9310000-0000-0000-0000-000000000008',
     'e9210000-0000-0000-0000-000000000008',
     2000000, 30, 70, 600000, 1400000, 100000, 80, 20,
     'PAID', 'PARTIALLY_RELEASED', 'PAID', 'NOT_RELEASED', 'APPROVED',
     DATE_SUB(@seed_now, INTERVAL 8 DAY),
     DATE_SUB(@seed_now, INTERVAL 10 DAY),
     DATE_SUB(@seed_now, INTERVAL 9 DAY),
     DATE_SUB(@seed_now, INTERVAL 9 DAY),
     DATE_SUB(@seed_now, INTERVAL 8 DAY), 'Scheduling contract 8');

-- ============================================================================
-- 6. ROUND VÀ RACE: CHƯA PHÂN ENTRY/LANE/NHÂN SỰ
-- ============================================================================

INSERT INTO rounds (
    round_id, round_name, sequence_order, is_final, prediction_type,
    advancement_rule, start_date, end_date, description,
    max_races, max_entries, min_entries, status,
    head_referee_id, head_referee_assigned_at,
    expected_entries, qualifiers_per_race, advanced_at,
    transition_status, created_at, tournament_id, created_by
) VALUES (
    @round_id,
    'Chung kết',
    1,
    1,
    'TOP3',
    'Top 3 nhận giải thưởng chung cuộc',
    @race_start,
    @race_end,
    'Round đang chờ Admin phân entry, lane và nhân sự.',
    1,
    8,
    8,
    'SCHEDULING',
    NULL,
    NULL,
    8,
    4,
    NULL,
    'NOT_READY',
    DATE_SUB(@seed_now, INTERVAL 2 DAY),
    @tournament_id,
    @admin_user_id
);

INSERT INTO races (
    race_id, name, start_time, end_time, track_condition, distance,
    sequence_order, status, started_at, finished_at,
    schedule_published_at, prediction_open_at, prediction_close_at,
    ai_prediction_publication_status,
    round_id, created_by, started_by, inspection_finalized_at
) VALUES (
    @race_id,
    'DEMO SCHEDULING - Final Race',
    @race_start,
    @race_end,
    'TURF',
    'MILE_1600M',
    1,
    'SCHEDULING',
    NULL,
    NULL,
    NULL,
    NULL,
    NULL,
    'DRAFT',
    @round_id,
    @admin_user_id,
    NULL,
    NULL
);

-- Cố ý KHÔNG insert:
--   race_entries
--   race_referees
--   race_inspection_staff_assignments
--
-- Điều này bảo đảm lần đầu mở Scheduling Board:
--   - entry = 0/8;
--   - lane chưa có;
--   - Head Referee chưa có;
--   - Race Referee chưa có;
--   - Vet/Medical chưa có;
--   - Publish bị khóa cho tới khi Admin hoàn thành checklist.

SET FOREIGN_KEY_CHECKS = @OLD_FOREIGN_KEY_CHECKS;
SET SQL_SAFE_UPDATES = @OLD_SQL_SAFE_UPDATES;

-- ============================================================================
-- 7. KIỂM TRA SAU SEED
-- ============================================================================

SELECT
    tournament_id,
    name,
    status,
    phase,
    competition_start_at
FROM tournaments
WHERE tournament_id = @tournament_id;

SELECT
    rd.round_id,
    rd.round_name,
    rd.status AS round_status,
    rd.head_referee_id,
    r.race_id,
    r.name AS race_name,
    r.status AS race_status,
    r.start_time,
    r.end_time,
    r.schedule_published_at
FROM rounds rd
JOIN races r ON r.round_id = rd.round_id
WHERE rd.round_id = @round_id;

SELECT
    (SELECT COUNT(*)
     FROM jockey_horse_contracts
     WHERE tournament_id = @tournament_id
       AND status = 'APPROVED') AS approved_contracts,
    (SELECT COUNT(*)
     FROM race_entries
     WHERE race_id = @race_id) AS initial_entries,
    (SELECT COUNT(*)
     FROM race_referees
     WHERE race_id = @race_id) AS initial_race_referees,
    (SELECT COUNT(*)
     FROM race_inspection_staff_assignments
     WHERE race_id = @race_id) AS initial_inspection_assignments;

SELECT
    u.username,
    u.full_name,
    r.role_name,
    '12345678' AS password
FROM users u
JOIN roles r ON r.role_id = u.role_id
WHERE u.username IN (
    'dmadmin',
    'schref1',
    'schref2',
    'schvet1',
    'schmed1'
)
ORDER BY r.role_name, u.username;

-- ============================================================================
-- 8. THỨ TỰ API/THAO TÁC ĐỂ DEMO
-- ============================================================================
--
-- 1) Phân 8 contract vào race, lane vẫn phải null:
-- POST /api/race-entries/rounds/e9010000-0000-0000-0000-000000000001/auto-assign
--
-- 2) Random lane:
-- POST /api/race-entries/races/e9020000-0000-0000-0000-000000000001/auto-assign-lanes
--
-- 3) Gán Head Referee schref2:
-- PUT /api/tournaments/rounds/e9010000-0000-0000-0000-000000000001/head-referee
--     ?refereeId=e9300000-0000-0000-0000-000000000002
--
-- 4) Gán đúng 1 Race Referee schref1:
-- POST /api/admin/races/e9020000-0000-0000-0000-000000000001/referees
-- Body:
-- {
--   "refereeId": "e9300000-0000-0000-0000-000000000001"
-- }
--
-- 5) Gán Vet + Medical thủ công:
-- POST /api/admin/races/e9020000-0000-0000-0000-000000000001/inspection-staff/assign
-- Body:
-- {
--   "veterinarianId": "e9300000-0000-0000-0000-000000000003",
--   "medStaffId": "e9300000-0000-0000-0000-000000000004"
-- }
--
-- Hoặc dùng:
-- POST /api/admin/races/e9020000-0000-0000-0000-000000000001/inspection-staff/auto-assign
--
-- 6) Publish toàn bộ active Round:
-- POST /api/admin/tournaments/e9000000-0000-0000-0000-000000000001/publish-schedule
--
-- Kết quả mong đợi:
--   - Race: SCHEDULING -> SCHEDULED.
--   - Round: SCHEDULING -> SCHEDULED.
--   - Tournament phase: SCHEDULING -> RACING.
--   - schedule_published_at và prediction window được tự sinh.
-- ============================================================================

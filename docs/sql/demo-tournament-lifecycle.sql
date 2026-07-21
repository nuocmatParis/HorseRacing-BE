-- ============================================================================
-- HRTMS DEMO — TOURNAMENT LIFECYCLE (Admin configure → Publish → Schedule)
-- ============================================================================
-- Mục đích:
--   Tạo dữ liệu mẫu cho flow Admin cấu hình giải đấu từ đầu đến khi
--   publish schedule, bao gồm:
--     • Tạo tournament + prize + eligibility
--     • Bracket confirm
--     • Publish tournament
--     • Đóng registration + complete review
--     • Complete matching (tạo contracts + distribute entries)
--     • Schedule proposal
--     • Set race times + assign referees
--     • Publish schedule
--
-- Kịch bản:
--   Tournament "DEMO LIFECYCLE" — 8 entries, 1 round (Final), 1 race.
--   maxApprovedEntries = 8 (lũy thừa 2, ≥ 8).
--
-- Cách dùng:
--   1. Chạy BE một lần để Hibernate/Flyway tạo schema mới nhất.
--   2. Mở file này trong DataGrip/MySQL Workbench, chạy từng SECTION
--      (các section độc lập có thể chạy cùng lúc).
--   3. Sau đó gọi API theo hướng dẫn ở cuối file.
--
-- Mật khẩu chung: admin123
-- ============================================================================

USE SWP391_Project_HRTMS;
SET NAMES utf8mb4;

SET @OLD_FOREIGN_KEY_CHECKS = @@FOREIGN_KEY_CHECKS;
SET @OLD_SQL_SAFE_UPDATES = @@SQL_SAFE_UPDATES;
SET FOREIGN_KEY_CHECKS = 0;
SET SQL_SAFE_UPDATES = 0;

-- Kiểm tra schema đã tồn tại chưa
IF NOT EXISTS (
    SELECT 1 FROM information_schema.tables
    WHERE table_schema = DATABASE() AND table_name = 'users'
) THEN
    SIGNAL SQLSTATE '45000'
        SET MESSAGE_TEXT = 'Schema chưa tồn tại. Hãy chạy BE/Hibernate/Flyway tạo bảng trước.';
END IF;

-- ============================================================================
-- UUID prefix & constants
-- ============================================================================
SET @UUID_PREFIX = 'a0000000-0000-0000-0000-0000000000';
SET @demo_password = '$2a$12$AsBCvrsZ2yvqd7RyEflkfOGwfTewt8CSx40CKh0ZIZuD4WZ49wo4a';
SET @now = NOW();

-- ============================================================================
-- 1. ROLES (chèn nếu chưa tồn tại)
-- ============================================================================
INSERT IGNORE INTO roles (role_id, role_name, description, is_active, created_at)
VALUES
    (CONCAT(@UUID_PREFIX, '01'), 'ADMIN',        'Administrator',             1, @now),
    (CONCAT(@UUID_PREFIX, '02'), 'HORSE_OWNER',  'Chủ ngựa',                  1, @now),
    (CONCAT(@UUID_PREFIX, '03'), 'JOCKEY',       'Kỵ sĩ',                     1, @now),
    (CONCAT(@UUID_PREFIX, '04'), 'SPECTATOR',    'Khán giả',                  1, @now),
    (CONCAT(@UUID_PREFIX, '05'), 'REFEREE',      'Trọng tài',                 1, @now),
    (CONCAT(@UUID_PREFIX, '06'), 'VETERINARIAN', 'Bác sĩ thú y',              1, @now),
    (CONCAT(@UUID_PREFIX, '07'), 'MEDICAL_STAFF', 'Nhân viên y tế',           1, @now);

SET @role_admin     = CONCAT(@UUID_PREFIX, '01');
SET @role_owner     = CONCAT(@UUID_PREFIX, '02');
SET @role_jockey    = CONCAT(@UUID_PREFIX, '03');
SET @role_referee   = CONCAT(@UUID_PREFIX, '05');

-- ============================================================================
-- 2. USERS
-- ============================================================================
-- Admin
INSERT IGNORE INTO users
    (user_id, username, password, email, dob, gender, full_name, phone_number,
     image_url, status, created_at, last_login_at, role_id)
VALUES
    (CONCAT(@UUID_PREFIX, '01'), 'admin1',   @demo_password, 'admin@hrtms.com',
     '1990-01-01', 'MALE', 'Admin Chính',     '0900000001',
     NULL, 'ACTIVE', @now, @now, @role_admin);

-- 2 Horse Owners
INSERT IGNORE INTO users
    (user_id, username, password, email, dob, gender, full_name, phone_number,
     image_url, status, created_at, last_login_at, role_id)
VALUES
    (CONCAT(@UUID_PREFIX, '11'), 'owner1',   @demo_password, 'owner1@hrtms.com',
     '1985-03-15', 'MALE', 'Nguyễn Văn A',   '0900000011',
     NULL, 'ACTIVE', @now, @now, @role_owner),
    (CONCAT(@UUID_PREFIX, '12'), 'owner2',   @demo_password, 'owner2@hrtms.com',
     '1990-07-20', 'FEMALE', 'Trần Thị B',   '0900000012',
     NULL, 'ACTIVE', @now, @now, @role_owner);

-- 8 Jockeys
INSERT IGNORE INTO users
    (user_id, username, password, email, dob, gender, full_name, phone_number,
     image_url, status, created_at, last_login_at, role_id)
VALUES
    (CONCAT(@UUID_PREFIX, '21'), 'jockey1', @demo_password, 'jockey1@hrtms.com',  '1995-01-01', 'MALE', 'Jockey 1', '0900000021', NULL, 'ACTIVE', @now, @now, @role_jockey),
    (CONCAT(@UUID_PREFIX, '22'), 'jockey2', @demo_password, 'jockey2@hrtms.com',  '1995-02-01', 'MALE', 'Jockey 2', '0900000022', NULL, 'ACTIVE', @now, @now, @role_jockey),
    (CONCAT(@UUID_PREFIX, '23'), 'jockey3', @demo_password, 'jockey3@hrtms.com',  '1995-03-01', 'MALE', 'Jockey 3', '0900000023', NULL, 'ACTIVE', @now, @now, @role_jockey),
    (CONCAT(@UUID_PREFIX, '24'), 'jockey4', @demo_password, 'jockey4@hrtms.com',  '1995-04-01', 'MALE', 'Jockey 4', '0900000024', NULL, 'ACTIVE', @now, @now, @role_jockey),
    (CONCAT(@UUID_PREFIX, '25'), 'jockey5', @demo_password, 'jockey5@hrtms.com',  '1995-05-01', 'MALE', 'Jockey 5', '0900000025', NULL, 'ACTIVE', @now, @now, @role_jockey),
    (CONCAT(@UUID_PREFIX, '26'), 'jockey6', @demo_password, 'jockey6@hrtms.com',  '1995-06-01', 'MALE', 'Jockey 6', '0900000026', NULL, 'ACTIVE', @now, @now, @role_jockey),
    (CONCAT(@UUID_PREFIX, '27'), 'jockey7', @demo_password, 'jockey7@hrtms.com',  '1995-07-01', 'MALE', 'Jockey 7', '0900000027', NULL, 'ACTIVE', @now, @now, @role_jockey),
    (CONCAT(@UUID_PREFIX, '28'), 'jockey8', @demo_password, 'jockey8@hrtms.com',  '1995-08-01', 'MALE', 'Jockey 8', '0900000028', NULL, 'ACTIVE', @now, @now, @role_jockey);

-- 1 Referee
INSERT IGNORE INTO users
    (user_id, username, password, email, dob, gender, full_name, phone_number,
     image_url, status, created_at, last_login_at, role_id)
VALUES
    (CONCAT(@UUID_PREFIX, '31'), 'referee1', @demo_password, 'referee1@hrtms.com',
     '1988-06-15', 'MALE', 'Trọng tài 1',   '0900000031',
     NULL, 'ACTIVE', @now, @now, @role_referee);

SET @admin_user_id   = CONCAT(@UUID_PREFIX, '01');
SET @owner1_user_id  = CONCAT(@UUID_PREFIX, '11');
SET @owner2_user_id  = CONCAT(@UUID_PREFIX, '12');
SET @referee_user_id = CONCAT(@UUID_PREFIX, '31');

-- ============================================================================
-- 3. HORSE OWNERS
-- ============================================================================
INSERT IGNORE INTO horse_owners
    (owner_id, user_id, farm_name, address, created_at)
VALUES
    (CONCAT(@UUID_PREFIX, '11'), @owner1_user_id, 'Trang trại Nguyễn Văn A', 'Hà Nội, Việt Nam', @now),
    (CONCAT(@UUID_PREFIX, '12'), @owner2_user_id, 'Trang trại Trần Thị B',   'TP.HCM, Việt Nam',  @now);

SET @owner1_id = CONCAT(@UUID_PREFIX, '11');
SET @owner2_id = CONCAT(@UUID_PREFIX, '12');

-- ============================================================================
-- 4. REFEREE PROFILE
-- ============================================================================
INSERT IGNORE INTO referees
    (referee_id, user_id, years_of_service, status, created_at)
VALUES
    (CONCAT(@UUID_PREFIX, '31'), @referee_user_id, 10, 'AVAILABLE', @now);

SET @referee_id = CONCAT(@UUID_PREFIX, '31');

-- ============================================================================
-- 5. HORSES (8 con, 4 per owner)
-- ============================================================================
INSERT IGNORE INTO horses
    (horse_id, name, breed, gender, age, weight, color, image_url,
     health_status, current_rating, race_class, highest_rating,
     total_races, total_wins, total_places, win_rate, created_at, owner_id)
VALUES
    -- Owner 1
    (CONCAT(@UUID_PREFIX, '41'), 'Ngựa Chiến Thần',  'THOROUGHBRED', 'MALE',   5, 520, 'Bay',   NULL, 'HEALTHY', 120, 'CLASS_1', 125, 10, 3, 5, 30.0, @now, @owner1_id),
    (CONCAT(@UUID_PREFIX, '42'), 'Ngựa Tốc Phong',   'THOROUGHBRED', 'MALE',   4, 510, 'Đen',   NULL, 'HEALTHY', 115, 'CLASS_1', 118,  8, 2, 4, 25.0, @now, @owner1_id),
    (CONCAT(@UUID_PREFIX, '43'), 'Ngựa Hỏa Tốc',     'THOROUGHBRED', 'MALE',   6, 530, 'Trắng', NULL, 'HEALTHY', 110, 'CLASS_1', 120, 15, 5, 8, 33.3, @now, @owner1_id),
    (CONCAT(@UUID_PREFIX, '44'), 'Ngựa Bạch Mã',      'THOROUGHBRED', 'FEMALE', 4, 490, 'Trắng', NULL, 'HEALTHY', 108, 'CLASS_1', 112,  6, 1, 3, 16.7, @now, @owner1_id),
    -- Owner 2
    (CONCAT(@UUID_PREFIX, '45'), 'Ngựa Phượng Hoàng', 'THOROUGHBRED', 'FEMALE', 5, 500, 'Đỏ',    NULL, 'HEALTHY', 118, 'CLASS_1', 122, 12, 4, 7, 33.3, @now, @owner2_id),
    (CONCAT(@UUID_PREFIX, '46'), 'Ngựa Long Vương',   'THOROUGHBRED', 'MALE',   6, 540, 'Xanh',  NULL, 'HEALTHY', 114, 'CLASS_1', 120, 16, 5, 8, 31.3, @now, @owner2_id),
    (CONCAT(@UUID_PREFIX, '47'), 'Ngựa Thanh Phong',  'THOROUGHBRED', 'MALE',   4, 510, 'Xám',   NULL, 'HEALTHY', 112, 'CLASS_1', 115,  8, 3, 5, 37.5, @now, @owner2_id),
    (CONCAT(@UUID_PREFIX, '48'), 'Ngựa Huyền Thoại',  'THOROUGHBRED', 'MALE',   5, 520, 'Đen',   NULL, 'HEALTHY', 109, 'CLASS_1', 114, 11, 3, 6, 27.3, @now, @owner2_id);

-- ============================================================================
-- 6. JOCKEYS (8 profiles)
-- ============================================================================
INSERT IGNORE INTO jockeys
    (jockey_id, user_id, height, weight, experience_years,
     specialization, status, total_races, total_wins, jockey_tier, created_at)
VALUES
    (CONCAT(@UUID_PREFIX, '21'), CONCAT(@UUID_PREFIX, '21'), 165, 52, 5, 'SPRINT',    'AVAILABLE',  50,  8, 'PROFESSIONAL', @now),
    (CONCAT(@UUID_PREFIX, '22'), CONCAT(@UUID_PREFIX, '22'), 168, 54, 4, 'MILE',      'AVAILABLE',  40,  6, 'JUNIOR',       @now),
    (CONCAT(@UUID_PREFIX, '23'), CONCAT(@UUID_PREFIX, '23'), 170, 55, 6, 'SPRINT',    'AVAILABLE',  60, 12, 'PROFESSIONAL', @now),
    (CONCAT(@UUID_PREFIX, '24'), CONCAT(@UUID_PREFIX, '24'), 163, 50, 3, 'MILE',      'AVAILABLE',  30,  4, 'JUNIOR',       @now),
    (CONCAT(@UUID_PREFIX, '25'), CONCAT(@UUID_PREFIX, '25'), 167, 53, 7, 'LONG',      'AVAILABLE',  70, 15, 'PROFESSIONAL', @now),
    (CONCAT(@UUID_PREFIX, '26'), CONCAT(@UUID_PREFIX, '26'), 166, 51, 2, 'SPRINT',    'AVAILABLE',  20,  3, 'APPRENTICE',   @now),
    (CONCAT(@UUID_PREFIX, '27'), CONCAT(@UUID_PREFIX, '27'), 169, 56, 5, 'MILE',      'AVAILABLE',  55, 10, 'PROFESSIONAL', @now),
    (CONCAT(@UUID_PREFIX, '28'), CONCAT(@UUID_PREFIX, '28'), 164, 49, 4, 'INTERMEDIATE','AVAILABLE', 35,  5, 'JUNIOR',       @now);

-- ============================================================================
-- 7. WALLETS
-- ============================================================================
INSERT IGNORE INTO wallets
    (wallet_id, owner_type, balance, currency, status, created_at, updated_at, wallet_purpose, user_id)
VALUES
    -- Admin
    (CONCAT(@UUID_PREFIX, '51'), 'USER', 10000000, 'VND', 'ACTIVE', @now, @now, 'USER_MAIN', @admin_user_id),
    -- 2 Owners
    (CONCAT(@UUID_PREFIX, '52'), 'USER',  5000000, 'VND', 'ACTIVE', @now, @now, 'USER_MAIN', @owner1_user_id),
    (CONCAT(@UUID_PREFIX, '53'), 'USER',  5000000, 'VND', 'ACTIVE', @now, @now, 'USER_MAIN', @owner2_user_id),
    -- 8 Jockeys
    (CONCAT(@UUID_PREFIX, '54'), 'USER',  2000000, 'VND', 'ACTIVE', @now, @now, 'USER_MAIN', CONCAT(@UUID_PREFIX, '21')),
    (CONCAT(@UUID_PREFIX, '55'), 'USER',  2000000, 'VND', 'ACTIVE', @now, @now, 'USER_MAIN', CONCAT(@UUID_PREFIX, '22')),
    (CONCAT(@UUID_PREFIX, '56'), 'USER',  2000000, 'VND', 'ACTIVE', @now, @now, 'USER_MAIN', CONCAT(@UUID_PREFIX, '23')),
    (CONCAT(@UUID_PREFIX, '57'), 'USER',  2000000, 'VND', 'ACTIVE', @now, @now, 'USER_MAIN', CONCAT(@UUID_PREFIX, '24')),
    (CONCAT(@UUID_PREFIX, '58'), 'USER',  2000000, 'VND', 'ACTIVE', @now, @now, 'USER_MAIN', CONCAT(@UUID_PREFIX, '25')),
    (CONCAT(@UUID_PREFIX, '59'), 'USER',  2000000, 'VND', 'ACTIVE', @now, @now, 'USER_MAIN', CONCAT(@UUID_PREFIX, '26')),
    (CONCAT(@UUID_PREFIX, '5a'), 'USER',  2000000, 'VND', 'ACTIVE', @now, @now, 'USER_MAIN', CONCAT(@UUID_PREFIX, '27')),
    (CONCAT(@UUID_PREFIX, '5b'), 'USER',  2000000, 'VND', 'ACTIVE', @now, @now, 'USER_MAIN', CONCAT(@UUID_PREFIX, '28')),
    -- Referee
    (CONCAT(@UUID_PREFIX, '5c'), 'USER',  2000000, 'VND', 'ACTIVE', @now, @now, 'USER_MAIN', @referee_user_id);

-- System wallets
INSERT IGNORE INTO wallets
    (wallet_id, owner_type, balance, currency, status, created_at, updated_at, wallet_purpose, user_id)
VALUES
    (CONCAT(@UUID_PREFIX, '5d'), 'SYSTEM', 0,         'VND', 'ACTIVE', @now, @now, 'SYSTEM_REVENUE',   NULL),
    (CONCAT(@UUID_PREFIX, '5e'), 'SYSTEM', 0,         'VND', 'ACTIVE', @now, @now, 'SYSTEM_ESCROW',     NULL),
    (CONCAT(@UUID_PREFIX, '5f'), 'SYSTEM', 50000000,  'VND', 'ACTIVE', @now, @now, 'SYSTEM_PRIZE_POOL', NULL);

-- ============================================================================
-- 8. TOURNAMENT (DRAFT, NOT_GENERATED, 8 entries — 1 round final)
-- ============================================================================
SET @tournament_id = CONCAT(@UUID_PREFIX, 'a1');

-- Timeline: đặt registration_open trong quá khứ để có thể close-registration ngay
-- Các deadline đã qua để không bị overdue check chặn
SET @reg_open    = DATE_SUB(@now, INTERVAL 20 DAY);
SET @reg_close   = DATE_ADD(@reg_open, INTERVAL 5 DAY);   -- passed
SET @review_dead = DATE_ADD(@reg_close, INTERVAL 4 DAY);  -- passed
SET @jockey_dead = DATE_ADD(@review_dead, INTERVAL 4 DAY);-- passed
SET @sched_dead  = DATE_ADD(@jockey_dead, INTERVAL 4 DAY);-- passed
SET @comp_start  = DATE_ADD(@now, INTERVAL 1 DAY);        -- tương lai gần
SET @tourn_start = DATE(@comp_start);
SET @tourn_end   = DATE_ADD(@tourn_start, INTERVAL 14 DAY);

-- Xóa dữ liệu cũ nếu có
DELETE FROM jockey_horse_contracts WHERE tournament_id = @tournament_id;
DELETE FROM jockey_tournament_registrations WHERE tournament_id = @tournament_id;
DELETE FROM horse_tournament_registrations WHERE tournament_id = @tournament_id;
DELETE FROM tournament_eligibility WHERE tournament_id = @tournament_id;
DELETE FROM prize_structures WHERE tournament_id = @tournament_id;
DELETE FROM race_entries WHERE race_id IN (SELECT race_id FROM races WHERE round_id IN (SELECT round_id FROM rounds WHERE tournament_id = @tournament_id));
DELETE FROM races WHERE round_id IN (SELECT round_id FROM rounds WHERE tournament_id = @tournament_id);
DELETE FROM rounds WHERE tournament_id = @tournament_id;
DELETE FROM tournaments WHERE tournament_id = @tournament_id;

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
     current_round_name, min_round_gap_days, race_class, distance,
     top_weight_lbs, min_weight_lbs, equipment_weight_kg, handicap_enabled,
     max_approved_horses, max_approved_jockeys, max_approved_entries,
     planned_round_count, planned_race_count, bracket_plan_status,
     bracket_plan_version, created_by)
VALUES
    (@tournament_id,
     'DEMO LIFECYCLE',
     'Tournament demo cho flow Admin cấu hình giải đấu: bracket confirm → publish → close registration → complete review → matching → schedule proposal → publish schedule. 8 entries, 1 race Final.',
     @tourn_start, @tourn_end, NULL,
     'Trường đua Demo Lifecycle',
     100000, 50000, 10000000,
     'THOROUGHBRED', 2, 12,
     -- Prediction points
     100, 30, 10, 50,
     -- Prediction timing
     120, 5, 24,
     -- Inspection timing
     90, 30,
     -- Race day config
     9, 35, 0, 30, 30,
     '08:00:00', '18:00:00',
     0, NULL, NULL,
     -- STATUS: DRAFT, PHASE: DRAFT, BRACKET: NOT_GENERATED
     'DRAFT', 'DRAFT', @now, NULL,
     @reg_open, @reg_close, @review_dead,
     @jockey_dead, @sched_dead, @comp_start,
     NULL, 7, 'CLASS_3', 'MILE_1600M',
     -- Handicap disabled
     0, 0, 0.0, 0,
     -- Max approved
     8, 10, 8,
     -- Bracket plan
     NULL, NULL, 'NOT_GENERATED', 1,
     @admin_user_id);

-- ============================================================================
-- 9. PRIZE STRUCTURES
-- ============================================================================
INSERT IGNORE INTO prize_structures
    (prize_structure_id, `prize_rank`, percentage, fixed_amount, is_active, tournament_id)
VALUES
    (CONCAT(@UUID_PREFIX, 'b1'), 1, 50, 0, 1, @tournament_id),
    (CONCAT(@UUID_PREFIX, 'b2'), 2, 30, 0, 1, @tournament_id),
    (CONCAT(@UUID_PREFIX, 'b3'), 3, 20, 0, 1, @tournament_id);

-- ============================================================================
-- 10. ELIGIBILITY RULES
-- ============================================================================
INSERT IGNORE INTO tournament_eligibility
    (eligibility_id, target_type, condition_name, condition_operator, condition_value, is_active, tournament_id)
VALUES
    -- Ngựa phải là THOROUGHBRED
    (CONCAT(@UUID_PREFIX, 'c1'), 'HORSE', 'BREED',  'EQUAL',   'THOROUGHBRED', 1, @tournament_id),
    -- Ngựa >= 2 tuổi
    (CONCAT(@UUID_PREFIX, 'c2'), 'HORSE', 'AGE',    'GREATER_THAN_OR_EQUAL', '2', 1, @tournament_id),
    -- Jockey phải có tier tối thiểu APPRENTICE
    (CONCAT(@UUID_PREFIX, 'c3'), 'JOCKEY', 'JOCKEY_TIER', 'NOT_EQUAL', 'APPRENTICE', 0, @tournament_id);

-- ============================================================================
-- RESTORE SETTINGS
-- ============================================================================
SET FOREIGN_KEY_CHECKS = @OLD_FOREIGN_KEY_CHECKS;
SET SQL_SAFE_UPDATES = @OLD_SQL_SAFE_UPDATES;

-- ============================================================================
-- HƯỚNG DẪN TEST — Gọi API theo thứ tự
-- ============================================================================
-- Login trước: lấy token từ POST /api/auth/login { username: "admin1", password: "admin123" }
-- Dùng token Bearer cho tất cả request dưới đây.
--
-- === Bước 1: Bracket Confirm ==============================================
-- POST /api/admin/tournaments/{tournament_id}/bracket-confirm
-- Body: {"maxApprovedEntries": 8, "expectedPlanVersion": 1}
--
-- => System sinh 1 Round + 1 Race skeleton:
--    - Round name: "Vòng 1 (Chung Kết)"
--    - Race name: "DEMO LIFECYCLE - Vòng 1 (Chung Kết) - Race 1"
--    - round.startDate, round.endDate được tính từ schedule
--    - race.startTime, race.endTime được tính từ schedule
--    - race.predictionOpenAt, race.predictionCloseAt được tính
--    - bracket_plan_status → CONFIRMED
--    - planned_round_count = 1, planned_race_count = 1
--
-- === Bước 2: Publish Tournament ===========================================
-- POST /api/admin/tournaments/{tournament_id}/publish
--
-- => status: DRAFT → OPEN
-- => phase: DRAFT → REGISTRATION_OPEN
--
-- === Bước 3: Seed registrations + contracts (chạy SQL dưới đây) ==========
-- Chạy phần SQL bên dưới (từ dòng 380 đến 480) để tạo:
--   - 8 horse_tournament_registrations (APPROVED)
--   - 8 jockey_tournament_registrations (APPROVED)
--   - 8 jockey_horse_contracts (APPROVED + PAID)
-- ==========================================================================

-- ============================================================================
-- SECTION B: Seed registrations & contracts (chạy SAU Bước 2)
-- ============================================================================
-- Bỏ comment đoạn dưới và chạy sau khi đã publish tournament:

-- 8 Horse Registrations (APPROVED)
INSERT IGNORE INTO horse_tournament_registrations
    (horse_tournament_reg_id, tournament_id, horse_id, owner_id, status,
     submitted_at, reviewed_by, reviewed_at, rating_at_registration, race_class_at_registration)
VALUES
    (CONCAT(@UUID_PREFIX, 'd1'), @tournament_id, CONCAT(@UUID_PREFIX, '41'), @owner1_id, 'APPROVED', @reg_open, @admin_user_id, DATE_ADD(@reg_open, INTERVAL 1 DAY), 120, 'CLASS_1'),
    (CONCAT(@UUID_PREFIX, 'd2'), @tournament_id, CONCAT(@UUID_PREFIX, '42'), @owner1_id, 'APPROVED', @reg_open, @admin_user_id, DATE_ADD(@reg_open, INTERVAL 1 DAY), 115, 'CLASS_1'),
    (CONCAT(@UUID_PREFIX, 'd3'), @tournament_id, CONCAT(@UUID_PREFIX, '43'), @owner1_id, 'APPROVED', @reg_open, @admin_user_id, DATE_ADD(@reg_open, INTERVAL 1 DAY), 110, 'CLASS_1'),
    (CONCAT(@UUID_PREFIX, 'd4'), @tournament_id, CONCAT(@UUID_PREFIX, '44'), @owner1_id, 'APPROVED', @reg_open, @admin_user_id, DATE_ADD(@reg_open, INTERVAL 1 DAY), 108, 'CLASS_1'),
    (CONCAT(@UUID_PREFIX, 'd5'), @tournament_id, CONCAT(@UUID_PREFIX, '45'), @owner2_id, 'APPROVED', @reg_open, @admin_user_id, DATE_ADD(@reg_open, INTERVAL 1 DAY), 118, 'CLASS_1'),
    (CONCAT(@UUID_PREFIX, 'd6'), @tournament_id, CONCAT(@UUID_PREFIX, '46'), @owner2_id, 'APPROVED', @reg_open, @admin_user_id, DATE_ADD(@reg_open, INTERVAL 1 DAY), 114, 'CLASS_1'),
    (CONCAT(@UUID_PREFIX, 'd7'), @tournament_id, CONCAT(@UUID_PREFIX, '47'), @owner2_id, 'APPROVED', @reg_open, @admin_user_id, DATE_ADD(@reg_open, INTERVAL 1 DAY), 112, 'CLASS_1'),
    (CONCAT(@UUID_PREFIX, 'd8'), @tournament_id, CONCAT(@UUID_PREFIX, '48'), @owner2_id, 'APPROVED', @reg_open, @admin_user_id, DATE_ADD(@reg_open, INTERVAL 1 DAY), 109, 'CLASS_1');

-- 8 Jockey Registrations (APPROVED)
INSERT IGNORE INTO jockey_tournament_registrations
    (jockey_tournament_reg_id, tournament_id, jockey_id, status,
     submitted_at, reviewed_by, reviewed_at, note, hire_fee)
VALUES
    (CONCAT(@UUID_PREFIX, 'e1'), @tournament_id, CONCAT(@UUID_PREFIX, '21'), 'APPROVED', @reg_open, @admin_user_id, DATE_ADD(@reg_open, INTERVAL 1 DAY), 'Demo', 500000),
    (CONCAT(@UUID_PREFIX, 'e2'), @tournament_id, CONCAT(@UUID_PREFIX, '22'), 'APPROVED', @reg_open, @admin_user_id, DATE_ADD(@reg_open, INTERVAL 1 DAY), 'Demo', 500000),
    (CONCAT(@UUID_PREFIX, 'e3'), @tournament_id, CONCAT(@UUID_PREFIX, '23'), 'APPROVED', @reg_open, @admin_user_id, DATE_ADD(@reg_open, INTERVAL 1 DAY), 'Demo', 500000),
    (CONCAT(@UUID_PREFIX, 'e4'), @tournament_id, CONCAT(@UUID_PREFIX, '24'), 'APPROVED', @reg_open, @admin_user_id, DATE_ADD(@reg_open, INTERVAL 1 DAY), 'Demo', 500000),
    (CONCAT(@UUID_PREFIX, 'e5'), @tournament_id, CONCAT(@UUID_PREFIX, '25'), 'APPROVED', @reg_open, @admin_user_id, DATE_ADD(@reg_open, INTERVAL 1 DAY), 'Demo', 500000),
    (CONCAT(@UUID_PREFIX, 'e6'), @tournament_id, CONCAT(@UUID_PREFIX, '26'), 'APPROVED', @reg_open, @admin_user_id, DATE_ADD(@reg_open, INTERVAL 1 DAY), 'Demo', 500000),
    (CONCAT(@UUID_PREFIX, 'e7'), @tournament_id, CONCAT(@UUID_PREFIX, '27'), 'APPROVED', @reg_open, @admin_user_id, DATE_ADD(@reg_open, INTERVAL 1 DAY), 'Demo', 500000),
    (CONCAT(@UUID_PREFIX, 'e8'), @tournament_id, CONCAT(@UUID_PREFIX, '28'), 'APPROVED', @reg_open, @admin_user_id, DATE_ADD(@reg_open, INTERVAL 1 DAY), 'Demo', 500000);

-- 8 Contracts (APPROVED + PAID)
-- Mỗi contract ghép 1 horse registration với 1 jockey registration
INSERT IGNORE INTO jockey_horse_contracts
    (contract_id, tournament_id, horse_tournament_reg_id, jockey_tournament_reg_id,
     owner_id, horse_id, jockey_id,
     hire_fee, advance_percent, final_percent,
     advance_paid_amount, escrow_amount, system_contract_fee,
     owner_prize_share_percent, jockey_prize_share_percent,
     payment_status, escrow_status, advance_payout_status, final_payout_status,
     status, requested_at, responded_at, accepted_at, submitted_at,
     reviewed_by, reviewed_at)
VALUES
    (CONCAT(@UUID_PREFIX, 'f1'), @tournament_id, CONCAT(@UUID_PREFIX, 'd1'), CONCAT(@UUID_PREFIX, 'e1'), @owner1_id, CONCAT(@UUID_PREFIX, '41'), CONCAT(@UUID_PREFIX, '21'), 1000000, 50, 50, 500000, 500000, 50000, 60, 40, 'PAID', 'HELD', 'PAID', 'NOT_RELEASED', 'APPROVED', @reg_open, DATE_ADD(@reg_open, INTERVAL 1 DAY), DATE_ADD(@reg_open, INTERVAL 1 DAY), DATE_ADD(@reg_open, INTERVAL 1 DAY), @admin_user_id, DATE_ADD(@reg_open, INTERVAL 1 DAY)),
    (CONCAT(@UUID_PREFIX, 'f2'), @tournament_id, CONCAT(@UUID_PREFIX, 'd2'), CONCAT(@UUID_PREFIX, 'e2'), @owner1_id, CONCAT(@UUID_PREFIX, '42'), CONCAT(@UUID_PREFIX, '22'), 1000000, 50, 50, 500000, 500000, 50000, 60, 40, 'PAID', 'HELD', 'PAID', 'NOT_RELEASED', 'APPROVED', @reg_open, DATE_ADD(@reg_open, INTERVAL 1 DAY), DATE_ADD(@reg_open, INTERVAL 1 DAY), DATE_ADD(@reg_open, INTERVAL 1 DAY), @admin_user_id, DATE_ADD(@reg_open, INTERVAL 1 DAY)),
    (CONCAT(@UUID_PREFIX, 'f3'), @tournament_id, CONCAT(@UUID_PREFIX, 'd3'), CONCAT(@UUID_PREFIX, 'e3'), @owner1_id, CONCAT(@UUID_PREFIX, '43'), CONCAT(@UUID_PREFIX, '23'), 1000000, 50, 50, 500000, 500000, 50000, 60, 40, 'PAID', 'HELD', 'PAID', 'NOT_RELEASED', 'APPROVED', @reg_open, DATE_ADD(@reg_open, INTERVAL 1 DAY), DATE_ADD(@reg_open, INTERVAL 1 DAY), DATE_ADD(@reg_open, INTERVAL 1 DAY), @admin_user_id, DATE_ADD(@reg_open, INTERVAL 1 DAY)),
    (CONCAT(@UUID_PREFIX, 'f4'), @tournament_id, CONCAT(@UUID_PREFIX, 'd4'), CONCAT(@UUID_PREFIX, 'e4'), @owner1_id, CONCAT(@UUID_PREFIX, '44'), CONCAT(@UUID_PREFIX, '24'), 1000000, 50, 50, 500000, 500000, 50000, 60, 40, 'PAID', 'HELD', 'PAID', 'NOT_RELEASED', 'APPROVED', @reg_open, DATE_ADD(@reg_open, INTERVAL 1 DAY), DATE_ADD(@reg_open, INTERVAL 1 DAY), DATE_ADD(@reg_open, INTERVAL 1 DAY), @admin_user_id, DATE_ADD(@reg_open, INTERVAL 1 DAY)),
    (CONCAT(@UUID_PREFIX, 'f5'), @tournament_id, CONCAT(@UUID_PREFIX, 'd5'), CONCAT(@UUID_PREFIX, 'e5'), @owner2_id, CONCAT(@UUID_PREFIX, '45'), CONCAT(@UUID_PREFIX, '25'), 1000000, 50, 50, 500000, 500000, 50000, 60, 40, 'PAID', 'HELD', 'PAID', 'NOT_RELEASED', 'APPROVED', @reg_open, DATE_ADD(@reg_open, INTERVAL 1 DAY), DATE_ADD(@reg_open, INTERVAL 1 DAY), DATE_ADD(@reg_open, INTERVAL 1 DAY), @admin_user_id, DATE_ADD(@reg_open, INTERVAL 1 DAY)),
    (CONCAT(@UUID_PREFIX, 'f6'), @tournament_id, CONCAT(@UUID_PREFIX, 'd6'), CONCAT(@UUID_PREFIX, 'e6'), @owner2_id, CONCAT(@UUID_PREFIX, '46'), CONCAT(@UUID_PREFIX, '26'), 1000000, 50, 50, 500000, 500000, 50000, 60, 40, 'PAID', 'HELD', 'PAID', 'NOT_RELEASED', 'APPROVED', @reg_open, DATE_ADD(@reg_open, INTERVAL 1 DAY), DATE_ADD(@reg_open, INTERVAL 1 DAY), DATE_ADD(@reg_open, INTERVAL 1 DAY), @admin_user_id, DATE_ADD(@reg_open, INTERVAL 1 DAY)),
    (CONCAT(@UUID_PREFIX, 'f7'), @tournament_id, CONCAT(@UUID_PREFIX, 'd7'), CONCAT(@UUID_PREFIX, 'e7'), @owner2_id, CONCAT(@UUID_PREFIX, '47'), CONCAT(@UUID_PREFIX, '27'), 1000000, 50, 50, 500000, 500000, 50000, 60, 40, 'PAID', 'HELD', 'PAID', 'NOT_RELEASED', 'APPROVED', @reg_open, DATE_ADD(@reg_open, INTERVAL 1 DAY), DATE_ADD(@reg_open, INTERVAL 1 DAY), DATE_ADD(@reg_open, INTERVAL 1 DAY), @admin_user_id, DATE_ADD(@reg_open, INTERVAL 1 DAY)),
    (CONCAT(@UUID_PREFIX, 'f8'), @tournament_id, CONCAT(@UUID_PREFIX, 'd8'), CONCAT(@UUID_PREFIX, 'e8'), @owner2_id, CONCAT(@UUID_PREFIX, '48'), CONCAT(@UUID_PREFIX, '28'), 1000000, 50, 50, 500000, 500000, 50000, 60, 40, 'PAID', 'HELD', 'PAID', 'NOT_RELEASED', 'APPROVED', @reg_open, DATE_ADD(@reg_open, INTERVAL 1 DAY), DATE_ADD(@reg_open, INTERVAL 1 DAY), DATE_ADD(@reg_open, INTERVAL 1 DAY), @admin_user_id, DATE_ADD(@reg_open, INTERVAL 1 DAY));

-- ============================================================================
-- TIẾP TỤC API (chạy sau khi seed registrations + contracts)
-- ============================================================================
--
-- === Bước 4: Close Registration ============================================
-- POST /api/admin/tournaments/{tournament_id}/close-registration
--
-- => phase: REGISTRATION_OPEN → REGISTRATION_REVIEW
-- => Các registration PENDING_PAYMENT bị REJECTED
--
-- === Bước 5: Complete Review ===============================================
-- POST /api/admin/tournaments/{tournament_id}/complete-review
--
-- => phase: REGISTRATION_REVIEW → JOCKEY_MATCHING
--
-- === Bước 6: Complete Matching =============================================
-- POST /api/admin/tournaments/{tournament_id}/complete-matching
--
-- => Phase: JOCKEY_MATCHING → SCHEDULING
-- => 8 approved contracts được distribute vào race vòng 1
--    (8 entries, lane numbers được gán tự động serpentine)
--
-- === Bước 7: Schedule Proposal =============================================
-- GET /api/admin/tournaments/{tournament_id}/schedule-proposal
--
-- => Xem lịch đề xuất:
--    - proposedStartAt, proposedFinalEndAt
--    - racingDays, calendarDays
--    - round + race start/end times
--
-- === Bước 8: Cập nhật round dates + race times + referees ==================
-- Cần set các giá trị sau TRƯỚC khi publish-schedule:
--
-- 8a. Set round startDate/endDate (dùng schedule proposal ở bước 7)
--     PUT /api/admin/rounds/{round_id}
--     Body: {
--       "startDate": "{proposedStartAt}",
--       "endDate": "{proposedFinalEndAt}"
--     }
--
-- 8b. Set race startTime/endTime
--     PUT /api/admin/races/{race_id}
--     Body: {
--       "startTime": "{suggestedStartTime}",
--       "endTime": "{suggestedEndTime}"
--     }
--
-- 8c. Gán head referee cho round
--     PUT /api/admin/rounds/{round_id}/head-referee
--     Body: { "refereeId": "{referee_id}" }
--
-- 8d. Gán race referee cho race
--     POST /api/admin/races/{race_id}/referees
--     Body: { "refereeId": "{referee_id}" }
--
-- 8e. Gán lane numbers cho entries (nếu complete-matching chưa gán tự động)
--     POST /api/admin/races/{race_id}/lanes
--     Body: [{"entryId": "...", "laneNumber": 1}, ...]
--
-- === Bước 9: Publish Schedule ==============================================
-- POST /api/admin/tournaments/{tournament_id}/publish-schedule
--
-- => phase: SCHEDULING → RACING
-- => round status → SCHEDULED
-- => race status → SCHEDULED
-- => bracket_plan_status → LOCKED (vì là round đầu tiên)
-- => predictionOpenAt, predictionCloseAt được set tự động
-- ============================================================================

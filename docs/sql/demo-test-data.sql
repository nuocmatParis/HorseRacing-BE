-- ============================================================================
-- HRTMS DEMO DATA - MYSQL 8+
-- ============================================================================
-- Mục đích:
--   1. Có tài khoản cho mọi role.
--   2. Có dashboard data: tournament, hồ sơ ngựa chờ duyệt, contract chờ duyệt,
--      race đã lên lịch.
--   3. Có race sắp chạy với 8 entry + AI winProbability để Spectator dự đoán Top 3.
--   4. Có Final Race đã FINISHED, report đã Signed để Admin bấm Publish và kiểm
--      tra scoring, prize payout, Jockey final payout, transaction, wallet balance.
--   5. Có FINISHED / DID_NOT_FINISH / DISQUALIFIED, violation, appeal và evidence.
--
-- Cách dùng:
--   - Khởi động BE một lần để Hibernate/Flyway tạo đủ schema mới nhất.
--   - Chạy file này trên database SWP391_Project_HRTMS.
--   - CHỈ DÙNG DATABASE LOCAL/TEST, KHÔNG CHẠY TRÊN PRODUCTION.
--   - File đặt lại số dư các ví demo và ba ví hệ thống để kết quả payout ổn định.
--   - Có thể chạy lại để đưa luồng Final Demo về trạng thái chờ publish ban đầu.
--
-- Mật khẩu chung cho toàn bộ tài khoản demo: admin123
-- Tài khoản chính:
--   admin1     / ADMIN
--   owner1     / HORSE_OWNER
--   spectator1 / SPECTATOR chưa dự đoán (dùng tạo dự đoán mới)
--   spectator2 / SPECTATOR có prediction chờ chấm
--   referee1   / REFEREE
--   vet1       / VETERINARIAN
--   medical1   / MEDICAL_STAFF
--   jockey1 ... jockey8 / JOCKEY
-- ============================================================================

USE SWP391_Project_HRTMS;

SET @demo_password = '$2a$12$AsBCvrsZ2yvqd7RyEflkfOGwfTewt8CSx40CKh0ZIZuD4WZ49wo4a';
SET @now = NOW();
SET @upcoming_start = DATE_ADD(@now, INTERVAL 60 MINUTE);
SET @upcoming_end = DATE_ADD(@upcoming_start, INTERVAL 30 MINUTE);
SET @final_start = DATE_SUB(@now, INTERVAL 1 DAY);
SET @final_end = DATE_ADD(@final_start, INTERVAL 30 MINUTE);

-- Dọn đúng dữ liệu phát sinh khi đã từng publish Final Demo. Không đụng tới
-- transaction/rating của tournament khác. Các bảng đã phải tồn tại trước khi chạy seed.
DELETE FROM horse_rating_histories
WHERE race_id = '90000000-0000-0000-0000-000000000002';

DELETE FROM wallet_transactions
WHERE contract_id LIKE '70000000-0000-0000-0000-0000000002%'
  AND type IN (
      'PRIZE_OWNER_SHARE',
      'PRIZE_JOCKEY_SHARE',
      'JOCKEY_HIRING_FINAL_PAYOUT',
      'JOCKEY_HIRING_FINAL_INCOME'
  );

-- --------------------------------------------------------------------------
-- 1. Roles
-- --------------------------------------------------------------------------

INSERT INTO roles (role_id, role_name, description, is_active, created_at)
VALUES
    (UUID(), 'ADMIN', 'Quản trị hệ thống', 1, @now),
    (UUID(), 'HORSE_OWNER', 'Chủ ngựa', 1, @now),
    (UUID(), 'JOCKEY', 'Kỵ sĩ', 1, @now),
    (UUID(), 'SPECTATOR', 'Khán giả', 1, @now),
    (UUID(), 'REFEREE', 'Trọng tài', 1, @now),
    (UUID(), 'VETERINARIAN', 'Bác sĩ thú y', 1, @now),
    (UUID(), 'MEDICAL_STAFF', 'Nhân viên y tế', 1, @now)
ON DUPLICATE KEY UPDATE
    description = VALUES(description),
    is_active = VALUES(is_active);

SET @role_admin = (SELECT role_id FROM roles WHERE role_name = 'ADMIN' LIMIT 1);
SET @role_owner = (SELECT role_id FROM roles WHERE role_name = 'HORSE_OWNER' LIMIT 1);
SET @role_jockey = (SELECT role_id FROM roles WHERE role_name = 'JOCKEY' LIMIT 1);
SET @role_spectator = (SELECT role_id FROM roles WHERE role_name = 'SPECTATOR' LIMIT 1);
SET @role_referee = (SELECT role_id FROM roles WHERE role_name = 'REFEREE' LIMIT 1);
SET @role_vet = (SELECT role_id FROM roles WHERE role_name = 'VETERINARIAN' LIMIT 1);
SET @role_medical = (SELECT role_id FROM roles WHERE role_name = 'MEDICAL_STAFF' LIMIT 1);

-- --------------------------------------------------------------------------
-- 2. Users và role profiles
-- --------------------------------------------------------------------------

INSERT INTO users
    (user_id, username, password, email, dob, gender, full_name, phone_number,
     image_url, status, created_at, last_login_at, role_id)
VALUES
    ('10000000-0000-0000-0000-000000000001', 'admin1', @demo_password, 'demo.admin@hrtms.local', '1990-01-01', 'MALE', 'Admin Demo', NULL, NULL, 'ACTIVE', @now, @now, @role_admin),
    ('10000000-0000-0000-0000-000000000002', 'owner1', @demo_password, 'demo.owner@hrtms.local', '1992-02-02', 'FEMALE', 'Chủ ngựa Demo', NULL, NULL, 'ACTIVE', @now, @now, @role_owner),
    ('10000000-0000-0000-0000-000000000003', 'spectator1', @demo_password, 'demo.spectator@hrtms.local', '2000-03-03', 'MALE', 'Khán giả Demo Mới', NULL, NULL, 'ACTIVE', @now, @now, @role_spectator),
    ('10000000-0000-0000-0000-000000000004', 'spectator2', @demo_password, 'demo.spectator.history@hrtms.local', '1999-04-04', 'FEMALE', 'Khán giả Có Lịch Sử', NULL, NULL, 'ACTIVE', @now, @now, @role_spectator),
    ('10000000-0000-0000-0000-000000000005', 'referee1', @demo_password, 'demo.referee@hrtms.local', '1988-05-05', 'MALE', 'Trọng tài Demo', NULL, NULL, 'ACTIVE', @now, @now, @role_referee),
    ('10000000-0000-0000-0000-000000000006', 'vet1', @demo_password, 'demo.vet@hrtms.local', '1987-06-06', 'FEMALE', 'Thú y Demo', NULL, NULL, 'ACTIVE', @now, @now, @role_vet),
    ('10000000-0000-0000-0000-000000000007', 'medical1', @demo_password, 'demo.medical@hrtms.local', '1989-07-07', 'MALE', 'Y tế Demo', NULL, NULL, 'ACTIVE', @now, @now, @role_medical),
    ('10000000-0000-0000-0000-000000000101', 'jockey1', @demo_password, 'demo.jockey01@hrtms.local', '1995-01-11', 'MALE', 'Kỵ sĩ Demo 01', NULL, NULL, 'ACTIVE', @now, @now, @role_jockey),
    ('10000000-0000-0000-0000-000000000102', 'jockey2', @demo_password, 'demo.jockey02@hrtms.local', '1995-02-12', 'FEMALE', 'Kỵ sĩ Demo 02', NULL, NULL, 'ACTIVE', @now, @now, @role_jockey),
    ('10000000-0000-0000-0000-000000000103', 'jockey3', @demo_password, 'demo.jockey03@hrtms.local', '1995-03-13', 'MALE', 'Kỵ sĩ Demo 03', NULL, NULL, 'ACTIVE', @now, @now, @role_jockey),
    ('10000000-0000-0000-0000-000000000104', 'jockey4', @demo_password, 'demo.jockey04@hrtms.local', '1995-04-14', 'FEMALE', 'Kỵ sĩ Demo 04', NULL, NULL, 'ACTIVE', @now, @now, @role_jockey),
    ('10000000-0000-0000-0000-000000000105', 'jockey5', @demo_password, 'demo.jockey05@hrtms.local', '1995-05-15', 'MALE', 'Kỵ sĩ Demo 05', NULL, NULL, 'ACTIVE', @now, @now, @role_jockey),
    ('10000000-0000-0000-0000-000000000106', 'jockey6', @demo_password, 'demo.jockey06@hrtms.local', '1995-06-16', 'FEMALE', 'Kỵ sĩ Demo 06', NULL, NULL, 'ACTIVE', @now, @now, @role_jockey),
    ('10000000-0000-0000-0000-000000000107', 'jockey7', @demo_password, 'demo.jockey07@hrtms.local', '1995-07-17', 'MALE', 'Kỵ sĩ Demo 07', NULL, NULL, 'ACTIVE', @now, @now, @role_jockey),
    ('10000000-0000-0000-0000-000000000108', 'jockey8', @demo_password, 'demo.jockey08@hrtms.local', '1995-08-18', 'FEMALE', 'Kỵ sĩ Demo 08', NULL, NULL, 'ACTIVE', @now, @now, @role_jockey)
ON DUPLICATE KEY UPDATE
    username = VALUES(username),
    password = VALUES(password),
    full_name = VALUES(full_name),
    status = 'ACTIVE',
    role_id = VALUES(role_id);

INSERT INTO horse_owners (owner_id, user_id, farm_name, address, license_number, created_at)
VALUES ('20000000-0000-0000-0000-000000000001', '10000000-0000-0000-0000-000000000002', 'Trang trại Demo HRTMS', 'Thành phố Hồ Chí Minh', 'OWNER-DEMO-001', @now)
ON DUPLICATE KEY UPDATE address = VALUES(address);

INSERT INTO spectators (spectator_id, user_id, total_points, created_at)
VALUES
    ('20000000-0000-0000-0000-000000000002', '10000000-0000-0000-0000-000000000003', 0, @now),
    ('20000000-0000-0000-0000-000000000003', '10000000-0000-0000-0000-000000000004', 120, @now)
ON DUPLICATE KEY UPDATE total_points = VALUES(total_points);

INSERT INTO referees (referee_id, user_id, certification_level, years_of_service, status, created_at)
VALUES ('20000000-0000-0000-0000-000000000004', '10000000-0000-0000-0000-000000000005', 'HEAD_REFEREE', 10, 'ASSIGNED', @now)
ON DUPLICATE KEY UPDATE status = VALUES(status);

INSERT INTO veterinarians (vet_id, user_id, license_number, specialization, years_of_service, status, created_at)
VALUES ('20000000-0000-0000-0000-000000000005', '10000000-0000-0000-0000-000000000006', 'VET-DEMO-001', 'Equine Medicine', 8, 'ASSIGNED', @now)
ON DUPLICATE KEY UPDATE status = VALUES(status);

INSERT INTO medical_staffs (med_staff_id, user_id, certification, years_of_service, status, created_at)
VALUES ('20000000-0000-0000-0000-000000000006', '10000000-0000-0000-0000-000000000007', 'Sports Medicine', 7, 'ASSIGNED', @now)
ON DUPLICATE KEY UPDATE status = VALUES(status);

INSERT INTO jockeys
    (jockey_id, user_id, height, weight, experience_years, license_number,
     specialization, status, total_races, total_wins, jockey_tier,
     tier_updated_at, last_race_at, created_at)
VALUES
    ('21000000-0000-0000-0000-000000000101', '10000000-0000-0000-0000-000000000101', 1.62, 52, 8, 'JOCKEY-DEMO-01', 'MILE', 'AVAILABLE', 40, 8, 'PROFESSIONAL', @now, @final_end, @now),
    ('21000000-0000-0000-0000-000000000102', '10000000-0000-0000-0000-000000000102', 1.60, 51, 7, 'JOCKEY-DEMO-02', 'SPRINT', 'AVAILABLE', 38, 7, 'PROFESSIONAL', @now, @final_end, @now),
    ('21000000-0000-0000-0000-000000000103', '10000000-0000-0000-0000-000000000103', 1.64, 53, 6, 'JOCKEY-DEMO-03', 'MILE', 'AVAILABLE', 35, 6, 'JUNIOR', @now, @final_end, @now),
    ('21000000-0000-0000-0000-000000000104', '10000000-0000-0000-0000-000000000104', 1.61, 50, 6, 'JOCKEY-DEMO-04', 'INTERMEDIATE', 'AVAILABLE', 34, 5, 'JUNIOR', @now, @final_end, @now),
    ('21000000-0000-0000-0000-000000000105', '10000000-0000-0000-0000-000000000105', 1.63, 52, 5, 'JOCKEY-DEMO-05', 'LONG', 'AVAILABLE', 30, 4, 'JUNIOR', @now, @final_end, @now),
    ('21000000-0000-0000-0000-000000000106', '10000000-0000-0000-0000-000000000106', 1.59, 50, 4, 'JOCKEY-DEMO-06', 'SPRINT', 'AVAILABLE', 25, 3, 'APPRENTICE', @now, @final_end, @now),
    ('21000000-0000-0000-0000-000000000107', '10000000-0000-0000-0000-000000000107', 1.65, 53, 4, 'JOCKEY-DEMO-07', 'MILE', 'AVAILABLE', 24, 2, 'APPRENTICE', @now, @final_end, @now),
    ('21000000-0000-0000-0000-000000000108', '10000000-0000-0000-0000-000000000108', 1.62, 51, 3, 'JOCKEY-DEMO-08', 'INTERMEDIATE', 'AVAILABLE', 20, 2, 'APPRENTICE', @now, @final_end, @now)
ON DUPLICATE KEY UPDATE
    status = VALUES(status),
    total_races = VALUES(total_races),
    total_wins = VALUES(total_wins);

-- --------------------------------------------------------------------------
-- 3. Wallets
-- --------------------------------------------------------------------------

INSERT INTO wallets
    (wallet_id, owner_type, balance, currency, status, created_at, updated_at, wallet_purpose, user_id)
VALUES
    ('30000000-0000-0000-0000-000000000001', 'USER', 10000000, 'VND', 'ACTIVE', @now, @now, 'USER_MAIN', '10000000-0000-0000-0000-000000000002'),
    ('30000000-0000-0000-0000-000000000101', 'USER', 300000, 'VND', 'ACTIVE', @now, @now, 'USER_MAIN', '10000000-0000-0000-0000-000000000101'),
    ('30000000-0000-0000-0000-000000000102', 'USER', 300000, 'VND', 'ACTIVE', @now, @now, 'USER_MAIN', '10000000-0000-0000-0000-000000000102'),
    ('30000000-0000-0000-0000-000000000103', 'USER', 300000, 'VND', 'ACTIVE', @now, @now, 'USER_MAIN', '10000000-0000-0000-0000-000000000103'),
    ('30000000-0000-0000-0000-000000000104', 'USER', 300000, 'VND', 'ACTIVE', @now, @now, 'USER_MAIN', '10000000-0000-0000-0000-000000000104'),
    ('30000000-0000-0000-0000-000000000105', 'USER', 300000, 'VND', 'ACTIVE', @now, @now, 'USER_MAIN', '10000000-0000-0000-0000-000000000105'),
    ('30000000-0000-0000-0000-000000000106', 'USER', 300000, 'VND', 'ACTIVE', @now, @now, 'USER_MAIN', '10000000-0000-0000-0000-000000000106'),
    ('30000000-0000-0000-0000-000000000107', 'USER', 300000, 'VND', 'ACTIVE', @now, @now, 'USER_MAIN', '10000000-0000-0000-0000-000000000107'),
    ('30000000-0000-0000-0000-000000000108', 'USER', 300000, 'VND', 'ACTIVE', @now, @now, 'USER_MAIN', '10000000-0000-0000-0000-000000000108')
ON DUPLICATE KEY UPDATE
    balance = VALUES(balance),
    status = 'ACTIVE',
    updated_at = @now;

INSERT INTO wallets
    (wallet_id, owner_type, balance, currency, status, created_at, updated_at, wallet_purpose, user_id)
SELECT UUID(), 'SYSTEM', 0, 'VND', 'ACTIVE', @now, @now, 'SYSTEM_REVENUE', NULL
WHERE NOT EXISTS (
    SELECT 1 FROM wallets WHERE owner_type = 'SYSTEM' AND user_id IS NULL AND wallet_purpose = 'SYSTEM_REVENUE'
);
INSERT INTO wallets
    (wallet_id, owner_type, balance, currency, status, created_at, updated_at, wallet_purpose, user_id)
SELECT UUID(), 'SYSTEM', 0, 'VND', 'ACTIVE', @now, @now, 'SYSTEM_ESCROW', NULL
WHERE NOT EXISTS (
    SELECT 1 FROM wallets WHERE owner_type = 'SYSTEM' AND user_id IS NULL AND wallet_purpose = 'SYSTEM_ESCROW'
);
INSERT INTO wallets
    (wallet_id, owner_type, balance, currency, status, created_at, updated_at, wallet_purpose, user_id)
SELECT UUID(), 'SYSTEM', 0, 'VND', 'ACTIVE', @now, @now, 'SYSTEM_PRIZE_POOL', NULL
WHERE NOT EXISTS (
    SELECT 1 FROM wallets WHERE owner_type = 'SYSTEM' AND user_id IS NULL AND wallet_purpose = 'SYSTEM_PRIZE_POOL'
);

SET @wallet_revenue = (SELECT wallet_id FROM wallets WHERE owner_type = 'SYSTEM' AND user_id IS NULL AND wallet_purpose = 'SYSTEM_REVENUE' LIMIT 1);
SET @wallet_escrow = (SELECT wallet_id FROM wallets WHERE owner_type = 'SYSTEM' AND user_id IS NULL AND wallet_purpose = 'SYSTEM_ESCROW' LIMIT 1);
SET @wallet_prize = (SELECT wallet_id FROM wallets WHERE owner_type = 'SYSTEM' AND user_id IS NULL AND wallet_purpose = 'SYSTEM_PRIZE_POOL' LIMIT 1);

UPDATE wallets SET balance = 5000000, status = 'ACTIVE', updated_at = @now WHERE wallet_id = @wallet_revenue;
UPDATE wallets SET balance = 20000000, status = 'ACTIVE', updated_at = @now WHERE wallet_id = @wallet_escrow;
UPDATE wallets SET balance = 50000000, status = 'ACTIVE', updated_at = @now WHERE wallet_id = @wallet_prize;

-- --------------------------------------------------------------------------
-- 4. Horses
-- --------------------------------------------------------------------------

INSERT INTO horses
    (horse_id, name, breed, gender, age, weight, color, image_url,
     health_status, current_rating, race_class, highest_rating,
     rating_updated_at, total_races, total_wins, total_places, win_rate,
     last_race_at, created_at, owner_id)
VALUES
    ('40000000-0000-0000-0000-000000000101', 'Sao Băng Demo', 'THOROUGHBRED', 'MALE', 4, 480, 'Đen', NULL, 'HEALTHY', 118, 'CLASS_1', 121, @now, 20, 7, 13, 35.0, @final_end, @now, '20000000-0000-0000-0000-000000000001'),
    ('40000000-0000-0000-0000-000000000102', 'Tia Chớp Demo', 'THOROUGHBRED', 'FEMALE', 4, 470, 'Nâu', NULL, 'HEALTHY', 112, 'CLASS_1', 116, @now, 18, 5, 11, 27.8, @final_end, @now, '20000000-0000-0000-0000-000000000001'),
    ('40000000-0000-0000-0000-000000000103', 'Hải Đăng Demo', 'THOROUGHBRED', 'MALE', 5, 485, 'Hạt dẻ', NULL, 'HEALTHY', 105, 'CLASS_1', 110, @now, 22, 5, 12, 22.7, @final_end, @now, '20000000-0000-0000-0000-000000000001'),
    ('40000000-0000-0000-0000-000000000104', 'Bình Minh Demo', 'THOROUGHBRED', 'FEMALE', 4, 468, 'Xám', NULL, 'HEALTHY', 99, 'CLASS_2', 104, @now, 17, 3, 8, 17.6, @final_end, @now, '20000000-0000-0000-0000-000000000001'),
    ('40000000-0000-0000-0000-000000000105', 'Gió Nam Demo', 'THOROUGHBRED', 'MALE', 5, 490, 'Nâu sẫm', NULL, 'HEALTHY', 92, 'CLASS_2', 98, @now, 19, 3, 7, 15.8, @final_end, @now, '20000000-0000-0000-0000-000000000001'),
    ('40000000-0000-0000-0000-000000000106', 'Mây Trắng Demo', 'THOROUGHBRED', 'FEMALE', 4, 465, 'Trắng', NULL, 'HEALTHY', 86, 'CLASS_2', 92, @now, 16, 2, 6, 12.5, @final_end, @now, '20000000-0000-0000-0000-000000000001'),
    ('40000000-0000-0000-0000-000000000107', 'Đại Dương Demo', 'THOROUGHBRED', 'MALE', 5, 492, 'Đen', NULL, 'HEALTHY', 81, 'CLASS_2', 88, @now, 15, 2, 5, 13.3, @final_end, @now, '20000000-0000-0000-0000-000000000001'),
    ('40000000-0000-0000-0000-000000000108', 'Lửa Việt Demo', 'THOROUGHBRED', 'MALE', 4, 478, 'Đỏ nâu', NULL, 'HEALTHY', 76, 'CLASS_3', 83, @now, 14, 1, 4, 7.1, @final_end, @now, '20000000-0000-0000-0000-000000000001'),
    ('40000000-0000-0000-0000-000000000109', 'Hồ Sơ Chờ Duyệt', 'THOROUGHBRED', 'FEMALE', 3, 460, 'Nâu', NULL, 'HEALTHY', 35, 'CLASS_5', 35, @now, 0, 0, 0, 0, NULL, @now, '20000000-0000-0000-0000-000000000001'),
    ('40000000-0000-0000-0000-000000000110', 'Hợp Đồng Chờ Duyệt', 'THOROUGHBRED', 'MALE', 4, 475, 'Đen', NULL, 'HEALTHY', 38, 'CLASS_5', 38, @now, 1, 0, 0, 0, NULL, @now, '20000000-0000-0000-0000-000000000001')
ON DUPLICATE KEY UPDATE
    current_rating = VALUES(current_rating),
    race_class = VALUES(race_class),
    health_status = VALUES(health_status);

-- --------------------------------------------------------------------------
-- 5. Tournaments
-- --------------------------------------------------------------------------

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
     apply_break_time, break_start_time, break_end_time, max_rounds,
     status, phase, created_at, published_at,
     registration_open_at, registration_close_at, review_deadline_at,
     jockey_matching_deadline_at, scheduling_deadline_at, competition_start_at,
     current_round_name, min_round_gap_days, race_class, distance,
     top_weight_lbs, min_weight_lbs, equipment_weight_kg, handicap_enabled,
     max_approved_horses, max_approved_jockeys, max_approved_entries,
     planned_round_count, planned_race_count, bracket_plan_status,
     bracket_plan_version, created_by)
VALUES
    ('50000000-0000-0000-0000-000000000001', 'DEMO 1 - Race sắp diễn ra', 'Dùng test upcoming race, entry, AI probability, prediction và inspection.', CURRENT_DATE, DATE_ADD(CURRENT_DATE, INTERVAL 14 DAY), NULL, 'Trường đua Demo A', 100000, 50000, 10000000, 'THOROUGHBRED', 2, 12, 100, 30, 10, 50, 120, 5, 24, 90, 30, 9, 35, 0, 30, 30, '08:00:00', '18:00:00', 0, NULL, NULL, 1, 'ONGOING', 'RACING', DATE_SUB(@now, INTERVAL 20 DAY), DATE_SUB(@now, INTERVAL 15 DAY), DATE_SUB(@now, INTERVAL 20 DAY), DATE_SUB(@now, INTERVAL 15 DAY), DATE_SUB(@now, INTERVAL 11 DAY), DATE_SUB(@now, INTERVAL 7 DAY), DATE_SUB(@now, INTERVAL 3 DAY), @upcoming_start, 'Vòng 1 (Chung Kết)', 7, 'CLASS_1', 'MILE_1600M', 135, 115, 1.5, 0, 8, 8, 8, 1, 1, 'LOCKED', 1, '10000000-0000-0000-0000-000000000001'),
    ('50000000-0000-0000-0000-000000000002', 'DEMO 2 - Final chờ publish', 'Dùng test Admin publish report, prediction scoring, prize và final payout.', DATE_SUB(CURRENT_DATE, INTERVAL 2 DAY), DATE_ADD(CURRENT_DATE, INTERVAL 2 DAY), NULL, 'Trường đua Demo B', 100000, 50000, 10000000, 'THOROUGHBRED', 2, 12, 100, 30, 10, 50, 120, 5, 24, 90, 30, 9, 35, 0, 30, 30, '08:00:00', '18:00:00', 0, NULL, NULL, 1, 'ONGOING', 'RESULT_PENDING', DATE_SUB(@now, INTERVAL 30 DAY), DATE_SUB(@now, INTERVAL 25 DAY), DATE_SUB(@now, INTERVAL 30 DAY), DATE_SUB(@now, INTERVAL 25 DAY), DATE_SUB(@now, INTERVAL 20 DAY), DATE_SUB(@now, INTERVAL 15 DAY), DATE_SUB(@now, INTERVAL 10 DAY), @final_start, 'Vòng 1 (Chung Kết)', 7, 'CLASS_1', 'MILE_1600M', 135, 115, 1.5, 0, 8, 8, 8, 1, 1, 'LOCKED', 1, '10000000-0000-0000-0000-000000000001'),
    ('50000000-0000-0000-0000-000000000003', 'DEMO 3 - Hồ sơ và contract chờ duyệt', 'Dùng test số liệu pending trên Admin Dashboard.', DATE_ADD(CURRENT_DATE, INTERVAL 20 DAY), DATE_ADD(CURRENT_DATE, INTERVAL 30 DAY), NULL, 'Trường đua Demo C', 100000, 50000, 5000000, 'THOROUGHBRED', 2, 12, 100, 30, 10, 50, 120, 5, 24, 90, 30, 9, 35, 0, 30, 30, '08:00:00', '18:00:00', 0, NULL, NULL, 1, 'OPEN', 'REGISTRATION_REVIEW', DATE_SUB(@now, INTERVAL 5 DAY), DATE_SUB(@now, INTERVAL 4 DAY), DATE_SUB(@now, INTERVAL 5 DAY), DATE_SUB(@now, INTERVAL 1 DAY), DATE_ADD(@now, INTERVAL 3 DAY), DATE_ADD(@now, INTERVAL 7 DAY), DATE_ADD(@now, INTERVAL 11 DAY), DATE_ADD(@now, INTERVAL 20 DAY), NULL, 7, 'CLASS_5', 'SPRINT_1200M', 135, 115, 1.5, 0, 8, 8, 8, 1, 1, 'CONFIRMED', 1, '10000000-0000-0000-0000-000000000001')
ON DUPLICATE KEY UPDATE
    phase = VALUES(phase),
    status = VALUES(status),
    current_round_name = VALUES(current_round_name);

-- Prize structure chỉ áp dụng cho Final Tournament chờ publish.
INSERT INTO prize_structures
    (prize_structure_id, `prize_rank`, percentage, fixed_amount, is_active, tournament_id)
VALUES
    ('51000000-0000-0000-0000-000000000001', 1, 50, 0, 1, '50000000-0000-0000-0000-000000000002'),
    ('51000000-0000-0000-0000-000000000002', 2, 30, 0, 1, '50000000-0000-0000-0000-000000000002'),
    ('51000000-0000-0000-0000-000000000003', 3, 20, 0, 1, '50000000-0000-0000-0000-000000000002')
ON DUPLICATE KEY UPDATE percentage = VALUES(percentage), is_active = 1;

-- --------------------------------------------------------------------------
-- 6. Registrations
-- --------------------------------------------------------------------------

-- Upcoming Tournament: 8 horse registrations.
INSERT INTO horse_tournament_registrations
    (horse_tournament_reg_id, tournament_id, horse_id, owner_id, status,
     submitted_at, reviewed_by, reviewed_at, rejected_reason, withdrawn_at,
     withdraw_reason, note, rating_at_registration, race_class_at_registration)
VALUES
    ('60000000-0000-0000-0000-000000000101', '50000000-0000-0000-0000-000000000001', '40000000-0000-0000-0000-000000000101', '20000000-0000-0000-0000-000000000001', 'APPROVED', DATE_SUB(@now, INTERVAL 14 DAY), '10000000-0000-0000-0000-000000000001', DATE_SUB(@now, INTERVAL 12 DAY), NULL, NULL, NULL, 'Demo approved', 118, 'CLASS_1'),
    ('60000000-0000-0000-0000-000000000102', '50000000-0000-0000-0000-000000000001', '40000000-0000-0000-0000-000000000102', '20000000-0000-0000-0000-000000000001', 'APPROVED', DATE_SUB(@now, INTERVAL 14 DAY), '10000000-0000-0000-0000-000000000001', DATE_SUB(@now, INTERVAL 12 DAY), NULL, NULL, NULL, 'Demo approved', 112, 'CLASS_1'),
    ('60000000-0000-0000-0000-000000000103', '50000000-0000-0000-0000-000000000001', '40000000-0000-0000-0000-000000000103', '20000000-0000-0000-0000-000000000001', 'APPROVED', DATE_SUB(@now, INTERVAL 14 DAY), '10000000-0000-0000-0000-000000000001', DATE_SUB(@now, INTERVAL 12 DAY), NULL, NULL, NULL, 'Demo approved', 105, 'CLASS_1'),
    ('60000000-0000-0000-0000-000000000104', '50000000-0000-0000-0000-000000000001', '40000000-0000-0000-0000-000000000104', '20000000-0000-0000-0000-000000000001', 'APPROVED', DATE_SUB(@now, INTERVAL 14 DAY), '10000000-0000-0000-0000-000000000001', DATE_SUB(@now, INTERVAL 12 DAY), NULL, NULL, NULL, 'Demo approved', 99, 'CLASS_2'),
    ('60000000-0000-0000-0000-000000000105', '50000000-0000-0000-0000-000000000001', '40000000-0000-0000-0000-000000000105', '20000000-0000-0000-0000-000000000001', 'APPROVED', DATE_SUB(@now, INTERVAL 14 DAY), '10000000-0000-0000-0000-000000000001', DATE_SUB(@now, INTERVAL 12 DAY), NULL, NULL, NULL, 'Demo approved', 92, 'CLASS_2'),
    ('60000000-0000-0000-0000-000000000106', '50000000-0000-0000-0000-000000000001', '40000000-0000-0000-0000-000000000106', '20000000-0000-0000-0000-000000000001', 'APPROVED', DATE_SUB(@now, INTERVAL 14 DAY), '10000000-0000-0000-0000-000000000001', DATE_SUB(@now, INTERVAL 12 DAY), NULL, NULL, NULL, 'Demo approved', 86, 'CLASS_2'),
    ('60000000-0000-0000-0000-000000000107', '50000000-0000-0000-0000-000000000001', '40000000-0000-0000-0000-000000000107', '20000000-0000-0000-0000-000000000001', 'APPROVED', DATE_SUB(@now, INTERVAL 14 DAY), '10000000-0000-0000-0000-000000000001', DATE_SUB(@now, INTERVAL 12 DAY), NULL, NULL, NULL, 'Demo approved', 81, 'CLASS_2'),
    ('60000000-0000-0000-0000-000000000108', '50000000-0000-0000-0000-000000000001', '40000000-0000-0000-0000-000000000108', '20000000-0000-0000-0000-000000000001', 'APPROVED', DATE_SUB(@now, INTERVAL 14 DAY), '10000000-0000-0000-0000-000000000001', DATE_SUB(@now, INTERVAL 12 DAY), NULL, NULL, NULL, 'Demo approved', 76, 'CLASS_3')
ON DUPLICATE KEY UPDATE status = VALUES(status), reviewed_at = VALUES(reviewed_at);

-- Final Tournament: 8 horse registrations.
INSERT INTO horse_tournament_registrations
    (horse_tournament_reg_id, tournament_id, horse_id, owner_id, status,
     submitted_at, reviewed_by, reviewed_at, note,
     rating_at_registration, race_class_at_registration)
SELECT
    REPLACE(horse_tournament_reg_id, '60000000-0000-0000-0000-0000000001', '60000000-0000-0000-0000-0000000002'),
    '50000000-0000-0000-0000-000000000002', horse_id, owner_id, 'APPROVED',
    DATE_SUB(@now, INTERVAL 24 DAY), '10000000-0000-0000-0000-000000000001',
    DATE_SUB(@now, INTERVAL 22 DAY), 'Demo final approved',
    rating_at_registration, race_class_at_registration
FROM horse_tournament_registrations
WHERE tournament_id = '50000000-0000-0000-0000-000000000001'
ON DUPLICATE KEY UPDATE status = 'APPROVED';

-- Review Tournament: một hồ sơ ngựa chờ duyệt và một hồ sơ đã duyệt để tạo contract pending.
INSERT INTO horse_tournament_registrations
    (horse_tournament_reg_id, tournament_id, horse_id, owner_id, status,
     submitted_at, reviewed_by, reviewed_at, note,
     rating_at_registration, race_class_at_registration)
VALUES
    ('60000000-0000-0000-0000-000000000301', '50000000-0000-0000-0000-000000000003', '40000000-0000-0000-0000-000000000109', '20000000-0000-0000-0000-000000000001', 'PENDING_REVIEW', DATE_SUB(@now, INTERVAL 1 DAY), NULL, NULL, 'Hồ sơ dùng test dashboard pending', 35, 'CLASS_5'),
    ('60000000-0000-0000-0000-000000000302', '50000000-0000-0000-0000-000000000003', '40000000-0000-0000-0000-000000000110', '20000000-0000-0000-0000-000000000001', 'APPROVED', DATE_SUB(@now, INTERVAL 2 DAY), '10000000-0000-0000-0000-000000000001', DATE_SUB(@now, INTERVAL 1 DAY), 'Dùng tạo contract chờ Admin duyệt', 38, 'CLASS_5')
ON DUPLICATE KEY UPDATE status = VALUES(status), reviewed_at = VALUES(reviewed_at);

-- Jockey registration cho Upcoming và Final.
INSERT INTO jockey_tournament_registrations
    (jockey_tournament_reg_id, tournament_id, jockey_id, status, submitted_at,
     reviewed_by, reviewed_at, note, hire_fee)
VALUES
    ('61000000-0000-0000-0000-000000000101', '50000000-0000-0000-0000-000000000001', '21000000-0000-0000-0000-000000000101', 'APPROVED', DATE_SUB(@now, INTERVAL 14 DAY), NULL, DATE_SUB(@now, INTERVAL 14 DAY), 'Jockey auto approved', 1000000),
    ('61000000-0000-0000-0000-000000000102', '50000000-0000-0000-0000-000000000001', '21000000-0000-0000-0000-000000000102', 'APPROVED', DATE_SUB(@now, INTERVAL 14 DAY), NULL, DATE_SUB(@now, INTERVAL 14 DAY), 'Jockey auto approved', 1000000),
    ('61000000-0000-0000-0000-000000000103', '50000000-0000-0000-0000-000000000001', '21000000-0000-0000-0000-000000000103', 'APPROVED', DATE_SUB(@now, INTERVAL 14 DAY), NULL, DATE_SUB(@now, INTERVAL 14 DAY), 'Jockey auto approved', 1000000),
    ('61000000-0000-0000-0000-000000000104', '50000000-0000-0000-0000-000000000001', '21000000-0000-0000-0000-000000000104', 'APPROVED', DATE_SUB(@now, INTERVAL 14 DAY), NULL, DATE_SUB(@now, INTERVAL 14 DAY), 'Jockey auto approved', 1000000),
    ('61000000-0000-0000-0000-000000000105', '50000000-0000-0000-0000-000000000001', '21000000-0000-0000-0000-000000000105', 'APPROVED', DATE_SUB(@now, INTERVAL 14 DAY), NULL, DATE_SUB(@now, INTERVAL 14 DAY), 'Jockey auto approved', 1000000),
    ('61000000-0000-0000-0000-000000000106', '50000000-0000-0000-0000-000000000001', '21000000-0000-0000-0000-000000000106', 'APPROVED', DATE_SUB(@now, INTERVAL 14 DAY), NULL, DATE_SUB(@now, INTERVAL 14 DAY), 'Jockey auto approved', 1000000),
    ('61000000-0000-0000-0000-000000000107', '50000000-0000-0000-0000-000000000001', '21000000-0000-0000-0000-000000000107', 'APPROVED', DATE_SUB(@now, INTERVAL 14 DAY), NULL, DATE_SUB(@now, INTERVAL 14 DAY), 'Jockey auto approved', 1000000),
    ('61000000-0000-0000-0000-000000000108', '50000000-0000-0000-0000-000000000001', '21000000-0000-0000-0000-000000000108', 'APPROVED', DATE_SUB(@now, INTERVAL 14 DAY), NULL, DATE_SUB(@now, INTERVAL 14 DAY), 'Jockey auto approved', 1000000)
ON DUPLICATE KEY UPDATE status = 'APPROVED', hire_fee = VALUES(hire_fee);

INSERT INTO jockey_tournament_registrations
    (jockey_tournament_reg_id, tournament_id, jockey_id, status, submitted_at,
     reviewed_by, reviewed_at, note, hire_fee)
SELECT
    REPLACE(jockey_tournament_reg_id, '61000000-0000-0000-0000-0000000001', '61000000-0000-0000-0000-0000000002'),
    '50000000-0000-0000-0000-000000000002', jockey_id, 'APPROVED',
    DATE_SUB(@now, INTERVAL 24 DAY), NULL, DATE_SUB(@now, INTERVAL 24 DAY),
    'Jockey auto approved for final', hire_fee
FROM jockey_tournament_registrations
WHERE tournament_id = '50000000-0000-0000-0000-000000000001'
ON DUPLICATE KEY UPDATE status = 'APPROVED';

INSERT INTO jockey_tournament_registrations
    (jockey_tournament_reg_id, tournament_id, jockey_id, status, submitted_at,
     reviewed_by, reviewed_at, note, hire_fee)
VALUES
    ('61000000-0000-0000-0000-000000000301', '50000000-0000-0000-0000-000000000003', '21000000-0000-0000-0000-000000000101', 'APPROVED', DATE_SUB(@now, INTERVAL 2 DAY), NULL, DATE_SUB(@now, INTERVAL 2 DAY), 'Jockey auto approved for pending contract', 1000000)
ON DUPLICATE KEY UPDATE status = 'APPROVED';

-- --------------------------------------------------------------------------
-- 7. Contracts
-- --------------------------------------------------------------------------

INSERT INTO jockey_horse_contracts
    (contract_id, tournament_id, horse_tournament_reg_id,
     jockey_tournament_reg_id, owner_id, horse_id, jockey_id,
     hire_fee, advance_percent, final_percent, advance_paid_amount,
     escrow_amount, system_contract_fee, owner_prize_share_percent,
     jockey_prize_share_percent, payment_status, escrow_status,
     advance_payout_status, final_payout_status, status,
     advance_payout_at, final_payout_at, requested_at, responded_at,
     accepted_at, submitted_at, reviewed_by, reviewed_at, contract_note)
VALUES
    ('70000000-0000-0000-0000-000000000101', '50000000-0000-0000-0000-000000000001', '60000000-0000-0000-0000-000000000101', '61000000-0000-0000-0000-000000000101', '20000000-0000-0000-0000-000000000001', '40000000-0000-0000-0000-000000000101', '21000000-0000-0000-0000-000000000101', 1000000, 30, 70, 300000, 700000, 50000, 80, 20, 'PAID', 'PARTIALLY_RELEASED', 'PAID', 'NOT_RELEASED', 'APPROVED', DATE_SUB(@now, INTERVAL 8 DAY), NULL, DATE_SUB(@now, INTERVAL 12 DAY), DATE_SUB(@now, INTERVAL 11 DAY), DATE_SUB(@now, INTERVAL 11 DAY), DATE_SUB(@now, INTERVAL 10 DAY), '10000000-0000-0000-0000-000000000001', DATE_SUB(@now, INTERVAL 9 DAY), 'Upcoming contract 1'),
    ('70000000-0000-0000-0000-000000000102', '50000000-0000-0000-0000-000000000001', '60000000-0000-0000-0000-000000000102', '61000000-0000-0000-0000-000000000102', '20000000-0000-0000-0000-000000000001', '40000000-0000-0000-0000-000000000102', '21000000-0000-0000-0000-000000000102', 1000000, 30, 70, 300000, 700000, 50000, 80, 20, 'PAID', 'PARTIALLY_RELEASED', 'PAID', 'NOT_RELEASED', 'APPROVED', DATE_SUB(@now, INTERVAL 8 DAY), NULL, DATE_SUB(@now, INTERVAL 12 DAY), DATE_SUB(@now, INTERVAL 11 DAY), DATE_SUB(@now, INTERVAL 11 DAY), DATE_SUB(@now, INTERVAL 10 DAY), '10000000-0000-0000-0000-000000000001', DATE_SUB(@now, INTERVAL 9 DAY), 'Upcoming contract 2'),
    ('70000000-0000-0000-0000-000000000103', '50000000-0000-0000-0000-000000000001', '60000000-0000-0000-0000-000000000103', '61000000-0000-0000-0000-000000000103', '20000000-0000-0000-0000-000000000001', '40000000-0000-0000-0000-000000000103', '21000000-0000-0000-0000-000000000103', 1000000, 30, 70, 300000, 700000, 50000, 80, 20, 'PAID', 'PARTIALLY_RELEASED', 'PAID', 'NOT_RELEASED', 'APPROVED', DATE_SUB(@now, INTERVAL 8 DAY), NULL, DATE_SUB(@now, INTERVAL 12 DAY), DATE_SUB(@now, INTERVAL 11 DAY), DATE_SUB(@now, INTERVAL 11 DAY), DATE_SUB(@now, INTERVAL 10 DAY), '10000000-0000-0000-0000-000000000001', DATE_SUB(@now, INTERVAL 9 DAY), 'Upcoming contract 3'),
    ('70000000-0000-0000-0000-000000000104', '50000000-0000-0000-0000-000000000001', '60000000-0000-0000-0000-000000000104', '61000000-0000-0000-0000-000000000104', '20000000-0000-0000-0000-000000000001', '40000000-0000-0000-0000-000000000104', '21000000-0000-0000-0000-000000000104', 1000000, 30, 70, 300000, 700000, 50000, 80, 20, 'PAID', 'PARTIALLY_RELEASED', 'PAID', 'NOT_RELEASED', 'APPROVED', DATE_SUB(@now, INTERVAL 8 DAY), NULL, DATE_SUB(@now, INTERVAL 12 DAY), DATE_SUB(@now, INTERVAL 11 DAY), DATE_SUB(@now, INTERVAL 11 DAY), DATE_SUB(@now, INTERVAL 10 DAY), '10000000-0000-0000-0000-000000000001', DATE_SUB(@now, INTERVAL 9 DAY), 'Upcoming contract 4'),
    ('70000000-0000-0000-0000-000000000105', '50000000-0000-0000-0000-000000000001', '60000000-0000-0000-0000-000000000105', '61000000-0000-0000-0000-000000000105', '20000000-0000-0000-0000-000000000001', '40000000-0000-0000-0000-000000000105', '21000000-0000-0000-0000-000000000105', 1000000, 30, 70, 300000, 700000, 50000, 80, 20, 'PAID', 'PARTIALLY_RELEASED', 'PAID', 'NOT_RELEASED', 'APPROVED', DATE_SUB(@now, INTERVAL 8 DAY), NULL, DATE_SUB(@now, INTERVAL 12 DAY), DATE_SUB(@now, INTERVAL 11 DAY), DATE_SUB(@now, INTERVAL 11 DAY), DATE_SUB(@now, INTERVAL 10 DAY), '10000000-0000-0000-0000-000000000001', DATE_SUB(@now, INTERVAL 9 DAY), 'Upcoming contract 5'),
    ('70000000-0000-0000-0000-000000000106', '50000000-0000-0000-0000-000000000001', '60000000-0000-0000-0000-000000000106', '61000000-0000-0000-0000-000000000106', '20000000-0000-0000-0000-000000000001', '40000000-0000-0000-0000-000000000106', '21000000-0000-0000-0000-000000000106', 1000000, 30, 70, 300000, 700000, 50000, 80, 20, 'PAID', 'PARTIALLY_RELEASED', 'PAID', 'NOT_RELEASED', 'APPROVED', DATE_SUB(@now, INTERVAL 8 DAY), NULL, DATE_SUB(@now, INTERVAL 12 DAY), DATE_SUB(@now, INTERVAL 11 DAY), DATE_SUB(@now, INTERVAL 11 DAY), DATE_SUB(@now, INTERVAL 10 DAY), '10000000-0000-0000-0000-000000000001', DATE_SUB(@now, INTERVAL 9 DAY), 'Upcoming contract 6'),
    ('70000000-0000-0000-0000-000000000107', '50000000-0000-0000-0000-000000000001', '60000000-0000-0000-0000-000000000107', '61000000-0000-0000-0000-000000000107', '20000000-0000-0000-0000-000000000001', '40000000-0000-0000-0000-000000000107', '21000000-0000-0000-0000-000000000107', 1000000, 30, 70, 300000, 700000, 50000, 80, 20, 'PAID', 'PARTIALLY_RELEASED', 'PAID', 'NOT_RELEASED', 'APPROVED', DATE_SUB(@now, INTERVAL 8 DAY), NULL, DATE_SUB(@now, INTERVAL 12 DAY), DATE_SUB(@now, INTERVAL 11 DAY), DATE_SUB(@now, INTERVAL 11 DAY), DATE_SUB(@now, INTERVAL 10 DAY), '10000000-0000-0000-0000-000000000001', DATE_SUB(@now, INTERVAL 9 DAY), 'Upcoming contract 7'),
    ('70000000-0000-0000-0000-000000000108', '50000000-0000-0000-0000-000000000001', '60000000-0000-0000-0000-000000000108', '61000000-0000-0000-0000-000000000108', '20000000-0000-0000-0000-000000000001', '40000000-0000-0000-0000-000000000108', '21000000-0000-0000-0000-000000000108', 1000000, 30, 70, 300000, 700000, 50000, 80, 20, 'PAID', 'PARTIALLY_RELEASED', 'PAID', 'NOT_RELEASED', 'APPROVED', DATE_SUB(@now, INTERVAL 8 DAY), NULL, DATE_SUB(@now, INTERVAL 12 DAY), DATE_SUB(@now, INTERVAL 11 DAY), DATE_SUB(@now, INTERVAL 11 DAY), DATE_SUB(@now, INTERVAL 10 DAY), '10000000-0000-0000-0000-000000000001', DATE_SUB(@now, INTERVAL 9 DAY), 'Upcoming contract 8')
ON DUPLICATE KEY UPDATE status = VALUES(status), escrow_status = VALUES(escrow_status);

-- Clone 8 contract vào Final Tournament với escrow 70% chưa release.
INSERT INTO jockey_horse_contracts
    (contract_id, tournament_id, horse_tournament_reg_id,
     jockey_tournament_reg_id, owner_id, horse_id, jockey_id,
     hire_fee, advance_percent, final_percent, advance_paid_amount,
     escrow_amount, system_contract_fee, owner_prize_share_percent,
     jockey_prize_share_percent, payment_status, escrow_status,
     advance_payout_status, final_payout_status, status,
     advance_payout_at, requested_at, responded_at, accepted_at, submitted_at,
     reviewed_by, reviewed_at, contract_note)
SELECT
    REPLACE(contract_id, '70000000-0000-0000-0000-0000000001', '70000000-0000-0000-0000-0000000002'),
    '50000000-0000-0000-0000-000000000002',
    REPLACE(horse_tournament_reg_id, '60000000-0000-0000-0000-0000000001', '60000000-0000-0000-0000-0000000002'),
    REPLACE(jockey_tournament_reg_id, '61000000-0000-0000-0000-0000000001', '61000000-0000-0000-0000-0000000002'),
    owner_id, horse_id, jockey_id, hire_fee, advance_percent, final_percent,
    advance_paid_amount, 700000, system_contract_fee,
    owner_prize_share_percent, jockey_prize_share_percent,
    'PAID', 'PARTIALLY_RELEASED', 'PAID', 'NOT_RELEASED', 'APPROVED',
    DATE_SUB(@now, INTERVAL 18 DAY), DATE_SUB(@now, INTERVAL 22 DAY),
    DATE_SUB(@now, INTERVAL 21 DAY), DATE_SUB(@now, INTERVAL 21 DAY),
    DATE_SUB(@now, INTERVAL 20 DAY),
    '10000000-0000-0000-0000-000000000001', DATE_SUB(@now, INTERVAL 19 DAY),
    'Final contract waiting for automatic payout'
FROM jockey_horse_contracts
WHERE tournament_id = '50000000-0000-0000-0000-000000000001'
ON DUPLICATE KEY UPDATE
    escrow_amount = 700000,
    escrow_status = 'PARTIALLY_RELEASED',
    final_payout_status = 'NOT_RELEASED',
    final_payout_at = NULL,
    status = 'APPROVED';

-- Contract chờ Admin duyệt trên dashboard.
INSERT INTO jockey_horse_contracts
    (contract_id, tournament_id, horse_tournament_reg_id,
     jockey_tournament_reg_id, owner_id, horse_id, jockey_id,
     hire_fee, advance_percent, final_percent, advance_paid_amount,
     escrow_amount, system_contract_fee, owner_prize_share_percent,
     jockey_prize_share_percent, payment_status, escrow_status,
     advance_payout_status, final_payout_status, status,
     requested_at, responded_at, accepted_at, submitted_at, contract_note)
VALUES
    ('70000000-0000-0000-0000-000000000301', '50000000-0000-0000-0000-000000000003', '60000000-0000-0000-0000-000000000302', '61000000-0000-0000-0000-000000000301', '20000000-0000-0000-0000-000000000001', '40000000-0000-0000-0000-000000000110', '21000000-0000-0000-0000-000000000101', 1000000, 30, 70, 0, 1000000, 50000, 80, 20, 'PAID', 'HELD', 'NOT_PAID', 'NOT_RELEASED', 'PENDING_ADMIN_REVIEW', DATE_SUB(@now, INTERVAL 2 DAY), DATE_SUB(@now, INTERVAL 2 DAY), DATE_SUB(@now, INTERVAL 2 DAY), DATE_SUB(@now, INTERVAL 1 DAY), 'Dùng test Admin duyệt contract')
ON DUPLICATE KEY UPDATE status = 'PENDING_ADMIN_REVIEW';

-- --------------------------------------------------------------------------
-- 8. Round, Race, assignment và entry
-- --------------------------------------------------------------------------

INSERT INTO rounds
    (round_id, round_name, sequence_order, is_final, prediction_type,
     advancement_rule, start_date, end_date, description, max_races,
     max_entries, min_entries, status, head_referee_id,
     head_referee_assigned_at, expected_entries, planned_race_count,
     qualifiers_per_race, bracket_plan_version, advanced_at,
     transition_status, created_at, tournament_id, created_by)
VALUES
    ('80000000-0000-0000-0000-000000000001', 'Vòng 1 (Chung Kết)', 1, 1, 'TOP3', 'Xác định Top 3 chung cuộc', @upcoming_start, @upcoming_end, 'Round dùng test upcoming', 1, 16, 8, 'SCHEDULED', '20000000-0000-0000-0000-000000000004', DATE_SUB(@now, INTERVAL 2 DAY), 8, 1, 0, 1, NULL, 'NOT_READY', DATE_SUB(@now, INTERVAL 3 DAY), '50000000-0000-0000-0000-000000000001', '10000000-0000-0000-0000-000000000001'),
    ('80000000-0000-0000-0000-000000000002', 'Vòng 1 (Chung Kết)', 1, 1, 'TOP3', 'Xác định Top 3 chung cuộc', @final_start, @final_end, 'Round final chờ Admin publish', 1, 16, 8, 'FINISHED', '20000000-0000-0000-0000-000000000004', DATE_SUB(@now, INTERVAL 10 DAY), 8, 1, 0, 1, NULL, 'READY', DATE_SUB(@now, INTERVAL 10 DAY), '50000000-0000-0000-0000-000000000002', '10000000-0000-0000-0000-000000000001')
ON DUPLICATE KEY UPDATE status = VALUES(status), transition_status = VALUES(transition_status);

INSERT INTO races
    (race_id, name, start_time, end_time, track_condition, distance,
     sequence_order, status, started_at, finished_at, schedule_published_at,
     prediction_open_at, prediction_close_at, round_id, created_by,
     started_by, inspection_finalized_at, cancelled_at,
     cancellation_reason, rescheduled_at, reschedule_reason)
VALUES
    ('90000000-0000-0000-0000-000000000001', 'DEMO Upcoming Race', @upcoming_start, @upcoming_end, 'TURF', 'MILE_1600M', 1, 'SCHEDULED', NULL, NULL, DATE_SUB(@now, INTERVAL 1 DAY), DATE_SUB(@now, INTERVAL 1 DAY), DATE_SUB(@upcoming_start, INTERVAL 5 MINUTE), '80000000-0000-0000-0000-000000000001', '10000000-0000-0000-0000-000000000001', NULL, NULL, NULL, NULL, NULL, NULL),
    ('90000000-0000-0000-0000-000000000002', 'DEMO Final Signed Race', @final_start, @final_end, 'TURF', 'MILE_1600M', 1, 'FINISHED', @final_start, @final_end, DATE_SUB(@final_start, INTERVAL 1 DAY), DATE_SUB(@final_start, INTERVAL 1 DAY), DATE_SUB(@final_start, INTERVAL 5 MINUTE), '80000000-0000-0000-0000-000000000002', '10000000-0000-0000-0000-000000000001', '10000000-0000-0000-0000-000000000005', DATE_SUB(@final_start, INTERVAL 30 MINUTE), NULL, NULL, NULL, NULL)
ON DUPLICATE KEY UPDATE
    status = VALUES(status),
    start_time = VALUES(start_time),
    end_time = VALUES(end_time),
    prediction_open_at = VALUES(prediction_open_at),
    prediction_close_at = VALUES(prediction_close_at);

INSERT INTO race_referees
    (race_referee_id, race_id, referee_id, assigned_by, assigned_at)
VALUES
    ('91000000-0000-0000-0000-000000000001', '90000000-0000-0000-0000-000000000001', '20000000-0000-0000-0000-000000000004', '10000000-0000-0000-0000-000000000001', DATE_SUB(@now, INTERVAL 2 DAY)),
    ('91000000-0000-0000-0000-000000000002', '90000000-0000-0000-0000-000000000002', '20000000-0000-0000-0000-000000000004', '10000000-0000-0000-0000-000000000001', DATE_SUB(@now, INTERVAL 10 DAY))
ON DUPLICATE KEY UPDATE assigned_at = VALUES(assigned_at);

INSERT INTO race_inspection_staff_assignments
    (assignment_id, race_id, vet_id, med_staff_id, assigned_by, assigned_at)
VALUES
    ('92000000-0000-0000-0000-000000000001', '90000000-0000-0000-0000-000000000001', '20000000-0000-0000-0000-000000000005', '20000000-0000-0000-0000-000000000006', '10000000-0000-0000-0000-000000000001', DATE_SUB(@now, INTERVAL 2 DAY))
ON DUPLICATE KEY UPDATE vet_id = VALUES(vet_id), med_staff_id = VALUES(med_staff_id);

-- Upcoming entries: giữ CONFIRMED và chưa có inspection để Vet/Medical có thể test submit.
INSERT INTO race_entries
    (entry_id, race_id, contract_id, lane_number, status, assigned_by,
     assigned_at, created_at)
VALUES
    ('a0000000-0000-0000-0000-000000000101', '90000000-0000-0000-0000-000000000001', '70000000-0000-0000-0000-000000000101', 1, 'CONFIRMED', '10000000-0000-0000-0000-000000000001', DATE_SUB(@now, INTERVAL 2 DAY), DATE_SUB(@now, INTERVAL 2 DAY)),
    ('a0000000-0000-0000-0000-000000000102', '90000000-0000-0000-0000-000000000001', '70000000-0000-0000-0000-000000000102', 2, 'CONFIRMED', '10000000-0000-0000-0000-000000000001', DATE_SUB(@now, INTERVAL 2 DAY), DATE_SUB(@now, INTERVAL 2 DAY)),
    ('a0000000-0000-0000-0000-000000000103', '90000000-0000-0000-0000-000000000001', '70000000-0000-0000-0000-000000000103', 3, 'CONFIRMED', '10000000-0000-0000-0000-000000000001', DATE_SUB(@now, INTERVAL 2 DAY), DATE_SUB(@now, INTERVAL 2 DAY)),
    ('a0000000-0000-0000-0000-000000000104', '90000000-0000-0000-0000-000000000001', '70000000-0000-0000-0000-000000000104', 4, 'CONFIRMED', '10000000-0000-0000-0000-000000000001', DATE_SUB(@now, INTERVAL 2 DAY), DATE_SUB(@now, INTERVAL 2 DAY)),
    ('a0000000-0000-0000-0000-000000000105', '90000000-0000-0000-0000-000000000001', '70000000-0000-0000-0000-000000000105', 5, 'CONFIRMED', '10000000-0000-0000-0000-000000000001', DATE_SUB(@now, INTERVAL 2 DAY), DATE_SUB(@now, INTERVAL 2 DAY)),
    ('a0000000-0000-0000-0000-000000000106', '90000000-0000-0000-0000-000000000001', '70000000-0000-0000-0000-000000000106', 6, 'CONFIRMED', '10000000-0000-0000-0000-000000000001', DATE_SUB(@now, INTERVAL 2 DAY), DATE_SUB(@now, INTERVAL 2 DAY)),
    ('a0000000-0000-0000-0000-000000000107', '90000000-0000-0000-0000-000000000001', '70000000-0000-0000-0000-000000000107', 7, 'CONFIRMED', '10000000-0000-0000-0000-000000000001', DATE_SUB(@now, INTERVAL 2 DAY), DATE_SUB(@now, INTERVAL 2 DAY)),
    ('a0000000-0000-0000-0000-000000000108', '90000000-0000-0000-0000-000000000001', '70000000-0000-0000-0000-000000000108', 8, 'CONFIRMED', '10000000-0000-0000-0000-000000000001', DATE_SUB(@now, INTERVAL 2 DAY), DATE_SUB(@now, INTERVAL 2 DAY))
ON DUPLICATE KEY UPDATE status = 'CONFIRMED', lane_number = VALUES(lane_number);

-- Final entries: 6 FINISHED, 1 DNF, 1 DISQUALIFIED.
INSERT INTO race_entries
    (entry_id, race_id, contract_id, lane_number, status, assigned_by,
     assigned_at, disqualified_at, disqualified_reason, created_at)
VALUES
    ('a0000000-0000-0000-0000-000000000201', '90000000-0000-0000-0000-000000000002', '70000000-0000-0000-0000-000000000201', 1, 'FINISHED', '10000000-0000-0000-0000-000000000001', DATE_SUB(@now, INTERVAL 10 DAY), NULL, NULL, DATE_SUB(@now, INTERVAL 10 DAY)),
    ('a0000000-0000-0000-0000-000000000202', '90000000-0000-0000-0000-000000000002', '70000000-0000-0000-0000-000000000202', 2, 'FINISHED', '10000000-0000-0000-0000-000000000001', DATE_SUB(@now, INTERVAL 10 DAY), NULL, NULL, DATE_SUB(@now, INTERVAL 10 DAY)),
    ('a0000000-0000-0000-0000-000000000203', '90000000-0000-0000-0000-000000000002', '70000000-0000-0000-0000-000000000203', 3, 'FINISHED', '10000000-0000-0000-0000-000000000001', DATE_SUB(@now, INTERVAL 10 DAY), NULL, NULL, DATE_SUB(@now, INTERVAL 10 DAY)),
    ('a0000000-0000-0000-0000-000000000204', '90000000-0000-0000-0000-000000000002', '70000000-0000-0000-0000-000000000204', 4, 'FINISHED', '10000000-0000-0000-0000-000000000001', DATE_SUB(@now, INTERVAL 10 DAY), NULL, NULL, DATE_SUB(@now, INTERVAL 10 DAY)),
    ('a0000000-0000-0000-0000-000000000205', '90000000-0000-0000-0000-000000000002', '70000000-0000-0000-0000-000000000205', 5, 'FINISHED', '10000000-0000-0000-0000-000000000001', DATE_SUB(@now, INTERVAL 10 DAY), NULL, NULL, DATE_SUB(@now, INTERVAL 10 DAY)),
    ('a0000000-0000-0000-0000-000000000206', '90000000-0000-0000-0000-000000000002', '70000000-0000-0000-0000-000000000206', 6, 'FINISHED', '10000000-0000-0000-0000-000000000001', DATE_SUB(@now, INTERVAL 10 DAY), NULL, NULL, DATE_SUB(@now, INTERVAL 10 DAY)),
    ('a0000000-0000-0000-0000-000000000207', '90000000-0000-0000-0000-000000000002', '70000000-0000-0000-0000-000000000207', 7, 'DID_NOT_FINISH', '10000000-0000-0000-0000-000000000001', DATE_SUB(@now, INTERVAL 10 DAY), NULL, NULL, DATE_SUB(@now, INTERVAL 10 DAY)),
    ('a0000000-0000-0000-0000-000000000208', '90000000-0000-0000-0000-000000000002', '70000000-0000-0000-0000-000000000208', 8, 'DISQUALIFIED', '10000000-0000-0000-0000-000000000001', DATE_SUB(@now, INTERVAL 10 DAY), @final_end, 'Cản trở đối thủ ở đoạn cuối', DATE_SUB(@now, INTERVAL 10 DAY))
ON DUPLICATE KEY UPDATE status = VALUES(status), lane_number = VALUES(lane_number);

-- --------------------------------------------------------------------------
-- 9. AI probability cho Upcoming Race
-- --------------------------------------------------------------------------

INSERT INTO ai_predictions
    (prediction_id, entry_id, horse_current_rating, horse_recent_form,
     horse_win_rate, horse_top3_rate, jockey_win_rate, jockey_top3_rate,
     jockey_recent_form, pair_win_rate, pair_top3_rate, race_distance,
     track_condition, number_of_competitors, lane_number,
     assigned_weight_kg, actual_carried_weight_kg, carried_weight_ratio,
     relative_rating, win_probability, predicted_top_n, top_n_probability,
     confidence_score, prediction_reason, model_version, generated_at, created_at)
VALUES
    ('b0000000-0000-0000-0000-000000000101', 'a0000000-0000-0000-0000-000000000101', 118, 82, 35, 65, 20, 55, 70, 30, 60, 1600, 'TURF', 8, 1, 52, 52, 100, 100, 28, 3, 72, 88, 'Rating cao nhất và phong độ ổn định.', 'demo-v1', @now, @now),
    ('b0000000-0000-0000-0000-000000000102', 'a0000000-0000-0000-0000-000000000102', 112, 78, 28, 61, 18, 50, 66, 26, 57, 1600, 'TURF', 8, 2, 51, 51, 100, 95, 22, 3, 66, 84, 'Khả năng bứt tốc tốt ở cự ly một dặm.', 'demo-v1', @now, @now),
    ('b0000000-0000-0000-0000-000000000103', 'a0000000-0000-0000-0000-000000000103', 105, 72, 23, 55, 17, 48, 63, 22, 52, 1600, 'TURF', 8, 3, 53, 53, 100, 89, 17, 3, 58, 79, 'Thành tích Top 3 tốt nhưng rating thấp hơn hai ứng viên đầu.', 'demo-v1', @now, @now),
    ('b0000000-0000-0000-0000-000000000104', 'a0000000-0000-0000-0000-000000000104', 99, 68, 18, 47, 15, 43, 60, 17, 45, 1600, 'TURF', 8, 4, 50, 50, 100, 84, 12, 3, 45, 74, 'Có khả năng cạnh tranh vị trí nhưng xác suất thắng trung bình.', 'demo-v1', @now, @now),
    ('b0000000-0000-0000-0000-000000000105', 'a0000000-0000-0000-0000-000000000105', 92, 62, 16, 40, 13, 38, 55, 14, 38, 1600, 'TURF', 8, 5, 52, 52, 100, 78, 8, 3, 34, 69, 'Phong độ gần đây chưa đủ nổi bật.', 'demo-v1', @now, @now),
    ('b0000000-0000-0000-0000-000000000106', 'a0000000-0000-0000-0000-000000000106', 86, 58, 13, 36, 12, 35, 52, 12, 34, 1600, 'TURF', 8, 6, 50, 50, 100, 73, 6, 3, 28, 63, 'Cần cải thiện khả năng duy trì tốc độ cuối race.', 'demo-v1', @now, @now),
    ('b0000000-0000-0000-0000-000000000107', 'a0000000-0000-0000-0000-000000000107', 81, 52, 13, 33, 8, 28, 47, 10, 30, 1600, 'TURF', 8, 7, 53, 53, 100, 69, 4, 3, 23, 58, 'Rating và kinh nghiệm cặp ngựa-kỵ sĩ còn hạn chế.', 'demo-v1', @now, @now),
    ('b0000000-0000-0000-0000-000000000108', 'a0000000-0000-0000-0000-000000000108', 76, 48, 7, 29, 10, 30, 50, 8, 27, 1600, 'TURF', 8, 8, 51, 51, 100, 64, 3, 3, 18, 52, 'Ứng viên cần tạo bất ngờ để vào Top 3.', 'demo-v1', @now, @now)
ON DUPLICATE KEY UPDATE
    win_probability = VALUES(win_probability),
    top_n_probability = VALUES(top_n_probability),
    generated_at = @now;

-- --------------------------------------------------------------------------
-- 10. Final results, Signed report, prediction scoring input
-- --------------------------------------------------------------------------

INSERT INTO race_results
    (result_id, race_id, entry_id, finish_time, finish_position,
     prize_money, owner_prize_amount, jockey_prize_amount, prize_status,
     is_prize_paid, prize_paid_at, status, recorded_by, recorded_at, updated_at)
VALUES
    ('c0000000-0000-0000-0000-000000000201', '90000000-0000-0000-0000-000000000002', 'a0000000-0000-0000-0000-000000000201', 95.21, 1, 0, 0, 0, 'PendingPayout', 0, NULL, 'FINISHED', '10000000-0000-0000-0000-000000000005', @final_end, @final_end),
    ('c0000000-0000-0000-0000-000000000202', '90000000-0000-0000-0000-000000000002', 'a0000000-0000-0000-0000-000000000202', 95.78, 2, 0, 0, 0, 'PendingPayout', 0, NULL, 'FINISHED', '10000000-0000-0000-0000-000000000005', @final_end, @final_end),
    ('c0000000-0000-0000-0000-000000000203', '90000000-0000-0000-0000-000000000002', 'a0000000-0000-0000-0000-000000000203', 96.10, 3, 0, 0, 0, 'PendingPayout', 0, NULL, 'FINISHED', '10000000-0000-0000-0000-000000000005', @final_end, @final_end),
    ('c0000000-0000-0000-0000-000000000204', '90000000-0000-0000-0000-000000000002', 'a0000000-0000-0000-0000-000000000204', 96.55, 4, 0, 0, 0, 'NotEligible', 0, NULL, 'FINISHED', '10000000-0000-0000-0000-000000000005', @final_end, @final_end),
    ('c0000000-0000-0000-0000-000000000205', '90000000-0000-0000-0000-000000000002', 'a0000000-0000-0000-0000-000000000205', 97.02, 5, 0, 0, 0, 'NotEligible', 0, NULL, 'FINISHED', '10000000-0000-0000-0000-000000000005', @final_end, @final_end),
    ('c0000000-0000-0000-0000-000000000206', '90000000-0000-0000-0000-000000000002', 'a0000000-0000-0000-0000-000000000206', 98.44, 6, 0, 0, 0, 'NotEligible', 0, NULL, 'FINISHED', '10000000-0000-0000-0000-000000000005', @final_end, @final_end),
    ('c0000000-0000-0000-0000-000000000207', '90000000-0000-0000-0000-000000000002', 'a0000000-0000-0000-0000-000000000207', NULL, NULL, 0, 0, 0, 'NotEligible', 0, NULL, 'DID_NOT_FINISH', '10000000-0000-0000-0000-000000000005', @final_end, @final_end),
    ('c0000000-0000-0000-0000-000000000208', '90000000-0000-0000-0000-000000000002', 'a0000000-0000-0000-0000-000000000208', NULL, NULL, 0, 0, 0, 'NotEligible', 0, NULL, 'DISQUALIFIED', '10000000-0000-0000-0000-000000000005', @final_end, @final_end)
ON DUPLICATE KEY UPDATE
    finish_time = VALUES(finish_time),
    finish_position = VALUES(finish_position),
    prize_money = 0,
    owner_prize_amount = 0,
    jockey_prize_amount = 0,
    status = VALUES(status),
    prize_status = VALUES(prize_status),
    is_prize_paid = VALUES(is_prize_paid),
    prize_paid_at = VALUES(prize_paid_at);

INSERT INTO race_reports
    (report_id, race_id, referee_id, summary, appeal_note, status,
     signed_by, signed_at, published_by, published_at, created_at)
VALUES
    ('d0000000-0000-0000-0000-000000000001', '90000000-0000-0000-0000-000000000002', '20000000-0000-0000-0000-000000000004', 'Kết quả Final Race đã được xác nhận. Có một DNF và một trường hợp bị loại.', 'Các appeal đã được xử lý trước khi ký.', 'Signed', '20000000-0000-0000-0000-000000000004', DATE_ADD(@final_end, INTERVAL 30 MINUTE), NULL, NULL, @final_end)
ON DUPLICATE KEY UPDATE
    status = 'Signed',
    published_by = NULL,
    published_at = NULL;

-- Spectator lịch sử dự đoán đúng Top 3. Khi Admin publish report, prediction này
-- chuyển PENDING -> SCORED và được cộng điểm.
INSERT INTO predictions
    (prediction_id, spectator_id, race_id, prediction_type, prediction_time,
     status, reward_points, scored_at, voided_at, void_reason)
VALUES
    ('e0000000-0000-0000-0000-000000000001', '20000000-0000-0000-0000-000000000003', '90000000-0000-0000-0000-000000000002', 'TOP3', DATE_SUB(@final_start, INTERVAL 1 HOUR), 'PENDING', NULL, NULL, NULL, NULL)
ON DUPLICATE KEY UPDATE status = 'PENDING', reward_points = NULL, scored_at = NULL;

INSERT INTO prediction_detail
    (prediction_detail_id, prediction_id, entry_id, predicted_rank, status, awarded_points)
VALUES
    ('e1000000-0000-0000-0000-000000000001', 'e0000000-0000-0000-0000-000000000001', 'a0000000-0000-0000-0000-000000000201', 1, 'UNSCORED', NULL),
    ('e1000000-0000-0000-0000-000000000002', 'e0000000-0000-0000-0000-000000000001', 'a0000000-0000-0000-0000-000000000202', 2, 'UNSCORED', NULL),
    ('e1000000-0000-0000-0000-000000000003', 'e0000000-0000-0000-0000-000000000001', 'a0000000-0000-0000-0000-000000000203', 3, 'UNSCORED', NULL)
ON DUPLICATE KEY UPDATE status = 'UNSCORED', awarded_points = NULL;

-- --------------------------------------------------------------------------
-- 11. Violation, appeal và evidence
-- --------------------------------------------------------------------------

INSERT INTO violations
    (violation_id, entry_id, referee_id, type, description, penalty_type,
     penalty_value, occurred_at, created_at, status)
VALUES
    ('f0000000-0000-0000-0000-000000000001', 'a0000000-0000-0000-0000-000000000208', '20000000-0000-0000-0000-000000000004', 'OBSTRUCTION', 'Cản trở ngựa khác ở đoạn nước rút.', 'DISQUALIFIED', NULL, DATE_SUB(@final_end, INTERVAL 2 MINUTE), @final_end, 'RESOLVED')
ON DUPLICATE KEY UPDATE status = 'RESOLVED';

INSERT INTO appeal_categories
    (category_id, code, name, description, is_active, created_at)
VALUES
    ('f1000000-0000-0000-0000-000000000001', 'RESULT_REVIEW', 'Xem xét kết quả', 'Khiếu nại liên quan đến thứ hạng hoặc thời gian.', 1, @now),
    ('f1000000-0000-0000-0000-000000000002', 'VIOLATION_REVIEW', 'Xem xét vi phạm', 'Khiếu nại liên quan đến quyết định vi phạm.', 1, @now)
ON DUPLICATE KEY UPDATE name = VALUES(name), is_active = 1;

INSERT INTO appeals
    (appeal_id, entry_id, race_result_id, related_violation_id, category_id,
     submitted_by_user_id, description, status, submitted_at,
     reviewed_by_referee_id, reviewed_at, resolution)
VALUES
    ('f2000000-0000-0000-0000-000000000001', 'a0000000-0000-0000-0000-000000000202', 'c0000000-0000-0000-0000-000000000202', NULL, 'f1000000-0000-0000-0000-000000000001', '10000000-0000-0000-0000-000000000002', 'Đề nghị kiểm tra lại camera tại vạch đích.', 'Accepted', DATE_ADD(@final_end, INTERVAL 5 MINUTE), '20000000-0000-0000-0000-000000000004', DATE_ADD(@final_end, INTERVAL 20 MINUTE), 'Đã kiểm tra camera; kết quả hiện tại được xác nhận.'),
    ('f2000000-0000-0000-0000-000000000002', 'a0000000-0000-0000-0000-000000000208', 'c0000000-0000-0000-0000-000000000208', 'f0000000-0000-0000-0000-000000000001', 'f1000000-0000-0000-0000-000000000002', '10000000-0000-0000-0000-000000000108', 'Đề nghị xem lại quyết định loại khỏi cuộc đua.', 'Rejected', DATE_ADD(@final_end, INTERVAL 6 MINUTE), '20000000-0000-0000-0000-000000000004', DATE_ADD(@final_end, INTERVAL 22 MINUTE), 'Bằng chứng xác nhận hành vi cản trở; giữ nguyên quyết định.')
ON DUPLICATE KEY UPDATE status = VALUES(status), resolution = VALUES(resolution);

INSERT INTO appeal_evidences
    (evidence_id, appeal_id, type, file_url, text_content, description, uploaded_at)
VALUES
    ('f3000000-0000-0000-0000-000000000001', 'f2000000-0000-0000-0000-000000000001', 'Image', 'https://example.com/demo-finish-line.jpg', NULL, 'Ảnh vạch đích dùng cho demo.', DATE_ADD(@final_end, INTERVAL 7 MINUTE)),
    ('f3000000-0000-0000-0000-000000000002', 'f2000000-0000-0000-0000-000000000002', 'Text', NULL, 'Kỵ sĩ cho rằng không có chủ ý cản trở.', 'Nội dung giải trình dùng cho demo.', DATE_ADD(@final_end, INTERVAL 8 MINUTE))
ON DUPLICATE KEY UPDATE description = VALUES(description);

-- --------------------------------------------------------------------------
-- 12. Transaction history ban đầu
-- --------------------------------------------------------------------------

INSERT INTO wallet_transactions
    (transaction_id, wallet_id, invoice_id, race_result_id, contract_id,
     type, direction, amount, balance_before, balance_after,
     counterparty_wallet_id, counterparty_type, transaction_group_id,
     status, note, performed_by_user_id, created_at)
VALUES
    ('fa000000-0000-0000-0000-000000000001', '30000000-0000-0000-0000-000000000001', NULL, NULL, NULL, 'DEPOSIT', 'CREDIT', 10000000, 0, 10000000, NULL, 'EXTERNAL', 'fb000000-0000-0000-0000-000000000001', 'SUCCESS', 'Nạp tiền demo cho Owner', NULL, DATE_SUB(@now, INTERVAL 30 DAY)),
    ('fa000000-0000-0000-0000-000000000002', '30000000-0000-0000-0000-000000000101', NULL, NULL, '70000000-0000-0000-0000-000000000201', 'JOCKEY_HIRING_ADVANCE_INCOME', 'CREDIT', 300000, 0, 300000, @wallet_escrow, 'SYSTEM', 'fb000000-0000-0000-0000-000000000002', 'SUCCESS', 'Nhận tạm ứng hợp đồng Final Demo', NULL, DATE_SUB(@now, INTERVAL 18 DAY)),
    ('fa000000-0000-0000-0000-000000000003', @wallet_prize, NULL, NULL, NULL, 'SYSTEM_PRIZE_POOL_TOP_UP', 'CREDIT', 50000000, 0, 50000000, NULL, 'EXTERNAL', 'fb000000-0000-0000-0000-000000000003', 'SUCCESS', 'Admin bổ sung quỹ giải thưởng phục vụ demo', '10000000-0000-0000-0000-000000000001', DATE_SUB(@now, INTERVAL 30 DAY))
ON DUPLICATE KEY UPDATE
    amount = VALUES(amount),
    balance_after = VALUES(balance_after),
    status = 'SUCCESS';

-- --------------------------------------------------------------------------
-- 13. Notification mẫu
-- --------------------------------------------------------------------------

INSERT INTO notification_events
    (event_id, event_type, aggregate_type, aggregate_id, deduplication_key,
     payload_json, status, attempt_count, next_retry_at, last_error,
     created_at, processed_at)
VALUES
    ('fc000000-0000-0000-0000-000000000001', 'SCHEDULE_PUBLISHED', 'RACE', '90000000-0000-0000-0000-000000000001', 'demo-schedule-published-90000000', JSON_OBJECT('raceId', '90000000-0000-0000-0000-000000000001', 'raceName', 'DEMO Upcoming Race'), 'PROCESSED', 1, NULL, NULL, @now, @now)
ON DUPLICATE KEY UPDATE status = 'PROCESSED', processed_at = @now;

INSERT INTO notifications
    (notification_id, event_id, recipient_user_id, title, content,
     related_type, related_id, visible_in_app, show_toast,
     is_read, read_at, archived_at, created_at)
VALUES
    ('fd000000-0000-0000-0000-000000000001', 'fc000000-0000-0000-0000-000000000001', '10000000-0000-0000-0000-000000000004', 'Lịch đua đã được công bố', 'DEMO Upcoming Race sẽ bắt đầu sau khoảng 60 phút.', 'RACE', '90000000-0000-0000-0000-000000000001', 1, 1, 0, NULL, NULL, @now)
ON DUPLICATE KEY UPDATE content = VALUES(content), is_read = 0, read_at = NULL;

INSERT INTO notification_deliveries
    (delivery_id, notification_id, channel, status, attempt_count,
     next_retry_at, sent_at, last_error, created_at, updated_at)
VALUES
    ('fe000000-0000-0000-0000-000000000001', 'fd000000-0000-0000-0000-000000000001', 'WEB_SOCKET', 'SENT', 1, NULL, @now, NULL, @now, @now),
    ('fe000000-0000-0000-0000-000000000002', 'fd000000-0000-0000-0000-000000000001', 'EMAIL', 'SKIPPED', 0, NULL, NULL, 'Demo data - không gửi email thật', @now, @now)
ON DUPLICATE KEY UPDATE status = VALUES(status), updated_at = @now;

-- --------------------------------------------------------------------------
-- 14. Kiểm tra nhanh sau khi import
-- --------------------------------------------------------------------------

SELECT 'Tournaments' AS demo_group, COUNT(*) AS total
FROM tournaments WHERE tournament_id LIKE '50000000-%'
UNION ALL
SELECT 'Pending horse registrations', COUNT(*)
FROM horse_tournament_registrations WHERE status = 'PENDING_REVIEW'
UNION ALL
SELECT 'Pending admin contracts', COUNT(*)
FROM jockey_horse_contracts WHERE status = 'PENDING_ADMIN_REVIEW'
UNION ALL
SELECT 'Scheduled races', COUNT(*)
FROM races WHERE status = 'SCHEDULED'
UNION ALL
SELECT 'Upcoming entries', COUNT(*)
FROM race_entries WHERE race_id = '90000000-0000-0000-0000-000000000001'
UNION ALL
SELECT 'Final results', COUNT(*)
FROM race_results WHERE race_id = '90000000-0000-0000-0000-000000000002';

-- API cần test sau khi import:
--   GET  /api/admin/dashboard/summary
--   GET  /api/spectator/races/upcoming
--   GET  /api/spectator/races/90000000-0000-0000-0000-000000000001
--   GET  /api/spectator/races/90000000-0000-0000-0000-000000000001/ai-predictions
--   POST /api/spectator/races/90000000-0000-0000-0000-000000000001/predictions
--        Đăng nhập spectator1 để tạo dự đoán Top 3 mới.
--   GET  /api/admin/races/90000000-0000-0000-0000-000000000002/report
--   POST /api/admin/races/90000000-0000-0000-0000-000000000002/report/publish
--        Sau request này kiểm tra Owner/Jockey wallet và transaction history.
-- ============================================================================

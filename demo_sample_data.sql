-- ============================================================================
-- HRTMS - FRESH DEMO DATA CHO LUỒNG 06 -> 11
--
-- Kịch bản A: DEMO FULL 8
--   8 ngựa, một Final Race, dùng để test xuyên suốt:
--   Inspection -> Prediction -> Start -> Violation -> Finish -> Appeal
--   -> Race Report -> Head Referee ký -> Admin publish -> Rating/Prize/Payout.
--
-- Kịch bản B: DEMO BRACKET 16
--   16 ngựa thực tế, 2 Race vòng 1 (8 + 8), Top 4 mỗi Race vào Final 8.
--   Theo thuật toán BE hiện tại, maxApprovedEntries <= 16 chỉ sinh một Final.
--   Vì vậy tournament này cấu hình sức chứa 32 nhưng chỉ có 16 contract APPROVED.
--   Đây là cấu trúc hợp lệ: 2 Race x tối thiểu 8 entry -> 1 Final x 8 entry.
--
-- Cách chạy:
--   1. Dừng BE để scheduler không thay đổi dữ liệu khi đang seed.
--   2. Schema phải tồn tại và đã ở migration mới nhất.
--   3. Chạy TOÀN BỘ file bằng chế độ Run Script của MySQL 8+/DataGrip.
--   4. Khởi động BE và test theo docs/fresh-demo-test-guide.md.
--
-- File này xóa toàn bộ dữ liệu nghiệp vụ nhưng giữ nguyên schema và
-- flyway_schema_history. Không dùng TRUNCATE nên không gặp MySQL error 1701.
-- Mọi cửa sổ demo được tính lại từ NOW() mỗi lần chạy file.
-- ============================================================================

USE SWP391_Project_HRTMS;
SET NAMES utf8mb4;

SET @OLD_FOREIGN_KEY_CHECKS = @@FOREIGN_KEY_CHECKS;
SET @OLD_SQL_SAFE_UPDATES = @@SQL_SAFE_UPDATES;
SET FOREIGN_KEY_CHECKS = 0;
SET SQL_SAFE_UPDATES = 0;

-- --------------------------------------------------------------------------
-- 1. XÓA SẠCH DỮ LIỆU CŨ, GIỮ LẠI FLYWAY HISTORY
-- --------------------------------------------------------------------------

DROP PROCEDURE IF EXISTS ResetAllDemoData;
DELIMITER $$
CREATE PROCEDURE ResetAllDemoData()
BEGIN
    DECLARE done INT DEFAULT 0;
    DECLARE table_to_clear VARCHAR(128);
    DECLARE table_cursor CURSOR FOR
        SELECT table_name
        FROM information_schema.tables
        WHERE table_schema = DATABASE()
          AND table_type = 'BASE TABLE'
          AND table_name <> 'flyway_schema_history';
    DECLARE CONTINUE HANDLER FOR NOT FOUND SET done = 1;

    IF NOT EXISTS (
        SELECT 1
        FROM information_schema.tables
        WHERE table_schema = DATABASE() AND table_name = 'users'
    ) THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Schema chưa tồn tại. Hãy chạy BE/Hibernate/Flyway tạo bảng trước khi chạy seed.';
    END IF;

    OPEN table_cursor;
    clear_loop: LOOP
        FETCH table_cursor INTO table_to_clear;
        IF done = 1 THEN
            LEAVE clear_loop;
        END IF;
        SET @delete_sql = CONCAT('DELETE FROM `', table_to_clear, '`');
        PREPARE delete_statement FROM @delete_sql;
        EXECUTE delete_statement;
        DEALLOCATE PREPARE delete_statement;
    END LOOP;
    CLOSE table_cursor;
END$$
DELIMITER ;

CALL ResetAllDemoData();
DROP PROCEDURE ResetAllDemoData;

-- Bật lại FK ngay sau khi dọn sạch để mọi INSERT phía dưới được MySQL kiểm tra.
SET FOREIGN_KEY_CHECKS = @OLD_FOREIGN_KEY_CHECKS;

SET @seed_now = NOW();
SET @account_created_at = DATE_SUB(@seed_now, INTERVAL 45 DAY);
SET @demo_password = '$2a$12$ZGrUyKDU0UvqY0kpswOtoO58uurKVC2yVAA0iTlcnYI4pmPb18mBS';

SET @admin_user_id = '11111111-1111-1111-1111-111111111111';
SET @owner1_user_id = '11111111-1111-1111-1111-111111111121';
SET @owner2_user_id = '11111111-1111-1111-1111-111111111122';
SET @owner1_id = '12111111-1111-1111-1111-111111111121';
SET @owner2_id = '12111111-1111-1111-1111-111111111122';

SET @full_tournament_id = '10000000-0000-0000-0000-000000000001';
SET @bracket_tournament_id = '10000000-0000-0000-0000-000000000002';

SET @full_round_id = '20000000-0000-0000-0000-000000000001';
SET @bracket_round1_id = '20000000-0000-0000-0000-000000000021';
SET @bracket_final_round_id = '20000000-0000-0000-0000-000000000022';

SET @full_race_id = '30000000-0000-0000-0000-000000000001';
SET @bracket_race1_id = '30000000-0000-0000-0000-000000000021';
SET @bracket_race2_id = '30000000-0000-0000-0000-000000000022';
SET @bracket_final_race_id = '30000000-0000-0000-0000-000000000023';

-- --------------------------------------------------------------------------
-- 2. ROLE, TÀI KHOẢN, PROFILE VÀ VÍ
-- --------------------------------------------------------------------------

INSERT INTO roles (role_id, role_name, description, is_active, created_at)
VALUES
('00000000-0000-0000-0000-000000000001', 'ADMIN', 'Quản trị hệ thống', 1, @account_created_at),
('00000000-0000-0000-0000-000000000002', 'HORSE_OWNER', 'Chủ ngựa', 1, @account_created_at),
('00000000-0000-0000-0000-000000000003', 'JOCKEY', 'Kỵ sĩ', 1, @account_created_at),
('00000000-0000-0000-0000-000000000004', 'SPECTATOR', 'Khán giả', 1, @account_created_at),
('00000000-0000-0000-0000-000000000005', 'REFEREE', 'Trọng tài', 1, @account_created_at),
('00000000-0000-0000-0000-000000000006', 'VETERINARIAN', 'Bác sĩ thú y', 1, @account_created_at),
('00000000-0000-0000-0000-000000000007', 'MEDICAL_STAFF', 'Nhân viên y tế', 1, @account_created_at);

INSERT INTO users
    (user_id, username, password, email, dob, gender, full_name,
     phone_number, image_url, status, role_id, created_at)
VALUES
(@admin_user_id, 'admin1', @demo_password, 'admin1@hrtms.test', '1990-01-01',
 'MALE', 'Quản trị viên Demo', '0900000001', NULL, 'ACTIVE',
 '00000000-0000-0000-0000-000000000001', @account_created_at),
(@owner1_user_id, 'owner1', @demo_password, 'owner1@hrtms.test', '1988-05-15',
 'MALE', 'Chủ ngựa Full Flow', '0900000011', NULL, 'ACTIVE',
 '00000000-0000-0000-0000-000000000002', @account_created_at),
(@owner2_user_id, 'owner2', @demo_password, 'owner2@hrtms.test', '1987-06-20',
 'FEMALE', 'Chủ ngựa Bracket', '0900000012', NULL, 'ACTIVE',
 '00000000-0000-0000-0000-000000000002', @account_created_at),
('11111111-1111-1111-1111-111111111131', 'spectator1', @demo_password,
 'spectator1@hrtms.test', '1998-01-01', 'MALE', 'Khán giả 1', '0900000021',
 NULL, 'ACTIVE', '00000000-0000-0000-0000-000000000004', @account_created_at),
('11111111-1111-1111-1111-111111111132', 'spectator2', @demo_password,
 'spectator2@hrtms.test', '1999-02-02', 'FEMALE', 'Khán giả 2', '0900000022',
 NULL, 'ACTIVE', '00000000-0000-0000-0000-000000000004', @account_created_at);

INSERT INTO horse_owners
    (owner_id, user_id, farm_name, address, created_at)
VALUES
(@owner1_id, @owner1_user_id, 'Trang trại Full Flow', 'TP. Hồ Chí Minh', @account_created_at),
(@owner2_id, @owner2_user_id, 'Trang trại Bracket', 'Đà Nẵng', @account_created_at);

INSERT INTO spectators (spectator_id, user_id, total_points, created_at)
VALUES
('13111111-1111-1111-1111-111111111131',
 '11111111-1111-1111-1111-111111111131', 0, @account_created_at),
('13111111-1111-1111-1111-111111111132',
 '11111111-1111-1111-1111-111111111132', 0, @account_created_at);

INSERT INTO wallets
    (wallet_id, owner_type, balance, currency, status, wallet_purpose,
     user_id, created_at, updated_at)
VALUES
('aaaa0000-0000-0000-0000-000000000011', 'USER', 100000000.00,
 'VND', 'ACTIVE', 'USER_MAIN', @owner1_user_id, @account_created_at, NOW()),
('aaaa0000-0000-0000-0000-000000000012', 'USER', 100000000.00,
 'VND', 'ACTIVE', 'USER_MAIN', @owner2_user_id, @account_created_at, NOW()),
('aaaa0000-0000-0000-0000-000000000001', 'SYSTEM', 500000000.00,
 'VND', 'ACTIVE', 'SYSTEM_REVENUE', NULL, @account_created_at, NOW()),
('aaaa0000-0000-0000-0000-000000000002', 'SYSTEM', 1000000000.00,
 'VND', 'ACTIVE', 'SYSTEM_ESCROW', NULL, @account_created_at, NOW()),
('aaaa0000-0000-0000-0000-000000000003', 'SYSTEM', 1000000000.00,
 'VND', 'ACTIVE', 'SYSTEM_PRIZE_POOL', NULL, @account_created_at, NOW());

DROP PROCEDURE IF EXISTS SeedStaffAccounts;
DELIMITER $$
CREATE PROCEDURE SeedStaffAccounts()
BEGIN
    DECLARE i INT DEFAULT 1;
    DECLARE v_user_id CHAR(36);
    DECLARE v_profile_id CHAR(36);

    -- 24 jockey: jockey1..jockey24.
    WHILE i <= 24 DO
        SET v_user_id = UUID();
        SET v_profile_id = UUID();
        INSERT INTO users
            (user_id, username, password, email, dob, gender, full_name,
             phone_number, status, role_id, created_at)
        VALUES
            (v_user_id, CONCAT('jockey', i), @demo_password,
             CONCAT('jockey', i, '@hrtms.test'), '1997-03-10',
             IF(MOD(i, 5) = 0, 'FEMALE', 'MALE'), CONCAT('Kỵ sĩ ', i),
             CONCAT('091', LPAD(i, 7, '0')), 'ACTIVE',
             '00000000-0000-0000-0000-000000000003', @account_created_at);
        INSERT INTO jockeys
            (jockey_id, user_id, height, weight, experience_years,
             specialization, status, total_races, total_wins,
             jockey_tier, tier_updated_at, created_at)
        VALUES
            (v_profile_id, v_user_id, 1.60 + MOD(i, 8) / 100,
             49 + MOD(i, 5), 2 + MOD(i, 9),
             'MILE', 'AVAILABLE', 10 + i, MOD(i, 7),
             CASE MOD(i, 4)
                 WHEN 0 THEN 'ELITE'
                 WHEN 1 THEN 'APPRENTICE'
                 WHEN 2 THEN 'JUNIOR'
                 ELSE 'PROFESSIONAL'
             END,
             NOW(), @account_created_at);
        INSERT INTO wallets
            (wallet_id, owner_type, balance, currency, status, wallet_purpose,
             user_id, created_at, updated_at)
        VALUES
            (UUID(), 'USER', 10000000.00, 'VND', 'ACTIVE', 'USER_MAIN',
             v_user_id, @account_created_at, NOW());
        SET i = i + 1;
    END WHILE;

    -- 7 referee: referee1, referee3, referee6 là Head Referee;
    -- referee2, referee4, referee5 là Race Referee; referee7 dự phòng.
    SET i = 1;
    WHILE i <= 7 DO
        SET v_user_id = UUID();
        SET v_profile_id = UUID();
        INSERT INTO users
            (user_id, username, password, email, dob, gender, full_name,
             phone_number, status, role_id, created_at)
        VALUES
            (v_user_id, CONCAT('referee', i), @demo_password,
             CONCAT('referee', i, '@hrtms.test'), '1985-02-12', 'MALE',
             CONCAT('Trọng tài ', i), CONCAT('092', LPAD(i, 7, '0')), 'ACTIVE',
             '00000000-0000-0000-0000-000000000005', @account_created_at);
        INSERT INTO referees
            (referee_id, user_id, certification_level, years_of_service,
             status, created_at)
        VALUES
            (v_profile_id, v_user_id,
             IF(i IN (1, 3, 6), 'International A', 'National A'),
             5 + i, 'AVAILABLE', @account_created_at);
        SET i = i + 1;
    END WHILE;

    -- 2 veterinarian.
    SET i = 1;
    WHILE i <= 2 DO
        SET v_user_id = UUID();
        SET v_profile_id = UUID();
        INSERT INTO users
            (user_id, username, password, email, dob, gender, full_name,
             phone_number, status, role_id, created_at)
        VALUES
            (v_user_id, CONCAT('vet', i), @demo_password,
             CONCAT('vet', i, '@hrtms.test'), '1987-04-20',
             IF(i = 1, 'FEMALE', 'MALE'), CONCAT('Bác sĩ thú y ', i),
             CONCAT('093', LPAD(i, 7, '0')), 'ACTIVE',
             '00000000-0000-0000-0000-000000000006', @account_created_at);
        INSERT INTO veterinarians
            (vet_id, user_id, specialization,
             years_of_service, status, created_at)
        VALUES
            (v_profile_id, v_user_id,
             'Equine Medicine', 5 + i, 'AVAILABLE', @account_created_at);
        SET i = i + 1;
    END WHILE;

    -- 2 medical staff.
    SET i = 1;
    WHILE i <= 2 DO
        SET v_user_id = UUID();
        SET v_profile_id = UUID();
        INSERT INTO users
            (user_id, username, password, email, dob, gender, full_name,
             phone_number, status, role_id, created_at)
        VALUES
            (v_user_id, CONCAT('medical', i), @demo_password,
             CONCAT('medical', i, '@hrtms.test'), '1990-09-18',
             IF(i = 1, 'MALE', 'FEMALE'), CONCAT('Nhân viên y tế ', i),
             CONCAT('094', LPAD(i, 7, '0')), 'ACTIVE',
             '00000000-0000-0000-0000-000000000007', @account_created_at);
        INSERT INTO medical_staffs
            (med_staff_id, user_id, certification, years_of_service,
             status, created_at)
        VALUES
            (v_profile_id, v_user_id, CONCAT('MED-', LPAD(i, 4, '0')),
             4 + i, 'AVAILABLE', @account_created_at);
        SET i = i + 1;
    END WHILE;
END$$
DELIMITER ;

CALL SeedStaffAccounts();
DROP PROCEDURE SeedStaffAccounts;

INSERT INTO appeal_categories
    (category_id, code, name, description, is_active, created_at)
VALUES
('ac000000-0000-0000-0000-000000000001', 'RESULT_ERROR',
 'Sai kết quả', 'Khiếu nại thứ hạng hoặc thời gian về đích.', 1, NOW()),
('ac000000-0000-0000-0000-000000000002', 'RACE_INCIDENT',
 'Sự cố đường đua', 'Khiếu nại va chạm, cản trở hoặc sự cố trong race.', 1, NOW()),
('ac000000-0000-0000-0000-000000000003', 'VIOLATION',
 'Vi phạm', 'Khiếu nại quyết định xử lý vi phạm.', 1, NOW());

-- --------------------------------------------------------------------------
-- 3. HAI TOURNAMENT VÀ CƠ CẤU GIẢI THƯỞNG
-- --------------------------------------------------------------------------

DROP PROCEDURE IF EXISTS SeedTournament;
DELIMITER $$
CREATE PROCEDURE SeedTournament(
    IN p_id CHAR(36), IN p_name VARCHAR(150), IN p_description VARCHAR(500),
    IN p_max_approved_entries INT, IN p_current_round VARCHAR(100),
    IN p_round_count INT, IN p_race_count INT, IN p_end_date DATE
)
BEGIN
    INSERT INTO tournaments
        (tournament_id, name, description, start_date, end_date, finished_at,
         location, registration_fee, system_contract_fee, total_prize_pool,
         allowed_breed, min_horse_age, max_horse_age,
         prediction_top1_correct_points, prediction_top3_exact_position_points,
         prediction_top3_correct_horse_points,
         prediction_top3_perfect_bonus_points,
         prediction_open_minutes_before, prediction_close_minutes_before,
         prediction_card_open_hours_before_first_race,
         inspection_open_minutes_before, inspection_close_minutes_before,
         min_race_interval_minutes,
         start_early_tolerance_minutes, start_late_tolerance_minutes,
         default_race_operational_minutes, race_day_start_time,
          race_day_end_time, apply_break_time, break_start_time, break_end_time,
          status, phase, created_at, published_at,
         registration_open_at, registration_close_at, review_deadline_at,
         jockey_matching_deadline_at, scheduling_deadline_at,
          competition_start_at, current_round_name,
         race_class, distance, top_weight_lbs, min_weight_lbs,
         equipment_weight_kg, handicap_enabled,
         max_approved_horses, max_approved_jockeys, max_approved_entries,
         planned_round_count, planned_race_count,
         bracket_plan_status, bracket_plan_version, created_by)
    VALUES
        (p_id, p_name, p_description,
         DATE_SUB(CURDATE(), INTERVAL 35 DAY), p_end_date, NULL,
         'HRTMS Demo Racing Center', 500000.00, 100000.00, 20000000.00,
         'THOROUGHBRED', 3, 8,
         100, 30, 10, 50,
         180, 5, 24,
         185, 6,
         35,
         185, 0,
         180, '00:00:00', '23:59:59',
          0, NULL, NULL,
          'ONGOING', 'RACING',
         DATE_SUB(NOW(), INTERVAL 36 DAY),
         DATE_SUB(NOW(), INTERVAL 35 DAY),
         DATE_SUB(NOW(), INTERVAL 35 DAY),
         DATE_SUB(NOW(), INTERVAL 30 DAY),
         DATE_SUB(NOW(), INTERVAL 26 DAY),
         DATE_SUB(NOW(), INTERVAL 20 DAY),
         DATE_SUB(NOW(), INTERVAL 16 DAY),
         DATE_SUB(NOW(), INTERVAL 2 DAY),
          p_current_round, 'CLASS_4', 'MILE_1600M',
         0, 0, 0.0, 0,
         p_max_approved_entries, 999999, p_max_approved_entries,
         p_round_count, p_race_count, 'LOCKED', 1, @admin_user_id);
END$$
DELIMITER ;

CALL SeedTournament(
    @full_tournament_id,
    'DEMO FULL 8 - Luồng 07 đến 11',
    'Một Final Race có 8 cặp để demo khám, prediction, vận hành, vi phạm, appeal, report, rating và payout.',
    8, 'Chung kết', 1, 1, DATE_ADD(CURDATE(), INTERVAL 2 DAY)
);

CALL SeedTournament(
    @bracket_tournament_id,
    'DEMO BRACKET 16 - Chuyển Top 4',
    'Có đúng 16 cặp APPROVED. Sức chứa cấu hình 32 để BE tạo cấu trúc 2 Race vòng 1 rồi Final 8.',
    32, 'Vòng 1', 2, 3, DATE_ADD(CURDATE(), INTERVAL 10 DAY)
);

DROP PROCEDURE SeedTournament;

INSERT INTO prize_structures
    (prize_structure_id, `prize_rank`, percentage, fixed_amount,
     is_active, tournament_id)
SELECT UUID(), 1, 50.0, 0.00, 1, tournament_id FROM tournaments;
INSERT INTO prize_structures
    (prize_structure_id, `prize_rank`, percentage, fixed_amount,
     is_active, tournament_id)
SELECT UUID(), 2, 30.0, 0.00, 1, tournament_id FROM tournaments;
INSERT INTO prize_structures
    (prize_structure_id, `prize_rank`, percentage, fixed_amount,
     is_active, tournament_id)
SELECT UUID(), 3, 20.0, 0.00, 1, tournament_id FROM tournaments;

-- --------------------------------------------------------------------------
-- 4. 24 NGỰA, REGISTRATION, CONTRACT VÀ INVOICE ĐÃ THANH TOÁN
-- --------------------------------------------------------------------------

DROP PROCEDURE IF EXISTS SeedApprovedCompetitors;
DELIMITER $$
CREATE PROCEDURE SeedApprovedCompetitors(
    IN p_tournament_id CHAR(36), IN p_owner_username VARCHAR(15),
    IN p_horse_prefix VARCHAR(40), IN p_jockey_offset INT, IN p_quantity INT
)
BEGIN
    DECLARE i INT DEFAULT 1;
    DECLARE v_horse_id CHAR(36);
    DECLARE v_owner_id CHAR(36);
    DECLARE v_owner_user_id CHAR(36);
    DECLARE v_jockey_id CHAR(36);
    DECLARE v_horse_reg_id CHAR(36);
    DECLARE v_jockey_reg_id CHAR(36);
    DECLARE v_contract_id CHAR(36);
    DECLARE v_rating INT;

    SELECT ho.owner_id, u.user_id
    INTO v_owner_id, v_owner_user_id
    FROM horse_owners ho
    JOIN users u ON u.user_id = ho.user_id
    WHERE u.username = p_owner_username;

    WHILE i <= p_quantity DO
        SET v_horse_id = UUID();
        SET v_horse_reg_id = UUID();
        SET v_jockey_reg_id = UUID();
        SET v_contract_id = UUID();
        -- CLASS_4 theo policy hiện tại chỉ nhận rating 40..59.
        SET v_rating = 48 + MOD(i * 3 + p_jockey_offset, 12);

        SELECT j.jockey_id INTO v_jockey_id
        FROM jockeys j
        JOIN users u ON u.user_id = j.user_id
        WHERE u.username = CONCAT('jockey', p_jockey_offset + i - 1);

        INSERT INTO horses
            (horse_id, name, breed, gender, age, weight, color, image_url,
             health_status, current_rating, race_class, highest_rating,
             rating_updated_at, total_races, total_wins, total_places,
             win_rate, last_race_at, created_at, owner_id)
        VALUES
            (v_horse_id, CONCAT(p_horse_prefix, LPAD(i, 2, '0')),
             'THOROUGHBRED', IF(MOD(i, 2) = 0, 'FEMALE', 'MALE'),
             4 + MOD(i, 3), 430 + MOD(i * 7, 35),
             CASE MOD(i, 4)
                 WHEN 0 THEN 'Bay'
                 WHEN 1 THEN 'Black'
                 WHEN 2 THEN 'Chestnut'
                 ELSE 'Gray'
             END,
             NULL, 'HEALTHY', v_rating, 'CLASS_4', v_rating + 3,
             NOW(), 5 + i, MOD(i, 4), MOD(i, 6), 0.0, NULL,
             @account_created_at, v_owner_id);

        INSERT INTO horse_tournament_registrations
            (horse_tournament_reg_id, tournament_id, horse_id, owner_id,
             status, submitted_at, reviewed_by, reviewed_at,
             rating_at_registration, race_class_at_registration, note)
        VALUES
            (v_horse_reg_id, p_tournament_id, v_horse_id, v_owner_id,
             'APPROVED', DATE_SUB(NOW(), INTERVAL 33 DAY), @admin_user_id,
             DATE_SUB(NOW(), INTERVAL 28 DAY), v_rating, 'CLASS_4',
             'Dữ liệu demo đã duyệt');

        INSERT INTO jockey_tournament_registrations
            (jockey_tournament_reg_id, tournament_id, jockey_id, status,
             submitted_at, reviewed_by, reviewed_at, hire_fee, note)
        VALUES
            (v_jockey_reg_id, p_tournament_id, v_jockey_id, 'APPROVED',
             DATE_SUB(NOW(), INTERVAL 33 DAY), NULL, NULL, 2000000.00,
             'Đăng ký kỵ sĩ tự động APPROVED');

        INSERT INTO jockey_horse_contracts
            (contract_id, tournament_id, horse_tournament_reg_id,
             jockey_tournament_reg_id, owner_id, horse_id, jockey_id,
             hire_fee, advance_percent, final_percent,
             advance_paid_amount, escrow_amount, system_contract_fee,
             owner_prize_share_percent, jockey_prize_share_percent,
             payment_status, escrow_status, advance_payout_status,
             final_payout_status, status, advance_payout_at,
             requested_at, responded_at, accepted_at, contract_note)
        VALUES
            (v_contract_id, p_tournament_id, v_horse_reg_id, v_jockey_reg_id,
             v_owner_id, v_horse_id, v_jockey_id,
             2000000.00, 30.0, 70.0,
             600000.00, 1400000.00, 100000.00,
             80.0, 20.0,
             'PAID', 'PARTIALLY_RELEASED', 'PAID',
             'NOT_RELEASED', 'APPROVED', DATE_SUB(NOW(), INTERVAL 23 DAY),
             DATE_SUB(NOW(), INTERVAL 25 DAY), DATE_SUB(NOW(), INTERVAL 24 DAY),
             DATE_SUB(NOW(), INTERVAL 24 DAY),
             'Hợp đồng hợp lệ để test prize và final payout');

        INSERT INTO invoices
            (invoice_id, payer_user_id, contract_id, invoice_type, amount,
             status, due_date, paid_at, created_at, note)
        VALUES
            (UUID(), v_owner_user_id, v_contract_id, 'JOCKEY_HIRING_FEE',
             2000000.00, 'PAID', DATE_SUB(NOW(), INTERVAL 23 DAY),
             DATE_SUB(NOW(), INTERVAL 24 DAY), DATE_SUB(NOW(), INTERVAL 25 DAY),
             'Đã thanh toán phí thuê kỵ sĩ'),
            (UUID(), v_owner_user_id, v_contract_id, 'CONTRACT_CREATION_FEE',
             100000.00, 'PAID', DATE_SUB(NOW(), INTERVAL 22 DAY),
             DATE_SUB(NOW(), INTERVAL 23 DAY), DATE_SUB(NOW(), INTERVAL 24 DAY),
             'Đã thanh toán phí lập hợp đồng');

        SET i = i + 1;
    END WHILE;
END$$
DELIMITER ;

CALL SeedApprovedCompetitors(
    @full_tournament_id, 'owner1', 'FullHorse', 1, 8
);
CALL SeedApprovedCompetitors(
    @bracket_tournament_id, 'owner2', 'BracketHorse', 9, 16
);

DROP PROCEDURE SeedApprovedCompetitors;

-- --------------------------------------------------------------------------
-- 5. ROUND, RACE, ENTRY VÀ PHÂN CÔNG NHÂN SỰ
-- --------------------------------------------------------------------------

-- FULL 8:
--   Race start S+185 phút.
--   Prediction open S-1 phút, close S+180 phút (đúng T-5).
--   Inspection open T-185 = S, close T-6 = S+179 phút.
--   Start earliest T-185 = S, latest T = S+185 phút.
--   Race operational duration = 180 phút.
SET @full_race_start = DATE_ADD(@seed_now, INTERVAL 185 MINUTE);
SET @full_race_end = DATE_ADD(@full_race_start, INTERVAL 180 MINUTE);
SET @full_prediction_open = DATE_SUB(@seed_now, INTERVAL 1 MINUTE);
SET @full_prediction_close = DATE_ADD(@seed_now, INTERVAL 180 MINUTE);

-- BRACKET 16: hai race vòng 1 đã kết thúc hôm qua; Final cách 7 ngày lịch.
SET @bracket_race1_start = TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 1 DAY), '09:00:00');
SET @bracket_race1_end = DATE_ADD(@bracket_race1_start, INTERVAL 180 MINUTE);
SET @bracket_race2_start = DATE_ADD(@bracket_race1_end, INTERVAL 35 MINUTE);
SET @bracket_race2_end = DATE_ADD(@bracket_race2_start, INTERVAL 180 MINUTE);
SET @bracket_final_start = TIMESTAMP(
    DATE_ADD(DATE(@bracket_race2_end), INTERVAL 7 DAY), '09:00:00'
);
SET @bracket_final_end = DATE_ADD(@bracket_final_start, INTERVAL 180 MINUTE);

DROP PROCEDURE IF EXISTS SeedRound;
DELIMITER $$
CREATE PROCEDURE SeedRound(
    IN p_round_id CHAR(36), IN p_tournament_id CHAR(36),
    IN p_name VARCHAR(100), IN p_sequence INT, IN p_is_final BOOLEAN,
    IN p_status VARCHAR(30), IN p_start DATETIME, IN p_end DATETIME,
    IN p_race_count INT, IN p_expected_entries INT,
    IN p_head_referee_username VARCHAR(15), IN p_transition VARCHAR(50)
)
BEGIN
    DECLARE v_head_referee_id CHAR(36);
    SELECT r.referee_id INTO v_head_referee_id
    FROM referees r
    JOIN users u ON u.user_id = r.user_id
    WHERE u.username = p_head_referee_username;

    INSERT INTO rounds
        (round_id, round_name, sequence_order, is_final, prediction_type,
         advancement_rule, start_date, end_date, description,
         max_races, max_entries, min_entries, status,
         head_referee_id, head_referee_assigned_at,
         expected_entries, planned_race_count, qualifiers_per_race,
         bracket_plan_version, advanced_at, transition_status,
         created_at, tournament_id, created_by)
    VALUES
        (p_round_id, p_name, p_sequence, p_is_final, 'TOP3',
         IF(p_is_final,
            'Xác định Top 3 chung cuộc nhận giải',
            'Top 4 FINISHED của mỗi Race đi tiếp'),
         p_start, p_end, CONCAT('Round demo: ', p_name),
         p_race_count, 16, 8, p_status,
         v_head_referee_id, DATE_SUB(NOW(), INTERVAL 3 DAY),
         p_expected_entries, p_race_count, IF(p_is_final, 0, 4),
         1, NULL, p_transition,
         DATE_SUB(NOW(), INTERVAL 10 DAY), p_tournament_id, @admin_user_id);
END$$
DELIMITER ;

CALL SeedRound(
    @full_round_id, @full_tournament_id, 'Chung kết', 1, 1,
    'SCHEDULED', @full_race_start, @full_race_end,
    1, 8, 'referee1', 'NOT_READY'
);
CALL SeedRound(
    @bracket_round1_id, @bracket_tournament_id, 'Vòng 1', 1, 0,
    'ONGOING', @bracket_race1_start, @bracket_race2_end,
    2, 16, 'referee3', 'NOT_READY'
);
CALL SeedRound(
    @bracket_final_round_id, @bracket_tournament_id, 'Vòng 2 (Chung kết)', 2, 1,
    'SCHEDULING', @bracket_final_start, @bracket_final_end,
    1, 8, 'referee6', 'NOT_READY'
);

DROP PROCEDURE IF EXISTS SeedRace;
DELIMITER $$
CREATE PROCEDURE SeedRace(
    IN p_race_id CHAR(36), IN p_round_id CHAR(36), IN p_name VARCHAR(150),
    IN p_sequence INT, IN p_status VARCHAR(30),
    IN p_start DATETIME, IN p_end DATETIME,
    IN p_schedule_published BOOLEAN,
    IN p_prediction_open DATETIME, IN p_prediction_close DATETIME,
    IN p_started_at DATETIME, IN p_finished_at DATETIME,
    IN p_inspection_finalized BOOLEAN
)
BEGIN
    INSERT INTO races
        (race_id, name, start_time, end_time, track_condition, distance,
         sequence_order, status, started_at, finished_at,
         schedule_published_at, prediction_open_at, prediction_close_at,
         ai_prediction_publication_status, ai_prediction_generated_at,
         ai_prediction_generated_by, ai_prediction_published_at,
         ai_prediction_published_by, round_id, created_by, started_by,
         inspection_finalized_at, cancelled_at, cancellation_reason,
         rescheduled_at, reschedule_reason)
    VALUES
        (p_race_id, p_name, p_start, p_end, 'TURF', 'MILE_1600M',
         p_sequence, p_status, p_started_at, p_finished_at,
         IF(p_schedule_published,
            DATE_SUB(p_prediction_open, INTERVAL 1 MINUTE), NULL),
         p_prediction_open, p_prediction_close,
         NULL, NULL, NULL, NULL, NULL,
         p_round_id, @admin_user_id, NULL,
         IF(p_inspection_finalized,
            DATE_SUB(p_start, INTERVAL 6 MINUTE), NULL),
         NULL, NULL, NULL, NULL);
END$$
DELIMITER ;

CALL SeedRace(
    @full_race_id, @full_round_id, 'DEMO FULL 8 - Final Race', 1,
    'SCHEDULED', @full_race_start, @full_race_end, 1,
    @full_prediction_open, @full_prediction_close,
    NULL, NULL, 0
);

CALL SeedRace(
    @bracket_race1_id, @bracket_round1_id, 'DEMO BRACKET 16 - Vòng 1 - Race 1', 1,
    'COMPLETED', @bracket_race1_start, @bracket_race1_end, 1,
    DATE_SUB(@bracket_race1_start, INTERVAL 24 HOUR),
    DATE_SUB(@bracket_race1_start, INTERVAL 5 MINUTE),
    @bracket_race1_start, DATE_ADD(@bracket_race1_start, INTERVAL 8 MINUTE), 1
);
CALL SeedRace(
    @bracket_race2_id, @bracket_round1_id, 'DEMO BRACKET 16 - Vòng 1 - Race 2', 2,
    'FINISHED', @bracket_race2_start, @bracket_race2_end, 1,
    DATE_SUB(@bracket_race1_start, INTERVAL 24 HOUR),
    DATE_SUB(@bracket_race2_start, INTERVAL 5 MINUTE),
    @bracket_race2_start, DATE_ADD(@bracket_race2_start, INTERVAL 8 MINUTE), 1
);
CALL SeedRace(
    @bracket_final_race_id, @bracket_final_round_id,
    'DEMO BRACKET 16 - Vòng 2 - Final Race', 1,
    'SCHEDULING', @bracket_final_start, @bracket_final_end, 0,
    NULL, NULL, NULL, NULL, 0
);

DROP PROCEDURE IF EXISTS SeedRaceEntries;
DELIMITER $$
CREATE PROCEDURE SeedRaceEntries(
    IN p_race_id CHAR(36), IN p_tournament_id CHAR(36),
    IN p_horse_prefix VARCHAR(40), IN p_first_number INT, IN p_quantity INT
)
BEGIN
    DECLARE i INT DEFAULT 0;
    DECLARE v_contract_id CHAR(36);
    DECLARE v_race_start DATETIME;

    SELECT start_time INTO v_race_start
    FROM races
    WHERE race_id = p_race_id;

    WHILE i < p_quantity DO
        SELECT c.contract_id INTO v_contract_id
        FROM jockey_horse_contracts c
        JOIN horses h ON h.horse_id = c.horse_id
        WHERE c.tournament_id = p_tournament_id
          AND h.name = CONCAT(p_horse_prefix, LPAD(p_first_number + i, 2, '0'))
          AND c.status = 'APPROVED';

        INSERT INTO race_entries
            (entry_id, race_id, contract_id, lane_number, status,
             assigned_by, assigned_at, created_at)
        VALUES
            (UUID(), p_race_id, v_contract_id, i + 1, 'CONFIRMED',
             @admin_user_id,
             DATE_SUB(v_race_start, INTERVAL 2 DAY),
             DATE_SUB(v_race_start, INTERVAL 2 DAY));
        SET i = i + 1;
    END WHILE;
END$$
DELIMITER ;

CALL SeedRaceEntries(
    @full_race_id, @full_tournament_id, 'FullHorse', 1, 8
);
CALL SeedRaceEntries(
    @bracket_race1_id, @bracket_tournament_id, 'BracketHorse', 1, 8
);
CALL SeedRaceEntries(
    @bracket_race2_id, @bracket_tournament_id, 'BracketHorse', 9, 8
);

DROP PROCEDURE IF EXISTS SeedRaceAssignment;
DELIMITER $$
CREATE PROCEDURE SeedRaceAssignment(
    IN p_race_id CHAR(36), IN p_referee_username VARCHAR(15),
    IN p_vet_username VARCHAR(15), IN p_medical_username VARCHAR(15),
    IN p_mark_staff_assigned BOOLEAN
)
BEGIN
    DECLARE v_referee_id CHAR(36);
    DECLARE v_referee_user_id CHAR(36);
    DECLARE v_vet_id CHAR(36);
    DECLARE v_medical_id CHAR(36);
    DECLARE v_race_start DATETIME;

    SELECT start_time INTO v_race_start
    FROM races
    WHERE race_id = p_race_id;

    SELECT r.referee_id, u.user_id
    INTO v_referee_id, v_referee_user_id
    FROM referees r
    JOIN users u ON u.user_id = r.user_id
    WHERE u.username = p_referee_username;

    SELECT v.vet_id INTO v_vet_id
    FROM veterinarians v
    JOIN users u ON u.user_id = v.user_id
    WHERE u.username = p_vet_username;

    SELECT m.med_staff_id INTO v_medical_id
    FROM medical_staffs m
    JOIN users u ON u.user_id = m.user_id
    WHERE u.username = p_medical_username;

    INSERT INTO race_referees
        (race_referee_id, race_id, referee_id, assigned_by, assigned_at)
    VALUES
        (UUID(), p_race_id, v_referee_id, @admin_user_id,
         DATE_SUB(v_race_start, INTERVAL 2 DAY));

    INSERT INTO race_inspection_staff_assignments
        (assignment_id, race_id, vet_id, med_staff_id, assigned_by, assigned_at)
    VALUES
        (UUID(), p_race_id, v_vet_id, v_medical_id, @admin_user_id,
         DATE_SUB(v_race_start, INTERVAL 2 DAY));

    UPDATE races
    SET started_by = v_referee_user_id
    WHERE race_id = p_race_id AND started_at IS NOT NULL;

    IF p_mark_staff_assigned THEN
        UPDATE veterinarians SET status = 'ASSIGNED' WHERE vet_id = v_vet_id;
        UPDATE medical_staffs SET status = 'ASSIGNED' WHERE med_staff_id = v_medical_id;
    END IF;
END$$
DELIMITER ;

-- Head Referee FULL 8 là referee1, Race Referee là referee2.
CALL SeedRaceAssignment(@full_race_id, 'referee2', 'vet1', 'medical1', 1);
-- Head Referee vòng 1 bracket là referee3; mỗi race có đúng một Race Referee.
CALL SeedRaceAssignment(@bracket_race1_id, 'referee4', 'vet2', 'medical2', 0);
CALL SeedRaceAssignment(@bracket_race2_id, 'referee5', 'vet2', 'medical2', 0);

DROP PROCEDURE IF EXISTS SeedConfirmedInspections;
DELIMITER $$
CREATE PROCEDURE SeedConfirmedInspections(IN p_race_id CHAR(36))
BEGIN
    DECLARE done INT DEFAULT 0;
    DECLARE v_entry_id CHAR(36);
    DECLARE v_vet_id CHAR(36);
    DECLARE v_medical_id CHAR(36);
    DECLARE v_race_start DATETIME;
    DECLARE v_horse_weight FLOAT;
    DECLARE v_horse_breed VARCHAR(50);
    DECLARE v_jockey_weight FLOAT;
    DECLARE entry_cursor CURSOR FOR
        SELECT entry_id
        FROM race_entries
        WHERE race_id = p_race_id
        ORDER BY lane_number;
    DECLARE CONTINUE HANDLER FOR NOT FOUND SET done = 1;

    SELECT vet_id, med_staff_id
    INTO v_vet_id, v_medical_id
    FROM race_inspection_staff_assignments
    WHERE race_id = p_race_id;

    SELECT start_time INTO v_race_start
    FROM races WHERE race_id = p_race_id;

    OPEN entry_cursor;
    inspection_loop: LOOP
        FETCH entry_cursor INTO v_entry_id;
        IF done = 1 THEN
            LEAVE inspection_loop;
        END IF;

        SELECT h.weight, h.breed, j.weight
        INTO v_horse_weight, v_horse_breed, v_jockey_weight
        FROM race_entries re
        JOIN jockey_horse_contracts c ON c.contract_id = re.contract_id
        JOIN horses h ON h.horse_id = c.horse_id
        JOIN jockeys j ON j.jockey_id = c.jockey_id
        WHERE re.entry_id = v_entry_id;

        INSERT INTO horse_inspections
            (horse_inspection_id, entry_id, vet_id, result, note,
             inspected_at, handicap_weight, registered_weight,
             registered_breed, actual_weight, actual_breed,
             doping_detected, is_handicap_confirmed, confirmed_at, status)
        VALUES
            (UUID(), v_entry_id, v_vet_id, 'PASS', 'Lịch sử: đủ điều kiện',
             DATE_SUB(v_race_start, INTERVAL 60 MINUTE), 0.0,
             v_horse_weight, v_horse_breed, v_horse_weight, v_horse_breed,
             0, 1, DATE_SUB(v_race_start, INTERVAL 55 MINUTE), 'CONFIRMED');

        INSERT INTO jockey_inspections
            (jockey_inspection_id, entry_id, med_staff_id, result, note,
             inspected_at, registered_weight, actual_weight,
             doping_detected, status)
        VALUES
            (UUID(), v_entry_id, v_medical_id, 'PASS', 'Lịch sử: đủ điều kiện',
             DATE_SUB(v_race_start, INTERVAL 60 MINUTE),
             v_jockey_weight, v_jockey_weight, 0, 'CONFIRMED');
    END LOOP;
    CLOSE entry_cursor;
END$$
DELIMITER ;

-- FULL 8 cố ý chưa có inspection để Vet/Medical thao tác thật trên FE.
-- Hai race lịch sử của bracket đã PASS inspection để dữ liệu nhất quán.
CALL SeedConfirmedInspections(@bracket_race1_id);
CALL SeedConfirmedInspections(@bracket_race2_id);

DROP PROCEDURE SeedConfirmedInspections;
DROP PROCEDURE SeedRaceAssignment;
DROP PROCEDURE SeedRaceEntries;
DROP PROCEDURE SeedRace;
DROP PROCEDURE SeedRound;

-- --------------------------------------------------------------------------
-- 6. SNAPSHOT KẾT QUẢ BRACKET: RACE 1 PUBLISHED, RACE 2 SIGNED
-- --------------------------------------------------------------------------

DROP PROCEDURE IF EXISTS SeedBracketResultsAndReport;
DELIMITER $$
CREATE PROCEDURE SeedBracketResultsAndReport(
    IN p_race_id CHAR(36), IN p_report_status VARCHAR(30)
)
BEGIN
    DECLARE done INT DEFAULT 0;
    DECLARE v_entry_id CHAR(36);
    DECLARE v_lane INT;
    DECLARE v_recorded_by_user CHAR(36);
    DECLARE v_race_referee_id CHAR(36);
    DECLARE v_head_referee_id CHAR(36);
    DECLARE v_race_finished DATETIME;
    DECLARE v_race_end DATETIME;
    DECLARE entry_cursor CURSOR FOR
        SELECT entry_id, lane_number
        FROM race_entries
        WHERE race_id = p_race_id
        ORDER BY lane_number;
    DECLARE CONTINUE HANDLER FOR NOT FOUND SET done = 1;

    SELECT rr.referee_id, u.user_id
    INTO v_race_referee_id, v_recorded_by_user
    FROM race_referees rr
    JOIN referees r ON r.referee_id = rr.referee_id
    JOIN users u ON u.user_id = r.user_id
    WHERE rr.race_id = p_race_id
    LIMIT 1;

    SELECT ro.head_referee_id, ra.finished_at, ra.end_time
    INTO v_head_referee_id, v_race_finished, v_race_end
    FROM races ra
    JOIN rounds ro ON ro.round_id = ra.round_id
    WHERE ra.race_id = p_race_id;

    OPEN entry_cursor;
    result_loop: LOOP
        FETCH entry_cursor INTO v_entry_id, v_lane;
        IF done = 1 THEN
            LEAVE result_loop;
        END IF;

        UPDATE race_entries
        SET status = 'FINISHED'
        WHERE entry_id = v_entry_id;

        INSERT INTO race_results
            (result_id, race_id, entry_id, finish_time, finish_position,
             prize_money, owner_prize_amount, jockey_prize_amount,
             prize_status, is_prize_paid, prize_paid_at, status, rating_change,
             recorded_by, recorded_at, updated_at)
        VALUES
            (UUID(), p_race_id, v_entry_id, 95.0 + v_lane * 0.8, v_lane,
             0.00, 0.00, 0.00, 'NotEligible', 0, NULL, 'FINISHED',
             CASE WHEN v_lane = 1 THEN 6 WHEN v_lane = 2 THEN 2
                  WHEN v_lane = 3 THEN 1 ELSE 0 END,
             v_recorded_by_user, v_race_finished, v_race_finished);
    END LOOP;
    CLOSE entry_cursor;

    INSERT INTO race_reports
        (report_id, race_id, referee_id, summary, appeal_note, status,
         submitted_at, submitted_by, returned_at, returned_by, return_reason,
         signed_by, signed_at, published_by, published_at, created_at)
    VALUES
        (UUID(), p_race_id, v_race_referee_id,
         CONCAT('Biên bản bracket hợp lệ của race ', p_race_id,
                '. Tám entry đều FINISHED; Top 4 sẽ đi tiếp.'),
         NULL, p_report_status,
         v_race_finished, v_race_referee_id,
         NULL, NULL, NULL,
         v_head_referee_id, DATE_ADD(v_race_finished, INTERVAL 2 MINUTE),
         IF(p_report_status = 'PUBLISHED', @admin_user_id, NULL),
         IF(p_report_status = 'PUBLISHED',
            DATE_ADD(v_race_end, INTERVAL 1 MINUTE), NULL),
         v_race_finished);
END$$
DELIMITER ;

CALL SeedBracketResultsAndReport(@bracket_race1_id, 'PUBLISHED');
CALL SeedBracketResultsAndReport(@bracket_race2_id, 'SIGNED');

DROP PROCEDURE SeedBracketResultsAndReport;

-- --------------------------------------------------------------------------
-- 7. KIỂM TRA CỨNG SAU SEED
-- --------------------------------------------------------------------------

DROP PROCEDURE IF EXISTS ValidateFreshDemoData;
DELIMITER $$
CREATE PROCEDURE ValidateFreshDemoData()
BEGIN
    DECLARE v_count INT;

    SELECT COUNT(*) INTO v_count FROM tournaments;
    IF v_count <> 2 THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Seed invalid: phải có đúng 2 tournament.';
    END IF;

    SELECT COUNT(*) INTO v_count
    FROM jockey_horse_contracts
    WHERE tournament_id = @full_tournament_id AND status = 'APPROVED';
    IF v_count <> 8 THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Seed invalid: DEMO FULL phải có 8 contract APPROVED.';
    END IF;

    SELECT COUNT(*) INTO v_count
    FROM jockey_horse_contracts
    WHERE tournament_id = @bracket_tournament_id AND status = 'APPROVED';
    IF v_count <> 16 THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Seed invalid: DEMO BRACKET phải có 16 contract APPROVED.';
    END IF;

    SELECT COUNT(*) INTO v_count
    FROM tournaments
    WHERE created_at > published_at
       OR start_date > DATE(registration_open_at)
       OR registration_open_at > registration_close_at
       OR registration_close_at > review_deadline_at
       OR review_deadline_at > jockey_matching_deadline_at
       OR jockey_matching_deadline_at > scheduling_deadline_at
       OR scheduling_deadline_at > competition_start_at;
    IF v_count <> 0 THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Seed invalid: timeline Tournament không đúng thứ tự.';
    END IF;

    SELECT COUNT(*) INTO v_count
    FROM tournaments
    WHERE handicap_enabled = 0
      AND (top_weight_lbs <> 0 OR min_weight_lbs <> 0
           OR equipment_weight_kg <> 0);
    IF v_count <> 0 THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Seed invalid: handicap tắt nhưng cấu hình cân không bằng 0.';
    END IF;

    SELECT COUNT(*) INTO v_count
    FROM horses
    WHERE race_class = 'CLASS_4'
      AND (current_rating < 40 OR current_rating > 59);
    IF v_count <> 0 THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Seed invalid: Horse CLASS_4 có rating ngoài 40..59.';
    END IF;

    SELECT COUNT(*) INTO v_count FROM race_entries WHERE race_id = @full_race_id;
    IF v_count <> 8 THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Seed invalid: Final Race FULL phải có 8 entry.';
    END IF;

    SELECT COUNT(*) INTO v_count FROM race_entries WHERE race_id = @bracket_race1_id;
    IF v_count <> 8 THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Seed invalid: Bracket Race 1 phải có 8 entry.';
    END IF;

    SELECT COUNT(*) INTO v_count FROM race_entries WHERE race_id = @bracket_race2_id;
    IF v_count <> 8 THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Seed invalid: Bracket Race 2 phải có 8 entry.';
    END IF;

    SELECT COUNT(*) INTO v_count FROM race_entries WHERE race_id = @bracket_final_race_id;
    IF v_count <> 0 THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Seed invalid: Final bracket phải rỗng trước khi chuyển Top 4.';
    END IF;

    SELECT COUNT(*) INTO v_count
    FROM (
        SELECT race_id, lane_number
        FROM race_entries
        GROUP BY race_id, lane_number
        HAVING COUNT(*) > 1
    ) duplicated_lanes;
    IF v_count <> 0 THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Seed invalid: có lane bị trùng trong cùng Race.';
    END IF;

    SELECT COUNT(*) INTO v_count
    FROM races ra
    JOIN rounds ro ON ro.round_id = ra.round_id
    JOIN tournaments t ON t.tournament_id = ro.tournament_id
    WHERE TIMESTAMPDIFF(MINUTE, ra.start_time, ra.end_time)
          <> t.default_race_operational_minutes;
    IF v_count <> 0 THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Seed invalid: thời lượng Race lệch default operational.';
    END IF;

    SELECT COUNT(*) INTO v_count
    FROM race_referees rr
    JOIN races ra ON ra.race_id = rr.race_id
    WHERE ra.schedule_published_at IS NOT NULL
      AND rr.assigned_at > ra.schedule_published_at;
    IF v_count <> 0 THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Seed invalid: Race Referee được gán sau khi publish lịch.';
    END IF;

    SELECT COUNT(*) INTO v_count
    FROM race_inspection_staff_assignments a
    JOIN races ra ON ra.race_id = a.race_id
    WHERE ra.schedule_published_at IS NOT NULL
      AND a.assigned_at > ra.schedule_published_at;
    IF v_count <> 0 THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Seed invalid: nhân sự inspection được gán sau khi publish lịch.';
    END IF;

    SELECT COUNT(*) INTO v_count
    FROM race_referees rr
    JOIN races ra ON ra.race_id = rr.race_id
    JOIN rounds ro ON ro.round_id = ra.round_id
    WHERE ro.head_referee_id = rr.referee_id;
    IF v_count <> 0 THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Seed invalid: Head Referee bị trùng Race Referee.';
    END IF;

    SELECT COUNT(*) INTO v_count
    FROM races ra
    WHERE ra.race_id IN (@full_race_id, @bracket_race1_id, @bracket_race2_id)
      AND (SELECT COUNT(*) FROM race_referees rr WHERE rr.race_id = ra.race_id) <> 1;
    IF v_count <> 0 THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Seed invalid: Race đã công bố phải có đúng một Race Referee.';
    END IF;

    SELECT COUNT(*) INTO v_count
    FROM race_reports
    WHERE race_id = @bracket_race1_id AND status = 'PUBLISHED';
    IF v_count <> 1 THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Seed invalid: Bracket Race 1 phải PUBLISHED.';
    END IF;

    SELECT COUNT(*) INTO v_count
    FROM race_reports
    WHERE race_id = @bracket_race2_id AND status = 'SIGNED';
    IF v_count <> 1 THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Seed invalid: Bracket Race 2 phải SIGNED.';
    END IF;

    SELECT COUNT(*) INTO v_count
    FROM race_results
    WHERE race_id IN (@bracket_race1_id, @bracket_race2_id)
      AND status = 'FINISHED'
      AND finish_position BETWEEN 1 AND 4;
    IF v_count <> 8 THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Seed invalid: hai Race bracket phải có tổng 8 Top 4 hợp lệ.';
    END IF;

    IF DATE(@bracket_final_start)
       < DATE_ADD(DATE(@bracket_race2_end), INTERVAL 7 DAY) THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Seed invalid: Final bracket chưa cách Round 1 đủ 7 ngày.';
    END IF;

    SELECT COUNT(*) INTO v_count
    FROM horse_inspections hi
    JOIN race_entries re ON re.entry_id = hi.entry_id
    WHERE re.race_id = @full_race_id;
    IF v_count <> 0 THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Seed invalid: FULL 8 phải bắt đầu khi chưa khám ngựa.';
    END IF;

    SELECT COUNT(*) INTO v_count
    FROM jockey_inspections ji
    JOIN race_entries re ON re.entry_id = ji.entry_id
    WHERE re.race_id = @full_race_id;
    IF v_count <> 0 THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Seed invalid: FULL 8 phải bắt đầu khi chưa khám jockey.';
    END IF;

    SELECT COUNT(*) INTO v_count FROM prize_structures;
    IF v_count <> 6 THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Seed invalid: hai Tournament phải có tổng 6 prize structure.';
    END IF;

    SELECT COUNT(*) INTO v_count
    FROM wallets
    WHERE owner_type = 'SYSTEM'
      AND wallet_purpose IN ('SYSTEM_REVENUE', 'SYSTEM_ESCROW', 'SYSTEM_PRIZE_POOL');
    IF v_count <> 3 THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Seed invalid: thiếu ví hệ thống cho scoring/payout.';
    END IF;

    IF TIMESTAMPDIFF(MINUTE, @full_prediction_close, @full_race_start) <> 5 THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Seed invalid: prediction phải đóng đúng T-5.';
    END IF;

    IF TIMESTAMPDIFF(MINUTE, @seed_now, @full_prediction_close) < 179 THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Seed invalid: prediction window phải còn gần 3 giờ.';
    END IF;
END$$
DELIMITER ;

CALL ValidateFreshDemoData();
DROP PROCEDURE ValidateFreshDemoData;

-- --------------------------------------------------------------------------
-- 8. KẾT QUẢ TÓM TẮT SAU KHI CHẠY SCRIPT
-- --------------------------------------------------------------------------

SELECT
    t.name AS tournament,
    t.max_approved_entries AS configured_capacity,
    COUNT(DISTINCT c.contract_id) AS actual_approved_contracts,
    t.phase,
    t.current_round_name
FROM tournaments t
LEFT JOIN jockey_horse_contracts c
       ON c.tournament_id = t.tournament_id AND c.status = 'APPROVED'
GROUP BY t.tournament_id, t.name, t.max_approved_entries,
         t.phase, t.current_round_name
ORDER BY t.name;

SELECT
    t.name AS tournament,
    ro.round_name,
    ro.sequence_order AS round_order,
    ro.is_final,
    ro.transition_status,
    ra.name AS race,
    ra.status AS race_status,
    COUNT(re.entry_id) AS entry_count,
    rp.status AS report_status,
    ra.start_time,
    ra.end_time,
    ra.prediction_open_at,
    ra.prediction_close_at
FROM tournaments t
JOIN rounds ro ON ro.tournament_id = t.tournament_id
JOIN races ra ON ra.round_id = ro.round_id
LEFT JOIN race_entries re ON re.race_id = ra.race_id
LEFT JOIN race_reports rp ON rp.race_id = ra.race_id
GROUP BY t.name, ro.round_name, ro.sequence_order, ro.is_final,
         ro.transition_status, ra.race_id, ra.name, ra.status,
         rp.status, ra.start_time, ra.end_time,
         ra.prediction_open_at, ra.prediction_close_at
ORDER BY t.name, ro.sequence_order, ra.sequence_order;

SELECT
    TIMESTAMPDIFF(MINUTE, @seed_now, @full_prediction_close)
        AS prediction_minutes_remaining,
    TIMESTAMPDIFF(
        MINUTE,
        @seed_now,
        DATE_SUB(@full_race_start, INTERVAL 6 MINUTE)
    ) AS inspection_minutes_remaining,
    TIMESTAMPDIFF(MINUTE, @seed_now, @full_race_start)
        AS start_window_minutes_remaining,
    TIMESTAMPDIFF(MINUTE, @full_race_start, @full_race_end)
        AS race_operational_minutes;

SELECT role_name, username, full_name
FROM users u
JOIN roles r ON r.role_id = u.role_id
WHERE username IN (
    'admin1', 'owner1', 'owner2', 'spectator1', 'spectator2',
    'jockey1', 'jockey9', 'referee1', 'referee2', 'referee3',
    'referee4', 'referee5', 'referee6', 'vet1', 'medical1'
)
ORDER BY role_name, username;

SET FOREIGN_KEY_CHECKS = @OLD_FOREIGN_KEY_CHECKS;
SET SQL_SAFE_UPDATES = @OLD_SQL_SAFE_UPDATES;

-- ============================================================================
-- HẾT FILE
-- Mật khẩu chung của tất cả tài khoản: 12345678
-- ============================================================================

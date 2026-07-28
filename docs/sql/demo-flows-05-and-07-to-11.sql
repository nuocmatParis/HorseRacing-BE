-- ============================================================================
-- HRTMS - DEMO LUỒNG 05 VÀ LUỒNG 07 -> 11 (MYSQL 8+)
-- ============================================================================
-- Chỉ dùng cho database LOCAL/TEST.
--
-- Tạo 2 giải độc lập:
--
--   1) DEMO FLOW 05 - JOCKEY MATCHING
--      - Phase JOCKEY_MATCHING.
--      - 4 ngựa thuộc 4 Owner khác nhau.
--      - 4 Jockey khác nhau, registration APPROVED.
--      - Chưa có contract để demo Owner gửi lời mời -> Jockey accept/reject
--        -> Owner thanh toán phí thuê và phí lập hợp đồng.
--
--   2) DEMO FLOW 07-11 - FULL FINAL RACE
--      - 8 ngựa thuộc 5 Owner khác nhau.
--      - 8 Jockey và 8 contract APPROVED.
--      - 1 Final Round, 1 Final Race, 8 entry, đã phân lane/staff/referee.
--      - Chưa có inspection, prediction, violation, appeal, result, report.
--      - Dùng UI/API để chạy tuần tự Inspection -> Prediction -> Start ->
--        Violation -> Finish -> Appeal -> Report -> Publish -> Payout.
--
-- Mật khẩu chung: 12345678
--
-- QUAN TRỌNG VỀ THỜI GIAN:
--   - Khi chạy seed, Final Race có start_time = NOW() + 5 giờ để Spectator
--     luôn nhìn thấy race trong API upcoming và có thể dự đoán.
--   - Inspection được nới riêng cho giải demo từ NOW()-5h đến NOW()+10h.
--   - Sau khi đã dự đoán + khám xong, chạy file:
--         docs/sql/demo-full-flow-unlock-start.sql
--     để chuyển start_time về NOW()-1 phút và cho Referee Start ngay.
--   - Sau khi unlock, cửa sổ Start và Appeal còn hiệu lực 5 giờ.
--
-- Có thể chạy lại script này để reset đúng 2 giải demo về trạng thái ban đầu.
-- Script không xóa dữ liệu giải đấu khác.
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

SET @matching_tournament_id = 'e5000000-0000-0000-0000-000000000001';
SET @matching_round_id      = 'e5010000-0000-0000-0000-000000000001';
SET @matching_race_id       = 'e5020000-0000-0000-0000-000000000001';
SET @full_tournament_id     = 'e7000000-0000-0000-0000-000000000001';
SET @full_round_id          = 'e7010000-0000-0000-0000-000000000001';
SET @full_race_id           = 'e7020000-0000-0000-0000-000000000001';

SET @admin_user_id = 'e0000000-0000-0000-0000-000000000001';

SET @full_race_start = DATE_ADD(@seed_now, INTERVAL 5 HOUR);
SET @full_race_end = DATE_ADD(@full_race_start, INTERVAL 5 HOUR);
SET @prediction_open = DATE_SUB(@seed_now, INTERVAL 5 HOUR);
SET @prediction_close = DATE_SUB(@full_race_start, INTERVAL 5 MINUTE);

-- ============================================================================
-- 1. CLEANUP ĐÚNG PHẠM VI HAI GIẢI DEMO
-- ============================================================================

DELETE FROM notification_deliveries
WHERE notification_id IN (
    SELECT notification_id
    FROM notifications
    WHERE related_id IN (
        @matching_tournament_id,
        @matching_round_id,
        @matching_race_id,
        @full_tournament_id,
        @full_round_id,
        @full_race_id
    )
);

DELETE FROM notifications
WHERE related_id IN (
    @matching_tournament_id,
    @matching_round_id,
    @matching_race_id,
    @full_tournament_id,
    @full_round_id,
    @full_race_id
);

DELETE FROM notification_events
WHERE aggregate_id IN (
    @matching_tournament_id,
    @matching_round_id,
    @matching_race_id,
    @full_tournament_id,
    @full_round_id,
    @full_race_id
);

DELETE FROM wallet_transactions
WHERE contract_id IN (
        SELECT contract_id
        FROM jockey_horse_contracts
        WHERE tournament_id IN (@matching_tournament_id, @full_tournament_id)
    )
   OR race_result_id IN (
        SELECT result_id
        FROM race_results
        WHERE race_id IN (@matching_race_id, @full_race_id)
    )
   OR invoice_id IN (
        SELECT invoice_id
        FROM invoices
        WHERE contract_id IN (
            SELECT contract_id
            FROM jockey_horse_contracts
            WHERE tournament_id IN (@matching_tournament_id, @full_tournament_id)
        )
    );

DELETE FROM horse_rating_histories
WHERE race_id IN (@matching_race_id, @full_race_id);

DELETE FROM prediction_detail
WHERE prediction_id IN (
    SELECT prediction_id
    FROM predictions
    WHERE race_id = @matching_race_id
);
DELETE FROM predictions WHERE race_id = @matching_race_id;
DELETE FROM appeal_evidences
WHERE appeal_id IN (
    SELECT appeal_id
    FROM appeals
    WHERE entry_id IN (
        SELECT entry_id
        FROM race_entries
        WHERE race_id = @matching_race_id
    )
);
DELETE FROM appeals
WHERE entry_id IN (
    SELECT entry_id
    FROM race_entries
    WHERE race_id = @matching_race_id
);
DELETE FROM violations
WHERE entry_id IN (
    SELECT entry_id
    FROM race_entries
    WHERE race_id = @matching_race_id
);
DELETE FROM race_reports WHERE race_id = @matching_race_id;
DELETE FROM race_results WHERE race_id = @matching_race_id;
DELETE FROM horse_inspections
WHERE entry_id IN (
    SELECT entry_id
    FROM race_entries
    WHERE race_id = @matching_race_id
);
DELETE FROM jockey_inspections
WHERE entry_id IN (
    SELECT entry_id
    FROM race_entries
    WHERE race_id = @matching_race_id
);
DELETE FROM race_referees WHERE race_id = @matching_race_id;
DELETE FROM race_inspection_staff_assignments WHERE race_id = @matching_race_id;
DELETE FROM race_entries WHERE race_id = @matching_race_id;
DELETE FROM races WHERE round_id = @matching_round_id;
DELETE FROM rounds WHERE tournament_id = @matching_tournament_id;

DELETE FROM prediction_detail
WHERE prediction_id IN (SELECT prediction_id FROM predictions WHERE race_id = @full_race_id);
DELETE FROM predictions WHERE race_id = @full_race_id;
DELETE FROM appeal_evidences
WHERE appeal_id IN (
    SELECT appeal_id
    FROM appeals
    WHERE entry_id IN (SELECT entry_id FROM race_entries WHERE race_id = @full_race_id)
);
DELETE FROM appeals
WHERE entry_id IN (SELECT entry_id FROM race_entries WHERE race_id = @full_race_id);
DELETE FROM violations
WHERE entry_id IN (SELECT entry_id FROM race_entries WHERE race_id = @full_race_id);
DELETE FROM race_reports WHERE race_id = @full_race_id;
DELETE FROM race_results WHERE race_id = @full_race_id;
DELETE FROM horse_inspections
WHERE entry_id IN (SELECT entry_id FROM race_entries WHERE race_id = @full_race_id);
DELETE FROM jockey_inspections
WHERE entry_id IN (SELECT entry_id FROM race_entries WHERE race_id = @full_race_id);
DELETE FROM race_referees WHERE race_id = @full_race_id;
DELETE FROM race_inspection_staff_assignments WHERE race_id = @full_race_id;
DELETE FROM race_entries WHERE race_id = @full_race_id;
DELETE FROM races WHERE round_id = @full_round_id;
DELETE FROM rounds WHERE tournament_id = @full_tournament_id;

DELETE FROM invoices
WHERE contract_id IN (
        SELECT contract_id
        FROM jockey_horse_contracts
        WHERE tournament_id IN (@matching_tournament_id, @full_tournament_id)
    )
   OR tournament_reg_id IN (
        SELECT horse_tournament_reg_id
        FROM horse_tournament_registrations
        WHERE tournament_id IN (@matching_tournament_id, @full_tournament_id)
    )
   OR jockey_tournament_reg_id IN (
        SELECT jockey_tournament_reg_id
        FROM jockey_tournament_registrations
        WHERE tournament_id IN (@matching_tournament_id, @full_tournament_id)
    );

DELETE FROM jockey_horse_contracts
WHERE tournament_id IN (@matching_tournament_id, @full_tournament_id);
DELETE FROM horse_tournament_registrations
WHERE tournament_id IN (@matching_tournament_id, @full_tournament_id);
DELETE FROM jockey_tournament_registrations
WHERE tournament_id IN (@matching_tournament_id, @full_tournament_id);
DELETE FROM tournament_eligibility
WHERE tournament_id IN (@matching_tournament_id, @full_tournament_id);
DELETE FROM tournament_phase_config
WHERE tournament_id IN (@matching_tournament_id, @full_tournament_id);
DELETE FROM prize_structures
WHERE tournament_id IN (@matching_tournament_id, @full_tournament_id);
DELETE FROM tournaments
WHERE tournament_id IN (@matching_tournament_id, @full_tournament_id);

DELETE FROM horses
WHERE horse_id LIKE 'e5300000-0000-0000-0000-%'
   OR horse_id LIKE 'e7310000-0000-0000-0000-%';

-- ============================================================================
-- 2. ROLE, USER VÀ PROFILE
-- ============================================================================

INSERT INTO roles (role_id, role_name, description, is_active, created_at)
VALUES
    ('e0000000-0000-0000-0001-000000000001', 'ADMIN', 'Quản trị hệ thống', 1, @created_at),
    ('e0000000-0000-0000-0001-000000000002', 'HORSE_OWNER', 'Chủ ngựa', 1, @created_at),
    ('e0000000-0000-0000-0001-000000000003', 'JOCKEY', 'Kỵ sĩ', 1, @created_at),
    ('e0000000-0000-0000-0001-000000000004', 'SPECTATOR', 'Khán giả', 1, @created_at),
    ('e0000000-0000-0000-0001-000000000005', 'REFEREE', 'Trọng tài', 1, @created_at),
    ('e0000000-0000-0000-0001-000000000006', 'VETERINARIAN', 'Bác sĩ thú y', 1, @created_at),
    ('e0000000-0000-0000-0001-000000000007', 'MEDICAL_STAFF', 'Nhân viên y tế', 1, @created_at)
ON DUPLICATE KEY UPDATE
    description = VALUES(description),
    is_active = 1;

SET @role_admin = (SELECT role_id FROM roles WHERE role_name = 'ADMIN' LIMIT 1);
SET @role_owner = (SELECT role_id FROM roles WHERE role_name = 'HORSE_OWNER' LIMIT 1);
SET @role_jockey = (SELECT role_id FROM roles WHERE role_name = 'JOCKEY' LIMIT 1);
SET @role_spectator = (SELECT role_id FROM roles WHERE role_name = 'SPECTATOR' LIMIT 1);
SET @role_referee = (SELECT role_id FROM roles WHERE role_name = 'REFEREE' LIMIT 1);
SET @role_vet = (SELECT role_id FROM roles WHERE role_name = 'VETERINARIAN' LIMIT 1);
SET @role_medical = (SELECT role_id FROM roles WHERE role_name = 'MEDICAL_STAFF' LIMIT 1);

-- Admin, 4 Owner + 4 Jockey của Flow 05,
-- 5 Owner + 8 Jockey + staff + spectator của Flow 07-11.
INSERT INTO users
    (user_id, username, password, email, dob, gender, full_name,
     phone_number, image_url, status, created_at, last_login_at, role_id)
VALUES
    (@admin_user_id, 'dmadmin', @demo_password, 'dmadmin@hrtms.test', '1990-01-01', 'MALE', 'Admin Demo', NULL, NULL, 'ACTIVE', @created_at, @seed_now, @role_admin),

    ('e5100000-0000-0000-0000-000000000001', 'mowner1', @demo_password, 'mowner1@hrtms.test', '1985-01-01', 'MALE', 'Matching Owner 1', NULL, NULL, 'ACTIVE', @created_at, @seed_now, @role_owner),
    ('e5100000-0000-0000-0000-000000000002', 'mowner2', @demo_password, 'mowner2@hrtms.test', '1986-02-02', 'FEMALE', 'Matching Owner 2', NULL, NULL, 'ACTIVE', @created_at, @seed_now, @role_owner),
    ('e5100000-0000-0000-0000-000000000003', 'mowner3', @demo_password, 'mowner3@hrtms.test', '1987-03-03', 'MALE', 'Matching Owner 3', NULL, NULL, 'ACTIVE', @created_at, @seed_now, @role_owner),
    ('e5100000-0000-0000-0000-000000000004', 'mowner4', @demo_password, 'mowner4@hrtms.test', '1988-04-04', 'FEMALE', 'Matching Owner 4', NULL, NULL, 'ACTIVE', @created_at, @seed_now, @role_owner),

    ('e5200000-0000-0000-0000-000000000001', 'mjockey1', @demo_password, 'mjockey1@hrtms.test', '1994-01-11', 'MALE', 'Matching Jockey 1', NULL, NULL, 'ACTIVE', @created_at, @seed_now, @role_jockey),
    ('e5200000-0000-0000-0000-000000000002', 'mjockey2', @demo_password, 'mjockey2@hrtms.test', '1994-02-12', 'FEMALE', 'Matching Jockey 2', NULL, NULL, 'ACTIVE', @created_at, @seed_now, @role_jockey),
    ('e5200000-0000-0000-0000-000000000003', 'mjockey3', @demo_password, 'mjockey3@hrtms.test', '1994-03-13', 'MALE', 'Matching Jockey 3', NULL, NULL, 'ACTIVE', @created_at, @seed_now, @role_jockey),
    ('e5200000-0000-0000-0000-000000000004', 'mjockey4', @demo_password, 'mjockey4@hrtms.test', '1994-04-14', 'FEMALE', 'Matching Jockey 4', NULL, NULL, 'ACTIVE', @created_at, @seed_now, @role_jockey),

    ('e7100000-0000-0000-0000-000000000001', 'fowner1', @demo_password, 'fowner1@hrtms.test', '1981-01-01', 'MALE', 'Full Flow Owner 1', NULL, NULL, 'ACTIVE', @created_at, @seed_now, @role_owner),
    ('e7100000-0000-0000-0000-000000000002', 'fowner2', @demo_password, 'fowner2@hrtms.test', '1982-02-02', 'FEMALE', 'Full Flow Owner 2', NULL, NULL, 'ACTIVE', @created_at, @seed_now, @role_owner),
    ('e7100000-0000-0000-0000-000000000003', 'fowner3', @demo_password, 'fowner3@hrtms.test', '1983-03-03', 'MALE', 'Full Flow Owner 3', NULL, NULL, 'ACTIVE', @created_at, @seed_now, @role_owner),
    ('e7100000-0000-0000-0000-000000000004', 'fowner4', @demo_password, 'fowner4@hrtms.test', '1984-04-04', 'FEMALE', 'Full Flow Owner 4', NULL, NULL, 'ACTIVE', @created_at, @seed_now, @role_owner),
    ('e7100000-0000-0000-0000-000000000005', 'fowner5', @demo_password, 'fowner5@hrtms.test', '1985-05-05', 'MALE', 'Full Flow Owner 5', NULL, NULL, 'ACTIVE', @created_at, @seed_now, @role_owner),

    ('e7200000-0000-0000-0000-000000000001', 'fjockey1', @demo_password, 'fjockey1@hrtms.test', '1995-01-01', 'MALE', 'Full Flow Jockey 1', NULL, NULL, 'ACTIVE', @created_at, @seed_now, @role_jockey),
    ('e7200000-0000-0000-0000-000000000002', 'fjockey2', @demo_password, 'fjockey2@hrtms.test', '1995-02-02', 'FEMALE', 'Full Flow Jockey 2', NULL, NULL, 'ACTIVE', @created_at, @seed_now, @role_jockey),
    ('e7200000-0000-0000-0000-000000000003', 'fjockey3', @demo_password, 'fjockey3@hrtms.test', '1995-03-03', 'MALE', 'Full Flow Jockey 3', NULL, NULL, 'ACTIVE', @created_at, @seed_now, @role_jockey),
    ('e7200000-0000-0000-0000-000000000004', 'fjockey4', @demo_password, 'fjockey4@hrtms.test', '1995-04-04', 'FEMALE', 'Full Flow Jockey 4', NULL, NULL, 'ACTIVE', @created_at, @seed_now, @role_jockey),
    ('e7200000-0000-0000-0000-000000000005', 'fjockey5', @demo_password, 'fjockey5@hrtms.test', '1995-05-05', 'MALE', 'Full Flow Jockey 5', NULL, NULL, 'ACTIVE', @created_at, @seed_now, @role_jockey),
    ('e7200000-0000-0000-0000-000000000006', 'fjockey6', @demo_password, 'fjockey6@hrtms.test', '1995-06-06', 'FEMALE', 'Full Flow Jockey 6', NULL, NULL, 'ACTIVE', @created_at, @seed_now, @role_jockey),
    ('e7200000-0000-0000-0000-000000000007', 'fjockey7', @demo_password, 'fjockey7@hrtms.test', '1995-07-07', 'MALE', 'Full Flow Jockey 7', NULL, NULL, 'ACTIVE', @created_at, @seed_now, @role_jockey),
    ('e7200000-0000-0000-0000-000000000008', 'fjockey8', @demo_password, 'fjockey8@hrtms.test', '1995-08-08', 'FEMALE', 'Full Flow Jockey 8', NULL, NULL, 'ACTIVE', @created_at, @seed_now, @role_jockey),

    ('e7400000-0000-0000-0000-000000000001', 'frace_ref', @demo_password, 'frace.ref@hrtms.test', '1980-01-10', 'MALE', 'Full Flow Race Referee', NULL, NULL, 'ACTIVE', @created_at, @seed_now, @role_referee),
    ('e7400000-0000-0000-0000-000000000002', 'fhead_ref', @demo_password, 'fhead.ref@hrtms.test', '1978-02-20', 'FEMALE', 'Full Flow Head Referee', NULL, NULL, 'ACTIVE', @created_at, @seed_now, @role_referee),
    ('e7400000-0000-0000-0000-000000000003', 'fvet1', @demo_password, 'fvet1@hrtms.test', '1982-03-15', 'FEMALE', 'Full Flow Veterinarian', NULL, NULL, 'ACTIVE', @created_at, @seed_now, @role_vet),
    ('e7400000-0000-0000-0000-000000000004', 'fmed1', @demo_password, 'fmed1@hrtms.test', '1983-04-16', 'MALE', 'Full Flow Medical Staff', NULL, NULL, 'ACTIVE', @created_at, @seed_now, @role_medical),
    ('e7400000-0000-0000-0000-000000000005', 'fspec1', @demo_password, 'fspec1@hrtms.test', '1999-05-17', 'FEMALE', 'Full Flow Spectator', NULL, NULL, 'ACTIVE', @created_at, @seed_now, @role_spectator)
ON DUPLICATE KEY UPDATE
    password = VALUES(password),
    full_name = VALUES(full_name),
    status = 'ACTIVE',
    role_id = VALUES(role_id);

INSERT INTO horse_owners (owner_id, user_id, farm_name, address, created_at)
VALUES
    ('e5110000-0000-0000-0000-000000000001', 'e5100000-0000-0000-0000-000000000001', 'Matching Farm 1', 'TP.HCM', @created_at),
    ('e5110000-0000-0000-0000-000000000002', 'e5100000-0000-0000-0000-000000000002', 'Matching Farm 2', 'Hà Nội', @created_at),
    ('e5110000-0000-0000-0000-000000000003', 'e5100000-0000-0000-0000-000000000003', 'Matching Farm 3', 'Đà Nẵng', @created_at),
    ('e5110000-0000-0000-0000-000000000004', 'e5100000-0000-0000-0000-000000000004', 'Matching Farm 4', 'Cần Thơ', @created_at),
    ('e7110000-0000-0000-0000-000000000001', 'e7100000-0000-0000-0000-000000000001', 'Full Flow Farm 1', 'TP.HCM', @created_at),
    ('e7110000-0000-0000-0000-000000000002', 'e7100000-0000-0000-0000-000000000002', 'Full Flow Farm 2', 'Hà Nội', @created_at),
    ('e7110000-0000-0000-0000-000000000003', 'e7100000-0000-0000-0000-000000000003', 'Full Flow Farm 3', 'Đà Nẵng', @created_at),
    ('e7110000-0000-0000-0000-000000000004', 'e7100000-0000-0000-0000-000000000004', 'Full Flow Farm 4', 'Huế', @created_at),
    ('e7110000-0000-0000-0000-000000000005', 'e7100000-0000-0000-0000-000000000005', 'Full Flow Farm 5', 'Cần Thơ', @created_at)
ON DUPLICATE KEY UPDATE
    farm_name = VALUES(farm_name),
    address = VALUES(address);

INSERT INTO jockeys
    (jockey_id, user_id, height, weight, experience_years, specialization,
     status, total_races, total_wins, jockey_tier, tier_updated_at, created_at)
VALUES
    ('e5210000-0000-0000-0000-000000000001', 'e5200000-0000-0000-0000-000000000001', 1.62, 50.0, 5, 'MILE', 'AVAILABLE', 15, 3, 'PROFESSIONAL', @created_at, @created_at),
    ('e5210000-0000-0000-0000-000000000002', 'e5200000-0000-0000-0000-000000000002', 1.60, 49.0, 4, 'MILE', 'AVAILABLE', 12, 2, 'JUNIOR', @created_at, @created_at),
    ('e5210000-0000-0000-0000-000000000003', 'e5200000-0000-0000-0000-000000000003', 1.64, 52.0, 7, 'MILE', 'AVAILABLE', 30, 8, 'ELITE', @created_at, @created_at),
    ('e5210000-0000-0000-0000-000000000004', 'e5200000-0000-0000-0000-000000000004', 1.61, 51.0, 6, 'MILE', 'AVAILABLE', 22, 5, 'PROFESSIONAL', @created_at, @created_at),
    ('e7210000-0000-0000-0000-000000000001', 'e7200000-0000-0000-0000-000000000001', 1.60, 48.0, 6, 'MILE', 'AVAILABLE', 20, 4, 'PROFESSIONAL', @created_at, @created_at),
    ('e7210000-0000-0000-0000-000000000002', 'e7200000-0000-0000-0000-000000000002', 1.61, 49.0, 5, 'MILE', 'AVAILABLE', 18, 3, 'PROFESSIONAL', @created_at, @created_at),
    ('e7210000-0000-0000-0000-000000000003', 'e7200000-0000-0000-0000-000000000003', 1.62, 50.0, 7, 'MILE', 'AVAILABLE', 28, 7, 'ELITE', @created_at, @created_at),
    ('e7210000-0000-0000-0000-000000000004', 'e7200000-0000-0000-0000-000000000004', 1.63, 51.0, 4, 'MILE', 'AVAILABLE', 14, 2, 'JUNIOR', @created_at, @created_at),
    ('e7210000-0000-0000-0000-000000000005', 'e7200000-0000-0000-0000-000000000005', 1.64, 52.0, 8, 'MILE', 'AVAILABLE', 35, 10, 'ELITE', @created_at, @created_at),
    ('e7210000-0000-0000-0000-000000000006', 'e7200000-0000-0000-0000-000000000006', 1.59, 48.5, 3, 'MILE', 'AVAILABLE', 10, 1, 'JUNIOR', @created_at, @created_at),
    ('e7210000-0000-0000-0000-000000000007', 'e7200000-0000-0000-0000-000000000007', 1.65, 53.0, 6, 'MILE', 'AVAILABLE', 24, 6, 'PROFESSIONAL', @created_at, @created_at),
    ('e7210000-0000-0000-0000-000000000008', 'e7200000-0000-0000-0000-000000000008', 1.58, 47.5, 5, 'MILE', 'AVAILABLE', 19, 4, 'PROFESSIONAL', @created_at, @created_at)
ON DUPLICATE KEY UPDATE
    status = 'AVAILABLE',
    weight = VALUES(weight),
    experience_years = VALUES(experience_years);

INSERT INTO referees
    (referee_id, user_id, certification_level, years_of_service, status, created_at)
VALUES
    ('e7500000-0000-0000-0000-000000000001', 'e7400000-0000-0000-0000-000000000001', 'RACE_REFEREE', 8, 'ASSIGNED', @created_at),
    ('e7500000-0000-0000-0000-000000000002', 'e7400000-0000-0000-0000-000000000002', 'HEAD_REFEREE', 12, 'ASSIGNED', @created_at)
ON DUPLICATE KEY UPDATE status = 'ASSIGNED';

INSERT INTO veterinarians
    (vet_id, user_id, specialization, years_of_service, status, created_at)
VALUES
    ('e7500000-0000-0000-0000-000000000003', 'e7400000-0000-0000-0000-000000000003', 'Equine Medicine', 9, 'ASSIGNED', @created_at)
ON DUPLICATE KEY UPDATE status = 'ASSIGNED';

INSERT INTO medical_staffs
    (med_staff_id, user_id, certification, years_of_service, status, created_at)
VALUES
    ('e7500000-0000-0000-0000-000000000004', 'e7400000-0000-0000-0000-000000000004', 'SPORT_MEDICINE', 7, 'ASSIGNED', @created_at)
ON DUPLICATE KEY UPDATE status = 'ASSIGNED';

INSERT INTO spectators (spectator_id, user_id, total_points, created_at)
VALUES
    ('e7500000-0000-0000-0000-000000000005', 'e7400000-0000-0000-0000-000000000005', 0, @created_at)
ON DUPLICATE KEY UPDATE total_points = 0;

-- ============================================================================
-- 3. WALLETS
-- ============================================================================

INSERT INTO wallets
    (wallet_id, owner_type, balance, currency, status, wallet_purpose,
     user_id, created_at, updated_at)
VALUES
    ('e8100000-0000-0000-0000-000000000001', 'USER', 30000000, 'VND', 'ACTIVE', 'USER_MAIN', 'e5100000-0000-0000-0000-000000000001', @created_at, @seed_now),
    ('e8100000-0000-0000-0000-000000000002', 'USER', 30000000, 'VND', 'ACTIVE', 'USER_MAIN', 'e5100000-0000-0000-0000-000000000002', @created_at, @seed_now),
    ('e8100000-0000-0000-0000-000000000003', 'USER', 30000000, 'VND', 'ACTIVE', 'USER_MAIN', 'e5100000-0000-0000-0000-000000000003', @created_at, @seed_now),
    ('e8100000-0000-0000-0000-000000000004', 'USER', 30000000, 'VND', 'ACTIVE', 'USER_MAIN', 'e5100000-0000-0000-0000-000000000004', @created_at, @seed_now),
    ('e8200000-0000-0000-0000-000000000001', 'USER', 0, 'VND', 'ACTIVE', 'USER_MAIN', 'e5200000-0000-0000-0000-000000000001', @created_at, @seed_now),
    ('e8200000-0000-0000-0000-000000000002', 'USER', 0, 'VND', 'ACTIVE', 'USER_MAIN', 'e5200000-0000-0000-0000-000000000002', @created_at, @seed_now),
    ('e8200000-0000-0000-0000-000000000003', 'USER', 0, 'VND', 'ACTIVE', 'USER_MAIN', 'e5200000-0000-0000-0000-000000000003', @created_at, @seed_now),
    ('e8200000-0000-0000-0000-000000000004', 'USER', 0, 'VND', 'ACTIVE', 'USER_MAIN', 'e5200000-0000-0000-0000-000000000004', @created_at, @seed_now),
    ('e8300000-0000-0000-0000-000000000001', 'USER', 10000000, 'VND', 'ACTIVE', 'USER_MAIN', 'e7100000-0000-0000-0000-000000000001', @created_at, @seed_now),
    ('e8300000-0000-0000-0000-000000000002', 'USER', 10000000, 'VND', 'ACTIVE', 'USER_MAIN', 'e7100000-0000-0000-0000-000000000002', @created_at, @seed_now),
    ('e8300000-0000-0000-0000-000000000003', 'USER', 10000000, 'VND', 'ACTIVE', 'USER_MAIN', 'e7100000-0000-0000-0000-000000000003', @created_at, @seed_now),
    ('e8300000-0000-0000-0000-000000000004', 'USER', 10000000, 'VND', 'ACTIVE', 'USER_MAIN', 'e7100000-0000-0000-0000-000000000004', @created_at, @seed_now),
    ('e8300000-0000-0000-0000-000000000005', 'USER', 10000000, 'VND', 'ACTIVE', 'USER_MAIN', 'e7100000-0000-0000-0000-000000000005', @created_at, @seed_now),
    ('e8400000-0000-0000-0000-000000000001', 'USER', 600000, 'VND', 'ACTIVE', 'USER_MAIN', 'e7200000-0000-0000-0000-000000000001', @created_at, @seed_now),
    ('e8400000-0000-0000-0000-000000000002', 'USER', 600000, 'VND', 'ACTIVE', 'USER_MAIN', 'e7200000-0000-0000-0000-000000000002', @created_at, @seed_now),
    ('e8400000-0000-0000-0000-000000000003', 'USER', 600000, 'VND', 'ACTIVE', 'USER_MAIN', 'e7200000-0000-0000-0000-000000000003', @created_at, @seed_now),
    ('e8400000-0000-0000-0000-000000000004', 'USER', 600000, 'VND', 'ACTIVE', 'USER_MAIN', 'e7200000-0000-0000-0000-000000000004', @created_at, @seed_now),
    ('e8400000-0000-0000-0000-000000000005', 'USER', 600000, 'VND', 'ACTIVE', 'USER_MAIN', 'e7200000-0000-0000-0000-000000000005', @created_at, @seed_now),
    ('e8400000-0000-0000-0000-000000000006', 'USER', 600000, 'VND', 'ACTIVE', 'USER_MAIN', 'e7200000-0000-0000-0000-000000000006', @created_at, @seed_now),
    ('e8400000-0000-0000-0000-000000000007', 'USER', 600000, 'VND', 'ACTIVE', 'USER_MAIN', 'e7200000-0000-0000-0000-000000000007', @created_at, @seed_now),
    ('e8400000-0000-0000-0000-000000000008', 'USER', 600000, 'VND', 'ACTIVE', 'USER_MAIN', 'e7200000-0000-0000-0000-000000000008', @created_at, @seed_now)
ON DUPLICATE KEY UPDATE
    balance = VALUES(balance),
    status = 'ACTIVE',
    updated_at = @seed_now;

INSERT INTO wallets
    (wallet_id, owner_type, balance, currency, status, wallet_purpose,
     user_id, created_at, updated_at)
SELECT 'efff0000-0000-0000-0000-000000000001', 'SYSTEM', 100000000,
       'VND', 'ACTIVE', 'SYSTEM_REVENUE', NULL, @created_at, @seed_now
WHERE NOT EXISTS (
    SELECT 1 FROM wallets
    WHERE owner_type = 'SYSTEM' AND wallet_purpose = 'SYSTEM_REVENUE'
);

INSERT INTO wallets
    (wallet_id, owner_type, balance, currency, status, wallet_purpose,
     user_id, created_at, updated_at)
SELECT 'efff0000-0000-0000-0000-000000000002', 'SYSTEM', 50000000,
       'VND', 'ACTIVE', 'SYSTEM_ESCROW', NULL, @created_at, @seed_now
WHERE NOT EXISTS (
    SELECT 1 FROM wallets
    WHERE owner_type = 'SYSTEM' AND wallet_purpose = 'SYSTEM_ESCROW'
);

INSERT INTO wallets
    (wallet_id, owner_type, balance, currency, status, wallet_purpose,
     user_id, created_at, updated_at)
SELECT 'efff0000-0000-0000-0000-000000000003', 'SYSTEM', 100000000,
       'VND', 'ACTIVE', 'SYSTEM_PRIZE_POOL', NULL, @created_at, @seed_now
WHERE NOT EXISTS (
    SELECT 1 FROM wallets
    WHERE owner_type = 'SYSTEM' AND wallet_purpose = 'SYSTEM_PRIZE_POOL'
);

UPDATE wallets
SET balance = 100000000, status = 'ACTIVE', updated_at = @seed_now
WHERE owner_type = 'SYSTEM' AND wallet_purpose = 'SYSTEM_REVENUE';
UPDATE wallets
SET balance = 50000000, status = 'ACTIVE', updated_at = @seed_now
WHERE owner_type = 'SYSTEM' AND wallet_purpose = 'SYSTEM_ESCROW';
UPDATE wallets
SET balance = 100000000, status = 'ACTIVE', updated_at = @seed_now
WHERE owner_type = 'SYSTEM' AND wallet_purpose = 'SYSTEM_PRIZE_POOL';

-- ============================================================================
-- 4. TOURNAMENT A - FLOW 05 JOCKEY MATCHING
-- ============================================================================

INSERT INTO tournaments (
    tournament_id, name, description, start_date, end_date, finished_at,
    location, image_url, registration_fee, system_contract_fee, total_prize_pool,
    allowed_breed, min_horse_age, max_horse_age,
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
    jockey_matching_deadline_at, scheduling_deadline_at, competition_start_at,
    current_round_name, race_class, distance, track_condition,
    top_weight_lbs, min_weight_lbs, equipment_weight_kg, handicap_enabled,
    max_approved_horses, max_approved_jockeys, max_approved_entries,
    qualifiers_per_race, max_entries_per_race, created_by
) VALUES (
    @matching_tournament_id,
    'DEMO FLOW 05 - Jockey Matching',
    '4 Owner, 4 ngựa và 4 Jockey để demo gửi lời mời, accept/reject và thanh toán hợp đồng.',
    DATE_ADD(CURDATE(), INTERVAL 2 DAY),
    DATE_ADD(CURDATE(), INTERVAL 3 DAY),
    NULL,
    'Matching Demo Track',
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
    DATE_SUB(@seed_now, INTERVAL 1 DAY),
    300,
    5,
    300,
    -300,
    30,
    300,
    300,
    '00:00:00',
    '23:59:59',
    'ONGOING',
    'JOCKEY_MATCHING',
    DATE_SUB(@seed_now, INTERVAL 10 DAY),
    DATE_SUB(@seed_now, INTERVAL 9 DAY),
    DATE_SUB(@seed_now, INTERVAL 9 DAY),
    DATE_SUB(@seed_now, INTERVAL 7 DAY),
    DATE_SUB(@seed_now, INTERVAL 5 DAY),
    DATE_ADD(@seed_now, INTERVAL 5 HOUR),
    DATE_ADD(@seed_now, INTERVAL 1 DAY),
    DATE_ADD(@seed_now, INTERVAL 2 DAY),
    NULL,
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
    16,
    @admin_user_id
);

INSERT INTO horses
    (horse_id, name, breed, gender, age, weight, color, image_url,
     health_status, current_rating, race_class, highest_rating, rating_updated_at,
     total_races, total_wins, total_places, win_rate, last_race_at,
     created_at, owner_id)
VALUES
    ('e5300000-0000-0000-0000-000000000001', 'Match Horse 1', 'THOROUGHBRED', 'MALE', 4, 445, 'BAY', NULL, 'HEALTHY', 45, 'CLASS_4', 49, @created_at, 5, 1, 2, 20, NULL, @created_at, 'e5110000-0000-0000-0000-000000000001'),
    ('e5300000-0000-0000-0000-000000000002', 'Match Horse 2', 'THOROUGHBRED', 'FEMALE', 5, 450, 'BLACK', NULL, 'HEALTHY', 48, 'CLASS_4', 52, @created_at, 6, 2, 2, 33, NULL, @created_at, 'e5110000-0000-0000-0000-000000000002'),
    ('e5300000-0000-0000-0000-000000000003', 'Match Horse 3', 'THOROUGHBRED', 'MALE', 4, 455, 'CHESTNUT', NULL, 'HEALTHY', 51, 'CLASS_4', 55, @created_at, 7, 2, 3, 29, NULL, @created_at, 'e5110000-0000-0000-0000-000000000003'),
    ('e5300000-0000-0000-0000-000000000004', 'Match Horse 4', 'THOROUGHBRED', 'FEMALE', 3, 440, 'GREY', NULL, 'HEALTHY', 54, 'CLASS_4', 57, @created_at, 4, 1, 1, 25, NULL, @created_at, 'e5110000-0000-0000-0000-000000000004');

INSERT INTO horse_tournament_registrations
    (horse_tournament_reg_id, tournament_id, horse_id, owner_id, status,
     submitted_at, reviewed_by, reviewed_at, rating_at_registration,
     race_class_at_registration, note)
VALUES
    ('e5310000-0000-0000-0000-000000000001', @matching_tournament_id, 'e5300000-0000-0000-0000-000000000001', 'e5110000-0000-0000-0000-000000000001', 'APPROVED', DATE_SUB(@seed_now, INTERVAL 8 DAY), @admin_user_id, DATE_SUB(@seed_now, INTERVAL 6 DAY), 45, 'CLASS_4', 'Approved for matching demo'),
    ('e5310000-0000-0000-0000-000000000002', @matching_tournament_id, 'e5300000-0000-0000-0000-000000000002', 'e5110000-0000-0000-0000-000000000002', 'APPROVED', DATE_SUB(@seed_now, INTERVAL 8 DAY), @admin_user_id, DATE_SUB(@seed_now, INTERVAL 6 DAY), 48, 'CLASS_4', 'Approved for matching demo'),
    ('e5310000-0000-0000-0000-000000000003', @matching_tournament_id, 'e5300000-0000-0000-0000-000000000003', 'e5110000-0000-0000-0000-000000000003', 'APPROVED', DATE_SUB(@seed_now, INTERVAL 8 DAY), @admin_user_id, DATE_SUB(@seed_now, INTERVAL 6 DAY), 51, 'CLASS_4', 'Approved for matching demo'),
    ('e5310000-0000-0000-0000-000000000004', @matching_tournament_id, 'e5300000-0000-0000-0000-000000000004', 'e5110000-0000-0000-0000-000000000004', 'APPROVED', DATE_SUB(@seed_now, INTERVAL 8 DAY), @admin_user_id, DATE_SUB(@seed_now, INTERVAL 6 DAY), 54, 'CLASS_4', 'Approved for matching demo');

INSERT INTO jockey_tournament_registrations
    (jockey_tournament_reg_id, tournament_id, jockey_id, status,
     submitted_at, reviewed_by, reviewed_at, hire_fee, note)
VALUES
    ('e5320000-0000-0000-0000-000000000001', @matching_tournament_id, 'e5210000-0000-0000-0000-000000000001', 'APPROVED', DATE_SUB(@seed_now, INTERVAL 8 DAY), @admin_user_id, DATE_SUB(@seed_now, INTERVAL 6 DAY), 2000000, 'Available for matching'),
    ('e5320000-0000-0000-0000-000000000002', @matching_tournament_id, 'e5210000-0000-0000-0000-000000000002', 'APPROVED', DATE_SUB(@seed_now, INTERVAL 8 DAY), @admin_user_id, DATE_SUB(@seed_now, INTERVAL 6 DAY), 2200000, 'Available for matching'),
    ('e5320000-0000-0000-0000-000000000003', @matching_tournament_id, 'e5210000-0000-0000-0000-000000000003', 'APPROVED', DATE_SUB(@seed_now, INTERVAL 8 DAY), @admin_user_id, DATE_SUB(@seed_now, INTERVAL 6 DAY), 2500000, 'Available for matching'),
    ('e5320000-0000-0000-0000-000000000004', @matching_tournament_id, 'e5210000-0000-0000-0000-000000000004', 'APPROVED', DATE_SUB(@seed_now, INTERVAL 8 DAY), @admin_user_id, DATE_SUB(@seed_now, INTERVAL 6 DAY), 2300000, 'Available for matching');

-- ============================================================================
-- 5. TOURNAMENT B - FULL FLOW 07 -> 11
-- ============================================================================

INSERT INTO tournaments (
    tournament_id, name, description, start_date, end_date, finished_at,
    location, image_url, registration_fee, system_contract_fee, total_prize_pool,
    allowed_breed, min_horse_age, max_horse_age,
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
    jockey_matching_deadline_at, scheduling_deadline_at, competition_start_at,
    current_round_name, race_class, distance, track_condition,
    top_weight_lbs, min_weight_lbs, equipment_weight_kg, handicap_enabled,
    max_approved_horses, max_approved_jockeys, max_approved_entries,
    qualifiers_per_race, max_entries_per_race, created_by
) VALUES (
    @full_tournament_id,
    'DEMO FLOW 07-11 - Full Final Race',
    '8 ngựa, 5 Owner, 8 Jockey: inspection, prediction, start, violation, appeal, report và payout.',
    CURDATE(),
    DATE(@full_race_end),
    NULL,
    'Full Flow Demo Track',
    NULL,
    500000,
    100000,
    20000000,
    'THOROUGHBRED',
    3,
    8,
    100, 30, 10, 50,
    6, 12, 2, 5, 1, 4, 0, 2, -8, 0, -8, 0,
    1,
    DATE_SUB(@seed_now, INTERVAL 2 DAY),
    600,
    5,
    300,
    -300,
    30,
    300,
    300,
    '00:00:00',
    '23:59:59',
    'ONGOING',
    'RACING',
    DATE_SUB(@seed_now, INTERVAL 20 DAY),
    DATE_SUB(@seed_now, INTERVAL 2 DAY),
    DATE_SUB(@seed_now, INTERVAL 20 DAY),
    DATE_SUB(@seed_now, INTERVAL 17 DAY),
    DATE_SUB(@seed_now, INTERVAL 14 DAY),
    DATE_SUB(@seed_now, INTERVAL 10 DAY),
    DATE_SUB(@seed_now, INTERVAL 5 DAY),
    @full_race_start,
    'Chung kết',
    'CLASS_3',
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
    16,
    @admin_user_id
);

INSERT INTO prize_structures
    (prize_structure_id, prize_rank, percentage, fixed_amount, is_active, tournament_id)
VALUES
    ('e7030000-0000-0000-0000-000000000001', 1, 50, 0, 1, @full_tournament_id),
    ('e7030000-0000-0000-0000-000000000002', 2, 30, 0, 1, @full_tournament_id),
    ('e7030000-0000-0000-0000-000000000003', 3, 20, 0, 1, @full_tournament_id);

-- Điều kiện hiển thị trên trang inspection.
INSERT INTO tournament_eligibility
    (eligibility_id, target_type, condition_name, condition_operator,
     condition_value, is_active, tournament_id)
VALUES
    ('e7040000-0000-0000-0000-000000000001', 'HORSE', 'AGE', 'GREATER_THAN_OR_EQUAL', '3', 1, @full_tournament_id),
    ('e7040000-0000-0000-0000-000000000002', 'HORSE', 'AGE', 'LESS_THAN_OR_EQUAL', '8', 1, @full_tournament_id),
    ('e7040000-0000-0000-0000-000000000003', 'HORSE', 'WEIGHT', 'GREATER_THAN_OR_EQUAL', '400', 1, @full_tournament_id),
    ('e7040000-0000-0000-0000-000000000004', 'HORSE', 'WEIGHT', 'LESS_THAN_OR_EQUAL', '600', 1, @full_tournament_id),
    ('e7040000-0000-0000-0000-000000000005', 'JOCKEY', 'WEIGHT', 'GREATER_THAN_OR_EQUAL', '45', 1, @full_tournament_id),
    ('e7040000-0000-0000-0000-000000000006', 'JOCKEY', 'WEIGHT', 'LESS_THAN_OR_EQUAL', '65', 1, @full_tournament_id),
    ('e7040000-0000-0000-0000-000000000007', 'JOCKEY', 'EXPERIENCE_YEARS', 'GREATER_THAN_OR_EQUAL', '1', 1, @full_tournament_id);

INSERT INTO horses
    (horse_id, name, breed, gender, age, weight, color, image_url,
     health_status, current_rating, race_class, highest_rating, rating_updated_at,
     total_races, total_wins, total_places, win_rate, last_race_at,
     created_at, owner_id)
VALUES
    ('e7310000-0000-0000-0000-000000000001', 'Final Horse 1', 'THOROUGHBRED', 'MALE', 4, 445, 'BAY', NULL, 'HEALTHY', 60, 'CLASS_3', 65, @created_at, 12, 3, 6, 25, NULL, @created_at, 'e7110000-0000-0000-0000-000000000001'),
    ('e7310000-0000-0000-0000-000000000002', 'Final Horse 2', 'THOROUGHBRED', 'FEMALE', 4, 450, 'BLACK', NULL, 'HEALTHY', 62, 'CLASS_3', 67, @created_at, 11, 2, 5, 18, NULL, @created_at, 'e7110000-0000-0000-0000-000000000002'),
    ('e7310000-0000-0000-0000-000000000003', 'Final Horse 3', 'THOROUGHBRED', 'MALE', 5, 455, 'CHESTNUT', NULL, 'HEALTHY', 64, 'CLASS_3', 70, @created_at, 15, 4, 7, 27, NULL, @created_at, 'e7110000-0000-0000-0000-000000000003'),
    ('e7310000-0000-0000-0000-000000000004', 'Final Horse 4', 'THOROUGHBRED', 'FEMALE', 3, 440, 'GREY', NULL, 'HEALTHY', 66, 'CLASS_3', 69, @created_at, 9, 2, 4, 22, NULL, @created_at, 'e7110000-0000-0000-0000-000000000004'),
    ('e7310000-0000-0000-0000-000000000005', 'Final Horse 5', 'THOROUGHBRED', 'MALE', 6, 460, 'BAY', NULL, 'HEALTHY', 68, 'CLASS_3', 74, @created_at, 18, 5, 9, 28, NULL, @created_at, 'e7110000-0000-0000-0000-000000000005'),
    ('e7310000-0000-0000-0000-000000000006', 'Final Horse 6', 'THOROUGHBRED', 'FEMALE', 4, 448, 'BLACK', NULL, 'HEALTHY', 70, 'CLASS_3', 75, @created_at, 14, 3, 7, 21, NULL, @created_at, 'e7110000-0000-0000-0000-000000000001'),
    ('e7310000-0000-0000-0000-000000000007', 'Final Horse 7', 'THOROUGHBRED', 'MALE', 5, 452, 'CHESTNUT', NULL, 'HEALTHY', 72, 'CLASS_3', 78, @created_at, 20, 6, 10, 30, NULL, @created_at, 'e7110000-0000-0000-0000-000000000002'),
    ('e7310000-0000-0000-0000-000000000008', 'Final Horse 8', 'THOROUGHBRED', 'FEMALE', 4, 442, 'GREY', NULL, 'HEALTHY', 74, 'CLASS_3', 79, @created_at, 16, 4, 8, 25, NULL, @created_at, 'e7110000-0000-0000-0000-000000000003');

INSERT INTO horse_tournament_registrations
    (horse_tournament_reg_id, tournament_id, horse_id, owner_id, status,
     submitted_at, reviewed_by, reviewed_at, rating_at_registration,
     race_class_at_registration, note)
VALUES
    ('e7320000-0000-0000-0000-000000000001', @full_tournament_id, 'e7310000-0000-0000-0000-000000000001', 'e7110000-0000-0000-0000-000000000001', 'APPROVED', DATE_SUB(@seed_now, INTERVAL 16 DAY), @admin_user_id, DATE_SUB(@seed_now, INTERVAL 14 DAY), 60, 'CLASS_3', 'Full flow approved'),
    ('e7320000-0000-0000-0000-000000000002', @full_tournament_id, 'e7310000-0000-0000-0000-000000000002', 'e7110000-0000-0000-0000-000000000002', 'APPROVED', DATE_SUB(@seed_now, INTERVAL 16 DAY), @admin_user_id, DATE_SUB(@seed_now, INTERVAL 14 DAY), 62, 'CLASS_3', 'Full flow approved'),
    ('e7320000-0000-0000-0000-000000000003', @full_tournament_id, 'e7310000-0000-0000-0000-000000000003', 'e7110000-0000-0000-0000-000000000003', 'APPROVED', DATE_SUB(@seed_now, INTERVAL 16 DAY), @admin_user_id, DATE_SUB(@seed_now, INTERVAL 14 DAY), 64, 'CLASS_3', 'Full flow approved'),
    ('e7320000-0000-0000-0000-000000000004', @full_tournament_id, 'e7310000-0000-0000-0000-000000000004', 'e7110000-0000-0000-0000-000000000004', 'APPROVED', DATE_SUB(@seed_now, INTERVAL 16 DAY), @admin_user_id, DATE_SUB(@seed_now, INTERVAL 14 DAY), 66, 'CLASS_3', 'Full flow approved'),
    ('e7320000-0000-0000-0000-000000000005', @full_tournament_id, 'e7310000-0000-0000-0000-000000000005', 'e7110000-0000-0000-0000-000000000005', 'APPROVED', DATE_SUB(@seed_now, INTERVAL 16 DAY), @admin_user_id, DATE_SUB(@seed_now, INTERVAL 14 DAY), 68, 'CLASS_3', 'Full flow approved'),
    ('e7320000-0000-0000-0000-000000000006', @full_tournament_id, 'e7310000-0000-0000-0000-000000000006', 'e7110000-0000-0000-0000-000000000001', 'APPROVED', DATE_SUB(@seed_now, INTERVAL 16 DAY), @admin_user_id, DATE_SUB(@seed_now, INTERVAL 14 DAY), 70, 'CLASS_3', 'Full flow approved'),
    ('e7320000-0000-0000-0000-000000000007', @full_tournament_id, 'e7310000-0000-0000-0000-000000000007', 'e7110000-0000-0000-0000-000000000002', 'APPROVED', DATE_SUB(@seed_now, INTERVAL 16 DAY), @admin_user_id, DATE_SUB(@seed_now, INTERVAL 14 DAY), 72, 'CLASS_3', 'Full flow approved'),
    ('e7320000-0000-0000-0000-000000000008', @full_tournament_id, 'e7310000-0000-0000-0000-000000000008', 'e7110000-0000-0000-0000-000000000003', 'APPROVED', DATE_SUB(@seed_now, INTERVAL 16 DAY), @admin_user_id, DATE_SUB(@seed_now, INTERVAL 14 DAY), 74, 'CLASS_3', 'Full flow approved');

INSERT INTO jockey_tournament_registrations
    (jockey_tournament_reg_id, tournament_id, jockey_id, status,
     submitted_at, reviewed_by, reviewed_at, hire_fee, note)
VALUES
    ('e7330000-0000-0000-0000-000000000001', @full_tournament_id, 'e7210000-0000-0000-0000-000000000001', 'APPROVED', DATE_SUB(@seed_now, INTERVAL 16 DAY), @admin_user_id, DATE_SUB(@seed_now, INTERVAL 14 DAY), 2000000, 'Full flow approved'),
    ('e7330000-0000-0000-0000-000000000002', @full_tournament_id, 'e7210000-0000-0000-0000-000000000002', 'APPROVED', DATE_SUB(@seed_now, INTERVAL 16 DAY), @admin_user_id, DATE_SUB(@seed_now, INTERVAL 14 DAY), 2000000, 'Full flow approved'),
    ('e7330000-0000-0000-0000-000000000003', @full_tournament_id, 'e7210000-0000-0000-0000-000000000003', 'APPROVED', DATE_SUB(@seed_now, INTERVAL 16 DAY), @admin_user_id, DATE_SUB(@seed_now, INTERVAL 14 DAY), 2000000, 'Full flow approved'),
    ('e7330000-0000-0000-0000-000000000004', @full_tournament_id, 'e7210000-0000-0000-0000-000000000004', 'APPROVED', DATE_SUB(@seed_now, INTERVAL 16 DAY), @admin_user_id, DATE_SUB(@seed_now, INTERVAL 14 DAY), 2000000, 'Full flow approved'),
    ('e7330000-0000-0000-0000-000000000005', @full_tournament_id, 'e7210000-0000-0000-0000-000000000005', 'APPROVED', DATE_SUB(@seed_now, INTERVAL 16 DAY), @admin_user_id, DATE_SUB(@seed_now, INTERVAL 14 DAY), 2000000, 'Full flow approved'),
    ('e7330000-0000-0000-0000-000000000006', @full_tournament_id, 'e7210000-0000-0000-0000-000000000006', 'APPROVED', DATE_SUB(@seed_now, INTERVAL 16 DAY), @admin_user_id, DATE_SUB(@seed_now, INTERVAL 14 DAY), 2000000, 'Full flow approved'),
    ('e7330000-0000-0000-0000-000000000007', @full_tournament_id, 'e7210000-0000-0000-0000-000000000007', 'APPROVED', DATE_SUB(@seed_now, INTERVAL 16 DAY), @admin_user_id, DATE_SUB(@seed_now, INTERVAL 14 DAY), 2000000, 'Full flow approved'),
    ('e7330000-0000-0000-0000-000000000008', @full_tournament_id, 'e7210000-0000-0000-0000-000000000008', 'APPROVED', DATE_SUB(@seed_now, INTERVAL 16 DAY), @admin_user_id, DATE_SUB(@seed_now, INTERVAL 14 DAY), 2000000, 'Full flow approved');

-- 8 contract đã qua thanh toán:
-- - 30% (600.000) đã trả Jockey.
-- - 70% (1.400.000) còn nằm trong SYSTEM_ESCROW.
-- - Khi Admin publish Final Report, BE tự release 70% còn lại.
INSERT INTO jockey_horse_contracts (
    contract_id, tournament_id, horse_tournament_reg_id, jockey_tournament_reg_id,
    owner_id, horse_id, jockey_id, hire_fee, advance_percent, final_percent,
    advance_paid_amount, escrow_amount, system_contract_fee,
    owner_prize_share_percent, jockey_prize_share_percent,
    payment_status, escrow_status, advance_payout_status, final_payout_status,
    status, advance_payout_at, requested_at, responded_at, accepted_at,
    submitted_at, contract_note
) VALUES
    ('e7340000-0000-0000-0000-000000000001', @full_tournament_id, 'e7320000-0000-0000-0000-000000000001', 'e7330000-0000-0000-0000-000000000001', 'e7110000-0000-0000-0000-000000000001', 'e7310000-0000-0000-0000-000000000001', 'e7210000-0000-0000-0000-000000000001', 2000000, 30, 70, 600000, 1400000, 100000, 80, 20, 'PAID', 'PARTIALLY_RELEASED', 'PAID', 'NOT_RELEASED', 'APPROVED', DATE_SUB(@seed_now, INTERVAL 8 DAY), DATE_SUB(@seed_now, INTERVAL 10 DAY), DATE_SUB(@seed_now, INTERVAL 9 DAY), DATE_SUB(@seed_now, INTERVAL 9 DAY), DATE_SUB(@seed_now, INTERVAL 8 DAY), 'Full flow contract 1'),
    ('e7340000-0000-0000-0000-000000000002', @full_tournament_id, 'e7320000-0000-0000-0000-000000000002', 'e7330000-0000-0000-0000-000000000002', 'e7110000-0000-0000-0000-000000000002', 'e7310000-0000-0000-0000-000000000002', 'e7210000-0000-0000-0000-000000000002', 2000000, 30, 70, 600000, 1400000, 100000, 80, 20, 'PAID', 'PARTIALLY_RELEASED', 'PAID', 'NOT_RELEASED', 'APPROVED', DATE_SUB(@seed_now, INTERVAL 8 DAY), DATE_SUB(@seed_now, INTERVAL 10 DAY), DATE_SUB(@seed_now, INTERVAL 9 DAY), DATE_SUB(@seed_now, INTERVAL 9 DAY), DATE_SUB(@seed_now, INTERVAL 8 DAY), 'Full flow contract 2'),
    ('e7340000-0000-0000-0000-000000000003', @full_tournament_id, 'e7320000-0000-0000-0000-000000000003', 'e7330000-0000-0000-0000-000000000003', 'e7110000-0000-0000-0000-000000000003', 'e7310000-0000-0000-0000-000000000003', 'e7210000-0000-0000-0000-000000000003', 2000000, 30, 70, 600000, 1400000, 100000, 80, 20, 'PAID', 'PARTIALLY_RELEASED', 'PAID', 'NOT_RELEASED', 'APPROVED', DATE_SUB(@seed_now, INTERVAL 8 DAY), DATE_SUB(@seed_now, INTERVAL 10 DAY), DATE_SUB(@seed_now, INTERVAL 9 DAY), DATE_SUB(@seed_now, INTERVAL 9 DAY), DATE_SUB(@seed_now, INTERVAL 8 DAY), 'Full flow contract 3'),
    ('e7340000-0000-0000-0000-000000000004', @full_tournament_id, 'e7320000-0000-0000-0000-000000000004', 'e7330000-0000-0000-0000-000000000004', 'e7110000-0000-0000-0000-000000000004', 'e7310000-0000-0000-0000-000000000004', 'e7210000-0000-0000-0000-000000000004', 2000000, 30, 70, 600000, 1400000, 100000, 80, 20, 'PAID', 'PARTIALLY_RELEASED', 'PAID', 'NOT_RELEASED', 'APPROVED', DATE_SUB(@seed_now, INTERVAL 8 DAY), DATE_SUB(@seed_now, INTERVAL 10 DAY), DATE_SUB(@seed_now, INTERVAL 9 DAY), DATE_SUB(@seed_now, INTERVAL 9 DAY), DATE_SUB(@seed_now, INTERVAL 8 DAY), 'Full flow contract 4'),
    ('e7340000-0000-0000-0000-000000000005', @full_tournament_id, 'e7320000-0000-0000-0000-000000000005', 'e7330000-0000-0000-0000-000000000005', 'e7110000-0000-0000-0000-000000000005', 'e7310000-0000-0000-0000-000000000005', 'e7210000-0000-0000-0000-000000000005', 2000000, 30, 70, 600000, 1400000, 100000, 80, 20, 'PAID', 'PARTIALLY_RELEASED', 'PAID', 'NOT_RELEASED', 'APPROVED', DATE_SUB(@seed_now, INTERVAL 8 DAY), DATE_SUB(@seed_now, INTERVAL 10 DAY), DATE_SUB(@seed_now, INTERVAL 9 DAY), DATE_SUB(@seed_now, INTERVAL 9 DAY), DATE_SUB(@seed_now, INTERVAL 8 DAY), 'Full flow contract 5'),
    ('e7340000-0000-0000-0000-000000000006', @full_tournament_id, 'e7320000-0000-0000-0000-000000000006', 'e7330000-0000-0000-0000-000000000006', 'e7110000-0000-0000-0000-000000000001', 'e7310000-0000-0000-0000-000000000006', 'e7210000-0000-0000-0000-000000000006', 2000000, 30, 70, 600000, 1400000, 100000, 80, 20, 'PAID', 'PARTIALLY_RELEASED', 'PAID', 'NOT_RELEASED', 'APPROVED', DATE_SUB(@seed_now, INTERVAL 8 DAY), DATE_SUB(@seed_now, INTERVAL 10 DAY), DATE_SUB(@seed_now, INTERVAL 9 DAY), DATE_SUB(@seed_now, INTERVAL 9 DAY), DATE_SUB(@seed_now, INTERVAL 8 DAY), 'Full flow contract 6'),
    ('e7340000-0000-0000-0000-000000000007', @full_tournament_id, 'e7320000-0000-0000-0000-000000000007', 'e7330000-0000-0000-0000-000000000007', 'e7110000-0000-0000-0000-000000000002', 'e7310000-0000-0000-0000-000000000007', 'e7210000-0000-0000-0000-000000000007', 2000000, 30, 70, 600000, 1400000, 100000, 80, 20, 'PAID', 'PARTIALLY_RELEASED', 'PAID', 'NOT_RELEASED', 'APPROVED', DATE_SUB(@seed_now, INTERVAL 8 DAY), DATE_SUB(@seed_now, INTERVAL 10 DAY), DATE_SUB(@seed_now, INTERVAL 9 DAY), DATE_SUB(@seed_now, INTERVAL 9 DAY), DATE_SUB(@seed_now, INTERVAL 8 DAY), 'Full flow contract 7'),
    ('e7340000-0000-0000-0000-000000000008', @full_tournament_id, 'e7320000-0000-0000-0000-000000000008', 'e7330000-0000-0000-0000-000000000008', 'e7110000-0000-0000-0000-000000000003', 'e7310000-0000-0000-0000-000000000008', 'e7210000-0000-0000-0000-000000000008', 2000000, 30, 70, 600000, 1400000, 100000, 80, 20, 'PAID', 'PARTIALLY_RELEASED', 'PAID', 'NOT_RELEASED', 'APPROVED', DATE_SUB(@seed_now, INTERVAL 8 DAY), DATE_SUB(@seed_now, INTERVAL 10 DAY), DATE_SUB(@seed_now, INTERVAL 9 DAY), DATE_SUB(@seed_now, INTERVAL 9 DAY), DATE_SUB(@seed_now, INTERVAL 8 DAY), 'Full flow contract 8');

INSERT INTO rounds (
    round_id, round_name, sequence_order, is_final, prediction_type,
    advancement_rule, start_date, end_date, description,
    max_races, max_entries, min_entries, status,
    head_referee_id, head_referee_assigned_at,
    expected_entries, qualifiers_per_race, advanced_at,
    transition_status, created_at, tournament_id, created_by
) VALUES (
    @full_round_id,
    'Chung kết',
    1,
    1,
    'TOP3',
    'Top 3 nhận giải thưởng chung cuộc',
    @full_race_start,
    @full_race_end,
    'Final Round gồm đúng một race và 8 entry.',
    1,
    8,
    8,
    'SCHEDULED',
    'e7500000-0000-0000-0000-000000000002',
    DATE_SUB(@seed_now, INTERVAL 2 DAY),
    8,
    4,
    NULL,
    'NOT_READY',
    DATE_SUB(@seed_now, INTERVAL 3 DAY),
    @full_tournament_id,
    @admin_user_id
);

INSERT INTO races (
    race_id, name, start_time, end_time, track_condition, distance,
    sequence_order, status, started_at, finished_at,
    schedule_published_at, prediction_open_at, prediction_close_at,
    ai_prediction_publication_status, round_id, created_by, started_by,
    inspection_finalized_at
) VALUES (
    @full_race_id,
    'DEMO FINAL RACE - Flow 07 to 11',
    @full_race_start,
    @full_race_end,
    'TURF',
    'MILE_1600M',
    1,
    'SCHEDULED',
    NULL,
    NULL,
    DATE_SUB(@seed_now, INTERVAL 2 DAY),
    @prediction_open,
    @prediction_close,
    'DRAFT',
    @full_round_id,
    @admin_user_id,
    NULL,
    NULL
);

INSERT INTO race_entries
    (entry_id, race_id, contract_id, lane_number, status,
     assigned_by, assigned_at, created_at)
VALUES
    ('e7350000-0000-0000-0000-000000000001', @full_race_id, 'e7340000-0000-0000-0000-000000000001', 1, 'CONFIRMED', @admin_user_id, DATE_SUB(@seed_now, INTERVAL 2 DAY), DATE_SUB(@seed_now, INTERVAL 2 DAY)),
    ('e7350000-0000-0000-0000-000000000002', @full_race_id, 'e7340000-0000-0000-0000-000000000002', 2, 'CONFIRMED', @admin_user_id, DATE_SUB(@seed_now, INTERVAL 2 DAY), DATE_SUB(@seed_now, INTERVAL 2 DAY)),
    ('e7350000-0000-0000-0000-000000000003', @full_race_id, 'e7340000-0000-0000-0000-000000000003', 3, 'CONFIRMED', @admin_user_id, DATE_SUB(@seed_now, INTERVAL 2 DAY), DATE_SUB(@seed_now, INTERVAL 2 DAY)),
    ('e7350000-0000-0000-0000-000000000004', @full_race_id, 'e7340000-0000-0000-0000-000000000004', 4, 'CONFIRMED', @admin_user_id, DATE_SUB(@seed_now, INTERVAL 2 DAY), DATE_SUB(@seed_now, INTERVAL 2 DAY)),
    ('e7350000-0000-0000-0000-000000000005', @full_race_id, 'e7340000-0000-0000-0000-000000000005', 5, 'CONFIRMED', @admin_user_id, DATE_SUB(@seed_now, INTERVAL 2 DAY), DATE_SUB(@seed_now, INTERVAL 2 DAY)),
    ('e7350000-0000-0000-0000-000000000006', @full_race_id, 'e7340000-0000-0000-0000-000000000006', 6, 'CONFIRMED', @admin_user_id, DATE_SUB(@seed_now, INTERVAL 2 DAY), DATE_SUB(@seed_now, INTERVAL 2 DAY)),
    ('e7350000-0000-0000-0000-000000000007', @full_race_id, 'e7340000-0000-0000-0000-000000000007', 7, 'CONFIRMED', @admin_user_id, DATE_SUB(@seed_now, INTERVAL 2 DAY), DATE_SUB(@seed_now, INTERVAL 2 DAY)),
    ('e7350000-0000-0000-0000-000000000008', @full_race_id, 'e7340000-0000-0000-0000-000000000008', 8, 'CONFIRMED', @admin_user_id, DATE_SUB(@seed_now, INTERVAL 2 DAY), DATE_SUB(@seed_now, INTERVAL 2 DAY));

INSERT INTO race_referees
    (race_referee_id, race_id, referee_id, assigned_by, assigned_at)
VALUES
    ('e7360000-0000-0000-0000-000000000001',
     @full_race_id,
     'e7500000-0000-0000-0000-000000000001',
     @admin_user_id,
     DATE_SUB(@seed_now, INTERVAL 2 DAY));

INSERT INTO race_inspection_staff_assignments
    (assignment_id, race_id, vet_id, med_staff_id, assigned_by, assigned_at)
VALUES
    ('e7360000-0000-0000-0000-000000000002',
     @full_race_id,
     'e7500000-0000-0000-0000-000000000003',
     'e7500000-0000-0000-0000-000000000004',
     @admin_user_id,
     DATE_SUB(@seed_now, INTERVAL 2 DAY));

INSERT INTO appeal_categories
    (category_id, code, name, description, is_active, created_at)
VALUES
    ('e7370000-0000-0000-0000-000000000001', 'RESULT_ERROR', 'Sai kết quả', 'Khiếu nại thứ hạng hoặc thời gian về đích.', 1, @created_at),
    ('e7370000-0000-0000-0000-000000000002', 'RACE_INCIDENT', 'Sự cố đường đua', 'Khiếu nại va chạm hoặc cản trở.', 1, @created_at),
    ('e7370000-0000-0000-0000-000000000003', 'VIOLATION', 'Quyết định vi phạm', 'Khiếu nại quyết định cảnh cáo hoặc loại.', 1, @created_at)
ON DUPLICATE KEY UPDATE
    name = VALUES(name),
    description = VALUES(description),
    is_active = 1;

-- ============================================================================
-- 6. KIỂM TRA SAU SEED
-- ============================================================================

SET FOREIGN_KEY_CHECKS = @OLD_FOREIGN_KEY_CHECKS;
SET SQL_SAFE_UPDATES = @OLD_SQL_SAFE_UPDATES;

SELECT '=== DEMO FLOW 05 - MATCHING ===' AS section;
SELECT tournament_id, name, phase, status, jockey_matching_deadline_at
FROM tournaments
WHERE tournament_id = @matching_tournament_id;

SELECT
    COUNT(DISTINCT htr.horse_id) AS approved_horses,
    COUNT(DISTINCT htr.owner_id) AS distinct_owners
FROM horse_tournament_registrations htr
WHERE htr.tournament_id = @matching_tournament_id
  AND htr.status = 'APPROVED';

SELECT COUNT(*) AS approved_jockeys
FROM jockey_tournament_registrations
WHERE tournament_id = @matching_tournament_id
  AND status = 'APPROVED';

SELECT '=== DEMO FLOW 07-11 - FINAL RACE ===' AS section;
SELECT
    t.name,
    t.phase,
    r.name AS race_name,
    r.status AS race_status,
    r.start_time,
    r.end_time,
    r.prediction_open_at,
    r.prediction_close_at
FROM tournaments t
JOIN rounds rd ON rd.tournament_id = t.tournament_id
JOIN races r ON r.round_id = rd.round_id
WHERE t.tournament_id = @full_tournament_id;

SELECT
    COUNT(*) AS entries,
    COUNT(DISTINCT c.owner_id) AS distinct_owners,
    COUNT(DISTINCT c.jockey_id) AS distinct_jockeys,
    SUM(c.escrow_amount) AS final_hiring_fee_waiting_release
FROM race_entries re
JOIN jockey_horse_contracts c ON c.contract_id = re.contract_id
WHERE re.race_id = @full_race_id;

SELECT username, full_name, '12345678' AS password
FROM users
WHERE user_id IN (
    @admin_user_id,
    'e5100000-0000-0000-0000-000000000001',
    'e5200000-0000-0000-0000-000000000001',
    'e7100000-0000-0000-0000-000000000001',
    'e7200000-0000-0000-0000-000000000001',
    'e7400000-0000-0000-0000-000000000001',
    'e7400000-0000-0000-0000-000000000002',
    'e7400000-0000-0000-0000-000000000003',
    'e7400000-0000-0000-0000-000000000004',
    'e7400000-0000-0000-0000-000000000005'
)
ORDER BY username;

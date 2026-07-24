-- ============================================================================
-- HRTMS - KỊCH BẢN DEMO 1 GIẢI 4 NGỰA (XUYÊN SUỐT LUỒNG 05 -> 11)
-- Phục vụ demo: 
--   - Luồng 05: Contract Matching (P Hưng)
--   - Luồng 07: Inspection (P Hưng)
--   - Luồng 08: Race Operations (Hải)
--   - Luồng 09: Vi phạm & Khiếu nại (KHung)
--   - Luồng 10: Spectator Prediction (Hải)
--   - Luồng 11: Prize Payout (P Hưng)
--
-- Đặc điểm:
--   - Thời gian linh hoạt theo NOW() / CURDATE(), chạy được bất kỳ lúc nào.
--   - Setup sẵn dữ liệu cho 1 giải 4 ngựa duy nhất: "DEMO 4H - Full Flow Demo".
--   - Có thể dùng để thao tác từng bước trên UI hoặc kiểm tra toàn bộ luồng.
-- ============================================================================

USE SWP391_Project_HRTMS;
SET NAMES utf8mb4 COLLATE utf8mb4_unicode_ci;

SET @OLD_FOREIGN_KEY_CHECKS = @@FOREIGN_KEY_CHECKS;
SET @OLD_SQL_SAFE_UPDATES = @@SQL_SAFE_UPDATES;
SET FOREIGN_KEY_CHECKS = 0;
SET SQL_SAFE_UPDATES = 0;

-- --------------------------------------------------------------------------
-- 1. THỜI GIAN & BIẾN CƠ BẢN
-- --------------------------------------------------------------------------
SET @seed_now = NOW();
SET @account_created_at = DATE_SUB(@seed_now, INTERVAL 45 DAY);
-- Mật khẩu BCrypt cho '12345678'
SET @demo_password = '$2a$12$ZGrUyKDU0UvqY0kpswOtoO58uurKVC2yVAA0iTlcnYI4pmPb18mBS';

-- FIXED UUIDs để dễ tham chiếu
SET @t4h_tournament_id = '90000000-0000-0000-0000-000000000001';
SET @t4h_round_id      = '90000000-0000-0000-0000-000000000002';
SET @t4h_race_id       = '90000000-0000-0000-0000-000000000003';

SET @u_admin_id     = '11111111-1111-1111-1111-111111111111';
SET @u_owner1_id    = '11111111-1111-1111-1111-111111111121';
SET @p_owner1_id    = '12111111-1111-1111-1111-111111111121';

SET @u_spectator1_id = '11111111-1111-1111-1111-111111111131';
SET @p_spectator1_id = '13111111-1111-1111-1111-111111111131';

SET @u_referee1_id  = '11111111-1111-1111-1111-111111111151'; -- Head Referee
SET @p_referee1_id  = '15111111-1111-1111-1111-111111111151';
SET @u_referee2_id  = '11111111-1111-1111-1111-111111111152'; -- Race Referee
SET @p_referee2_id  = '15111111-1111-1111-1111-111111111152';

SET @u_vet1_id      = '11111111-1111-1111-1111-111111111161';
SET @p_vet1_id      = '16111111-1111-1111-1111-111111111161';

SET @u_medical1_id  = '11111111-1111-1111-1111-111111111171';
SET @p_medical1_id  = '17111111-1111-1111-1111-111111111171';

-- 4 Jockeys cho 4 Lane
SET @u_jockey1_id = '11111111-1111-1111-1111-111111111141'; SET @p_jockey1_id = '14111111-1111-1111-1111-111111111141';
SET @u_jockey2_id = '11111111-1111-1111-1111-111111111142'; SET @p_jockey2_id = '14111111-1111-1111-1111-111111111142';
SET @u_jockey3_id = '11111111-1111-1111-1111-111111111143'; SET @p_jockey3_id = '14111111-1111-1111-1111-111111111143';
SET @u_jockey9_id = '11111111-1111-1111-1111-111111111149'; SET @p_jockey9_id = '14111111-1111-1111-1111-111111111149'; -- Dùng cho Luồng 05

-- 4 Horses
SET @h_horse1_id = '80000000-0000-0000-0000-000000000001';
SET @h_horse2_id = '80000000-0000-0000-0000-000000000002';
SET @h_horse3_id = '80000000-0000-0000-0000-000000000003';
SET @h_horse4_id = '80000000-0000-0000-0000-000000000004';

-- Registrations
SET @h_reg1_id = '81000000-0000-0000-0000-000000000001';
SET @h_reg2_id = '81000000-0000-0000-0000-000000000002';
SET @h_reg3_id = '81000000-0000-0000-0000-000000000003';
SET @h_reg4_id = '81000000-0000-0000-0000-000000000004';

SET @j_reg1_id = '82000000-0000-0000-0000-000000000001';
SET @j_reg2_id = '82000000-0000-0000-0000-000000000002';
SET @j_reg3_id = '82000000-0000-0000-0000-000000000003';
SET @j_reg9_id = '82000000-0000-0000-0000-000000000009';

-- Contracts
SET @c_contract1_id = '83000000-0000-0000-0000-000000000001';
SET @c_contract2_id = '83000000-0000-0000-0000-000000000002';
SET @c_contract3_id = '83000000-0000-0000-0000-000000000003';
SET @c_contract4_id = '83000000-0000-0000-0000-000000000004'; -- Luồng 05 demo

-- Race Entries
SET @e_entry1_id = '84000000-0000-0000-0000-000000000001';
SET @e_entry2_id = '84000000-0000-0000-0000-000000000002';
SET @e_entry3_id = '84000000-0000-0000-0000-000000000003';
SET @e_entry4_id = '84000000-0000-0000-0000-000000000004';

-- Wallets
SET @w_owner1_id   = 'aaaa0000-0000-0000-0000-000000000011';
SET @w_jockey1_id  = 'aaaa0000-0000-0000-0000-000000000041';
SET @w_jockey2_id  = 'aaaa0000-0000-0000-0000-000000000042';
SET @w_jockey3_id  = 'aaaa0000-0000-0000-0000-000000000043';
SET @w_jockey9_id  = 'aaaa0000-0000-0000-0000-000000000049';
SET @w_sys_revenue = 'aaaa0000-0000-0000-0000-000000000001';
SET @w_sys_escrow  = 'aaaa0000-0000-0000-0000-000000000002';
SET @w_sys_prize   = 'aaaa0000-0000-0000-0000-000000000003';

-- --------------------------------------------------------------------------
-- 2. DỌN CỦ DỮ LIỆU DEMO 4H (IF EXISTS)
-- --------------------------------------------------------------------------
DELETE FROM wallet_transactions WHERE contract_id = @c_contract4_id COLLATE utf8mb4_unicode_ci OR race_result_id IN (SELECT result_id FROM race_results WHERE race_id = @t4h_race_id COLLATE utf8mb4_unicode_ci);
DELETE FROM horse_rating_histories WHERE race_id = @t4h_race_id;
DELETE FROM prediction_detail WHERE prediction_id IN (SELECT prediction_id FROM predictions WHERE race_id = @t4h_race_id);
DELETE FROM predictions WHERE race_id = @t4h_race_id;
DELETE FROM appeal_evidences WHERE appeal_id IN (SELECT appeal_id FROM appeals WHERE entry_id IN (SELECT entry_id FROM race_entries WHERE race_id = @t4h_race_id));
DELETE FROM appeals WHERE entry_id IN (SELECT entry_id FROM race_entries WHERE race_id = @t4h_race_id);
DELETE FROM violations WHERE entry_id IN (SELECT entry_id FROM race_entries WHERE race_id = @t4h_race_id);
DELETE FROM race_reports WHERE race_id = @t4h_race_id;
DELETE FROM race_results WHERE race_id = @t4h_race_id;
DELETE FROM horse_inspections WHERE entry_id IN (SELECT entry_id FROM race_entries WHERE race_id = @t4h_race_id);
DELETE FROM jockey_inspections WHERE entry_id IN (SELECT entry_id FROM race_entries WHERE race_id = @t4h_race_id);
DELETE FROM race_referees WHERE race_id = @t4h_race_id;
DELETE FROM race_inspection_staff_assignments WHERE race_id = @t4h_race_id;
DELETE FROM race_entries WHERE race_id = @t4h_race_id;
DELETE FROM races WHERE race_id = @t4h_race_id;
DELETE FROM rounds WHERE tournament_id = @t4h_tournament_id;
DELETE FROM invoices WHERE contract_id IN (@c_contract1_id, @c_contract2_id, @c_contract3_id, @c_contract4_id);
DELETE FROM jockey_horse_contracts WHERE tournament_id = @t4h_tournament_id;
DELETE FROM horse_tournament_registrations WHERE tournament_id = @t4h_tournament_id;
DELETE FROM jockey_tournament_registrations WHERE tournament_id = @t4h_tournament_id;
DELETE FROM prize_structures WHERE tournament_id = @t4h_tournament_id;
DELETE FROM tournaments WHERE tournament_id = @t4h_tournament_id;

-- --------------------------------------------------------------------------
-- 3. ROLES, USERS, PROFILES, WALLETS
-- --------------------------------------------------------------------------

-- Đảm bảo có Roles
INSERT INTO roles (role_id, role_name, description, is_active, created_at)
VALUES
('00000000-0000-0000-0000-000000000001', 'ADMIN', 'Quản trị hệ thống', 1, @account_created_at),
('00000000-0000-0000-0000-000000000002', 'HORSE_OWNER', 'Chủ ngựa', 1, @account_created_at),
('00000000-0000-0000-0000-000000000003', 'JOCKEY', 'Kỵ sĩ', 1, @account_created_at),
('00000000-0000-0000-0000-000000000004', 'SPECTATOR', 'Khán giả', 1, @account_created_at),
('00000000-0000-0000-0000-000000000005', 'REFEREE', 'Trọng tài', 1, @account_created_at),
('00000000-0000-0000-0000-000000000006', 'VETERINARIAN', 'Bác sĩ thú y', 1, @account_created_at),
('00000000-0000-0000-0000-000000000007', 'MEDICAL_STAFF', 'Nhân viên y tế', 1, @account_created_at)
ON DUPLICATE KEY UPDATE is_active = 1;

-- Users
INSERT INTO users (user_id, username, password, email, dob, gender, full_name, phone_number, status, role_id, created_at) VALUES
(@u_admin_id, 'admin1', @demo_password, 'admin1@hrtms.test', '1990-01-01', 'MALE', 'Hải Admin', '0900000001', 'ACTIVE', '00000000-0000-0000-0000-000000000001', @account_created_at),
(@u_owner1_id, 'owner1', @demo_password, 'owner1@hrtms.test', '1988-05-15', 'MALE', 'Tuấn Chủ Ngựa', '0900000011', 'ACTIVE', '00000000-0000-0000-0000-000000000002', @account_created_at),
(@u_spectator1_id, 'spectator1', @demo_password, 'spectator1@hrtms.test', '1998-01-01', 'MALE', 'Hải Khán Giả', '0900000021', 'ACTIVE', '00000000-0000-0000-0000-000000000004', @account_created_at),
(@u_referee1_id, 'referee1', @demo_password, 'referee1@hrtms.test', '1985-02-12', 'MALE', 'Hải Trọng Tài Chính', '0900000051', 'ACTIVE', '00000000-0000-0000-0000-000000000005', @account_created_at),
(@u_referee2_id, 'referee2', @demo_password, 'referee2@hrtms.test', '1986-03-14', 'MALE', 'Hải Trọng Tài Đua', '0900000052', 'ACTIVE', '00000000-0000-0000-0000-000000000005', @account_created_at),
(@u_vet1_id, 'vet1', @demo_password, 'vet1@hrtms.test', '1987-04-20', 'FEMALE', 'P Hưng Thú Y', '0900000061', 'ACTIVE', '00000000-0000-0000-0000-000000000006', @account_created_at),
(@u_medical1_id, 'medical1', @demo_password, 'medical1@hrtms.test', '1990-09-18', 'MALE', 'P Hưng Y Tế', '0900000071', 'ACTIVE', '00000000-0000-0000-0000-000000000007', @account_created_at),
(@u_jockey1_id, 'jockey1', @demo_password, 'jockey1@hrtms.test', '1995-01-10', 'MALE', 'KHung Kỵ Sĩ 1', '0900000041', 'ACTIVE', '00000000-0000-0000-0000-000000000003', @account_created_at),
(@u_jockey2_id, 'jockey2', @demo_password, 'jockey2@hrtms.test', '1996-02-11', 'MALE', 'Kỵ Sĩ 2', '0900000042', 'ACTIVE', '00000000-0000-0000-0000-000000000003', @account_created_at),
(@u_jockey3_id, 'jockey3', @demo_password, 'jockey3@hrtms.test', '1997-03-12', 'MALE', 'Kỵ Sĩ 3', '0900000043', 'ACTIVE', '00000000-0000-0000-0000-000000000003', @account_created_at),
(@u_jockey9_id, 'jockey9', @demo_password, 'jockey9@hrtms.test', '1994-09-09', 'MALE', 'P Hưng Kỵ Sĩ 9', '0900000049', 'ACTIVE', '00000000-0000-0000-0000-000000000003', @account_created_at)
ON DUPLICATE KEY UPDATE status = 'ACTIVE';

-- Profiles
INSERT INTO horse_owners (owner_id, user_id, farm_name, address, created_at) VALUES
(@p_owner1_id, @u_owner1_id, 'Trang Trại Demo 4H', 'TP. Hồ Chí Minh', @account_created_at)
ON DUPLICATE KEY UPDATE farm_name = VALUES(farm_name);

INSERT INTO spectators (spectator_id, user_id, total_points, created_at) VALUES
(@p_spectator1_id, @u_spectator1_id, 100, @account_created_at)
ON DUPLICATE KEY UPDATE total_points = 100;

INSERT INTO referees (referee_id, user_id, certification_level, years_of_service, status, created_at) VALUES
(@p_referee1_id, @u_referee1_id, 'International A', 10, 'AVAILABLE', @account_created_at),
(@p_referee2_id, @u_referee2_id, 'National A', 6, 'AVAILABLE', @account_created_at)
ON DUPLICATE KEY UPDATE status = 'AVAILABLE';

INSERT INTO veterinarians (vet_id, user_id, specialization, years_of_service, status, created_at) VALUES
(@p_vet1_id, @u_vet1_id, 'Equine Medicine', 8, 'AVAILABLE', @account_created_at)
ON DUPLICATE KEY UPDATE status = 'AVAILABLE';

INSERT INTO medical_staffs (med_staff_id, user_id, certification, years_of_service, status, created_at) VALUES
(@p_medical1_id, @u_medical1_id, 'MED-0001', 6, 'AVAILABLE', @account_created_at)
ON DUPLICATE KEY UPDATE status = 'AVAILABLE';

INSERT INTO jockeys (jockey_id, user_id, height, weight, experience_years, specialization, status, total_races, total_wins, jockey_tier, tier_updated_at, created_at) VALUES
(@p_jockey1_id, @u_jockey1_id, 1.62, 50.0, 5, 'MILE', 'AVAILABLE', 20, 5, 'PROFESSIONAL', NOW(), @account_created_at),
(@p_jockey2_id, @u_jockey2_id, 1.60, 49.0, 4, 'MILE', 'AVAILABLE', 15, 3, 'JUNIOR', NOW(), @account_created_at),
(@p_jockey3_id, @u_jockey3_id, 1.65, 51.0, 6, 'MILE', 'AVAILABLE', 25, 7, 'ELITE', NOW(), @account_created_at),
(@p_jockey9_id, @u_jockey9_id, 1.63, 50.5, 7, 'MILE', 'AVAILABLE', 30, 10, 'ELITE', NOW(), @account_created_at)
ON DUPLICATE KEY UPDATE status = 'AVAILABLE';

-- Wallets
INSERT INTO wallets (wallet_id, owner_type, balance, currency, status, wallet_purpose, user_id, created_at, updated_at) VALUES
(@w_owner1_id, 'USER', 150000000.00, 'VND', 'ACTIVE', 'USER_MAIN', @u_owner1_id, @account_created_at, NOW()),
(@w_jockey1_id, 'USER', 20000000.00, 'VND', 'ACTIVE', 'USER_MAIN', @u_jockey1_id, @account_created_at, NOW()),
(@w_jockey2_id, 'USER', 15000000.00, 'VND', 'ACTIVE', 'USER_MAIN', @u_jockey2_id, @account_created_at, NOW()),
(@w_jockey3_id, 'USER', 18000000.00, 'VND', 'ACTIVE', 'USER_MAIN', @u_jockey3_id, @account_created_at, NOW()),
(@w_jockey9_id, 'USER', 25000000.00, 'VND', 'ACTIVE', 'USER_MAIN', @u_jockey9_id, @account_created_at, NOW()),
(@w_sys_revenue, 'SYSTEM', 500000000.00, 'VND', 'ACTIVE', 'SYSTEM_REVENUE', NULL, @account_created_at, NOW()),
(@w_sys_escrow, 'SYSTEM', 1000000000.00, 'VND', 'ACTIVE', 'SYSTEM_ESCROW', NULL, @account_created_at, NOW()),
(@w_sys_prize, 'SYSTEM', 1000000000.00, 'VND', 'ACTIVE', 'SYSTEM_PRIZE_POOL', NULL, @account_created_at, NOW())
ON DUPLICATE KEY UPDATE status = 'ACTIVE';

-- Category khiếu nại (Luồng 09)
INSERT INTO appeal_categories (category_id, code, name, description, is_active, created_at) VALUES
('ac000000-0000-0000-0000-000000000001', 'RESULT_ERROR', 'Sai kết quả', 'Khiếu nại thứ hạng hoặc thời gian', 1, NOW()),
('ac000000-0000-0000-0000-000000000002', 'RACE_INCIDENT', 'Sự cố đường đua', 'Khiếu nại va chạm, cản trở', 1, NOW()),
('ac000000-0000-0000-0000-000000000003', 'VIOLATION', 'Vi phạm', 'Khiếu nại quyết định xử lý vi phạm', 1, NOW())
ON DUPLICATE KEY UPDATE is_active = 1;

-- --------------------------------------------------------------------------
-- 4. TOURNAMENT (DEMO 4H - Flow 05-11) & PRIZE STRUCTURE
-- --------------------------------------------------------------------------

INSERT INTO tournaments (
    tournament_id, name, description, start_date, end_date, finished_at,
    location, registration_fee, system_contract_fee, total_prize_pool,
    allowed_breed, min_horse_age, max_horse_age,
    prediction_top1_correct_points, prediction_top3_exact_position_points,
    prediction_top3_correct_horse_points, prediction_top3_perfect_bonus_points,
    prediction_open_minutes_before, prediction_close_minutes_before,
    inspection_open_minutes_before, inspection_close_minutes_before,
    min_race_interval_minutes, start_late_tolerance_minutes,
    default_race_operational_minutes, race_day_start_time, race_day_end_time,
    track_condition, status, phase,
    created_at, published_at, registration_open_at, registration_close_at,
    review_deadline_at, jockey_matching_deadline_at, scheduling_deadline_at,
    competition_start_at, current_round_name, race_class, distance,
    top_weight_lbs, min_weight_lbs, equipment_weight_kg, handicap_enabled,
    max_approved_horses, max_approved_jockeys, max_approved_entries,
    qualifiers_per_race, max_entries_per_race, created_by
) VALUES (
    @t4h_tournament_id, 'DEMO 4H - Full Flow Demo (05-11)',
    'Giải đấu demo duy nhất 4 ngựa chạy xuyên suốt toàn bộ các luồng từ ghép kỵ sĩ tới trả thưởng.',
    CURDATE(), DATE_ADD(CURDATE(), INTERVAL 5 DAY), NULL,
    'Trường đua Demo HRTMS', 500000.00, 100000.00, 20000000.00,
    'THOROUGHBRED', 3, 8,
    100, 30, 10, 50,
    180, 5,
    185, 6, 35, 0, 180, '00:00:00', '23:59:59',
    'TURF', 'ONGOING', 'RACING',
    DATE_SUB(NOW(), INTERVAL 10 DAY), DATE_SUB(NOW(), INTERVAL 9 DAY),
    DATE_SUB(NOW(), INTERVAL 9 DAY), DATE_SUB(NOW(), INTERVAL 7 DAY),
    DATE_SUB(NOW(), INTERVAL 6 DAY), DATE_SUB(NOW(), INTERVAL 4 DAY),
    DATE_SUB(NOW(), INTERVAL 2 DAY), NOW(),
    'Chung kết', 'CLASS_4', 'MILE_1600M',
    0, 0, 0.0, 0,
    4, 4, 4,
    4, 16, @u_admin_id
);

-- Cơ cấu giải thưởng: Top 1 (50% = 10tr), Top 2 (30% = 6tr), Top 3 (20% = 4tr)
INSERT INTO prize_structures (prize_structure_id, `prize_rank`, percentage, fixed_amount, is_active, tournament_id) VALUES
(UUID(), 1, 50.0, 0.00, 1, @t4h_tournament_id),
(UUID(), 2, 30.0, 0.00, 1, @t4h_tournament_id),
(UUID(), 3, 20.0, 0.00, 1, @t4h_tournament_id);

-- --------------------------------------------------------------------------
-- 5. HỒ SƠ NGỰA, ĐĂNG KÝ VÀ HỢP ĐỒNG (LUỒNG 05 DEMO)
-- --------------------------------------------------------------------------

-- 4 chú ngựa cho owner1
INSERT INTO horses (horse_id, name, breed, gender, age, weight, color, health_status, current_rating, race_class, highest_rating, rating_updated_at, total_races, total_wins, total_places, win_rate, created_at, owner_id) VALUES
(@h_horse1_id, 'DemoHorse01', 'THOROUGHBRED', 'MALE', 4, 440, 'Bay', 'HEALTHY', 48, 'CLASS_4', 52, NOW(), 5, 2, 1, 0.40, @account_created_at, @p_owner1_id),
(@h_horse2_id, 'DemoHorse02', 'THOROUGHBRED', 'FEMALE', 4, 435, 'Black', 'HEALTHY', 50, 'CLASS_4', 54, NOW(), 6, 1, 3, 0.16, @account_created_at, @p_owner1_id),
(@h_horse3_id, 'DemoHorse03', 'THOROUGHBRED', 'MALE', 5, 445, 'Chestnut', 'HEALTHY', 46, 'CLASS_4', 50, NOW(), 4, 1, 1, 0.25, @account_created_at, @p_owner1_id),
(@h_horse4_id, 'DemoHorse04', 'THOROUGHBRED', 'MALE', 4, 438, 'Gray', 'HEALTHY', 49, 'CLASS_4', 51, NOW(), 5, 2, 0, 0.40, @account_created_at, @p_owner1_id)
ON DUPLICATE KEY UPDATE name = VALUES(name);

-- Horse Registrations (APPROVED)
INSERT INTO horse_tournament_registrations (horse_tournament_reg_id, tournament_id, horse_id, owner_id, status, submitted_at, reviewed_by, reviewed_at, rating_at_registration, race_class_at_registration, note) VALUES
(@h_reg1_id, @t4h_tournament_id, @h_horse1_id, @p_owner1_id, 'APPROVED', DATE_SUB(NOW(), INTERVAL 8 DAY), @u_admin_id, DATE_SUB(NOW(), INTERVAL 7 DAY), 48, 'CLASS_4', 'Duyệt đăng ký 4H-1'),
(@h_reg2_id, @t4h_tournament_id, @h_horse2_id, @p_owner1_id, 'APPROVED', DATE_SUB(NOW(), INTERVAL 8 DAY), @u_admin_id, DATE_SUB(NOW(), INTERVAL 7 DAY), 50, 'CLASS_4', 'Duyệt đăng ký 4H-2'),
(@h_reg3_id, @t4h_tournament_id, @h_horse3_id, @p_owner1_id, 'APPROVED', DATE_SUB(NOW(), INTERVAL 8 DAY), @u_admin_id, DATE_SUB(NOW(), INTERVAL 7 DAY), 46, 'CLASS_4', 'Duyệt đăng ký 4H-3'),
(@h_reg4_id, @t4h_tournament_id, @h_horse4_id, @p_owner1_id, 'APPROVED', DATE_SUB(NOW(), INTERVAL 8 DAY), @u_admin_id, DATE_SUB(NOW(), INTERVAL 7 DAY), 49, 'CLASS_4', 'Duyệt đăng ký 4H-4');

-- Jockey Registrations (APPROVED)
INSERT INTO jockey_tournament_registrations (jockey_tournament_reg_id, tournament_id, jockey_id, status, submitted_at, hire_fee, note) VALUES
(@j_reg1_id, @t4h_tournament_id, @p_jockey1_id, 'APPROVED', DATE_SUB(NOW(), INTERVAL 8 DAY), 2000000.00, 'Kỵ sĩ 1 sẵn sàng'),
(@j_reg2_id, @t4h_tournament_id, @p_jockey2_id, 'APPROVED', DATE_SUB(NOW(), INTERVAL 8 DAY), 2000000.00, 'Kỵ sĩ 2 sẵn sàng'),
(@j_reg3_id, @t4h_tournament_id, @p_jockey3_id, 'APPROVED', DATE_SUB(NOW(), INTERVAL 8 DAY), 2000000.00, 'Kỵ sĩ 3 sẵn sàng'),
(@j_reg9_id, @t4h_tournament_id, @p_jockey9_id, 'APPROVED', DATE_SUB(NOW(), INTERVAL 8 DAY), 5000000.00, 'Kỵ sĩ 9 sẵn sàng (Luồng 05)');

-- --------------------------------------------------------------------------
-- LUỒNG 05: Hợp đồng Chủ ngựa & Jockey (Contract Matching)
-- 3 Contract đầu đã APPROVED sẵn.
-- Contract thứ 4 (DemoHorse04 + Jockey9) được set APPROVED để tham gia đua,
-- đồng thời tạo đầy đủ hóa đơn đã thanh toán để kiểm tra quy trình ký quỹ (Escrow).
-- --------------------------------------------------------------------------

INSERT INTO jockey_horse_contracts (
    contract_id, tournament_id, horse_tournament_reg_id, jockey_tournament_reg_id,
    owner_id, horse_id, jockey_id, hire_fee, advance_percent, final_percent,
    advance_paid_amount, escrow_amount, system_contract_fee,
    owner_prize_share_percent, jockey_prize_share_percent,
    payment_status, escrow_status, advance_payout_status, final_payout_status,
    status, advance_payout_at, requested_at, responded_at, accepted_at, contract_note
) VALUES
(@c_contract1_id, @t4h_tournament_id, @h_reg1_id, @j_reg1_id, @p_owner1_id, @h_horse1_id, @p_jockey1_id, 2000000.00, 30.0, 70.0, 600000.00, 1400000.00, 100000.00, 80.0, 20.0, 'PAID', 'HELD', 'PAID', 'NOT_RELEASED', 'APPROVED', DATE_SUB(NOW(), INTERVAL 3 DAY), DATE_SUB(NOW(), INTERVAL 5 DAY), DATE_SUB(NOW(), INTERVAL 4 DAY), DATE_SUB(NOW(), INTERVAL 4 DAY), 'Hợp đồng 1'),
(@c_contract2_id, @t4h_tournament_id, @h_reg2_id, @j_reg2_id, @p_owner1_id, @h_horse2_id, @p_jockey2_id, 2000000.00, 30.0, 70.0, 600000.00, 1400000.00, 100000.00, 80.0, 20.0, 'PAID', 'HELD', 'PAID', 'NOT_RELEASED', 'APPROVED', DATE_SUB(NOW(), INTERVAL 3 DAY), DATE_SUB(NOW(), INTERVAL 5 DAY), DATE_SUB(NOW(), INTERVAL 4 DAY), DATE_SUB(NOW(), INTERVAL 4 DAY), 'Hợp đồng 2'),
(@c_contract3_id, @t4h_tournament_id, @h_reg3_id, @j_reg3_id, @p_owner1_id, @h_horse3_id, @p_jockey3_id, 2000000.00, 30.0, 70.0, 600000.00, 1400000.00, 100000.00, 80.0, 20.0, 'PAID', 'HELD', 'PAID', 'NOT_RELEASED', 'APPROVED', DATE_SUB(NOW(), INTERVAL 3 DAY), DATE_SUB(NOW(), INTERVAL 5 DAY), DATE_SUB(NOW(), INTERVAL 4 DAY), DATE_SUB(NOW(), INTERVAL 4 DAY), 'Hợp đồng 3'),
-- Contract 4 demo Luồng 05 (Phí thuê 5,000,000, Cọc 30% = 1.5tr, Escrow 70% = 3.5tr)
(@c_contract4_id, @t4h_tournament_id, @h_reg4_id, @j_reg9_id, @p_owner1_id, @h_horse4_id, @p_jockey9_id, 5000000.00, 30.0, 70.0, 1500000.00, 3500000.00, 100000.00, 80.0, 20.0, 'PAID', 'HELD', 'PAID', 'NOT_RELEASED', 'APPROVED', DATE_SUB(NOW(), INTERVAL 2 DAY), DATE_SUB(NOW(), INTERVAL 4 DAY), DATE_SUB(NOW(), INTERVAL 3 DAY), DATE_SUB(NOW(), INTERVAL 3 DAY), 'Hợp đồng Luồng 05: Owner1 thuê Jockey9');

-- Invoices cho Contract 4
INSERT INTO invoices (invoice_id, payer_user_id, contract_id, invoice_type, amount, status, due_date, paid_at, created_at, note) VALUES
(UUID(), @u_owner1_id, @c_contract4_id, 'JOCKEY_HIRING_FEE', 5000000.00, 'PAID', DATE_SUB(NOW(), INTERVAL 2 DAY), DATE_SUB(NOW(), INTERVAL 3 DAY), DATE_SUB(NOW(), INTERVAL 4 DAY), 'Thanh toán phí thuê kỵ sĩ Jockey9'),
(UUID(), @u_owner1_id, @c_contract4_id, 'CONTRACT_CREATION_FEE', 100000.00, 'PAID', DATE_SUB(NOW(), INTERVAL 2 DAY), DATE_SUB(NOW(), INTERVAL 3 DAY), DATE_SUB(NOW(), INTERVAL 4 DAY), 'Phí lập hợp đồng hệ thống');

-- --------------------------------------------------------------------------
-- 6. ROUND, RACE, ENTRY & STAFF ASSIGNMENT (LUỒNG 07, 08 TIMELINE)
-- --------------------------------------------------------------------------

-- Race Start lúc NOW() + 185 phút, Kết thúc lúc NOW() + 365 phút
SET @t4h_race_start = DATE_ADD(@seed_now, INTERVAL 185 MINUTE);
SET @t4h_race_end   = DATE_ADD(@t4h_race_start, INTERVAL 180 MINUTE);
SET @t4h_pred_open  = DATE_SUB(@seed_now, INTERVAL 1 MINUTE);
SET @t4h_pred_close = DATE_ADD(@seed_now, INTERVAL 180 MINUTE);

INSERT INTO rounds (
    round_id, round_name, sequence_order, is_final, prediction_type,
    advancement_rule, start_date, end_date, description,
    max_races, max_entries, min_entries, status,
    head_referee_id, head_referee_assigned_at,
    expected_entries, qualifiers_per_race,
    transition_status, created_at, tournament_id, created_by
) VALUES (
    @t4h_round_id, 'Chung kết', 1, 1, 'TOP3',
    'Xác định Top 3 chung cuộc nhận giải',
    @t4h_race_start, @t4h_race_end, 'Vòng chung kết 4 ngựa',
    1, 4, 4, 'SCHEDULED',
    @p_referee1_id, DATE_SUB(NOW(), INTERVAL 2 DAY),
    4, 0, 'NOT_READY', DATE_SUB(NOW(), INTERVAL 3 DAY), @t4h_tournament_id, @u_admin_id
);

INSERT INTO races (
    race_id, name, start_time, end_time, track_condition, distance,
    sequence_order, status, schedule_published_at, prediction_open_at, prediction_close_at,
    round_id, created_by
) VALUES (
    @t4h_race_id, 'DEMO 4H - Final Race', @t4h_race_start, @t4h_race_end, 'TURF', 'MILE_1600M',
    1, 'FINISHED', DATE_SUB(@t4h_pred_open, INTERVAL 1 MINUTE), @t4h_pred_open, @t4h_pred_close,
    @t4h_round_id, @u_admin_id
);

-- Race Entries cho 4 làn
INSERT INTO race_entries (entry_id, race_id, contract_id, lane_number, status, assigned_by, assigned_at, created_at) VALUES
(@e_entry1_id, @t4h_race_id, @c_contract1_id, 1, 'FINISHED', @u_admin_id, DATE_SUB(NOW(), INTERVAL 2 DAY), DATE_SUB(NOW(), INTERVAL 2 DAY)),
(@e_entry2_id, @t4h_race_id, @c_contract2_id, 2, 'FINISHED', @u_admin_id, DATE_SUB(NOW(), INTERVAL 2 DAY), DATE_SUB(NOW(), INTERVAL 2 DAY)),
(@e_entry3_id, @t4h_race_id, @c_contract3_id, 3, 'FINISHED', @u_admin_id, DATE_SUB(NOW(), INTERVAL 2 DAY), DATE_SUB(NOW(), INTERVAL 2 DAY)),
(@e_entry4_id, @t4h_race_id, @c_contract4_id, 4, 'FINISHED', @u_admin_id, DATE_SUB(NOW(), INTERVAL 2 DAY), DATE_SUB(NOW(), INTERVAL 2 DAY));

-- Phân công trọng tài & y tế/thú y
INSERT INTO race_referees (race_referee_id, race_id, referee_id, assigned_by, assigned_at) VALUES
(UUID(), @t4h_race_id, @p_referee2_id, @u_admin_id, DATE_SUB(NOW(), INTERVAL 2 DAY));

INSERT INTO race_inspection_staff_assignments (assignment_id, race_id, vet_id, med_staff_id, assigned_by, assigned_at) VALUES
(UUID(), @t4h_race_id, @p_vet1_id, @p_medical1_id, @u_admin_id, DATE_SUB(NOW(), INTERVAL 2 DAY));

-- --------------------------------------------------------------------------
-- 7. LUỒNG 07: INSPECTION (BÁC SĨ THÚ Y VÀ NHÂN VIÊN Y TẾ KHÁM)
-- --------------------------------------------------------------------------

INSERT INTO horse_inspections (horse_inspection_id, entry_id, vet_id, result, note, inspected_at, handicap_weight, registered_weight, registered_breed, actual_weight, actual_breed, doping_detected, is_handicap_confirmed, confirmed_at, status) VALUES
(UUID(), @e_entry1_id, @p_vet1_id, 'PASS', 'Sức khỏe tốt, âm tính doping', DATE_SUB(NOW(), INTERVAL 1 HOUR), 0.0, 440, 'THOROUGHBRED', 440, 'THOROUGHBRED', 0, 1, DATE_SUB(NOW(), INTERVAL 50 MINUTE), 'CONFIRMED'),
(UUID(), @e_entry2_id, @p_vet1_id, 'PASS', 'Sức khỏe tốt, âm tính doping', DATE_SUB(NOW(), INTERVAL 1 HOUR), 0.0, 435, 'THOROUGHBRED', 435, 'THOROUGHBRED', 0, 1, DATE_SUB(NOW(), INTERVAL 50 MINUTE), 'CONFIRMED'),
(UUID(), @e_entry3_id, @p_vet1_id, 'PASS', 'Sức khỏe tốt, âm tính doping', DATE_SUB(NOW(), INTERVAL 1 HOUR), 0.0, 445, 'THOROUGHBRED', 445, 'THOROUGHBRED', 0, 1, DATE_SUB(NOW(), INTERVAL 50 MINUTE), 'CONFIRMED'),
(UUID(), @e_entry4_id, @p_vet1_id, 'PASS', 'Sức khỏe tốt, âm tính doping', DATE_SUB(NOW(), INTERVAL 1 HOUR), 0.0, 438, 'THOROUGHBRED', 438, 'THOROUGHBRED', 0, 1, DATE_SUB(NOW(), INTERVAL 50 MINUTE), 'CONFIRMED');

INSERT INTO jockey_inspections (jockey_inspection_id, entry_id, med_staff_id, result, note, inspected_at, registered_weight, actual_weight, doping_detected, status) VALUES
(UUID(), @e_entry1_id, @p_medical1_id, 'PASS', 'Thể trạng đạt chuẩn, huyết áp bình thường', DATE_SUB(NOW(), INTERVAL 1 HOUR), 50.0, 50.0, 0, 'CONFIRMED'),
(UUID(), @e_entry2_id, @p_medical1_id, 'PASS', 'Thể trạng đạt chuẩn, huyết áp bình thường', DATE_SUB(NOW(), INTERVAL 1 HOUR), 49.0, 49.0, 0, 'CONFIRMED'),
(UUID(), @e_entry3_id, @p_medical1_id, 'PASS', 'Thể trạng đạt chuẩn, huyết áp bình thường', DATE_SUB(NOW(), INTERVAL 1 HOUR), 51.0, 51.0, 0, 'CONFIRMED'),
(UUID(), @e_entry4_id, @p_medical1_id, 'PASS', 'Thể trạng đạt chuẩn, huyết áp bình thường', DATE_SUB(NOW(), INTERVAL 1 HOUR), 50.5, 50.5, 0, 'CONFIRMED');

-- --------------------------------------------------------------------------
-- 8. LUỒNG 10: DỰ ĐOÁN CỦA KHÁN GIẢ (SPECTATOR PREDICTION)
-- Spectator1 dự đoán Top 3 cho Race Final
-- --------------------------------------------------------------------------

SET @pred_id = '70000000-0000-0000-0000-000000000001';

INSERT INTO predictions (prediction_id, spectator_id, race_id, prediction_type, prediction_time, status, reward_points, scored_at) VALUES
(@pred_id, @p_spectator1_id, @t4h_race_id, 'TOP3', DATE_SUB(NOW(), INTERVAL 30 MINUTE), 'SCORED', 140, NOW());

INSERT INTO prediction_detail (prediction_detail_id, prediction_id, entry_id, predicted_rank, status, awarded_points) VALUES
(UUID(), @pred_id, @e_entry1_id, 1, 'CORRECT', 100), -- Dự đoán Hạng 1 -> Đúng (100đ)
(UUID(), @pred_id, @e_entry3_id, 2, 'INCORRECT', 10), -- Dự đoán Hạng 2 -> Ngựa về Hạng 3 (10đ đúng ngựa)
(UUID(), @pred_id, @e_entry2_id, 3, 'INCORRECT', 30);  -- Dự đoán Hạng 3 -> Ngựa về Hạng 2 (30đ đúng vị trí)

-- --------------------------------------------------------------------------
-- 9. LUỒNG 08: VẬN HÀNH RACE (KẾT QUẢ VỀ ĐÍCH CỦA 4 NGỰA)
-- --------------------------------------------------------------------------

SET @res1_id = '71000000-0000-0000-0000-000000000001';
SET @res2_id = '71000000-0000-0000-0000-000000000002';
SET @res3_id = '71000000-0000-0000-0000-000000000003';
SET @res4_id = '71000000-0000-0000-0000-000000000004';

INSERT INTO race_results (
    result_id, race_id, entry_id, finish_time, finish_position,
    prize_money, owner_prize_amount, jockey_prize_amount,
    prize_status, is_prize_paid, prize_paid_at, status, rating_change,
    recorded_by, recorded_at, updated_at
) VALUES
-- Hạng 1: DemoHorse01 (Thưởng 10tr -> Owner 8tr, Jockey 2tr)
(@res1_id, @t4h_race_id, @e_entry1_id, 95.20, 1, 10000000.00, 8000000.00, 2000000.00, 'Paid', 1, NOW(), 'FINISHED', 6, @u_referee2_id, NOW(), NOW()),
-- Hạng 2: DemoHorse02 (Thưởng 6tr -> Owner 4.8tr, Jockey 1.2tr)
(@res2_id, @t4h_race_id, @e_entry2_id, 96.00, 2, 6000000.00, 4800000.00, 1200000.00, 'Paid', 1, NOW(), 'FINISHED', 2, @u_referee2_id, NOW(), NOW()),
-- Hạng 3: DemoHorse03 (Thưởng 4tr -> Owner 3.2tr, Jockey 800k)
(@res3_id, @t4h_race_id, @e_entry3_id, 96.80, 3, 4000000.00, 3200000.00, 8000000.00, 'Paid', 1, NOW(), 'FINISHED', 1, @u_referee2_id, NOW(), NOW()),
-- Hạng 4: DemoHorse04 (Không giải thưởng)
(@res4_id, @t4h_race_id, @e_entry4_id, 97.60, 4, 0.00, 0.00, 0.00, 'NotEligible', 0, NULL, 'FINISHED', 0, @u_referee2_id, NOW(), NOW());

-- --------------------------------------------------------------------------
-- 10. LUỒNG 09: VI PHẠM & KHIẾU NẠI (VIOLATION & APPEAL)
-- Trọng tài đua 2 ghi vi phạm chèn làn cho Entry 4 (DemoHorse04 - Jockey9) -> Warning.
-- Owner1 gửi khiếu nại -> Trọng tài chính Referee1 xử lý Từ Chối (Rejected).
-- --------------------------------------------------------------------------

SET @violation_id = '72000000-0000-0000-0000-000000000001';
SET @appeal_id    = '73000000-0000-0000-0000-000000000001';

INSERT INTO violations (violation_id, entry_id, referee_id, type, description, penalty_type, penalty_value, occurred_at, created_at, status) VALUES
(@violation_id, @e_entry4_id, @p_referee2_id, 'OBSTRUCTION', 'Cản trở chèn làn tại khúc ngoặt 200m cuối', 'WARNING', 0.0, DATE_SUB(NOW(), INTERVAL 20 MINUTE), DATE_SUB(NOW(), INTERVAL 20 MINUTE), 'RESOLVED');

INSERT INTO appeals (appeal_id, entry_id, race_result_id, related_violation_id, category_id, submitted_by_user_id, description, status, submitted_at, reviewed_by_referee_id, reviewed_at, resolution) VALUES
(@appeal_id, @e_entry4_id, @res4_id, @violation_id, 'ac000000-0000-0000-0000-000000000003', @u_owner1_id, 'Khiếu nại quyết định cảnh cáo: Jockey9 không cố ý cản trở, chỉ giữ làn đua.', 'Rejected', DATE_SUB(NOW(), INTERVAL 15 MINUTE), @p_referee1_id, DATE_SUB(NOW(), INTERVAL 5 MINUTE), 'Bác bỏ khiếu nại: Video xem lại cho thấy rõ hành vi ép làn gây nguy hiểm.');

INSERT INTO appeal_evidences (evidence_id, appeal_id, type, file_url, text_content, description, uploaded_at) VALUES
(UUID(), @appeal_id, 'Text', NULL, 'Bằng chứng ghi hình từ góc camera lane 4 góc ngoặt cuối.', 'Video bằng chứng từ Owner', DATE_SUB(NOW(), INTERVAL 15 MINUTE));

-- --------------------------------------------------------------------------
-- 11. LUỒNG 11: BÁO CÁO, KÝ DUYỆT, RATING VÀ THANH TOÁN GIẢI THƯỞNG (PRIZE PAYOUT)
-- Race Report: Draft (Referee2) -> Signed (Referee1) -> Published (Admin1)
-- Trả thưởng: Chia giải 20tr (Owner 80%, Jockey 20%) & Giải phóng 70% Escrow cho Jockey
-- --------------------------------------------------------------------------

INSERT INTO race_reports (
    report_id, race_id, referee_id, summary, appeal_note, status,
    submitted_at, submitted_by, signed_by, signed_at, published_by, published_at, created_at
) VALUES (
    UUID(), @t4h_race_id, @p_referee2_id,
    'Biên bản trận đấu chung kết 4 ngựa. Đã xử lý xong 1 khiếu nại vi phạm cản trở. Kết quả hợp lệ.',
    'Khiếu nại của Owner1 đã bị bác bỏ sau khi xem xét kỹ băng hình.',
    'PUBLISHED',
    DATE_SUB(NOW(), INTERVAL 10 MINUTE), @p_referee2_id,
    @p_referee1_id, DATE_SUB(NOW(), INTERVAL 5 MINUTE),
    @u_admin_id, NOW(), DATE_SUB(NOW(), INTERVAL 10 MINUTE)
);

-- Cập nhật Rating lịch sử cho 4 chú ngựa
INSERT INTO horse_rating_histories (rating_history_id, horse_id, race_id, race_result_id, old_rating, final_change, adjustment_reason, new_rating, old_race_class, new_race_class, policy_version, calculated_at) VALUES
(UUID(), @h_horse1_id, @t4h_race_id, @res1_id, 48, 6, 'Hạng 1 giải CLASS_4', 54, 'CLASS_4', 'CLASS_4', 1, NOW()),
(UUID(), @h_horse2_id, @t4h_race_id, @res2_id, 50, 2, 'Hạng 2 giải CLASS_4', 52, 'CLASS_4', 'CLASS_4', 1, NOW()),
(UUID(), @h_horse3_id, @t4h_race_id, @res3_id, 46, 1, 'Hạng 3 giải CLASS_4', 47, 'CLASS_4', 'CLASS_4', 1, NOW()),
(UUID(), @h_horse4_id, @t4h_race_id, @res4_id, 49, 0, 'Hạng 4 giải CLASS_4', 49, 'CLASS_4', 'CLASS_4', 1, NOW());

-- Chuyển trạng thái hợp đồng & Escrow của Jockey9 (Luồng 05 & 11) sang RELEASED
UPDATE jockey_horse_contracts
SET final_payout_status = 'RELEASED', escrow_status = 'RELEASED', final_payout_at = NOW()
WHERE contract_id = @c_contract4_id;

-- Ghi nhận lịch sử giao dịch ví (Wallet Transactions) cho Trả thưởng & Giải phóng Escrow
-- 1. Giải thưởng Hạng 1 cho Owner1 (8,000,000 VND)
INSERT INTO wallet_transactions (transaction_id, wallet_id, race_result_id, type, direction, amount, balance_before, balance_after, counterparty_wallet_id, counterparty_type, status, note, performed_by_user_id, created_at) VALUES
(UUID(), @w_owner1_id, @res1_id, 'PRIZE_OWNER_SHARE', 'CREDIT', 8000000.00, 150000000.00, 158000000.00, @w_sys_prize, 'SYSTEM', 'SUCCESS', 'Tiền thưởng Hạng 1 giải DEMO 4H (80%)', @u_admin_id, NOW());

-- 2. Giải thưởng Hạng 1 cho Jockey1 (2,000,000 VND)
INSERT INTO wallet_transactions (transaction_id, wallet_id, race_result_id, type, direction, amount, balance_before, balance_after, counterparty_wallet_id, counterparty_type, status, note, performed_by_user_id, created_at) VALUES
(UUID(), @w_jockey1_id, @res1_id, 'PRIZE_JOCKEY_SHARE', 'CREDIT', 2000000.00, 20000000.00, 22000000.00, @w_sys_prize, 'SYSTEM', 'SUCCESS', 'Tiền thưởng Hạng 1 giải DEMO 4H (20%)', @u_admin_id, NOW());

-- 3. Giải phóng 70% Phí thuê Kỵ sĩ (3,500,000 VND) từ Ví Escrow về Ví Jockey9
INSERT INTO wallet_transactions (transaction_id, wallet_id, contract_id, type, direction, amount, balance_before, balance_after, counterparty_wallet_id, counterparty_type, status, note, performed_by_user_id, created_at) VALUES
(UUID(), @w_jockey9_id, NULL, @c_contract4_id, 'JOCKEY_HIRING_FINAL_INCOME', 'CREDIT', 3500000.00, 25000000.00, 28500000.00, @w_sys_escrow, 'SYSTEM', 'SUCCESS', 'Nhận 70% phí thuê kỵ sĩ còn lại sau giải đấu', @u_admin_id, NOW());

-- Cập nhật số dư Ví thực tế
UPDATE wallets SET balance = balance + 8000000.00 WHERE wallet_id = @w_owner1_id;
UPDATE wallets SET balance = balance + 2000000.00 WHERE wallet_id = @w_jockey1_id;
UPDATE wallets SET balance = balance + 3500000.00 WHERE wallet_id = @w_jockey9_id;
UPDATE wallets SET balance = balance - 10000000.00 WHERE wallet_id = @w_sys_prize;
UPDATE wallets SET balance = balance - 3500000.00 WHERE wallet_id = @w_sys_escrow;

-- Bật lại Foreign Keys
SET FOREIGN_KEY_CHECKS = @OLD_FOREIGN_KEY_CHECKS;
SET SQL_SAFE_UPDATES = @OLD_SQL_SAFE_UPDATES;

-- --------------------------------------------------------------------------
-- 12. BÁO CÁO KẾT QUẢ SEED DEMO
-- --------------------------------------------------------------------------

SELECT '=== GIẢI ĐẤU DEMO 4 NGỰA ===' AS info;
SELECT tournament_id, name, phase, status, total_prize_pool FROM tournaments WHERE tournament_id = @t4h_tournament_id;

SELECT '=== DANH SÁCH 4 NGỰA VÀ HỢP ĐỒNG ===' AS info;
SELECT h.name AS horse, j.user_id, u.full_name AS jockey, c.status AS contract_status, c.escrow_status
FROM jockey_horse_contracts c
JOIN horses h ON h.horse_id = c.horse_id
JOIN jockeys j ON j.jockey_id = c.jockey_id
JOIN users u ON u.user_id = j.user_id
WHERE c.tournament_id = @t4h_tournament_id;

SELECT '=== KẾT QUẢ ĐUA & THƯỞNG ===' AS info;
SELECT re.lane_number, h.name AS horse, r.finish_position, r.finish_time, r.prize_money, r.owner_prize_amount, r.jockey_prize_amount
FROM race_results r
JOIN race_entries re ON re.entry_id = r.entry_id
JOIN jockey_horse_contracts c ON c.contract_id = re.contract_id
JOIN horses h ON h.horse_id = c.horse_id
WHERE r.race_id = @t4h_race_id
ORDER BY r.finish_position;

SELECT '=== VI PHẠM & KHIẾU NẠI ===' AS info;
SELECT v.type AS violation_type, v.penalty_type, a.description AS appeal_reason, a.status AS appeal_status, a.resolution
FROM appeals a
JOIN violations v ON v.violation_id = a.related_violation_id;

-- ============================================================================
-- HẾT SCRIPT
-- ============================================================================

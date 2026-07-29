-- ============================================================================
-- HRTMS TEST DATA - XUYÊN SUỐT LUỒNG 07 ĐẾN LUỒNG 11 TRÊN 1 GIẢI ĐẤU DUY NHẤT
-- ============================================================================
-- Mục đích: Nạp bộ dữ liệu thử nghiệm độc lập trên 1 GIẢI ĐẤU DUY NHẤT để test:
--   Luồng 07: Inspection (Kiểm tra Ngựa & Kỵ sĩ, phân công khám)
--   Luồng 08: Vận hành Race (Start race, nhập kết quả, lập & ký Race Report)
--   Luồng 09: Vi phạm & Khiếu nại (Ghi nhận vi phạm, duyệt Appeal)
--   Luồng 10: Dự đoán của khán giả (Mở card dự đoán, chấm điểm sau publish)
--   Luồng 11: Thanh toán giải thưởng Top 3 & Tự động release 70% tiền thuê Kỵ sĩ
--
-- Phân định vai trò Trọng tài (Nghiệp vụ chuẩn):
--   - Head Referee (Trọng tài Trưởng):  `referee_t99`    -> Duyệt Khiếu nại (Appeals)
--   - Race Referee (Trọng tài Cuộc đua): `referee_race99` -> Vận hành trận đua (Start, Results, Report)
-- Mật khẩu tài khoản demo: admin123
-- ============================================================================

USE SWP391_Project_HRTMS;
SET NAMES utf8mb4;

SET @now = NOW();
SET @demo_password = '$2a$12$AsBCvrsZ2yvqd7RyEflkfOGwfTewt8CSx40CKh0ZIZuD4WZ49wo4a';

-- Role IDs
SET @role_admin  = (SELECT role_id FROM roles WHERE role_name = 'ADMIN' LIMIT 1);
SET @role_owner  = (SELECT role_id FROM roles WHERE role_name = 'HORSE_OWNER' LIMIT 1);
SET @role_jockey = (SELECT role_id FROM roles WHERE role_name = 'JOCKEY' LIMIT 1);
SET @role_ref    = (SELECT role_id FROM roles WHERE role_name = 'REFEREE' LIMIT 1);
SET @role_vet    = (SELECT role_id FROM roles WHERE role_name = 'VETERINARIAN' LIMIT 1);
SET @role_med    = (SELECT role_id FROM roles WHERE role_name = 'MEDICAL_STAFF' LIMIT 1);
SET @role_spec   = (SELECT role_id FROM roles WHERE role_name = 'SPECTATOR' LIMIT 1);

-- UUIDs cố định cho User & Profile
SET @admin_user_id        = '10000000-0000-0000-0000-000000000001';
SET @owner_user_id        = '10000000-0000-0000-0000-000000000002';
SET @head_referee_user_id = '10000000-0000-0000-0000-000000000005';
SET @race_referee_user_id = '10000000-0000-0000-0000-000000000055';
SET @vet_user_id          = '10000000-0000-0000-0000-000000000006';
SET @med_user_id          = '10000000-0000-0000-0000-000000000007';
SET @spectator_user_id    = '10000000-0000-0000-0000-000000000008';

SET @owner_id         = '20000000-0000-0000-0000-000000000001';
SET @head_referee_id  = '20000000-0000-0000-0000-000000000004';
SET @race_referee_id  = '20000000-0000-0000-0000-000000000055';
SET @vet_id           = '20000000-0000-0000-0000-000000000005';
SET @med_staff_id     = '20000000-0000-0000-0000-000000000006';
SET @spectator_id     = '20000000-0000-0000-0000-000000000002';

SET @tournament_id = '50000000-0000-0000-0000-000000000099';
SET @round_id      = '80000000-0000-0000-0000-000000000099';
SET @race_id       = '90000000-0000-0000-0000-000000000099';

-- ----------------------------------------------------------------------------
-- 1. NẠP TÀI KHOẢN USERS & PROFILES (TÁCH BIỆT TRỌNG TÀI TRƯỞNG & TRỌNG TÀI ĐUA)
-- ----------------------------------------------------------------------------
INSERT INTO users (user_id, username, password, email, dob, gender, full_name, status, created_at, role_id)
VALUES
    (@admin_user_id, 'admin_t99', @demo_password, 'admin99@hrtms.local', '1990-01-01', 'MALE', 'Admin Test 99', 'ACTIVE', @now, @role_admin),
    (@owner_user_id, 'owner_t99', @demo_password, 'owner99@hrtms.local', '1992-02-02', 'MALE', 'Chủ ngựa Test 99', 'ACTIVE', @now, @role_owner),
    (@head_referee_user_id, 'referee_t99', @demo_password, 'ref99@hrtms.local', '1988-03-03', 'MALE', 'Trọng tài Trưởng (Head Ref)', 'ACTIVE', @now, @role_ref),
    (@race_referee_user_id, 'referee_race99', @demo_password, 'refrace99@hrtms.local', '1989-03-03', 'MALE', 'Trọng tài Cuộc đua (Race Ref)', 'ACTIVE', @now, @role_ref),
    (@vet_user_id, 'vet_t99', @demo_password, 'vet99@hrtms.local', '1987-04-04', 'FEMALE', 'Thú y Test 99', 'ACTIVE', @now, @role_vet),
    (@med_user_id, 'med_t99', @demo_password, 'med99@hrtms.local', '1989-05-05', 'MALE', 'Y tế Test 99', 'ACTIVE', @now, @role_med),
    (@spectator_user_id, 'spec_t99', @demo_password, 'spec99@hrtms.local', '1995-06-06', 'MALE', 'Khán giả Test 99', 'ACTIVE', @now, @role_spec)
ON DUPLICATE KEY UPDATE status = 'ACTIVE';

INSERT INTO horse_owners (owner_id, user_id, farm_name, address, created_at)
VALUES (@owner_id, @owner_user_id, 'Trang Trại Test 99', 'TP.HCM', @now)
ON DUPLICATE KEY UPDATE farm_name = VALUES(farm_name);

-- Hồ sơ 2 Trọng tài riêng biệt
INSERT INTO referees (referee_id, user_id, certification_level, years_of_service, status, created_at)
VALUES
    (@head_referee_id, @head_referee_user_id, 'HEAD_REFEREE', 10, 'AVAILABLE', @now),
    (@race_referee_id, @race_referee_user_id, 'RACE_REFEREE', 7, 'AVAILABLE', @now)
ON DUPLICATE KEY UPDATE status = 'AVAILABLE';

INSERT INTO veterinarians (vet_id, user_id, specialization, years_of_service, status, created_at)
VALUES (@vet_id, @vet_user_id, 'Equine Medicine', 8, 'AVAILABLE', @now)
ON DUPLICATE KEY UPDATE status = 'AVAILABLE';

INSERT INTO medical_staffs (med_staff_id, user_id, certification, years_of_service, status, created_at)
VALUES (@med_staff_id, @med_user_id, 'Sports Medicine', 7, 'AVAILABLE', @now)
ON DUPLICATE KEY UPDATE status = 'AVAILABLE';

INSERT INTO spectators (spectator_id, user_id, total_points, created_at)
VALUES (@spectator_id, @spectator_user_id, 100, @now)
ON DUPLICATE KEY UPDATE total_points = 100;

-- Nạp 4 Kỵ sĩ & 4 Ngựa
INSERT INTO users (user_id, username, password, email, dob, gender, full_name, status, created_at, role_id)
VALUES
    ('10000000-0000-0000-0000-000000000191', 'jockey_t91', @demo_password, 'jock91@hrtms.local', '1994-01-01', 'MALE', 'Kỵ sĩ Test 91', 'ACTIVE', @now, @role_jockey),
    ('10000000-0000-0000-0000-000000000192', 'jockey_t92', @demo_password, 'jock92@hrtms.local', '1995-02-02', 'MALE', 'Kỵ sĩ Test 92', 'ACTIVE', @now, @role_jockey),
    ('10000000-0000-0000-0000-000000000193', 'jockey_t93', @demo_password, 'jock93@hrtms.local', '1996-03-03', 'MALE', 'Kỵ sĩ Test 93', 'ACTIVE', @now, @role_jockey),
    ('10000000-0000-0000-0000-000000000194', 'jockey_t94', @demo_password, 'jock94@hrtms.local', '1997-04-04', 'MALE', 'Kỵ sĩ Test 94', 'ACTIVE', @now, @role_jockey)
ON DUPLICATE KEY UPDATE status = 'ACTIVE';

INSERT INTO jockeys (jockey_id, user_id, height, weight, experience_years, specialization, status, total_races, total_wins, jockey_tier, created_at)
VALUES
    ('21000000-0000-0000-0000-000000000191', '10000000-0000-0000-0000-000000000191', 1.60, 50, 5, 'MILE', 'AVAILABLE', 20, 5, 'PROFESSIONAL', @now),
    ('21000000-0000-0000-0000-000000000192', '10000000-0000-0000-0000-000000000192', 1.62, 52, 6, 'MILE', 'AVAILABLE', 25, 7, 'PROFESSIONAL', @now),
    ('21000000-0000-0000-0000-000000000193', '10000000-0000-0000-0000-000000000193', 1.58, 49, 4, 'MILE', 'AVAILABLE', 15, 3, 'JUNIOR', @now),
    ('21000000-0000-0000-0000-000000000194', '10000000-0000-0000-0000-000000000194', 1.61, 51, 3, 'MILE', 'AVAILABLE', 12, 2, 'JUNIOR', @now)
ON DUPLICATE KEY UPDATE status = 'AVAILABLE';

INSERT INTO horses (horse_id, name, breed, age, color, gender, health_status, race_class, current_rating, highest_rating, total_races, total_places, total_wins, weight, win_rate, owner_id, created_at)
VALUES
    ('40000000-0000-0000-0000-000000000091', 'Ngựa Thần Mã 91', 'THOROUGHBRED', 4, 'BAY', 'MALE', 'HEALTHY', 'CLASS_1', 85, 100, 10, 3, 4, 450, 0.4, @owner_id, @now),
    ('40000000-0000-0000-0000-000000000092', 'Ngựa Xích Thố 92', 'THOROUGHBRED', 5, 'CHESTNUT', 'MALE', 'HEALTHY', 'CLASS_1', 88, 105, 12, 4, 5, 460, 0.41, @owner_id, @now),
    ('40000000-0000-0000-0000-000000000093', 'Ngựa Truy Phong 93', 'THOROUGHBRED', 3, 'BLACK', 'MALE', 'HEALTHY', 'CLASS_1', 80, 95, 8, 2, 3, 440, 0.37, @owner_id, @now),
    ('40000000-0000-0000-0000-000000000094', 'Ngựa Bạch Long 94', 'THOROUGHBRED', 4, 'GREY', 'MALE', 'HEALTHY', 'CLASS_1', 82, 98, 9, 3, 2, 445, 0.22, @owner_id, @now)
ON DUPLICATE KEY UPDATE current_rating = VALUES(current_rating);

-- ----------------------------------------------------------------------------
-- 2. BẢNG GIẢI ĐẤU (TOURNAMENT) & CẤU HÌNH RATING POLICY
-- ----------------------------------------------------------------------------
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
     max_entries_per_race, min_entries_per_race, qualifiers_per_race,
     rating_first_min, rating_first_max,
     rating_second_min, rating_second_max,
     rating_third_min, rating_third_max,
     rating_fourth_fifth_min, rating_fourth_fifth_max,
     rating_other_min, rating_other_max,
     rating_disqualified_min, rating_disqualified_max,
     rating_policy_version, rating_policy_locked_at,
     created_by)
VALUES
    (@tournament_id,
     'GIẢI ĐẤU TEST LUỒNG 07 - 11 (SINGLE TOURNAMENT)',
     'Giải đấu thử nghiệm xuyên suốt từ Inspection, Race Operations, Vi phạm, Dự đoán đến Payout 70%.',
     CURRENT_DATE, DATE_ADD(CURRENT_DATE, INTERVAL 7 DAY), NULL,
     'Trường đua Trung Tâm Test', 100000, 50000, 100000000.00,
     'THOROUGHBRED', 2, 12, 100, 30, 10, 50, 120, 5, 24,
     90, 30, 30, 0, 30, 10, '08:00:00', '18:00:00',
     0, NULL, NULL,
     'ONGOING', 'RACING', @now, @now,
     DATE_SUB(@now, INTERVAL 10 DAY), DATE_SUB(@now, INTERVAL 8 DAY),
     DATE_SUB(@now, INTERVAL 6 DAY), DATE_SUB(@now, INTERVAL 4 DAY),
     DATE_SUB(@now, INTERVAL 2 DAY), @now,
     'Chung Kết', 'CLASS_1', 'MILE_1600M',
     135, 115, 1.5, 1, 4, 4, 4,
     16, 8, 4,
     6, 12, 2, 5, 1, 4, 0, 2, -8, 0, -8, 0,
     1, @now, @admin_user_id)
ON DUPLICATE KEY UPDATE
    name = VALUES(name),
    status = 'ONGOING',
    phase = 'RACING';

-- ----------------------------------------------------------------------------
-- 3. CƠ CẤU GIẢI THƯỞNG (PRIZE STRUCTURES FOR TOURNAMENT)
-- ----------------------------------------------------------------------------
DELETE FROM prize_structures WHERE tournament_id = @tournament_id;

INSERT INTO prize_structures (prize_structure_id, tournament_id, prize_rank, percentage, fixed_amount, is_active)
VALUES
    ('e1000000-0000-0000-0000-000000000091', @tournament_id, 1, 50.00, 50000000.00, 1),
    ('e1000000-0000-0000-0000-000000000092', @tournament_id, 2, 30.00, 30000000.00, 1),
    ('e1000000-0000-0000-0000-000000000093', @tournament_id, 3, 20.00, 20000000.00, 1);

-- ----------------------------------------------------------------------------
-- 4. VÒNG ĐẤU (ROUND) & CUỘC ĐƯA (RACE) CHUNG KẾT
-- ----------------------------------------------------------------------------
-- Gán Trọng tài Trưởng (Head Referee = @head_referee_id -> referee_t99)
INSERT INTO rounds
    (round_id, tournament_id, round_name, sequence_order, max_races, min_entries, max_entries,
     qualifiers_per_race, status, transition_status, is_final, advancement_rule, description, prediction_type,
     created_by, head_referee_id, head_referee_assigned_at, created_at)
VALUES
    (@round_id, @tournament_id, 'Chung Kết Luồng Test', 1, 1, 4, 16,
     0, 'SCHEDULED', 'READY', 1, 'TOP_4_PER_RACE', 'Vòng chung kết thử nghiệm', 'TOP3',
     @admin_user_id, @head_referee_id, @now, @now)
ON DUPLICATE KEY UPDATE
    status = 'SCHEDULED',
    head_referee_id = @head_referee_id,
    is_final = 1;

-- Cuộc đua với schedule_published_at = @now
INSERT INTO races
    (race_id, round_id, name, sequence_order, distance, track_condition, start_time, end_time, status, schedule_published_at, created_by)
VALUES
    (@race_id, @round_id, 'Race 1 - Chung kết Luồng Test', 1, 'MILE_1600M', 'GOOD',
     DATE_ADD(@now, INTERVAL 5 MINUTE), DATE_ADD(@now, INTERVAL 35 MINUTE),
     'SCHEDULED', @now, @admin_user_id)
ON DUPLICATE KEY UPDATE
    status = 'SCHEDULED',
    schedule_published_at = NOW(),
    start_time = DATE_ADD(NOW(), INTERVAL 5 MINUTE),
    end_time = DATE_ADD(NOW(), INTERVAL 35 MINUTE);

-- Gán Trọng tài Cuộc đua (Race Referee = @race_referee_id -> referee_race99)
DELETE FROM race_referees WHERE race_id = @race_id;

INSERT INTO race_referees
    (race_referee_id, race_id, referee_id, assigned_by, assigned_at)
VALUES
    ('91000000-0000-0000-0000-000000000099', @race_id, @race_referee_id, @admin_user_id, @now);

-- ----------------------------------------------------------------------------
-- 5. HỒ SƠ ĐĂNG KÝ & HỢP ĐỒNG (REGISTRATIONS & CONTRACTS)
-- ----------------------------------------------------------------------------
INSERT INTO horse_tournament_registrations (horse_tournament_reg_id, tournament_id, horse_id, owner_id, status, rating_at_registration, race_class_at_registration, submitted_at)
VALUES
    ('61000000-0000-0000-0000-000000000091', @tournament_id, '40000000-0000-0000-0000-000000000091', @owner_id, 'APPROVED', 85, 'CLASS_1', @now),
    ('61000000-0000-0000-0000-000000000092', @tournament_id, '40000000-0000-0000-0000-000000000092', @owner_id, 'APPROVED', 88, 'CLASS_1', @now),
    ('61000000-0000-0000-0000-000000000093', @tournament_id, '40000000-0000-0000-0000-000000000093', @owner_id, 'APPROVED', 80, 'CLASS_1', @now),
    ('61000000-0000-0000-0000-000000000094', @tournament_id, '40000000-0000-0000-0000-000000000094', @owner_id, 'APPROVED', 82, 'CLASS_1', @now)
ON DUPLICATE KEY UPDATE status = 'APPROVED';

INSERT INTO jockey_tournament_registrations (jockey_tournament_reg_id, tournament_id, jockey_id, status, hire_fee, submitted_at)
VALUES
    ('62000000-0000-0000-0000-000000000091', @tournament_id, '21000000-0000-0000-0000-000000000191', 'APPROVED', 1000000.00, @now),
    ('62000000-0000-0000-0000-000000000092', @tournament_id, '21000000-0000-0000-0000-000000000192', 'APPROVED', 1000000.00, @now),
    ('62000000-0000-0000-0000-000000000093', @tournament_id, '21000000-0000-0000-0000-000000000193', 'APPROVED', 1000000.00, @now),
    ('62000000-0000-0000-0000-000000000094', @tournament_id, '21000000-0000-0000-0000-000000000194', 'APPROVED', 1000000.00, @now)
ON DUPLICATE KEY UPDATE status = 'APPROVED';

-- Hợp đồng Thuê Kỵ sĩ (4 Hợp đồng ở trạng thái APPROVED + PARTIALLY_RELEASED để test Luồng 11)
INSERT INTO jockey_horse_contracts
    (contract_id, owner_id, jockey_id, horse_id, tournament_id,
     horse_tournament_reg_id, jockey_tournament_reg_id,
     hire_fee, system_contract_fee, owner_prize_share_percent, jockey_prize_share_percent,
     advance_percent, final_percent, advance_paid_amount, escrow_amount,
     contract_note, status, payment_status, escrow_status, advance_payout_status, final_payout_status,
     requested_at, submitted_at)
VALUES
    ('70000000-0000-0000-0000-000000000091', @owner_id, '21000000-0000-0000-0000-000000000191', '40000000-0000-0000-0000-000000000091', @tournament_id,
     '61000000-0000-0000-0000-000000000091', '62000000-0000-0000-0000-000000000091', 1000000.00, 200000.00, 80.00, 20.00, 30.00, 70.00, 300000.00, 700000.00,
     'Hợp đồng test 1', 'APPROVED', 'PAID', 'PARTIALLY_RELEASED', 'PAID', 'NOT_RELEASED', @now, @now),

    ('70000000-0000-0000-0000-000000000092', @owner_id, '21000000-0000-0000-0000-000000000192', '40000000-0000-0000-0000-000000000092', @tournament_id,
     '61000000-0000-0000-0000-000000000092', '62000000-0000-0000-0000-000000000092', 1000000.00, 200000.00, 80.00, 20.00, 30.00, 70.00, 300000.00, 700000.00,
     'Hợp đồng test 2', 'APPROVED', 'PAID', 'PARTIALLY_RELEASED', 'PAID', 'NOT_RELEASED', @now, @now),

    ('70000000-0000-0000-0000-000000000093', @owner_id, '21000000-0000-0000-0000-000000000193', '40000000-0000-0000-0000-000000000093', @tournament_id,
     '61000000-0000-0000-0000-000000000093', '62000000-0000-0000-0000-000000000093', 1000000.00, 200000.00, 80.00, 20.00, 30.00, 70.00, 300000.00, 700000.00,
     'Hợp đồng test 3', 'APPROVED', 'PAID', 'PARTIALLY_RELEASED', 'PAID', 'NOT_RELEASED', @now, @now),

    ('70000000-0000-0000-0000-000000000094', @owner_id, '21000000-0000-0000-0000-000000000194', '40000000-0000-0000-0000-000000000094', @tournament_id,
     '61000000-0000-0000-0000-000000000094', '62000000-0000-0000-0000-000000000094', 1000000.00, 200000.00, 80.00, 20.00, 30.00, 70.00, 300000.00, 700000.00,
     'Hợp đồng test 4', 'APPROVED', 'PAID', 'PARTIALLY_RELEASED', 'PAID', 'NOT_RELEASED', @now, @now)
ON DUPLICATE KEY UPDATE status = 'APPROVED';

-- Insert Race Entries
INSERT INTO race_entries
    (entry_id, race_id, contract_id, lane_number, status, assigned_by, assigned_at, created_at)
VALUES
    ('a0000000-0000-0000-0000-000000000091', @race_id, '70000000-0000-0000-0000-000000000091', 1, 'CONFIRMED', @admin_user_id, @now, @now),
    ('a0000000-0000-0000-0000-000000000092', @race_id, '70000000-0000-0000-0000-000000000092', 2, 'CONFIRMED', @admin_user_id, @now, @now),
    ('a0000000-0000-0000-0000-000000000093', @race_id, '70000000-0000-0000-0000-000000000093', 3, 'CONFIRMED', @admin_user_id, @now, @now),
    ('a0000000-0000-0000-0000-000000000094', @race_id, '70000000-0000-0000-0000-000000000094', 4, 'CONFIRMED', @admin_user_id, @now, @now)
ON DUPLICATE KEY UPDATE
    status = 'CONFIRMED',
    lane_number = VALUES(lane_number);

-- ----------------------------------------------------------------------------
-- 6. PHÂN CÔNG NHÂN SỰ KHÁM & PHIẾU KHÁM (INSPECTION - LUỒNG 07)
-- ----------------------------------------------------------------------------
DELETE FROM race_inspection_staff_assignments WHERE race_id = @race_id;

INSERT INTO race_inspection_staff_assignments
    (assignment_id, race_id, vet_id, med_staff_id, assigned_by, assigned_at)
VALUES
    ('b0000000-0000-0000-0000-000000000099', @race_id, @vet_id, @med_staff_id, @admin_user_id, @now);

-- Phiếu khám Ngựa PASS
INSERT INTO horse_inspections
    (horse_inspection_id, entry_id, vet_id, status, result, note,
     registered_breed, actual_breed, registered_weight, actual_weight, doping_detected,
     handicap_weight, is_handicap_confirmed, inspected_at, confirmed_at)
VALUES
    ('c1000000-0000-0000-0000-000000000091', 'a0000000-0000-0000-0000-000000000091', @vet_id, 'CONFIRMED', 'PASS', 'Ngựa sức khỏe tốt', 'THOROUGHBRED', 'THOROUGHBRED', 450.0, 450.0, 0, 120.0, 1, @now, @now),
    ('c1000000-0000-0000-0000-000000000092', 'a0000000-0000-0000-0000-000000000092', @vet_id, 'CONFIRMED', 'PASS', 'Ngựa sức khỏe tốt', 'THOROUGHBRED', 'THOROUGHBRED', 460.0, 460.0, 0, 120.0, 1, @now, @now),
    ('c1000000-0000-0000-0000-000000000093', 'a0000000-0000-0000-0000-000000000093', @vet_id, 'CONFIRMED', 'PASS', 'Ngựa sức khỏe tốt', 'THOROUGHBRED', 'THOROUGHBRED', 440.0, 440.0, 0, 120.0, 1, @now, @now),
    ('c1000000-0000-0000-0000-000000000094', 'a0000000-0000-0000-0000-000000000094', @vet_id, 'CONFIRMED', 'PASS', 'Ngựa sức khỏe tốt', 'THOROUGHBRED', 'THOROUGHBRED', 445.0, 445.0, 0, 120.0, 1, @now, @now)
ON DUPLICATE KEY UPDATE result = 'PASS', status = 'CONFIRMED';

-- Phiếu khám Kỵ sĩ PASS
INSERT INTO jockey_inspections
    (jockey_inspection_id, entry_id, med_staff_id, status, result, note,
     registered_weight, actual_weight, doping_detected, inspected_at)
VALUES
    ('c2000000-0000-0000-0000-000000000091', 'a0000000-0000-0000-0000-000000000091', @med_staff_id, 'CONFIRMED', 'PASS', 'Kỵ sĩ đủ điều kiện', 50.0, 50.0, 0, @now),
    ('c2000000-0000-0000-0000-000000000092', 'a0000000-0000-0000-0000-000000000092', @med_staff_id, 'CONFIRMED', 'PASS', 'Kỵ sĩ đủ điều kiện', 52.0, 52.0, 0, @now),
    ('c2000000-0000-0000-0000-000000000093', 'a0000000-0000-0000-0000-000000000093', @med_staff_id, 'CONFIRMED', 'PASS', 'Kỵ sĩ đủ điều kiện', 49.0, 49.0, 0, @now),
    ('c2000000-0000-0000-0000-000000000094', 'a0000000-0000-0000-0000-000000000094', @med_staff_id, 'CONFIRMED', 'PASS', 'Kỵ sĩ đủ điều kiện', 51.0, 51.0, 0, @now)
ON DUPLICATE KEY UPDATE result = 'PASS', status = 'CONFIRMED';

-- ----------------------------------------------------------------------------
-- 7. KHIẾU NẠI & VI PHẠM (VIOLATION & APPEAL - LUỒNG 09)
-- ----------------------------------------------------------------------------
INSERT INTO appeal_categories (category_id, code, name, description, is_active, created_at)
VALUES ('f0000000-0000-0000-0000-000000000001', 'OBSTRUCTION', 'Cản trở đường đua', 'Khiếu nại về hành vi cản trở lane trong quá trình đua', 1, @now)
ON DUPLICATE KEY UPDATE name = VALUES(name);

INSERT INTO appeals
    (appeal_id, entry_id, category_id, description, status, submitted_by_user_id, submitted_at)
VALUES
    ('f1000000-0000-0000-0000-000000000099',
     'a0000000-0000-0000-0000-000000000092',
     'f0000000-0000-0000-0000-000000000001',
     'Đề nghị xem lại tình huống chèn ép ở góc cua thứ hai.',
     'Pending',
     @owner_user_id,
     @now)
ON DUPLICATE KEY UPDATE status = 'Pending';

-- ----------------------------------------------------------------------------
-- 8. DỰ ĐOÁN CỦA KHÁN GIẢ (SPECTATOR PREDICTION - LUỒNG 10)
-- ----------------------------------------------------------------------------
INSERT INTO predictions
    (prediction_id, spectator_id, race_id, status, prediction_type, prediction_time, reward_points)
VALUES
    ('p1000000-0000-0000-0000-000000000099',
     @spectator_id,
     @race_id,
     'PENDING',
     'TOP3',
     @now,
     0)
ON DUPLICATE KEY UPDATE status = 'PENDING';

DELETE FROM prediction_detail WHERE prediction_id = 'p1000000-0000-0000-0000-000000000099';

INSERT INTO prediction_detail (prediction_detail_id, prediction_id, entry_id, predicted_rank, status)
VALUES
    ('p2000000-0000-0000-0000-000000000091', 'p1000000-0000-0000-0000-000000000099', 'a0000000-0000-0000-0000-000000000091', 1, 'UNSCORED'),
    ('p2000000-0000-0000-0000-000000000092', 'p1000000-0000-0000-0000-000000000099', 'a0000000-0000-0000-0000-000000000092', 2, 'UNSCORED'),
    ('p2000000-0000-0000-0000-000000000093', 'p1000000-0000-0000-0000-000000000099', 'a0000000-0000-0000-0000-000000000093', 3, 'UNSCORED');

-- ----------------------------------------------------------------------------
-- 9. VÍ HỆ THỐNG & VÍ NGƯỜI DÙNG (WALLETS FOR PAYOUT - LUỒNG 11)
-- ----------------------------------------------------------------------------
INSERT INTO wallets
    (wallet_id, owner_type, balance, currency, status, created_at, updated_at, wallet_purpose, user_id)
VALUES
    ('30000000-0000-0000-0000-000000000099', 'SYSTEM', 100000000.00, 'VND', 'ACTIVE', @now, @now, 'SYSTEM_PRIZE_POOL', NULL),
    ('30000000-0000-0000-0000-000000000098', 'SYSTEM', 20000000.00, 'VND', 'ACTIVE', @now, @now, 'SYSTEM_ESCROW', NULL),
    ('30000000-0000-0000-0000-000000000091', 'USER', 10000000.00, 'VND', 'ACTIVE', @now, @now, 'USER_MAIN', @owner_user_id),
    ('30000000-0000-0000-0000-000000000191', 'USER', 1000000.00, 'VND', 'ACTIVE', @now, @now, 'USER_MAIN', '10000000-0000-0000-0000-000000000191'),
    ('30000000-0000-0000-0000-000000000192', 'USER', 1000000.00, 'VND', 'ACTIVE', @now, @now, 'USER_MAIN', '10000000-0000-0000-0000-000000000192'),
    ('30000000-0000-0000-0000-000000000193', 'USER', 1000000.00, 'VND', 'ACTIVE', @now, @now, 'USER_MAIN', '10000000-0000-0000-0000-000000000193'),
    ('30000000-0000-0000-0000-000000000194', 'USER', 1000000.00, 'VND', 'ACTIVE', @now, @now, 'USER_MAIN', '10000000-0000-0000-0000-000000000194')
ON DUPLICATE KEY UPDATE
    balance = VALUES(balance),
    status = 'ACTIVE';

-- ============================================================================
-- HOÀN TẤT NẠP DỮ LIỆU MẪU CHO GIẢI ĐẤU TEST LUỒNG 07 - 11
-- ============================================================================

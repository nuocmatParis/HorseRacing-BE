USE SWP391_Project_HRTMS;

-- Vô hiệu hóa kiểm tra khóa ngoại tạm thời để tránh lỗi ràng buộc khi làm sạch/chèn dữ liệu
SET @OLD_FOREIGN_KEY_CHECKS = @@FOREIGN_KEY_CHECKS;
SET @OLD_SQL_SAFE_UPDATES = @@SQL_SAFE_UPDATES;
SET FOREIGN_KEY_CHECKS = 0;
SET SQL_SAFE_UPDATES = 0;

-- 1. Xóa dữ liệu cũ theo thứ tự phụ thuộc.
-- Không dùng TRUNCATE cho các bảng có foreign key vì MySQL có thể trả lỗi 1701
-- ngay cả khi FOREIGN_KEY_CHECKS đã tắt.
DELETE FROM notification_deliveries;
DELETE FROM notifications;
DELETE FROM notification_events;
DELETE FROM notification_preferences;

DELETE FROM appeal_evidences;
DELETE FROM appeals;
DELETE FROM violations;

DELETE FROM payment_transactions;
DELETE FROM wallet_transactions;
DELETE FROM invoices;

DELETE FROM horse_rating_histories;
DELETE FROM prediction_detail;
DELETE FROM predictions;
DELETE FROM ai_predictions;
DELETE FROM horse_inspections;
DELETE FROM jockey_inspections;
DELETE FROM race_reports;
DELETE FROM race_results;
DELETE FROM race_referees;
DELETE FROM race_inspection_staff_assignments;
DELETE FROM race_entries;
DELETE FROM races;
DELETE FROM rounds;

DELETE FROM jockey_horse_contracts;
DELETE FROM horse_tournament_registrations;
DELETE FROM jockey_tournament_registrations;
DELETE FROM tournament_eligibility;
DELETE FROM prize_structures;
DELETE FROM tournaments;
DELETE FROM horses;

DELETE FROM wallets;
DELETE FROM horse_owners;
DELETE FROM jockeys;
DELETE FROM spectators;
DELETE FROM referees;
DELETE FROM veterinarians;
DELETE FROM medical_staffs;
DELETE FROM users;

-- Cấu hình mặc định cho các cột trong bảng jockeys để tránh lỗi insert khi có trigger tự động tạo profile
ALTER TABLE jockeys MODIFY COLUMN total_races INT NOT NULL DEFAULT 0;
ALTER TABLE jockeys MODIFY COLUMN total_wins INT NOT NULL DEFAULT 0;
ALTER TABLE jockeys MODIFY COLUMN experience_years INT NOT NULL DEFAULT 0;

-- Cấu hình mặc định cho các cột trong bảng horses
ALTER TABLE horses MODIFY COLUMN current_rating INT NOT NULL DEFAULT 0;
ALTER TABLE horses MODIFY COLUMN highest_rating INT NOT NULL DEFAULT 0;
ALTER TABLE horses MODIFY COLUMN total_races INT NOT NULL DEFAULT 0;
ALTER TABLE horses MODIFY COLUMN total_wins INT NOT NULL DEFAULT 0;
ALTER TABLE horses MODIFY COLUMN total_places INT NOT NULL DEFAULT 0;
ALTER TABLE horses MODIFY COLUMN win_rate DOUBLE NOT NULL DEFAULT 0.0;

-- Cấu hình mặc định cho bảng spectators
ALTER TABLE spectators MODIFY COLUMN total_points INT NOT NULL DEFAULT 0;

-- Cấu hình mặc định cho bảng tournaments để tránh lỗi insert khi thiếu trường mặc định của Hibernate
ALTER TABLE tournaments MODIFY COLUMN prediction_top1_correct_points INT NOT NULL DEFAULT 100;
ALTER TABLE tournaments MODIFY COLUMN prediction_top3_exact_position_points INT NOT NULL DEFAULT 30;
ALTER TABLE tournaments MODIFY COLUMN prediction_top3_correct_horse_points INT NOT NULL DEFAULT 10;
ALTER TABLE tournaments MODIFY COLUMN prediction_top3_perfect_bonus_points INT NOT NULL DEFAULT 50;
ALTER TABLE tournaments MODIFY COLUMN prediction_open_minutes_before INT NOT NULL DEFAULT 120;
ALTER TABLE tournaments MODIFY COLUMN prediction_close_minutes_before INT NOT NULL DEFAULT 5;
ALTER TABLE tournaments MODIFY COLUMN prediction_card_open_hours_before_first_race INT NOT NULL DEFAULT 24;
ALTER TABLE tournaments MODIFY COLUMN inspection_open_minutes_before INT NOT NULL DEFAULT 90;
ALTER TABLE tournaments MODIFY COLUMN inspection_close_minutes_before INT NOT NULL DEFAULT 30;
ALTER TABLE tournaments MODIFY COLUMN max_races_per_day INT NOT NULL DEFAULT 9;
ALTER TABLE tournaments MODIFY COLUMN min_race_interval_minutes INT NOT NULL DEFAULT 35;
ALTER TABLE tournaments MODIFY COLUMN start_early_tolerance_minutes INT NOT NULL DEFAULT 0;
ALTER TABLE tournaments MODIFY COLUMN start_late_tolerance_minutes INT NOT NULL DEFAULT 30;
ALTER TABLE tournaments MODIFY COLUMN default_race_operational_minutes INT NOT NULL DEFAULT 30;
ALTER TABLE tournaments MODIFY COLUMN race_day_start_time TIME NOT NULL DEFAULT '08:00:00';
ALTER TABLE tournaments MODIFY COLUMN race_day_end_time TIME NOT NULL DEFAULT '18:00:00';
ALTER TABLE tournaments MODIFY COLUMN apply_break_time BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE tournaments MODIFY COLUMN min_round_gap_days INT NOT NULL DEFAULT 7;
ALTER TABLE tournaments MODIFY COLUMN handicap_enabled BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE tournaments MODIFY COLUMN bracket_plan_status VARCHAR(50) NOT NULL DEFAULT 'NOT_GENERATED';
ALTER TABLE tournaments MODIFY COLUMN bracket_plan_version INT NOT NULL DEFAULT 1;
ALTER TABLE tournaments MODIFY COLUMN created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE tournaments MODIFY COLUMN top_weight_lbs INT NOT NULL DEFAULT 135;
ALTER TABLE tournaments MODIFY COLUMN min_weight_lbs INT NOT NULL DEFAULT 115;
ALTER TABLE tournaments MODIFY COLUMN equipment_weight_kg DOUBLE NOT NULL DEFAULT 1.5;

-- Cấu hình mặc định cho bảng race_entries
ALTER TABLE race_entries MODIFY COLUMN status VARCHAR(50) NOT NULL DEFAULT 'CONFIRMED';

select * from users where username like '%admin%';
select * from tournaments;
-- 2. Đảm bảo các vai trò (roles) đã được chèn với UUID cố định
INSERT INTO roles (role_id, role_name, description, is_active, created_at)
VALUES
    ('00000000-0000-0000-0000-000000000001', 'ADMIN', 'System Administrator', 1, NOW()),
    ('00000000-0000-0000-0000-000000000002', 'HORSE_OWNER', 'Owner of the racing horses', 1, NOW()),
    ('00000000-0000-0000-0000-000000000003', 'JOCKEY', 'Professional horse rider', 1, NOW()),
    ('00000000-0000-0000-0000-000000000004', 'SPECTATOR', 'Audience / Spectator of the race', 1, NOW()),
    ('00000000-0000-0000-0000-000000000005', 'REFEREE', 'Official referee of the race', 1, NOW()),
    ('00000000-0000-0000-0000-000000000006', 'VETERINARIAN', 'Veterinarian checking horse health', 1, NOW()),
    ('00000000-0000-0000-0000-000000000007', 'MEDICAL_STAFF', 'Medical staff checking jockey health', 1, NOW())
ON DUPLICATE KEY UPDATE description = VALUES(description);

-- Lấy role_id thực tế theo role_name. Nếu database đã có role với UUID khác,
-- INSERT ... ON DUPLICATE KEY chỉ cập nhật dòng cũ chứ không tạo UUID cố định trên.
SET @admin_role = (SELECT role_id FROM roles WHERE role_name = 'ADMIN' LIMIT 1);
SET @owner_role = (SELECT role_id FROM roles WHERE role_name = 'HORSE_OWNER' LIMIT 1);
SET @jockey_role = (SELECT role_id FROM roles WHERE role_name = 'JOCKEY' LIMIT 1);
SET @spectator_role = (SELECT role_id FROM roles WHERE role_name = 'SPECTATOR' LIMIT 1);
SET @referee_role = (SELECT role_id FROM roles WHERE role_name = 'REFEREE' LIMIT 1);
SET @vet_role = (SELECT role_id FROM roles WHERE role_name = 'VETERINARIAN' LIMIT 1);
SET @medical_role = (SELECT role_id FROM roles WHERE role_name = 'MEDICAL_STAFF' LIMIT 1);

-- 3. Tạo tài khoản người dùng (Users) - Mật khẩu chung
SET @demo_password = '$2a$12$55CXrg2hHP6dBaN2i9FEcOSA1hADvKDeVOVHGfH55ojDXQ/mWEHyW';

INSERT INTO users (user_id, username, password, email, dob, gender, full_name, phone_number, status, role_id, created_at)
VALUES
    -- Admin (admin1 theo flow.md)
    ('11111111-1111-1111-1111-111111111111', 'admin1', @demo_password, 'admin1@horseracing.com', '1995-01-01', 'MALE', 'System Admin', '0901234560', 'ACTIVE', @admin_role, NOW()),
    
    -- 6 Chủ ngựa (Owners: owner1 -> owner6)
    ('22222222-2222-2222-2222-222222222201', 'owner1', @demo_password, 'owner1@horseracing.com', '1990-05-15', 'MALE', 'Nguyen Van Owner One', '0901234561', 'ACTIVE', @owner_role, NOW()),
    ('22222222-2222-2222-2222-222222222202', 'owner2', @demo_password, 'owner2@horseracing.com', '1988-10-25', 'MALE', 'Tran Dan Owner Two', '0901234572', 'ACTIVE', @owner_role, NOW()),
    ('22222222-2222-2222-2222-222222222203', 'owner3', @demo_password, 'owner3@horseracing.com', '1991-03-14', 'FEMALE', 'Le Thi Owner Three', '0901234573', 'ACTIVE', @owner_role, NOW()),
    ('22222222-2222-2222-2222-222222222204', 'owner4', @demo_password, 'owner4@horseracing.com', '1985-07-30', 'MALE', 'Pham Minh Owner Four', '0901234574', 'ACTIVE', @owner_role, NOW()),
    ('22222222-2222-2222-2222-222222222205', 'owner5', @demo_password, 'owner5@horseracing.com', '1992-12-05', 'MALE', 'Hoang Gia Owner Five', '0901234575', 'ACTIVE', @owner_role, NOW()),
    ('22222222-2222-2222-2222-222222222206', 'owner6', @demo_password, 'owner6@horseracing.com', '1993-01-20', 'FEMALE', 'Bui Thi Owner Six', '0901234576', 'ACTIVE', @owner_role, NOW()),

    -- 2 Khán giả (spectator1: tạo cược, spectator2: xem cược đã chấm)
    ('44444444-4444-4444-4444-444444444401', 'spectator1', @demo_password, 'spectator1@horseracing.com', '1999-09-09', 'FEMALE', 'Bui Thi Spectator One', '0901234566', 'ACTIVE', @spectator_role, NOW()),
    ('44444444-4444-4444-4444-444444444402', 'spectator2', @demo_password, 'spectator2@horseracing.com', '1998-05-15', 'MALE', 'Tran Hung Spectator Two', '0901234567', 'ACTIVE', @spectator_role, NOW()),

    -- 5 Trọng tài (referee1 đến referee5)
    ('55555555-5555-5555-5555-555555555501', 'referee1', @demo_password, 'referee1@horseracing.com', '1985-02-14', 'MALE', 'Ngo Quyen Referee One', '0901234581', 'ACTIVE', @referee_role, NOW()),
    ('55555555-5555-5555-5555-555555555502', 'referee2', @demo_password, 'referee2@horseracing.com', '1982-11-20', 'MALE', 'Le Loi Referee Two', '0901234582', 'ACTIVE', @referee_role, NOW()),
    ('55555555-5555-5555-5555-555555555503', 'referee3', @demo_password, 'referee3@horseracing.com', '1980-05-15', 'MALE', 'Tran Hung Dao Referee Three', '0901234583', 'ACTIVE', @referee_role, NOW()),
    ('55555555-5555-5555-5555-555555555504', 'referee4', @demo_password, 'referee4@horseracing.com', '1987-08-22', 'MALE', 'Ly Thuong Kiet Referee Four', '0901234584', 'ACTIVE', @referee_role, NOW()),
    ('55555555-5555-5555-5555-555555555505', 'referee5', @demo_password, 'referee5@horseracing.com', '1984-04-12', 'MALE', 'Quang Trung Referee Five', '0901234585', 'ACTIVE', @referee_role, NOW()),

    -- 5 Bác sĩ thú y (vet1 đến vet5)
    ('66666666-6666-6666-6666-666666666601', 'vet1', @demo_password, 'vet1@horseracing.com', '1988-06-30', 'FEMALE', 'Dr. Nguyen Vet One', '0901234591', 'ACTIVE', @vet_role, NOW()),
    ('66666666-6666-6666-6666-666666666602', 'vet2', @demo_password, 'vet2@horseracing.com', '1985-09-12', 'MALE', 'Dr. Pham Vet Two', '0901234592', 'ACTIVE', @vet_role, NOW()),
    ('66666666-6666-6666-6666-666666666603', 'vet3', @demo_password, 'vet3@horseracing.com', '1987-12-05', 'FEMALE', 'Dr. Le Vet Three', '0901234593', 'ACTIVE', @vet_role, NOW()),
    ('66666666-6666-6666-6666-666666666604', 'vet4', @demo_password, 'vet4@horseracing.com', '1989-01-25', 'MALE', 'Dr. Hoang Vet Four', '0901234594', 'ACTIVE', @vet_role, NOW()),
    ('66666666-6666-6666-6666-666666666605', 'vet5', @demo_password, 'vet5@horseracing.com', '1986-04-15', 'FEMALE', 'Dr. Vu Vet Five', '0901234595', 'ACTIVE', @vet_role, NOW()),

    -- 5 Nhân viên y tế (medical1 đến medical5)
    ('77777777-7777-7777-7777-777777777701', 'medical1', @demo_password, 'medical1@horseracing.com', '1992-04-04', 'FEMALE', 'Dr. Tran Medical One', '0901234601', 'ACTIVE', @medical_role, NOW()),
    ('77777777-7777-7777-7777-777777777702', 'medical2', @demo_password, 'medical2@horseracing.com', '1990-10-18', 'MALE', 'Dr. Bui Medical Two', '0901234602', 'ACTIVE', @medical_role, NOW()),
    ('77777777-7777-7777-7777-777777777703', 'medical3', @demo_password, 'medical3@horseracing.com', '1991-05-12', 'FEMALE', 'Dr. Ngo Medical Three', '0901234603', 'ACTIVE', @medical_role, NOW()),
    ('77777777-7777-7777-7777-777777777704', 'medical4', @demo_password, 'medical4@horseracing.com', '1993-09-28', 'MALE', 'Dr. Dang Medical Four', '0901234604', 'ACTIVE', @medical_role, NOW()),
    ('77777777-7777-7777-7777-777777777705', 'medical5', @demo_password, 'medical5@horseracing.com', '1992-02-15', 'FEMALE', 'Dr. Do Medical Five', '0901234605', 'ACTIVE', @medical_role, NOW());

-- Chèn 40 Nài ngựa (jockey1 đến jockey40) bằng thủ công để đáp ứng luồng Bracket 32
INSERT INTO users (user_id, username, password, email, dob, gender, full_name, phone_number, status, role_id, created_at)
VALUES
    ('33333333-3333-3333-3333-333333333301', 'jockey1', @demo_password, 'jockey1@horseracing.com', '1996-08-20', 'MALE', 'Nai Ngua One', '0901234501', 'ACTIVE', @jockey_role, NOW()),
    ('33333333-3333-3333-3333-333333333302', 'jockey2', @demo_password, 'jockey2@horseracing.com', '1998-03-12', 'MALE', 'Nai Ngua Two', '0901234502', 'ACTIVE', @jockey_role, NOW()),
    ('33333333-3333-3333-3333-333333333303', 'jockey3', @demo_password, 'jockey3@horseracing.com', '1997-11-05', 'MALE', 'Nai Ngua Three', '0901234503', 'ACTIVE', @jockey_role, NOW()),
    ('33333333-3333-3333-3333-333333333304', 'jockey4', @demo_password, 'jockey4@horseracing.com', '1995-12-25', 'MALE', 'Nai Ngua Four', '0901234504', 'ACTIVE', @jockey_role, NOW()),
    ('33333333-3333-3333-3333-333333333305', 'jockey5', @demo_password, 'jockey5@horseracing.com', '1996-01-15', 'MALE', 'Nai Ngua Five', '0901234505', 'ACTIVE', @jockey_role, NOW()),
    ('33333333-3333-3333-3333-333333333306', 'jockey6', @demo_password, 'jockey6@horseracing.com', '1994-04-18', 'MALE', 'Nai Ngua Six', '0901234506', 'ACTIVE', @jockey_role, NOW()),
    ('33333333-3333-3333-3333-333333333307', 'jockey7', @demo_password, 'jockey7@horseracing.com', '1995-09-22', 'MALE', 'Nai Ngua Seven', '0901234507', 'ACTIVE', @jockey_role, NOW()),
    ('33333333-3333-3333-3333-333333333308', 'jockey8', @demo_password, 'jockey8@horseracing.com', '1997-02-10', 'MALE', 'Nai Ngua Eight', '0901234508', 'ACTIVE', @jockey_role, NOW()),
    ('33333333-3333-3333-3333-333333333309', 'jockey9', @demo_password, 'jockey9@horseracing.com', '1998-05-11', 'MALE', 'Nai Ngua Nine', '0901234509', 'ACTIVE', @jockey_role, NOW()),
    ('33333333-3333-3333-3333-333333333310', 'jockey10', @demo_password, 'jockey10@horseracing.com', '1999-07-15', 'MALE', 'Nai Ngua Ten', '0901234510', 'ACTIVE', @jockey_role, NOW()),
    ('33333333-3333-3333-3333-333333333311', 'jockey11', @demo_password, 'jockey11@horseracing.com', '1996-10-30', 'MALE', 'Nai Ngua Eleven', '0901234511', 'ACTIVE', @jockey_role, NOW()),
    ('33333333-3333-3333-3333-333333333312', 'jockey12', @demo_password, 'jockey12@horseracing.com', '1997-03-05', 'MALE', 'Nai Ngua Twelve', '0901234512', 'ACTIVE', @jockey_role, NOW()),
    ('33333333-3333-3333-3333-333333333313', 'jockey13', @demo_password, 'jockey13@horseracing.com', '1995-11-12', 'MALE', 'Nai Ngua Thirteen', '0901234513', 'ACTIVE', @jockey_role, NOW());

-- Tạo tiếp các Jockey từ 14 đến 40 để làm phong phú dữ liệu khớp nài ngựa
DELIMITER //
CREATE PROCEDURE CreateRemainingJockeys()
BEGIN
    DECLARE i INT DEFAULT 14;
    DECLARE phone VARCHAR(20);
    DECLARE u_id VARCHAR(36);
    WHILE i <= 40 DO
        SET phone = CONCAT('09012345', i);
        SET u_id = CONCAT('33333333-3333-3333-3333-3333333333', LPAD(i, 2, '0'));
        INSERT INTO users (user_id, username, password, email, dob, gender, full_name, phone_number, status, role_id, created_at)
        VALUES (u_id, CONCAT('jockey', i), @demo_password, CONCAT('jockey', i, '@horseracing.com'), '1997-01-01', 'MALE', CONCAT('Nai Ngua ', i), phone, 'ACTIVE', @jockey_role, NOW());
        SET i = i + 1;
    END WHILE;
END //
DELIMITER ;
CALL CreateRemainingJockeys();
DROP PROCEDURE CreateRemainingJockeys;

-- 4. Tạo Ví điện tử (Wallets) tương ứng với tất cả tài khoản
INSERT INTO wallets (wallet_id, owner_type, balance, currency, status, wallet_purpose, user_id, created_at, updated_at)
VALUES
    -- Admin & Spectators
    ('a0000000-1111-1111-1111-111111111111', 'USER', 10000000.00, 'VND', 'ACTIVE', 'USER_MAIN', '11111111-1111-1111-1111-111111111111', NOW(), NOW()),
    ('a0000000-4444-4444-4444-444444444401', 'USER', 50000000.00, 'VND', 'ACTIVE', 'USER_MAIN', '44444444-4444-4444-4444-444444444401', NOW(), NOW()),
    ('a0000000-4444-4444-4444-444444444402', 'USER', 50000000.00, 'VND', 'ACTIVE', 'USER_MAIN', '44444444-4444-4444-4444-444444444402', NOW(), NOW()),
    
    -- 6 Owners
    ('a0000000-2222-2222-2222-222222222201', 'USER', 50000000.00, 'VND', 'ACTIVE', 'USER_MAIN', '22222222-2222-2222-2222-222222222201', NOW(), NOW()),
    ('a0000000-2222-2222-2222-222222222202', 'USER', 50000000.00, 'VND', 'ACTIVE', 'USER_MAIN', '22222222-2222-2222-2222-222222222202', NOW(), NOW()),
    ('a0000000-2222-2222-2222-222222222203', 'USER', 50000000.00, 'VND', 'ACTIVE', 'USER_MAIN', '22222222-2222-2222-2222-222222222203', NOW(), NOW()),
    ('a0000000-2222-2222-2222-222222222204', 'USER', 50000000.00, 'VND', 'ACTIVE', 'USER_MAIN', '22222222-2222-2222-2222-222222222204', NOW(), NOW()),
    ('a0000000-2222-2222-2222-222222222205', 'USER', 50000000.00, 'VND', 'ACTIVE', 'USER_MAIN', '22222222-2222-2222-2222-222222222205', NOW(), NOW()),
    ('a0000000-2222-2222-2222-222222222206', 'USER', 50000000.00, 'VND', 'ACTIVE', 'USER_MAIN', '22222222-2222-2222-2222-222222222206', NOW(), NOW()),

    -- 5 Referees
    ('a0000000-5555-5555-5555-555555555501', 'USER', 10000000.00, 'VND', 'ACTIVE', 'USER_MAIN', '55555555-5555-5555-5555-555555555501', NOW(), NOW()),
    ('a0000000-5555-5555-5555-555555555502', 'USER', 10000000.00, 'VND', 'ACTIVE', 'USER_MAIN', '55555555-5555-5555-5555-555555555502', NOW(), NOW()),
    ('a0000000-5555-5555-5555-555555555503', 'USER', 10000000.00, 'VND', 'ACTIVE', 'USER_MAIN', '55555555-5555-5555-5555-555555555503', NOW(), NOW()),
    ('a0000000-5555-5555-5555-555555555504', 'USER', 10000000.00, 'VND', 'ACTIVE', 'USER_MAIN', '55555555-5555-5555-5555-555555555504', NOW(), NOW()),
    ('a0000000-5555-5555-5555-555555555505', 'USER', 10000000.00, 'VND', 'ACTIVE', 'USER_MAIN', '55555555-5555-5555-5555-555555555505', NOW(), NOW()),

    -- 5 Vets
    ('a0000000-6666-6666-6666-666666666601', 'USER', 10000000.00, 'VND', 'ACTIVE', 'USER_MAIN', '66666666-6666-6666-6666-666666666601', NOW(), NOW()),
    ('a0000000-6666-6666-6666-666666666602', 'USER', 10000000.00, 'VND', 'ACTIVE', 'USER_MAIN', '66666666-6666-6666-6666-666666666602', NOW(), NOW()),
    ('a0000000-6666-6666-6666-666666666603', 'USER', 10000000.00, 'VND', 'ACTIVE', 'USER_MAIN', '66666666-6666-6666-6666-666666666603', NOW(), NOW()),
    ('a0000000-6666-6666-6666-666666666604', 'USER', 10000000.00, 'VND', 'ACTIVE', 'USER_MAIN', '66666666-6666-6666-6666-666666666604', NOW(), NOW()),
    ('a0000000-6666-6666-6666-666666666605', 'USER', 10000000.00, 'VND', 'ACTIVE', 'USER_MAIN', '66666666-6666-6666-6666-666666666605', NOW(), NOW()),

    -- 5 Medical Staffs
    ('a0000000-7777-7777-7777-777777777701', 'USER', 10000000.00, 'VND', 'ACTIVE', 'USER_MAIN', '77777777-7777-7777-7777-777777777701', NOW(), NOW()),
    ('a0000000-7777-7777-7777-777777777702', 'USER', 10000000.00, 'VND', 'ACTIVE', 'USER_MAIN', '77777777-7777-7777-7777-777777777702', NOW(), NOW()),
    ('a0000000-7777-7777-7777-777777777703', 'USER', 10000000.00, 'VND', 'ACTIVE', 'USER_MAIN', '77777777-7777-7777-7777-777777777703', NOW(), NOW()),
    ('a0000000-7777-7777-7777-777777777704', 'USER', 10000000.00, 'VND', 'ACTIVE', 'USER_MAIN', '77777777-7777-7777-7777-777777777704', NOW(), NOW()),
    ('a0000000-7777-7777-7777-777777777705', 'USER', 10000000.00, 'VND', 'ACTIVE', 'USER_MAIN', '77777777-7777-7777-7777-777777777705', NOW(), NOW());

-- Tạo ví cho 40 Jockeys
DELIMITER //
CREATE PROCEDURE CreateJockeyWallets()
BEGIN
    DECLARE i INT DEFAULT 1;
    DECLARE u_id VARCHAR(36);
    DECLARE w_id VARCHAR(36);
    WHILE i <= 40 DO
        SET u_id = CONCAT('33333333-3333-3333-3333-3333333333', LPAD(i, 2, '0'));
        SET w_id = CONCAT('a0000000-3333-3333-3333-3333333333', LPAD(i, 2, '0'));
        INSERT INTO wallets (wallet_id, owner_type, balance, currency, status, wallet_purpose, user_id, created_at, updated_at)
        VALUES (w_id, 'USER', 10000000.00, 'VND', 'ACTIVE', 'USER_MAIN', u_id, NOW(), NOW());
        SET i = i + 1;
    END WHILE;
END //
DELIMITER ;
CALL CreateJockeyWallets();
DROP PROCEDURE CreateJockeyWallets;

-- 5. Tạo thông tin chi tiết cho từng Vai Trò (Profiles)
-- 6 Chủ Ngựa (Horse Owners)
INSERT INTO horse_owners (owner_id, user_id, farm_name, address, license_number, created_at)
VALUES
    ('aaaaaaaa-1111-1111-1111-111111111101', '22222222-2222-2222-2222-222222222201', 'Golden Fields Farm', '123 Ba Dinh, Hanoi', 'LIC-OWN-0001', NOW()),
    ('aaaaaaaa-1111-1111-1111-111111111102', '22222222-2222-2222-2222-222222222202', 'Silver Valleys Stud', '456 District 1, HCMC', 'LIC-OWN-0002', NOW()),
    ('aaaaaaaa-1111-1111-1111-111111111103', '22222222-2222-2222-2222-222222222203', 'Breeze Hills Farm', '789 Son Tra, Da Nang', 'LIC-OWN-0003', NOW()),
    ('aaaaaaaa-1111-1111-1111-111111111104', '22222222-2222-2222-2222-222222222204', 'Red River Meadows', '101 Hoan Kiem, Hanoi', 'LIC-OWN-0004', NOW()),
    ('aaaaaaaa-1111-1111-1111-111111111105', '22222222-2222-2222-2222-222222222205', 'Blue Oceans Ranch', '202 Nha Trang, Khanh Hoa', 'LIC-OWN-0005', NOW()),
    ('aaaaaaaa-1111-1111-1111-111111111106', '22222222-2222-2222-2222-222222222206', 'Green Hills Farm', '303 Da Lat, Lam Dong', 'LIC-OWN-0006', NOW());

-- 40 Nài Ngựa (Jockeys)
DELIMITER //
CREATE PROCEDURE CreateJockeyProfiles()
BEGIN
    DECLARE i INT DEFAULT 1;
    DECLARE u_id VARCHAR(36);
    DECLARE j_id VARCHAR(36);
    DECLARE tier VARCHAR(30);
    DECLARE spec VARCHAR(30);
    
    WHILE i <= 40 DO
        SET u_id = CONCAT('33333333-3333-3333-3333-3333333333', LPAD(i, 2, '0'));
        SET j_id = CONCAT('bbbbbbbb-3333-3333-3333-3333333333', LPAD(i, 2, '0'));
        
        -- Chia đều phân hạng cho nài ngựa
        IF i % 4 = 0 THEN SET tier = 'ELITE';
        ELSEIF i % 3 = 0 THEN SET tier = 'PROFESSIONAL';
        ELSEIF i % 2 = 0 THEN SET tier = 'JUNIOR';
        ELSE SET tier = 'APPRENTICE';
        END IF;

        -- Chia đều chuyên môn
        IF i % 3 = 0 THEN SET spec = 'SPRINT';
        ELSEIF i % 2 = 0 THEN SET spec = 'MILE';
        ELSE SET spec = 'INTERMEDIATE';
        END IF;

        INSERT INTO jockeys (jockey_id, user_id, height, weight, experience_years, license_number, specialization, status, total_races, total_wins, jockey_tier, tier_updated_at, created_at)
        VALUES (j_id, u_id, 1.60 + (i%5)*0.01, 50.0 + (i%3), 1 + (i%10), CONCAT('LIC-JOC-', LPAD(i, 4, '0')), spec, 'AVAILABLE', 5 + i, i%3, tier, NOW(), NOW());
        SET i = i + 1;
    END WHILE;
END //
DELIMITER ;
CALL CreateJockeyProfiles();
DROP PROCEDURE CreateJockeyProfiles;

-- Khán Giả (Spectators)
INSERT INTO spectators (spectator_id, user_id, total_points, created_at)
VALUES 
    ('cccccccc-1111-1111-1111-111111111101', '44444444-4444-4444-4444-444444444401', 30000, NOW()),
    ('cccccccc-1111-1111-1111-111111111102', '44444444-4444-4444-4444-444444444402', 15000, NOW());

-- 5 Trọng Tài (Referees)
INSERT INTO referees (referee_id, user_id, certification_level, years_of_service, status, created_at)
VALUES 
    ('dddddddd-5555-5555-5555-555555555501', '55555555-5555-5555-5555-555555555501', 'International Class A', 10, 'AVAILABLE', NOW()),
    ('dddddddd-5555-5555-5555-555555555502', '55555555-5555-5555-5555-555555555502', 'National Level 1', 6, 'AVAILABLE', NOW()),
    ('dddddddd-5555-5555-5555-555555555503', '55555555-5555-5555-5555-555555555503', 'Regional Class B', 4, 'AVAILABLE', NOW()),
    ('dddddddd-5555-5555-5555-555555555504', '55555555-5555-5555-5555-555555555504', 'International Class A', 8, 'AVAILABLE', NOW()),
    ('dddddddd-5555-5555-5555-555555555505', '55555555-5555-5555-5555-555555555505', 'National Level 1', 5, 'AVAILABLE', NOW());

-- 5 Bác Sĩ Thú Y (Veterinarians)
INSERT INTO veterinarians (vet_id, user_id, license_number, specialization, years_of_service, status, created_at)
VALUES 
    ('eeeeeeee-6666-6666-6666-666666666601', '66666666-6666-6666-6666-666666666601', 'LIC-VET-0001', 'Equine Surgery', 8, 'AVAILABLE', NOW()),
    ('eeeeeeee-6666-6666-6666-666666666602', '66666666-6666-6666-6666-666666666602', 'LIC-VET-0002', 'Equine Orthopedics', 6, 'AVAILABLE', NOW()),
    ('eeeeeeee-6666-6666-6666-666666666603', '66666666-6666-6666-6666-666666666603', 'LIC-VET-0003', 'Equine Nutrition', 4, 'AVAILABLE', NOW()),
    ('eeeeeeee-6666-6666-6666-666666666604', '66666666-6666-6666-6666-666666666604', 'LIC-VET-0004', 'Equine Cardiology', 10, 'AVAILABLE', NOW()),
    ('eeeeeeee-6666-6666-6666-666666666605', '66666666-6666-6666-6666-666666666605', 'LIC-VET-0005', 'Equine Rehabilitation', 5, 'AVAILABLE', NOW());

-- 5 Nhân Viên Y Tế (Medical Staff)
INSERT INTO medical_staffs (med_staff_id, user_id, certification, years_of_service, status, created_at)
VALUES 
    ('ffffffff-7777-7777-7777-777777777701', '77777777-7777-7777-7777-777777777701', 'CERT-MED-0001', 5, 'AVAILABLE', NOW()),
    ('ffffffff-7777-7777-7777-777777777702', '77777777-7777-7777-7777-777777777702', 'CERT-MED-0002', 7, 'AVAILABLE', NOW()),
    ('ffffffff-7777-7777-7777-777777777703', '77777777-7777-7777-7777-777777777703', 'CERT-MED-0003', 3, 'AVAILABLE', NOW()),
    ('ffffffff-7777-7777-7777-777777777704', '77777777-7777-7777-7777-777777777704', 'CERT-MED-0004', 12, 'AVAILABLE', NOW()),
    ('ffffffff-7777-7777-7777-777777777705', '77777777-7777-7777-7777-777777777705', 'CERT-MED-0005', 4, 'AVAILABLE', NOW());


-- 6. Tạo 16 ngựa (Horses) phân bố cho các Chủ ngựa
INSERT INTO horses (horse_id, name, breed, gender, age, weight, color, health_status, current_rating, highest_rating, race_class, created_at, owner_id)
VALUES
    -- Owner 1 (3 ngựa)
    ('99999999-9999-9999-9999-999999999901', 'Thunderbolt', 'THOROUGHBRED', 'MALE', 5, 455.0, 'Bay', 'HEALTHY', 45, 50, 'CLASS_4', NOW(), 'aaaaaaaa-1111-1111-1111-111111111101'),
    ('99999999-9999-9999-9999-999999999902', 'Wind Runner', 'THOROUGHBRED', 'FEMALE', 4, 430.0, 'Chestnut', 'HEALTHY', 55, 55, 'CLASS_4', NOW(), 'aaaaaaaa-1111-1111-1111-111111111101'),
    ('99999999-9999-9999-9999-999999999903', 'Silver Streak', 'THOROUGHBRED', 'MALE', 6, 470.0, 'Gray', 'HEALTHY', 35, 40, 'CLASS_5', NOW(), 'aaaaaaaa-1111-1111-1111-111111111101'),
    
    -- Owner 2 (3 ngựa)
    ('99999999-9999-9999-9999-999999999904', 'Black Beauty', 'THOROUGHBRED', 'FEMALE', 5, 460.0, 'Black', 'HEALTHY', 62, 65, 'CLASS_3', NOW(), 'aaaaaaaa-1111-1111-1111-111111111102'),
    ('99999999-9999-9999-9999-999999999905', 'Golden Mane', 'THOROUGHBRED', 'MALE', 4, 440.0, 'Palomino', 'HEALTHY', 42, 45, 'CLASS_4', NOW(), 'aaaaaaaa-1111-1111-1111-111111111102'),
    ('99999999-9999-9999-9999-999999999906', 'Crimson Flash', 'THOROUGHBRED', 'MALE', 5, 458.0, 'Red Bay', 'HEALTHY', 48, 50, 'CLASS_4', NOW(), 'aaaaaaaa-1111-1111-1111-111111111102'),

    -- Owner 3 (3 ngựa)
    ('99999999-9999-9999-9999-999999999907', 'Midnight Star', 'THOROUGHBRED', 'FEMALE', 6, 448.0, 'Black', 'HEALTHY', 52, 54, 'CLASS_4', NOW(), 'aaaaaaaa-1111-1111-1111-111111111103'),
    ('99999999-9999-9999-9999-999999999908', 'Pegasus', 'THOROUGHBRED', 'MALE', 4, 452.0, 'White', 'HEALTHY', 50, 50, 'CLASS_4', NOW(), 'aaaaaaaa-1111-1111-1111-111111111103'),
    ('99999999-9999-9999-9999-999999999909', 'Desert Wind', 'THOROUGHBRED', 'MALE', 5, 435.0, 'Grey', 'HEALTHY', 38, 40, 'CLASS_5', NOW(), 'aaaaaaaa-1111-1111-1111-111111111103'),

    -- Owner 4 (3 ngựa)
    ('99999999-9999-9999-9999-999999999910', 'Storm Chaser', 'THOROUGHBRED', 'MALE', 5, 465.0, 'Brown', 'HEALTHY', 41, 41, 'CLASS_4', NOW(), 'aaaaaaaa-1111-1111-1111-111111111104'),
    ('99999999-9999-9999-9999-999999999911', 'Eclipse', 'THOROUGHBRED', 'FEMALE', 6, 450.0, 'Dark Brown', 'HEALTHY', 46, 48, 'CLASS_4', NOW(), 'aaaaaaaa-1111-1111-1111-111111111104'),
    ('99999999-9999-9999-9999-999999999912', 'Iron Gallop', 'THOROUGHBRED', 'MALE', 4, 462.0, 'Spotted', 'HEALTHY', 39, 39, 'CLASS_5', NOW(), 'aaaaaaaa-1111-1111-1111-111111111104'),

    -- Owner 5 (2 ngựa)
    ('99999999-9999-9999-9999-999999999913', 'Phantom Rider', 'THOROUGHBRED', 'MALE', 5, 457.0, 'Bay', 'HEALTHY', 43, 45, 'CLASS_4', NOW(), 'aaaaaaaa-1111-1111-1111-111111111105'),
    ('99999999-9999-9999-9999-999999999914', 'Stardust', 'THOROUGHBRED', 'FEMALE', 4, 442.0, 'Chestnut', 'HEALTHY', 44, 44, 'CLASS_4', NOW(), 'aaaaaaaa-1111-1111-1111-111111111105'),

    -- Owner 6 (2 ngựa)
    ('99999999-9999-9999-9999-999999999915', 'Neptune', 'THOROUGHBRED', 'MALE', 6, 468.0, 'Roan', 'HEALTHY', 37, 37, 'CLASS_5', NOW(), 'aaaaaaaa-1111-1111-1111-111111111106'),
    ('99999999-9999-9999-9999-999999999916', 'Valkyrie', 'THOROUGHBRED', 'FEMALE', 5, 449.0, 'Dun', 'HEALTHY', 47, 49, 'CLASS_4', NOW(), 'aaaaaaaa-1111-1111-1111-111111111106');


-- 7. KỊCH BẢN GIẢI ĐẤU MẪU ĐỂ TEST TỪNG PHASE CỦA HỆ THỐNG

-- Kịch bản A: Giải đấu "DEMO 5 - Đang mở đăng ký" (Phase: REGISTRATION_OPEN)
-- Phục vụ test: Owner 1 nộp đăng ký ngựa, Jockey 13 nộp đăng ký tham gia
INSERT INTO tournaments (tournament_id, name, description, start_date, end_date, location, registration_fee, system_contract_fee, total_prize_pool, allowed_breed, min_horse_age, max_horse_age, registration_open_at, registration_close_at, review_deadline_at, jockey_matching_deadline_at, scheduling_deadline_at, competition_start_at, status, phase, created_by, max_approved_horses, max_approved_jockeys, max_approved_entries, distance)
VALUES (
    '88888888-8888-8888-8888-888888888805',
    'DEMO 5 - Đang mở đăng ký',
    'Giải đấu mùa Xuân chuẩn bị mở cổng đăng ký.',
    DATE_ADD(CURDATE(), INTERVAL 10 DAY),
    DATE_ADD(CURDATE(), INTERVAL 15 DAY),
    'Hanoi National Turf Club',
    500000.00,
    100000.00,
    15000000.00,
    'THOROUGHBRED',
    3, 8,
    DATE_SUB(NOW(), INTERVAL 1 DAY), -- đã mở
    DATE_ADD(NOW(), INTERVAL 5 DAY),
    DATE_ADD(NOW(), INTERVAL 6 DAY),
    DATE_ADD(NOW(), INTERVAL 7 DAY),
    DATE_ADD(NOW(), INTERVAL 8 DAY),
    DATE_ADD(NOW(), INTERVAL 9 DAY),
    'OPEN',
    'REGISTRATION_OPEN',
    '11111111-1111-1111-1111-111111111111',
    20, 20, 20,
    'SPRINT_1200M'
);

-- Kịch bản B: Giải đấu "DEMO 6 - Đang ghép Kỵ sĩ" (Phase: JOCKEY_MATCHING)
-- Phục vụ test: Owner 1 (owner1) tìm Jockey (từ jockey9 đến jockey12) để đề xuất hợp đồng (Contract)
INSERT INTO tournaments (tournament_id, name, description, start_date, end_date, location, registration_fee, system_contract_fee, total_prize_pool, allowed_breed, min_horse_age, max_horse_age, registration_open_at, registration_close_at, review_deadline_at, jockey_matching_deadline_at, scheduling_deadline_at, competition_start_at, status, phase, created_by, max_approved_horses, max_approved_jockeys, max_approved_entries, distance)
VALUES (
    '88888888-8888-8888-8888-888888888806',
    'DEMO 6 - Đang ghép Kỵ sĩ',
    'Giải đấu đang trong thời gian ghép cặp kỵ sĩ và ký hợp đồng.',
    DATE_ADD(CURDATE(), INTERVAL 8 DAY),
    DATE_ADD(CURDATE(), INTERVAL 12 DAY),
    'Saigon Turf Racing Arena',
    800000.00,
    150000.00,
    25000000.00,
    'THOROUGHBRED',
    3, 8,
    DATE_SUB(NOW(), INTERVAL 5 DAY),
    DATE_SUB(NOW(), INTERVAL 2 DAY),
    DATE_SUB(NOW(), INTERVAL 1 DAY),
    DATE_ADD(NOW(), INTERVAL 3 DAY), -- Hạn ghép kỵ sĩ còn 3 ngày
    DATE_ADD(NOW(), INTERVAL 4 DAY),
    DATE_ADD(NOW(), INTERVAL 5 DAY),
    'ONGOING',
    'JOCKEY_MATCHING',
    '11111111-1111-1111-1111-111111111111',
    16, 16, 16,
    'MILE_1400M'
);

-- Tạo sẵn các hồ sơ duyệt APPROVED cho giải DEMO 6 để ghép cặp kỵ sĩ
INSERT INTO horse_tournament_registrations (horse_tournament_reg_id, tournament_id, horse_id, owner_id, status, submitted_at, rating_at_registration, race_class_at_registration)
VALUES 
    ('reg-demo6-horse01', '88888888-8888-8888-8888-888888888806', '99999999-9999-9999-9999-999999999901', 'aaaaaaaa-1111-1111-1111-111111111101', 'APPROVED', DATE_SUB(NOW(), INTERVAL 3 DAY), 45, 'CLASS_4'),
    ('reg-demo6-horse02', '88888888-8888-8888-8888-888888888806', '99999999-9999-9999-9999-999999999902', 'aaaaaaaa-1111-1111-1111-111111111101', 'APPROVED', DATE_SUB(NOW(), INTERVAL 3 DAY), 55, 'CLASS_4');

INSERT INTO jockey_tournament_registrations (jockey_tournament_reg_id, tournament_id, jockey_id, status, submitted_at, hire_fee)
VALUES 
    ('reg-demo6-jock09', '88888888-8888-8888-8888-888888888806', 'bbbbbbbb-3333-3333-3333-333333333309', 'APPROVED', DATE_SUB(NOW(), INTERVAL 3 DAY), 500000.00),
    ('reg-demo6-jock10', '88888888-8888-8888-8888-888888888806', 'bbbbbbbb-3333-3333-3333-333333333310', 'APPROVED', DATE_SUB(NOW(), INTERVAL 3 DAY), 400000.00),
    ('reg-demo6-jock11', '88888888-8888-8888-8888-888888888806', 'bbbbbbbb-3333-3333-3333-333333333311', 'APPROVED', DATE_SUB(NOW(), INTERVAL 3 DAY), 600000.00),
    ('reg-demo6-jock12', '88888888-8888-8888-8888-888888888806', 'bbbbbbbb-3333-3333-3333-333333333312', 'APPROVED', DATE_SUB(NOW(), INTERVAL 3 DAY), 300000.00);

-- Tạo 1 hợp đồng chờ kỵ sĩ xác nhận (PENDING_JOCKEY) để demo
INSERT INTO jockey_horse_contracts (contract_id, tournament_id, horse_tournament_reg_id, jockey_tournament_reg_id, owner_id, horse_id, jockey_id, hire_fee, advance_percent, final_percent, advance_paid_amount, escrow_amount, system_contract_fee, owner_prize_share_percent, jockey_prize_share_percent, payment_status, escrow_status, advance_payout_status, final_payout_status, status, requested_at)
VALUES ('con-demo6-pending', '88888888-8888-8888-8888-888888888806', 'reg-demo6-horse01', 'reg-demo6-jock09', 'aaaaaaaa-1111-1111-1111-111111111101', '99999999-9999-9999-9999-999999999901', 'bbbbbbbb-3333-3333-3333-333333333309', 500000.00, 50.0, 50.0, 0.00, 0.00, 20000.00, 80.0, 20.0, 'UNPAID', 'NOT_HELD', 'NOT_PAID', 'NOT_RELEASED', 'PENDING_JOCKEY', NOW());


-- Kịch bản C: Giải đấu "DEMO 7 - Đang xếp lịch" (Phase: SCHEDULING)
-- Phục vụ test: Admin (admin1) mở bàn điều phối, phân công Entry, Làn và Nhân sự (Referee, Vet, Medical Staff)
INSERT INTO tournaments (tournament_id, name, description, start_date, end_date, location, registration_fee, system_contract_fee, total_prize_pool, allowed_breed, min_horse_age, max_horse_age, registration_open_at, registration_close_at, review_deadline_at, jockey_matching_deadline_at, scheduling_deadline_at, competition_start_at, status, phase, created_by, max_approved_horses, max_approved_jockeys, max_approved_entries, distance)
VALUES (
    '88888888-8888-8888-8888-888888888807',
    'DEMO 7 - Đang xếp lịch',
    'Giải đấu đã kết thúc ghép kỵ sĩ, đang cấu hình lịch thi đấu.',
    DATE_ADD(CURDATE(), INTERVAL 5 DAY),
    DATE_ADD(CURDATE(), INTERVAL 10 DAY),
    'Da Nang Turf Park',
    1000000.00,
    200000.00,
    40000000.00,
    'THOROUGHBRED',
    3, 8,
    DATE_SUB(NOW(), INTERVAL 8 DAY),
    DATE_SUB(NOW(), INTERVAL 5 DAY),
    DATE_SUB(NOW(), INTERVAL 4 DAY),
    DATE_SUB(NOW(), INTERVAL 2 DAY),
    DATE_ADD(NOW(), INTERVAL 1 DAY), -- deadline còn 1 ngày
    DATE_ADD(NOW(), INTERVAL 2 DAY),
    'ONGOING',
    'SCHEDULING',
    '11111111-1111-1111-1111-111111111111',
    16, 16, 16,
    'MILE_1600M'
);

-- Khai báo vòng đấu (Rounds) dạng SCHEDULING cho giải DEMO 7
INSERT INTO rounds (round_id, round_name, sequence_order, is_final, prediction_type, advancement_rule, start_date, end_date, description, max_races, max_entries, min_entries, status, head_referee_id, head_referee_assigned_at, expected_entries, planned_race_count, qualifiers_per_race, bracket_plan_version, transition_status, created_at, tournament_id, created_by)
VALUES (
    'round-demo7-01',
    'Vòng 1 - Lập lịch',
    1, 1, 'TOP3', 'Chung kết tranh cup',
    DATE_ADD(NOW(), INTERVAL 2 DAY),
    DATE_ADD(NOW(), INTERVAL 3 DAY),
    'Vòng đấu lập lịch thi đấu mẫu.',
    1, 4, 4,
    'SCHEDULING',
    'dddddddd-5555-5555-5555-555555555501',
    NOW(),
    4, 1, 0, 1,
    'NOT_READY',
    NOW(),
    '88888888-8888-8888-8888-888888888807',
    '11111111-1111-1111-1111-111111111111'
);

-- Trận đấu dạng SCHEDULING
INSERT INTO races (race_id, name, start_time, end_time, track_condition, distance, sequence_order, status, round_id, created_by)
VALUES (
    'race-demo7-01',
    'DEMO 7 - Trận đấu kiểm thử lập lịch',
    NULL, NULL, 'TBD', 'MILE_1600M', 1, 'SCHEDULING',
    'round-demo7-01', '11111111-1111-1111-1111-111111111111'
);

-- Tạo 4 hợp đồng (APPROVED) để sẵn sàng phân entry cho giải DEMO 7
INSERT INTO horse_tournament_registrations (horse_tournament_reg_id, tournament_id, horse_id, owner_id, status, submitted_at, rating_at_registration, race_class_at_registration)
VALUES
    ('reg-demo7-horse01', '88888888-8888-8888-8888-888888888807', '99999999-9999-9999-9999-999999999901', 'aaaaaaaa-1111-1111-1111-111111111101', 'APPROVED', DATE_SUB(NOW(), INTERVAL 4 DAY), 45, 'CLASS_4'),
    ('reg-demo7-horse02', '88888888-8888-8888-8888-888888888807', '99999999-9999-9999-9999-999999999902', 'aaaaaaaa-1111-1111-1111-111111111101', 'APPROVED', DATE_SUB(NOW(), INTERVAL 4 DAY), 55, 'CLASS_4'),
    ('reg-demo7-horse03', '88888888-8888-8888-8888-888888888807', '99999999-9999-9999-9999-999999999903', 'aaaaaaaa-1111-1111-1111-111111111101', 'APPROVED', DATE_SUB(NOW(), INTERVAL 4 DAY), 35, 'CLASS_5'),
    ('reg-demo7-horse04', '88888888-8888-8888-8888-888888888807', '99999999-9999-9999-9999-999999999904', 'aaaaaaaa-1111-1111-1111-111111111102', 'APPROVED', DATE_SUB(NOW(), INTERVAL 4 DAY), 62, 'CLASS_3');

INSERT INTO jockey_tournament_registrations (jockey_tournament_reg_id, tournament_id, jockey_id, status, submitted_at, hire_fee)
VALUES
    ('reg-demo7-jock01', '88888888-8888-8888-8888-888888888807', 'bbbbbbbb-3333-3333-3333-333333333301', 'APPROVED', DATE_SUB(NOW(), INTERVAL 4 DAY), 500000.00),
    ('reg-demo7-jock02', '88888888-8888-8888-8888-888888888807', 'bbbbbbbb-3333-3333-3333-333333333302', 'APPROVED', DATE_SUB(NOW(), INTERVAL 4 DAY), 400000.00),
    ('reg-demo7-jock03', '88888888-8888-8888-8888-888888888807', 'bbbbbbbb-3333-3333-3333-333333333303', 'APPROVED', DATE_SUB(NOW(), INTERVAL 4 DAY), 700000.00),
    ('reg-demo7-jock04', '88888888-8888-8888-8888-888888888807', 'bbbbbbbb-3333-3333-3333-333333333304', 'APPROVED', DATE_SUB(NOW(), INTERVAL 4 DAY), 300000.00);

INSERT INTO jockey_horse_contracts (contract_id, tournament_id, horse_tournament_reg_id, jockey_tournament_reg_id, owner_id, horse_id, jockey_id, hire_fee, advance_percent, final_percent, advance_paid_amount, escrow_amount, system_contract_fee, owner_prize_share_percent, jockey_prize_share_percent, payment_status, escrow_status, advance_payout_status, final_payout_status, status, requested_at, accepted_at)
VALUES
    ('con-demo7-01', '88888888-8888-8888-8888-888888888807', 'reg-demo7-horse01', 'reg-demo7-jock01', 'aaaaaaaa-1111-1111-1111-111111111101', '99999999-9999-9999-9999-999999999901', 'bbbbbbbb-3333-3333-3333-333333333301', 500000.00, 50.0, 50.0, 250000.00, 250000.00, 20000.00, 80.0, 20.0, 'PAID', 'HELD', 'PAID', 'NOT_RELEASED', 'APPROVED', DATE_SUB(NOW(), INTERVAL 3 DAY), DATE_SUB(NOW(), INTERVAL 3 DAY)),
    ('con-demo7-02', '88888888-8888-8888-8888-888888888807', 'reg-demo7-horse02', 'reg-demo7-jock02', 'aaaaaaaa-1111-1111-1111-111111111101', '99999999-9999-9999-9999-999999999902', 'bbbbbbbb-3333-3333-3333-333333333302', 400000.00, 50.0, 50.0, 200000.00, 200000.00, 20000.00, 80.0, 20.0, 'PAID', 'HELD', 'PAID', 'NOT_RELEASED', 'APPROVED', DATE_SUB(NOW(), INTERVAL 3 DAY), DATE_SUB(NOW(), INTERVAL 3 DAY)),
    ('con-demo7-03', '88888888-8888-8888-8888-888888888807', 'reg-demo7-horse03', 'reg-demo7-jock03', 'aaaaaaaa-1111-1111-1111-111111111101', '99999999-9999-9999-9999-999999999903', 'bbbbbbbb-3333-3333-3333-333333333303', 700000.00, 50.0, 50.0, 350000.00, 350000.00, 20000.00, 85.0, 15.0, 'PAID', 'HELD', 'PAID', 'NOT_RELEASED', 'APPROVED', DATE_SUB(NOW(), INTERVAL 3 DAY), DATE_SUB(NOW(), INTERVAL 3 DAY)),
    ('con-demo7-04', '88888888-8888-8888-8888-888888888807', 'reg-demo7-horse04', 'reg-demo7-jock04', 'aaaaaaaa-1111-1111-1111-111111111102', '99999999-9999-9999-9999-999999999904', 'bbbbbbbb-3333-3333-3333-333333333304', 300000.00, 50.0, 50.0, 150000.00, 150000.00, 20000.00, 90.0, 10.0, 'PAID', 'HELD', 'PAID', 'NOT_RELEASED', 'APPROVED', DATE_SUB(NOW(), INTERVAL 3 DAY), DATE_SUB(NOW(), INTERVAL 3 DAY));


-- Kịch bản D: Giải đấu "Summer Grand Championship" (Phase: RACING, Status: ONGOING)
-- Phục vụ các luồng chính: Khám sức khỏe (Inspection), Đặt cược (Prediction), Trọng tài nộp kết quả (Race 3)
INSERT INTO tournaments (tournament_id, name, description, start_date, end_date, location, registration_fee, system_contract_fee, total_prize_pool, allowed_breed, min_horse_age, max_horse_age, registration_open_at, registration_close_at, review_deadline_at, jockey_matching_deadline_at, scheduling_deadline_at, competition_start_at, status, phase, created_by, max_approved_horses, max_approved_jockeys, max_approved_entries, distance)
VALUES (
    '88888888-8888-8888-8888-888888888808',
    'Summer Grand Championship',
    'The premium racing championship of the summer. Top rated horses competing for prestige.',
    DATE_SUB(CURDATE(), INTERVAL 2 DAY),
    DATE_ADD(CURDATE(), INTERVAL 2 DAY),
    'Saigon Turf Racing Arena',
    1000000.00,
    200000.00,
    50000000.00,
    'THOROUGHBRED',
    4, 10,
    DATE_SUB(NOW(), INTERVAL 10 DAY),
    DATE_SUB(NOW(), INTERVAL 7 DAY),
    DATE_SUB(NOW(), INTERVAL 6 DAY),
    DATE_SUB(NOW(), INTERVAL 5 DAY),
    DATE_SUB(NOW(), INTERVAL 4 DAY),
    DATE_SUB(NOW(), INTERVAL 3 DAY),
    'ONGOING',
    'RACING',
    '11111111-1111-1111-1111-111111111111',
    16, 16, 16,
    'MILE_1600M'
);

-- Cấu hình giải thưởng
INSERT INTO prize_structures (prize_structure_id, `prize_rank`, percentage, fixed_amount, is_active, tournament_id)
VALUES
    ('p-demo8-01', 1, 60.0, 30000000.00, 1, '88888888-8888-8888-8888-888888888808'),
    ('p-demo8-02', 2, 25.0, 12500000.00, 1, '88888888-8888-8888-8888-888888888808'),
    ('p-demo8-03', 3, 15.0, 7500000.00, 1, '88888888-8888-8888-8888-888888888808');

-- Đăng ký hồ sơ APPROVED
INSERT INTO horse_tournament_registrations (horse_tournament_reg_id, tournament_id, horse_id, owner_id, status, submitted_at, rating_at_registration, race_class_at_registration)
VALUES
    ('reg-demo8-horse01', '88888888-8888-8888-8888-888888888808', '99999999-9999-9999-9999-999999999901', 'aaaaaaaa-1111-1111-1111-111111111101', 'APPROVED', DATE_SUB(NOW(), INTERVAL 9 DAY), 45, 'CLASS_4'),
    ('reg-demo8-horse02', '88888888-8888-8888-8888-888888888808', '99999999-9999-9999-9999-999999999902', 'aaaaaaaa-1111-1111-1111-111111111101', 'APPROVED', DATE_SUB(NOW(), INTERVAL 9 DAY), 55, 'CLASS_4'),
    ('reg-demo8-horse03', '88888888-8888-8888-8888-888888888808', '99999999-9999-9999-9999-999999999903', 'aaaaaaaa-1111-1111-1111-111111111101', 'APPROVED', DATE_SUB(NOW(), INTERVAL 9 DAY), 35, 'CLASS_5'),
    ('reg-demo8-horse04', '88888888-8888-8888-8888-888888888808', '99999999-9999-9999-9999-999999999904', 'aaaaaaaa-1111-1111-1111-111111111102', 'APPROVED', DATE_SUB(NOW(), INTERVAL 9 DAY), 62, 'CLASS_3'),
    ('reg-demo8-horse05', '88888888-8888-8888-8888-888888888808', '99999999-9999-9999-9999-999999999905', 'aaaaaaaa-1111-1111-1111-111111111102', 'APPROVED', DATE_SUB(NOW(), INTERVAL 9 DAY), 42, 'CLASS_4'),
    ('reg-demo8-horse06', '88888888-8888-8888-8888-888888888808', '99999999-9999-9999-9999-999999999906', 'aaaaaaaa-1111-1111-1111-111111111102', 'APPROVED', DATE_SUB(NOW(), INTERVAL 9 DAY), 48, 'CLASS_4'),
    ('reg-demo8-horse07', '88888888-8888-8888-8888-888888888808', '99999999-9999-9999-9999-999999999907', 'aaaaaaaa-1111-1111-1111-111111111103', 'APPROVED', DATE_SUB(NOW(), INTERVAL 9 DAY), 52, 'CLASS_4'),
    ('reg-demo8-horse08', '88888888-8888-8888-8888-888888888808', '99999999-9999-9999-9999-999999999908', 'aaaaaaaa-1111-1111-1111-111111111103', 'APPROVED', DATE_SUB(NOW(), INTERVAL 9 DAY), 50, 'CLASS_4');

-- Đăng ký Nài
INSERT INTO jockey_tournament_registrations (jockey_tournament_reg_id, tournament_id, jockey_id, status, submitted_at, hire_fee)
VALUES
    ('reg-demo8-jock01', '88888888-8888-8888-8888-888888888808', 'bbbbbbbb-3333-3333-3333-333333333301', 'APPROVED', DATE_SUB(NOW(), INTERVAL 9 DAY), 500000.00),
    ('reg-demo8-jock02', '88888888-8888-8888-8888-888888888808', 'bbbbbbbb-3333-3333-3333-333333333302', 'APPROVED', DATE_SUB(NOW(), INTERVAL 9 DAY), 400000.00),
    ('reg-demo8-jock03', '88888888-8888-8888-8888-888888888808', 'bbbbbbbb-3333-3333-3333-333333333303', 'APPROVED', DATE_SUB(NOW(), INTERVAL 9 DAY), 700000.00),
    ('reg-demo8-jock04', '88888888-8888-8888-8888-888888888808', 'bbbbbbbb-3333-3333-3333-333333333304', 'APPROVED', DATE_SUB(NOW(), INTERVAL 9 DAY), 300000.00),
    ('reg-demo8-jock05', '88888888-8888-8888-8888-888888888808', 'bbbbbbbb-3333-3333-3333-333333333305', 'APPROVED', DATE_SUB(NOW(), INTERVAL 9 DAY), 500000.00),
    ('reg-demo8-jock06', '88888888-8888-8888-8888-888888888808', 'bbbbbbbb-3333-3333-3333-333333333306', 'APPROVED', DATE_SUB(NOW(), INTERVAL 9 DAY), 400000.00),
    ('reg-demo8-jock07', '88888888-8888-8888-8888-888888888808', 'bbbbbbbb-3333-3333-3333-333333333307', 'APPROVED', DATE_SUB(NOW(), INTERVAL 9 DAY), 600000.00),
    ('reg-demo8-jock08', '88888888-8888-8888-8888-888888888808', 'bbbbbbbb-3333-3333-3333-333333333308', 'APPROVED', DATE_SUB(NOW(), INTERVAL 9 DAY), 450000.00);

-- Hợp đồng đã ký
INSERT INTO jockey_horse_contracts (contract_id, tournament_id, horse_tournament_reg_id, jockey_tournament_reg_id, owner_id, horse_id, jockey_id, hire_fee, advance_percent, final_percent, advance_paid_amount, escrow_amount, system_contract_fee, owner_prize_share_percent, jockey_prize_share_percent, payment_status, escrow_status, advance_payout_status, final_payout_status, status, requested_at, accepted_at)
VALUES
    ('con-demo8-01', '88888888-8888-8888-8888-888888888808', 'reg-demo8-horse01', 'reg-demo8-jock01', 'aaaaaaaa-1111-1111-1111-111111111101', '99999999-9999-9999-9999-999999999901', 'bbbbbbbb-3333-3333-3333-333333333301', 500000.00, 50.0, 50.0, 250000.00, 250000.00, 20000.00, 80.0, 20.0, 'PAID', 'HELD', 'PAID', 'NOT_RELEASED', 'APPROVED', DATE_SUB(NOW(), INTERVAL 8 DAY), DATE_SUB(NOW(), INTERVAL 8 DAY)),
    ('con-demo8-02', '88888888-8888-8888-8888-888888888808', 'reg-demo8-horse02', 'reg-demo8-jock02', 'aaaaaaaa-1111-1111-1111-111111111101', '99999999-9999-9999-9999-999999999902', 'bbbbbbbb-3333-3333-3333-333333333302', 400000.00, 50.0, 50.0, 200000.00, 200000.00, 20000.00, 80.0, 20.0, 'PAID', 'HELD', 'PAID', 'NOT_RELEASED', 'APPROVED', DATE_SUB(NOW(), INTERVAL 8 DAY), DATE_SUB(NOW(), INTERVAL 8 DAY)),
    ('con-demo8-03', '88888888-8888-8888-8888-888888888808', 'reg-demo8-horse03', 'reg-demo8-jock03', 'aaaaaaaa-1111-1111-1111-111111111101', '99999999-9999-9999-9999-999999999903', 'bbbbbbbb-3333-3333-3333-333333333303', 700000.00, 50.0, 50.0, 350000.00, 350000.00, 20000.00, 85.0, 15.0, 'PAID', 'HELD', 'PAID', 'NOT_RELEASED', 'APPROVED', DATE_SUB(NOW(), INTERVAL 8 DAY), DATE_SUB(NOW(), INTERVAL 8 DAY)),
    ('con-demo8-04', '88888888-8888-8888-8888-888888888808', 'reg-demo8-horse04', 'reg-demo8-jock04', 'aaaaaaaa-1111-1111-1111-111111111102', '99999999-9999-9999-9999-999999999904', 'bbbbbbbb-3333-3333-3333-333333333304', 300000.00, 50.0, 50.0, 150000.00, 150000.00, 20000.00, 90.0, 10.0, 'PAID', 'HELD', 'PAID', 'NOT_RELEASED', 'APPROVED', DATE_SUB(NOW(), INTERVAL 8 DAY), DATE_SUB(NOW(), INTERVAL 8 DAY)),
    ('con-demo8-05', '88888888-8888-8888-8888-888888888808', 'reg-demo8-horse05', 'reg-demo8-jock05', 'aaaaaaaa-1111-1111-1111-111111111102', '99999999-9999-9999-9999-999999999905', 'bbbbbbbb-3333-3333-3333-333333333305', 500000.00, 50.0, 50.0, 250000.00, 250000.00, 20000.00, 80.0, 20.0, 'PAID', 'HELD', 'PAID', 'NOT_RELEASED', 'APPROVED', DATE_SUB(NOW(), INTERVAL 8 DAY), DATE_SUB(NOW(), INTERVAL 8 DAY)),
    ('con-demo8-06', '88888888-8888-8888-8888-888888888808', 'reg-demo8-horse06', 'reg-demo8-jock06', 'aaaaaaaa-1111-1111-1111-111111111102', '99999999-9999-9999-9999-999999999906', 'bbbbbbbb-3333-3333-3333-333333333306', 400000.00, 50.0, 50.0, 200000.00, 200000.00, 20000.00, 80.0, 20.0, 'PAID', 'HELD', 'PAID', 'NOT_RELEASED', 'APPROVED', DATE_SUB(NOW(), INTERVAL 8 DAY), DATE_SUB(NOW(), INTERVAL 8 DAY)),
    ('con-demo8-07', '88888888-8888-8888-8888-888888888808', 'reg-demo8-horse07', 'reg-demo8-jock07', 'aaaaaaaa-1111-1111-1111-111111111103', '99999999-9999-9999-9999-999999999907', 'bbbbbbbb-3333-3333-3333-333333333307', 600000.00, 50.0, 50.0, 300000.00, 300000.00, 20000.00, 85.0, 15.0, 'PAID', 'HELD', 'PAID', 'NOT_RELEASED', 'APPROVED', DATE_SUB(NOW(), INTERVAL 8 DAY), DATE_SUB(NOW(), INTERVAL 8 DAY)),
    ('con-demo8-08', '88888888-8888-8888-8888-888888888808', 'reg-demo8-horse08', 'reg-demo8-jock08', 'aaaaaaaa-1111-1111-1111-111111111103', '99999999-9999-9999-9999-999999999908', 'bbbbbbbb-3333-3333-3333-333333333308', 450000.00, 50.0, 50.0, 225000.00, 225000.00, 20000.00, 85.0, 15.0, 'PAID', 'HELD', 'PAID', 'NOT_RELEASED', 'APPROVED', DATE_SUB(NOW(), INTERVAL 8 DAY), DATE_SUB(NOW(), INTERVAL 8 DAY));

-- Khai báo vòng đấu giải Championship
INSERT INTO rounds (round_id, round_name, sequence_order, is_final, prediction_type, advancement_rule, start_date, end_date, description, max_races, max_entries, min_entries, status, head_referee_id, head_referee_assigned_at, expected_entries, planned_race_count, qualifiers_per_race, bracket_plan_version, transition_status, created_at, tournament_id, created_by)
VALUES 
    ('round-demo8-01', 'Round 1 - Semifinals', 1, 0, 'TOP1', 'Top 4 horses from each Semifinal advance to Finals', DATE_SUB(NOW(), INTERVAL 3 DAY), DATE_SUB(NOW(), INTERVAL 2 DAY), 'Bán kết', 2, 4, 4, 'COMPLETED', 'dddddddd-5555-5555-5555-555555555501', DATE_SUB(NOW(), INTERVAL 4 DAY), 8, 2, 4, 1, 'COMPLETED', DATE_SUB(NOW(), INTERVAL 4 DAY), '88888888-8888-8888-8888-888888888808', '11111111-1111-1111-1111-111111111111'),
    ('round-demo8-02', 'Round 2 - Finals', 2, 1, 'TOP3', 'Final championship race', DATE_SUB(NOW(), INTERVAL 1 DAY), DATE_ADD(NOW(), INTERVAL 1 DAY), 'Chung kết', 1, 8, 8, 'SCHEDULED', 'dddddddd-5555-5555-5555-555555555502', DATE_SUB(NOW(), INTERVAL 2 DAY), 8, 1, 0, 1, 'READY', DATE_SUB(NOW(), INTERVAL 2 DAY), '88888888-8888-8888-8888-888888888808', '11111111-1111-1111-1111-111111111111');

-- Trận Bán Kết A & B (Đã xong, có báo cáo Published để chuyển tiếp sang chung kết)
INSERT INTO races (race_id, name, start_time, end_time, track_condition, distance, sequence_order, status, started_at, finished_at, schedule_published_at, prediction_open_at, prediction_close_at, round_id, created_by, started_by)
VALUES 
    ('race-demo8-01', 'DEMO 8 - Semifinal A', DATE_SUB(NOW(), INTERVAL 3 DAY), DATE_SUB(NOW(), INTERVAL 3 DAY) + INTERVAL 30 MINUTE, 'Good', 'MILE_1600M', 1, 'COMPLETED', DATE_SUB(NOW(), INTERVAL 3 DAY), DATE_SUB(NOW(), INTERVAL 3 DAY) + INTERVAL 2 MINUTE, DATE_SUB(NOW(), INTERVAL 4 DAY), DATE_SUB(NOW(), INTERVAL 3 DAY) - INTERVAL 2 HOUR, DATE_SUB(NOW(), INTERVAL 3 DAY) - INTERVAL 5 MINUTE, 'round-demo8-01', '11111111-1111-1111-1111-111111111111', '11111111-1111-1111-1111-111111111111'),
    ('race-demo8-02', 'DEMO 8 - Semifinal B', DATE_SUB(NOW(), INTERVAL 3 DAY) + INTERVAL 1 HOUR, DATE_SUB(NOW(), INTERVAL 3 DAY) + INTERVAL 1 HOUR + INTERVAL 30 MINUTE, 'Good', 'MILE_1600M', 2, 'COMPLETED', DATE_SUB(NOW(), INTERVAL 3 DAY) + INTERVAL 1 HOUR, DATE_SUB(NOW(), INTERVAL 3 DAY) + INTERVAL 1 HOUR + INTERVAL 2 MINUTE, DATE_SUB(NOW(), INTERVAL 4 DAY), DATE_SUB(NOW(), INTERVAL 3 DAY) - INTERVAL 1 HOUR, DATE_SUB(NOW(), INTERVAL 3 DAY) + INTERVAL 55 MINUTE, 'round-demo8-01', '11111111-1111-1111-1111-111111111111', '11111111-1111-1111-1111-111111111111');

-- Trận Chung Kết (Sắp diễn ra - để test cược & khám bệnh)
INSERT INTO races (race_id, name, start_time, end_time, track_condition, distance, sequence_order, status, schedule_published_at, prediction_open_at, prediction_close_at, round_id, created_by)
VALUES (
    'race-demo8-03', 'DEMO Upcoming Race', DATE_ADD(NOW(), INTERVAL 3 HOUR), DATE_ADD(NOW(), INTERVAL 3 HOUR) + INTERVAL 30 MINUTE, 'Fast', 'MILE_1600M', 1, 'SCHEDULED', DATE_SUB(NOW(), INTERVAL 1 DAY),
    DATE_SUB(NOW(), INTERVAL 1 HOUR), DATE_ADD(NOW(), INTERVAL 2 HOUR) + INTERVAL 55 MINUTE, 'round-demo8-02', '11111111-1111-1111-1111-111111111111'
);

-- Xếp làn Bán kết A (Làn 1-4)
INSERT INTO race_entries (entry_id, race_id, contract_id, lane_number, status, assigned_by, assigned_at, created_at)
VALUES
    ('ent-demo8-01', 'race-demo8-01', 'con-demo8-01', 1, 'CONFIRMED', '11111111-1111-1111-1111-111111111111', DATE_SUB(NOW(), INTERVAL 4 DAY), DATE_SUB(NOW(), INTERVAL 4 DAY)),
    ('ent-demo8-02', 'race-demo8-01', 'con-demo8-02', 2, 'CONFIRMED', '11111111-1111-1111-1111-111111111111', DATE_SUB(NOW(), INTERVAL 4 DAY), DATE_SUB(NOW(), INTERVAL 4 DAY)),
    ('ent-demo8-03', 'race-demo8-01', 'con-demo8-03', 3, 'CONFIRMED', '11111111-1111-1111-1111-111111111111', DATE_SUB(NOW(), INTERVAL 4 DAY), DATE_SUB(NOW(), INTERVAL 4 DAY)),
    ('ent-demo8-04', 'race-demo8-01', 'con-demo8-04', 4, 'CONFIRMED', '11111111-1111-1111-1111-111111111111', DATE_SUB(NOW(), INTERVAL 4 DAY), DATE_SUB(NOW(), INTERVAL 4 DAY));

-- Xếp làn Bán kết B (Làn 1-4)
INSERT INTO race_entries (entry_id, race_id, contract_id, lane_number, status, assigned_by, assigned_at, created_at)
VALUES
    ('ent-demo8-05', 'race-demo8-02', 'con-demo8-05', 1, 'CONFIRMED', '11111111-1111-1111-1111-111111111111', DATE_SUB(NOW(), INTERVAL 4 DAY), DATE_SUB(NOW(), INTERVAL 4 DAY)),
    ('ent-demo8-06', 'race-demo8-02', 'con-demo8-06', 2, 'CONFIRMED', '11111111-1111-1111-1111-111111111111', DATE_SUB(NOW(), INTERVAL 4 DAY), DATE_SUB(NOW(), INTERVAL 4 DAY)),
    ('ent-demo8-07', 'race-demo8-02', 'con-demo8-07', 3, 'CONFIRMED', '11111111-1111-1111-1111-111111111111', DATE_SUB(NOW(), INTERVAL 4 DAY), DATE_SUB(NOW(), INTERVAL 4 DAY)),
    ('ent-demo8-08', 'race-demo8-02', 'con-demo8-08', 4, 'CONFIRMED', '11111111-1111-1111-1111-111111111111', DATE_SUB(NOW(), INTERVAL 4 DAY), DATE_SUB(NOW(), INTERVAL 4 DAY));

-- Xếp làn Chung kết (Làn 1-8) đại diện cho top 4 của bán kết A & B đã đi tiếp
INSERT INTO race_entries (entry_id, race_id, contract_id, lane_number, status, assigned_by, assigned_at, created_at)
VALUES
    ('ent-demo8-09', 'race-demo8-03', 'con-demo8-01', 1, 'CONFIRMED', '11111111-1111-1111-1111-111111111111', DATE_SUB(NOW(), INTERVAL 1 DAY), NOW()),
    ('ent-demo8-10', 'race-demo8-03', 'con-demo8-02', 2, 'CONFIRMED', '11111111-1111-1111-1111-111111111111', DATE_SUB(NOW(), INTERVAL 1 DAY), NOW()),
    ('ent-demo8-11', 'race-demo8-03', 'con-demo8-03', 3, 'CONFIRMED', '11111111-1111-1111-1111-111111111111', DATE_SUB(NOW(), INTERVAL 1 DAY), NOW()),
    ('ent-demo8-12', 'race-demo8-03', 'con-demo8-04', 4, 'CONFIRMED', '11111111-1111-1111-1111-111111111111', DATE_SUB(NOW(), INTERVAL 1 DAY), NOW()),
    ('ent-demo8-13', 'race-demo8-03', 'con-demo8-05', 5, 'CONFIRMED', '11111111-1111-1111-1111-111111111111', DATE_SUB(NOW(), INTERVAL 1 DAY), NOW()),
    ('ent-demo8-14', 'race-demo8-03', 'con-demo8-06', 6, 'CONFIRMED', '11111111-1111-1111-1111-111111111111', DATE_SUB(NOW(), INTERVAL 1 DAY), NOW()),
    ('ent-demo8-15', 'race-demo8-03', 'con-demo8-07', 7, 'CONFIRMED', '11111111-1111-1111-1111-111111111111', DATE_SUB(NOW(), INTERVAL 1 DAY), NOW()),
    ('ent-demo8-16', 'race-demo8-03', 'con-demo8-08', 8, 'CONFIRMED', '11111111-1111-1111-1111-111111111111', DATE_SUB(NOW(), INTERVAL 1 DAY), NOW());

-- Khám y tế Vòng 1 (PASS toàn bộ)
INSERT INTO horse_inspections (horse_inspection_id, entry_id, vet_id, result, note, inspected_at, handicap_weight, is_handicap_confirmed, status)
VALUES
    ('ins-demo8-h01', 'ent-demo8-01', 'eeeeeeee-6666-6666-6666-666666666601', 'PASS', 'Healthy.', DATE_SUB(NOW(), INTERVAL 3 DAY) - INTERVAL 1 HOUR, 0.0, 0, 'CONFIRMED'),
    ('ins-demo8-h02', 'ent-demo8-02', 'eeeeeeee-6666-6666-6666-666666666601', 'PASS', 'Healthy.', DATE_SUB(NOW(), INTERVAL 3 DAY) - INTERVAL 1 HOUR, 0.0, 0, 'CONFIRMED'),
    ('ins-demo8-h03', 'ent-demo8-03', 'eeeeeeee-6666-6666-6666-666666666601', 'PASS', 'Healthy.', DATE_SUB(NOW(), INTERVAL 3 DAY) - INTERVAL 1 HOUR, 0.0, 0, 'CONFIRMED'),
    ('ins-demo8-h04', 'ent-demo8-04', 'eeeeeeee-6666-6666-6666-666666666601', 'PASS', 'Healthy.', DATE_SUB(NOW(), INTERVAL 3 DAY) - INTERVAL 1 HOUR, 0.0, 0, 'CONFIRMED'),
    ('ins-demo8-h05', 'ent-demo8-05', 'eeeeeeee-6666-6666-6666-666666666602', 'PASS', 'Healthy.', DATE_SUB(NOW(), INTERVAL 3 DAY), 0.0, 0, 'CONFIRMED'),
    ('ins-demo8-h06', 'ent-demo8-06', 'eeeeeeee-6666-6666-6666-666666666602', 'PASS', 'Healthy.', DATE_SUB(NOW(), INTERVAL 3 DAY), 0.0, 0, 'CONFIRMED'),
    ('ins-demo8-h07', 'ent-demo8-07', 'eeeeeeee-6666-6666-6666-666666666602', 'PASS', 'Healthy.', DATE_SUB(NOW(), INTERVAL 3 DAY), 0.0, 0, 'CONFIRMED'),
    ('ins-demo8-h08', 'ent-demo8-08', 'eeeeeeee-6666-6666-6666-666666666602', 'PASS', 'Healthy.', DATE_SUB(NOW(), INTERVAL 3 DAY), 0.0, 0, 'CONFIRMED');

INSERT INTO jockey_inspections (jockey_inspection_id, entry_id, med_staff_id, result, note, inspected_at, status)
VALUES
    ('ins-demo8-j01', 'ent-demo8-01', 'ffffffff-7777-7777-7777-777777777701', 'PASS', 'Normal.', DATE_SUB(NOW(), INTERVAL 3 DAY) - INTERVAL 1 HOUR, 'CONFIRMED'),
    ('ins-demo8-j02', 'ent-demo8-02', 'ffffffff-7777-7777-7777-777777777701', 'PASS', 'Normal.', DATE_SUB(NOW(), INTERVAL 3 DAY) - INTERVAL 1 HOUR, 'CONFIRMED'),
    ('ins-demo8-j03', 'ent-demo8-03', 'ffffffff-7777-7777-7777-777777777701', 'PASS', 'Normal.', DATE_SUB(NOW(), INTERVAL 3 DAY) - INTERVAL 1 HOUR, 'CONFIRMED'),
    ('ins-demo8-j04', 'ent-demo8-04', 'ffffffff-7777-7777-7777-777777777701', 'PASS', 'Normal.', DATE_SUB(NOW(), INTERVAL 3 DAY) - INTERVAL 1 HOUR, 'CONFIRMED'),
    ('ins-demo8-j05', 'ent-demo8-05', 'ffffffff-7777-7777-7777-777777777702', 'PASS', 'Normal.', DATE_SUB(NOW(), INTERVAL 3 DAY), 'CONFIRMED'),
    ('ins-demo8-j06', 'ent-demo8-06', 'ffffffff-7777-7777-7777-777777777702', 'PASS', 'Normal.', DATE_SUB(NOW(), INTERVAL 3 DAY), 'CONFIRMED'),
    ('ins-demo8-j07', 'ent-demo8-07', 'ffffffff-7777-7777-7777-777777777702', 'PASS', 'Normal.', DATE_SUB(NOW(), INTERVAL 3 DAY), 'CONFIRMED'),
    ('ins-demo8-j08', 'ent-demo8-08', 'ffffffff-7777-7777-7777-777777777702', 'PASS', 'Normal.', DATE_SUB(NOW(), INTERVAL 3 DAY), 'CONFIRMED');

-- Khám y tế Chung kết (Để sẵn 1 bản nháp SUBMITTED cho Vet 1 & Medical 1 khám các làn còn lại)
INSERT INTO horse_inspections (horse_inspection_id, entry_id, vet_id, result, note, inspected_at, handicap_weight, is_handicap_confirmed, status)
VALUES ('ins-demo8-final-h01', 'ent-demo8-09', 'eeeeeeee-6666-6666-6666-666666666601', 'PASS', 'Passed cardiac check.', NOW(), 0.0, 0, 'SUBMITTED');

INSERT INTO jockey_inspections (jockey_inspection_id, entry_id, med_staff_id, result, note, inspected_at, status)
VALUES ('ins-demo8-final-j01', 'ent-demo8-09', 'ffffffff-7777-7777-7777-777777777701', 'PASS', 'Normal blood pressure.', NOW(), 'SUBMITTED');

-- Lưu kết quả bán kết (Mỗi race chọn 4 làn thắng cuộc)
INSERT INTO race_results (result_id, race_id, entry_id, finish_time, finish_position, prize_money, owner_prize_amount, jockey_prize_amount, prize_status, is_prize_paid, status, recorded_by, recorded_at, updated_at)
VALUES
    ('res-demo8-01', 'race-demo8-01', 'ent-demo8-01', 95.20, 1, 0.00, 0.00, 0.00, 'NotEligible', 0, 'FINISHED', '55555555-5555-5555-5555-555555555501', DATE_SUB(NOW(), INTERVAL 3 DAY), DATE_SUB(NOW(), INTERVAL 3 DAY)),
    ('res-demo8-02', 'race-demo8-01', 'ent-demo8-02', 96.50, 2, 0.00, 0.00, 0.00, 'NotEligible', 0, 'FINISHED', '55555555-5555-5555-5555-555555555501', DATE_SUB(NOW(), INTERVAL 3 DAY), DATE_SUB(NOW(), INTERVAL 3 DAY)),
    ('res-demo8-03', 'race-demo8-01', 'ent-demo8-03', 98.10, 3, 0.00, 0.00, 0.00, 'NotEligible', 0, 'FINISHED', '55555555-5555-5555-5555-555555555501', DATE_SUB(NOW(), INTERVAL 3 DAY), DATE_SUB(NOW(), INTERVAL 3 DAY)),
    ('res-demo8-04', 'race-demo8-01', 'ent-demo8-04', 101.40, 4, 0.00, 0.00, 0.00, 'NotEligible', 0, 'FINISHED', '55555555-5555-5555-5555-555555555501', DATE_SUB(NOW(), INTERVAL 3 DAY), DATE_SUB(NOW(), INTERVAL 3 DAY)),
    ('res-demo8-05', 'race-demo8-02', 'ent-demo8-05', 94.80, 1, 0.00, 0.00, 0.00, 'NotEligible', 0, 'FINISHED', '55555555-5555-5555-5555-555555555501', DATE_SUB(NOW(), INTERVAL 3 DAY), DATE_SUB(NOW(), INTERVAL 3 DAY)),
    ('res-demo8-06', 'race-demo8-02', 'ent-demo8-06', 95.90, 2, 0.00, 0.00, 0.00, 'NotEligible', 0, 'FINISHED', '55555555-5555-5555-5555-555555555501', DATE_SUB(NOW(), INTERVAL 3 DAY), DATE_SUB(NOW(), INTERVAL 3 DAY)),
    ('res-demo8-07', 'race-demo8-02', 'ent-demo8-07', 97.40, 3, 0.00, 0.00, 0.00, 'NotEligible', 0, 'FINISHED', '55555555-5555-5555-5555-555555555501', DATE_SUB(NOW(), INTERVAL 3 DAY), DATE_SUB(NOW(), INTERVAL 3 DAY)),
    ('res-demo8-08', 'race-demo8-02', 'ent-demo8-08', 99.80, 4, 0.00, 0.00, 0.00, 'NotEligible', 0, 'FINISHED', '55555555-5555-5555-5555-555555555501', DATE_SUB(NOW(), INTERVAL 3 DAY), DATE_SUB(NOW(), INTERVAL 3 DAY));

-- Công bố báo cáo để Vòng 1 hoàn tất chuyển tiếp sang Vòng 2
INSERT INTO race_reports (report_id, race_id, referee_id, summary, status, signed_by, signed_at, published_by, published_at, created_at)
VALUES
    ('rep-demo8-01', 'race-demo8-01', 'dddddddd-5555-5555-5555-555555555501', 'Trận bán kết A kết thúc hợp lệ. Không có khiếu nại.', 'Published', 'dddddddd-5555-5555-5555-555555555501', DATE_SUB(NOW(), INTERVAL 3 DAY), '11111111-1111-1111-1111-111111111111', DATE_SUB(NOW(), INTERVAL 3 DAY), DATE_SUB(NOW(), INTERVAL 3 DAY)),
    ('rep-demo8-02', 'race-demo8-02', 'dddddddd-5555-5555-5555-555555555501', 'Trận bán kết B kết thúc hợp lệ. Không có khiếu nại.', 'Published', 'dddddddd-5555-5555-5555-555555555501', DATE_SUB(NOW(), INTERVAL 3 DAY), '11111111-1111-1111-1111-111111111111', DATE_SUB(NOW(), INTERVAL 3 DAY), DATE_SUB(NOW(), INTERVAL 3 DAY));

-- Tạo sẵn dự đoán của khán giả spectator1 trên trận chung kết sắp diễn ra (Race 3)
INSERT INTO predictions (prediction_id, spectator_id, race_id, prediction_type, prediction_time, status, reward_points)
VALUES ('pred-demo8-01', 'cccccccc-1111-1111-1111-111111111101', 'race-demo8-03', 'TOP3', DATE_SUB(NOW(), INTERVAL 30 MINUTE), 'PENDING', 0);

INSERT INTO prediction_detail (prediction_detail_id, prediction_id, entry_id, predicted_rank, status, awarded_points)
VALUES 
    ('pred-dt-demo8-01', 'pred-demo8-01', 'ent-demo8-09', 1, 'UNSCORED', 0),
    ('pred-dt-demo8-02', 'pred-demo8-01', 'ent-demo8-10', 2, 'UNSCORED', 0),
    ('pred-dt-demo8-03', 'pred-demo8-01', 'ent-demo8-11', 3, 'UNSCORED', 0);


-- Kịch bản E: Giải đấu "DEMO 2 - Final chờ publish" (Phase: RESULT_PENDING)
-- Phục vụ test: Trọng tài (referee1) đã nhập kết quả trận chung kết và ĐÃ KÝ (Signed) báo cáo kết quả.
-- Admin (admin1) vào kiểm tra thay đổi Rating và bấm công bố (Publish) báo cáo để:
-- Hệ thống tự chia giải thưởng (Prize money) chuyển ví, trả nốt 70% lương nài, và chấm điểm dự đoán.
INSERT INTO tournaments (tournament_id, name, description, start_date, end_date, location, registration_fee, system_contract_fee, total_prize_pool, allowed_breed, min_horse_age, max_horse_age, registration_open_at, registration_close_at, review_deadline_at, jockey_matching_deadline_at, scheduling_deadline_at, competition_start_at, status, phase, created_by, max_approved_horses, max_approved_jockeys, max_approved_entries, distance)
VALUES (
    '88888888-8888-8888-8888-888888888802',
    'DEMO 2 - Final chờ publish',
    'Giải đấu mẫu đã kết thúc đua chung kết, báo cáo kết quả ở dạng Signed chờ Admin duyệt và công bố.',
    DATE_SUB(CURDATE(), INTERVAL 4 DAY),
    DATE_SUB(CURDATE(), INTERVAL 1 DAY),
    'Saigon Turf Racing Arena',
    1000000.00,
    200000.00,
    50000000.00,
    'THOROUGHBRED',
    4, 10,
    DATE_SUB(NOW(), INTERVAL 15 DAY),
    DATE_SUB(NOW(), INTERVAL 12 DAY),
    DATE_SUB(NOW(), INTERVAL 11 DAY),
    DATE_SUB(NOW(), INTERVAL 10 DAY),
    DATE_SUB(NOW(), INTERVAL 9 DAY),
    DATE_SUB(NOW(), INTERVAL 8 DAY),
    'ONGOING',
    'RACING',
    '11111111-1111-1111-1111-111111111111',
    16, 16, 16,
    'MILE_1600M'
);

-- Cấu hình giải thưởng giải DEMO 2
INSERT INTO prize_structures (prize_structure_id, `prize_rank`, percentage, fixed_amount, is_active, tournament_id)
VALUES
    ('p-demo2-01', 1, 60.0, 30000000.00, 1, '88888888-8888-8888-8888-888888888802'),
    ('p-demo2-02', 2, 25.0, 12500000.00, 1, '88888888-8888-8888-8888-888888888802'),
    ('p-demo2-03', 3, 15.0, 7500000.00, 1, '88888888-8888-8888-8888-888888888802');

-- Đăng ký hồ sơ APPROVED
INSERT INTO horse_tournament_registrations (horse_tournament_reg_id, tournament_id, horse_id, owner_id, status, submitted_at, rating_at_registration, race_class_at_registration)
VALUES
    ('reg-demo2-horse01', '88888888-8888-8888-8888-888888888802', '99999999-9999-9999-9999-999999999901', 'aaaaaaaa-1111-1111-1111-111111111101', 'APPROVED', DATE_SUB(NOW(), INTERVAL 14 DAY), 45, 'CLASS_4'),
    ('reg-demo2-horse02', '88888888-8888-8888-8888-888888888802', '99999999-9999-9999-9999-999999999902', 'aaaaaaaa-1111-1111-1111-111111111101', 'APPROVED', DATE_SUB(NOW(), INTERVAL 14 DAY), 55, 'CLASS_4'),
    ('reg-demo2-horse03', '88888888-8888-8888-8888-888888888802', '99999999-9999-9999-9999-999999999903', 'aaaaaaaa-1111-1111-1111-111111111101', 'APPROVED', DATE_SUB(NOW(), INTERVAL 14 DAY), 35, 'CLASS_5'),
    ('reg-demo2-horse04', '88888888-8888-8888-8888-888888888802', '99999999-9999-9999-9999-999999999904', 'aaaaaaaa-1111-1111-1111-111111111102', 'APPROVED', DATE_SUB(NOW(), INTERVAL 14 DAY), 62, 'CLASS_3');

-- Đăng ký Nài
INSERT INTO jockey_tournament_registrations (jockey_tournament_reg_id, tournament_id, jockey_id, status, submitted_at, hire_fee)
VALUES
    ('reg-demo2-jock01', '88888888-8888-8888-8888-888888888802', 'bbbbbbbb-3333-3333-3333-333333333301', 'APPROVED', DATE_SUB(NOW(), INTERVAL 14 DAY), 500000.00),
    ('reg-demo2-jock02', '88888888-8888-8888-8888-888888888802', 'bbbbbbbb-3333-3333-3333-333333333302', 'APPROVED', DATE_SUB(NOW(), INTERVAL 14 DAY), 400000.00),
    ('reg-demo2-jock03', '88888888-8888-8888-8888-888888888802', 'bbbbbbbb-3333-3333-3333-333333333303', 'APPROVED', DATE_SUB(NOW(), INTERVAL 14 DAY), 700000.00),
    ('reg-demo2-jock04', '88888888-8888-8888-8888-888888888802', 'bbbbbbbb-3333-3333-3333-333333333304', 'APPROVED', DATE_SUB(NOW(), INTERVAL 14 DAY), 300000.00);

-- Hợp đồng giải DEMO 2
INSERT INTO jockey_horse_contracts (contract_id, tournament_id, horse_tournament_reg_id, jockey_tournament_reg_id, owner_id, horse_id, jockey_id, hire_fee, advance_percent, final_percent, advance_paid_amount, escrow_amount, system_contract_fee, owner_prize_share_percent, jockey_prize_share_percent, payment_status, escrow_status, advance_payout_status, final_payout_status, status, requested_at, accepted_at)
VALUES
    ('con-demo2-01', '88888888-8888-8888-8888-888888888802', 'reg-demo2-horse01', 'reg-demo2-jock01', 'aaaaaaaa-1111-1111-1111-111111111101', '99999999-9999-9999-9999-999999999901', 'bbbbbbbb-3333-3333-3333-333333333301', 500000.00, 50.0, 50.0, 250000.00, 250000.00, 20000.00, 80.0, 20.0, 'PAID', 'HELD', 'PAID', 'NOT_RELEASED', 'APPROVED', DATE_SUB(NOW(), INTERVAL 13 DAY), DATE_SUB(NOW(), INTERVAL 13 DAY)),
    ('con-demo2-02', '88888888-8888-8888-8888-888888888802', 'reg-demo2-horse02', 'reg-demo2-jock02', 'aaaaaaaa-1111-1111-1111-111111111101', '99999999-9999-9999-9999-999999999902', 'bbbbbbbb-3333-3333-3333-333333333302', 400000.00, 50.0, 50.0, 200000.00, 200000.00, 20000.00, 80.0, 20.0, 'PAID', 'HELD', 'PAID', 'NOT_RELEASED', 'APPROVED', DATE_SUB(NOW(), INTERVAL 13 DAY), DATE_SUB(NOW(), INTERVAL 13 DAY)),
    ('con-demo2-03', '88888888-8888-8888-8888-888888888802', 'reg-demo2-horse03', 'reg-demo2-jock03', 'aaaaaaaa-1111-1111-1111-111111111101', '99999999-9999-9999-9999-999999999903', 'bbbbbbbb-3333-3333-3333-333333333303', 700000.00, 50.0, 50.0, 350000.00, 350000.00, 20000.00, 85.0, 15.0, 'PAID', 'HELD', 'PAID', 'NOT_RELEASED', 'APPROVED', DATE_SUB(NOW(), INTERVAL 13 DAY), DATE_SUB(NOW(), INTERVAL 13 DAY)),
    ('con-demo2-04', '88888888-8888-8888-8888-888888888802', 'reg-demo2-horse04', 'reg-demo2-jock04', 'aaaaaaaa-1111-1111-1111-111111111102', '99999999-9999-9999-9999-999999999904', 'bbbbbbbb-3333-3333-3333-333333333304', 300000.00, 50.0, 50.0, 150000.00, 150000.00, 20000.00, 90.0, 10.0, 'PAID', 'HELD', 'PAID', 'NOT_RELEASED', 'APPROVED', DATE_SUB(NOW(), INTERVAL 13 DAY), DATE_SUB(NOW(), INTERVAL 13 DAY));

-- Vòng đấu và Trận Chung kết
INSERT INTO rounds (round_id, round_name, sequence_order, is_final, prediction_type, advancement_rule, start_date, end_date, description, max_races, max_entries, min_entries, status, head_referee_id, head_referee_assigned_at, expected_entries, planned_race_count, qualifiers_per_race, bracket_plan_version, transition_status, created_at, tournament_id, created_by)
VALUES ('round-demo2-01', 'Chung kết', 1, 1, 'TOP3', 'Chung kết chung cuộc', DATE_SUB(NOW(), INTERVAL 1 DAY), DATE_ADD(NOW(), INTERVAL 1 DAY), 'Final', 1, 4, 4, 'FINISHED', 'dddddddd-5555-5555-5555-555555555501', NOW(), 4, 1, 0, 1, 'NOT_READY', NOW(), '88888888-8888-8888-8888-888888888802', '11111111-1111-1111-1111-111111111111');

INSERT INTO races (race_id, name, start_time, end_time, track_condition, distance, sequence_order, status, started_at, finished_at, schedule_published_at, prediction_open_at, prediction_close_at, round_id, created_by, started_by)
VALUES ('race-demo2-01', 'DEMO 2 - Final Race', DATE_SUB(NOW(), INTERVAL 1 DAY), DATE_SUB(NOW(), INTERVAL 1 DAY) + INTERVAL 30 MINUTE, 'Good', 'MILE_1600M', 1, 'FINISHED', DATE_SUB(NOW(), INTERVAL 1 DAY), DATE_SUB(NOW(), INTERVAL 1 DAY) + INTERVAL 2 MINUTE, DATE_SUB(NOW(), INTERVAL 2 DAY), DATE_SUB(NOW(), INTERVAL 1 DAY) - INTERVAL 2 HOUR, DATE_SUB(NOW(), INTERVAL 1 DAY) - INTERVAL 5 MINUTE, 'round-demo2-01', '11111111-1111-1111-1111-111111111111', '11111111-1111-1111-1111-111111111111');

-- Entry và Kiểm tra y tế
INSERT INTO race_entries (entry_id, race_id, contract_id, lane_number, status, assigned_by, assigned_at, created_at)
VALUES
    ('ent-demo2-01', 'race-demo2-01', 'con-demo2-01', 1, 'CONFIRMED', '11111111-1111-1111-1111-111111111111', DATE_SUB(NOW(), INTERVAL 3 DAY), DATE_SUB(NOW(), INTERVAL 3 DAY)),
    ('ent-demo2-02', 'race-demo2-01', 'con-demo2-02', 2, 'CONFIRMED', '11111111-1111-1111-1111-111111111111', DATE_SUB(NOW(), INTERVAL 3 DAY), DATE_SUB(NOW(), INTERVAL 3 DAY)),
    ('ent-demo2-03', 'race-demo2-01', 'con-demo2-03', 3, 'CONFIRMED', '11111111-1111-1111-1111-111111111111', DATE_SUB(NOW(), INTERVAL 3 DAY), DATE_SUB(NOW(), INTERVAL 3 DAY)),
    ('ent-demo2-04', 'race-demo2-01', 'con-demo2-04', 4, 'CONFIRMED', '11111111-1111-1111-1111-111111111111', DATE_SUB(NOW(), INTERVAL 3 DAY), DATE_SUB(NOW(), INTERVAL 3 DAY));

INSERT INTO horse_inspections (horse_inspection_id, entry_id, vet_id, result, note, inspected_at, handicap_weight, is_handicap_confirmed, status)
VALUES
    ('ins-demo2-h01', 'ent-demo2-01', 'eeeeeeee-6666-6666-6666-666666666601', 'PASS', 'Healthy.', DATE_SUB(NOW(), INTERVAL 1 DAY) - INTERVAL 1 HOUR, 0.0, 0, 'CONFIRMED'),
    ('ins-demo2-h02', 'ent-demo2-02', 'eeeeeeee-6666-6666-6666-666666666601', 'PASS', 'Healthy.', DATE_SUB(NOW(), INTERVAL 1 DAY) - INTERVAL 1 HOUR, 0.0, 0, 'CONFIRMED'),
    ('ins-demo2-h03', 'ent-demo2-03', 'eeeeeeee-6666-6666-6666-666666666601', 'PASS', 'Healthy.', DATE_SUB(NOW(), INTERVAL 1 DAY) - INTERVAL 1 HOUR, 0.0, 0, 'CONFIRMED'),
    ('ins-demo2-h04', 'ent-demo2-04', 'eeeeeeee-6666-6666-6666-666666666601', 'PASS', 'Healthy.', DATE_SUB(NOW(), INTERVAL 1 DAY) - INTERVAL 1 HOUR, 0.0, 0, 'CONFIRMED');

INSERT INTO jockey_inspections (jockey_inspection_id, entry_id, med_staff_id, result, note, inspected_at, status)
VALUES
    ('ins-demo2-j01', 'ent-demo2-01', 'ffffffff-7777-7777-7777-777777777701', 'PASS', 'Normal.', DATE_SUB(NOW(), INTERVAL 1 DAY) - INTERVAL 1 HOUR, 'CONFIRMED'),
    ('ins-demo2-j02', 'ent-demo2-02', 'ffffffff-7777-7777-7777-777777777701', 'PASS', 'Normal.', DATE_SUB(NOW(), INTERVAL 1 DAY) - INTERVAL 1 HOUR, 'CONFIRMED'),
    ('ins-demo2-j03', 'ent-demo2-03', 'ffffffff-7777-7777-7777-777777777701', 'PASS', 'Normal.', DATE_SUB(NOW(), INTERVAL 1 DAY) - INTERVAL 1 HOUR, 'CONFIRMED'),
    ('ins-demo2-j04', 'ent-demo2-04', 'ffffffff-7777-7777-7777-777777777701', 'PASS', 'Normal.', DATE_SUB(NOW(), INTERVAL 1 DAY) - INTERVAL 1 HOUR, 'CONFIRMED');

-- Kết quả đua (1st: con1, 2nd: con2, 3rd: con3, 4th: con4)
INSERT INTO race_results (result_id, race_id, entry_id, finish_time, finish_position, prize_money, owner_prize_amount, jockey_prize_amount, prize_status, is_prize_paid, status, recorded_by, recorded_at, updated_at)
VALUES
    ('res-demo2-01', 'race-demo2-01', 'ent-demo2-01', 95.20, 1, 30000000.00, 24000000.00, 6000000.00, 'PendingPayout', 0, 'FINISHED', '55555555-5555-5555-5555-555555555501', DATE_SUB(NOW(), INTERVAL 1 DAY), DATE_SUB(NOW(), INTERVAL 1 DAY)),
    ('res-demo2-02', 'race-demo2-01', 'ent-demo2-02', 96.50, 2, 12500000.00, 10000000.00, 2500000.00, 'PendingPayout', 0, 'FINISHED', '55555555-5555-5555-5555-555555555501', DATE_SUB(NOW(), INTERVAL 1 DAY), DATE_SUB(NOW(), INTERVAL 1 DAY)),
    ('res-demo2-03', 'race-demo2-01', 'ent-demo2-03', 98.10, 3, 7500000.00, 6375000.00, 1125000.00, 'PendingPayout', 0, 'FINISHED', '55555555-5555-5555-5555-555555555501', DATE_SUB(NOW(), INTERVAL 1 DAY), DATE_SUB(NOW(), INTERVAL 1 DAY)),
    ('res-demo2-04', 'race-demo2-01', 'ent-demo2-04', 101.40, 4, 0.00, 0.00, 0.00, 'NotEligible', 0, 'FINISHED', '55555555-5555-5555-5555-555555555501', DATE_SUB(NOW(), INTERVAL 1 DAY), DATE_SUB(NOW(), INTERVAL 1 DAY));

-- Báo cáo kết quả đua ở dạng 'Signed' (để Admin duyệt và Publish kết quả chung cuộc)
INSERT INTO race_reports (report_id, race_id, referee_id, summary, status, signed_by, signed_at, created_at)
VALUES ('rep-demo2-01', 'race-demo2-01', 'dddddddd-5555-5555-5555-555555555501', 'Trận chung kết kết thúc tốt đẹp. Thứ tự 1, 2, 3 được phân chia rõ ràng. Chờ Admin công bố để trao thưởng.', 'Signed', 'dddddddd-5555-5555-5555-555555555501', DATE_SUB(NOW(), INTERVAL 1 DAY), DATE_SUB(NOW(), INTERVAL 1 DAY));

-- Dự đoán cược của spectator2 ở giải DEMO 2 để kiểm thử tính năng tính điểm sau khi publish
INSERT INTO predictions (prediction_id, spectator_id, race_id, prediction_type, prediction_time, status, reward_points)
VALUES ('pred-demo2-02', 'cccccccc-1111-1111-1111-111111111102', 'race-demo2-01', 'TOP3', DATE_SUB(NOW(), INTERVAL 1 DAY) - INTERVAL 1 HOUR, 'PENDING', 0);

INSERT INTO prediction_detail (prediction_detail_id, prediction_id, entry_id, predicted_rank, status, awarded_points)
VALUES 
    ('pred-dt-demo2-01', 'pred-demo2-02', 'ent-demo2-01', 1, 'UNSCORED', 0),
    ('pred-dt-demo2-02', 'pred-demo2-02', 'ent-demo2-02', 2, 'UNSCORED', 0),
    ('pred-dt-demo2-03', 'pred-demo2-02', 'ent-demo2-03', 3, 'UNSCORED', 0);


-- Kịch bản F: Giải đấu "DEMO 4 - Bracket 32 chuyển vòng" (Phase: RACING)
-- Phục vụ test: Đã chạy xong 2 trận Bán kết A & B. Trận bán kết A đã Publish báo cáo.
-- Trận bán kết B chỉ mới Signed báo cáo. Khi Admin bấm Publish báo cáo trận bán kết B,
-- Hệ thống sẽ tự động chuyển tiếp vòng (Round Transition) và xếp 8 ngựa đi tiếp vào Chung kết trong 1 Transaction nguyên tử (Atomic).
INSERT INTO tournaments (tournament_id, name, description, start_date, end_date, location, registration_fee, system_contract_fee, total_prize_pool, allowed_breed, min_horse_age, max_horse_age, registration_open_at, registration_close_at, review_deadline_at, jockey_matching_deadline_at, scheduling_deadline_at, competition_start_at, status, phase, created_by, max_approved_horses, max_approved_jockeys, max_approved_entries, distance)
VALUES (
    '88888888-8888-8888-8888-888888888804',
    'DEMO 4 - Bracket 32 chuyển vòng',
    'Tournament test chuyển vòng bán kết lên chung kết.',
    DATE_SUB(CURDATE(), INTERVAL 3 DAY),
    DATE_ADD(CURDATE(), INTERVAL 3 DAY),
    'Hanoi National Turf Club',
    1000000.00,
    200000.00,
    50000000.00,
    'THOROUGHBRED',
    4, 10,
    DATE_SUB(NOW(), INTERVAL 15 DAY),
    DATE_SUB(NOW(), INTERVAL 12 DAY),
    DATE_SUB(NOW(), INTERVAL 11 DAY),
    DATE_SUB(NOW(), INTERVAL 10 DAY),
    DATE_SUB(NOW(), INTERVAL 9 DAY),
    DATE_SUB(NOW(), INTERVAL 8 DAY),
    'ONGOING',
    'RACING',
    '11111111-1111-1111-1111-111111111111',
    16, 16, 16,
    'MILE_1600M'
);

-- Vòng 1 (Bán kết) & Vòng 2 (Chung kết) giải DEMO 4
INSERT INTO rounds (round_id, round_name, sequence_order, is_final, prediction_type, advancement_rule, start_date, end_date, description, max_races, max_entries, min_entries, status, head_referee_id, head_referee_assigned_at, expected_entries, planned_race_count, qualifiers_per_race, bracket_plan_version, transition_status, created_at, tournament_id, created_by)
VALUES 
    ('round-demo4-01', 'Round 1 - Semifinals', 1, 0, 'TOP1', 'Top 4 đi tiếp', DATE_SUB(NOW(), INTERVAL 2 DAY), DATE_SUB(NOW(), INTERVAL 1 DAY), 'Semi-finals', 2, 4, 4, 'SCHEDULED', 'dddddddd-5555-5555-5555-555555555501', NOW(), 8, 2, 4, 1, 'NOT_READY', NOW(), '88888888-8888-8888-8888-888888888804', '11111111-1111-1111-1111-111111111111'),
    ('round-demo4-02', 'Round 2 - Finals', 2, 1, 'TOP3', 'Chung kết tranh giải', DATE_SUB(NOW(), INTERVAL 1 DAY), DATE_ADD(NOW(), INTERVAL 2 DAY), 'Chung kết', 1, 8, 8, 'SCHEDULING', 'dddddddd-5555-5555-5555-555555555501', NOW(), 8, 1, 0, 1, 'NOT_READY', NOW(), '88888888-8888-8888-8888-888888888804', '11111111-1111-1111-1111-111111111111');

-- Trận Bán kết A & B giải DEMO 4 (Đều ở trạng thái FINISHED)
INSERT INTO races (race_id, name, start_time, end_time, track_condition, distance, sequence_order, status, started_at, finished_at, round_id, created_by)
VALUES 
    ('race-demo4-01', 'DEMO 4 - Semifinal A', DATE_SUB(NOW(), INTERVAL 2 DAY), DATE_SUB(NOW(), INTERVAL 2 DAY) + INTERVAL 30 MINUTE, 'Good', 'MILE_1600M', 1, 'FINISHED', DATE_SUB(NOW(), INTERVAL 2 DAY), DATE_SUB(NOW(), INTERVAL 2 DAY) + INTERVAL 2 MINUTE, 'round-demo4-01', '11111111-1111-1111-1111-111111111111'),
    ('race-demo4-02', 'DEMO 4 - Semifinal B', DATE_SUB(NOW(), INTERVAL 2 DAY) + INTERVAL 1 HOUR, DATE_SUB(NOW(), INTERVAL 2 DAY) + INTERVAL 1 HOUR + INTERVAL 30 MINUTE, 'Good', 'MILE_1600M', 2, 'FINISHED', DATE_SUB(NOW(), INTERVAL 2 DAY) + INTERVAL 1 HOUR, DATE_SUB(NOW(), INTERVAL 2 DAY) + INTERVAL 1 HOUR + INTERVAL 2 MINUTE, 'round-demo4-01', '11111111-1111-1111-1111-111111111111');

-- Trận Chung kết giải DEMO 4 (Chưa xếp lịch, chưa có entry)
INSERT INTO races (race_id, name, start_time, end_time, track_condition, distance, sequence_order, status, round_id, created_by)
VALUES ('race-demo4-03', 'DEMO 4 - Grand Final', NULL, NULL, 'TBD', 'MILE_1600M', 1, 'SCHEDULING', 'round-demo4-02', '11111111-1111-1111-1111-111111111111');

-- Hồ sơ APPROVED ngựa giải DEMO 4
INSERT INTO horse_tournament_registrations (horse_tournament_reg_id, tournament_id, horse_id, owner_id, status, submitted_at, rating_at_registration, race_class_at_registration)
VALUES
    ('reg-demo4-horse01', '88888888-8888-8888-8888-888888888804', '99999999-9999-9999-9999-999999999901', 'aaaaaaaa-1111-1111-1111-111111111101', 'APPROVED', DATE_SUB(NOW(), INTERVAL 9 DAY), 45, 'CLASS_4'),
    ('reg-demo4-horse02', '88888888-8888-8888-8888-888888888804', '99999999-9999-9999-9999-999999999902', 'aaaaaaaa-1111-1111-1111-111111111101', 'APPROVED', DATE_SUB(NOW(), INTERVAL 9 DAY), 55, 'CLASS_4'),
    ('reg-demo4-horse03', '88888888-8888-8888-8888-888888888804', '99999999-9999-9999-9999-999999999903', 'aaaaaaaa-1111-1111-1111-111111111101', 'APPROVED', DATE_SUB(NOW(), INTERVAL 9 DAY), 35, 'CLASS_5'),
    ('reg-demo4-horse04', '88888888-8888-8888-8888-888888888804', '99999999-9999-9999-9999-999999999904', 'aaaaaaaa-1111-1111-1111-111111111102', 'APPROVED', DATE_SUB(NOW(), INTERVAL 9 DAY), 62, 'CLASS_3'),
    ('reg-demo4-horse05', '88888888-8888-8888-8888-888888888804', '99999999-9999-9999-9999-999999999905', 'aaaaaaaa-1111-1111-1111-111111111102', 'APPROVED', DATE_SUB(NOW(), INTERVAL 9 DAY), 42, 'CLASS_4'),
    ('reg-demo4-horse06', '88888888-8888-8888-8888-888888888804', '99999999-9999-9999-9999-999999999906', 'aaaaaaaa-1111-1111-1111-111111111102', 'APPROVED', DATE_SUB(NOW(), INTERVAL 9 DAY), 48, 'CLASS_4'),
    ('reg-demo4-horse07', '88888888-8888-8888-8888-888888888804', '99999999-9999-9999-9999-999999999907', 'aaaaaaaa-1111-1111-1111-111111111103', 'APPROVED', DATE_SUB(NOW(), INTERVAL 9 DAY), 52, 'CLASS_4'),
    ('reg-demo4-horse08', '88888888-8888-8888-8888-888888888804', '99999999-9999-9999-9999-999999999908', 'aaaaaaaa-1111-1111-1111-111111111103', 'APPROVED', DATE_SUB(NOW(), INTERVAL 9 DAY), 50, 'CLASS_4');

-- Đăng ký Nài giải DEMO 4
INSERT INTO jockey_tournament_registrations (jockey_tournament_reg_id, tournament_id, jockey_id, status, submitted_at, hire_fee)
VALUES
    ('reg-demo4-jock01', '88888888-8888-8888-8888-888888888804', 'bbbbbbbb-3333-3333-3333-333333333301', 'APPROVED', DATE_SUB(NOW(), INTERVAL 9 DAY), 500000.00),
    ('reg-demo4-jock02', '88888888-8888-8888-8888-888888888804', 'bbbbbbbb-3333-3333-3333-333333333302', 'APPROVED', DATE_SUB(NOW(), INTERVAL 9 DAY), 400000.00),
    ('reg-demo4-jock03', '88888888-8888-8888-8888-888888888804', 'bbbbbbbb-3333-3333-3333-333333333303', 'APPROVED', DATE_SUB(NOW(), INTERVAL 9 DAY), 700000.00),
    ('reg-demo4-jock04', '88888888-8888-8888-8888-888888888804', 'bbbbbbbb-3333-3333-3333-333333333304', 'APPROVED', DATE_SUB(NOW(), INTERVAL 9 DAY), 300000.00),
    ('reg-demo4-jock05', '88888888-8888-8888-8888-888888888804', 'bbbbbbbb-3333-3333-3333-333333333305', 'APPROVED', DATE_SUB(NOW(), INTERVAL 9 DAY), 500000.00),
    ('reg-demo4-jock06', '88888888-8888-8888-8888-888888888804', 'bbbbbbbb-3333-3333-3333-333333333306', 'APPROVED', DATE_SUB(NOW(), INTERVAL 9 DAY), 400000.00),
    ('reg-demo4-jock07', '88888888-8888-8888-8888-888888888804', 'bbbbbbbb-3333-3333-3333-333333333307', 'APPROVED', DATE_SUB(NOW(), INTERVAL 9 DAY), 600000.00),
    ('reg-demo4-jock08', '88888888-8888-8888-8888-888888888804', 'bbbbbbbb-3333-3333-3333-333333333308', 'APPROVED', DATE_SUB(NOW(), INTERVAL 9 DAY), 450000.00);

-- Hợp đồng giải DEMO 4
INSERT INTO jockey_horse_contracts (contract_id, tournament_id, horse_tournament_reg_id, jockey_tournament_reg_id, owner_id, horse_id, jockey_id, hire_fee, advance_percent, final_percent, advance_paid_amount, escrow_amount, system_contract_fee, owner_prize_share_percent, jockey_prize_share_percent, payment_status, escrow_status, advance_payout_status, final_payout_status, status, requested_at, accepted_at)
VALUES
    ('con-demo4-01', '88888888-8888-8888-8888-888888888804', 'reg-demo4-horse01', 'reg-demo4-jock01', 'aaaaaaaa-1111-1111-1111-111111111101', '99999999-9999-9999-9999-999999999901', 'bbbbbbbb-3333-3333-3333-333333333301', 500000.00, 50.0, 50.0, 250000.00, 250000.00, 20000.00, 80.0, 20.0, 'PAID', 'HELD', 'PAID', 'NOT_RELEASED', 'APPROVED', DATE_SUB(NOW(), INTERVAL 8 DAY), DATE_SUB(NOW(), INTERVAL 8 DAY)),
    ('con-demo4-02', '88888888-8888-8888-8888-888888888804', 'reg-demo4-horse02', 'reg-demo4-jock02', 'aaaaaaaa-1111-1111-1111-111111111101', '99999999-9999-9999-9999-999999999902', 'bbbbbbbb-3333-3333-3333-333333333302', 400000.00, 50.0, 50.0, 200000.00, 200000.00, 20000.00, 80.0, 20.0, 'PAID', 'HELD', 'PAID', 'NOT_RELEASED', 'APPROVED', DATE_SUB(NOW(), INTERVAL 8 DAY), DATE_SUB(NOW(), INTERVAL 8 DAY)),
    ('con-demo4-03', '88888888-8888-8888-8888-888888888804', 'reg-demo4-horse03', 'reg-demo4-jock03', 'aaaaaaaa-1111-1111-1111-111111111101', '99999999-9999-9999-9999-999999999903', 'bbbbbbbb-3333-3333-3333-333333333303', 700000.00, 50.0, 50.0, 350000.00, 350000.00, 20000.00, 85.0, 15.0, 'PAID', 'HELD', 'PAID', 'NOT_RELEASED', 'APPROVED', DATE_SUB(NOW(), INTERVAL 8 DAY), DATE_SUB(NOW(), INTERVAL 8 DAY)),
    ('con-demo4-04', '88888888-8888-8888-8888-888888888804', 'reg-demo4-horse04', 'reg-demo4-jock04', 'aaaaaaaa-1111-1111-1111-111111111102', '99999999-9999-9999-9999-999999999904', 'bbbbbbbb-3333-3333-3333-333333333304', 300000.00, 50.0, 50.0, 150000.00, 150000.00, 20000.00, 90.0, 10.0, 'PAID', 'HELD', 'PAID', 'NOT_RELEASED', 'APPROVED', DATE_SUB(NOW(), INTERVAL 8 DAY), DATE_SUB(NOW(), INTERVAL 8 DAY)),
    ('con-demo4-05', '88888888-8888-8888-8888-888888888804', 'reg-demo4-horse05', 'reg-demo4-jock05', 'aaaaaaaa-1111-1111-1111-111111111102', '99999999-9999-9999-9999-999999999905', 'bbbbbbbb-3333-3333-3333-333333333305', 500000.00, 50.0, 50.0, 250000.00, 250000.00, 20000.00, 80.0, 20.0, 'PAID', 'HELD', 'PAID', 'NOT_RELEASED', 'APPROVED', DATE_SUB(NOW(), INTERVAL 8 DAY), DATE_SUB(NOW(), INTERVAL 8 DAY)),
    ('con-demo4-06', '88888888-8888-8888-8888-888888888804', 'reg-demo4-horse06', 'reg-demo4-jock06', 'aaaaaaaa-1111-1111-1111-111111111102', '99999999-9999-9999-9999-999999999906', 'bbbbbbbb-3333-3333-3333-333333333306', 400000.00, 50.0, 50.0, 200000.00, 200000.00, 20000.00, 80.0, 20.0, 'PAID', 'HELD', 'PAID', 'NOT_RELEASED', 'APPROVED', DATE_SUB(NOW(), INTERVAL 8 DAY), DATE_SUB(NOW(), INTERVAL 8 DAY)),
    ('con-demo4-07', '88888888-8888-8888-8888-888888888804', 'reg-demo4-horse07', 'reg-demo4-jock07', 'aaaaaaaa-1111-1111-1111-111111111103', '99999999-9999-9999-9999-999999999907', 'bbbbbbbb-3333-3333-3333-333333333307', 600000.00, 50.0, 50.0, 300000.00, 300000.00, 20000.00, 85.0, 15.0, 'PAID', 'HELD', 'PAID', 'NOT_RELEASED', 'APPROVED', DATE_SUB(NOW(), INTERVAL 8 DAY), DATE_SUB(NOW(), INTERVAL 8 DAY)),
    ('con-demo4-08', '88888888-8888-8888-8888-888888888804', 'reg-demo4-horse08', 'reg-demo4-jock08', 'aaaaaaaa-1111-1111-1111-111111111103', '99999999-9999-9999-9999-999999999908', 'bbbbbbbb-3333-3333-3333-333333333308', 450000.00, 50.0, 50.0, 225000.00, 225000.00, 20000.00, 85.0, 15.0, 'PAID', 'HELD', 'PAID', 'NOT_RELEASED', 'APPROVED', DATE_SUB(NOW(), INTERVAL 8 DAY), DATE_SUB(NOW(), INTERVAL 8 DAY));

-- Xếp làn Bán kết A & B giải DEMO 4
INSERT INTO race_entries (entry_id, race_id, contract_id, lane_number, status, assigned_by, assigned_at, created_at)
VALUES
    ('ent-demo4-01', 'race-demo4-01', 'con-demo4-01', 1, 'CONFIRMED', '11111111-1111-1111-1111-111111111111', DATE_SUB(NOW(), INTERVAL 3 DAY), DATE_SUB(NOW(), INTERVAL 3 DAY)),
    ('ent-demo4-02', 'race-demo4-01', 'con-demo4-02', 2, 'CONFIRMED', '11111111-1111-1111-1111-111111111111', DATE_SUB(NOW(), INTERVAL 3 DAY), DATE_SUB(NOW(), INTERVAL 3 DAY)),
    ('ent-demo4-03', 'race-demo4-01', 'con-demo4-03', 3, 'CONFIRMED', '11111111-1111-1111-1111-111111111111', DATE_SUB(NOW(), INTERVAL 3 DAY), DATE_SUB(NOW(), INTERVAL 3 DAY)),
    ('ent-demo4-04', 'race-demo4-01', 'con-demo4-04', 4, 'CONFIRMED', '11111111-1111-1111-1111-111111111111', DATE_SUB(NOW(), INTERVAL 3 DAY), DATE_SUB(NOW(), INTERVAL 3 DAY)),
    ('ent-demo4-05', 'race-demo4-02', 'con-demo4-05', 1, 'CONFIRMED', '11111111-1111-1111-1111-111111111111', DATE_SUB(NOW(), INTERVAL 3 DAY), DATE_SUB(NOW(), INTERVAL 3 DAY)),
    ('ent-demo4-06', 'race-demo4-02', 'con-demo4-06', 2, 'CONFIRMED', '11111111-1111-1111-1111-111111111111', DATE_SUB(NOW(), INTERVAL 3 DAY), DATE_SUB(NOW(), INTERVAL 3 DAY)),
    ('ent-demo4-07', 'race-demo4-02', 'con-demo4-07', 3, 'CONFIRMED', '11111111-1111-1111-1111-111111111111', DATE_SUB(NOW(), INTERVAL 3 DAY), DATE_SUB(NOW(), INTERVAL 3 DAY)),
    ('ent-demo4-08', 'race-demo4-02', 'con-demo4-08', 4, 'CONFIRMED', '11111111-1111-1111-1111-111111111111', DATE_SUB(NOW(), INTERVAL 3 DAY), DATE_SUB(NOW(), INTERVAL 3 DAY));

-- Khám y tế Vòng 1 giải DEMO 4
INSERT INTO horse_inspections (horse_inspection_id, entry_id, vet_id, result, note, inspected_at, handicap_weight, is_handicap_confirmed, status)
VALUES
    ('ins-demo4-h01', 'ent-demo4-01', 'eeeeeeee-6666-6666-6666-666666666601', 'PASS', 'Healthy.', DATE_SUB(NOW(), INTERVAL 2 DAY) - INTERVAL 1 HOUR, 0.0, 0, 'CONFIRMED'),
    ('ins-demo4-h02', 'ent-demo4-02', 'eeeeeeee-6666-6666-6666-666666666601', 'PASS', 'Healthy.', DATE_SUB(NOW(), INTERVAL 2 DAY) - INTERVAL 1 HOUR, 0.0, 0, 'CONFIRMED'),
    ('ins-demo4-h03', 'ent-demo4-03', 'eeeeeeee-6666-6666-6666-666666666601', 'PASS', 'Healthy.', DATE_SUB(NOW(), INTERVAL 2 DAY) - INTERVAL 1 HOUR, 0.0, 0, 'CONFIRMED'),
    ('ins-demo4-h04', 'ent-demo4-04', 'eeeeeeee-6666-6666-6666-666666666601', 'PASS', 'Healthy.', DATE_SUB(NOW(), INTERVAL 2 DAY) - INTERVAL 1 HOUR, 0.0, 0, 'CONFIRMED'),
    ('ins-demo4-h05', 'ent-demo4-05', 'eeeeeeee-6666-6666-6666-666666666601', 'PASS', 'Healthy.', DATE_SUB(NOW(), INTERVAL 2 DAY), 0.0, 0, 'CONFIRMED'),
    ('ins-demo4-h06', 'ent-demo4-06', 'eeeeeeee-6666-6666-6666-666666666601', 'PASS', 'Healthy.', DATE_SUB(NOW(), INTERVAL 2 DAY), 0.0, 0, 'CONFIRMED'),
    ('ins-demo4-h07', 'ent-demo4-07', 'eeeeeeee-6666-6666-6666-666666666601', 'PASS', 'Healthy.', DATE_SUB(NOW(), INTERVAL 2 DAY), 0.0, 0, 'CONFIRMED'),
    ('ins-demo4-h08', 'ent-demo4-08', 'eeeeeeee-6666-6666-6666-666666666601', 'PASS', 'Healthy.', DATE_SUB(NOW(), INTERVAL 2 DAY), 0.0, 0, 'CONFIRMED');

INSERT INTO jockey_inspections (jockey_inspection_id, entry_id, med_staff_id, result, note, inspected_at, status)
VALUES
    ('ins-demo4-j01', 'ent-demo4-01', 'ffffffff-7777-7777-7777-777777777701', 'PASS', 'Normal.', DATE_SUB(NOW(), INTERVAL 2 DAY) - INTERVAL 1 HOUR, 'CONFIRMED'),
    ('ins-demo4-j02', 'ent-demo4-02', 'ffffffff-7777-7777-7777-777777777701', 'PASS', 'Normal.', DATE_SUB(NOW(), INTERVAL 2 DAY) - INTERVAL 1 HOUR, 'CONFIRMED'),
    ('ins-demo4-j03', 'ent-demo4-03', 'ffffffff-7777-7777-7777-777777777701', 'PASS', 'Normal.', DATE_SUB(NOW(), INTERVAL 2 DAY) - INTERVAL 1 HOUR, 'CONFIRMED'),
    ('ins-demo4-j04', 'ent-demo4-04', 'ffffffff-7777-7777-7777-777777777701', 'PASS', 'Normal.', DATE_SUB(NOW(), INTERVAL 2 DAY) - INTERVAL 1 HOUR, 'CONFIRMED'),
    ('ins-demo4-j05', 'ent-demo4-05', 'ffffffff-7777-7777-7777-777777777701', 'PASS', 'Normal.', DATE_SUB(NOW(), INTERVAL 2 DAY), 'CONFIRMED'),
    ('ins-demo4-j06', 'ent-demo4-06', 'ffffffff-7777-7777-7777-777777777701', 'PASS', 'Normal.', DATE_SUB(NOW(), INTERVAL 2 DAY), 'CONFIRMED'),
    ('ins-demo4-j07', 'ent-demo4-07', 'ffffffff-7777-7777-7777-777777777701', 'PASS', 'Normal.', DATE_SUB(NOW(), INTERVAL 2 DAY), 'CONFIRMED'),
    ('ins-demo4-j08', 'ent-demo4-08', 'ffffffff-7777-7777-7777-777777777701', 'PASS', 'Normal.', DATE_SUB(NOW(), INTERVAL 2 DAY), 'CONFIRMED');

-- Kết quả đua Bán kết A & B giải DEMO 4
INSERT INTO race_results (result_id, race_id, entry_id, finish_time, finish_position, prize_money, owner_prize_amount, jockey_prize_amount, prize_status, is_prize_paid, status, recorded_by, recorded_at, updated_at)
VALUES
    ('res-demo4-01', 'race-demo4-01', 'ent-demo4-01', 95.20, 1, 0.00, 0.00, 0.00, 'NotEligible', 0, 'FINISHED', '55555555-5555-5555-5555-555555555501', DATE_SUB(NOW(), INTERVAL 2 DAY), DATE_SUB(NOW(), INTERVAL 2 DAY)),
    ('res-demo4-02', 'race-demo4-01', 'ent-demo4-02', 96.50, 2, 0.00, 0.00, 0.00, 'NotEligible', 0, 'FINISHED', '55555555-5555-5555-5555-555555555501', DATE_SUB(NOW(), INTERVAL 2 DAY), DATE_SUB(NOW(), INTERVAL 2 DAY)),
    ('res-demo4-03', 'race-demo4-01', 'ent-demo4-03', 98.10, 3, 0.00, 0.00, 0.00, 'NotEligible', 0, 'FINISHED', '55555555-5555-5555-5555-555555555501', DATE_SUB(NOW(), INTERVAL 2 DAY), DATE_SUB(NOW(), INTERVAL 2 DAY)),
    ('res-demo4-04', 'race-demo4-01', 'ent-demo4-04', 101.40, 4, 0.00, 0.00, 0.00, 'NotEligible', 0, 'FINISHED', '55555555-5555-5555-5555-555555555501', DATE_SUB(NOW(), INTERVAL 2 DAY), DATE_SUB(NOW(), INTERVAL 2 DAY)),
    ('res-demo4-05', 'race-demo4-02', 'ent-demo4-05', 94.80, 1, 0.00, 0.00, 0.00, 'NotEligible', 0, 'FINISHED', '55555555-5555-5555-5555-555555555501', DATE_SUB(NOW(), INTERVAL 2 DAY), DATE_SUB(NOW(), INTERVAL 2 DAY)),
    ('res-demo4-06', 'race-demo4-02', 'ent-demo4-06', 95.90, 2, 0.00, 0.00, 0.00, 'NotEligible', 0, 'FINISHED', '55555555-5555-5555-5555-555555555501', DATE_SUB(NOW(), INTERVAL 2 DAY), DATE_SUB(NOW(), INTERVAL 2 DAY)),
    ('res-demo4-07', 'race-demo4-02', 'ent-demo4-07', 97.40, 3, 0.00, 0.00, 0.00, 'NotEligible', 0, 'FINISHED', '55555555-5555-5555-5555-555555555501', DATE_SUB(NOW(), INTERVAL 2 DAY), DATE_SUB(NOW(), INTERVAL 2 DAY)),
    ('res-demo4-08', 'race-demo4-02', 'ent-demo4-08', 99.80, 4, 0.00, 0.00, 0.00, 'NotEligible', 0, 'FINISHED', '55555555-5555-5555-5555-555555555501', DATE_SUB(NOW(), INTERVAL 2 DAY), DATE_SUB(NOW(), INTERVAL 2 DAY));

-- Báo cáo kết quả đua cho Bán kết A đã Published
INSERT INTO race_reports (report_id, race_id, referee_id, summary, status, signed_by, signed_at, published_by, published_at, created_at)
VALUES ('rep-demo4-01', 'race-demo4-01', 'dddddddd-5555-5555-5555-555555555501', 'Trận bán kết A hoàn tất. Kết quả chính thức.', 'Published', 'dddddddd-5555-5555-5555-555555555501', DATE_SUB(NOW(), INTERVAL 1 DAY), '11111111-1111-1111-1111-111111111111', DATE_SUB(NOW(), INTERVAL 1 DAY), DATE_SUB(NOW(), INTERVAL 1 DAY));

-- Báo cáo kết quả đua cho Bán kết B chỉ mới dừng ở dạng Signed (chờ Admin vào bấm Publish để kích hoạt chuyển vòng)
INSERT INTO race_reports (report_id, race_id, referee_id, summary, status, signed_by, signed_at, created_at)
VALUES ('rep-demo4-02', 'race-demo4-02', 'dddddddd-5555-5555-5555-555555555501', 'Trận bán kết B đã ký. Đang chờ Admin công bố báo cáo này.', 'Signed', 'dddddddd-5555-5555-5555-555555555501', DATE_SUB(NOW(), INTERVAL 1 DAY), DATE_SUB(NOW(), INTERVAL 1 DAY));

-- Chuẩn hóa các ID dễ đọc như tour-demo1/race-demo4-01 thành UUID hợp lệ.
-- Các cột này được BE ánh xạ java.util.UUID; để chuỗi ngắn trong CHAR(36) sẽ
-- insert được ở MySQL nhưng API sẽ lỗi khi Hibernate đọc dữ liệu.
DROP TEMPORARY TABLE IF EXISTS demo_invalid_uuid_map;
CREATE TEMPORARY TABLE demo_invalid_uuid_map (
    old_id VARCHAR(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci PRIMARY KEY,
    new_id CHAR(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL,
    UNIQUE KEY uk_demo_new_uuid (new_id)
);

INSERT IGNORE INTO demo_invalid_uuid_map (old_id)
SELECT raw_id
FROM (
    SELECT tournament_id AS raw_id FROM tournaments
    UNION ALL SELECT prize_structure_id FROM prize_structures
    UNION ALL SELECT horse_tournament_reg_id FROM horse_tournament_registrations
    UNION ALL SELECT jockey_tournament_reg_id FROM jockey_tournament_registrations
    UNION ALL SELECT contract_id FROM jockey_horse_contracts
    UNION ALL SELECT round_id FROM rounds
    UNION ALL SELECT race_id FROM races
    UNION ALL SELECT entry_id FROM race_entries
    UNION ALL SELECT horse_inspection_id FROM horse_inspections
    UNION ALL SELECT jockey_inspection_id FROM jockey_inspections
    UNION ALL SELECT result_id FROM race_results
    UNION ALL SELECT report_id FROM race_reports
    UNION ALL SELECT prediction_id FROM predictions
    UNION ALL SELECT prediction_detail_id FROM prediction_detail
) demo_ids
WHERE raw_id NOT REGEXP '^[0-9A-Fa-f]{8}-[0-9A-Fa-f]{4}-[0-9A-Fa-f]{4}-[0-9A-Fa-f]{4}-[0-9A-Fa-f]{12}$';

UPDATE demo_invalid_uuid_map
SET new_id = LOWER(UUID());

-- Tournament và các bảng tham chiếu Tournament.
UPDATE prize_structures x JOIN demo_invalid_uuid_map m ON x.tournament_id = m.old_id SET x.tournament_id = m.new_id;
UPDATE horse_tournament_registrations x JOIN demo_invalid_uuid_map m ON x.tournament_id = m.old_id SET x.tournament_id = m.new_id;
UPDATE jockey_tournament_registrations x JOIN demo_invalid_uuid_map m ON x.tournament_id = m.old_id SET x.tournament_id = m.new_id;
UPDATE jockey_horse_contracts x JOIN demo_invalid_uuid_map m ON x.tournament_id = m.old_id SET x.tournament_id = m.new_id;
UPDATE rounds x JOIN demo_invalid_uuid_map m ON x.tournament_id = m.old_id SET x.tournament_id = m.new_id;
UPDATE tournaments x JOIN demo_invalid_uuid_map m ON x.tournament_id = m.old_id SET x.tournament_id = m.new_id;

-- Registration và Contract.
UPDATE jockey_horse_contracts x JOIN demo_invalid_uuid_map m ON x.horse_tournament_reg_id = m.old_id SET x.horse_tournament_reg_id = m.new_id;
UPDATE invoices x JOIN demo_invalid_uuid_map m ON x.tournament_reg_id = m.old_id SET x.tournament_reg_id = m.new_id;
UPDATE horse_tournament_registrations x JOIN demo_invalid_uuid_map m ON x.horse_tournament_reg_id = m.old_id SET x.horse_tournament_reg_id = m.new_id;

UPDATE jockey_horse_contracts x JOIN demo_invalid_uuid_map m ON x.jockey_tournament_reg_id = m.old_id SET x.jockey_tournament_reg_id = m.new_id;
UPDATE invoices x JOIN demo_invalid_uuid_map m ON x.jockey_tournament_reg_id = m.old_id SET x.jockey_tournament_reg_id = m.new_id;
UPDATE jockey_tournament_registrations x JOIN demo_invalid_uuid_map m ON x.jockey_tournament_reg_id = m.old_id SET x.jockey_tournament_reg_id = m.new_id;

UPDATE race_entries x JOIN demo_invalid_uuid_map m ON x.contract_id = m.old_id SET x.contract_id = m.new_id;
UPDATE invoices x JOIN demo_invalid_uuid_map m ON x.contract_id = m.old_id SET x.contract_id = m.new_id;
UPDATE wallet_transactions x JOIN demo_invalid_uuid_map m ON x.contract_id = m.old_id SET x.contract_id = m.new_id;
UPDATE jockey_horse_contracts x JOIN demo_invalid_uuid_map m ON x.contract_id = m.old_id SET x.contract_id = m.new_id;

-- Round và Race.
UPDATE races x JOIN demo_invalid_uuid_map m ON x.round_id = m.old_id SET x.round_id = m.new_id;
UPDATE rounds x JOIN demo_invalid_uuid_map m ON x.round_id = m.old_id SET x.round_id = m.new_id;

UPDATE race_entries x JOIN demo_invalid_uuid_map m ON x.race_id = m.old_id SET x.race_id = m.new_id;
UPDATE race_results x JOIN demo_invalid_uuid_map m ON x.race_id = m.old_id SET x.race_id = m.new_id;
UPDATE race_reports x JOIN demo_invalid_uuid_map m ON x.race_id = m.old_id SET x.race_id = m.new_id;
UPDATE predictions x JOIN demo_invalid_uuid_map m ON x.race_id = m.old_id SET x.race_id = m.new_id;
UPDATE race_referees x JOIN demo_invalid_uuid_map m ON x.race_id = m.old_id SET x.race_id = m.new_id;
UPDATE race_inspection_staff_assignments x JOIN demo_invalid_uuid_map m ON x.race_id = m.old_id SET x.race_id = m.new_id;
UPDATE horse_rating_histories x JOIN demo_invalid_uuid_map m ON x.race_id = m.old_id SET x.race_id = m.new_id;
UPDATE races x JOIN demo_invalid_uuid_map m ON x.race_id = m.old_id SET x.race_id = m.new_id;

-- RaceEntry, result và prediction.
UPDATE horse_inspections x JOIN demo_invalid_uuid_map m ON x.entry_id = m.old_id SET x.entry_id = m.new_id;
UPDATE jockey_inspections x JOIN demo_invalid_uuid_map m ON x.entry_id = m.old_id SET x.entry_id = m.new_id;
UPDATE ai_predictions x JOIN demo_invalid_uuid_map m ON x.entry_id = m.old_id SET x.entry_id = m.new_id;
UPDATE race_results x JOIN demo_invalid_uuid_map m ON x.entry_id = m.old_id SET x.entry_id = m.new_id;
UPDATE prediction_detail x JOIN demo_invalid_uuid_map m ON x.entry_id = m.old_id SET x.entry_id = m.new_id;
UPDATE violations x JOIN demo_invalid_uuid_map m ON x.entry_id = m.old_id SET x.entry_id = m.new_id;
UPDATE appeals x JOIN demo_invalid_uuid_map m ON x.entry_id = m.old_id SET x.entry_id = m.new_id;
UPDATE race_entries x JOIN demo_invalid_uuid_map m ON x.entry_id = m.old_id SET x.entry_id = m.new_id;

UPDATE appeals x JOIN demo_invalid_uuid_map m ON x.race_result_id = m.old_id SET x.race_result_id = m.new_id;
UPDATE horse_rating_histories x JOIN demo_invalid_uuid_map m ON x.race_result_id = m.old_id SET x.race_result_id = m.new_id;
UPDATE wallet_transactions x JOIN demo_invalid_uuid_map m ON x.race_result_id = m.old_id SET x.race_result_id = m.new_id;
UPDATE race_results x JOIN demo_invalid_uuid_map m ON x.result_id = m.old_id SET x.result_id = m.new_id;

UPDATE prediction_detail x JOIN demo_invalid_uuid_map m ON x.prediction_id = m.old_id SET x.prediction_id = m.new_id;
UPDATE predictions x JOIN demo_invalid_uuid_map m ON x.prediction_id = m.old_id SET x.prediction_id = m.new_id;

-- Các khóa chính không có bảng con trong bộ seed này.
UPDATE prize_structures x JOIN demo_invalid_uuid_map m ON x.prize_structure_id = m.old_id SET x.prize_structure_id = m.new_id;
UPDATE horse_inspections x JOIN demo_invalid_uuid_map m ON x.horse_inspection_id = m.old_id SET x.horse_inspection_id = m.new_id;
UPDATE jockey_inspections x JOIN demo_invalid_uuid_map m ON x.jockey_inspection_id = m.old_id SET x.jockey_inspection_id = m.new_id;
UPDATE race_reports x JOIN demo_invalid_uuid_map m ON x.report_id = m.old_id SET x.report_id = m.new_id;
UPDATE prediction_detail x JOIN demo_invalid_uuid_map m ON x.prediction_detail_id = m.old_id SET x.prediction_detail_id = m.new_id;

DROP TEMPORARY TABLE demo_invalid_uuid_map;

-- Kích hoạt lại kiểm tra khóa ngoại
SET FOREIGN_KEY_CHECKS = @OLD_FOREIGN_KEY_CHECKS;
SET SQL_SAFE_UPDATES = @OLD_SQL_SAFE_UPDATES;

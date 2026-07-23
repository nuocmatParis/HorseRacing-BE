-- ============================================================================
-- HRTMS DEMO — SEED DATA CHO FLOW LẬP LỊCH THI ĐẤU
-- ============================================================================
-- Mục đích:
--   Tạo dữ liệu mẫu để test bracket-confirm + schedule-proposal +
--   confirm-schedule cho tournament 32 entry (2 round).
--
-- Kịch bản:
--   Tournament "DEMO SCHEDULING" có 32 entries được duyệt,
--   bracket: Vòng 1 (2 race × 16 entry) → Chung Kết (1 race × 8 entry)
--
-- Cách dùng:
--   1. Chạy BE một lần để Hibernate/Flyway tạo schema mới nhất.
--   2. Chạy file này trên database SWP391_Project_HRTMS.
--   3. Gọi API theo hướng dẫn ở cuối file.
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
-- UUID prefix constants
-- ============================================================================
SET @UUID_PREFIX = '90000000-0000-0000-0000-0000000000';

-- Password hash của "admin123"
SET @demo_password = '$2a$12$AsBCvrsZ2yvqd7RyEflkfOGwfTewt8CSx40CKh0ZIZuD4WZ49wo4a';
SET @now = NOW();

-- ============================================================================
-- 1. ROLES
-- ============================================================================
INSERT IGNORE INTO roles (role_id, role_name, description, is_active, created_at)
VALUES
    (CONCAT(@UUID_PREFIX, '01'), 'ADMIN',       'Administrator',           1, @now),
    (CONCAT(@UUID_PREFIX, '02'), 'HORSE_OWNER', 'Chủ ngựa',                1, @now),
    (CONCAT(@UUID_PREFIX, '03'), 'JOCKEY',      'Kỵ sĩ',                   1, @now),
    (CONCAT(@UUID_PREFIX, '04'), 'SPECTATOR',   'Khán giả',                1, @now),
    (CONCAT(@UUID_PREFIX, '05'), 'REFEREE',     'Trọng tài',               1, @now),
    (CONCAT(@UUID_PREFIX, '06'), 'VETERINARIAN','Bác sĩ thú y',            1, @now),
    (CONCAT(@UUID_PREFIX, '07'), 'MEDICAL_STAFF','Nhân viên y tế',         1, @now);

SET @role_admin     = CONCAT(@UUID_PREFIX, '01');
SET @role_owner     = CONCAT(@UUID_PREFIX, '02');
SET @role_jockey    = CONCAT(@UUID_PREFIX, '03');
SET @role_spectator = CONCAT(@UUID_PREFIX, '04');
SET @role_referee   = CONCAT(@UUID_PREFIX, '05');
SET @role_vet       = CONCAT(@UUID_PREFIX, '06');
SET @role_medical   = CONCAT(@UUID_PREFIX, '07');

-- ============================================================================
-- 2. USERS
-- ============================================================================
-- Admin
INSERT IGNORE INTO users
    (user_id, username, password, email, dob, gender, full_name, phone_number,
     image_url, status, created_at, last_login_at, role_id)
VALUES
    (CONCAT(@UUID_PREFIX, '01'), 'admin1',    @demo_password, 'admin@hrtms.com',
     '1990-01-01', 'MALE',   'Admin Chính',    '0900000001',
     NULL, 'ACTIVE', @now, @now, @role_admin);

-- 2 Horse Owners
INSERT IGNORE INTO users
    (user_id, username, password, email, dob, gender, full_name, phone_number,
     image_url, status, created_at, last_login_at, role_id)
VALUES
    (CONCAT(@UUID_PREFIX, '11'), 'owner1',    @demo_password, 'owner1@hrtms.com',
     '1985-03-15', 'MALE',   'Nguyễn Văn A',  '0900000011',
     NULL, 'ACTIVE', @now, @now, @role_owner),
    (CONCAT(@UUID_PREFIX, '12'), 'owner2',    @demo_password, 'owner2@hrtms.com',
     '1990-07-20', 'FEMALE', 'Trần Thị B',    '0900000012',
     NULL, 'ACTIVE', @now, @now, @role_owner);

-- 32 Jockeys
INSERT IGNORE INTO users
    (user_id, username, password, email, dob, gender, full_name, phone_number,
     image_url, status, created_at, last_login_at, role_id)
VALUES
    (CONCAT(@UUID_PREFIX, '21'), 'jockey01', @demo_password, 'jockey01@hrtms.com', '1995-01-01', 'MALE',   'Jockey 01', '0900000021', NULL, 'ACTIVE', @now, @now, @role_jockey),
    (CONCAT(@UUID_PREFIX, '22'), 'jockey02', @demo_password, 'jockey02@hrtms.com', '1995-02-01', 'MALE',   'Jockey 02', '0900000022', NULL, 'ACTIVE', @now, @now, @role_jockey),
    (CONCAT(@UUID_PREFIX, '23'), 'jockey03', @demo_password, 'jockey03@hrtms.com', '1995-03-01', 'MALE',   'Jockey 03', '0900000023', NULL, 'ACTIVE', @now, @now, @role_jockey),
    (CONCAT(@UUID_PREFIX, '24'), 'jockey04', @demo_password, 'jockey04@hrtms.com', '1995-04-01', 'MALE',   'Jockey 04', '0900000024', NULL, 'ACTIVE', @now, @now, @role_jockey),
    (CONCAT(@UUID_PREFIX, '25'), 'jockey05', @demo_password, 'jockey05@hrtms.com', '1995-05-01', 'MALE',   'Jockey 05', '0900000025', NULL, 'ACTIVE', @now, @now, @role_jockey),
    (CONCAT(@UUID_PREFIX, '26'), 'jockey06', @demo_password, 'jockey06@hrtms.com', '1995-06-01', 'MALE',   'Jockey 06', '0900000026', NULL, 'ACTIVE', @now, @now, @role_jockey),
    (CONCAT(@UUID_PREFIX, '27'), 'jockey07', @demo_password, 'jockey07@hrtms.com', '1995-07-01', 'MALE',   'Jockey 07', '0900000027', NULL, 'ACTIVE', @now, @now, @role_jockey),
    (CONCAT(@UUID_PREFIX, '28'), 'jockey08', @demo_password, 'jockey08@hrtms.com', '1995-08-01', 'MALE',   'Jockey 08', '0900000028', NULL, 'ACTIVE', @now, @now, @role_jockey),
    (CONCAT(@UUID_PREFIX, '29'), 'jockey09', @demo_password, 'jockey09@hrtms.com', '1995-09-01', 'MALE',   'Jockey 09', '0900000029', NULL, 'ACTIVE', @now, @now, @role_jockey),
    (CONCAT(@UUID_PREFIX, '2a'), 'jockey10', @demo_password, 'jockey10@hrtms.com', '1995-10-01', 'MALE',   'Jockey 10', '0900000030', NULL, 'ACTIVE', @now, @now, @role_jockey),
    (CONCAT(@UUID_PREFIX, '2b'), 'jockey11', @demo_password, 'jockey11@hrtms.com', '1996-01-01', 'MALE',   'Jockey 11', '0900000031', NULL, 'ACTIVE', @now, @now, @role_jockey),
    (CONCAT(@UUID_PREFIX, '2c'), 'jockey12', @demo_password, 'jockey12@hrtms.com', '1996-02-01', 'MALE',   'Jockey 12', '0900000032', NULL, 'ACTIVE', @now, @now, @role_jockey),
    (CONCAT(@UUID_PREFIX, '2d'), 'jockey13', @demo_password, 'jockey13@hrtms.com', '1996-03-01', 'MALE',   'Jockey 13', '0900000033', NULL, 'ACTIVE', @now, @now, @role_jockey),
    (CONCAT(@UUID_PREFIX, '2e'), 'jockey14', @demo_password, 'jockey14@hrtms.com', '1996-04-01', 'MALE',   'Jockey 14', '0900000034', NULL, 'ACTIVE', @now, @now, @role_jockey),
    (CONCAT(@UUID_PREFIX, '2f'), 'jockey15', @demo_password, 'jockey15@hrtms.com', '1996-05-01', 'MALE',   'Jockey 15', '0900000035', NULL, 'ACTIVE', @now, @now, @role_jockey),
    (CONCAT(@UUID_PREFIX, '30'), 'jockey16', @demo_password, 'jockey16@hrtms.com', '1996-06-01', 'MALE',   'Jockey 16', '0900000036', NULL, 'ACTIVE', @now, @now, @role_jockey),
    (CONCAT(@UUID_PREFIX, '31'), 'jockey17', @demo_password, 'jockey17@hrtms.com', '1996-07-01', 'MALE',   'Jockey 17', '0900000037', NULL, 'ACTIVE', @now, @now, @role_jockey),
    (CONCAT(@UUID_PREFIX, '32'), 'jockey18', @demo_password, 'jockey18@hrtms.com', '1996-08-01', 'MALE',   'Jockey 18', '0900000038', NULL, 'ACTIVE', @now, @now, @role_jockey),
    (CONCAT(@UUID_PREFIX, '33'), 'jockey19', @demo_password, 'jockey19@hrtms.com', '1996-09-01', 'MALE',   'Jockey 19', '0900000039', NULL, 'ACTIVE', @now, @now, @role_jockey),
    (CONCAT(@UUID_PREFIX, '34'), 'jockey20', @demo_password, 'jockey20@hrtms.com', '1996-10-01', 'MALE',   'Jockey 20', '0900000040', NULL, 'ACTIVE', @now, @now, @role_jockey),
    (CONCAT(@UUID_PREFIX, '35'), 'jockey21', @demo_password, 'jockey21@hrtms.com', '1997-01-01', 'MALE',   'Jockey 21', '0900000041', NULL, 'ACTIVE', @now, @now, @role_jockey),
    (CONCAT(@UUID_PREFIX, '36'), 'jockey22', @demo_password, 'jockey22@hrtms.com', '1997-02-01', 'MALE',   'Jockey 22', '0900000042', NULL, 'ACTIVE', @now, @now, @role_jockey),
    (CONCAT(@UUID_PREFIX, '37'), 'jockey23', @demo_password, 'jockey23@hrtms.com', '1997-03-01', 'MALE',   'Jockey 23', '0900000043', NULL, 'ACTIVE', @now, @now, @role_jockey),
    (CONCAT(@UUID_PREFIX, '38'), 'jockey24', @demo_password, 'jockey24@hrtms.com', '1997-04-01', 'MALE',   'Jockey 24', '0900000044', NULL, 'ACTIVE', @now, @now, @role_jockey),
    (CONCAT(@UUID_PREFIX, '39'), 'jockey25', @demo_password, 'jockey25@hrtms.com', '1997-05-01', 'MALE',   'Jockey 25', '0900000045', NULL, 'ACTIVE', @now, @now, @role_jockey),
    (CONCAT(@UUID_PREFIX, '3a'), 'jockey26', @demo_password, 'jockey26@hrtms.com', '1997-06-01', 'MALE',   'Jockey 26', '0900000046', NULL, 'ACTIVE', @now, @now, @role_jockey),
    (CONCAT(@UUID_PREFIX, '3b'), 'jockey27', @demo_password, 'jockey27@hrtms.com', '1997-07-01', 'MALE',   'Jockey 27', '0900000047', NULL, 'ACTIVE', @now, @now, @role_jockey),
    (CONCAT(@UUID_PREFIX, '3c'), 'jockey28', @demo_password, 'jockey28@hrtms.com', '1997-08-01', 'MALE',   'Jockey 28', '0900000048', NULL, 'ACTIVE', @now, @now, @role_jockey),
    (CONCAT(@UUID_PREFIX, '3d'), 'jockey29', @demo_password, 'jockey29@hrtms.com', '1997-09-01', 'MALE',   'Jockey 29', '0900000049', NULL, 'ACTIVE', @now, @now, @role_jockey),
    (CONCAT(@UUID_PREFIX, '3e'), 'jockey30', @demo_password, 'jockey30@hrtms.com', '1997-10-01', 'MALE',   'Jockey 30', '0900000050', NULL, 'ACTIVE', @now, @now, @role_jockey),
    (CONCAT(@UUID_PREFIX, '3f'), 'jockey31', @demo_password, 'jockey31@hrtms.com', '1998-01-01', 'MALE',   'Jockey 31', '0900000051', NULL, 'ACTIVE', @now, @now, @role_jockey),
    (CONCAT(@UUID_PREFIX, '40'), 'jockey32', @demo_password, 'jockey32@hrtms.com', '1998-02-01', 'MALE',   'Jockey 32', '0900000052', NULL, 'ACTIVE', @now, @now, @role_jockey);

SET @admin_user_id = CONCAT(@UUID_PREFIX, '01');
SET @owner1_user_id = CONCAT(@UUID_PREFIX, '11');
SET @owner2_user_id = CONCAT(@UUID_PREFIX, '12');

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
-- 4. HORSES (32 con, 16 per owner)
-- ============================================================================
INSERT IGNORE INTO horses
    (horse_id, name, breed, gender, age, weight, color, image_url,
     health_status, current_rating, race_class, highest_rating, rating_updated_at,
     total_races, total_wins, total_places, win_rate, last_race_at, created_at, owner_id)
VALUES
    -- Owner 1 (16 horses)
    (CONCAT(@UUID_PREFIX, '41'), 'Ngựa Chiến Thần',   'THOROUGHBRED', 'MALE',   5, 520, 'Bay',    NULL, 'HEALTHY', 120, 'CLASS_1', 125, NULL, 10, 3, 5, 30.0, NULL, @now, @owner1_id),
    (CONCAT(@UUID_PREFIX, '42'), 'Ngựa Tốc Phong',    'THOROUGHBRED', 'MALE',   4, 510, 'Đen',    NULL, 'HEALTHY', 115, 'CLASS_1', 118, NULL, 8,  2, 4, 25.0, NULL, @now, @owner1_id),
    (CONCAT(@UUID_PREFIX, '43'), 'Ngựa Hỏa Tốc',      'THOROUGHBRED', 'MALE',   6, 530, 'Trắng',  NULL, 'HEALTHY', 110, 'CLASS_1', 120, NULL, 15, 5, 8, 33.3, NULL, @now, @owner1_id),
    (CONCAT(@UUID_PREFIX, '44'), 'Ngựa Bạch Mã',       'THOROUGHBRED', 'FEMALE', 4, 490, 'Trắng',  NULL, 'HEALTHY', 108, 'CLASS_1', 112, NULL, 6,  1, 3, 16.7, NULL, @now, @owner1_id),
    (CONCAT(@UUID_PREFIX, '45'), 'Ngựa Kim Cương',     'THOROUGHBRED', 'MALE',   5, 515, 'Vàng',   NULL, 'HEALTHY', 105, 'CLASS_1', 110, NULL, 12, 4, 6, 33.3, NULL, @now, @owner1_id),
    (CONCAT(@UUID_PREFIX, '46'), 'Ngựa Thiên Mã',      'THOROUGHBRED', 'MALE',   3, 500, 'Xám',    NULL, 'HEALTHY', 102, 'CLASS_1', 105, NULL, 5,  1, 2, 20.0, NULL, @now, @owner1_id),
    (CONCAT(@UUID_PREFIX, '47'), 'Ngựa Hồng Lâu',      'THOROUGHBRED', 'FEMALE', 4, 485, 'Hồng',   NULL, 'HEALTHY', 98,  'CLASS_2', 100, NULL, 7,  2, 3, 28.6, NULL, @now, @owner1_id),
    (CONCAT(@UUID_PREFIX, '48'), 'Ngựa Bảo Bối',       'THOROUGHBRED', 'MALE',   5, 525, 'Nâu',    NULL, 'HEALTHY', 95,  'CLASS_2', 102, NULL, 14, 3, 6, 21.4, NULL, @now, @owner1_id),
    (CONCAT(@UUID_PREFIX, '49'), 'Ngựa Tia Chớp',      'THOROUGHBRED', 'MALE',   4, 508, 'Vàng',   NULL, 'HEALTHY', 92,  'CLASS_2', 98,  NULL, 9,  2, 4, 22.2, NULL, @now, @owner1_id),
    (CONCAT(@UUID_PREFIX, '4a'), 'Ngựa Cơn Lốc',       'THOROUGHBRED', 'MALE',   6, 535, 'Đen',    NULL, 'HEALTHY', 90,  'CLASS_2', 95,  NULL, 18, 4, 8, 22.2, NULL, @now, @owner1_id),
    (CONCAT(@UUID_PREFIX, '4b'), 'Ngựa Mặt Trời',      'THOROUGHBRED', 'FEMALE', 3, 480, 'Vàng',   NULL, 'HEALTHY', 88,  'CLASS_2', 90,  NULL, 4,  1, 2, 25.0, NULL, @now, @owner1_id),
    (CONCAT(@UUID_PREFIX, '4c'), 'Ngựa Hoàng Hôn',     'THOROUGHBRED', 'FEMALE', 5, 495, 'Cam',    NULL, 'HEALTHY', 85,  'CLASS_3', 92,  NULL, 11, 2, 5, 18.2, NULL, @now, @owner1_id),
    (CONCAT(@UUID_PREFIX, '4d'), 'Ngựa Sét Đánh',      'THOROUGHBRED', 'MALE',   4, 512, 'Xanh',   NULL, 'HEALTHY', 82,  'CLASS_3', 88,  NULL, 8,  1, 3, 12.5, NULL, @now, @owner1_id),
    (CONCAT(@UUID_PREFIX, '4e'), 'Ngựa Băng Giá',      'THOROUGHBRED', 'MALE',   5, 505, 'Trắng',  NULL, 'HEALTHY', 80,  'CLASS_3', 85,  NULL, 13, 3, 5, 23.1, NULL, @now, @owner1_id),
    (CONCAT(@UUID_PREFIX, '4f'), 'Ngựa Thần Tốc',      'THOROUGHBRED', 'FEMALE', 4, 488, 'Bạc',    NULL, 'HEALTHY', 78,  'CLASS_3', 82,  NULL, 6,  1, 2, 16.7, NULL, @now, @owner1_id),
    (CONCAT(@UUID_PREFIX, '50'), 'Ngựa Vũ Môn',        'THOROUGHBRED', 'MALE',   3, 498, 'Đỏ',     NULL, 'HEALTHY', 75,  'CLASS_3', 78,  NULL, 3,  0, 1, 0.0,  NULL, @now, @owner1_id),
    -- Owner 2 (16 horses)
    (CONCAT(@UUID_PREFIX, '51'), 'Ngựa Phượng Hoàng',  'THOROUGHBRED', 'FEMALE', 5, 500, 'Đỏ',     NULL, 'HEALTHY', 118, 'CLASS_1', 122, NULL, 12, 4, 7, 33.3, NULL, @now, @owner2_id),
    (CONCAT(@UUID_PREFIX, '52'), 'Ngựa Long Vương',    'THOROUGHBRED', 'MALE',   6, 540, 'Xanh',   NULL, 'HEALTHY', 114, 'CLASS_1', 120, NULL, 16, 5, 8, 31.3, NULL, @now, @owner2_id),
    (CONCAT(@UUID_PREFIX, '53'), 'Ngựa Thanh Phong',   'THOROUGHBRED', 'MALE',   4, 510, 'Xám',    NULL, 'HEALTHY', 112, 'CLASS_1', 115, NULL, 8,  3, 5, 37.5, NULL, @now, @owner2_id),
    (CONCAT(@UUID_PREFIX, '54'), 'Ngựa Huyền Thoại',   'THOROUGHBRED', 'MALE',   5, 520, 'Đen',    NULL, 'HEALTHY', 109, 'CLASS_1', 114, NULL, 11, 3, 6, 27.3, NULL, @now, @owner2_id),
    (CONCAT(@UUID_PREFIX, '55'), 'Ngựa Ánh Sao',       'THOROUGHBRED', 'FEMALE', 3, 475, 'Bạc',    NULL, 'HEALTHY', 106, 'CLASS_1', 108, NULL, 4,  1, 2, 25.0, NULL, @now, @owner2_id),
    (CONCAT(@UUID_PREFIX, '56'), 'Ngựa Cầu Vồng',      'THOROUGHBRED', 'FEMALE', 4, 490, 'Hồng',   NULL, 'HEALTHY', 103, 'CLASS_1', 106, NULL, 7,  2, 4, 28.6, NULL, @now, @owner2_id),
    (CONCAT(@UUID_PREFIX, '57'), 'Ngựa Sơn Tinh',      'THOROUGHBRED', 'MALE',   5, 530, 'Nâu',    NULL, 'HEALTHY', 100, 'CLASS_2', 105, NULL, 13, 4, 6, 30.8, NULL, @now, @owner2_id),
    (CONCAT(@UUID_PREFIX, '58'), 'Ngựa Thủy Tinh',     'THOROUGHBRED', 'FEMALE', 4, 485, 'Xanh',   NULL, 'HEALTHY', 96,  'CLASS_2', 100, NULL, 6,  1, 3, 16.7, NULL, @now, @owner2_id),
    (CONCAT(@UUID_PREFIX, '59'), 'Ngựa Lửa Thiêng',    'THOROUGHBRED', 'MALE',   6, 525, 'Đỏ',     NULL, 'HEALTHY', 93,  'CLASS_2', 98,  NULL, 17, 5, 9, 29.4, NULL, @now, @owner2_id),
    (CONCAT(@UUID_PREFIX, '5a'), 'Ngựa Sấm Sét',       'THOROUGHBRED', 'MALE',   4, 515, 'Vàng',   NULL, 'HEALTHY', 91,  'CLASS_2', 96,  NULL, 9,  2, 4, 22.2, NULL, @now, @owner2_id),
    (CONCAT(@UUID_PREFIX, '5b'), 'Ngựa Gió Lộng',      'THOROUGHBRED', 'FEMALE', 3, 478, 'Trắng',  NULL, 'HEALTHY', 87,  'CLASS_2', 90,  NULL, 5,  1, 2, 20.0, NULL, @now, @owner2_id),
    (CONCAT(@UUID_PREFIX, '5c'), 'Ngựa Tuyết Sơn',     'THOROUGHBRED', 'MALE',   5, 508, 'Trắng',  NULL, 'HEALTHY', 84,  'CLASS_3', 90,  NULL, 12, 2, 5, 16.7, NULL, @now, @owner2_id),
    (CONCAT(@UUID_PREFIX, '5d'), 'Ngựa Hải Vân',       'THOROUGHBRED', 'FEMALE', 4, 492, 'Xanh',   NULL, 'HEALTHY', 81,  'CLASS_3', 86,  NULL, 7,  1, 3, 14.3, NULL, @now, @owner2_id),
    (CONCAT(@UUID_PREFIX, '5e'), 'Ngựa Đại Ngàn',      'THOROUGHBRED', 'MALE',   6, 538, 'Nâu',    NULL, 'HEALTHY', 79,  'CLASS_3', 84,  NULL, 15, 3, 6, 20.0, NULL, @now, @owner2_id),
    (CONCAT(@UUID_PREFIX, '5f'), 'Ngựa Trường Sơn',    'THOROUGHBRED', 'MALE',   4, 505, 'Xám',    NULL, 'HEALTHY', 76,  'CLASS_3', 80,  NULL, 8,  2, 3, 25.0, NULL, @now, @owner2_id),
    (CONCAT(@UUID_PREFIX, '60'), 'Ngựa Bình Minh',     'THOROUGHBRED', 'FEMALE', 3, 470, 'Vàng',   NULL, 'HEALTHY', 73,  'CLASS_4', 76,  NULL, 3,  0, 1, 0.0,  NULL, @now, @owner2_id);

-- ============================================================================
-- 5. JOCKEYS (profiles for each jockey user)
-- ============================================================================
INSERT IGNORE INTO jockeys
    (jockey_id, user_id, height, weight, experience_years,
     specialization, status, total_races, total_wins, jockey_tier, tier_updated_at,
     last_race_at, created_at)
VALUES
    (CONCAT(@UUID_PREFIX, '21'), CONCAT(@UUID_PREFIX, '21'), 165, 52, 5, 'SPRINT',    'AVAILABLE',  50, 8,  'PROFESSIONAL', NULL, NULL, @now),
    (CONCAT(@UUID_PREFIX, '22'), CONCAT(@UUID_PREFIX, '22'), 168, 54, 4, 'MILE',      'AVAILABLE',  40, 6,  'JUNIOR',       NULL, NULL, @now),
    (CONCAT(@UUID_PREFIX, '23'), CONCAT(@UUID_PREFIX, '23'), 170, 55, 6, 'JOCKEY-00003', 'SPRINT',    'AVAILABLE',  60, 12, 'PROFESSIONAL', NULL, NULL, @now),
    (CONCAT(@UUID_PREFIX, '24'), CONCAT(@UUID_PREFIX, '24'), 163, 50, 3, 'JOCKEY-00004', 'MILE',      'AVAILABLE',  30, 4,  'JUNIOR',       NULL, NULL, @now),
    (CONCAT(@UUID_PREFIX, '25'), CONCAT(@UUID_PREFIX, '25'), 167, 53, 7, 'JOCKEY-00005', 'LONG',      'AVAILABLE',  70, 15, 'PROFESSIONAL', NULL, NULL, @now),
    (CONCAT(@UUID_PREFIX, '26'), CONCAT(@UUID_PREFIX, '26'), 166, 51, 2, 'JOCKEY-00006', 'SPRINT',    'AVAILABLE',  20, 3,  'APPRENTICE',   NULL, NULL, @now),
    (CONCAT(@UUID_PREFIX, '27'), CONCAT(@UUID_PREFIX, '27'), 169, 56, 5, 'JOCKEY-00007', 'MILE',      'AVAILABLE',  55, 10, 'PROFESSIONAL', NULL, NULL, @now),
    (CONCAT(@UUID_PREFIX, '28'), CONCAT(@UUID_PREFIX, '28'), 164, 49, 4, 'JOCKEY-00008', 'INTERMEDIATE','AVAILABLE', 35, 5,  'JUNIOR',       NULL, NULL, @now),
    (CONCAT(@UUID_PREFIX, '29'), CONCAT(@UUID_PREFIX, '29'), 171, 57, 8, 'JOCKEY-00009', 'SPRINT',    'AVAILABLE',  80, 20, 'ELITE',        NULL, NULL, @now),
    (CONCAT(@UUID_PREFIX, '2a'), CONCAT(@UUID_PREFIX, '2a'), 162, 48, 3, 'JOCKEY-00010', 'MILE',      'AVAILABLE',  25, 4,  'APPRENTICE',   NULL, NULL, @now),
    (CONCAT(@UUID_PREFIX, '2b'), CONCAT(@UUID_PREFIX, '2b'), 168, 54, 6, 'JOCKEY-00011', 'LONG',      'AVAILABLE',  65, 11, 'PROFESSIONAL', NULL, NULL, @now),
    (CONCAT(@UUID_PREFIX, '2c'), CONCAT(@UUID_PREFIX, '2c'), 165, 52, 4, 'JOCKEY-00012', 'SPRINT',    'AVAILABLE',  45, 7,  'JUNIOR',       NULL, NULL, @now),
    (CONCAT(@UUID_PREFIX, '2d'), CONCAT(@UUID_PREFIX, '2d'), 170, 55, 5, 'JOCKEY-00013', 'MILE',      'AVAILABLE',  50, 9,  'PROFESSIONAL', NULL, NULL, @now),
    (CONCAT(@UUID_PREFIX, '2e'), CONCAT(@UUID_PREFIX, '2e'), 163, 50, 7, 'JOCKEY-00014', 'EXTENDED',  'AVAILABLE',  75, 14, 'PROFESSIONAL', NULL, NULL, @now),
    (CONCAT(@UUID_PREFIX, '2f'), CONCAT(@UUID_PREFIX, '2f'), 167, 53, 3, 'JOCKEY-00015', 'SPRINT',    'AVAILABLE',  28, 5,  'JUNIOR',       NULL, NULL, @now),
    (CONCAT(@UUID_PREFIX, '30'), CONCAT(@UUID_PREFIX, '30'), 166, 51, 6, 'JOCKEY-00016', 'MILE',      'AVAILABLE',  58, 10, 'PROFESSIONAL', NULL, NULL, @now),
    (CONCAT(@UUID_PREFIX, '31'), CONCAT(@UUID_PREFIX, '31'), 169, 56, 4, 'JOCKEY-00017', 'INTERMEDIATE','AVAILABLE', 38, 6,  'JUNIOR',       NULL, NULL, @now),
    (CONCAT(@UUID_PREFIX, '32'), CONCAT(@UUID_PREFIX, '32'), 164, 49, 5, 'JOCKEY-00018', 'LONG',      'AVAILABLE',  52, 8,  'PROFESSIONAL', NULL, NULL, @now),
    (CONCAT(@UUID_PREFIX, '33'), CONCAT(@UUID_PREFIX, '33'), 171, 57, 8, 'JOCKEY-00019', 'SPRINT',    'AVAILABLE',  90, 22, 'ELITE',        NULL, NULL, @now),
    (CONCAT(@UUID_PREFIX, '34'), CONCAT(@UUID_PREFIX, '34'), 162, 48, 2, 'JOCKEY-00020', 'MILE',      'AVAILABLE',  18, 2,  'APPRENTICE',   NULL, NULL, @now),
    (CONCAT(@UUID_PREFIX, '35'), CONCAT(@UUID_PREFIX, '35'), 168, 54, 5, 'JOCKEY-00021', 'SPRINT',    'AVAILABLE',  48, 9,  'PROFESSIONAL', NULL, NULL, @now),
    (CONCAT(@UUID_PREFIX, '36'), CONCAT(@UUID_PREFIX, '36'), 165, 52, 4, 'JOCKEY-00022', 'MILE',      'AVAILABLE',  42, 7,  'JUNIOR',       NULL, NULL, @now),
    (CONCAT(@UUID_PREFIX, '37'), CONCAT(@UUID_PREFIX, '37'), 170, 55, 6, 'JOCKEY-00023', 'LONG',      'AVAILABLE',  62, 12, 'PROFESSIONAL', NULL, NULL, @now),
    (CONCAT(@UUID_PREFIX, '38'), CONCAT(@UUID_PREFIX, '38'), 163, 50, 3, 'JOCKEY-00024', 'SPRINT',    'AVAILABLE',  32, 4,  'JUNIOR',       NULL, NULL, @now),
    (CONCAT(@UUID_PREFIX, '39'), CONCAT(@UUID_PREFIX, '39'), 167, 53, 7, 'JOCKEY-00025', 'MILE',      'AVAILABLE',  72, 16, 'PROFESSIONAL', NULL, NULL, @now),
    (CONCAT(@UUID_PREFIX, '3a'), CONCAT(@UUID_PREFIX, '3a'), 166, 51, 5, 'JOCKEY-00026', 'INTERMEDIATE','AVAILABLE', 55, 8,  'PROFESSIONAL', NULL, NULL, @now),
    (CONCAT(@UUID_PREFIX, '3b'), CONCAT(@UUID_PREFIX, '3b'), 169, 56, 4, 'JOCKEY-00027', 'SPRINT',    'AVAILABLE',  36, 6,  'JUNIOR',       NULL, NULL, @now),
    (CONCAT(@UUID_PREFIX, '3c'), CONCAT(@UUID_PREFIX, '3c'), 164, 49, 6, 'JOCKEY-00028', 'MILE',      'AVAILABLE',  60, 11, 'PROFESSIONAL', NULL, NULL, @now),
    (CONCAT(@UUID_PREFIX, '3d'), CONCAT(@UUID_PREFIX, '3d'), 171, 57, 8, 'JOCKEY-00029', 'EXTENDED',  'AVAILABLE',  85, 18, 'ELITE',        NULL, NULL, @now),
    (CONCAT(@UUID_PREFIX, '3e'), CONCAT(@UUID_PREFIX, '3e'), 162, 48, 3, 'JOCKEY-00030', 'SPRINT',    'AVAILABLE',  22, 3,  'APPRENTICE',   NULL, NULL, @now),
    (CONCAT(@UUID_PREFIX, '3f'), CONCAT(@UUID_PREFIX, '3f'), 168, 54, 5, 'JOCKEY-00031', 'MILE',      'AVAILABLE',  55, 9,  'PROFESSIONAL', NULL, NULL, @now),
    (CONCAT(@UUID_PREFIX, '40'), CONCAT(@UUID_PREFIX, '40'), 165, 52, 4, 'JOCKEY-00032', 'LONG',      'AVAILABLE',  40, 6,  'JUNIOR',       NULL, NULL, @now);

-- ============================================================================
-- 6. WALLETS (cho mỗi user + 3 system wallets)
-- ============================================================================
INSERT IGNORE INTO wallets
    (wallet_id, owner_type, balance, currency, status, created_at, updated_at, wallet_purpose, user_id)
VALUES
    -- Admin
    (CONCAT(@UUID_PREFIX, '71'), 'USER', 10000000, 'VND', 'ACTIVE', @now, @now, 'USER_MAIN', @admin_user_id),
    -- 2 Owners
    (CONCAT(@UUID_PREFIX, '72'), 'USER',  5000000, 'VND', 'ACTIVE', @now, @now, 'USER_MAIN', @owner1_user_id),
    (CONCAT(@UUID_PREFIX, '73'), 'USER',  5000000, 'VND', 'ACTIVE', @now, @now, 'USER_MAIN', @owner2_user_id),
    -- 32 Jockeys
    (CONCAT(@UUID_PREFIX, '74'), 'USER',  2000000, 'VND', 'ACTIVE', @now, @now, 'USER_MAIN', CONCAT(@UUID_PREFIX, '21')),
    (CONCAT(@UUID_PREFIX, '75'), 'USER',  2000000, 'VND', 'ACTIVE', @now, @now, 'USER_MAIN', CONCAT(@UUID_PREFIX, '22')),
    (CONCAT(@UUID_PREFIX, '76'), 'USER',  2000000, 'VND', 'ACTIVE', @now, @now, 'USER_MAIN', CONCAT(@UUID_PREFIX, '23')),
    (CONCAT(@UUID_PREFIX, '77'), 'USER',  2000000, 'VND', 'ACTIVE', @now, @now, 'USER_MAIN', CONCAT(@UUID_PREFIX, '24')),
    (CONCAT(@UUID_PREFIX, '78'), 'USER',  2000000, 'VND', 'ACTIVE', @now, @now, 'USER_MAIN', CONCAT(@UUID_PREFIX, '25')),
    (CONCAT(@UUID_PREFIX, '79'), 'USER',  2000000, 'VND', 'ACTIVE', @now, @now, 'USER_MAIN', CONCAT(@UUID_PREFIX, '26')),
    (CONCAT(@UUID_PREFIX, '7a'), 'USER',  2000000, 'VND', 'ACTIVE', @now, @now, 'USER_MAIN', CONCAT(@UUID_PREFIX, '27')),
    (CONCAT(@UUID_PREFIX, '7b'), 'USER',  2000000, 'VND', 'ACTIVE', @now, @now, 'USER_MAIN', CONCAT(@UUID_PREFIX, '28')),
    (CONCAT(@UUID_PREFIX, '7c'), 'USER',  2000000, 'VND', 'ACTIVE', @now, @now, 'USER_MAIN', CONCAT(@UUID_PREFIX, '29')),
    (CONCAT(@UUID_PREFIX, '7d'), 'USER',  2000000, 'VND', 'ACTIVE', @now, @now, 'USER_MAIN', CONCAT(@UUID_PREFIX, '2a')),
    (CONCAT(@UUID_PREFIX, '7e'), 'USER',  2000000, 'VND', 'ACTIVE', @now, @now, 'USER_MAIN', CONCAT(@UUID_PREFIX, '2b')),
    (CONCAT(@UUID_PREFIX, '7f'), 'USER',  2000000, 'VND', 'ACTIVE', @now, @now, 'USER_MAIN', CONCAT(@UUID_PREFIX, '2c')),
    (CONCAT(@UUID_PREFIX, '80'), 'USER',  2000000, 'VND', 'ACTIVE', @now, @now, 'USER_MAIN', CONCAT(@UUID_PREFIX, '2d')),
    (CONCAT(@UUID_PREFIX, '81'), 'USER',  2000000, 'VND', 'ACTIVE', @now, @now, 'USER_MAIN', CONCAT(@UUID_PREFIX, '2e')),
    (CONCAT(@UUID_PREFIX, '82'), 'USER',  2000000, 'VND', 'ACTIVE', @now, @now, 'USER_MAIN', CONCAT(@UUID_PREFIX, '2f')),
    (CONCAT(@UUID_PREFIX, '83'), 'USER',  2000000, 'VND', 'ACTIVE', @now, @now, 'USER_MAIN', CONCAT(@UUID_PREFIX, '30')),
    (CONCAT(@UUID_PREFIX, '84'), 'USER',  2000000, 'VND', 'ACTIVE', @now, @now, 'USER_MAIN', CONCAT(@UUID_PREFIX, '31')),
    (CONCAT(@UUID_PREFIX, '85'), 'USER',  2000000, 'VND', 'ACTIVE', @now, @now, 'USER_MAIN', CONCAT(@UUID_PREFIX, '32')),
    (CONCAT(@UUID_PREFIX, '86'), 'USER',  2000000, 'VND', 'ACTIVE', @now, @now, 'USER_MAIN', CONCAT(@UUID_PREFIX, '33')),
    (CONCAT(@UUID_PREFIX, '87'), 'USER',  2000000, 'VND', 'ACTIVE', @now, @now, 'USER_MAIN', CONCAT(@UUID_PREFIX, '34')),
    (CONCAT(@UUID_PREFIX, '88'), 'USER',  2000000, 'VND', 'ACTIVE', @now, @now, 'USER_MAIN', CONCAT(@UUID_PREFIX, '35')),
    (CONCAT(@UUID_PREFIX, '89'), 'USER',  2000000, 'VND', 'ACTIVE', @now, @now, 'USER_MAIN', CONCAT(@UUID_PREFIX, '36')),
    (CONCAT(@UUID_PREFIX, '8a'), 'USER',  2000000, 'VND', 'ACTIVE', @now, @now, 'USER_MAIN', CONCAT(@UUID_PREFIX, '37')),
    (CONCAT(@UUID_PREFIX, '8b'), 'USER',  2000000, 'VND', 'ACTIVE', @now, @now, 'USER_MAIN', CONCAT(@UUID_PREFIX, '38')),
    (CONCAT(@UUID_PREFIX, '8c'), 'USER',  2000000, 'VND', 'ACTIVE', @now, @now, 'USER_MAIN', CONCAT(@UUID_PREFIX, '39')),
    (CONCAT(@UUID_PREFIX, '8d'), 'USER',  2000000, 'VND', 'ACTIVE', @now, @now, 'USER_MAIN', CONCAT(@UUID_PREFIX, '3a')),
    (CONCAT(@UUID_PREFIX, '8e'), 'USER',  2000000, 'VND', 'ACTIVE', @now, @now, 'USER_MAIN', CONCAT(@UUID_PREFIX, '3b')),
    (CONCAT(@UUID_PREFIX, '8f'), 'USER',  2000000, 'VND', 'ACTIVE', @now, @now, 'USER_MAIN', CONCAT(@UUID_PREFIX, '3c')),
    (CONCAT(@UUID_PREFIX, '90'), 'USER',  2000000, 'VND', 'ACTIVE', @now, @now, 'USER_MAIN', CONCAT(@UUID_PREFIX, '3d')),
    (CONCAT(@UUID_PREFIX, '91'), 'USER',  2000000, 'VND', 'ACTIVE', @now, @now, 'USER_MAIN', CONCAT(@UUID_PREFIX, '3e')),
    (CONCAT(@UUID_PREFIX, '92'), 'USER',  2000000, 'VND', 'ACTIVE', @now, @now, 'USER_MAIN', CONCAT(@UUID_PREFIX, '3f')),
    (CONCAT(@UUID_PREFIX, '93'), 'USER',  2000000, 'VND', 'ACTIVE', @now, @now, 'USER_MAIN', CONCAT(@UUID_PREFIX, '40'));

-- System wallets (revenue, escrow, prize pool)
INSERT IGNORE INTO wallets
    (wallet_id, owner_type, balance, currency, status, created_at, updated_at, wallet_purpose, user_id)
VALUES
    (CONCAT(@UUID_PREFIX, '94'), 'SYSTEM', 0, 'VND', 'ACTIVE', @now, @now, 'SYSTEM_REVENUE',   NULL),
    (CONCAT(@UUID_PREFIX, '95'), 'SYSTEM', 0, 'VND', 'ACTIVE', @now, @now, 'SYSTEM_ESCROW',     NULL),
    (CONCAT(@UUID_PREFIX, '96'), 'SYSTEM', 50000000, 'VND', 'ACTIVE', @now, @now, 'SYSTEM_PRIZE_POOL', NULL);

-- ============================================================================
-- 7. TOURNAMENT (DRAFT, NOT_GENERATED, 32 entries)
-- ============================================================================
SET @tournament_id = CONCAT(@UUID_PREFIX, 'a1');

-- Timeline: registration mở cách đây 30 ngày, scheduling kết thúc 20 ngày sau,
-- competition kéo dài 25 ngày (dư cho bracket 2 round với gap 7 ngày)
SET @reg_open    = DATE_SUB(@now, INTERVAL 30 DAY);
SET @reg_close   = DATE_ADD(@reg_open, INTERVAL 7 DAY);
SET @review_dead = DATE_ADD(@reg_close, INTERVAL 4 DAY);
SET @jockey_dead = DATE_ADD(@review_dead, INTERVAL 7 DAY);
SET @sched_dead  = DATE_ADD(@jockey_dead, INTERVAL 4 DAY);
SET @comp_start  = DATE_ADD(@sched_dead, INTERVAL 2 DAY);
SET @tourn_start = DATE(@comp_start);
SET @tourn_end   = DATE_ADD(@tourn_start, INTERVAL 25 DAY);

DELETE FROM jockey_horse_contracts WHERE tournament_id = @tournament_id;
DELETE FROM jockey_tournament_registrations WHERE tournament_id = @tournament_id;
DELETE FROM horse_tournament_registrations WHERE tournament_id = @tournament_id;
DELETE FROM prize_structures WHERE tournament_id = @tournament_id;
DELETE FROM races WHERE round_id IN (SELECT round_id FROM rounds WHERE tournament_id = @tournament_id);
DELETE FROM round_race_entries WHERE round_id IN (SELECT round_id FROM rounds WHERE tournament_id = @tournament_id);
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
    (@tournament_id,
     'DEMO SCHEDULING',
     'Tournament demo cho flow lập lịch thi đấu: bracket confirm → schedule proposal → confirm schedule. 32 entries, 2 round (Vòng 1 + Chung Kết).',
     @tourn_start, @tourn_end, NULL,
     'Trường đua Demo Scheduling',
     100000, 50000, 10000000,
     'THOROUGHBRED', 2, 12,
     -- Prediction points (defaults)
     100, 30, 10, 50,
     -- Prediction timing (defaults)
     120, 5, 24,
     -- Inspection timing (defaults)
     90, 30,
     -- Race day config (defaults)
     35, 0, 30, 30,
     '08:00:00', '18:00:00',
     0, NULL, NULL,
     -- Status: DRAFT, phase: DRAFT, bracket NOT_GENERATED
     'DRAFT', 'DRAFT', @now, NULL,
     @reg_open, @reg_close, @review_dead,
     @jockey_dead, @sched_dead, @comp_start,
      NULL, 'CLASS_3', 'MILE_1600M',
     -- Handicap (disabled)
     0, 0, 0.0, 0,
     -- Max approved
     32, 40, 32,
     -- Bracket plan (NOT_GENERATED, version=1)
     NULL, NULL, 'NOT_GENERATED', 1,
     @admin_user_id);

-- ============================================================================
-- 8. PRIZE STRUCTURES
-- ============================================================================
INSERT IGNORE INTO prize_structures
    (prize_structure_id, `prize_rank`, percentage, fixed_amount, is_active, tournament_id)
VALUES
    (CONCAT(@UUID_PREFIX, 'b1'), 1, 50, 0, 1, @tournament_id),
    (CONCAT(@UUID_PREFIX, 'b2'), 2, 30, 0, 1, @tournament_id),
    (CONCAT(@UUID_PREFIX, 'b3'), 3, 20, 0, 1, @tournament_id);

-- ============================================================================
-- 9. HORSE TOURNAMENT REGISTRATIONS (32 registrations, APPROVED)
-- ============================================================================
-- Owner 1: horses 41-50 (16 horses)
INSERT IGNORE INTO horse_tournament_registrations
    (horse_tournament_reg_id, tournament_id, horse_id, owner_id, status,
     submitted_at, reviewed_by, reviewed_at, rating_at_registration, race_class_at_registration)
VALUES
    (CONCAT(@UUID_PREFIX, 'c1'),  @tournament_id, CONCAT(@UUID_PREFIX, '41'), @owner1_id, 'APPROVED', @reg_open, @admin_user_id, @reg_close, 120, 'CLASS_1'),
    (CONCAT(@UUID_PREFIX, 'c2'),  @tournament_id, CONCAT(@UUID_PREFIX, '42'), @owner1_id, 'APPROVED', @reg_open, @admin_user_id, @reg_close, 115, 'CLASS_1'),
    (CONCAT(@UUID_PREFIX, 'c3'),  @tournament_id, CONCAT(@UUID_PREFIX, '43'), @owner1_id, 'APPROVED', @reg_open, @admin_user_id, @reg_close, 110, 'CLASS_1'),
    (CONCAT(@UUID_PREFIX, 'c4'),  @tournament_id, CONCAT(@UUID_PREFIX, '44'), @owner1_id, 'APPROVED', @reg_open, @admin_user_id, @reg_close, 108, 'CLASS_1'),
    (CONCAT(@UUID_PREFIX, 'c5'),  @tournament_id, CONCAT(@UUID_PREFIX, '45'), @owner1_id, 'APPROVED', @reg_open, @admin_user_id, @reg_close, 105, 'CLASS_1'),
    (CONCAT(@UUID_PREFIX, 'c6'),  @tournament_id, CONCAT(@UUID_PREFIX, '46'), @owner1_id, 'APPROVED', @reg_open, @admin_user_id, @reg_close, 102, 'CLASS_1'),
    (CONCAT(@UUID_PREFIX, 'c7'),  @tournament_id, CONCAT(@UUID_PREFIX, '47'), @owner1_id, 'APPROVED', @reg_open, @admin_user_id, @reg_close, 98,  'CLASS_2'),
    (CONCAT(@UUID_PREFIX, 'c8'),  @tournament_id, CONCAT(@UUID_PREFIX, '48'), @owner1_id, 'APPROVED', @reg_open, @admin_user_id, @reg_close, 95,  'CLASS_2'),
    (CONCAT(@UUID_PREFIX, 'c9'),  @tournament_id, CONCAT(@UUID_PREFIX, '49'), @owner1_id, 'APPROVED', @reg_open, @admin_user_id, @reg_close, 92,  'CLASS_2'),
    (CONCAT(@UUID_PREFIX, 'ca'),  @tournament_id, CONCAT(@UUID_PREFIX, '4a'), @owner1_id, 'APPROVED', @reg_open, @admin_user_id, @reg_close, 90,  'CLASS_2'),
    (CONCAT(@UUID_PREFIX, 'cb'),  @tournament_id, CONCAT(@UUID_PREFIX, '4b'), @owner1_id, 'APPROVED', @reg_open, @admin_user_id, @reg_close, 88,  'CLASS_2'),
    (CONCAT(@UUID_PREFIX, 'cc'),  @tournament_id, CONCAT(@UUID_PREFIX, '4c'), @owner1_id, 'APPROVED', @reg_open, @admin_user_id, @reg_close, 85,  'CLASS_3'),
    (CONCAT(@UUID_PREFIX, 'cd'),  @tournament_id, CONCAT(@UUID_PREFIX, '4d'), @owner1_id, 'APPROVED', @reg_open, @admin_user_id, @reg_close, 82,  'CLASS_3'),
    (CONCAT(@UUID_PREFIX, 'ce'),  @tournament_id, CONCAT(@UUID_PREFIX, '4e'), @owner1_id, 'APPROVED', @reg_open, @admin_user_id, @reg_close, 80,  'CLASS_3'),
    (CONCAT(@UUID_PREFIX, 'cf'),  @tournament_id, CONCAT(@UUID_PREFIX, '4f'), @owner1_id, 'APPROVED', @reg_open, @admin_user_id, @reg_close, 78,  'CLASS_3'),
    (CONCAT(@UUID_PREFIX, 'd0'),  @tournament_id, CONCAT(@UUID_PREFIX, '50'), @owner1_id, 'APPROVED', @reg_open, @admin_user_id, @reg_close, 75,  'CLASS_3'),
    -- Owner 2: horses 51-60 (16 horses)
    (CONCAT(@UUID_PREFIX, 'd1'),  @tournament_id, CONCAT(@UUID_PREFIX, '51'), @owner2_id, 'APPROVED', @reg_open, @admin_user_id, @reg_close, 118, 'CLASS_1'),
    (CONCAT(@UUID_PREFIX, 'd2'),  @tournament_id, CONCAT(@UUID_PREFIX, '52'), @owner2_id, 'APPROVED', @reg_open, @admin_user_id, @reg_close, 114, 'CLASS_1'),
    (CONCAT(@UUID_PREFIX, 'd3'),  @tournament_id, CONCAT(@UUID_PREFIX, '53'), @owner2_id, 'APPROVED', @reg_open, @admin_user_id, @reg_close, 112, 'CLASS_1'),
    (CONCAT(@UUID_PREFIX, 'd4'),  @tournament_id, CONCAT(@UUID_PREFIX, '54'), @owner2_id, 'APPROVED', @reg_open, @admin_user_id, @reg_close, 109, 'CLASS_1'),
    (CONCAT(@UUID_PREFIX, 'd5'),  @tournament_id, CONCAT(@UUID_PREFIX, '55'), @owner2_id, 'APPROVED', @reg_open, @admin_user_id, @reg_close, 106, 'CLASS_1'),
    (CONCAT(@UUID_PREFIX, 'd6'),  @tournament_id, CONCAT(@UUID_PREFIX, '56'), @owner2_id, 'APPROVED', @reg_open, @admin_user_id, @reg_close, 103, 'CLASS_1'),
    (CONCAT(@UUID_PREFIX, 'd7'),  @tournament_id, CONCAT(@UUID_PREFIX, '57'), @owner2_id, 'APPROVED', @reg_open, @admin_user_id, @reg_close, 100, 'CLASS_2'),
    (CONCAT(@UUID_PREFIX, 'd8'),  @tournament_id, CONCAT(@UUID_PREFIX, '58'), @owner2_id, 'APPROVED', @reg_open, @admin_user_id, @reg_close, 96,  'CLASS_2'),
    (CONCAT(@UUID_PREFIX, 'd9'),  @tournament_id, CONCAT(@UUID_PREFIX, '59'), @owner2_id, 'APPROVED', @reg_open, @admin_user_id, @reg_close, 93,  'CLASS_2'),
    (CONCAT(@UUID_PREFIX, 'da'),  @tournament_id, CONCAT(@UUID_PREFIX, '5a'), @owner2_id, 'APPROVED', @reg_open, @admin_user_id, @reg_close, 91,  'CLASS_2'),
    (CONCAT(@UUID_PREFIX, 'db'),  @tournament_id, CONCAT(@UUID_PREFIX, '5b'), @owner2_id, 'APPROVED', @reg_open, @admin_user_id, @reg_close, 87,  'CLASS_2'),
    (CONCAT(@UUID_PREFIX, 'dc'),  @tournament_id, CONCAT(@UUID_PREFIX, '5c'), @owner2_id, 'APPROVED', @reg_open, @admin_user_id, @reg_close, 84,  'CLASS_3'),
    (CONCAT(@UUID_PREFIX, 'dd'),  @tournament_id, CONCAT(@UUID_PREFIX, '5d'), @owner2_id, 'APPROVED', @reg_open, @admin_user_id, @reg_close, 81,  'CLASS_3'),
    (CONCAT(@UUID_PREFIX, 'de'),  @tournament_id, CONCAT(@UUID_PREFIX, '5e'), @owner2_id, 'APPROVED', @reg_open, @admin_user_id, @reg_close, 79,  'CLASS_3'),
    (CONCAT(@UUID_PREFIX, 'df'),  @tournament_id, CONCAT(@UUID_PREFIX, '5f'), @owner2_id, 'APPROVED', @reg_open, @admin_user_id, @reg_close, 76,  'CLASS_3'),
    (CONCAT(@UUID_PREFIX, 'e0'),  @tournament_id, CONCAT(@UUID_PREFIX, '60'), @owner2_id, 'APPROVED', @reg_open, @admin_user_id, @reg_close, 73,  'CLASS_4');

-- ============================================================================
-- 10. JOCKEY TOURNAMENT REGISTRATIONS (32 registrations, APPROVED)
-- ============================================================================
INSERT IGNORE INTO jockey_tournament_registrations
    (jockey_tournament_reg_id, tournament_id, jockey_id, status,
     submitted_at, reviewed_by, reviewed_at, note, hire_fee)
VALUES
    (CONCAT(@UUID_PREFIX, 'f1'),  @tournament_id, CONCAT(@UUID_PREFIX, '21'), 'APPROVED', @reg_open, @admin_user_id, @reg_close, 'Demo registration', 500000),
    (CONCAT(@UUID_PREFIX, 'f2'),  @tournament_id, CONCAT(@UUID_PREFIX, '22'), 'APPROVED', @reg_open, @admin_user_id, @reg_close, 'Demo registration', 500000),
    (CONCAT(@UUID_PREFIX, 'f3'),  @tournament_id, CONCAT(@UUID_PREFIX, '23'), 'APPROVED', @reg_open, @admin_user_id, @reg_close, 'Demo registration', 500000),
    (CONCAT(@UUID_PREFIX, 'f4'),  @tournament_id, CONCAT(@UUID_PREFIX, '24'), 'APPROVED', @reg_open, @admin_user_id, @reg_close, 'Demo registration', 500000),
    (CONCAT(@UUID_PREFIX, 'f5'),  @tournament_id, CONCAT(@UUID_PREFIX, '25'), 'APPROVED', @reg_open, @admin_user_id, @reg_close, 'Demo registration', 500000),
    (CONCAT(@UUID_PREFIX, 'f6'),  @tournament_id, CONCAT(@UUID_PREFIX, '26'), 'APPROVED', @reg_open, @admin_user_id, @reg_close, 'Demo registration', 500000),
    (CONCAT(@UUID_PREFIX, 'f7'),  @tournament_id, CONCAT(@UUID_PREFIX, '27'), 'APPROVED', @reg_open, @admin_user_id, @reg_close, 'Demo registration', 500000),
    (CONCAT(@UUID_PREFIX, 'f8'),  @tournament_id, CONCAT(@UUID_PREFIX, '28'), 'APPROVED', @reg_open, @admin_user_id, @reg_close, 'Demo registration', 500000),
    (CONCAT(@UUID_PREFIX, 'f9'),  @tournament_id, CONCAT(@UUID_PREFIX, '29'), 'APPROVED', @reg_open, @admin_user_id, @reg_close, 'Demo registration', 500000),
    (CONCAT(@UUID_PREFIX, 'fa'),  @tournament_id, CONCAT(@UUID_PREFIX, '2a'), 'APPROVED', @reg_open, @admin_user_id, @reg_close, 'Demo registration', 500000),
    (CONCAT(@UUID_PREFIX, 'fb'),  @tournament_id, CONCAT(@UUID_PREFIX, '2b'), 'APPROVED', @reg_open, @admin_user_id, @reg_close, 'Demo registration', 500000),
    (CONCAT(@UUID_PREFIX, 'fc'),  @tournament_id, CONCAT(@UUID_PREFIX, '2c'), 'APPROVED', @reg_open, @admin_user_id, @reg_close, 'Demo registration', 500000),
    (CONCAT(@UUID_PREFIX, 'fd'),  @tournament_id, CONCAT(@UUID_PREFIX, '2d'), 'APPROVED', @reg_open, @admin_user_id, @reg_close, 'Demo registration', 500000),
    (CONCAT(@UUID_PREFIX, 'fe'),  @tournament_id, CONCAT(@UUID_PREFIX, '2e'), 'APPROVED', @reg_open, @admin_user_id, @reg_close, 'Demo registration', 500000),
    (CONCAT(@UUID_PREFIX, 'ff'),  @tournament_id, CONCAT(@UUID_PREFIX, '2f'), 'APPROVED', @reg_open, @admin_user_id, @reg_close, 'Demo registration', 500000),
    (CONCAT(@UUID_PREFIX, '01'),  @tournament_id, CONCAT(@UUID_PREFIX, '30'), 'APPROVED', @reg_open, @admin_user_id, @reg_close, 'Demo registration', 500000),
    (CONCAT(@UUID_PREFIX, '02'),  @tournament_id, CONCAT(@UUID_PREFIX, '31'), 'APPROVED', @reg_open, @admin_user_id, @reg_close, 'Demo registration', 500000),
    (CONCAT(@UUID_PREFIX, '03'),  @tournament_id, CONCAT(@UUID_PREFIX, '32'), 'APPROVED', @reg_open, @admin_user_id, @reg_close, 'Demo registration', 500000),
    (CONCAT(@UUID_PREFIX, '04'),  @tournament_id, CONCAT(@UUID_PREFIX, '33'), 'APPROVED', @reg_open, @admin_user_id, @reg_close, 'Demo registration', 500000),
    (CONCAT(@UUID_PREFIX, '05'),  @tournament_id, CONCAT(@UUID_PREFIX, '34'), 'APPROVED', @reg_open, @admin_user_id, @reg_close, 'Demo registration', 500000),
    (CONCAT(@UUID_PREFIX, '06'),  @tournament_id, CONCAT(@UUID_PREFIX, '35'), 'APPROVED', @reg_open, @admin_user_id, @reg_close, 'Demo registration', 500000),
    (CONCAT(@UUID_PREFIX, '07'),  @tournament_id, CONCAT(@UUID_PREFIX, '36'), 'APPROVED', @reg_open, @admin_user_id, @reg_close, 'Demo registration', 500000),
    (CONCAT(@UUID_PREFIX, '08'),  @tournament_id, CONCAT(@UUID_PREFIX, '37'), 'APPROVED', @reg_open, @admin_user_id, @reg_close, 'Demo registration', 500000),
    (CONCAT(@UUID_PREFIX, '09'),  @tournament_id, CONCAT(@UUID_PREFIX, '38'), 'APPROVED', @reg_open, @admin_user_id, @reg_close, 'Demo registration', 500000),
    (CONCAT(@UUID_PREFIX, '0a'),  @tournament_id, CONCAT(@UUID_PREFIX, '39'), 'APPROVED', @reg_open, @admin_user_id, @reg_close, 'Demo registration', 500000),
    (CONCAT(@UUID_PREFIX, '0b'),  @tournament_id, CONCAT(@UUID_PREFIX, '3a'), 'APPROVED', @reg_open, @admin_user_id, @reg_close, 'Demo registration', 500000),
    (CONCAT(@UUID_PREFIX, '0c'),  @tournament_id, CONCAT(@UUID_PREFIX, '3b'), 'APPROVED', @reg_open, @admin_user_id, @reg_close, 'Demo registration', 500000),
    (CONCAT(@UUID_PREFIX, '0d'),  @tournament_id, CONCAT(@UUID_PREFIX, '3c'), 'APPROVED', @reg_open, @admin_user_id, @reg_close, 'Demo registration', 500000),
    (CONCAT(@UUID_PREFIX, '0e'),  @tournament_id, CONCAT(@UUID_PREFIX, '3d'), 'APPROVED', @reg_open, @admin_user_id, @reg_close, 'Demo registration', 500000),
    (CONCAT(@UUID_PREFIX, '0f'),  @tournament_id, CONCAT(@UUID_PREFIX, '3e'), 'APPROVED', @reg_open, @admin_user_id, @reg_close, 'Demo registration', 500000),
    (CONCAT(@UUID_PREFIX, '10'),  @tournament_id, CONCAT(@UUID_PREFIX, '3f'), 'APPROVED', @reg_open, @admin_user_id, @reg_close, 'Demo registration', 500000),
    (CONCAT(@UUID_PREFIX, '11'),  @tournament_id, CONCAT(@UUID_PREFIX, '40'), 'APPROVED', @reg_open, @admin_user_id, @reg_close, 'Demo registration', 500000);

-- ============================================================================
-- 11. JOCKEY-HORSE CONTRACTS (32 contracts, APPROVED)
-- ============================================================================
-- Mỗi contract ghép 1 horse registration với 1 jockey registration
-- Horse registration c1-d0 (owner 1) + jockey f1-f0, hire_fee 1,000,000
-- Horse registration d1-e0 (owner 2) + jockey 01-11, hire_fee 1,000,000
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
    -- Owner 1 contracts (16)
    (CONCAT(@UUID_PREFIX, '11'), @tournament_id, CONCAT(@UUID_PREFIX, 'c1'),  CONCAT(@UUID_PREFIX, 'f1'),  @owner1_id, CONCAT(@UUID_PREFIX, '41'), CONCAT(@UUID_PREFIX, '21'), 1000000, 50, 50, 500000, 500000, 50000, 60, 40, 'PAID', 'HELD', 'PAID', 'NOT_RELEASED', 'APPROVED', @reg_open, @reg_close, @reg_close, @reg_close, @admin_user_id, @reg_close),
    (CONCAT(@UUID_PREFIX, '12'), @tournament_id, CONCAT(@UUID_PREFIX, 'c2'),  CONCAT(@UUID_PREFIX, 'f2'),  @owner1_id, CONCAT(@UUID_PREFIX, '42'), CONCAT(@UUID_PREFIX, '22'), 1000000, 50, 50, 500000, 500000, 50000, 60, 40, 'PAID', 'HELD', 'PAID', 'NOT_RELEASED', 'APPROVED', @reg_open, @reg_close, @reg_close, @reg_close, @admin_user_id, @reg_close),
    (CONCAT(@UUID_PREFIX, '13'), @tournament_id, CONCAT(@UUID_PREFIX, 'c3'),  CONCAT(@UUID_PREFIX, 'f3'),  @owner1_id, CONCAT(@UUID_PREFIX, '43'), CONCAT(@UUID_PREFIX, '23'), 1000000, 50, 50, 500000, 500000, 50000, 60, 40, 'PAID', 'HELD', 'PAID', 'NOT_RELEASED', 'APPROVED', @reg_open, @reg_close, @reg_close, @reg_close, @admin_user_id, @reg_close),
    (CONCAT(@UUID_PREFIX, '14'), @tournament_id, CONCAT(@UUID_PREFIX, 'c4'),  CONCAT(@UUID_PREFIX, 'f4'),  @owner1_id, CONCAT(@UUID_PREFIX, '44'), CONCAT(@UUID_PREFIX, '24'), 1000000, 50, 50, 500000, 500000, 50000, 60, 40, 'PAID', 'HELD', 'PAID', 'NOT_RELEASED', 'APPROVED', @reg_open, @reg_close, @reg_close, @reg_close, @admin_user_id, @reg_close),
    (CONCAT(@UUID_PREFIX, '15'), @tournament_id, CONCAT(@UUID_PREFIX, 'c5'),  CONCAT(@UUID_PREFIX, 'f5'),  @owner1_id, CONCAT(@UUID_PREFIX, '45'), CONCAT(@UUID_PREFIX, '25'), 1000000, 50, 50, 500000, 500000, 50000, 60, 40, 'PAID', 'HELD', 'PAID', 'NOT_RELEASED', 'APPROVED', @reg_open, @reg_close, @reg_close, @reg_close, @admin_user_id, @reg_close),
    (CONCAT(@UUID_PREFIX, '16'), @tournament_id, CONCAT(@UUID_PREFIX, 'c6'),  CONCAT(@UUID_PREFIX, 'f6'),  @owner1_id, CONCAT(@UUID_PREFIX, '46'), CONCAT(@UUID_PREFIX, '26'), 1000000, 50, 50, 500000, 500000, 50000, 60, 40, 'PAID', 'HELD', 'PAID', 'NOT_RELEASED', 'APPROVED', @reg_open, @reg_close, @reg_close, @reg_close, @admin_user_id, @reg_close),
    (CONCAT(@UUID_PREFIX, '17'), @tournament_id, CONCAT(@UUID_PREFIX, 'c7'),  CONCAT(@UUID_PREFIX, 'f7'),  @owner1_id, CONCAT(@UUID_PREFIX, '47'), CONCAT(@UUID_PREFIX, '27'), 1000000, 50, 50, 500000, 500000, 50000, 60, 40, 'PAID', 'HELD', 'PAID', 'NOT_RELEASED', 'APPROVED', @reg_open, @reg_close, @reg_close, @reg_close, @admin_user_id, @reg_close),
    (CONCAT(@UUID_PREFIX, '18'), @tournament_id, CONCAT(@UUID_PREFIX, 'c8'),  CONCAT(@UUID_PREFIX, 'f8'),  @owner1_id, CONCAT(@UUID_PREFIX, '48'), CONCAT(@UUID_PREFIX, '28'), 1000000, 50, 50, 500000, 500000, 50000, 60, 40, 'PAID', 'HELD', 'PAID', 'NOT_RELEASED', 'APPROVED', @reg_open, @reg_close, @reg_close, @reg_close, @admin_user_id, @reg_close),
    (CONCAT(@UUID_PREFIX, '19'), @tournament_id, CONCAT(@UUID_PREFIX, 'c9'),  CONCAT(@UUID_PREFIX, 'f9'),  @owner1_id, CONCAT(@UUID_PREFIX, '49'), CONCAT(@UUID_PREFIX, '29'), 1000000, 50, 50, 500000, 500000, 50000, 60, 40, 'PAID', 'HELD', 'PAID', 'NOT_RELEASED', 'APPROVED', @reg_open, @reg_close, @reg_close, @reg_close, @admin_user_id, @reg_close),
    (CONCAT(@UUID_PREFIX, '1a'), @tournament_id, CONCAT(@UUID_PREFIX, 'ca'), CONCAT(@UUID_PREFIX, 'fa'), @owner1_id, CONCAT(@UUID_PREFIX, '4a'), CONCAT(@UUID_PREFIX, '2a'), 1000000, 50, 50, 500000, 500000, 50000, 60, 40, 'PAID', 'HELD', 'PAID', 'NOT_RELEASED', 'APPROVED', @reg_open, @reg_close, @reg_close, @reg_close, @admin_user_id, @reg_close),
    (CONCAT(@UUID_PREFIX, '1b'), @tournament_id, CONCAT(@UUID_PREFIX, 'cb'), CONCAT(@UUID_PREFIX, 'fb'), @owner1_id, CONCAT(@UUID_PREFIX, '4b'), CONCAT(@UUID_PREFIX, '2b'), 1000000, 50, 50, 500000, 500000, 50000, 60, 40, 'PAID', 'HELD', 'PAID', 'NOT_RELEASED', 'APPROVED', @reg_open, @reg_close, @reg_close, @reg_close, @admin_user_id, @reg_close),
    (CONCAT(@UUID_PREFIX, '1c'), @tournament_id, CONCAT(@UUID_PREFIX, 'cc'), CONCAT(@UUID_PREFIX, 'fc'), @owner1_id, CONCAT(@UUID_PREFIX, '4c'), CONCAT(@UUID_PREFIX, '2c'), 1000000, 50, 50, 500000, 500000, 50000, 60, 40, 'PAID', 'HELD', 'PAID', 'NOT_RELEASED', 'APPROVED', @reg_open, @reg_close, @reg_close, @reg_close, @admin_user_id, @reg_close),
    (CONCAT(@UUID_PREFIX, '1d'), @tournament_id, CONCAT(@UUID_PREFIX, 'cd'), CONCAT(@UUID_PREFIX, 'fd'), @owner1_id, CONCAT(@UUID_PREFIX, '4d'), CONCAT(@UUID_PREFIX, '2d'), 1000000, 50, 50, 500000, 500000, 50000, 60, 40, 'PAID', 'HELD', 'PAID', 'NOT_RELEASED', 'APPROVED', @reg_open, @reg_close, @reg_close, @reg_close, @admin_user_id, @reg_close),
    (CONCAT(@UUID_PREFIX, '1e'), @tournament_id, CONCAT(@UUID_PREFIX, 'ce'), CONCAT(@UUID_PREFIX, 'fe'), @owner1_id, CONCAT(@UUID_PREFIX, '4e'), CONCAT(@UUID_PREFIX, '2e'), 1000000, 50, 50, 500000, 500000, 50000, 60, 40, 'PAID', 'HELD', 'PAID', 'NOT_RELEASED', 'APPROVED', @reg_open, @reg_close, @reg_close, @reg_close, @admin_user_id, @reg_close),
    (CONCAT(@UUID_PREFIX, '1f'), @tournament_id, CONCAT(@UUID_PREFIX, 'cf'), CONCAT(@UUID_PREFIX, 'ff'), @owner1_id, CONCAT(@UUID_PREFIX, '4f'), CONCAT(@UUID_PREFIX, '2f'), 1000000, 50, 50, 500000, 500000, 50000, 60, 40, 'PAID', 'HELD', 'PAID', 'NOT_RELEASED', 'APPROVED', @reg_open, @reg_close, @reg_close, @reg_close, @admin_user_id, @reg_close),
    (CONCAT(@UUID_PREFIX, '20'), @tournament_id, CONCAT(@UUID_PREFIX, 'd0'), CONCAT(@UUID_PREFIX, '01'), @owner1_id, CONCAT(@UUID_PREFIX, '50'), CONCAT(@UUID_PREFIX, '30'), 1000000, 50, 50, 500000, 500000, 50000, 60, 40, 'PAID', 'HELD', 'PAID', 'NOT_RELEASED', 'APPROVED', @reg_open, @reg_close, @reg_close, @reg_close, @admin_user_id, @reg_close),
    -- Owner 2 contracts (16)
    (CONCAT(@UUID_PREFIX, '21'), @tournament_id, CONCAT(@UUID_PREFIX, 'd1'), CONCAT(@UUID_PREFIX, '02'), @owner2_id, CONCAT(@UUID_PREFIX, '51'), CONCAT(@UUID_PREFIX, '31'), 1000000, 50, 50, 500000, 500000, 50000, 60, 40, 'PAID', 'HELD', 'PAID', 'NOT_RELEASED', 'APPROVED', @reg_open, @reg_close, @reg_close, @reg_close, @admin_user_id, @reg_close),
    (CONCAT(@UUID_PREFIX, '22'), @tournament_id, CONCAT(@UUID_PREFIX, 'd2'), CONCAT(@UUID_PREFIX, '03'), @owner2_id, CONCAT(@UUID_PREFIX, '52'), CONCAT(@UUID_PREFIX, '32'), 1000000, 50, 50, 500000, 500000, 50000, 60, 40, 'PAID', 'HELD', 'PAID', 'NOT_RELEASED', 'APPROVED', @reg_open, @reg_close, @reg_close, @reg_close, @admin_user_id, @reg_close),
    (CONCAT(@UUID_PREFIX, '23'), @tournament_id, CONCAT(@UUID_PREFIX, 'd3'), CONCAT(@UUID_PREFIX, '04'), @owner2_id, CONCAT(@UUID_PREFIX, '53'), CONCAT(@UUID_PREFIX, '33'), 1000000, 50, 50, 500000, 500000, 50000, 60, 40, 'PAID', 'HELD', 'PAID', 'NOT_RELEASED', 'APPROVED', @reg_open, @reg_close, @reg_close, @reg_close, @admin_user_id, @reg_close),
    (CONCAT(@UUID_PREFIX, '24'), @tournament_id, CONCAT(@UUID_PREFIX, 'd4'), CONCAT(@UUID_PREFIX, '05'), @owner2_id, CONCAT(@UUID_PREFIX, '54'), CONCAT(@UUID_PREFIX, '34'), 1000000, 50, 50, 500000, 500000, 50000, 60, 40, 'PAID', 'HELD', 'PAID', 'NOT_RELEASED', 'APPROVED', @reg_open, @reg_close, @reg_close, @reg_close, @admin_user_id, @reg_close),
    (CONCAT(@UUID_PREFIX, '25'), @tournament_id, CONCAT(@UUID_PREFIX, 'd5'), CONCAT(@UUID_PREFIX, '06'), @owner2_id, CONCAT(@UUID_PREFIX, '55'), CONCAT(@UUID_PREFIX, '35'), 1000000, 50, 50, 500000, 500000, 50000, 60, 40, 'PAID', 'HELD', 'PAID', 'NOT_RELEASED', 'APPROVED', @reg_open, @reg_close, @reg_close, @reg_close, @admin_user_id, @reg_close),
    (CONCAT(@UUID_PREFIX, '26'), @tournament_id, CONCAT(@UUID_PREFIX, 'd6'), CONCAT(@UUID_PREFIX, '07'), @owner2_id, CONCAT(@UUID_PREFIX, '56'), CONCAT(@UUID_PREFIX, '36'), 1000000, 50, 50, 500000, 500000, 50000, 60, 40, 'PAID', 'HELD', 'PAID', 'NOT_RELEASED', 'APPROVED', @reg_open, @reg_close, @reg_close, @reg_close, @admin_user_id, @reg_close),
    (CONCAT(@UUID_PREFIX, '27'), @tournament_id, CONCAT(@UUID_PREFIX, 'd7'), CONCAT(@UUID_PREFIX, '08'), @owner2_id, CONCAT(@UUID_PREFIX, '57'), CONCAT(@UUID_PREFIX, '37'), 1000000, 50, 50, 500000, 500000, 50000, 60, 40, 'PAID', 'HELD', 'PAID', 'NOT_RELEASED', 'APPROVED', @reg_open, @reg_close, @reg_close, @reg_close, @admin_user_id, @reg_close),
    (CONCAT(@UUID_PREFIX, '28'), @tournament_id, CONCAT(@UUID_PREFIX, 'd8'), CONCAT(@UUID_PREFIX, '09'), @owner2_id, CONCAT(@UUID_PREFIX, '58'), CONCAT(@UUID_PREFIX, '38'), 1000000, 50, 50, 500000, 500000, 50000, 60, 40, 'PAID', 'HELD', 'PAID', 'NOT_RELEASED', 'APPROVED', @reg_open, @reg_close, @reg_close, @reg_close, @admin_user_id, @reg_close),
    (CONCAT(@UUID_PREFIX, '29'), @tournament_id, CONCAT(@UUID_PREFIX, 'd9'), CONCAT(@UUID_PREFIX, '0a'), @owner2_id, CONCAT(@UUID_PREFIX, '59'), CONCAT(@UUID_PREFIX, '39'), 1000000, 50, 50, 500000, 500000, 50000, 60, 40, 'PAID', 'HELD', 'PAID', 'NOT_RELEASED', 'APPROVED', @reg_open, @reg_close, @reg_close, @reg_close, @admin_user_id, @reg_close),
    (CONCAT(@UUID_PREFIX, '2a'), @tournament_id, CONCAT(@UUID_PREFIX, 'da'), CONCAT(@UUID_PREFIX, '0b'), @owner2_id, CONCAT(@UUID_PREFIX, '5a'), CONCAT(@UUID_PREFIX, '3a'), 1000000, 50, 50, 500000, 500000, 50000, 60, 40, 'PAID', 'HELD', 'PAID', 'NOT_RELEASED', 'APPROVED', @reg_open, @reg_close, @reg_close, @reg_close, @admin_user_id, @reg_close),
    (CONCAT(@UUID_PREFIX, '2b'), @tournament_id, CONCAT(@UUID_PREFIX, 'db'), CONCAT(@UUID_PREFIX, '0c'), @owner2_id, CONCAT(@UUID_PREFIX, '5b'), CONCAT(@UUID_PREFIX, '3b'), 1000000, 50, 50, 500000, 500000, 50000, 60, 40, 'PAID', 'HELD', 'PAID', 'NOT_RELEASED', 'APPROVED', @reg_open, @reg_close, @reg_close, @reg_close, @admin_user_id, @reg_close),
    (CONCAT(@UUID_PREFIX, '2c'), @tournament_id, CONCAT(@UUID_PREFIX, 'dc'), CONCAT(@UUID_PREFIX, '0d'), @owner2_id, CONCAT(@UUID_PREFIX, '5c'), CONCAT(@UUID_PREFIX, '3c'), 1000000, 50, 50, 500000, 500000, 50000, 60, 40, 'PAID', 'HELD', 'PAID', 'NOT_RELEASED', 'APPROVED', @reg_open, @reg_close, @reg_close, @reg_close, @admin_user_id, @reg_close),
    (CONCAT(@UUID_PREFIX, '2d'), @tournament_id, CONCAT(@UUID_PREFIX, 'dd'), CONCAT(@UUID_PREFIX, '0e'), @owner2_id, CONCAT(@UUID_PREFIX, '5d'), CONCAT(@UUID_PREFIX, '3d'), 1000000, 50, 50, 500000, 500000, 50000, 60, 40, 'PAID', 'HELD', 'PAID', 'NOT_RELEASED', 'APPROVED', @reg_open, @reg_close, @reg_close, @reg_close, @admin_user_id, @reg_close),
    (CONCAT(@UUID_PREFIX, '2e'), @tournament_id, CONCAT(@UUID_PREFIX, 'de'), CONCAT(@UUID_PREFIX, '0f'), @owner2_id, CONCAT(@UUID_PREFIX, '5e'), CONCAT(@UUID_PREFIX, '3e'), 1000000, 50, 50, 500000, 500000, 50000, 60, 40, 'PAID', 'HELD', 'PAID', 'NOT_RELEASED', 'APPROVED', @reg_open, @reg_close, @reg_close, @reg_close, @admin_user_id, @reg_close),
    (CONCAT(@UUID_PREFIX, '2f'), @tournament_id, CONCAT(@UUID_PREFIX, 'df'), CONCAT(@UUID_PREFIX, '10'), @owner2_id, CONCAT(@UUID_PREFIX, '5f'), CONCAT(@UUID_PREFIX, '3f'), 1000000, 50, 50, 500000, 500000, 50000, 60, 40, 'PAID', 'HELD', 'PAID', 'NOT_RELEASED', 'APPROVED', @reg_open, @reg_close, @reg_close, @reg_close, @admin_user_id, @reg_close),
    (CONCAT(@UUID_PREFIX, '30'), @tournament_id, CONCAT(@UUID_PREFIX, 'e0'), CONCAT(@UUID_PREFIX, '11'), @owner2_id, CONCAT(@UUID_PREFIX, '60'), CONCAT(@UUID_PREFIX, '40'), 1000000, 50, 50, 500000, 500000, 50000, 60, 40, 'PAID', 'HELD', 'PAID', 'NOT_RELEASED', 'APPROVED', @reg_open, @reg_close, @reg_close, @reg_close, @admin_user_id, @reg_close);

-- ============================================================================
-- RESTORE SETTINGS
-- ============================================================================
SET FOREIGN_KEY_CHECKS = @OLD_FOREIGN_KEY_CHECKS;
SET SQL_SAFE_UPDATES = @OLD_SQL_SAFE_UPDATES;

-- ============================================================================
-- HƯỚNG DẪN TEST
-- ============================================================================
-- Sau khi chạy seed xong:
--
-- Bước 1: Bracket Confirm
--   POST /api/admin/tournaments/{tournament_id}/bracket-confirm
--   Body: {"maxApprovedEntries": 32, "expectedPlanVersion": 1}
--   Headers: Authorization: Bearer {admin_token}
--
--   => System tạo 2 round skeleton:
--      - Vòng 1: 2 races × 16 entries
--      - Chung Kết: 1 race × 8 qualifiers
--   => 32 entries được phân phối vào 2 race vòng 1 (16 mỗi race)
--
-- Bước 2: Schedule Proposal
--   GET /api/admin/tournaments/{tournament_id}/schedule-proposal
--   => Xem lịch đề xuất cho từng race
--
-- Bước 3: Xác nhận schedule (nếu API có)
--   POST /api/admin/tournaments/{tournament_id}/schedule
-- ============================================================================

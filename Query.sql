USE SWP391_Project_HRTMS;

-- 1. Lấy role_id của vai trò ADMIN trong bảng roles
SET @admin_role_id = (SELECT role_id FROM roles WHERE role_name = 'ADMIN' LIMIT 1);

-- 2. Chèn tài khoản Admin mẫu
INSERT INTO users (
    user_id,
    username,
    password,
    email,
    dob,
    gender,
    full_name,
    phone_number,
    status,
    role_id,
    created_at
)
VALUES (
           UUID(),
           'admin',
           '$2a$12$AsBCvrsZ2yvqd7RyEflkfOGwfTewt8CSx40CKh0ZIZuD4WZ49wo4a', -- Mã hóa BCrypt của mật khẩu: admin123
           'admin@horseracing.com',
           '2000-01-01',
           'MALE',
           'System Administrator',
           '0987654321',
           'ACTIVE',
           @admin_role_id,
           NOW()
       )
    ON DUPLICATE KEY UPDATE
                         password = VALUES(password),
                         email = VALUES(email),
                         full_name = VALUES(full_name),
                         status = VALUES(status),
                         role_id = VALUES(role_id);

USE SWP391_Project_HRTMS;

-- Xóa các bản ghi trùng lặp (nếu có) trước khi chèn mới để tránh lỗi trùng khóa UNIQUE
INSERT INTO roles (role_id, role_name, description, is_active, created_at)
VALUES
    (UUID(), 'ADMIN', 'Administrator of the system', 1, NOW()),
    (UUID(), 'HORSE_OWNER', 'Owner of the racing horses', 1, NOW()),
    (UUID(), 'JOCKEY', 'Professional horse rider', 1, NOW()),
    (UUID(), 'SPECTATOR', 'Audience / Spectator of the race', 1, NOW()),
    (UUID(), 'REFEREE', 'Official referee of the race', 1, NOW()),
    (UUID(), 'VETERINARIAN', 'Veterinarian checking horse health', 1, NOW()),
    (UUID(), 'MEDICAL_STAFF', 'Medical staff checking jockey health', 1, NOW())
    ON DUPLICATE KEY UPDATE
                         description = VALUES(description),
                         is_active = VALUES(is_active);


SET FOREIGN_KEY_CHECKS = 0;

SET SESSION group_concat_max_len = 1000000;

SELECT GROUP_CONCAT(
               CONCAT('`', table_name, '`')
                   SEPARATOR ', '
       )
INTO @table_names
FROM information_schema.tables
WHERE table_schema = DATABASE()
  AND table_type = 'BASE TABLE';

SET @drop_sql = IF(
        @table_names IS NULL,
        'SELECT "Database không có table nào";',
        CONCAT('DROP TABLE IF EXISTS ', @table_names, ';')
                );

PREPARE drop_statement FROM @drop_sql;
EXECUTE drop_statement;
DEALLOCATE PREPARE drop_statement;

SET FOREIGN_KEY_CHECKS = 1;
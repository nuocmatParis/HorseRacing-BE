-- Add tournament phase timeline columns (develop pull: API-Tournament_schedule_timeline)
USE swp391_project_hrtms;

SET @db := DATABASE();

-- registration_open_at
SET @exists := (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'tournaments' AND COLUMN_NAME = 'registration_open_at'
);
SET @sql := IF(@exists = 0,
    'ALTER TABLE tournaments ADD COLUMN registration_open_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP AFTER published_at',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- registration_close_at
SET @exists := (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'tournaments' AND COLUMN_NAME = 'registration_close_at'
);
SET @sql := IF(@exists = 0,
    'ALTER TABLE tournaments ADD COLUMN registration_close_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP AFTER registration_open_at',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- review_deadline_at
SET @exists := (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'tournaments' AND COLUMN_NAME = 'review_deadline_at'
);
SET @sql := IF(@exists = 0,
    'ALTER TABLE tournaments ADD COLUMN review_deadline_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP AFTER registration_close_at',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- jockey_matching_deadline_at
SET @exists := (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'tournaments' AND COLUMN_NAME = 'jockey_matching_deadline_at'
);
SET @sql := IF(@exists = 0,
    'ALTER TABLE tournaments ADD COLUMN jockey_matching_deadline_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP AFTER review_deadline_at',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- scheduling_deadline_at
SET @exists := (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'tournaments' AND COLUMN_NAME = 'scheduling_deadline_at'
);
SET @sql := IF(@exists = 0,
    'ALTER TABLE tournaments ADD COLUMN scheduling_deadline_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP AFTER jockey_matching_deadline_at',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- Fix invalid zero dates (MySQL strict mode rejects '0000-00-00' reads/writes)
SET @old_sql_mode = @@SESSION.sql_mode;
SET SESSION sql_mode = 'ALLOW_INVALID_DATES';

UPDATE tournaments
SET registration_open_at = COALESCE(
        NULLIF(registration_open_at, '0000-00-00 00:00:00'),
        created_at,
        TIMESTAMP(start_date),
        NOW()
    );

UPDATE tournaments
SET registration_close_at = COALESCE(
        NULLIF(registration_close_at, '0000-00-00 00:00:00'),
        DATE_ADD(registration_open_at, INTERVAL 7 DAY)
    );

UPDATE tournaments
SET review_deadline_at = COALESCE(
        NULLIF(review_deadline_at, '0000-00-00 00:00:00'),
        DATE_ADD(registration_close_at, INTERVAL 3 DAY)
    );

UPDATE tournaments
SET jockey_matching_deadline_at = COALESCE(
        NULLIF(jockey_matching_deadline_at, '0000-00-00 00:00:00'),
        DATE_ADD(review_deadline_at, INTERVAL 3 DAY)
    );

UPDATE tournaments
SET scheduling_deadline_at = COALESCE(
        NULLIF(scheduling_deadline_at, '0000-00-00 00:00:00'),
        DATE_ADD(jockey_matching_deadline_at, INTERVAL 3 DAY)
    );

SET SESSION sql_mode = @old_sql_mode;

SELECT 'tournament timeline columns migration completed' AS message;

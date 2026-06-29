-- Fix invoices FK: old DB pointed tournament_reg_id -> tournament_registration (deprecated)
-- Backend expects tournament_reg_id -> horse_tournament_registrations(horse_tournament_reg_id)

USE swp391_project_hrtms;

SET FOREIGN_KEY_CHECKS = 0;

-- Drop legacy FK if it references old table name
SET @fk_exists := (
    SELECT COUNT(*)
    FROM information_schema.TABLE_CONSTRAINTS
    WHERE CONSTRAINT_SCHEMA = DATABASE()
      AND TABLE_NAME = 'invoices'
      AND CONSTRAINT_NAME = 'fk_invoice_tournament_registration'
      AND CONSTRAINT_TYPE = 'FOREIGN KEY'
);

SET @sql_drop := IF(
    @fk_exists > 0,
    'ALTER TABLE invoices DROP FOREIGN KEY fk_invoice_tournament_registration',
    'SELECT 1'
);
PREPARE stmt FROM @sql_drop;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- Ensure horse_tournament_registrations exists (skip if already migrated)
CREATE TABLE IF NOT EXISTS horse_tournament_registrations (
    horse_tournament_reg_id CHAR(36) PRIMARY KEY,
    tournament_id CHAR(36) NOT NULL,
    horse_id CHAR(36) NOT NULL,
    owner_id CHAR(36) NULL,
    status ENUM(
        'PENDING_PAYMENT',
        'PENDING_REVIEW',
        'APPROVED',
        'REJECTED',
        'WITHDRAWN'
    ) NOT NULL DEFAULT 'PENDING_PAYMENT',
    submitted_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    reviewed_by CHAR(36) NULL,
    reviewed_at TIMESTAMP NULL,
    rejected_reason TEXT,
    withdrawn_at TIMESTAMP NULL,
    withdraw_reason TEXT,
    note TEXT,
    UNIQUE KEY uk_tournament_horse (tournament_id, horse_id)
);

-- Add missing columns on horse_tournament_registrations if table was old
SET @col_exists := (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'horse_tournament_registrations'
      AND COLUMN_NAME = 'withdraw_reason'
);
SET @sql_col := IF(
    @col_exists = 0,
    'ALTER TABLE horse_tournament_registrations ADD COLUMN withdraw_reason TEXT NULL AFTER withdrawn_at',
    'SELECT 1'
);
PREPARE stmt FROM @sql_col;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- Re-add correct FK
ALTER TABLE invoices
    ADD CONSTRAINT fk_invoice_tournament_registration
        FOREIGN KEY (tournament_reg_id)
        REFERENCES horse_tournament_registrations (horse_tournament_reg_id);

-- Fix jockey invoice FK if still pointing to wrong table
SET @jfk_exists := (
    SELECT COUNT(*)
    FROM information_schema.TABLE_CONSTRAINTS
    WHERE CONSTRAINT_SCHEMA = DATABASE()
      AND TABLE_NAME = 'invoices'
      AND CONSTRAINT_NAME = 'fk_invoice_jockey_tournament_registration'
      AND CONSTRAINT_TYPE = 'FOREIGN KEY'
);

SET @sql_jdrop := IF(
    @jfk_exists > 0,
    'ALTER TABLE invoices DROP FOREIGN KEY fk_invoice_jockey_tournament_registration',
    'SELECT 1'
);
PREPARE stmt FROM @sql_jdrop;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

CREATE TABLE IF NOT EXISTS jockey_tournament_registrations (
    jockey_tournament_reg_id CHAR(36) PRIMARY KEY,
    tournament_id CHAR(36) NOT NULL,
    jockey_id CHAR(36) NOT NULL,
    status ENUM(
        'PENDING_PAYMENT',
        'PENDING_REVIEW',
        'APPROVED',
        'REJECTED',
        'WITHDRAWN'
    ) NOT NULL DEFAULT 'PENDING_PAYMENT',
    submitted_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    reviewed_by CHAR(36) NULL,
    reviewed_at TIMESTAMP NULL,
    rejected_reason TEXT,
    withdrawn_at TIMESTAMP NULL,
    withdraw_reason TEXT NULL,
    note TEXT,
    UNIQUE KEY uk_tournament_jockey (tournament_id, jockey_id)
);

ALTER TABLE invoices
    ADD CONSTRAINT fk_invoice_jockey_tournament_registration
        FOREIGN KEY (jockey_tournament_reg_id)
        REFERENCES jockey_tournament_registrations (jockey_tournament_reg_id);

SET FOREIGN_KEY_CHECKS = 1;

SELECT 'invoices FK migration completed' AS message;

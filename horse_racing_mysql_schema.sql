-- ============================================================
-- Horse Racing Tournament Management System - MySQL 8 Schema
-- Author: generated for SWP391 project
-- Notes:
--   - UUID is implemented as CHAR(36) DEFAULT (UUID()) for readability.
--   - Money fields use DECIMAL(15,2).
--   - Important percentage / measurement fields use DECIMAL instead of FLOAT.
--   - Enum values are normalized to UPPER_SNAKE_CASE for Java/Spring enum mapping.
-- ============================================================

-- ============================================================
-- 00. CREATE DATABASE + NON-ROOT APP USER
-- Run this section once using a MySQL admin/root account.
-- Your Spring Boot app should connect using horse_app, not root.
-- ============================================================



CREATE DATABASE IF NOT EXISTS SWP391_Project_HRTMS
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;

CREATE USER IF NOT EXISTS 'horse_app'@'localhost' IDENTIFIED BY 'horse_app';

GRANT ALL PRIVILEGES
ON SWP391_Project_HRTMS.* TO 'horse_app'@'localhost';

FLUSH PRIVILEGES;

USE SWP391_Project_HRTMS;

SET FOREIGN_KEY_CHECKS = 0;

-- ============================================================
-- 01. ROLE + USER + ACTOR PROFILES
-- ============================================================

CREATE TABLE IF NOT EXISTS roles (
    role_id CHAR(36) NOT NULL DEFAULT (UUID()),
    role_name VARCHAR(50) NOT NULL,
    description TEXT NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (role_id),
    UNIQUE KEY uk_roles_role_name (role_name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS users (
    user_id CHAR(36) NOT NULL DEFAULT (UUID()),
    role_id CHAR(36) NOT NULL,
    username VARCHAR(50) NOT NULL,
    password VARCHAR(255) NOT NULL,
    email VARCHAR(100) NOT NULL,
    dob DATE NULL,
    gender ENUM('MALE', 'FEMALE', 'OTHER') NULL,
    full_name VARCHAR(100) NOT NULL,
    phone_number VARCHAR(20) NULL,
    status ENUM('ACTIVE', 'INACTIVE', 'BANNED') NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_login_at TIMESTAMP NULL,
    PRIMARY KEY (user_id),
    UNIQUE KEY uk_users_username (username),
    UNIQUE KEY uk_users_email (email),
    KEY idx_users_role_id (role_id),
    CONSTRAINT fk_users_role
        FOREIGN KEY (role_id) REFERENCES roles(role_id)
        ON UPDATE CASCADE ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS horse_owners (
    owner_id CHAR(36) NOT NULL DEFAULT (UUID()),
    user_id CHAR(36) NOT NULL,
    farm_name VARCHAR(100) NULL,
    address TEXT NULL,
    license_number VARCHAR(50) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (owner_id),
    UNIQUE KEY uk_horse_owners_user_id (user_id),
    UNIQUE KEY uk_horse_owners_license_number (license_number),
    CONSTRAINT fk_horse_owners_user
        FOREIGN KEY (user_id) REFERENCES users(user_id)
        ON UPDATE CASCADE ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS jockeys (
    jockey_id CHAR(36) NOT NULL DEFAULT (UUID()),
    user_id CHAR(36) NOT NULL,
    height DECIMAL(5,2) NULL,
    weight DECIMAL(5,2) NULL,
    experience_years INT NOT NULL DEFAULT 0,
    license_number VARCHAR(50) NOT NULL,
    specialization VARCHAR(100) NULL,
    hire_fee DECIMAL(15,2) NOT NULL DEFAULT 0.00,
    status ENUM('AVAILABLE', 'BUSY', 'SUSPENDED', 'INACTIVE') NOT NULL DEFAULT 'AVAILABLE',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (jockey_id),
    UNIQUE KEY uk_jockeys_user_id (user_id),
    UNIQUE KEY uk_jockeys_license_number (license_number),
    KEY idx_jockeys_status (status),
    CONSTRAINT fk_jockeys_user
        FOREIGN KEY (user_id) REFERENCES users(user_id)
        ON UPDATE CASCADE ON DELETE RESTRICT,
    CONSTRAINT chk_jockeys_experience_non_negative CHECK (experience_years >= 0),
    CONSTRAINT chk_jockeys_hire_fee_non_negative CHECK (hire_fee >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS referees (
    referee_id CHAR(36) NOT NULL DEFAULT (UUID()),
    user_id CHAR(36) NOT NULL,
    certification_level VARCHAR(50) NULL,
    years_of_service INT NOT NULL DEFAULT 0,
    status ENUM('AVAILABLE', 'ASSIGNED', 'SUSPENDED') NOT NULL DEFAULT 'AVAILABLE',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (referee_id),
    UNIQUE KEY uk_referees_user_id (user_id),
    KEY idx_referees_status (status),
    CONSTRAINT fk_referees_user
        FOREIGN KEY (user_id) REFERENCES users(user_id)
        ON UPDATE CASCADE ON DELETE RESTRICT,
    CONSTRAINT chk_referees_years_non_negative CHECK (years_of_service >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS spectators (
    spectator_id CHAR(36) NOT NULL DEFAULT (UUID()),
    user_id CHAR(36) NOT NULL,
    total_points INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (spectator_id),
    UNIQUE KEY uk_spectators_user_id (user_id),
    CONSTRAINT fk_spectators_user
        FOREIGN KEY (user_id) REFERENCES users(user_id)
        ON UPDATE CASCADE ON DELETE RESTRICT,
    CONSTRAINT chk_spectators_points_non_negative CHECK (total_points >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS veterinarians (
    vet_id CHAR(36) NOT NULL DEFAULT (UUID()),
    user_id CHAR(36) NOT NULL,
    license_number VARCHAR(50) NOT NULL,
    specialization VARCHAR(100) NULL,
    years_of_service INT NOT NULL DEFAULT 0,
    status ENUM('AVAILABLE', 'ASSIGNED', 'SUSPENDED') NOT NULL DEFAULT 'AVAILABLE',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (vet_id),
    UNIQUE KEY uk_veterinarians_user_id (user_id),
    UNIQUE KEY uk_veterinarians_license_number (license_number),
    KEY idx_veterinarians_status (status),
    CONSTRAINT fk_veterinarians_user
        FOREIGN KEY (user_id) REFERENCES users(user_id)
        ON UPDATE CASCADE ON DELETE RESTRICT,
    CONSTRAINT chk_veterinarians_years_non_negative CHECK (years_of_service >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS medical_staff (
    med_staff_id CHAR(36) NOT NULL DEFAULT (UUID()),
    user_id CHAR(36) NOT NULL,
    certification VARCHAR(100) NULL,
    years_of_service INT NOT NULL DEFAULT 0,
    status ENUM('AVAILABLE', 'ASSIGNED', 'SUSPENDED') NOT NULL DEFAULT 'AVAILABLE',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (med_staff_id),
    UNIQUE KEY uk_medical_staff_user_id (user_id),
    KEY idx_medical_staff_status (status),
    CONSTRAINT fk_medical_staff_user
        FOREIGN KEY (user_id) REFERENCES users(user_id)
        ON UPDATE CASCADE ON DELETE RESTRICT,
    CONSTRAINT chk_medical_staff_years_non_negative CHECK (years_of_service >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================================
-- 02. HORSE
-- ============================================================

CREATE TABLE IF NOT EXISTS horses (
    horse_id CHAR(36) NOT NULL DEFAULT (UUID()),
    owner_id CHAR(36) NOT NULL,
    name VARCHAR(100) NOT NULL,
    breed VARCHAR(100) NOT NULL,
    gender ENUM('MALE', 'FEMALE') NOT NULL,
    age INT NOT NULL,
    weight DECIMAL(7,2) NULL,
    color VARCHAR(50) NULL,
    health_status ENUM('HEALTHY', 'INJURED', 'RETIRED') NOT NULL DEFAULT 'HEALTHY',
    race_class VARCHAR(50) NULL,
    total_races INT NOT NULL DEFAULT 0,
    total_wins INT NOT NULL DEFAULT 0,
    win_rate DECIMAL(5,2) NOT NULL DEFAULT 0.00,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (horse_id),
    KEY idx_horses_owner_id (owner_id),
    KEY idx_horses_breed (breed),
    KEY idx_horses_health_status (health_status),
    UNIQUE KEY uk_horses_owner_name (owner_id, name),
    CONSTRAINT fk_horses_owner
        FOREIGN KEY (owner_id) REFERENCES horse_owners(owner_id)
        ON UPDATE CASCADE ON DELETE RESTRICT,
    CONSTRAINT chk_horses_age_non_negative CHECK (age >= 0),
    CONSTRAINT chk_horses_total_races_non_negative CHECK (total_races >= 0),
    CONSTRAINT chk_horses_total_wins_non_negative CHECK (total_wins >= 0),
    CONSTRAINT chk_horses_win_rate_range CHECK (win_rate >= 0 AND win_rate <= 100)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================================
-- 03. WALLET
-- ============================================================

CREATE TABLE IF NOT EXISTS wallets (
    wallet_id CHAR(36) NOT NULL DEFAULT (UUID()),
    owner_type ENUM('USER', 'SYSTEM') NOT NULL,
    user_id CHAR(36) NULL,
    balance DECIMAL(15,2) NOT NULL DEFAULT 0.00,
    currency VARCHAR(10) NOT NULL DEFAULT 'VND',
    status ENUM('ACTIVE', 'FROZEN', 'CLOSED') NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    system_wallet_key TINYINT GENERATED ALWAYS AS (
        CASE WHEN owner_type = 'SYSTEM' THEN 1 ELSE NULL END
    ) STORED,
    PRIMARY KEY (wallet_id),
    UNIQUE KEY uk_wallets_user_id (user_id),
    UNIQUE KEY uk_wallets_one_system_wallet (system_wallet_key),
    KEY idx_wallets_owner_type (owner_type),
    CONSTRAINT fk_wallets_user
        FOREIGN KEY (user_id) REFERENCES users(user_id)
        ON UPDATE CASCADE ON DELETE RESTRICT,
    CONSTRAINT chk_wallets_owner_type_user_id CHECK (
        (owner_type = 'USER' AND user_id IS NOT NULL)
        OR
        (owner_type = 'SYSTEM' AND user_id IS NULL)
    ),
    CONSTRAINT chk_wallets_balance_non_negative CHECK (balance >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================================
-- 04. TOURNAMENT CONFIG
-- ============================================================

CREATE TABLE IF NOT EXISTS tournaments (
    tournament_id CHAR(36) NOT NULL DEFAULT (UUID()),
    created_by CHAR(36) NOT NULL,
    name VARCHAR(150) NOT NULL,
    description TEXT NULL,
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    finished_at TIMESTAMP NULL,
    location VARCHAR(200) NULL,
    registration_fee DECIMAL(15,2) NOT NULL DEFAULT 0.00,
    jockey_registration_fee DECIMAL(15,2) NOT NULL DEFAULT 0.00,
    system_contract_fee DECIMAL(15,2) NOT NULL DEFAULT 0.00,
    total_prize_pool DECIMAL(15,2) NOT NULL DEFAULT 0.00,
    allowed_breed VARCHAR(100) NULL,
    race_class VARCHAR(50) NULL,
    weight_class VARCHAR(50) NULL,
    min_horse_age INT NULL,
    max_horse_age INT NULL,
    tournament_division VARCHAR(100) NULL,
    handicap_rule TEXT NULL,
    prediction_open_at DATETIME NULL,
    prediction_close_at DATETIME NULL,
    prediction_reward_rule TEXT NULL,
    status ENUM('DRAFT', 'OPEN', 'ONGOING', 'FINISHED', 'CANCELLED') NOT NULL DEFAULT 'DRAFT',
    phase ENUM(
        'DRAFT',
        'REGISTRATION_OPEN',
        'REGISTRATION_REVIEW',
        'JOCKEY_MATCHING',
        'SCHEDULING',
        'PREDICTION_OPEN',
        'RACING',
        'RESULT_PENDING',
        'RESULT_PUBLISHED',
        'FINISHED'
    ) NOT NULL DEFAULT 'DRAFT',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    published_at TIMESTAMP NULL,
    PRIMARY KEY (tournament_id),
    KEY idx_tournaments_created_by (created_by),
    KEY idx_tournaments_status_phase (status, phase),
    KEY idx_tournaments_dates (start_date, end_date),
    CONSTRAINT fk_tournaments_created_by
        FOREIGN KEY (created_by) REFERENCES users(user_id)
        ON UPDATE CASCADE ON DELETE RESTRICT,
    CONSTRAINT chk_tournaments_date_range CHECK (end_date >= start_date),
    CONSTRAINT chk_tournaments_money_non_negative CHECK (
        registration_fee >= 0
        AND jockey_registration_fee >= 0
        AND system_contract_fee >= 0
        AND total_prize_pool >= 0
    ),
    CONSTRAINT chk_tournaments_horse_age_range CHECK (
        min_horse_age IS NULL
        OR max_horse_age IS NULL
        OR max_horse_age >= min_horse_age
    ),
    CONSTRAINT chk_tournaments_prediction_range CHECK (
        prediction_open_at IS NULL
        OR prediction_close_at IS NULL
        OR prediction_close_at > prediction_open_at
    )
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS tournament_eligibilities (
    eligibility_id CHAR(36) NOT NULL DEFAULT (UUID()),
    tournament_id CHAR(36) NOT NULL,
    target_type ENUM('HORSE', 'OWNER', 'JOCKEY') NOT NULL,
    condition_name VARCHAR(100) NOT NULL,
    condition_operator VARCHAR(10) NOT NULL,
    condition_value VARCHAR(100) NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    PRIMARY KEY (eligibility_id),
    KEY idx_tournament_eligibilities_tournament (tournament_id),
    KEY idx_tournament_eligibilities_target (target_type),
    CONSTRAINT fk_tournament_eligibilities_tournament
        FOREIGN KEY (tournament_id) REFERENCES tournaments(tournament_id)
        ON UPDATE CASCADE ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS prize_structures (
    prize_structure_id CHAR(36) NOT NULL DEFAULT (UUID()),
    tournament_id CHAR(36) NOT NULL,
    prize_rank INT NOT NULL,
    percentage DECIMAL(5,2) NULL,
    fixed_amount DECIMAL(15,2) NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    PRIMARY KEY (prize_structure_id),
    UNIQUE KEY uk_prize_structures_tournament_rank (tournament_id, prize_rank),
    CONSTRAINT fk_prize_structures_tournament
        FOREIGN KEY (tournament_id) REFERENCES tournaments(tournament_id)
        ON UPDATE CASCADE ON DELETE RESTRICT,
    CONSTRAINT chk_prize_structures_rank_positive CHECK (prize_rank > 0),
    CONSTRAINT chk_prize_structures_percentage_range CHECK (
        percentage IS NULL OR (percentage >= 0 AND percentage <= 100)
    ),
    CONSTRAINT chk_prize_structures_fixed_non_negative CHECK (
        fixed_amount IS NULL OR fixed_amount >= 0
    ),
    CONSTRAINT chk_prize_structures_percent_or_fixed CHECK (
        percentage IS NOT NULL OR fixed_amount IS NOT NULL
    )
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================================
-- 05. TOURNAMENT REGISTRATION + CONTRACT
-- ============================================================

CREATE TABLE IF NOT EXISTS tournament_registrations (
    tournament_reg_id CHAR(36) NOT NULL DEFAULT (UUID()),
    tournament_id CHAR(36) NOT NULL,
    horse_id CHAR(36) NOT NULL,
    owner_id CHAR(36) NOT NULL,
    status ENUM('PENDING_PAYMENT', 'PENDING_REVIEW', 'APPROVED', 'REJECTED', 'WITHDRAWN') NOT NULL DEFAULT 'PENDING_PAYMENT',
    submitted_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    reviewed_by CHAR(36) NULL,
    reviewed_at TIMESTAMP NULL,
    rejected_reason TEXT NULL,
    withdrawn_at TIMESTAMP NULL,
    withdraw_reason TEXT NULL,
    note TEXT NULL,
    PRIMARY KEY (tournament_reg_id),
    UNIQUE KEY uk_tournament_registrations_tournament_horse (tournament_id, horse_id),
    KEY idx_tournament_registrations_owner (owner_id),
    KEY idx_tournament_registrations_status (status),
    CONSTRAINT fk_tournament_registrations_tournament
        FOREIGN KEY (tournament_id) REFERENCES tournaments(tournament_id)
        ON UPDATE CASCADE ON DELETE RESTRICT,
    CONSTRAINT fk_tournament_registrations_horse
        FOREIGN KEY (horse_id) REFERENCES horses(horse_id)
        ON UPDATE CASCADE ON DELETE RESTRICT,
    CONSTRAINT fk_tournament_registrations_owner
        FOREIGN KEY (owner_id) REFERENCES horse_owners(owner_id)
        ON UPDATE CASCADE ON DELETE RESTRICT,
    CONSTRAINT fk_tournament_registrations_reviewed_by
        FOREIGN KEY (reviewed_by) REFERENCES users(user_id)
        ON UPDATE CASCADE ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS jockey_tournament_registrations (
    jockey_tournament_reg_id CHAR(36) NOT NULL DEFAULT (UUID()),
    tournament_id CHAR(36) NOT NULL,
    jockey_id CHAR(36) NOT NULL,
    status ENUM('PENDING_PAYMENT', 'PENDING_REVIEW', 'APPROVED', 'REJECTED', 'WITHDRAWN') NOT NULL DEFAULT 'PENDING_PAYMENT',
    submitted_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    reviewed_by CHAR(36) NULL,
    reviewed_at TIMESTAMP NULL,
    rejected_reason TEXT NULL,
    withdrawn_at TIMESTAMP NULL,
    note TEXT NULL,
    PRIMARY KEY (jockey_tournament_reg_id),
    UNIQUE KEY uk_jockey_tournament_registrations_tournament_jockey (tournament_id, jockey_id),
    KEY idx_jockey_tournament_registrations_jockey (jockey_id),
    KEY idx_jockey_tournament_registrations_status (status),
    CONSTRAINT fk_jockey_tournament_registrations_tournament
        FOREIGN KEY (tournament_id) REFERENCES tournaments(tournament_id)
        ON UPDATE CASCADE ON DELETE RESTRICT,
    CONSTRAINT fk_jockey_tournament_registrations_jockey
        FOREIGN KEY (jockey_id) REFERENCES jockeys(jockey_id)
        ON UPDATE CASCADE ON DELETE RESTRICT,
    CONSTRAINT fk_jockey_tournament_registrations_reviewed_by
        FOREIGN KEY (reviewed_by) REFERENCES users(user_id)
        ON UPDATE CASCADE ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS jockey_horse_contracts (
    contract_id CHAR(36) NOT NULL DEFAULT (UUID()),
    tournament_id CHAR(36) NOT NULL,
    tournament_reg_id CHAR(36) NOT NULL,
    jockey_tournament_reg_id CHAR(36) NOT NULL,
    owner_id CHAR(36) NOT NULL,
    horse_id CHAR(36) NOT NULL,
    jockey_id CHAR(36) NOT NULL,
    hire_fee DECIMAL(15,2) NOT NULL DEFAULT 0.00,
    advance_percent DECIMAL(5,2) NOT NULL DEFAULT 30.00,
    final_percent DECIMAL(5,2) NOT NULL DEFAULT 70.00,
    advance_paid_amount DECIMAL(15,2) NOT NULL DEFAULT 0.00,
    escrow_amount DECIMAL(15,2) NOT NULL DEFAULT 0.00,
    system_contract_fee DECIMAL(15,2) NOT NULL DEFAULT 0.00,
    owner_prize_share_percent DECIMAL(5,2) NOT NULL DEFAULT 80.00,
    jockey_prize_share_percent DECIMAL(5,2) NOT NULL DEFAULT 20.00,
    payment_status ENUM('UNPAID', 'PAID', 'REFUNDED') NOT NULL DEFAULT 'UNPAID',
    escrow_status ENUM('NOT_HELD', 'HELD', 'PARTIALLY_RELEASED', 'RELEASED', 'REFUNDED') NOT NULL DEFAULT 'NOT_HELD',
    advance_payout_status ENUM('NOT_PAID', 'PAID', 'CANCELLED') NOT NULL DEFAULT 'NOT_PAID',
    final_payout_status ENUM('NOT_RELEASED', 'RELEASED', 'CANCELLED') NOT NULL DEFAULT 'NOT_RELEASED',
    advance_payout_at TIMESTAMP NULL,
    final_payout_at TIMESTAMP NULL,
    status ENUM(
        'PENDING_JOCKEY',
        'ACCEPTED',
        'REJECTED',
        'HIRING_PAID',
        'PENDING_ADMIN_REVIEW',
        'APPROVED',
        'CANCELLED',
        'TERMINATED'
    ) NOT NULL DEFAULT 'PENDING_JOCKEY',
    requested_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    responded_at TIMESTAMP NULL,
    accepted_at TIMESTAMP NULL,
    submitted_at TIMESTAMP NULL,
    reviewed_by CHAR(36) NULL,
    reviewed_at TIMESTAMP NULL,
    rejected_reason TEXT NULL,
    cancelled_at TIMESTAMP NULL,
    cancel_reason TEXT NULL,
    terminated_at TIMESTAMP NULL,
    contract_note TEXT NULL,
    active_horse_contract_key VARCHAR(80) GENERATED ALWAYS AS (
        CASE
            WHEN status IN ('ACCEPTED', 'HIRING_PAID', 'PENDING_ADMIN_REVIEW', 'APPROVED')
            THEN CONCAT(tournament_id, ':', horse_id)
            ELSE NULL
        END
    ) STORED,
    active_jockey_contract_key VARCHAR(80) GENERATED ALWAYS AS (
        CASE
            WHEN status IN ('ACCEPTED', 'HIRING_PAID', 'PENDING_ADMIN_REVIEW', 'APPROVED')
            THEN CONCAT(tournament_id, ':', jockey_id)
            ELSE NULL
        END
    ) STORED,
    PRIMARY KEY (contract_id),
    UNIQUE KEY uk_contracts_active_horse_in_tournament (active_horse_contract_key),
    UNIQUE KEY uk_contracts_active_jockey_in_tournament (active_jockey_contract_key),
    KEY idx_contracts_tournament (tournament_id),
    KEY idx_contracts_tournament_reg (tournament_reg_id),
    KEY idx_contracts_jockey_tournament_reg (jockey_tournament_reg_id),
    KEY idx_contracts_owner (owner_id),
    KEY idx_contracts_horse (horse_id),
    KEY idx_contracts_jockey (jockey_id),
    KEY idx_contracts_status (status),
    CONSTRAINT fk_contracts_tournament
        FOREIGN KEY (tournament_id) REFERENCES tournaments(tournament_id)
        ON UPDATE CASCADE ON DELETE RESTRICT,
    CONSTRAINT fk_contracts_tournament_reg
        FOREIGN KEY (tournament_reg_id) REFERENCES tournament_registrations(tournament_reg_id)
        ON UPDATE CASCADE ON DELETE RESTRICT,
    CONSTRAINT fk_contracts_jockey_tournament_reg
        FOREIGN KEY (jockey_tournament_reg_id) REFERENCES jockey_tournament_registrations(jockey_tournament_reg_id)
        ON UPDATE CASCADE ON DELETE RESTRICT,
    CONSTRAINT fk_contracts_owner
        FOREIGN KEY (owner_id) REFERENCES horse_owners(owner_id)
        ON UPDATE CASCADE ON DELETE RESTRICT,
    CONSTRAINT fk_contracts_horse
        FOREIGN KEY (horse_id) REFERENCES horses(horse_id)
        ON UPDATE CASCADE ON DELETE RESTRICT,
    CONSTRAINT fk_contracts_jockey
        FOREIGN KEY (jockey_id) REFERENCES jockeys(jockey_id)
        ON UPDATE CASCADE ON DELETE RESTRICT,
    CONSTRAINT fk_contracts_reviewed_by
        FOREIGN KEY (reviewed_by) REFERENCES users(user_id)
        ON UPDATE CASCADE ON DELETE SET NULL,
    CONSTRAINT chk_contracts_money_non_negative CHECK (
        hire_fee >= 0
        AND advance_paid_amount >= 0
        AND escrow_amount >= 0
        AND system_contract_fee >= 0
    ),
    CONSTRAINT chk_contracts_hire_split_percent CHECK (advance_percent + final_percent = 100.00),
    CONSTRAINT chk_contracts_prize_split_percent CHECK (owner_prize_share_percent + jockey_prize_share_percent = 100.00),
    CONSTRAINT chk_contracts_percent_range CHECK (
        advance_percent >= 0 AND advance_percent <= 100
        AND final_percent >= 0 AND final_percent <= 100
        AND owner_prize_share_percent >= 0 AND owner_prize_share_percent <= 100
        AND jockey_prize_share_percent >= 0 AND jockey_prize_share_percent <= 100
    )
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================================
-- 06. INVOICE
-- ============================================================

CREATE TABLE IF NOT EXISTS invoices (
    invoice_id CHAR(36) NOT NULL DEFAULT (UUID()),
    payer_user_id CHAR(36) NOT NULL,
    tournament_reg_id CHAR(36) NULL,
    jockey_tournament_reg_id CHAR(36) NULL,
    contract_id CHAR(36) NULL,
    invoice_type ENUM(
        'OWNER_TOURNAMENT_REGISTRATION_FEE',
        'JOCKEY_TOURNAMENT_REGISTRATION_FEE',
        'JOCKEY_HIRING_FEE',
        'CONTRACT_CREATION_FEE'
    ) NOT NULL,
    amount DECIMAL(15,2) NOT NULL,
    status ENUM('UNPAID', 'PAID', 'REFUNDED', 'CANCELLED') NOT NULL DEFAULT 'UNPAID',
    due_date DATETIME NULL,
    paid_at TIMESTAMP NULL,
    refunded_at TIMESTAMP NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    note TEXT NULL,
    PRIMARY KEY (invoice_id),
    KEY idx_invoices_payer (payer_user_id),
    KEY idx_invoices_tournament_reg (tournament_reg_id),
    KEY idx_invoices_jockey_tournament_reg (jockey_tournament_reg_id),
    KEY idx_invoices_contract (contract_id),
    KEY idx_invoices_status (status),
    CONSTRAINT fk_invoices_payer
        FOREIGN KEY (payer_user_id) REFERENCES users(user_id)
        ON UPDATE CASCADE ON DELETE RESTRICT,
    CONSTRAINT fk_invoices_tournament_reg
        FOREIGN KEY (tournament_reg_id) REFERENCES tournament_registrations(tournament_reg_id)
        ON UPDATE CASCADE ON DELETE RESTRICT,
    CONSTRAINT fk_invoices_jockey_tournament_reg
        FOREIGN KEY (jockey_tournament_reg_id) REFERENCES jockey_tournament_registrations(jockey_tournament_reg_id)
        ON UPDATE CASCADE ON DELETE RESTRICT,
    CONSTRAINT fk_invoices_contract
        FOREIGN KEY (contract_id) REFERENCES jockey_horse_contracts(contract_id)
        ON UPDATE CASCADE ON DELETE RESTRICT,
    CONSTRAINT chk_invoices_amount_non_negative CHECK (amount >= 0),
    CONSTRAINT chk_invoices_has_business_ref CHECK (
        tournament_reg_id IS NOT NULL
        OR jockey_tournament_reg_id IS NOT NULL
        OR contract_id IS NOT NULL
    )
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================================
-- 07. ROUND + RACE + RACE ENTRY
-- ============================================================

CREATE TABLE IF NOT EXISTS rounds (
    round_id CHAR(36) NOT NULL DEFAULT (UUID()),
    tournament_id CHAR(36) NOT NULL,
    created_by CHAR(36) NOT NULL,
    round_name VARCHAR(100) NOT NULL,
    sequence_order INT NOT NULL,
    is_final BOOLEAN NOT NULL DEFAULT FALSE,
    prediction_type ENUM('TOP1', 'TOP3') NOT NULL DEFAULT 'TOP1',
    advancement_rule TEXT NULL,
    status ENUM('SCHEDULED', 'ONGOING', 'FINISHED') NOT NULL DEFAULT 'SCHEDULED',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (round_id),
    UNIQUE KEY uk_rounds_tournament_sequence (tournament_id, sequence_order),
    KEY idx_rounds_created_by (created_by),
    KEY idx_rounds_status (status),
    CONSTRAINT fk_rounds_tournament
        FOREIGN KEY (tournament_id) REFERENCES tournaments(tournament_id)
        ON UPDATE CASCADE ON DELETE RESTRICT,
    CONSTRAINT fk_rounds_created_by
        FOREIGN KEY (created_by) REFERENCES users(user_id)
        ON UPDATE CASCADE ON DELETE RESTRICT,
    CONSTRAINT chk_rounds_sequence_positive CHECK (sequence_order > 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS races (
    race_id CHAR(36) NOT NULL DEFAULT (UUID()),
    round_id CHAR(36) NOT NULL,
    created_by CHAR(36) NOT NULL,
    name VARCHAR(150) NOT NULL,
    start_time DATETIME NOT NULL,
    end_time DATETIME NULL,
    track_condition VARCHAR(100) NULL,
    distance DECIMAL(8,2) NOT NULL,
    max_entries INT NOT NULL,
    status ENUM('SCHEDULED', 'RUNNING', 'FINISHED_PENDING', 'OFFICIALLY_FINISHED', 'CANCELLED') NOT NULL DEFAULT 'SCHEDULED',
    started_by CHAR(36) NULL,
    started_at DATETIME NULL,
    finished_at DATETIME NULL,
    schedule_published_at DATETIME NULL,
    PRIMARY KEY (race_id),
    KEY idx_races_round (round_id),
    KEY idx_races_created_by (created_by),
    KEY idx_races_started_by (started_by),
    KEY idx_races_status_start_time (status, start_time),
    CONSTRAINT fk_races_round
        FOREIGN KEY (round_id) REFERENCES rounds(round_id)
        ON UPDATE CASCADE ON DELETE RESTRICT,
    CONSTRAINT fk_races_created_by
        FOREIGN KEY (created_by) REFERENCES users(user_id)
        ON UPDATE CASCADE ON DELETE RESTRICT,
    CONSTRAINT fk_races_started_by
        FOREIGN KEY (started_by) REFERENCES users(user_id)
        ON UPDATE CASCADE ON DELETE SET NULL,
    CONSTRAINT chk_races_distance_positive CHECK (distance > 0),
    CONSTRAINT chk_races_max_entries_positive CHECK (max_entries > 0),
    CONSTRAINT chk_races_time_range CHECK (end_time IS NULL OR end_time >= start_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS race_referees (
    race_id CHAR(36) NOT NULL,
    referee_id CHAR(36) NOT NULL,
    referee_role ENUM('MAIN', 'ASSISTANT') NOT NULL,
    assigned_by CHAR(36) NOT NULL,
    assigned_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (race_id, referee_id),
    KEY idx_race_referees_referee (referee_id),
    KEY idx_race_referees_assigned_by (assigned_by),
    CONSTRAINT fk_race_referees_race
        FOREIGN KEY (race_id) REFERENCES races(race_id)
        ON UPDATE CASCADE ON DELETE RESTRICT,
    CONSTRAINT fk_race_referees_referee
        FOREIGN KEY (referee_id) REFERENCES referees(referee_id)
        ON UPDATE CASCADE ON DELETE RESTRICT,
    CONSTRAINT fk_race_referees_assigned_by
        FOREIGN KEY (assigned_by) REFERENCES users(user_id)
        ON UPDATE CASCADE ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS race_entries (
    entry_id CHAR(36) NOT NULL DEFAULT (UUID()),
    race_id CHAR(36) NOT NULL,
    contract_id CHAR(36) NOT NULL,
    lane_number INT NOT NULL,
    starting_position INT NULL,
    status ENUM(
        'CONFIRMED',
        'SCRATCHED',
        'DISQUALIFIED',
        'FINISHED',
        'WITHDRAWN_BEFORE_SCHEDULE',
        'WITHDRAWN_AFTER_SCHEDULE'
    ) NOT NULL DEFAULT 'CONFIRMED',
    assigned_by CHAR(36) NOT NULL,
    assigned_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    withdrawn_at TIMESTAMP NULL,
    withdraw_reason TEXT NULL,
    scratched_reason TEXT NULL,
    disqualified_at TIMESTAMP NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (entry_id),
    UNIQUE KEY uk_race_entries_race_contract (race_id, contract_id),
    UNIQUE KEY uk_race_entries_race_lane (race_id, lane_number),
    KEY idx_race_entries_contract (contract_id),
    KEY idx_race_entries_assigned_by (assigned_by),
    KEY idx_race_entries_status (status),
    CONSTRAINT fk_race_entries_race
        FOREIGN KEY (race_id) REFERENCES races(race_id)
        ON UPDATE CASCADE ON DELETE RESTRICT,
    CONSTRAINT fk_race_entries_contract
        FOREIGN KEY (contract_id) REFERENCES jockey_horse_contracts(contract_id)
        ON UPDATE CASCADE ON DELETE RESTRICT,
    CONSTRAINT fk_race_entries_assigned_by
        FOREIGN KEY (assigned_by) REFERENCES users(user_id)
        ON UPDATE CASCADE ON DELETE RESTRICT,
    CONSTRAINT chk_race_entries_lane_positive CHECK (lane_number > 0),
    CONSTRAINT chk_race_entries_starting_position_positive CHECK (starting_position IS NULL OR starting_position > 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================================
-- 08. INSPECTION + VIOLATION + RESULT + REPORT
-- ============================================================

CREATE TABLE IF NOT EXISTS horse_inspections (
    horse_inspection_id CHAR(36) NOT NULL DEFAULT (UUID()),
    entry_id CHAR(36) NOT NULL,
    vet_id CHAR(36) NOT NULL,
    result ENUM('PASS', 'FAIL') NULL,
    note TEXT NULL,
    inspected_at TIMESTAMP NULL,
    handicap_weight DECIMAL(7,2) NULL,
    is_handicap_confirmed BOOLEAN NOT NULL DEFAULT FALSE,
    confirmed_at TIMESTAMP NULL,
    status ENUM('DRAFT', 'SUBMITTED', 'CONFIRMED') NOT NULL DEFAULT 'DRAFT',
    PRIMARY KEY (horse_inspection_id),
    UNIQUE KEY uk_horse_inspections_entry (entry_id),
    KEY idx_horse_inspections_vet (vet_id),
    KEY idx_horse_inspections_status (status),
    CONSTRAINT fk_horse_inspections_entry
        FOREIGN KEY (entry_id) REFERENCES race_entries(entry_id)
        ON UPDATE CASCADE ON DELETE RESTRICT,
    CONSTRAINT fk_horse_inspections_vet
        FOREIGN KEY (vet_id) REFERENCES veterinarians(vet_id)
        ON UPDATE CASCADE ON DELETE RESTRICT,
    CONSTRAINT chk_horse_inspections_handicap_non_negative CHECK (
        handicap_weight IS NULL OR handicap_weight >= 0
    )
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS jockey_inspections (
    jockey_inspection_id CHAR(36) NOT NULL DEFAULT (UUID()),
    entry_id CHAR(36) NOT NULL,
    med_staff_id CHAR(36) NOT NULL,
    result ENUM('PASS', 'FAIL') NULL,
    note TEXT NULL,
    inspected_at TIMESTAMP NULL,
    status ENUM('DRAFT', 'SUBMITTED', 'CONFIRMED') NOT NULL DEFAULT 'DRAFT',
    PRIMARY KEY (jockey_inspection_id),
    UNIQUE KEY uk_jockey_inspections_entry (entry_id),
    KEY idx_jockey_inspections_med_staff (med_staff_id),
    KEY idx_jockey_inspections_status (status),
    CONSTRAINT fk_jockey_inspections_entry
        FOREIGN KEY (entry_id) REFERENCES race_entries(entry_id)
        ON UPDATE CASCADE ON DELETE RESTRICT,
    CONSTRAINT fk_jockey_inspections_med_staff
        FOREIGN KEY (med_staff_id) REFERENCES medical_staff(med_staff_id)
        ON UPDATE CASCADE ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS violations (
    violation_id CHAR(36) NOT NULL DEFAULT (UUID()),
    entry_id CHAR(36) NOT NULL,
    referee_id CHAR(36) NOT NULL,
    violation_type ENUM('FALSE_START', 'OBSTRUCTION', 'WRONG_LANE', 'EQUIPMENT', 'DOPING', 'OTHER') NOT NULL,
    description TEXT NULL,
    penalty_type ENUM('WARNING', 'TIME_PENALTY', 'DISQUALIFIED') NOT NULL,
    penalty_value DECIMAL(10,3) NULL,
    occurred_at TIMESTAMP NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    status ENUM('ACTIVE', 'RESOLVED', 'CANCELLED') NOT NULL DEFAULT 'ACTIVE',
    PRIMARY KEY (violation_id),
    KEY idx_violations_entry (entry_id),
    KEY idx_violations_referee (referee_id),
    KEY idx_violations_type_status (violation_type, status),
    CONSTRAINT fk_violations_entry
        FOREIGN KEY (entry_id) REFERENCES race_entries(entry_id)
        ON UPDATE CASCADE ON DELETE RESTRICT,
    CONSTRAINT fk_violations_referee
        FOREIGN KEY (referee_id) REFERENCES referees(referee_id)
        ON UPDATE CASCADE ON DELETE RESTRICT,
    CONSTRAINT chk_violations_penalty_value_non_negative CHECK (
        penalty_value IS NULL OR penalty_value >= 0
    )
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS race_results (
    result_id CHAR(36) NOT NULL DEFAULT (UUID()),
    race_id CHAR(36) NOT NULL,
    entry_id CHAR(36) NOT NULL,
    finish_time DECIMAL(10,3) NULL,
    result_rank INT NULL,
    prize_money DECIMAL(15,2) NOT NULL DEFAULT 0.00,
    owner_prize_amount DECIMAL(15,2) NOT NULL DEFAULT 0.00,
    jockey_prize_amount DECIMAL(15,2) NOT NULL DEFAULT 0.00,
    prize_status ENUM('NOT_ELIGIBLE', 'PENDING_PAYOUT', 'PAID') NOT NULL DEFAULT 'NOT_ELIGIBLE',
    is_prize_paid BOOLEAN NOT NULL DEFAULT FALSE,
    prize_paid_at TIMESTAMP NULL,
    status ENUM('FINISHED', 'DISQUALIFIED') NOT NULL,
    recorded_by CHAR(36) NOT NULL,
    recorded_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (result_id),
    UNIQUE KEY uk_race_results_entry (entry_id),
    UNIQUE KEY uk_race_results_race_rank (race_id, result_rank),
    KEY idx_race_results_race (race_id),
    KEY idx_race_results_recorded_by (recorded_by),
    KEY idx_race_results_prize_status (prize_status),
    CONSTRAINT fk_race_results_race
        FOREIGN KEY (race_id) REFERENCES races(race_id)
        ON UPDATE CASCADE ON DELETE RESTRICT,
    CONSTRAINT fk_race_results_entry
        FOREIGN KEY (entry_id) REFERENCES race_entries(entry_id)
        ON UPDATE CASCADE ON DELETE RESTRICT,
    CONSTRAINT fk_race_results_recorded_by
        FOREIGN KEY (recorded_by) REFERENCES users(user_id)
        ON UPDATE CASCADE ON DELETE RESTRICT,
    CONSTRAINT chk_race_results_finish_time_positive CHECK (finish_time IS NULL OR finish_time > 0),
    CONSTRAINT chk_race_results_rank_positive CHECK (result_rank IS NULL OR result_rank > 0),
    CONSTRAINT chk_race_results_money_non_negative CHECK (
        prize_money >= 0 AND owner_prize_amount >= 0 AND jockey_prize_amount >= 0
    )
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS race_reports (
    report_id CHAR(36) NOT NULL DEFAULT (UUID()),
    race_id CHAR(36) NOT NULL,
    referee_id CHAR(36) NOT NULL,
    summary TEXT NULL,
    appeal_note TEXT NULL,
    status ENUM('DRAFT', 'SIGNED', 'PUBLISHED') NOT NULL DEFAULT 'DRAFT',
    signed_by CHAR(36) NULL,
    signed_at TIMESTAMP NULL,
    published_by CHAR(36) NULL,
    published_at TIMESTAMP NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (report_id),
    UNIQUE KEY uk_race_reports_race (race_id),
    KEY idx_race_reports_referee (referee_id),
    KEY idx_race_reports_signed_by (signed_by),
    KEY idx_race_reports_published_by (published_by),
    KEY idx_race_reports_status (status),
    CONSTRAINT fk_race_reports_race
        FOREIGN KEY (race_id) REFERENCES races(race_id)
        ON UPDATE CASCADE ON DELETE RESTRICT,
    CONSTRAINT fk_race_reports_referee
        FOREIGN KEY (referee_id) REFERENCES referees(referee_id)
        ON UPDATE CASCADE ON DELETE RESTRICT,
    CONSTRAINT fk_race_reports_signed_by
        FOREIGN KEY (signed_by) REFERENCES referees(referee_id)
        ON UPDATE CASCADE ON DELETE SET NULL,
    CONSTRAINT fk_race_reports_published_by
        FOREIGN KEY (published_by) REFERENCES users(user_id)
        ON UPDATE CASCADE ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================================
-- 09. APPEAL
-- ============================================================

CREATE TABLE IF NOT EXISTS appeal_categories (
    category_id CHAR(36) NOT NULL DEFAULT (UUID()),
    code VARCHAR(50) NOT NULL,
    name VARCHAR(100) NOT NULL,
    description TEXT NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (category_id),
    UNIQUE KEY uk_appeal_categories_code (code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS appeals (
    appeal_id CHAR(36) NOT NULL DEFAULT (UUID()),
    entry_id CHAR(36) NOT NULL,
    race_result_id CHAR(36) NULL,
    related_violation_id CHAR(36) NULL,
    category_id CHAR(36) NOT NULL,
    submitted_by_user_id CHAR(36) NOT NULL,
    description TEXT NOT NULL,
    status ENUM('PENDING', 'UNDER_REVIEW', 'ACCEPTED', 'REJECTED', 'CANCELLED') NOT NULL DEFAULT 'PENDING',
    submitted_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    reviewed_by_referee_id CHAR(36) NULL,
    reviewed_at TIMESTAMP NULL,
    resolution TEXT NULL,
    PRIMARY KEY (appeal_id),
    KEY idx_appeals_entry (entry_id),
    KEY idx_appeals_race_result (race_result_id),
    KEY idx_appeals_violation (related_violation_id),
    KEY idx_appeals_category (category_id),
    KEY idx_appeals_submitted_by (submitted_by_user_id),
    KEY idx_appeals_status (status),
    CONSTRAINT fk_appeals_entry
        FOREIGN KEY (entry_id) REFERENCES race_entries(entry_id)
        ON UPDATE CASCADE ON DELETE RESTRICT,
    CONSTRAINT fk_appeals_race_result
        FOREIGN KEY (race_result_id) REFERENCES race_results(result_id)
        ON UPDATE CASCADE ON DELETE SET NULL,
    CONSTRAINT fk_appeals_violation
        FOREIGN KEY (related_violation_id) REFERENCES violations(violation_id)
        ON UPDATE CASCADE ON DELETE SET NULL,
    CONSTRAINT fk_appeals_category
        FOREIGN KEY (category_id) REFERENCES appeal_categories(category_id)
        ON UPDATE CASCADE ON DELETE RESTRICT,
    CONSTRAINT fk_appeals_submitted_by
        FOREIGN KEY (submitted_by_user_id) REFERENCES users(user_id)
        ON UPDATE CASCADE ON DELETE RESTRICT,
    CONSTRAINT fk_appeals_reviewed_by_referee
        FOREIGN KEY (reviewed_by_referee_id) REFERENCES referees(referee_id)
        ON UPDATE CASCADE ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS appeal_evidences (
    evidence_id CHAR(36) NOT NULL DEFAULT (UUID()),
    appeal_id CHAR(36) NOT NULL,
    evidence_type ENUM('IMAGE', 'VIDEO', 'TEXT', 'DOCUMENT') NOT NULL,
    file_url TEXT NULL,
    text_content TEXT NULL,
    description TEXT NULL,
    uploaded_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (evidence_id),
    KEY idx_appeal_evidences_appeal (appeal_id),
    CONSTRAINT fk_appeal_evidences_appeal
        FOREIGN KEY (appeal_id) REFERENCES appeals(appeal_id)
        ON UPDATE CASCADE ON DELETE RESTRICT,
    CONSTRAINT chk_appeal_evidences_content CHECK (
        file_url IS NOT NULL OR text_content IS NOT NULL
    )
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================================
-- 10. PREDICTION + AI PREDICTION
-- ============================================================

CREATE TABLE IF NOT EXISTS predictions (
    prediction_id CHAR(36) NOT NULL DEFAULT (UUID()),
    spectator_id CHAR(36) NOT NULL,
    race_id CHAR(36) NOT NULL,
    prediction_type ENUM('TOP1', 'TOP3') NOT NULL,
    prediction_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    status ENUM('PENDING', 'CORRECT', 'INCORRECT', 'PARTIALLY_CORRECT', 'CANCELLED') NOT NULL DEFAULT 'PENDING',
    reward_points INT NOT NULL DEFAULT 0,
    scored_at TIMESTAMP NULL,
    PRIMARY KEY (prediction_id),
    UNIQUE KEY uk_predictions_spectator_race (spectator_id, race_id),
    KEY idx_predictions_race (race_id),
    KEY idx_predictions_status (status),
    CONSTRAINT fk_predictions_spectator
        FOREIGN KEY (spectator_id) REFERENCES spectators(spectator_id)
        ON UPDATE CASCADE ON DELETE RESTRICT,
    CONSTRAINT fk_predictions_race
        FOREIGN KEY (race_id) REFERENCES races(race_id)
        ON UPDATE CASCADE ON DELETE RESTRICT,
    CONSTRAINT chk_predictions_reward_points_non_negative CHECK (reward_points >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS prediction_details (
    prediction_detail_id CHAR(36) NOT NULL DEFAULT (UUID()),
    prediction_id CHAR(36) NOT NULL,
    entry_id CHAR(36) NOT NULL,
    predicted_rank INT NOT NULL,
    is_correct BOOLEAN NULL,
    awarded_points INT NOT NULL DEFAULT 0,
    PRIMARY KEY (prediction_detail_id),
    UNIQUE KEY uk_prediction_details_prediction_rank (prediction_id, predicted_rank),
    UNIQUE KEY uk_prediction_details_prediction_entry (prediction_id, entry_id),
    KEY idx_prediction_details_entry (entry_id),
    CONSTRAINT fk_prediction_details_prediction
        FOREIGN KEY (prediction_id) REFERENCES predictions(prediction_id)
        ON UPDATE CASCADE ON DELETE RESTRICT,
    CONSTRAINT fk_prediction_details_entry
        FOREIGN KEY (entry_id) REFERENCES race_entries(entry_id)
        ON UPDATE CASCADE ON DELETE RESTRICT,
    CONSTRAINT chk_prediction_details_rank_positive CHECK (predicted_rank > 0),
    CONSTRAINT chk_prediction_details_points_non_negative CHECK (awarded_points >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS ai_predictions (
    ai_prediction_id CHAR(36) NOT NULL DEFAULT (UUID()),
    entry_id CHAR(36) NOT NULL,
    win_rate_horse DECIMAL(5,2) NOT NULL DEFAULT 0.00,
    win_rate_jockey DECIMAL(5,2) NOT NULL DEFAULT 0.00,
    experience_score DECIMAL(8,2) NOT NULL DEFAULT 0.00,
    score DECIMAL(10,2) NOT NULL DEFAULT 0.00,
    win_probability DECIMAL(5,2) NOT NULL DEFAULT 0.00,
    algorithm_type ENUM('WEIGHTED_SCORE', 'ML_MODEL', 'LLM_EXPLANATION') NOT NULL DEFAULT 'WEIGHTED_SCORE',
    model_version VARCHAR(50) NULL,
    explanation TEXT NULL,
    generated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (ai_prediction_id),
    KEY idx_ai_predictions_entry (entry_id),
    KEY idx_ai_predictions_probability (win_probability),
    CONSTRAINT fk_ai_predictions_entry
        FOREIGN KEY (entry_id) REFERENCES race_entries(entry_id)
        ON UPDATE CASCADE ON DELETE RESTRICT,
    CONSTRAINT chk_ai_predictions_percent_range CHECK (
        win_rate_horse >= 0 AND win_rate_horse <= 100
        AND win_rate_jockey >= 0 AND win_rate_jockey <= 100
        AND win_probability >= 0 AND win_probability <= 100
    )
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================================
-- 11. TRANSACTION
-- Place after invoice/result/contract because it references all of them.
-- ============================================================

CREATE TABLE IF NOT EXISTS transactions (
    transaction_id CHAR(36) NOT NULL DEFAULT (UUID()),
    wallet_id CHAR(36) NOT NULL,
    invoice_id CHAR(36) NULL,
    race_result_id CHAR(36) NULL,
    contract_id CHAR(36) NULL,
    transaction_type ENUM(
        'DEPOSIT',
        'OWNER_REGISTRATION_FEE',
        'JOCKEY_REGISTRATION_FEE',
        'JOCKEY_HIRING_FEE',
        'JOCKEY_HIRING_ESCROW',
        'JOCKEY_HIRING_ADVANCE_PAYOUT',
        'JOCKEY_HIRING_ADVANCE_INCOME',
        'JOCKEY_HIRING_FINAL_PAYOUT',
        'JOCKEY_HIRING_FINAL_INCOME',
        'CONTRACT_CREATION_FEE',
        'PRIZE_OWNER_SHARE',
        'PRIZE_JOCKEY_SHARE',
        'REFUND'
    ) NOT NULL,
    direction ENUM('DEBIT', 'CREDIT') NOT NULL,
    amount DECIMAL(15,2) NOT NULL,
    balance_before DECIMAL(15,2) NOT NULL,
    balance_after DECIMAL(15,2) NOT NULL,
    counterparty_wallet_id CHAR(36) NULL,
    counterparty_type ENUM('USER', 'SYSTEM', 'EXTERNAL') NOT NULL,
    transaction_group_id CHAR(36) NOT NULL,
    status ENUM('PENDING', 'SUCCESS', 'FAILED', 'REVERSED') NOT NULL DEFAULT 'PENDING',
    note TEXT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (transaction_id),
    KEY idx_transactions_wallet (wallet_id),
    KEY idx_transactions_invoice (invoice_id),
    KEY idx_transactions_race_result (race_result_id),
    KEY idx_transactions_contract (contract_id),
    KEY idx_transactions_counterparty_wallet (counterparty_wallet_id),
    KEY idx_transactions_group (transaction_group_id),
    KEY idx_transactions_type_status (transaction_type, status),
    KEY idx_transactions_created_at (created_at),
    CONSTRAINT fk_transactions_wallet
        FOREIGN KEY (wallet_id) REFERENCES wallets(wallet_id)
        ON UPDATE CASCADE ON DELETE RESTRICT,
    CONSTRAINT fk_transactions_invoice
        FOREIGN KEY (invoice_id) REFERENCES invoices(invoice_id)
        ON UPDATE CASCADE ON DELETE SET NULL,
    CONSTRAINT fk_transactions_race_result
        FOREIGN KEY (race_result_id) REFERENCES race_results(result_id)
        ON UPDATE CASCADE ON DELETE SET NULL,
    CONSTRAINT fk_transactions_contract
        FOREIGN KEY (contract_id) REFERENCES jockey_horse_contracts(contract_id)
        ON UPDATE CASCADE ON DELETE SET NULL,
    CONSTRAINT fk_transactions_counterparty_wallet
        FOREIGN KEY (counterparty_wallet_id) REFERENCES wallets(wallet_id)
        ON UPDATE CASCADE ON DELETE SET NULL,
    CONSTRAINT chk_transactions_amount_positive CHECK (amount > 0),
    CONSTRAINT chk_transactions_balance_non_negative CHECK (balance_before >= 0 AND balance_after >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================================
-- 12. NOTIFICATION
-- ============================================================

CREATE TABLE IF NOT EXISTS notifications (
    noti_id CHAR(36) NOT NULL DEFAULT (UUID()),
    user_id CHAR(36) NOT NULL,
    title VARCHAR(200) NOT NULL,
    content TEXT NOT NULL,
    notification_type ENUM(
        'TOURNAMENT_PUBLISHED',
        'REGISTRATION_APPROVED',
        'REGISTRATION_REJECTED',
        'JOCKEY_INVITATION',
        'INVOICE_CREATED',
        'PAYMENT_SUCCESS',
        'CONTRACT_APPROVED',
        'RACE_SCHEDULED',
        'RACE_STARTED',
        'VIOLATION_CREATED',
        'APPEAL_REVIEWED',
        'RESULT_PUBLISHED',
        'PRIZE_RECEIVED',
        'PREDICTION_SCORED',
        'JOCKEY_PAYOUT_RELEASED'
    ) NOT NULL,
    related_type VARCHAR(50) NULL,
    related_id CHAR(36) NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    is_read BOOLEAN NOT NULL DEFAULT FALSE,
    read_at TIMESTAMP NULL,
    PRIMARY KEY (noti_id),
    KEY idx_notifications_user_read (user_id, is_read),
    KEY idx_notifications_type (notification_type),
    KEY idx_notifications_related (related_type, related_id),
    CONSTRAINT fk_notifications_user
        FOREIGN KEY (user_id) REFERENCES users(user_id)
        ON UPDATE CASCADE ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================================
-- 13. SEED DATA
-- ============================================================

INSERT INTO roles (role_name, description, is_active)
VALUES
    ('ADMIN', 'System administrator', TRUE),
    ('HORSE_OWNER', 'Horse owner', TRUE),
    ('JOCKEY', 'Jockey', TRUE),
    ('REFEREE', 'Race referee', TRUE),
    ('SPECTATOR', 'Spectator prediction user', TRUE),
    ('VETERINARIAN', 'Veterinarian for horse inspection', TRUE),
    ('MEDICAL_STAFF', 'Medical staff for jockey inspection', TRUE)
ON DUPLICATE KEY UPDATE
    description = VALUES(description),
    is_active = VALUES(is_active);

-- Create default SystemWallet.
-- UUID is fixed here so the seed is idempotent.
INSERT INTO wallets (wallet_id, owner_type, user_id, balance, currency, status)
VALUES ('00000000-0000-0000-0000-000000000001', 'SYSTEM', NULL, 0.00, 'VND', 'ACTIVE')
ON DUPLICATE KEY UPDATE
    owner_type = VALUES(owner_type),
    user_id = VALUES(user_id),
    currency = VALUES(currency),
    status = VALUES(status);

INSERT INTO appeal_categories (code, name, description, is_active)
VALUES
    ('WRONG_RESULT', 'Wrong result', 'Appeal related to incorrect race result or rank', TRUE),
    ('WRONG_VIOLATION', 'Wrong violation', 'Appeal related to a violation decision', TRUE),
    ('UNFAIR_START', 'Unfair start', 'Appeal related to unfair or invalid race start', TRUE),
    ('OTHER', 'Other', 'Other appeal reason', TRUE)
ON DUPLICATE KEY UPDATE
    name = VALUES(name),
    description = VALUES(description),
    is_active = VALUES(is_active);

SET FOREIGN_KEY_CHECKS = 1;

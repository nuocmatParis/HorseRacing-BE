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

DROP TABLE IF EXISTS notifications;
DROP TABLE IF EXISTS ai_predictions;
DROP TABLE IF EXISTS prediction_details;
DROP TABLE IF EXISTS predictions;
DROP TABLE IF EXISTS appeal_evidences;
DROP TABLE IF EXISTS appeals;
DROP TABLE IF EXISTS appeal_categories;
DROP TABLE IF EXISTS wallet_transactions;
DROP TABLE IF EXISTS race_reports;
DROP TABLE IF EXISTS race_results;
DROP TABLE IF EXISTS violations;
DROP TABLE IF EXISTS jockey_inspections;
DROP TABLE IF EXISTS horse_inspections;
DROP TABLE IF EXISTS race_entries;
DROP TABLE IF EXISTS race_referees;
DROP TABLE IF EXISTS races;
DROP TABLE IF EXISTS rounds;
DROP TABLE IF EXISTS invoices;
DROP TABLE IF EXISTS jockey_horse_contracts;
DROP TABLE IF EXISTS jockey_tournament_registrations;
DROP TABLE IF EXISTS tournament_registrations;
DROP TABLE IF EXISTS prize_structures;
DROP TABLE IF EXISTS tournament_eligibilities;
DROP TABLE IF EXISTS tournaments;
DROP TABLE IF EXISTS wallets;
DROP TABLE IF EXISTS horses;
DROP TABLE IF EXISTS medical_staffs;
DROP TABLE IF EXISTS veterinarians;
DROP TABLE IF EXISTS spectators;
DROP TABLE IF EXISTS referees;
DROP TABLE IF EXISTS jockeys;
DROP TABLE IF EXISTS horse_owners;
DROP TABLE IF EXISTS users;
DROP TABLE IF EXISTS roles;
DROP TABLE IF EXISTS horse_tournament_registrations;

SET FOREIGN_KEY_CHECKS = 1;

-- =========================
-- 1. ROLE
-- =========================
CREATE TABLE roles (
                       role_id CHAR(36) PRIMARY KEY,
                       role_name VARCHAR(50) NOT NULL UNIQUE,
                       description TEXT,
                       is_active BOOLEAN NOT NULL DEFAULT TRUE,
                       created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- =========================
-- 2. USER
-- =========================
CREATE TABLE users (
                       user_id CHAR(36) PRIMARY KEY,
                       role_id CHAR(36) NOT NULL,

                       username VARCHAR(15) NOT NULL UNIQUE,
                       password VARCHAR(255) NOT NULL,
                       email VARCHAR(100) NOT NULL UNIQUE,
                       dob DATE,
                       gender ENUM('MALE', 'FEMALE') NOT NULL,
                       full_name VARCHAR(100) NOT NULL,
                       phone_number VARCHAR(20) NOT NULL UNIQUE,
                       status ENUM('ACTIVE', 'INACTIVE', 'BANNED') NOT NULL DEFAULT 'ACTIVE',

                       created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                       last_login_at TIMESTAMP NULL,

                       CONSTRAINT fk_users_role
                           FOREIGN KEY (role_id) REFERENCES roles(role_id)
);

-- =========================
-- 3. HORSE OWNER
-- =========================
CREATE TABLE horse_owners (
                             owner_id CHAR(36) PRIMARY KEY,
                             user_id CHAR(36) NOT NULL UNIQUE,

                             farm_name VARCHAR(100),
                             address TEXT,
                             license_number VARCHAR(50) UNIQUE,
                             created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

                             CONSTRAINT fk_horse_owner_user
                                 FOREIGN KEY (user_id) REFERENCES users(user_id)
);

-- =========================
-- 4. JOCKEY
-- =========================
CREATE TABLE jockeys (
                        jockey_id CHAR(36) PRIMARY KEY,
                        user_id CHAR(36) NOT NULL UNIQUE,

                        height FLOAT,
                        weight FLOAT,
                        experience_years INT DEFAULT 0,
                        license_number VARCHAR(50) UNIQUE,
                        specialization VARCHAR(100),
                        hire_fee DECIMAL(15,2) NOT NULL DEFAULT 0,
                        status ENUM('AVAILABLE', 'BUSY', 'SUSPENDED', 'INACTIVE') NOT NULL DEFAULT 'AVAILABLE',
                        created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

                        CONSTRAINT fk_jockey_user
                            FOREIGN KEY (user_id) REFERENCES users(user_id)
);

-- =========================
-- 5. REFEREE
-- =========================
CREATE TABLE referees (
                         referee_id CHAR(36) PRIMARY KEY,
                         user_id CHAR(36) NOT NULL UNIQUE,

                         certification_level VARCHAR(50),
                         years_of_service INT DEFAULT 0,
                         status ENUM('AVAILABLE', 'ASSIGNED', 'SUSPENDED') NOT NULL DEFAULT 'AVAILABLE',
                         created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

                         CONSTRAINT fk_referee_user
                             FOREIGN KEY (user_id) REFERENCES users(user_id)
);

-- =========================
-- 6. SPECTATOR
-- =========================
CREATE TABLE spectators (
                           spectator_id CHAR(36) PRIMARY KEY,
                           user_id CHAR(36) NOT NULL UNIQUE,

                           total_points INT NOT NULL DEFAULT 0,
                           created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

                           CONSTRAINT fk_spectator_user
                               FOREIGN KEY (user_id) REFERENCES users(user_id)
);

-- =========================
-- 7. VETERINARIAN
-- =========================
CREATE TABLE veterinarians (
                              vet_id CHAR(36) PRIMARY KEY,
                              user_id CHAR(36) NOT NULL UNIQUE,

                              license_number VARCHAR(50) UNIQUE,
                              specialization VARCHAR(100),
                              years_of_service INT DEFAULT 0,
                              status ENUM('AVAILABLE', 'ASSIGNED', 'SUSPENDED') NOT NULL DEFAULT 'AVAILABLE',
                              created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

                              CONSTRAINT fk_veterinarian_user
                                  FOREIGN KEY (user_id) REFERENCES users(user_id)
);

-- =========================
-- 8. MEDICAL STAFF
-- =========================
CREATE TABLE medical_staffs (
                               med_staff_id CHAR(36) PRIMARY KEY,
                               user_id CHAR(36) NOT NULL UNIQUE,

                               certification VARCHAR(100),
                               years_of_service INT DEFAULT 0,
                               status ENUM('AVAILABLE', 'ASSIGNED', 'SUSPENDED') NOT NULL DEFAULT 'AVAILABLE',
                               created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

                               CONSTRAINT fk_medical_staff_user
                                   FOREIGN KEY (user_id) REFERENCES users(user_id)
);

-- =========================
-- 9. HORSE
-- =========================
CREATE TABLE horses (
                       horse_id CHAR(36) PRIMARY KEY,
                       owner_id CHAR(36) NOT NULL,

                       name VARCHAR(100) NOT NULL,
                       breed VARCHAR(100) NOT NULL,
                       gender ENUM('MALE', 'FEMALE') NOT NULL,
                       age INT NOT NULL,
                       weight DECIMAL(6,2),
                       color VARCHAR(50),
                       health_status ENUM('HEALTHY', 'INJURED', 'RETIRED') NOT NULL DEFAULT 'HEALTHY',
                       race_class VARCHAR(50),
                       total_races INT NOT NULL DEFAULT 0,
                       total_wins INT NOT NULL DEFAULT 0,
                       win_rate DECIMAL(5,2) NOT NULL DEFAULT 0,
                       created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

                       CONSTRAINT fk_horse_owner
                           FOREIGN KEY (owner_id) REFERENCES horse_owners(owner_id)
);

-- =========================
-- 10. WALLET
-- =========================
CREATE TABLE wallets (
                        wallet_id CHAR(36) PRIMARY KEY,

                        owner_type ENUM(
                            'USER',
                            'SYSTEM'
                            ) NOT NULL,

                        user_id CHAR(36) NULL,

                        wallet_purpose ENUM(
                            'USER_MAIN',
                            'SYSTEM_REVENUE',
                            'SYSTEM_ESCROW',
                            'SYSTEM_PRIZE_POOL'
                            ) NOT NULL,

                        balance DECIMAL(15,2) NOT NULL DEFAULT 0,
                        currency VARCHAR(10) NOT NULL DEFAULT 'VND',

                        status ENUM(
                            'ACTIVE',
                            'FROZEN',
                            'CLOSED'
                            ) NOT NULL DEFAULT 'ACTIVE',

                        created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

                        updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
                            ON UPDATE CURRENT_TIMESTAMP,

                        CONSTRAINT fk_wallet_user
                            FOREIGN KEY (user_id)
                                REFERENCES users(user_id),

                        CONSTRAINT chk_wallet_owner_purpose
                            CHECK (
                                (
                                    owner_type = 'USER'
                                        AND user_id IS NOT NULL
                                        AND wallet_purpose = 'USER_MAIN'
                                    )
                                    OR
                                (
                                    owner_type = 'SYSTEM'
                                        AND user_id IS NULL
                                        AND wallet_purpose IN (
                                                               'SYSTEM_REVENUE',
                                                               'SYSTEM_ESCROW',
                                                               'SYSTEM_PRIZE_POOL'
                                        )
                                    )
                                ),

                        CONSTRAINT uk_wallet_user_purpose
                            UNIQUE (user_id, wallet_purpose)
);

-- =========================
-- 13. TOURNAMENT
-- =========================
CREATE TABLE tournaments (
                            tournament_id CHAR(36) PRIMARY KEY,
                            created_by CHAR(36) NOT NULL,

                            name VARCHAR(150) NOT NULL,
                            description TEXT,
                            start_date DATE NOT NULL,
                            end_date DATE NOT NULL,
                            finished_at TIMESTAMP NULL,
                            location VARCHAR(200),

                            registration_fee DECIMAL(15,2) NOT NULL DEFAULT 0,
                            system_contract_fee DECIMAL(15,2) NOT NULL DEFAULT 0,
                            total_prize_pool DECIMAL(15,2) NOT NULL DEFAULT 0,

                            allowed_breed VARCHAR(100),
                            race_class VARCHAR(50),
                            weight_class VARCHAR(50),
                            min_horse_age INT,
                            max_horse_age INT,
                            tournament_division VARCHAR(100),
                            handicap_rule TEXT,
                            prediction_open_minutes_before INT NOT NULL DEFAULT 120,
                            prediction_close_minutes_before INT NOT NULL DEFAULT 5,

                            prediction_open_at DATETIME NULL,
                            prediction_close_at DATETIME NULL,
                            prediction_reward_rule TEXT,

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

                            CONSTRAINT fk_tournament_created_by
                                FOREIGN KEY (created_by) REFERENCES users(user_id)
);

-- =========================
-- 14. TOURNAMENT ELIGIBILITY
-- =========================
CREATE TABLE tournament_eligibilities (
                                        eligibility_id CHAR(36) PRIMARY KEY,
                                        tournament_id CHAR(36) NOT NULL,

                                        target_type ENUM('HORSE', 'OWNER', 'JOCKEY') NOT NULL,
                                        condition_name VARCHAR(100) NOT NULL,
                                        condition_operator VARCHAR(10) NOT NULL,
                                        condition_value VARCHAR(100) NOT NULL,
                                        is_active BOOLEAN NOT NULL DEFAULT TRUE,

                                        CONSTRAINT fk_eligibility_tournament
                                            FOREIGN KEY (tournament_id) REFERENCES tournaments(tournament_id)
);

-- =========================
-- 15. PRIZE STRUCTURE
-- =========================
CREATE TABLE prize_structures (
                                 prize_structure_id CHAR(36) PRIMARY KEY,
                                 tournament_id CHAR(36) NOT NULL,

                                 prize_rank INT NOT NULL,
                                 percentage DECIMAL(5,2),
                                 fixed_amount DECIMAL(15,2),
                                 is_active BOOLEAN NOT NULL DEFAULT TRUE,

                                 CONSTRAINT fk_prize_structure_tournament
                                     FOREIGN KEY (tournament_id) REFERENCES tournaments(tournament_id),

                                 UNIQUE KEY uk_prize_tournament_rank (tournament_id, prize_rank)
);

-- =========================
-- 16. TOURNAMENT REGISTRATION
-- =========================
CREATE TABLE horse_tournament_registrations (
                                         horse_tournament_reg_id CHAR(36) PRIMARY KEY,
                                         tournament_id CHAR(36) NOT NULL,
                                         horse_id CHAR(36) NOT NULL,
                                         owner_id CHAR(36) NOT NULL,

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

                                         CONSTRAINT fk_tournament_registration_tournament
                                             FOREIGN KEY (tournament_id) REFERENCES tournaments(tournament_id),

                                         CONSTRAINT fk_tournament_registration_horse
                                             FOREIGN KEY (horse_id) REFERENCES horses(horse_id),

                                         CONSTRAINT fk_tournament_registration_owner
                                             FOREIGN KEY (owner_id) REFERENCES horse_owners(owner_id),

                                         CONSTRAINT fk_tournament_registration_reviewed_by
                                             FOREIGN KEY (reviewed_by) REFERENCES users(user_id),

                                         UNIQUE KEY uk_tournament_horse (tournament_id, horse_id)
);

-- =========================
-- 17. JOCKEY TOURNAMENT REGISTRATION
-- =========================
CREATE TABLE jockey_tournament_registrations (
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
                                                withdraw_reason TEXT,
                                                note TEXT,

                                                CONSTRAINT fk_jockey_tournament_registration_tournament
                                                    FOREIGN KEY (tournament_id) REFERENCES tournaments(tournament_id),

                                                CONSTRAINT fk_jockey_tournament_registration_jockey
                                                    FOREIGN KEY (jockey_id) REFERENCES jockeys(jockey_id),

                                                CONSTRAINT fk_jockey_tournament_registration_reviewed_by
                                                    FOREIGN KEY (reviewed_by) REFERENCES users(user_id),

                                                UNIQUE KEY uk_tournament_jockey (tournament_id, jockey_id)
);

-- =========================
-- 18. JOCKEY HORSE CONTRACT
-- =========================
CREATE TABLE jockey_horse_contracts (
                                       contract_id CHAR(36) PRIMARY KEY,

                                       tournament_id CHAR(36) NOT NULL,
                                       tournament_reg_id CHAR(36) NOT NULL,
                                       jockey_tournament_reg_id CHAR(36) NOT NULL,

                                       owner_id CHAR(36) NOT NULL,
                                       horse_id CHAR(36) NOT NULL,
                                       jockey_id CHAR(36) NOT NULL,

                                       hire_fee DECIMAL(15,2) NOT NULL DEFAULT 0,
                                       advance_percent DECIMAL(5,2) NOT NULL DEFAULT 30,
                                       final_percent DECIMAL(5,2) NOT NULL DEFAULT 70,

                                       advance_paid_amount DECIMAL(15,2) NOT NULL DEFAULT 0,
                                       escrow_amount DECIMAL(15,2) NOT NULL DEFAULT 0,
                                       system_contract_fee DECIMAL(15,2) NOT NULL DEFAULT 0,

                                       owner_prize_share_percent DECIMAL(5,2) NOT NULL DEFAULT 80,
                                       jockey_prize_share_percent DECIMAL(5,2) NOT NULL DEFAULT 20,

                                       payment_status ENUM('UNPAID', 'PAID', 'REFUNDED') NOT NULL DEFAULT 'UNPAID',
                                       escrow_status ENUM(
        'NOT_HELD',
        'HELD',
        'PARTIALLY_RELEASED',
        'RELEASED',
        'REFUNDED'
    ) NOT NULL DEFAULT 'NOT_HELD',

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
                                       rejected_reason TEXT,

                                       cancelled_at TIMESTAMP NULL,
                                       cancel_reason TEXT,

                                       terminated_at TIMESTAMP NULL,
                                       contract_note TEXT,

                                       CONSTRAINT fk_contract_tournament
                                           FOREIGN KEY (tournament_id) REFERENCES tournaments(tournament_id),

                                       CONSTRAINT fk_contract_tournament_registration
                                           FOREIGN KEY (tournament_reg_id) REFERENCES horse_tournament_registrations(horse_tournament_reg_id),

                                       CONSTRAINT fk_contract_jockey_tournament_registration
                                           FOREIGN KEY (jockey_tournament_reg_id) REFERENCES jockey_tournament_registrations(jockey_tournament_reg_id),

                                       CONSTRAINT fk_contract_owner
                                           FOREIGN KEY (owner_id) REFERENCES horse_owners(owner_id),

                                       CONSTRAINT fk_contract_horse
                                           FOREIGN KEY (horse_id) REFERENCES horses(horse_id),

                                       CONSTRAINT fk_contract_jockey
                                           FOREIGN KEY (jockey_id) REFERENCES jockeys(jockey_id),

                                       CONSTRAINT fk_contract_reviewed_by
                                           FOREIGN KEY (reviewed_by) REFERENCES users(user_id)
);

-- =========================
-- 12. INVOICE
-- =========================
CREATE TABLE invoices (
                         invoice_id CHAR(36) PRIMARY KEY,

                         payer_user_id CHAR(36) NOT NULL,
                         tournament_reg_id CHAR(36) NULL,
                         jockey_tournament_reg_id CHAR(36) NULL,
                         contract_id CHAR(36) NULL,

                         invoice_type ENUM(
        'OWNER_TOURNAMENT_REGISTRATION_FEE',
        'JOCKEY_HIRING_FEE',
        'CONTRACT_CREATION_FEE'
    ) NOT NULL,

                         amount DECIMAL(15,2) NOT NULL,
                         status ENUM('UNPAID', 'PAID', 'REFUNDED', 'CANCELLED') NOT NULL DEFAULT 'UNPAID',

                         due_date DATETIME NULL,
                         paid_at TIMESTAMP NULL,
                         refunded_at TIMESTAMP NULL,
                         created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                         note TEXT,

                         CONSTRAINT fk_invoice_payer_user
                             FOREIGN KEY (payer_user_id) REFERENCES users(user_id),

                         CONSTRAINT fk_invoice_tournament_registration
                             FOREIGN KEY (tournament_reg_id) REFERENCES horse_tournament_registrations(horse_tournament_reg_id),

                         CONSTRAINT fk_invoice_jockey_tournament_registration
                             FOREIGN KEY (jockey_tournament_reg_id) REFERENCES jockey_tournament_registrations(jockey_tournament_reg_id),

                         CONSTRAINT fk_invoice_contract
                             FOREIGN KEY (contract_id) REFERENCES jockey_horse_contracts(contract_id)
);

-- =========================
-- 19. ROUND
-- =========================
CREATE TABLE rounds (
                        round_id CHAR(36) PRIMARY KEY,
                        tournament_id CHAR(36) NOT NULL,
                        created_by CHAR(36) NOT NULL,

                        round_name VARCHAR(100) NOT NULL,
                        sequence_order INT NOT NULL,
                        is_final BOOLEAN NOT NULL DEFAULT FALSE,
                        prediction_type ENUM('TOP1', 'TOP3') NOT NULL DEFAULT 'TOP1',
                        advancement_rule TEXT NOT NULL,
                        start_date DATETIME NOT NULL,
                        end_date DATETIME NOT NULL,
                        description TEXT NOT NULL,
                        max_races INT NOT NULL,
                        status ENUM('SCHEDULED', 'ONGOING', 'FINISHED') NOT NULL DEFAULT 'SCHEDULED',
                        created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

                        CONSTRAINT fk_round_tournament
                            FOREIGN KEY (tournament_id) REFERENCES tournaments(tournament_id),

                        CONSTRAINT fk_round_created_by
                            FOREIGN KEY (created_by) REFERENCES users(user_id),

                        UNIQUE KEY uk_round_tournament_sequence (tournament_id, sequence_order)
);

-- =========================
-- 20. RACE
-- =========================
CREATE TABLE races (
                      race_id CHAR(36) PRIMARY KEY,
                      round_id CHAR(36) NOT NULL,
                      created_by CHAR(36) NOT NULL,

                      name VARCHAR(150) NOT NULL,
                      start_time DATETIME NOT NULL,
                      end_time DATETIME NOT NULL,
                      track_condition VARCHAR(100) NOT NULL,
                      distance DECIMAL(8,2) NOT NULL,
                      max_entries INT NOT NULL,
                      prediction_open_at DATETIME NOT NULL,
                      prediction_close_at DATETIME NOT NULL,
                      status ENUM(
        'SCHEDULED',
        'RUNNING',
        'FINISHED_PENDING',
        'OFFICIALLY_FINISHED',
        'CANCELLED'
    ) NOT NULL DEFAULT 'SCHEDULED',

                      started_by CHAR(36) NULL,
                      started_at DATETIME NULL,
                      finished_at DATETIME NULL,
                      schedule_published_at DATETIME NULL,

                      CONSTRAINT fk_race_round
                          FOREIGN KEY (round_id) REFERENCES rounds(round_id),

                      CONSTRAINT fk_race_created_by
                          FOREIGN KEY (created_by) REFERENCES users(user_id),

                      CONSTRAINT fk_race_started_by
                          FOREIGN KEY (started_by) REFERENCES users(user_id)
);

-- =========================
-- 21. RACE REFEREE
-- =========================
CREATE TABLE race_referees (
                              race_id CHAR(36) NOT NULL,
                              referee_id CHAR(36) NOT NULL,

                              role ENUM('MAIN', 'ASSISTANT') NOT NULL,
                              assigned_by CHAR(36) NOT NULL,
                              assigned_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

                              PRIMARY KEY (race_id, referee_id),

                              CONSTRAINT fk_race_referee_race
                                  FOREIGN KEY (race_id) REFERENCES races(race_id),

                              CONSTRAINT fk_race_referee_referee
                                  FOREIGN KEY (referee_id) REFERENCES referees(referee_id),

                              CONSTRAINT fk_race_referee_assigned_by
                                  FOREIGN KEY (assigned_by) REFERENCES users(user_id)
);

-- =========================
-- 22. RACE ENTRY
-- =========================
CREATE TABLE race_entries (
                            entry_id CHAR(36) PRIMARY KEY,
                            race_id CHAR(36) NOT NULL,
                            contract_id CHAR(36) NOT NULL,

                            lane_number INT NOT NULL,
                            starting_position INT,

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
                            withdraw_reason TEXT,
                            scratched_reason TEXT,
                            disqualified_at TIMESTAMP NULL,
                            created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

                            CONSTRAINT fk_race_entry_race
                                FOREIGN KEY (race_id) REFERENCES races(race_id),

                            CONSTRAINT fk_race_entry_contract
                                FOREIGN KEY (contract_id) REFERENCES jockey_horse_contracts(contract_id),

                            CONSTRAINT fk_race_entry_assigned_by
                                FOREIGN KEY (assigned_by) REFERENCES users(user_id),

                            UNIQUE KEY uk_race_lane (race_id, lane_number),
                            UNIQUE KEY uk_race_contract (race_id, contract_id)
);

-- =========================
-- 23. HORSE INSPECTION
-- =========================
CREATE TABLE horse_inspections (
                                  horse_inspection_id CHAR(36) PRIMARY KEY,
                                  entry_id CHAR(36) NOT NULL,
                                  vet_id CHAR(36) NOT NULL,

                                  result ENUM('PASS', 'FAIL') NOT NULL,
                                  note TEXT,
                                  inspected_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

                                  handicap_weight DECIMAL(6,2),
                                  is_handicap_confirmed BOOLEAN NOT NULL DEFAULT FALSE,
                                  confirmed_at TIMESTAMP NULL,

                                  status ENUM('DRAFT', 'SUBMITTED', 'CONFIRMED') NOT NULL DEFAULT 'DRAFT',

                                  CONSTRAINT fk_horse_inspection_entry
                                      FOREIGN KEY (entry_id) REFERENCES race_entries(entry_id),

                                  CONSTRAINT fk_horse_inspection_vet
                                      FOREIGN KEY (vet_id) REFERENCES veterinarians(vet_id),

                                  UNIQUE KEY uk_horse_inspection_entry (entry_id)
);

-- =========================
-- 24. JOCKEY INSPECTION
-- =========================
CREATE TABLE jockey_inspections (
                                   jockey_inspection_id CHAR(36) PRIMARY KEY,
                                   entry_id CHAR(36) NOT NULL,
                                   med_staff_id CHAR(36) NOT NULL,

                                   result ENUM('PASS', 'FAIL') NOT NULL,
                                   note TEXT,
                                   inspected_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                   status ENUM('DRAFT', 'SUBMITTED', 'CONFIRMED') NOT NULL DEFAULT 'DRAFT',

                                   CONSTRAINT fk_jockey_inspection_entry
                                       FOREIGN KEY (entry_id) REFERENCES race_entries(entry_id),

                                   CONSTRAINT fk_jockey_inspection_med_staff
                                       FOREIGN KEY (med_staff_id) REFERENCES medical_staffs(med_staff_id),

                                   UNIQUE KEY uk_jockey_inspection_entry (entry_id)
);

-- =========================
-- 25. VIOLATION
-- =========================
CREATE TABLE violations (
                           violation_id CHAR(36) PRIMARY KEY,
                           entry_id CHAR(36) NOT NULL,
                           referee_id CHAR(36) NOT NULL,

                           type ENUM(
        'FALSE_START',
        'OBSTRUCTION',
        'WRONG_LANE',
        'EQUIPMENT',
        'DOPING',
        'OTHER'
    ) NOT NULL,

                           description TEXT,

                           penalty_type ENUM('WARNING', 'TIME_PENALTY', 'DISQUALIFIED') NOT NULL,
                           penalty_value DECIMAL(8,2),

                           occurred_at TIMESTAMP NOT NULL,
                           created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

                           status ENUM('ACTIVE', 'RESOLVED', 'CANCELLED') NOT NULL DEFAULT 'ACTIVE',

                           CONSTRAINT fk_violation_entry
                               FOREIGN KEY (entry_id) REFERENCES race_entries(entry_id),

                           CONSTRAINT fk_violation_referee
                               FOREIGN KEY (referee_id) REFERENCES referees(referee_id)
);

-- =========================
-- 26. RACE RESULT
-- =========================
CREATE TABLE race_results (
                             result_id CHAR(36) PRIMARY KEY,
                             race_id CHAR(36) NOT NULL,
                             entry_id CHAR(36) NOT NULL,

                             finish_time DECIMAL(10,3),
                             `rank` INT,

                             prize_money DECIMAL(15,2) NOT NULL DEFAULT 0,
                             owner_prize_amount DECIMAL(15,2) NOT NULL DEFAULT 0,
                             jockey_prize_amount DECIMAL(15,2) NOT NULL DEFAULT 0,

                             prize_status ENUM('NOT_ELIGIBLE', 'PENDING_PAYOUT', 'PAID') NOT NULL DEFAULT 'NOT_ELIGIBLE',
                             is_prize_paid BOOLEAN NOT NULL DEFAULT FALSE,
                             prize_paid_at TIMESTAMP NULL,

                             status ENUM('FINISHED', 'DISQUALIFIED') NOT NULL,

                             recorded_by CHAR(36) NOT NULL,
                             recorded_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                             updated_at TIMESTAMP NULL DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,

                             CONSTRAINT fk_race_result_race
                                 FOREIGN KEY (race_id) REFERENCES races(race_id),

                             CONSTRAINT fk_race_result_entry
                                 FOREIGN KEY (entry_id) REFERENCES race_entries(entry_id),

                             CONSTRAINT fk_race_result_recorded_by
                                 FOREIGN KEY (recorded_by) REFERENCES users(user_id),

                             UNIQUE KEY uk_result_entry (entry_id)
);

-- =========================
-- 27. RACE REPORT
-- =========================
CREATE TABLE race_reports (
                             report_id CHAR(36) PRIMARY KEY,
                             race_id CHAR(36) NOT NULL,
                             referee_id CHAR(36) NOT NULL,

                             summary TEXT,
                             appeal_note TEXT,

                             status ENUM('DRAFT', 'SIGNED', 'PUBLISHED') NOT NULL DEFAULT 'DRAFT',

                             signed_by CHAR(36) NULL,
                             signed_at TIMESTAMP NULL,

                             published_by CHAR(36) NULL,
                             published_at TIMESTAMP NULL,

                             created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

                             CONSTRAINT fk_race_report_race
                                 FOREIGN KEY (race_id) REFERENCES races(race_id),

                             CONSTRAINT fk_race_report_referee
                                 FOREIGN KEY (referee_id) REFERENCES referees(referee_id),

                             CONSTRAINT fk_race_report_signed_by
                                 FOREIGN KEY (signed_by) REFERENCES referees(referee_id),

                             CONSTRAINT fk_race_report_published_by
                                 FOREIGN KEY (published_by) REFERENCES users(user_id),

                             UNIQUE KEY uk_race_report_race (race_id)
);

-- =========================
-- 11. WALLET TRANSACTIONS
-- =========================
CREATE TABLE wallet_transactions (
                                     transaction_id CHAR(36) PRIMARY KEY,

                                     wallet_id CHAR(36) NOT NULL,
                                     invoice_id CHAR(36) NULL,
                                     race_result_id CHAR(36) NULL,
                                     contract_id CHAR(36) NULL,

                                     type ENUM(
        'DEPOSIT',
        'OWNER_REGISTRATION_FEE',
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

                                     transaction_group_id CHAR(36) NULL,

                                     status ENUM('PENDING', 'SUCCESS', 'FAILED', 'REVERSED') NOT NULL DEFAULT 'PENDING',
                                     note TEXT,
                                     created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

                                     CONSTRAINT fk_transaction_wallet
                                         FOREIGN KEY (wallet_id) REFERENCES wallets(wallet_id),

                                     CONSTRAINT fk_transaction_invoice
                                         FOREIGN KEY (invoice_id) REFERENCES invoices(invoice_id),

                                     CONSTRAINT fk_transaction_race_result
                                         FOREIGN KEY (race_result_id) REFERENCES race_results(result_id),

                                     CONSTRAINT fk_transaction_contract
                                         FOREIGN KEY (contract_id) REFERENCES jockey_horse_contracts(contract_id),

                                     CONSTRAINT fk_transaction_counterparty_wallet
                                         FOREIGN KEY (counterparty_wallet_id) REFERENCES wallets(wallet_id)
);

-- =========================
-- 28. APPEAL CATEGORY
-- =========================
CREATE TABLE appeal_categories (
                                 category_id CHAR(36) PRIMARY KEY,

                                 code VARCHAR(50) NOT NULL UNIQUE,
                                 name VARCHAR(100) NOT NULL,
                                 description TEXT,
                                 is_active BOOLEAN NOT NULL DEFAULT TRUE,
                                 created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- =========================
-- 29. APPEAL
-- =========================
CREATE TABLE appeals (
                        appeal_id CHAR(36) PRIMARY KEY,

                        entry_id CHAR(36) NOT NULL,
                        race_result_id CHAR(36) NULL,
                        related_violation_id CHAR(36) NULL,
                        category_id CHAR(36) NOT NULL,

                        submitted_by_user_id CHAR(36) NOT NULL,
                        description TEXT NOT NULL,

                        status ENUM(
        'PENDING',
        'UNDER_REVIEW',
        'ACCEPTED',
        'REJECTED',
        'CANCELLED'
    ) NOT NULL DEFAULT 'PENDING',

                        submitted_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

                        reviewed_by_referee_id CHAR(36) NULL,
                        reviewed_at TIMESTAMP NULL,
                        resolution TEXT,

                        CONSTRAINT fk_appeal_entry
                            FOREIGN KEY (entry_id) REFERENCES race_entries(entry_id),

                        CONSTRAINT fk_appeal_race_result
                            FOREIGN KEY (race_result_id) REFERENCES race_results(result_id),

                        CONSTRAINT fk_appeal_violation
                            FOREIGN KEY (related_violation_id) REFERENCES violations(violation_id),

                        CONSTRAINT fk_appeal_category
                            FOREIGN KEY (category_id) REFERENCES appeal_categories(category_id),

                        CONSTRAINT fk_appeal_submitted_by
                            FOREIGN KEY (submitted_by_user_id) REFERENCES users(user_id),

                        CONSTRAINT fk_appeal_reviewed_by_referee
                            FOREIGN KEY (reviewed_by_referee_id) REFERENCES referees(referee_id)
);

-- =========================
-- 30. APPEAL EVIDENCE
-- =========================
CREATE TABLE appeal_evidences (
                                 evidence_id CHAR(36) PRIMARY KEY,
                                 appeal_id CHAR(36) NOT NULL,

                                 type ENUM('IMAGE', 'VIDEO', 'TEXT', 'DOCUMENT') NOT NULL,
                                 file_url TEXT,
                                 text_content TEXT,
                                 description TEXT,
                                 uploaded_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

                                 CONSTRAINT fk_appeal_evidence_appeal
                                     FOREIGN KEY (appeal_id) REFERENCES appeals(appeal_id)
);

-- =========================
-- 31. PREDICTION
-- =========================
CREATE TABLE predictions (
                            prediction_id CHAR(36) PRIMARY KEY,

                            spectator_id CHAR(36) NOT NULL,
                            race_id CHAR(36) NOT NULL,

                            prediction_type ENUM('TOP1', 'TOP3') NOT NULL,
                            prediction_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

                            status ENUM(
        'PENDING',
        'CORRECT',
        'INCORRECT',
        'PARTIALLY_CORRECT',
        'CANCELLED'
    ) NOT NULL DEFAULT 'PENDING',

                            reward_points INT NOT NULL DEFAULT 0,
                            scored_at TIMESTAMP NULL,

                            CONSTRAINT fk_prediction_spectator
                                FOREIGN KEY (spectator_id) REFERENCES spectators(spectator_id),

                            CONSTRAINT fk_prediction_race
                                FOREIGN KEY (race_id) REFERENCES races(race_id),

                            UNIQUE KEY uk_prediction_spectator_race (spectator_id, race_id)
);

-- =========================
-- 32. PREDICTION DETAIL
-- =========================
CREATE TABLE prediction_details (
                                   prediction_detail_id CHAR(36) PRIMARY KEY,

                                   prediction_id CHAR(36) NOT NULL,
                                   entry_id CHAR(36) NOT NULL,

                                   predicted_rank INT NOT NULL,
                                   is_correct BOOLEAN NULL,
                                   awarded_points INT NOT NULL DEFAULT 0,

                                   CONSTRAINT fk_prediction_detail_prediction
                                       FOREIGN KEY (prediction_id) REFERENCES predictions(prediction_id),

                                   CONSTRAINT fk_prediction_detail_entry
                                       FOREIGN KEY (entry_id) REFERENCES race_entries(entry_id),

                                   UNIQUE KEY uk_prediction_rank (prediction_id, predicted_rank),
                                   UNIQUE KEY uk_prediction_entry (prediction_id, entry_id)
);

-- =========================
-- 33. AI PREDICTION
-- =========================
CREATE TABLE ai_predictions (
                               ai_prediction_id CHAR(36) PRIMARY KEY,

                               entry_id CHAR(36) NOT NULL,

                               win_rate_horse DECIMAL(5,2) NOT NULL DEFAULT 0,
                               win_rate_jockey DECIMAL(5,2) NOT NULL DEFAULT 0,
                               experience_score DECIMAL(8,2) NOT NULL DEFAULT 0,
                               score DECIMAL(8,2) NOT NULL DEFAULT 0,
                               win_probability DECIMAL(5,2) NOT NULL DEFAULT 0,

                               algorithm_type ENUM('WEIGHTED_SCORE', 'ML_MODEL', 'LLM_EXPLANATION') NOT NULL DEFAULT 'WEIGHTED_SCORE',
                               model_version VARCHAR(50),
                               explanation TEXT,
                               generated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

                               CONSTRAINT fk_ai_prediction_entry
                                   FOREIGN KEY (entry_id) REFERENCES race_entries(entry_id),

                               UNIQUE KEY uk_ai_prediction_entry (entry_id)
);

-- =========================
-- 34. NOTIFICATION
-- =========================
CREATE TABLE notifications (
                              noti_id CHAR(36) PRIMARY KEY,

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

                              related_type VARCHAR(50),
                              related_id CHAR(36),

                              created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

                              is_read BOOLEAN NOT NULL DEFAULT FALSE,
                              read_at TIMESTAMP NULL,

                              CONSTRAINT fk_notification_user
                                  FOREIGN KEY (user_id) REFERENCES users(user_id)
);

-- ============================================================================
-- HRTMS DEMO CLEANUP (MySQL 8+)
-- ============================================================================
-- Xóa dữ liệu do ba file demo tạo ra:
--   1) demo-test-data.sql
--   2) demo-full-coverage-extension.sql
--   3) demo-workflow-scenarios.sql
--
-- Dừng BE trước khi chạy để scheduler không cập nhật dữ liệu giữa chừng.
-- Chỉ chạy trên database local/test. File 3 dùng lại dữ liệu của file 1 và 2,
-- vì vậy không thể xóa an toàn riêng file 1/2 khi file 3 vẫn còn khóa ngoại.
-- Role và ba ví hệ thống được giữ lại vì đây là dữ liệu nền của ứng dụng.
-- ============================================================================

USE SWP391_Project_HRTMS;

SET @old_safe_updates = @@SQL_SAFE_UPDATES;
SET @old_foreign_key_checks = @@FOREIGN_KEY_CHECKS;
SET SQL_SAFE_UPDATES = 0;
SET FOREIGN_KEY_CHECKS = 0;

START TRANSACTION;
DROP TEMPORARY TABLE IF EXISTS tmp_demo_users;
DROP TEMPORARY TABLE IF EXISTS tmp_demo_tournaments;
DROP TEMPORARY TABLE IF EXISTS tmp_demo_rounds;
DROP TEMPORARY TABLE IF EXISTS tmp_demo_races;
DROP TEMPORARY TABLE IF EXISTS tmp_demo_horses;
DROP TEMPORARY TABLE IF EXISTS tmp_demo_jockeys;
DROP TEMPORARY TABLE IF EXISTS tmp_demo_horse_regs;
DROP TEMPORARY TABLE IF EXISTS tmp_demo_jockey_regs;
DROP TEMPORARY TABLE IF EXISTS tmp_demo_contracts;
DROP TEMPORARY TABLE IF EXISTS tmp_demo_entries;
DROP TEMPORARY TABLE IF EXISTS tmp_demo_results;
DROP TEMPORARY TABLE IF EXISTS tmp_demo_wallets;
DROP TEMPORARY TABLE IF EXISTS tmp_demo_invoices;
DROP TEMPORARY TABLE IF EXISTS tmp_demo_predictions;
DROP TEMPORARY TABLE IF EXISTS tmp_demo_violations;
DROP TEMPORARY TABLE IF EXISTS tmp_demo_appeals;
DROP TEMPORARY TABLE IF EXISTS tmp_demo_events;
DROP TEMPORARY TABLE IF EXISTS tmp_demo_notifications;
CREATE TEMPORARY TABLE tmp_demo_users (id CHAR(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci PRIMARY KEY);
CREATE TEMPORARY TABLE tmp_demo_tournaments (id CHAR(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci PRIMARY KEY);
CREATE TEMPORARY TABLE tmp_demo_rounds (id CHAR(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci PRIMARY KEY);
CREATE TEMPORARY TABLE tmp_demo_races (id CHAR(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci PRIMARY KEY);
CREATE TEMPORARY TABLE tmp_demo_horses (id CHAR(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci PRIMARY KEY);
CREATE TEMPORARY TABLE tmp_demo_jockeys (id CHAR(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci PRIMARY KEY);
CREATE TEMPORARY TABLE tmp_demo_horse_regs (id CHAR(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci PRIMARY KEY);
CREATE TEMPORARY TABLE tmp_demo_jockey_regs (id CHAR(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci PRIMARY KEY);
CREATE TEMPORARY TABLE tmp_demo_contracts (id CHAR(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci PRIMARY KEY);
CREATE TEMPORARY TABLE tmp_demo_entries (id CHAR(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci PRIMARY KEY);
CREATE TEMPORARY TABLE tmp_demo_results (id CHAR(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci PRIMARY KEY);
CREATE TEMPORARY TABLE tmp_demo_wallets (id CHAR(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci PRIMARY KEY);
CREATE TEMPORARY TABLE tmp_demo_invoices (id CHAR(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci PRIMARY KEY);
CREATE TEMPORARY TABLE tmp_demo_predictions (id CHAR(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci PRIMARY KEY);
CREATE TEMPORARY TABLE tmp_demo_violations (id CHAR(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci PRIMARY KEY);
CREATE TEMPORARY TABLE tmp_demo_appeals (id CHAR(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci PRIMARY KEY);
CREATE TEMPORARY TABLE tmp_demo_events (id CHAR(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci PRIMARY KEY);
CREATE TEMPORARY TABLE tmp_demo_notifications (id CHAR(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci PRIMARY KEY);

CREATE TEMPORARY TABLE tmp_demo_users (id CHAR(36) PRIMARY KEY);
INSERT INTO tmp_demo_users (id)
SELECT user_id
FROM users
WHERE user_id LIKE '10000000-0000-0000-0000-%'
   OR user_id LIKE '11000000-0000-0000-0000-%'
   OR user_id LIKE '12000000-0000-0000-0000-%';

CREATE TEMPORARY TABLE tmp_demo_tournaments (id CHAR(36) PRIMARY KEY);
INSERT INTO tmp_demo_tournaments (id)
SELECT tournament_id
FROM tournaments
WHERE tournament_id IN (
    '50000000-0000-0000-0000-000000000001',
    '50000000-0000-0000-0000-000000000002',
    '50000000-0000-0000-0000-000000000003',
    '50000000-0000-0000-0000-000000000004',
    '50000000-0000-0000-0000-000000000005',
    '50000000-0000-0000-0000-000000000006',
    '50000000-0000-0000-0000-000000000007'
);

CREATE TEMPORARY TABLE tmp_demo_rounds (id CHAR(36) PRIMARY KEY);
INSERT INTO tmp_demo_rounds (id)
SELECT round_id FROM rounds
WHERE tournament_id IN (SELECT id FROM tmp_demo_tournaments);

CREATE TEMPORARY TABLE tmp_demo_races (id CHAR(36) PRIMARY KEY);
INSERT INTO tmp_demo_races (id)
SELECT race_id FROM races
WHERE round_id IN (SELECT id FROM tmp_demo_rounds);

CREATE TEMPORARY TABLE tmp_demo_horses (id CHAR(36) PRIMARY KEY);
INSERT INTO tmp_demo_horses (id)
SELECT horse_id FROM horses
WHERE horse_id LIKE '40000000-0000-0000-0000-%'
   OR horse_id LIKE '44000000-0000-0000-0000-%';

CREATE TEMPORARY TABLE tmp_demo_jockeys (id CHAR(36) PRIMARY KEY);
INSERT INTO tmp_demo_jockeys (id)
SELECT jockey_id FROM jockeys
WHERE user_id IN (SELECT id FROM tmp_demo_users);

CREATE TEMPORARY TABLE tmp_demo_horse_regs (id CHAR(36) PRIMARY KEY);
INSERT INTO tmp_demo_horse_regs (id)
SELECT horse_tournament_reg_id
FROM horse_tournament_registrations
WHERE tournament_id IN (SELECT id FROM tmp_demo_tournaments)
   OR horse_id IN (SELECT id FROM tmp_demo_horses);

CREATE TEMPORARY TABLE tmp_demo_jockey_regs (id CHAR(36) PRIMARY KEY);
INSERT INTO tmp_demo_jockey_regs (id)
SELECT jockey_tournament_reg_id
FROM jockey_tournament_registrations
WHERE tournament_id IN (SELECT id FROM tmp_demo_tournaments)
   OR jockey_id IN (SELECT id FROM tmp_demo_jockeys);

CREATE TEMPORARY TABLE tmp_demo_contracts (id CHAR(36) PRIMARY KEY);
INSERT INTO tmp_demo_contracts (id)
SELECT contract_id
FROM jockey_horse_contracts
WHERE tournament_id IN (SELECT id FROM tmp_demo_tournaments)
   OR horse_tournament_reg_id IN (SELECT id FROM tmp_demo_horse_regs)
   OR jockey_tournament_reg_id IN (SELECT id FROM tmp_demo_jockey_regs);

CREATE TEMPORARY TABLE tmp_demo_entries (id CHAR(36) PRIMARY KEY);
INSERT INTO tmp_demo_entries (id)
SELECT entry_id
FROM race_entries
WHERE race_id IN (SELECT id FROM tmp_demo_races)
   OR contract_id IN (SELECT id FROM tmp_demo_contracts);

CREATE TEMPORARY TABLE tmp_demo_results (id CHAR(36) PRIMARY KEY);
INSERT INTO tmp_demo_results (id)
SELECT result_id
FROM race_results
WHERE race_id IN (SELECT id FROM tmp_demo_races)
   OR entry_id IN (SELECT id FROM tmp_demo_entries);

CREATE TEMPORARY TABLE tmp_demo_wallets (id CHAR(36) PRIMARY KEY);
INSERT INTO tmp_demo_wallets (id)
SELECT wallet_id FROM wallets
WHERE user_id IN (SELECT id FROM tmp_demo_users);

CREATE TEMPORARY TABLE tmp_demo_invoices (id CHAR(36) PRIMARY KEY);
INSERT INTO tmp_demo_invoices (id)
SELECT invoice_id
FROM invoices
WHERE payer_user_id IN (SELECT id FROM tmp_demo_users)
   OR tournament_reg_id IN (SELECT id FROM tmp_demo_horse_regs)
   OR jockey_tournament_reg_id IN (SELECT id FROM tmp_demo_jockey_regs)
   OR contract_id IN (SELECT id FROM tmp_demo_contracts);

CREATE TEMPORARY TABLE tmp_demo_predictions (id CHAR(36) PRIMARY KEY);
INSERT INTO tmp_demo_predictions (id)
SELECT prediction_id
FROM predictions
WHERE race_id IN (SELECT id FROM tmp_demo_races)
   OR spectator_id IN (
       SELECT spectator_id FROM spectators
       WHERE user_id IN (SELECT id FROM tmp_demo_users)
   );

CREATE TEMPORARY TABLE tmp_demo_violations (id CHAR(36) PRIMARY KEY);
INSERT INTO tmp_demo_violations (id)
SELECT violation_id FROM violations
WHERE entry_id IN (SELECT id FROM tmp_demo_entries);

CREATE TEMPORARY TABLE tmp_demo_appeals (id CHAR(36) PRIMARY KEY);
INSERT INTO tmp_demo_appeals (id)
SELECT appeal_id
FROM appeals
WHERE entry_id IN (SELECT id FROM tmp_demo_entries)
   OR race_result_id IN (SELECT id FROM tmp_demo_results)
   OR related_violation_id IN (SELECT id FROM tmp_demo_violations)
   OR submitted_by_user_id IN (SELECT id FROM tmp_demo_users);

CREATE TEMPORARY TABLE tmp_demo_events (id CHAR(36) PRIMARY KEY);
INSERT IGNORE INTO tmp_demo_events (id)
SELECT event_id
FROM notification_events
WHERE event_id = 'fc000000-0000-0000-0000-000000000001'
   OR aggregate_id IN (SELECT id FROM tmp_demo_tournaments)
   OR aggregate_id IN (SELECT id FROM tmp_demo_rounds)
   OR aggregate_id IN (SELECT id FROM tmp_demo_races)
   OR aggregate_id IN (SELECT id FROM tmp_demo_contracts)
   OR aggregate_id IN (SELECT id FROM tmp_demo_entries)
   OR aggregate_id IN (SELECT id FROM tmp_demo_appeals);

CREATE TEMPORARY TABLE tmp_demo_notifications (id CHAR(36) PRIMARY KEY);
INSERT IGNORE INTO tmp_demo_notifications (id)
SELECT notification_id
FROM notifications
WHERE event_id IN (SELECT id FROM tmp_demo_events)
   OR recipient_user_id IN (SELECT id FROM tmp_demo_users)
   OR related_id IN (SELECT id FROM tmp_demo_tournaments)
   OR related_id IN (SELECT id FROM tmp_demo_races)
   OR related_id IN (SELECT id FROM tmp_demo_contracts)
   OR related_id IN (SELECT id FROM tmp_demo_appeals);

-- Notification và thông tin xác thực của các tài khoản demo.
DELETE FROM notification_deliveries
WHERE notification_id IN (SELECT id FROM tmp_demo_notifications);

DELETE FROM notifications
WHERE notification_id IN (SELECT id FROM tmp_demo_notifications);

DELETE FROM notification_events
WHERE event_id IN (SELECT id FROM tmp_demo_events);

DELETE FROM notification_preferences
WHERE user_id IN (SELECT id FROM tmp_demo_users);

DELETE FROM email_verifications
WHERE username IN (
    'admin1', 'owner1', 'spectator1', 'spectator2',
    'referee1', 'referee2', 'vet1', 'vet2', 'medical1', 'medical2'
)
   OR username REGEXP '^jockey([1-9]|[1-3][0-9]|40)$'
   OR email LIKE 'demo.%@hrtms.local';

-- Thanh toán và sổ cái phải xóa trước invoice/wallet/user.
DELETE FROM payment_transactions
WHERE user_id IN (SELECT id FROM tmp_demo_users)
   OR wallet_id IN (SELECT id FROM tmp_demo_wallets);

DELETE FROM wallet_transactions
WHERE wallet_id IN (SELECT id FROM tmp_demo_wallets)
   OR counterparty_wallet_id IN (SELECT id FROM tmp_demo_wallets)
   OR invoice_id IN (SELECT id FROM tmp_demo_invoices)
   OR race_result_id IN (SELECT id FROM tmp_demo_results)
   OR contract_id IN (SELECT id FROM tmp_demo_contracts)
   OR performed_by_user_id IN (SELECT id FROM tmp_demo_users)
   OR transaction_id IN (
       'fa000000-0000-0000-0000-000000000001',
       'fa000000-0000-0000-0000-000000000002',
       'fa000000-0000-0000-0000-000000000003'
   );

DELETE FROM invoices
WHERE invoice_id IN (SELECT id FROM tmp_demo_invoices);

-- Appeal, prediction, inspection, result và dữ liệu vận hành của race.
DELETE FROM appeal_evidences
WHERE appeal_id IN (SELECT id FROM tmp_demo_appeals);

DELETE FROM appeals
WHERE appeal_id IN (SELECT id FROM tmp_demo_appeals);

DELETE FROM violations
WHERE violation_id IN (SELECT id FROM tmp_demo_violations);

DELETE FROM prediction_detail
WHERE prediction_id IN (SELECT id FROM tmp_demo_predictions)
   OR entry_id IN (SELECT id FROM tmp_demo_entries);

DELETE FROM predictions
WHERE prediction_id IN (SELECT id FROM tmp_demo_predictions);

DELETE FROM horse_rating_histories
WHERE race_id IN (SELECT id FROM tmp_demo_races)
   OR horse_id IN (SELECT id FROM tmp_demo_horses)
   OR race_result_id IN (SELECT id FROM tmp_demo_results);

DELETE FROM horse_inspections
WHERE entry_id IN (SELECT id FROM tmp_demo_entries);

DELETE FROM jockey_inspections
WHERE entry_id IN (SELECT id FROM tmp_demo_entries);

DELETE FROM ai_predictions
WHERE entry_id IN (SELECT id FROM tmp_demo_entries);

DELETE FROM race_reports
WHERE race_id IN (SELECT id FROM tmp_demo_races);

DELETE FROM race_results
WHERE result_id IN (SELECT id FROM tmp_demo_results);

DELETE FROM race_referees
WHERE race_id IN (SELECT id FROM tmp_demo_races);

DELETE FROM race_inspection_staff_assignments
WHERE race_id IN (SELECT id FROM tmp_demo_races);

DELETE FROM race_entries
WHERE entry_id IN (SELECT id FROM tmp_demo_entries);

DELETE FROM races
WHERE race_id IN (SELECT id FROM tmp_demo_races);

DELETE FROM rounds
WHERE round_id IN (SELECT id FROM tmp_demo_rounds);

-- Tournament, hồ sơ, contract và profile.
DELETE FROM jockey_horse_contracts
WHERE contract_id IN (SELECT id FROM tmp_demo_contracts);

DELETE FROM prize_structures
WHERE tournament_id IN (SELECT id FROM tmp_demo_tournaments);

DELETE FROM tournament_eligibility
WHERE tournament_id IN (SELECT id FROM tmp_demo_tournaments);

DELETE FROM horse_tournament_registrations
WHERE horse_tournament_reg_id IN (SELECT id FROM tmp_demo_horse_regs);

DELETE FROM jockey_tournament_registrations
WHERE jockey_tournament_reg_id IN (SELECT id FROM tmp_demo_jockey_regs);

DELETE FROM tournaments
WHERE tournament_id IN (SELECT id FROM tmp_demo_tournaments);

DELETE FROM horses
WHERE horse_id IN (SELECT id FROM tmp_demo_horses);

DELETE FROM wallets
WHERE wallet_id IN (SELECT id FROM tmp_demo_wallets);

DELETE FROM spectators
WHERE user_id IN (SELECT id FROM tmp_demo_users);

DELETE FROM jockeys
WHERE user_id IN (SELECT id FROM tmp_demo_users);

DELETE FROM referees
WHERE user_id IN (SELECT id FROM tmp_demo_users);

DELETE FROM veterinarians
WHERE user_id IN (SELECT id FROM tmp_demo_users);

DELETE FROM medical_staffs
WHERE user_id IN (SELECT id FROM tmp_demo_users);

DELETE FROM horse_owners
WHERE user_id IN (SELECT id FROM tmp_demo_users);

DELETE FROM users
WHERE user_id IN (SELECT id FROM tmp_demo_users);

-- Giữ ba ví hệ thống, nhưng đưa balance về số dư cuối của giao dịch thật còn lại.
-- Nếu ví chưa từng có giao dịch thật thì số dư trở về 0.
UPDATE wallets w
SET w.balance = COALESCE((
        SELECT wt.balance_after
        FROM wallet_transactions wt
        WHERE wt.wallet_id = w.wallet_id
          AND wt.status = 'SUCCESS'
        ORDER BY wt.created_at DESC, wt.transaction_id DESC
        LIMIT 1
    ), 0),
    w.updated_at = NOW()
WHERE w.owner_type = 'SYSTEM'
  AND w.user_id IS NULL
  AND w.wallet_purpose IN ('SYSTEM_REVENUE', 'SYSTEM_ESCROW', 'SYSTEM_PRIZE_POOL');

-- Chỉ xóa hai category do seed tạo nếu không còn appeal thật sử dụng.
DELETE ac FROM appeal_categories ac
WHERE ac.category_id IN (
    'f1000000-0000-0000-0000-000000000001',
    'f1000000-0000-0000-0000-000000000002'
)
AND NOT EXISTS (
    SELECT 1 FROM appeals a WHERE a.category_id = ac.category_id
);

COMMIT;

SET FOREIGN_KEY_CHECKS = @old_foreign_key_checks;
SET SQL_SAFE_UPDATES = @old_safe_updates;

SELECT 'Demo users còn lại' AS check_name, COUNT(*) AS total
FROM users
WHERE user_id LIKE '10000000-0000-0000-0000-%'
   OR user_id LIKE '11000000-0000-0000-0000-%'
   OR user_id LIKE '12000000-0000-0000-0000-%'
UNION ALL
SELECT 'Demo tournaments còn lại', COUNT(*)
FROM tournaments
WHERE tournament_id LIKE '50000000-0000-0000-0000-%'
UNION ALL
SELECT 'Demo races còn lại', COUNT(*)
FROM races
WHERE race_id LIKE '90000000-0000-0000-0000-%';

-- ============================================================
-- Migration v2: Make Round dynamic, add sequence_order to Race
-- ============================================================

ALTER TABLE tournaments
    ADD COLUMN max_rounds INT NOT NULL DEFAULT 2
    AFTER prediction_reward_rule;

ALTER TABLE races
    ADD COLUMN sequence_order INT NOT NULL DEFAULT 1
    AFTER max_entries;

ALTER TABLE races
    ADD UNIQUE KEY uk_race_round_sequence (round_id, sequence_order);

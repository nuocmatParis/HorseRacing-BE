ALTER TABLE tournaments ADD COLUMN max_approved_entries INT NULL;
ALTER TABLE tournaments ADD COLUMN planned_round_count INT NULL;
ALTER TABLE tournaments ADD COLUMN planned_race_count INT NULL;
ALTER TABLE tournaments ADD COLUMN bracket_plan_status VARCHAR(50) NOT NULL DEFAULT 'NOT_GENERATED';
ALTER TABLE tournaments ADD COLUMN bracket_plan_version INT NULL DEFAULT 1;
ALTER TABLE tournaments ADD COLUMN current_round_name VARCHAR(100) NULL;
ALTER TABLE tournaments ADD COLUMN min_round_gap_days INT NOT NULL DEFAULT 1;

ALTER TABLE rounds ADD COLUMN expected_entries INT NULL;
ALTER TABLE rounds ADD COLUMN planned_race_count INT NULL;
ALTER TABLE rounds ADD COLUMN qualifiers_per_race INT NULL;

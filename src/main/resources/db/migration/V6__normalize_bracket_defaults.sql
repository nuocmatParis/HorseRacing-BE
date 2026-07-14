UPDATE tournaments
SET max_approved_entries = max_approved_horses
WHERE max_approved_entries IS NULL;

ALTER TABLE tournaments
    MODIFY COLUMN max_approved_entries INT NOT NULL;

-- V4 introduced this column with 1 although the agreed business default is 7.
UPDATE tournaments
SET min_round_gap_days = 7
WHERE min_round_gap_days = 1;

ALTER TABLE tournaments
    MODIFY COLUMN min_round_gap_days INT NOT NULL DEFAULT 7;

UPDATE tournaments
SET bracket_plan_version = 1
WHERE bracket_plan_version IS NULL;

ALTER TABLE tournaments
    MODIFY COLUMN bracket_plan_version INT NOT NULL DEFAULT 1;

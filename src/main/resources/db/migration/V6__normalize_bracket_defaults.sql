UPDATE tournaments
SET max_approved_entries = max_approved_horses
WHERE max_approved_entries IS NULL;

ALTER TABLE tournaments
    MODIFY COLUMN max_approved_entries INT NOT NULL;

UPDATE tournaments
SET bracket_plan_version = 1
WHERE bracket_plan_version IS NULL;

ALTER TABLE tournaments
    MODIFY COLUMN bracket_plan_version INT NOT NULL DEFAULT 1;

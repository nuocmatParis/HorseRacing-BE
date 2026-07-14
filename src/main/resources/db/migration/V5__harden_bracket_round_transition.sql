ALTER TABLE rounds MODIFY COLUMN start_date DATETIME NULL;
ALTER TABLE rounds MODIFY COLUMN end_date DATETIME NULL;

-- Legacy test data can contain orphan races created while FK checks were disabled.
-- Changing nullability must not delete those unrelated rows during this migration.
SET FOREIGN_KEY_CHECKS = 0;
ALTER TABLE races MODIFY COLUMN start_time DATETIME NULL;
ALTER TABLE races MODIFY COLUMN end_time DATETIME NULL;
ALTER TABLE races MODIFY COLUMN prediction_open_at DATETIME NULL;
ALTER TABLE races MODIFY COLUMN prediction_close_at DATETIME NULL;
SET FOREIGN_KEY_CHECKS = 1;

ALTER TABLE rounds ADD COLUMN bracket_plan_version INT NULL;
ALTER TABLE rounds ADD COLUMN advanced_at DATETIME NULL;
ALTER TABLE rounds ADD COLUMN transition_status VARCHAR(50) NOT NULL DEFAULT 'NOT_READY';

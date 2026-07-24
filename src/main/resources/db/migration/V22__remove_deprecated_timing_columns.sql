-- Add track_condition to tournaments table

use swp391_project_hrtms
ALTER TABLE tournaments ADD COLUMN track_condition VARCHAR(50) NULL DEFAULT 'TURF';
UPDATE tournaments SET track_condition = 'TURF' WHERE track_condition IS NULL;

-- Drop deprecated timing columns from tournaments table
ALTER TABLE tournaments DROP COLUMN apply_break_time;
ALTER TABLE tournaments DROP COLUMN break_start_time;
ALTER TABLE tournaments DROP COLUMN break_end_time;
ALTER TABLE tournaments DROP COLUMN start_early_tolerance_minutes;
ALTER TABLE tournaments DROP COLUMN prediction_card_open_hours_before_first_race;
ALTER TABLE tournaments DROP COLUMN min_entries_per_race;

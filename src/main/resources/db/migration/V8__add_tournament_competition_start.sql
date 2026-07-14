ALTER TABLE tournaments ADD COLUMN competition_start_at DATETIME NULL;

UPDATE tournaments
SET competition_start_at = TIMESTAMP(
        DATE_ADD(DATE(scheduling_deadline_at), INTERVAL 2 DAY),
        race_day_start_time
    )
WHERE competition_start_at IS NULL;

ALTER TABLE tournaments MODIFY COLUMN competition_start_at DATETIME NOT NULL;

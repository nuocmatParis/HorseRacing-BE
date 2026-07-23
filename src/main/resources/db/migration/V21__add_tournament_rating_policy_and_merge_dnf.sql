UPDATE race_results
SET status = 'DISQUALIFIED'
WHERE status = 'DID_NOT_FINISH';

UPDATE race_entries
SET status = 'DISQUALIFIED'
WHERE status = 'DID_NOT_FINISH';

ALTER TABLE tournaments
    ADD COLUMN rating_first_min INT NOT NULL DEFAULT 6,
    ADD COLUMN rating_first_max INT NOT NULL DEFAULT 12,
    ADD COLUMN rating_second_min INT NOT NULL DEFAULT 2,
    ADD COLUMN rating_second_max INT NOT NULL DEFAULT 5,
    ADD COLUMN rating_third_min INT NOT NULL DEFAULT 1,
    ADD COLUMN rating_third_max INT NOT NULL DEFAULT 4,
    ADD COLUMN rating_fourth_fifth_min INT NOT NULL DEFAULT 0,
    ADD COLUMN rating_fourth_fifth_max INT NOT NULL DEFAULT 2,
    ADD COLUMN rating_other_min INT NOT NULL DEFAULT -8,
    ADD COLUMN rating_other_max INT NOT NULL DEFAULT 0,
    ADD COLUMN rating_disqualified_min INT NOT NULL DEFAULT -8,
    ADD COLUMN rating_disqualified_max INT NOT NULL DEFAULT 0,
    ADD COLUMN rating_policy_version INT NOT NULL DEFAULT 1,
    ADD COLUMN rating_policy_locked_at DATETIME(6) NULL;

UPDATE tournaments
SET rating_policy_locked_at = COALESCE(published_at, created_at)
WHERE status <> 'DRAFT' OR phase <> 'DRAFT';

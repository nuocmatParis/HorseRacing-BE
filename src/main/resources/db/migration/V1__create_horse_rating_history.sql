-- 1. Create horse_rating_histories table
DROP TABLE IF EXISTS horse_rating_histories;
CREATE TABLE horse_rating_histories (
    rating_history_id CHAR(36) NOT NULL,
    horse_id CHAR(36) NOT NULL,
    race_id CHAR(36) NOT NULL,
    race_result_id CHAR(36) NOT NULL,
    old_rating INT NOT NULL,
    base_change INT NOT NULL,
    opponent_strength_bonus INT NOT NULL,
    finish_performance_bonus INT NOT NULL,
    field_size_bonus INT NOT NULL,
    underperformance_penalty INT NOT NULL,
    final_change INT NOT NULL,
    new_rating INT NOT NULL,
    old_race_class VARCHAR(50) NOT NULL,
    new_race_class VARCHAR(50) NOT NULL,
    policy_version INT NOT NULL,
    calculated_at DATETIME NOT NULL,
    PRIMARY KEY (rating_history_id),
    CONSTRAINT uq_race_result_id UNIQUE (race_result_id),
    CONSTRAINT fk_hrh_horse FOREIGN KEY (horse_id) REFERENCES horses(horse_id),
    CONSTRAINT fk_hrh_race FOREIGN KEY (race_id) REFERENCES races(race_id),
    CONSTRAINT fk_hrh_race_result FOREIGN KEY (race_result_id) REFERENCES race_results(result_id)
);

CREATE INDEX idx_hrh_horse_calculated ON horse_rating_histories(horse_id, calculated_at);
CREATE INDEX idx_hrh_race ON horse_rating_histories(race_id);

-- 2. Add columns to horse_tournament_registrations if they don't exist
DROP PROCEDURE IF EXISTS AddRatingColumns;
DELIMITER //
CREATE PROCEDURE AddRatingColumns()
BEGIN
    DECLARE col_exists INT DEFAULT 0;
    
    SELECT COUNT(*) INTO col_exists 
    FROM information_schema.COLUMNS 
    WHERE TABLE_SCHEMA = DATABASE() 
      AND TABLE_NAME = 'horse_tournament_registrations' 
      AND COLUMN_NAME = 'rating_at_registration';
      
    IF col_exists = 0 THEN
        ALTER TABLE horse_tournament_registrations ADD COLUMN rating_at_registration INT NULL;
    END IF;

    SET col_exists = 0;
    SELECT COUNT(*) INTO col_exists 
    FROM information_schema.COLUMNS 
    WHERE TABLE_SCHEMA = DATABASE() 
      AND TABLE_NAME = 'horse_tournament_registrations' 
      AND COLUMN_NAME = 'race_class_at_registration';
      
    IF col_exists = 0 THEN
        ALTER TABLE horse_tournament_registrations ADD COLUMN race_class_at_registration VARCHAR(50) NULL;
    END IF;
END //
DELIMITER ;
CALL AddRatingColumns();
DROP PROCEDURE AddRatingColumns;

-- 3. Backfill data
UPDATE horse_tournament_registrations htr
JOIN horses h ON htr.horse_id = h.horse_id
SET htr.rating_at_registration = h.current_rating,
    htr.race_class_at_registration = h.race_class
WHERE htr.rating_at_registration IS NULL;

-- 4. Alter columns to NOT NULL
ALTER TABLE horse_tournament_registrations MODIFY COLUMN rating_at_registration INT NOT NULL;
ALTER TABLE horse_tournament_registrations MODIFY COLUMN race_class_at_registration VARCHAR(50) NOT NULL;

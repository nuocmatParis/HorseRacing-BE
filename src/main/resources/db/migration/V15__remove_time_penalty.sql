DROP PROCEDURE IF EXISTS RemoveLegacyTimePenalty;
DELIMITER $$
CREATE PROCEDURE RemoveLegacyTimePenalty()
BEGIN
    IF EXISTS (
        SELECT 1
        FROM information_schema.tables
        WHERE table_schema = DATABASE()
          AND table_name = 'violations'
    ) THEN
        UPDATE violations
        SET penalty_type = 'WARNING',
            penalty_value = NULL
        WHERE penalty_type = 'TIME_PENALTY';
    END IF;
END$$
DELIMITER ;

CALL RemoveLegacyTimePenalty();
DROP PROCEDURE RemoveLegacyTimePenalty;

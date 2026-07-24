DELETE FROM tournament_eligibility
WHERE condition_name IN ('BREED', 'JOCKEY_TIER')
   OR (target_type = 'JOCKEY' AND condition_name = 'AGE');

ALTER TABLE tournament_eligibility
    MODIFY COLUMN condition_operator VARCHAR(30) NOT NULL;

INSERT INTO spectators (spectator_id, user_id, total_points, created_at)
SELECT UUID(), users.user_id, 0, CURRENT_TIMESTAMP(6)
FROM users
JOIN roles ON roles.role_id = users.role_id
LEFT JOIN spectators ON spectators.user_id = users.user_id
WHERE roles.role_name = 'SPECTATOR'
  AND spectators.spectator_id IS NULL;

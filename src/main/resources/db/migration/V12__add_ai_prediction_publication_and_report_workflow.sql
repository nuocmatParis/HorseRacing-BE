ALTER TABLE races
    ADD COLUMN ai_prediction_publication_status VARCHAR(20) NULL,
    ADD COLUMN ai_prediction_generated_at DATETIME(6) NULL,
    ADD COLUMN ai_prediction_generated_by CHAR(36) NULL,
    ADD COLUMN ai_prediction_published_at DATETIME(6) NULL,
    ADD COLUMN ai_prediction_published_by CHAR(36) NULL;

ALTER TABLE races
    ADD CONSTRAINT fk_races_ai_prediction_generated_by
        FOREIGN KEY (ai_prediction_generated_by) REFERENCES users(user_id) ON DELETE SET NULL,
    ADD CONSTRAINT fk_races_ai_prediction_published_by
        FOREIGN KEY (ai_prediction_published_by) REFERENCES users(user_id) ON DELETE SET NULL;

UPDATE races r
SET r.ai_prediction_publication_status = 'DRAFT',
    r.ai_prediction_generated_at = (
        SELECT MAX(p.generated_at)
        FROM ai_predictions p
        JOIN race_entries e ON e.entry_id = p.entry_id
        WHERE e.race_id = r.race_id
    )
WHERE EXISTS (
    SELECT 1
    FROM ai_predictions p
    JOIN race_entries e ON e.entry_id = p.entry_id
    WHERE e.race_id = r.race_id
);

ALTER TABLE race_reports
    ADD COLUMN submitted_at DATETIME(6) NULL,
    ADD COLUMN submitted_by CHAR(36) NULL,
    ADD COLUMN returned_at DATETIME(6) NULL,
    ADD COLUMN returned_by CHAR(36) NULL,
    ADD COLUMN return_reason TEXT NULL;

ALTER TABLE race_reports
    ADD CONSTRAINT fk_race_reports_submitted_by
        FOREIGN KEY (submitted_by) REFERENCES referees(referee_id) ON DELETE SET NULL,
    ADD CONSTRAINT fk_race_reports_returned_by
        FOREIGN KEY (returned_by) REFERENCES referees(referee_id) ON DELETE SET NULL;

UPDATE race_reports SET status = 'DRAFT' WHERE status IN ('Draft', 'draft');
UPDATE race_reports SET status = 'SIGNED' WHERE status IN ('Signed', 'signed');
UPDATE race_reports SET status = 'PUBLISHED' WHERE status IN ('Published', 'published');

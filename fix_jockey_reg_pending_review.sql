-- Move existing jockey tournament registrations to PENDING_REVIEW (skip payment step)
USE swp391_project_hrtms;

UPDATE jockey_tournament_registration
SET status = 'PENDING_REVIEW'
WHERE status = 'PENDING_PAYMENT';

-- Some DBs use plural table name from migration script
UPDATE jockey_tournament_registrations
SET status = 'PENDING_REVIEW'
WHERE status = 'PENDING_PAYMENT';

SELECT jockey_tournament_reg_id, status
FROM jockey_tournament_registration
WHERE status = 'PENDING_REVIEW';

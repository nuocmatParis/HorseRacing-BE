-- Add distance column to tournaments
ALTER TABLE tournaments ADD COLUMN distance VARCHAR(50) NULL;

-- Update existing tournaments to have a default distance
UPDATE tournaments SET distance = 'SPRINT_1000M' WHERE distance IS NULL;

-- Make distance column NOT NULL
ALTER TABLE tournaments MODIFY COLUMN distance VARCHAR(50) NOT NULL;

-- Modify races distance column type to match the new enum RaceDistance
ALTER TABLE races MODIFY COLUMN distance VARCHAR(50) NOT NULL;

-- Modify jockeys specialization column type to match the enum Specialization
ALTER TABLE jockeys MODIFY COLUMN specialization VARCHAR(100) NOT NULL;

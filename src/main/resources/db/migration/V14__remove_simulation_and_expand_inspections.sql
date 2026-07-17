DROP TABLE IF EXISTS race_provisional_results;
DROP TABLE IF EXISTS race_simulation_flags;
DROP TABLE IF EXISTS race_simulation_warnings;
DROP TABLE IF EXISTS race_simulation_participants;
DROP TABLE IF EXISTS race_simulation_sessions;

ALTER TABLE horse_inspections
    ADD COLUMN registered_weight FLOAT NULL AFTER handicap_weight,
    ADD COLUMN registered_breed VARCHAR(50) NULL AFTER registered_weight,
    ADD COLUMN actual_weight FLOAT NULL AFTER registered_breed,
    ADD COLUMN actual_breed VARCHAR(50) NULL AFTER actual_weight,
    ADD COLUMN doping_detected BOOLEAN NOT NULL DEFAULT FALSE AFTER actual_breed;

ALTER TABLE jockey_inspections
    ADD COLUMN registered_weight FLOAT NULL AFTER inspected_at,
    ADD COLUMN actual_weight FLOAT NULL AFTER registered_weight,
    ADD COLUMN doping_detected BOOLEAN NOT NULL DEFAULT FALSE AFTER actual_weight;

UPDATE horse_inspections hi
JOIN race_entries re ON re.entry_id = hi.entry_id
JOIN jockey_horse_contracts contract ON contract.contract_id = re.contract_id
JOIN horses horse ON horse.horse_id = contract.horse_id
SET hi.registered_weight = horse.weight,
    hi.registered_breed = horse.breed,
    hi.actual_weight = horse.weight,
    hi.actual_breed = horse.breed
WHERE hi.registered_weight IS NULL
   OR hi.registered_breed IS NULL
   OR hi.actual_weight IS NULL
   OR hi.actual_breed IS NULL;

UPDATE jockey_inspections ji
JOIN race_entries re ON re.entry_id = ji.entry_id
JOIN jockey_horse_contracts contract ON contract.contract_id = re.contract_id
JOIN jockeys jockey ON jockey.jockey_id = contract.jockey_id
SET ji.registered_weight = jockey.weight,
    ji.actual_weight = jockey.weight
WHERE ji.registered_weight IS NULL
   OR ji.actual_weight IS NULL;

ALTER TABLE horse_inspections
    MODIFY COLUMN registered_weight FLOAT NOT NULL,
    MODIFY COLUMN registered_breed VARCHAR(50) NOT NULL,
    MODIFY COLUMN actual_weight FLOAT NOT NULL,
    MODIFY COLUMN actual_breed VARCHAR(50) NOT NULL;

ALTER TABLE jockey_inspections
    MODIFY COLUMN registered_weight FLOAT NOT NULL,
    MODIFY COLUMN actual_weight FLOAT NOT NULL;

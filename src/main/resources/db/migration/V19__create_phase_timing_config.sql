CREATE TABLE IF NOT EXISTS phase_timing_config (
    id            BIGINT       AUTO_INCREMENT PRIMARY KEY,
    phase_name    VARCHAR(50)  NOT NULL,
    min_capacity  INT          NOT NULL DEFAULT 0,
    max_capacity  INT          NOT NULL DEFAULT 9999999,
    duration_days INT          NOT NULL,
    description   VARCHAR(255),
    created_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_phase_capacity (phase_name, min_capacity, max_capacity)
);

CREATE TABLE IF NOT EXISTS tournament_phase_config (
    id            BIGINT       AUTO_INCREMENT PRIMARY KEY,
    tournament_id BINARY(16)   NOT NULL,
    phase_name    VARCHAR(50)  NOT NULL,
    duration_days INT          NOT NULL,
    created_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (tournament_id) REFERENCES tournaments(tournament_id) ON DELETE CASCADE,
    UNIQUE KEY uk_tournament_phase (tournament_id, phase_name)
);

-- Seed global defaults (mirroring the old hardcoded policy)
INSERT IGNORE INTO phase_timing_config (phase_name, min_capacity, max_capacity, duration_days, description) VALUES
    ('REGISTRATION',     0,       8,       3, 'Registration base days for ≤8 entries'),
    ('REGISTRATION',     9,       16,      4, 'Registration days for 16 entries'),
    ('REGISTRATION',     17,      32,      5, 'Registration days for 32 entries'),
    ('REGISTRATION',     33,      64,      6, 'Registration days for 64 entries'),
    ('REGISTRATION',     65,      128,     7, 'Registration days for 128 entries'),
    ('REGISTRATION',     129,     9999999, 8, 'Registration days for 256+ entries'),
    ('REVIEW',           0,       9999999, 4, 'Review period days'),
    ('JOCKEY_MATCHING',  0,       8,       3, 'Jockey matching days for ≤8 entries'),
    ('JOCKEY_MATCHING',  9,       16,      5, 'Jockey matching days for 16 entries'),
    ('JOCKEY_MATCHING',  17,      32,      6, 'Jockey matching days for 32 entries'),
    ('JOCKEY_MATCHING',  33,      64,      7, 'Jockey matching days for 64 entries'),
    ('JOCKEY_MATCHING',  65,      128,     8, 'Jockey matching days for 128 entries'),
    ('JOCKEY_MATCHING',  129,     9999999, 9, 'Jockey matching days for 256+ entries'),
    ('SCHEDULING',       0,       9999999, 4, 'Scheduling period days'),
    ('PRE_RACE_BUFFER',  0,       9999999, 2, 'Pre-race buffer days');

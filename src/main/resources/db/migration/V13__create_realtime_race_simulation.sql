CREATE TABLE race_simulation_sessions (
    session_id CHAR(36) NOT NULL,
    race_id CHAR(36) NOT NULL,
    status VARCHAR(20) NOT NULL,
    random_seed BIGINT NOT NULL,
    current_race_time_seconds DOUBLE NOT NULL DEFAULT 0,
    current_sequence BIGINT NOT NULL DEFAULT 0,
    prepared_at DATETIME(6) NOT NULL,
    started_at DATETIME(6) NULL,
    finished_at DATETIME(6) NULL,
    prepared_by CHAR(36) NOT NULL,
    started_by CHAR(36) NULL,
    timeline_payload LONGTEXT NULL,
    current_snapshot_json LONGTEXT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    PRIMARY KEY (session_id),
    CONSTRAINT uk_simulation_session_race UNIQUE (race_id),
    CONSTRAINT fk_simulation_session_race FOREIGN KEY (race_id) REFERENCES races(race_id),
    CONSTRAINT fk_simulation_session_prepared_by FOREIGN KEY (prepared_by) REFERENCES users(user_id),
    CONSTRAINT fk_simulation_session_started_by FOREIGN KEY (started_by) REFERENCES users(user_id)
) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

CREATE TABLE race_simulation_participants (
    participant_id CHAR(36) NOT NULL,
    session_id CHAR(36) NOT NULL,
    entry_id CHAR(36) NOT NULL,
    horse_id CHAR(36) NOT NULL,
    horse_name VARCHAR(100) NOT NULL,
    horse_image_url VARCHAR(500) NULL,
    jockey_id CHAR(36) NOT NULL,
    jockey_name VARCHAR(100) NOT NULL,
    lane_number INT NOT NULL,
    base_speed DOUBLE NOT NULL,
    acceleration DOUBLE NOT NULL,
    stamina DOUBLE NOT NULL,
    consistency_score DOUBLE NOT NULL,
    jockey_skill DOUBLE NOT NULL,
    jockey_aggressiveness DOUBLE NOT NULL,
    cornering_skill DOUBLE NOT NULL,
    stamina_management DOUBLE NOT NULL,
    handicap_weight DOUBLE NULL,
    PRIMARY KEY (participant_id),
    CONSTRAINT uk_simulation_participant_entry UNIQUE (session_id, entry_id),
    CONSTRAINT uk_simulation_participant_lane UNIQUE (session_id, lane_number),
    CONSTRAINT fk_simulation_participant_session FOREIGN KEY (session_id)
        REFERENCES race_simulation_sessions(session_id) ON DELETE CASCADE,
    CONSTRAINT fk_simulation_participant_entry FOREIGN KEY (entry_id) REFERENCES race_entries(entry_id),
    INDEX idx_simulation_participant_session (session_id)
) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

CREATE TABLE race_simulation_warnings (
    warning_id CHAR(36) NOT NULL,
    session_id CHAR(36) NOT NULL,
    race_id CHAR(36) NOT NULL,
    entry_id CHAR(36) NOT NULL,
    horse_id CHAR(36) NOT NULL,
    warning_type VARCHAR(40) NOT NULL,
    severity VARCHAR(20) NOT NULL,
    risk_score DOUBLE NOT NULL,
    sequence_number BIGINT NOT NULL,
    race_time_seconds DOUBLE NOT NULL,
    message VARCHAR(500) NOT NULL,
    suggested_action VARCHAR(500) NULL,
    created_at DATETIME(6) NOT NULL,
    review_status VARCHAR(20) NOT NULL,
    reviewed_by CHAR(36) NULL,
    reviewed_at DATETIME(6) NULL,
    review_note TEXT NULL,
    PRIMARY KEY (warning_id),
    CONSTRAINT uk_simulation_warning_frame UNIQUE (session_id, entry_id, warning_type, sequence_number),
    CONSTRAINT fk_simulation_warning_session FOREIGN KEY (session_id)
        REFERENCES race_simulation_sessions(session_id) ON DELETE CASCADE,
    CONSTRAINT fk_simulation_warning_race FOREIGN KEY (race_id) REFERENCES races(race_id),
    CONSTRAINT fk_simulation_warning_entry FOREIGN KEY (entry_id) REFERENCES race_entries(entry_id),
    CONSTRAINT fk_simulation_warning_reviewed_by FOREIGN KEY (reviewed_by) REFERENCES users(user_id),
    INDEX idx_simulation_warning_race (race_id, review_status)
) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

CREATE TABLE race_simulation_flags (
    flag_id CHAR(36) NOT NULL,
    session_id CHAR(36) NOT NULL,
    race_id CHAR(36) NOT NULL,
    warning_id CHAR(36) NULL,
    entry_id CHAR(36) NOT NULL,
    horse_id CHAR(36) NOT NULL,
    source VARCHAR(20) NOT NULL,
    status VARCHAR(30) NOT NULL,
    severity VARCHAR(20) NOT NULL,
    race_time_seconds DOUBLE NOT NULL,
    note TEXT NOT NULL,
    flagged_by CHAR(36) NOT NULL,
    flagged_at DATETIME(6) NOT NULL,
    reviewed_by CHAR(36) NULL,
    reviewed_at DATETIME(6) NULL,
    review_note TEXT NULL,
    PRIMARY KEY (flag_id),
    CONSTRAINT fk_simulation_flag_session FOREIGN KEY (session_id)
        REFERENCES race_simulation_sessions(session_id) ON DELETE CASCADE,
    CONSTRAINT fk_simulation_flag_race FOREIGN KEY (race_id) REFERENCES races(race_id),
    CONSTRAINT fk_simulation_flag_warning FOREIGN KEY (warning_id)
        REFERENCES race_simulation_warnings(warning_id) ON DELETE SET NULL,
    CONSTRAINT fk_simulation_flag_entry FOREIGN KEY (entry_id) REFERENCES race_entries(entry_id),
    CONSTRAINT fk_simulation_flag_flagged_by FOREIGN KEY (flagged_by) REFERENCES users(user_id),
    CONSTRAINT fk_simulation_flag_reviewed_by FOREIGN KEY (reviewed_by) REFERENCES users(user_id),
    INDEX idx_simulation_flag_race (race_id, status)
) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

CREATE TABLE race_provisional_results (
    provisional_result_id CHAR(36) NOT NULL,
    session_id CHAR(36) NOT NULL,
    race_id CHAR(36) NOT NULL,
    entry_id CHAR(36) NOT NULL,
    finish_position INT NULL,
    finish_time DOUBLE NULL,
    status VARCHAR(30) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    PRIMARY KEY (provisional_result_id),
    CONSTRAINT uk_provisional_result_entry UNIQUE (session_id, entry_id),
    CONSTRAINT fk_provisional_result_session FOREIGN KEY (session_id)
        REFERENCES race_simulation_sessions(session_id) ON DELETE CASCADE,
    CONSTRAINT fk_provisional_result_race FOREIGN KEY (race_id) REFERENCES races(race_id),
    CONSTRAINT fk_provisional_result_entry FOREIGN KEY (entry_id) REFERENCES race_entries(entry_id),
    INDEX idx_provisional_result_race (race_id, finish_position)
) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

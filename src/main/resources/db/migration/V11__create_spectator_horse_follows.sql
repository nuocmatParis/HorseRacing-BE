CREATE TABLE IF NOT EXISTS spectator_horse_follows (
    follow_id CHAR(36) NOT NULL,
    spectator_id CHAR(36) NOT NULL,
    horse_id CHAR(36) NOT NULL,
    followed_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (follow_id),
    CONSTRAINT uk_spectator_horse_follow UNIQUE (spectator_id, horse_id),
    CONSTRAINT fk_spectator_horse_follow_spectator FOREIGN KEY (spectator_id)
        REFERENCES spectators (spectator_id),
    CONSTRAINT fk_spectator_horse_follow_horse FOREIGN KEY (horse_id)
        REFERENCES horses (horse_id)
);

CREATE INDEX idx_spectator_horse_follow_spectator_time
    ON spectator_horse_follows (spectator_id, followed_at);

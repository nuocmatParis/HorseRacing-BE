DROP TABLE IF EXISTS notification_preferences;
DROP TABLE IF EXISTS notification_deliveries;
DROP TABLE IF EXISTS notifications;
DROP TABLE IF EXISTS notification_events;

CREATE TABLE notification_events (
    event_id CHAR(36) NOT NULL,
    event_type VARCHAR(60) NOT NULL,
    aggregate_type VARCHAR(50) NOT NULL,
    aggregate_id CHAR(36) NOT NULL,
    deduplication_key VARCHAR(200) NOT NULL,
    payload_json JSON NOT NULL,
    status VARCHAR(20) NOT NULL,
    attempt_count INT NOT NULL DEFAULT 0,
    next_retry_at DATETIME NULL,
    last_error TEXT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    processed_at DATETIME NULL,
    PRIMARY KEY (event_id),
    CONSTRAINT uk_notification_event_deduplication UNIQUE (deduplication_key),
    INDEX idx_notification_event_processing (status, next_retry_at, created_at)
);

CREATE TABLE notifications (
    notification_id CHAR(36) NOT NULL,
    event_id CHAR(36) NOT NULL,
    recipient_user_id CHAR(36) NOT NULL,
    title VARCHAR(200) NOT NULL,
    content TEXT NOT NULL,
    related_type VARCHAR(50) NULL,
    related_id CHAR(36) NULL,
    visible_in_app BOOLEAN NOT NULL DEFAULT TRUE,
    show_toast BOOLEAN NOT NULL DEFAULT TRUE,
    is_read BOOLEAN NOT NULL DEFAULT FALSE,
    read_at DATETIME NULL,
    archived_at DATETIME NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (notification_id),
    CONSTRAINT fk_notification_event FOREIGN KEY (event_id) REFERENCES notification_events(event_id),
    CONSTRAINT fk_notification_recipient FOREIGN KEY (recipient_user_id) REFERENCES users(user_id),
    CONSTRAINT uk_notification_event_recipient UNIQUE (event_id, recipient_user_id),
    INDEX idx_notification_recipient_list (recipient_user_id, archived_at, created_at),
    INDEX idx_notification_recipient_unread (recipient_user_id, is_read, archived_at)
);

CREATE TABLE notification_deliveries (
    delivery_id CHAR(36) NOT NULL,
    notification_id CHAR(36) NOT NULL,
    channel VARCHAR(20) NOT NULL,
    status VARCHAR(20) NOT NULL,
    attempt_count INT NOT NULL DEFAULT 0,
    next_retry_at DATETIME NULL,
    sent_at DATETIME NULL,
    last_error TEXT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (delivery_id),
    CONSTRAINT fk_delivery_notification FOREIGN KEY (notification_id) REFERENCES notifications(notification_id),
    CONSTRAINT uk_notification_delivery_channel UNIQUE (notification_id, channel),
    INDEX idx_notification_delivery_processing (status, next_retry_at, created_at)
);

CREATE TABLE notification_preferences (
    preference_id CHAR(36) NOT NULL,
    user_id CHAR(36) NOT NULL,
    event_type VARCHAR(60) NOT NULL,
    in_app_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    email_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (preference_id),
    CONSTRAINT fk_notification_preference_user FOREIGN KEY (user_id) REFERENCES users(user_id),
    CONSTRAINT uk_notification_preference_user_type UNIQUE (user_id, event_type)
);

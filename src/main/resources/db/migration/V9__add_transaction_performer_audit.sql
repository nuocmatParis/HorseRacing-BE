ALTER TABLE wallet_transactions
    ADD COLUMN performed_by_user_id CHAR(36) NULL AFTER note;

ALTER TABLE wallet_transactions
    ADD CONSTRAINT fk_wallet_transactions_performed_by
        FOREIGN KEY (performed_by_user_id) REFERENCES users(user_id)
        ON DELETE SET NULL;

CREATE INDEX idx_wallet_transactions_performed_by
    ON wallet_transactions (performed_by_user_id);

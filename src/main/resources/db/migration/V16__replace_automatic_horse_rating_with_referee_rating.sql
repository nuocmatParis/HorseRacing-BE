ALTER TABLE race_results
    ADD COLUMN rating_change INT NULL AFTER status,
    ADD COLUMN rating_adjustment_reason TEXT NULL AFTER rating_change;

ALTER TABLE horse_rating_histories
    DROP COLUMN base_change,
    DROP COLUMN opponent_strength_bonus,
    DROP COLUMN finish_performance_bonus,
    DROP COLUMN field_size_bonus,
    DROP COLUMN underperformance_penalty,
    ADD COLUMN adjustment_reason TEXT NULL AFTER final_change;

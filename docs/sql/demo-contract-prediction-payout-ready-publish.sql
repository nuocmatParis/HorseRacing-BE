-- ============================================================================
-- HRTMS - CHUYỂN DEMO FINAL RACE SANG TRẠNG THÁI SẴN SÀNG PUBLISH
-- MySQL 8+
-- ============================================================================
-- Chạy file này SAU KHI:
--   1. Đã chạy docs/sql/demo-flows-05-and-07-to-11.sql.
--   2. Khuyến nghị: đăng nhập fspec1 và tạo prediction TOP3 cho Final Race.
--
-- Mục tiêu:
--   - Giữ prediction do người dùng tạo qua FE/API.
--   - Nếu người dùng chưa tạo, tự thêm một prediction TOP3 chính xác để fallback.
--   - Tạo 8 RaceResult hợp lệ và RaceReport SIGNED.
--   - Đặt Race ở FINISHED để Admin chỉ cần bấm Publish.
--   - Khi Admin publish, BE thật sẽ tự:
--       + chấm điểm prediction;
--       + áp dụng horse rating;
--       + trả thưởng Top 3;
--       + release 70% tiền thuê còn lại cho 8 Jockey.
--
-- Chỉ dùng cho database LOCAL/TEST.
-- Nếu đã publish trước đó, nên chạy lại file seed gốc trước khi chạy file này.
-- ============================================================================

USE swp391_project_hrtms;

SET @OLD_FOREIGN_KEY_CHECKS = @@FOREIGN_KEY_CHECKS;
SET @OLD_SQL_SAFE_UPDATES = @@SQL_SAFE_UPDATES;
SET FOREIGN_KEY_CHECKS = 0;
SET SQL_SAFE_UPDATES = 0;

SET @demo_now = NOW();
SET @full_tournament_id = 'e7000000-0000-0000-0000-000000000001';
SET @full_round_id = 'e7010000-0000-0000-0000-000000000001';
SET @full_race_id = 'e7020000-0000-0000-0000-000000000001';
SET @spectator_id = 'e7500000-0000-0000-0000-000000000005';
SET @race_referee_id = 'e7500000-0000-0000-0000-000000000001';
SET @head_referee_id = 'e7500000-0000-0000-0000-000000000002';
SET @race_referee_user_id = 'e7400000-0000-0000-0000-000000000001';

-- ============================================================================
-- 1. KIỂM TRA DỮ LIỆU GỐC
-- ============================================================================

SELECT
    CASE
        WHEN COUNT(*) = 1 THEN 'OK - Final Race demo tồn tại'
        ELSE 'STOP - Hãy chạy docs/sql/demo-flows-05-and-07-to-11.sql trước'
    END AS seed_precondition
FROM races
WHERE race_id = @full_race_id;

-- ============================================================================
-- 2. DỌN OUTPUT CỦA LẦN CHUẨN BỊ TRƯỚC
-- ============================================================================

DELETE FROM appeal_evidences
WHERE appeal_id IN (
    SELECT appeal_id
    FROM appeals
    WHERE entry_id IN (
        SELECT entry_id FROM race_entries WHERE race_id = @full_race_id
    )
);

DELETE FROM appeals
WHERE entry_id IN (
    SELECT entry_id FROM race_entries WHERE race_id = @full_race_id
);

DELETE FROM violations
WHERE entry_id IN (
    SELECT entry_id FROM race_entries WHERE race_id = @full_race_id
);

DELETE FROM horse_rating_histories
WHERE race_id = @full_race_id;

DELETE FROM wallet_transactions
WHERE race_result_id IN (
        SELECT result_id FROM race_results WHERE race_id = @full_race_id
    )
   OR (
        contract_id IN (
            SELECT contract_id
            FROM jockey_horse_contracts
            WHERE tournament_id = @full_tournament_id
        )
        AND type IN (
            'JOCKEY_HIRING_FINAL_PAYOUT',
            'JOCKEY_HIRING_FINAL_INCOME',
            'PRIZE_OWNER_SHARE',
            'PRIZE_JOCKEY_SHARE'
        )
    );

DELETE FROM race_reports
WHERE race_id = @full_race_id;

DELETE FROM race_results
WHERE race_id = @full_race_id;

-- ============================================================================
-- 3. RESET SNAPSHOT TÀI CHÍNH VÀ RATING TRƯỚC PUBLISH
-- ============================================================================

UPDATE wallets
SET balance = 10000000,
    status = 'ACTIVE',
    updated_at = @demo_now
WHERE wallet_purpose = 'USER_MAIN'
  AND user_id IN (
      'e7100000-0000-0000-0000-000000000001',
      'e7100000-0000-0000-0000-000000000002',
      'e7100000-0000-0000-0000-000000000003',
      'e7100000-0000-0000-0000-000000000004',
      'e7100000-0000-0000-0000-000000000005'
  );

UPDATE wallets
SET balance = 600000,
    status = 'ACTIVE',
    updated_at = @demo_now
WHERE wallet_purpose = 'USER_MAIN'
  AND user_id IN (
      'e7200000-0000-0000-0000-000000000001',
      'e7200000-0000-0000-0000-000000000002',
      'e7200000-0000-0000-0000-000000000003',
      'e7200000-0000-0000-0000-000000000004',
      'e7200000-0000-0000-0000-000000000005',
      'e7200000-0000-0000-0000-000000000006',
      'e7200000-0000-0000-0000-000000000007',
      'e7200000-0000-0000-0000-000000000008'
  );

UPDATE wallets
SET balance = 100000000,
    status = 'ACTIVE',
    updated_at = @demo_now
WHERE owner_type = 'SYSTEM'
  AND wallet_purpose = 'SYSTEM_PRIZE_POOL';

UPDATE wallets
SET balance = 50000000,
    status = 'ACTIVE',
    updated_at = @demo_now
WHERE owner_type = 'SYSTEM'
  AND wallet_purpose = 'SYSTEM_ESCROW';

UPDATE jockey_horse_contracts
SET advance_paid_amount = 600000,
    escrow_amount = 1400000,
    payment_status = 'PAID',
    escrow_status = 'PARTIALLY_RELEASED',
    advance_payout_status = 'PAID',
    final_payout_status = 'NOT_RELEASED',
    final_payout_at = NULL,
    status = 'APPROVED'
WHERE tournament_id = @full_tournament_id;

UPDATE horses
SET current_rating = CASE horse_id
        WHEN 'e7310000-0000-0000-0000-000000000001' THEN 60
        WHEN 'e7310000-0000-0000-0000-000000000002' THEN 62
        WHEN 'e7310000-0000-0000-0000-000000000003' THEN 64
        WHEN 'e7310000-0000-0000-0000-000000000004' THEN 66
        WHEN 'e7310000-0000-0000-0000-000000000005' THEN 68
        WHEN 'e7310000-0000-0000-0000-000000000006' THEN 70
        WHEN 'e7310000-0000-0000-0000-000000000007' THEN 72
        WHEN 'e7310000-0000-0000-0000-000000000008' THEN 74
        ELSE current_rating
    END,
    race_class = 'CLASS_3',
    rating_updated_at = @demo_now
WHERE horse_id IN (
    'e7310000-0000-0000-0000-000000000001',
    'e7310000-0000-0000-0000-000000000002',
    'e7310000-0000-0000-0000-000000000003',
    'e7310000-0000-0000-0000-000000000004',
    'e7310000-0000-0000-0000-000000000005',
    'e7310000-0000-0000-0000-000000000006',
    'e7310000-0000-0000-0000-000000000007',
    'e7310000-0000-0000-0000-000000000008'
);

-- ============================================================================
-- 4. GIỮ PREDICTION FE/API; TẠO FALLBACK NẾU CHƯA CÓ
-- ============================================================================

INSERT INTO predictions (
    prediction_id, spectator_id, race_id, prediction_type, prediction_time,
    status, reward_points, scored_at, voided_at, void_reason
)
SELECT
    'e7620000-0000-0000-0000-000000000001',
    @spectator_id,
    @full_race_id,
    'TOP3',
    DATE_SUB(@demo_now, INTERVAL 1 HOUR),
    'PENDING',
    NULL,
    NULL,
    NULL,
    NULL
WHERE NOT EXISTS (
    SELECT 1
    FROM predictions
    WHERE spectator_id = @spectator_id
      AND race_id = @full_race_id
);

INSERT INTO prediction_detail (
    prediction_detail_id, prediction_id, entry_id,
    predicted_rank, status, awarded_points
)
SELECT
    'e7630000-0000-0000-0000-000000000001',
    'e7620000-0000-0000-0000-000000000001',
    'e7350000-0000-0000-0000-000000000001',
    1,
    'UNSCORED',
    NULL
WHERE EXISTS (
    SELECT 1 FROM predictions
    WHERE prediction_id = 'e7620000-0000-0000-0000-000000000001'
)
  AND NOT EXISTS (
    SELECT 1 FROM prediction_detail
    WHERE prediction_detail_id = 'e7630000-0000-0000-0000-000000000001'
);

INSERT INTO prediction_detail (
    prediction_detail_id, prediction_id, entry_id,
    predicted_rank, status, awarded_points
)
SELECT
    'e7630000-0000-0000-0000-000000000002',
    'e7620000-0000-0000-0000-000000000001',
    'e7350000-0000-0000-0000-000000000002',
    2,
    'UNSCORED',
    NULL
WHERE EXISTS (
    SELECT 1 FROM predictions
    WHERE prediction_id = 'e7620000-0000-0000-0000-000000000001'
)
  AND NOT EXISTS (
    SELECT 1 FROM prediction_detail
    WHERE prediction_detail_id = 'e7630000-0000-0000-0000-000000000002'
);

INSERT INTO prediction_detail (
    prediction_detail_id, prediction_id, entry_id,
    predicted_rank, status, awarded_points
)
SELECT
    'e7630000-0000-0000-0000-000000000003',
    'e7620000-0000-0000-0000-000000000001',
    'e7350000-0000-0000-0000-000000000003',
    3,
    'UNSCORED',
    NULL
WHERE EXISTS (
    SELECT 1 FROM predictions
    WHERE prediction_id = 'e7620000-0000-0000-0000-000000000001'
)
  AND NOT EXISTS (
    SELECT 1 FROM prediction_detail
    WHERE prediction_detail_id = 'e7630000-0000-0000-0000-000000000003'
);

UPDATE predictions
SET status = 'PENDING',
    reward_points = NULL,
    scored_at = NULL,
    voided_at = NULL,
    void_reason = NULL
WHERE race_id = @full_race_id;

UPDATE prediction_detail
SET status = 'UNSCORED',
    awarded_points = NULL
WHERE prediction_id IN (
    SELECT prediction_id
    FROM predictions
    WHERE race_id = @full_race_id
);

UPDATE spectators
SET total_points = 0
WHERE spectator_id = @spectator_id;

-- ============================================================================
-- 5. ĐƯA FINAL RACE ĐẾN TRẠNG THÁI FINISHED
-- ============================================================================

UPDATE race_entries
SET status = 'FINISHED'
WHERE race_id = @full_race_id;

UPDATE races
SET start_time = DATE_SUB(@demo_now, INTERVAL 30 MINUTE),
    end_time = DATE_ADD(@demo_now, INTERVAL 4 HOUR),
    status = 'FINISHED',
    started_at = DATE_SUB(@demo_now, INTERVAL 20 MINUTE),
    finished_at = @demo_now,
    prediction_open_at = DATE_SUB(@demo_now, INTERVAL 6 HOUR),
    prediction_close_at = DATE_SUB(@demo_now, INTERVAL 5 MINUTE),
    inspection_finalized_at = DATE_SUB(@demo_now, INTERVAL 30 MINUTE)
WHERE race_id = @full_race_id;

UPDATE rounds
SET start_date = DATE_SUB(@demo_now, INTERVAL 30 MINUTE),
    end_date = DATE_ADD(@demo_now, INTERVAL 4 HOUR),
    status = 'FINISHED',
    transition_status = 'NOT_READY'
WHERE round_id = @full_round_id;

UPDATE tournaments
SET start_date = CURDATE(),
    end_date = DATE(DATE_ADD(@demo_now, INTERVAL 4 HOUR)),
    competition_start_at = DATE_SUB(@demo_now, INTERVAL 30 MINUTE),
    status = 'ONGOING',
    phase = 'RACING',
    finished_at = NULL,
    current_round_name = 'Chung kết'
WHERE tournament_id = @full_tournament_id;

-- ============================================================================
-- 6. OFFICIAL RESULT + RATING CHANGE HỢP LỆ
-- ============================================================================

INSERT INTO race_results (
    result_id, race_id, entry_id, finish_time, finish_position,
    prize_money, owner_prize_amount, jockey_prize_amount,
    prize_status, is_prize_paid, prize_paid_at,
    status, rating_change, rating_adjustment_reason,
    recorded_by, recorded_at, updated_at
) VALUES
    ('e7600000-0000-0000-0000-000000000001', @full_race_id, 'e7350000-0000-0000-0000-000000000001', 95.10, 1, 0, 0, 0, 'PendingPayout', 0, NULL, 'FINISHED',  6, 'Hạng 1 - mức tối thiểu hợp lệ', @race_referee_user_id, @demo_now, @demo_now),
    ('e7600000-0000-0000-0000-000000000002', @full_race_id, 'e7350000-0000-0000-0000-000000000002', 95.55, 2, 0, 0, 0, 'PendingPayout', 0, NULL, 'FINISHED',  2, 'Hạng 2 - không vượt điểm hạng 1', @race_referee_user_id, @demo_now, @demo_now),
    ('e7600000-0000-0000-0000-000000000003', @full_race_id, 'e7350000-0000-0000-0000-000000000003', 96.00, 3, 0, 0, 0, 'PendingPayout', 0, NULL, 'FINISHED',  1, 'Hạng 3 - không vượt điểm hạng 2', @race_referee_user_id, @demo_now, @demo_now),
    ('e7600000-0000-0000-0000-000000000004', @full_race_id, 'e7350000-0000-0000-0000-000000000004', 96.40, 4, 0, 0, 0, 'NotEligible',  0, NULL, 'FINISHED',  0, 'Hạng 4', @race_referee_user_id, @demo_now, @demo_now),
    ('e7600000-0000-0000-0000-000000000005', @full_race_id, 'e7350000-0000-0000-0000-000000000005', 96.85, 5, 0, 0, 0, 'NotEligible',  0, NULL, 'FINISHED',  0, 'Hạng 5', @race_referee_user_id, @demo_now, @demo_now),
    ('e7600000-0000-0000-0000-000000000006', @full_race_id, 'e7350000-0000-0000-0000-000000000006', 97.20, 6, 0, 0, 0, 'NotEligible',  0, NULL, 'FINISHED', -1, 'Hạng 6', @race_referee_user_id, @demo_now, @demo_now),
    ('e7600000-0000-0000-0000-000000000007', @full_race_id, 'e7350000-0000-0000-0000-000000000007', 97.65, 7, 0, 0, 0, 'NotEligible',  0, NULL, 'FINISHED', -2, 'Hạng 7', @race_referee_user_id, @demo_now, @demo_now),
    ('e7600000-0000-0000-0000-000000000008', @full_race_id, 'e7350000-0000-0000-0000-000000000008', 98.10, 8, 0, 0, 0, 'NotEligible',  0, NULL, 'FINISHED', -3, 'Hạng 8', @race_referee_user_id, @demo_now, @demo_now);

-- ============================================================================
-- 7. REPORT ĐÃ ĐƯỢC HEAD REFEREE KÝ, CHỜ ADMIN PUBLISH
-- ============================================================================

INSERT INTO race_reports (
    report_id, race_id, referee_id,
    summary, appeal_note, status,
    submitted_at, submitted_by,
    returned_at, returned_by, return_reason,
    signed_by, signed_at,
    published_by, published_at, created_at
) VALUES (
    'e7610000-0000-0000-0000-000000000001',
    @full_race_id,
    @race_referee_id,
    'Final Race đã hoàn tất. Kết quả và ratingChange đã được Head Referee xác nhận.',
    'Không có khiếu nại PENDING.',
    'SIGNED',
    DATE_SUB(@demo_now, INTERVAL 10 MINUTE),
    @race_referee_id,
    NULL,
    NULL,
    NULL,
    @head_referee_id,
    DATE_SUB(@demo_now, INTERVAL 5 MINUTE),
    NULL,
    NULL,
    DATE_SUB(@demo_now, INTERVAL 15 MINUTE)
);

SET FOREIGN_KEY_CHECKS = @OLD_FOREIGN_KEY_CHECKS;
SET SQL_SAFE_UPDATES = @OLD_SQL_SAFE_UPDATES;

-- ============================================================================
-- 8. KIỂM TRA TRƯỚC KHI ADMIN PUBLISH
-- ============================================================================

SELECT
    r.race_id,
    r.name,
    r.status AS race_status,
    rr.status AS report_status,
    rd.status AS round_status,
    rd.is_final
FROM races r
JOIN rounds rd ON rd.round_id = r.round_id
JOIN race_reports rr ON rr.race_id = r.race_id
WHERE r.race_id = @full_race_id;

SELECT
    p.prediction_id,
    p.status,
    p.prediction_type,
    COUNT(pd.prediction_detail_id) AS selected_entries
FROM predictions p
LEFT JOIN prediction_detail pd ON pd.prediction_id = p.prediction_id
WHERE p.race_id = @full_race_id
GROUP BY p.prediction_id, p.status, p.prediction_type;

SELECT
    finish_position,
    status,
    rating_change,
    prize_status,
    is_prize_paid
FROM race_results
WHERE race_id = @full_race_id
ORDER BY finish_position;

SELECT
    wallet_purpose,
    balance
FROM wallets
WHERE owner_type = 'SYSTEM'
  AND wallet_purpose IN ('SYSTEM_ESCROW', 'SYSTEM_PRIZE_POOL')
ORDER BY wallet_purpose;

SELECT
    'READY' AS demo_state,
    'POST /api/admin/races/e7020000-0000-0000-0000-000000000001/report/publish' AS next_action;

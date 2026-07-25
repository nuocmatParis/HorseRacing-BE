-- ============================================================================
-- HRTMS DEMO - MỞ KHÓA START CHO FINAL RACE FLOW 07 -> 11
-- ============================================================================
-- Chạy file này SAU KHI:
--   1. Spectator fspec1 đã gửi prediction.
--   2. Vet fvet1 đã khám ngựa.
--   3. Medical fmed1 đã khám Jockey.
--
-- File chỉ đổi timeline của đúng Final Race demo.
-- Sau khi chạy:
--   - Race Referee frace_ref có thể Start ngay.
--   - Start còn hợp lệ trong 5 giờ.
--   - end_time = NOW()+5 giờ nên Owner/Jockey còn gửi Appeal được sau Finish.
-- ============================================================================

USE SWP391_Project_HRTMS;

SET @unlock_now = NOW();
SET @full_tournament_id = 'e7000000-0000-0000-0000-000000000001';
SET @full_round_id = 'e7010000-0000-0000-0000-000000000001';
SET @full_race_id = 'e7020000-0000-0000-0000-000000000001';
SET @new_start = DATE_SUB(@unlock_now, INTERVAL 1 MINUTE);
SET @new_end = DATE_ADD(@unlock_now, INTERVAL 5 HOUR);

UPDATE races
SET start_time = @new_start,
    end_time = @new_end,
    prediction_close_at = @unlock_now
WHERE race_id = @full_race_id
  AND status = 'SCHEDULED'
  AND started_at IS NULL;

UPDATE rounds
SET start_date = @new_start,
    end_date = @new_end
WHERE round_id = @full_round_id;

UPDATE tournaments
SET competition_start_at = @new_start,
    end_date = DATE(@new_end),
    start_late_tolerance_minutes = 300
WHERE tournament_id = @full_tournament_id;

SELECT
    race_id,
    name,
    status,
    start_time,
    DATE_ADD(start_time, INTERVAL 300 MINUTE) AS latest_start,
    end_time,
    prediction_close_at
FROM races
WHERE race_id = @full_race_id;

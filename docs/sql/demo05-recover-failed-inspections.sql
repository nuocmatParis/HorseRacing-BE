-- DEMO ONLY
-- Phục hồi riêng DEMO 05 khi một hoặc nhiều entry đã rớt inspection.
-- Script không hạ min_entries và không đụng tới dữ liệu của các DEMO khác.
-- Sau khi chạy, Vet và Medical phải khám lại cả hai phiếu của các entry vừa reset.

SET @demo05_tournament_id = '10000000-0000-0000-0000-000000000005';
SET @demo05_round_id = '20000000-0000-0000-0000-000000000051';
SET @demo05_race_id = '30000000-0000-0000-0000-000000000501';

-- Mở lại đúng cửa sổ demo 3 giờ:
-- inspection mở từ NOW() đến NOW()+180 phút, race theo lịch tại NOW()+210 phút.
-- start_early_tolerance=210 cho phép referee Start ngay sau khi đủ 8/8 PASS.
SET @demo05_new_start = DATE_ADD(NOW(), INTERVAL 210 MINUTE);
SET @demo05_new_end = DATE_ADD(@demo05_new_start, INTERVAL 180 MINUTE);

START TRANSACTION;

-- Xóa hai phiếu của riêng các entry đã bị SCRATCHED để hai bộ phận có thể khám lại.
-- Xóa cả hai vì phiếu còn lại có thể đã PASS hoặc chưa từng được tạo.
DELETE hi
FROM horse_inspections hi
JOIN race_entries re ON re.entry_id = hi.entry_id
WHERE re.race_id = @demo05_race_id
  AND re.status = 'SCRATCHED';

DELETE ji
FROM jockey_inspections ji
JOIN race_entries re ON re.entry_id = ji.entry_id
WHERE re.race_id = @demo05_race_id
  AND re.status = 'SCRATCHED';

UPDATE race_entries
SET status = 'CONFIRMED',
    scratched_reason = NULL
WHERE race_id = @demo05_race_id
  AND status = 'SCRATCHED';

UPDATE races
SET start_time = @demo05_new_start,
    end_time = @demo05_new_end,
    status = 'SCHEDULED',
    started_at = NULL,
    started_by = NULL,
    finished_at = NULL,
    inspection_finalized_at = NULL,
    prediction_open_at = DATE_SUB(NOW(), INTERVAL 1 DAY),
    prediction_close_at = DATE_SUB(@demo05_new_start, INTERVAL 5 MINUTE)
WHERE race_id = @demo05_race_id;

UPDATE rounds
SET start_date = @demo05_new_start,
    end_date = @demo05_new_end,
    status = 'SCHEDULED'
WHERE round_id = @demo05_round_id;

UPDATE tournaments
SET status = 'ONGOING',
    phase = 'RACING',
    start_early_tolerance_minutes = 210,
    start_late_tolerance_minutes = 180
WHERE tournament_id = @demo05_tournament_id;

COMMIT;

-- Phải trả về 8 entry. Các entry vừa reset sẽ chưa có hai inspection cho tới
-- khi vet1 và medical1 khám lại; năm entry PASS cũ vẫn được giữ nguyên.
SELECT re.entry_id,
       re.lane_number,
       re.status AS entry_status,
       hi.result AS horse_result,
       hi.status AS horse_inspection_status,
       ji.result AS jockey_result,
       ji.status AS jockey_inspection_status
FROM race_entries re
LEFT JOIN horse_inspections hi ON hi.entry_id = re.entry_id
LEFT JOIN jockey_inspections ji ON ji.entry_id = re.entry_id
WHERE re.race_id = @demo05_race_id
ORDER BY re.lane_number;


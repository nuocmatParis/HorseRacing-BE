-- ============================================================================
-- HRTMS DEMO - CHUYỂN GIẢI 4 NGỰA TỪ MATCHING SANG DỰ ĐOÁN + KIỂM TRA
-- MySQL 8+
-- ============================================================================
-- Chạy file này SAU KHI đã hoàn tất đúng 4 hợp đồng trên FE:
--   Owner invite -> Jockey accept -> Owner thanh toán phí thuê
--   -> Owner thanh toán phí hợp đồng -> Contract APPROVED.
--
-- Dữ liệu nền phải được tạo từ:
--   docs/sql/demo-flows-05-and-07-to-11.sql
--
-- File này KHÔNG giả kết quả kiểm tra và KHÔNG tạo prediction.
-- Nó chỉ:
--   1. Kiểm tra giải có đúng 4 contract APPROVED, 4 Owner, 4 Horse, 4 Jockey.
--   2. Tạo 1 Final Round và 1 Final Race có 4 entry.
--   3. Phân lane 1..4, Race Referee, Head Referee, Vet và Medical Staff.
--   4. Mở đồng thời cửa sổ dự đoán và kiểm tra sức khỏe.
--
-- Chỉ chạy MỘT LẦN. Nếu Round đã tồn tại, guard sẽ chặn để tránh xóa dữ liệu
-- prediction/inspection mà người dùng đã nhập.
-- ============================================================================

USE SWP391_Project_HRTMS;
SET NAMES utf8mb4;

SET @now = NOW();
SET @tournament_id = 'e5000000-0000-0000-0000-000000000001';
SET @round_id      = 'e5010000-0000-0000-0000-000000000001';
SET @race_id       = 'e5020000-0000-0000-0000-000000000001';

SET @admin_user_id  = 'e0000000-0000-0000-0000-000000000001';
SET @race_referee_id = 'e7500000-0000-0000-0000-000000000001';
SET @head_referee_id = 'e7500000-0000-0000-0000-000000000002';
SET @vet_id          = 'e7500000-0000-0000-0000-000000000003';
SET @med_staff_id    = 'e7500000-0000-0000-0000-000000000004';

-- Race còn 5 giờ nữa mới chạy.
-- Prediction mở từ 1 giờ trước và đóng 5 phút trước giờ chạy.
-- Inspection được nới từ T-600 đến T+600 phút riêng cho giải demo.
SET @race_start = DATE_ADD(@now, INTERVAL 5 HOUR);
SET @race_end = DATE_ADD(@race_start, INTERVAL 5 HOUR);
SET @prediction_open = DATE_SUB(@now, INTERVAL 1 HOUR);
SET @prediction_close = DATE_SUB(@race_start, INTERVAL 5 MINUTE);

-- ============================================================================
-- GUARD: script phải dừng nếu dữ liệu Matching chưa hoàn tất hoặc đã tạo Round.
-- ============================================================================

DROP TEMPORARY TABLE IF EXISTS demo_matching_open_guard;

CREATE TEMPORARY TABLE demo_matching_open_guard (
    tournament_count INT NOT NULL CHECK (tournament_count = 1),
    approved_contract_count INT NOT NULL CHECK (approved_contract_count = 4),
    distinct_owner_count INT NOT NULL CHECK (distinct_owner_count = 4),
    distinct_horse_count INT NOT NULL CHECK (distinct_horse_count = 4),
    distinct_jockey_count INT NOT NULL CHECK (distinct_jockey_count = 4),
    prerequisite_staff_count INT NOT NULL CHECK (prerequisite_staff_count = 4),
    existing_round_count INT NOT NULL CHECK (existing_round_count = 0)
);

INSERT INTO demo_matching_open_guard (
    tournament_count,
    approved_contract_count,
    distinct_owner_count,
    distinct_horse_count,
    distinct_jockey_count,
    prerequisite_staff_count,
    existing_round_count
)
SELECT
    (SELECT COUNT(*)
     FROM tournaments
     WHERE tournament_id = @tournament_id),
    (SELECT COUNT(*)
     FROM jockey_horse_contracts
     WHERE tournament_id = @tournament_id
       AND status = 'APPROVED'),
    (SELECT COUNT(DISTINCT owner_id)
     FROM jockey_horse_contracts
     WHERE tournament_id = @tournament_id
       AND status = 'APPROVED'),
    (SELECT COUNT(DISTINCT horse_id)
     FROM jockey_horse_contracts
     WHERE tournament_id = @tournament_id
       AND status = 'APPROVED'),
    (SELECT COUNT(DISTINCT jockey_id)
     FROM jockey_horse_contracts
     WHERE tournament_id = @tournament_id
       AND status = 'APPROVED'),
    ((SELECT COUNT(*) FROM referees
      WHERE referee_id IN (@race_referee_id, @head_referee_id))
     + (SELECT COUNT(*) FROM veterinarians WHERE vet_id = @vet_id)
     + (SELECT COUNT(*) FROM medical_staffs WHERE med_staff_id = @med_staff_id)),
    (SELECT COUNT(*)
     FROM rounds
     WHERE tournament_id = @tournament_id);

DROP TEMPORARY TABLE demo_matching_open_guard;

START TRANSACTION;

-- Mở phase thi đấu và nới cửa sổ inspection cho demo.
UPDATE tournaments
SET phase = 'RACING',
    status = 'ONGOING',
    start_date = DATE(@now),
    end_date = DATE(@race_end),
    published_at = COALESCE(published_at, @now),
    competition_start_at = @race_start,
    current_round_name = 'Chung kết demo 4 ngựa',
    prediction_open_minutes_before = 360,
    prediction_close_minutes_before = 5,
    inspection_open_minutes_before = 600,
    inspection_close_minutes_before = -600,
    start_late_tolerance_minutes = 300,
    default_race_operational_minutes = 300,
    race_day_start_time = '00:00:00',
    race_day_end_time = '23:59:59',
    rating_policy_locked_at = COALESCE(rating_policy_locked_at, @now)
WHERE tournament_id = @tournament_id;

INSERT INTO rounds (
    round_id,
    round_name,
    sequence_order,
    is_final,
    prediction_type,
    advancement_rule,
    start_date,
    end_date,
    description,
    max_races,
    max_entries,
    min_entries,
    status,
    head_referee_id,
    head_referee_assigned_at,
    expected_entries,
    qualifiers_per_race,
    advanced_at,
    transition_status,
    created_at,
    tournament_id,
    created_by
) VALUES (
    @round_id,
    'Chung kết demo 4 ngựa',
    1,
    1,
    'TOP3',
    'Final Race demo; không có vòng tiếp theo',
    @race_start,
    @race_end,
    'Được tạo sau khi hoàn tất 4 hợp đồng của Flow Matching.',
    1,
    4,
    4,
    'SCHEDULED',
    @head_referee_id,
    @now,
    4,
    4,
    NULL,
    'NOT_READY',
    @now,
    @tournament_id,
    @admin_user_id
);

INSERT INTO races (
    race_id,
    name,
    start_time,
    end_time,
    track_condition,
    distance,
    sequence_order,
    status,
    started_at,
    finished_at,
    schedule_published_at,
    prediction_open_at,
    prediction_close_at,
    ai_prediction_publication_status,
    round_id,
    created_by,
    started_by,
    inspection_finalized_at
) VALUES (
    @race_id,
    'DEMO MATCHING 4 - Final Race',
    @race_start,
    @race_end,
    'TURF',
    'MILE_1600M',
    1,
    'SCHEDULED',
    NULL,
    NULL,
    @now,
    @prediction_open,
    @prediction_close,
    'DRAFT',
    @round_id,
    @admin_user_id,
    NULL,
    NULL
);

-- Contract được tạo qua API nên contract_id là UUID động.
-- ROW_NUMBER() giúp phân lane 1..4 ổn định theo thời điểm gửi lời mời.
INSERT INTO race_entries (
    entry_id,
    race_id,
    contract_id,
    lane_number,
    status,
    assigned_by,
    assigned_at,
    created_at
)
SELECT
    UUID(),
    @race_id,
    ranked.contract_id,
    ranked.lane_number,
    'CONFIRMED',
    @admin_user_id,
    @now,
    @now
FROM (
    SELECT
        contract_id,
        ROW_NUMBER() OVER (
            ORDER BY requested_at ASC, contract_id ASC
        ) AS lane_number
    FROM jockey_horse_contracts
    WHERE tournament_id = @tournament_id
      AND status = 'APPROVED'
) ranked;

INSERT INTO race_referees (
    race_referee_id,
    race_id,
    referee_id,
    assigned_by,
    assigned_at
) VALUES (
    'e5060000-0000-0000-0000-000000000001',
    @race_id,
    @race_referee_id,
    @admin_user_id,
    @now
);

INSERT INTO race_inspection_staff_assignments (
    assignment_id,
    race_id,
    vet_id,
    med_staff_id,
    assigned_by,
    assigned_at
) VALUES (
    'e5060000-0000-0000-0000-000000000002',
    @race_id,
    @vet_id,
    @med_staff_id,
    @admin_user_id,
    @now
);

COMMIT;

-- ============================================================================
-- KẾT QUẢ DÙNG ĐỂ DEMO / COPY ID
-- ============================================================================

SELECT
    t.tournament_id,
    t.name AS tournament_name,
    t.phase,
    t.status,
    r.round_id,
    r.round_name,
    race.race_id,
    race.name AS race_name,
    race.status AS race_status,
    race.start_time,
    race.end_time,
    race.prediction_open_at,
    race.prediction_close_at,
    DATE_SUB(race.start_time, INTERVAL t.inspection_open_minutes_before MINUTE)
        AS inspection_open_at,
    DATE_SUB(race.start_time, INTERVAL t.inspection_close_minutes_before MINUTE)
        AS inspection_close_at
FROM tournaments t
JOIN rounds r ON r.tournament_id = t.tournament_id
JOIN races race ON race.round_id = r.round_id
WHERE t.tournament_id = @tournament_id;

SELECT
    re.entry_id,
    re.lane_number,
    re.status AS entry_status,
    h.name AS horse_name,
    owner_user.username AS owner_username,
    jockey_user.username AS jockey_username,
    c.contract_id,
    c.status AS contract_status
FROM race_entries re
JOIN jockey_horse_contracts c ON c.contract_id = re.contract_id
JOIN horses h ON h.horse_id = c.horse_id
JOIN horse_owners ho ON ho.owner_id = c.owner_id
JOIN users owner_user ON owner_user.user_id = ho.user_id
JOIN jockeys j ON j.jockey_id = c.jockey_id
JOIN users jockey_user ON jockey_user.user_id = j.user_id
WHERE re.race_id = @race_id
ORDER BY re.lane_number;

SELECT
    'BƯỚC TIẾP THEO' AS note,
    'Dùng fspec1 dự đoán; fvet1 khám 4 ngựa; fmed1 khám 4 jockey. Sau khi tất cả PASS, chạy demo-matching-4-unlock-start.sql.' AS action;

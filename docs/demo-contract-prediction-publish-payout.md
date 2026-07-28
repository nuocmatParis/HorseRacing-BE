# Demo Contract Matching, Spectator Prediction và Publish/Payout

Tài liệu này dùng cho ba phần:

1. Owner lập hợp đồng với Jockey.
2. Spectator tạo dự đoán Top 3.
3. Admin publish Final Race Report để BE tự chấm điểm, cộng rating, chia thưởng và release 70% tiền thuê Jockey.

## 1. File SQL

Chạy theo đúng thứ tự:

```text
1. docs/sql/demo-flows-05-and-07-to-11.sql
2. Thao tác lập hợp đồng và tạo prediction trên FE/API
3. docs/sql/demo-contract-prediction-payout-ready-publish.sql
4. Admin bấm Publish trên FE hoặc gọi API publish
```

Script thứ nhất tạo hai giải:

- `DEMO FLOW 05 - Jockey Matching`: 4 Owner, 4 ngựa, 4 Jockey, chưa có contract.
- `DEMO FLOW 07-11 - Full Final Race`: 8 entry, Final Round, prediction đang mở và tài chính sẵn sàng.

Script thứ hai là helper một chiều, đưa Final Race sang `FINISHED` và tạo report `SIGNED`.

Nếu cần demo lại từ đầu, chạy lại script thứ nhất.

## 2. Tài khoản

Mật khẩu chung:

```text
12345678
```

| Mục đích | Username |
|---|---|
| Admin | `dmadmin` |
| Owner lập hợp đồng | `mowner1` |
| Jockey nhận lời mời | `mjockey1` |
| Spectator dự đoán | `fspec1` |
| Owner xem tiền thưởng hạng 1 | `fowner1` |
| Jockey xem payout hạng 1 | `fjockey1` |

## 3. Demo lập hợp đồng

Giải:

```text
Tournament ID: e5000000-0000-0000-0000-000000000001
Tên: DEMO FLOW 05 - Jockey Matching
Phase: JOCKEY_MATCHING
```

### Bước 1 — Owner gửi lời mời

Đăng nhập `mowner1`, sau đó gọi:

```http
POST /api/owner/contracts/invite
Authorization: Bearer <OWNER_TOKEN>
Content-Type: application/json
```

```json
{
  "tournamentRegistrationId": "e5310000-0000-0000-0000-000000000001",
  "jockeyTournamentRegistrationId": "e5320000-0000-0000-0000-000000000001",
  "ownerPrizeSharePercent": 80,
  "jockeyPrizeSharePercent": 20,
  "contractNote": "Demo hợp đồng Owner 1 và Jockey 1"
}
```

BE tự lấy:

- Hire fee từ Jockey registration: `2.000.000 VND`.
- Advance percent: `30%`.
- Final percent: `70%`.
- System contract fee từ giải: `100.000 VND`.

Sau bước này:

```text
Contract status = PENDING_JOCKEY
Payment status = UNPAID
```

### Bước 2 — Jockey accept

Đăng nhập `mjockey1`, lấy contract ID từ:

```http
GET /api/jockey/contracts/invitations
```

Accept:

```http
POST /api/jockey/contracts/{contractId}/accept
```

Sau bước này:

```text
Contract status = ACCEPTED
Invoice JOCKEY_HIRING_FEE được tạo
```

### Bước 3 — Owner trả phí thuê

Đăng nhập lại `mowner1`:

```http
POST /api/contracts/{contractId}/pay-hiring-fee
```

Sau bước này:

```text
Contract status = HIRING_PAID
2.000.000 VND được giữ trong SYSTEM_ESCROW
Invoice CONTRACT_CREATION_FEE được tạo
```

### Bước 4 — Owner trả phí hệ thống

```http
POST /api/contracts/{contractId}/pay-contract-fee
```

BE tự kích hoạt hợp đồng, không qua Admin review:

```text
Contract status = APPROVED
30% = 600.000 VND chuyển cho Jockey
70% = 1.400.000 VND tiếp tục nằm trong escrow
100.000 VND phí hợp đồng chuyển vào SYSTEM_REVENUE
```

Kiểm tra:

```sql
SELECT
    contract_id,
    status,
    payment_status,
    escrow_status,
    advance_payout_status,
    final_payout_status,
    advance_paid_amount,
    escrow_amount
FROM jockey_horse_contracts
WHERE tournament_id = 'e5000000-0000-0000-0000-000000000001';
```

## 4. Demo Spectator Prediction

Final Race:

```text
Race ID: e7020000-0000-0000-0000-000000000001
```

Đăng nhập `fspec1` và gửi:

```http
POST /api/spectator/races/e7020000-0000-0000-0000-000000000001/predictions
Authorization: Bearer <SPECTATOR_TOKEN>
Content-Type: application/json
```

```json
{
  "predictionType": "TOP3",
  "entries": [
    {
      "entryId": "e7350000-0000-0000-0000-000000000001",
      "predictedRank": 1
    },
    {
      "entryId": "e7350000-0000-0000-0000-000000000002",
      "predictedRank": 2
    },
    {
      "entryId": "e7350000-0000-0000-0000-000000000003",
      "predictedRank": 3
    }
  ]
}
```

Đây là prediction chính xác theo helper SQL nên kết quả dự kiến:

```text
30 điểm x 3 vị trí = 90 điểm
Perfect bonus = 50 điểm
Tổng = 140 điểm
```

Nếu không thao tác prediction trên FE/API, helper SQL sẽ tự tạo một prediction fallback giống dữ liệu trên.

## 5. Chuyển sang trạng thái chờ Admin Publish

Sau khi đã tạo prediction, chạy:

```text
docs/sql/demo-contract-prediction-payout-ready-publish.sql
```

Kết quả cần thấy:

```text
Race status = FINISHED
Round status = FINISHED
Report status = SIGNED
8 RaceResult
Prediction status = PENDING
SYSTEM_PRIZE_POOL = 100.000.000 VND
SYSTEM_ESCROW = 50.000.000 VND
```

## 6. Admin Publish

Đăng nhập `dmadmin`, sau đó dùng UI công bố Race Report hoặc gọi:

```http
POST /api/admin/races/e7020000-0000-0000-0000-000000000001/report/publish
Authorization: Bearer <ADMIN_TOKEN>
```

API này mới là nơi chạy nghiệp vụ thật:

```text
publishReport()
→ scoreRace()
→ applyManualRatingsForPublish()
→ completeFinalRoundIfPossible()
→ payoutPrizeIfFinal()
→ releaseJockeyFinalPayoutAfterFinalRacePublished()
```

## 7. Kết quả kỳ vọng

### Prediction

```text
Prediction: PENDING → SCORED
Reward points: 140
Spectator fspec1.totalPoints: 140
```

Kiểm tra:

```sql
SELECT
    p.prediction_id,
    p.status,
    p.reward_points,
    p.scored_at,
    s.total_points
FROM predictions p
JOIN spectators s ON s.spectator_id = p.spectator_id
WHERE p.race_id = 'e7020000-0000-0000-0000-000000000001';
```

### Prize payout

Giải có tổng quỹ `20.000.000 VND`:

| Hạng | Tổng thưởng | Owner 80% | Jockey 20% |
|---:|---:|---:|---:|
| 1 | 10.000.000 | 8.000.000 | 2.000.000 |
| 2 | 6.000.000 | 4.800.000 | 1.200.000 |
| 3 | 4.000.000 | 3.200.000 | 800.000 |

Kiểm tra:

```sql
SELECT
    finish_position,
    prize_money,
    owner_prize_amount,
    jockey_prize_amount,
    prize_status,
    is_prize_paid
FROM race_results
WHERE race_id = 'e7020000-0000-0000-0000-000000000001'
ORDER BY finish_position;
```

### Final Jockey payout

Cả 8 contract đều được release:

```text
1.400.000 VND/contract
Tổng release = 11.200.000 VND
Final payout status = RELEASED
Escrow status = RELEASED
Escrow amount = 0
```

Kiểm tra:

```sql
SELECT
    contract_id,
    status,
    escrow_status,
    escrow_amount,
    final_payout_status,
    final_payout_at
FROM jockey_horse_contracts
WHERE tournament_id = 'e7000000-0000-0000-0000-000000000001'
ORDER BY contract_id;
```

### System wallets

```text
SYSTEM_PRIZE_POOL: 100.000.000 → 80.000.000
SYSTEM_ESCROW: 50.000.000 → 38.800.000
```

```sql
SELECT wallet_purpose, balance
FROM wallets
WHERE owner_type = 'SYSTEM'
  AND wallet_purpose IN ('SYSTEM_PRIZE_POOL', 'SYSTEM_ESCROW')
ORDER BY wallet_purpose;
```

### Horse rating

```sql
SELECT
    h.name,
    rr.finish_position,
    rr.rating_change,
    h.current_rating,
    h.race_class
FROM race_results rr
JOIN race_entries re ON re.entry_id = rr.entry_id
JOIN jockey_horse_contracts c ON c.contract_id = re.contract_id
JOIN horses h ON h.horse_id = c.horse_id
WHERE rr.race_id = 'e7020000-0000-0000-0000-000000000001'
ORDER BY rr.finish_position;
```

### Transaction được tạo

```sql
SELECT
    type,
    direction,
    amount,
    contract_id,
    race_result_id,
    status,
    created_at
FROM wallet_transactions
WHERE race_result_id IN (
        SELECT result_id
        FROM race_results
        WHERE race_id = 'e7020000-0000-0000-0000-000000000001'
    )
   OR (
        contract_id IN (
            SELECT contract_id
            FROM jockey_horse_contracts
            WHERE tournament_id = 'e7000000-0000-0000-0000-000000000001'
        )
        AND type IN (
            'JOCKEY_HIRING_FINAL_PAYOUT',
            'JOCKEY_HIRING_FINAL_INCOME'
        )
    )
ORDER BY created_at, transaction_id;
```

## 8. Lưu ý

- Không gọi publish hai lần; lần hai phải bị chặn bởi `RACE_REPORT_ALREADY_PUBLISHED`.
- Không sửa trực tiếp tiền/điểm sau khi helper đã chuẩn bị. Hãy để API publish thực hiện nghiệp vụ.
- Nếu API publish rollback, kiểm tra trước tiên:
  - report phải là `SIGNED`;
  - race phải là `FINISHED`;
  - không có appeal `Pending`;
  - system wallets phải đủ tiền;
  - Final Round chỉ có đúng một race.
- Muốn reset toàn bộ demo, chạy lại `docs/sql/demo-flows-05-and-07-to-11.sql`.

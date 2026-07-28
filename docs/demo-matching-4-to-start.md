# Demo một giải 4 ngựa: Matching → Dự đoán + Inspection → Start Race

Tài liệu này dùng đúng một Tournament:

```text
Tournament ID: e5000000-0000-0000-0000-000000000001
Tên: DEMO FLOW 05 - Jockey Matching
Race ID sau khi mở lịch: e5020000-0000-0000-0000-000000000001
Mật khẩu chung: 12345678
```

## 1. Chuẩn bị dữ liệu nền

Trên database local/test đã chạy migration mới nhất, chạy:

```text
docs/sql/demo-flows-05-and-07-to-11.sql
```

Giải ban đầu ở phase `JOCKEY_MATCHING`, có:

| Owner | Horse | Jockey ghép đề xuất |
|---|---|---|
| `mowner1` | Match Horse 1 | `mjockey1` |
| `mowner2` | Match Horse 2 | `mjockey2` |
| `mowner3` | Match Horse 3 | `mjockey3` |
| `mowner4` | Match Horse 4 | `mjockey4` |

## 2. Demo Matching trên FE

Thực hiện lần lượt với cả bốn cặp:

1. Owner đăng nhập và gửi lời mời cho Jockey.
2. Jockey đăng nhập và chấp nhận lời mời.
3. Owner thanh toán phí thuê.
4. Owner thanh toán phí hợp đồng hệ thống.
5. Kiểm tra hợp đồng đã chuyển thành `APPROVED`.

Luồng hiện tại không có bước Admin duyệt hợp đồng. Sau hai khoản thanh toán, BE tự kích hoạt hợp đồng; 30% tiền thuê được trả trước cho Jockey và 70% còn lại nằm trong escrow.

Trước khi sang bước kế tiếp, chạy câu kiểm tra:

```sql
SELECT
    COUNT(*) AS approved_contracts,
    COUNT(DISTINCT owner_id) AS owners,
    COUNT(DISTINCT horse_id) AS horses,
    COUNT(DISTINCT jockey_id) AS jockeys
FROM jockey_horse_contracts
WHERE tournament_id = 'e5000000-0000-0000-0000-000000000001'
  AND status = 'APPROVED';
```

Kết quả bắt buộc là:

```text
approved_contracts = 4
owners = 4
horses = 4
jockeys = 4
```

## 3. Mở đồng thời Prediction và Inspection

Chạy file:

```text
docs/sql/demo-matching-4-open-prediction-inspection.sql
```

Script tạo một Final Round, một Final Race, bốn RaceEntry, lane 1–4 và phân công sẵn nhân sự. Race được đặt sau thời điểm chạy script 5 giờ:

- Prediction mở ngay và đóng trước Race 5 phút.
- Inspection mở ngay; riêng demo này cửa sổ được nới rộng để không hết giờ giữa lúc trình bày.
- Race vẫn là `SCHEDULED`, nên chưa Start được trước `start_time`.

Tài khoản thực hiện:

| Nghiệp vụ | Username |
|---|---|
| Spectator dự đoán TOP3 | `fspec1` |
| Veterinarian khám bốn ngựa | `fvet1` |
| Medical Staff khám bốn Jockey | `fmed1` |
| Race Referee vận hành Race | `frace_ref` |
| Head Referee của Round | `fhead_ref` |

Mật khẩu của tất cả tài khoản là `12345678`.

API chính mà FE đang gọi:

```text
GET  /api/spectator/races/upcoming
GET  /api/spectator/races/{raceId}
POST /api/spectator/races/{raceId}/predictions

GET  /api/vet/races/assigned
POST /api/vet/race-entries/{entryId}/horse-inspection

GET  /api/medical/races/assigned
POST /api/medical/race-entries/{entryId}/jockey-inspection
```

Khi khám để chuẩn bị Start:

- Cả bốn HorseInspection phải là `CONFIRMED + PASS`.
- Cả bốn JockeyInspection phải là `CONFIRMED + PASS`.
- Nếu phiếu ngựa có `handicapWeight > 0`, `isHandicapConfirmed` cũng phải là `true`.
- Nếu một bên FAIL, entry chuyển `SCRATCHED`; script mở Start sẽ chủ động chặn.

## 4. Dời Race về thời điểm có thể Start

Chỉ sau khi đã dự đoán và khám PASS đủ bốn cặp, chạy:

```text
docs/sql/demo-matching-4-unlock-start.sql
```

Script có guard kiểm tra:

- Race vẫn `SCHEDULED` và chưa Start.
- Có đúng bốn entry `CONFIRMED`.
- Có đủ bốn phiếu khám ngựa đạt.
- Có đủ bốn phiếu khám Jockey đạt.
- Có ít nhất một prediction `PENDING`.

Nếu thiếu một điều kiện, MySQL báo lỗi `CHECK constraint` và không dời timeline.

Nếu hợp lệ, script:

- Dời `start_time` về `NOW() - 1 phút`.
- Đóng prediction.
- Giữ Race ở `SCHEDULED`.
- Cho Race Referee khoảng gần 5 giờ để bấm Start.
- Không giả dữ liệu và không tự gọi Start.

## 5. Race Referee Start

Đăng nhập `frace_ref`, mở Race:

```text
e5020000-0000-0000-0000-000000000001
```

FE nên gọi readiness trước:

```http
GET /api/referee/races/e5020000-0000-0000-0000-000000000001/start-readiness
```

Khi `canStart = true`, bấm Start:

```http
POST /api/referee/races/e5020000-0000-0000-0000-000000000001/start
```

BE sẽ tự kiểm tra lại toàn bộ điều kiện trong transaction. Thành công thì Race và Round chuyển sang `ONGOING`; không cần chạy thêm SQL để giả việc Start.

## 6. Nếu cần làm lại từ đầu

Không chạy lại riêng script mở Prediction/Inspection vì nó cố ý chặn khi Round đã tồn tại. Muốn reset toàn bộ demo:

1. Chạy lại `docs/sql/demo-flows-05-and-07-to-11.sql`.
2. Làm lại bốn hợp đồng trên FE.
3. Chạy lại hai script chuyển bước theo đúng thứ tự.


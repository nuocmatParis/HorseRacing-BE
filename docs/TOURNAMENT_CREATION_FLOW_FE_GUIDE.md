# Luồng Tạo Giải Đấu (Tournament Creation Flow) — Hướng dẫn cho Frontend

> Tài liệu mô tả luồng tạo giải đấu theo từng bước wizard, từ nhập thông tin cơ bản đến confirm bracket.  
> Mỗi bước bao gồm: **fields cần hiển thị**, **validation**, **API tương ứng**, và **giao diện gợi ý**.

---

## Tổng Quan Luồng (Wizard 5 Bước)

```
┌───────────────────┐    ┌───────────────────────┐    ┌──────────────────────┐
│  Bước 1           │    │  Bước 2               │    │  Bước 3              │
│  Thông Tin Cơ Bản │───►│  Cấu Hình Phase       │───►│  Cấu Hình Vận Hành   │
│  & Sức Chứa       │    │  Timing               │    │  Ngày Đua            │
└───────────────────┘    └───────────────────────┘    └──────────────────────┘
                                                                │
                         ┌───────────────────────┐              │
                         │  Bước 5               │              ▼
                         │  Preview Bracket       │    ┌──────────────────────┐
                         │  & Confirm             │◄───│  Bước 4              │
                         └───────────────────────┘    │  Cấu Hình Nâng Cao   │
                                                      └──────────────────────┘
```

---

## Bước 1 — Thông Tin Cơ Bản & Sức Chứa

### Mục đích
Admin nhập các thông tin cơ bản của giải đấu: tên, mô tả, địa điểm, ngày thi đấu, các loại phí, và **sức chứa** (số lượng tối đa ngựa/đội tham gia).

### Fields hiển thị

| # | Field | Type | Required | Validation | Mô tả |
|---|-------|------|----------|-----------|-------|
| 1 | `name` | Text | ✅ | Max 150 ký tự, không trùng | Tên giải đấu |
| 2 | `description` | Text | ✅ | Max 150 ký tự | Mô tả giải đấu |
| 3 | `location` | Text | ✅ | Max 200 ký tự | Địa điểm tổ chức |
| 4 | `startDate` | Date | ✅ | Phải là ngày hôm nay | Ngày bắt đầu giải |
| 5 | `endDate` | Date | ✅ | ≥ `startDate` | Ngày kết thúc giải |
| 6 | `registrationFee` | Number | ✅ | > 0 | Phí đăng ký (VNĐ) |
| 7 | `systemContractFee` | Number | ✅ | > 0 | Phí hợp đồng hệ thống (VNĐ) |
| 8 | `totalPrizePool` | Number | ✅ | > 0 | Tổng giải thưởng (VNĐ) |
| 9 | `allowedBreed` | Dropdown | ✅ | Enum: `THOROUGHBRED`, `ARABIAN`, `QUARTER_HORSE` | Giống ngựa cho phép |
| 10 | `raceClass` | Dropdown | ✅ | Enum: `CLASS_1`, `CLASS_2`, `CLASS_3`, `CLASS_4`, `CLASS_5` | Hạng đua |
| 11 | `distance` | Dropdown | ✅ | Enum: `DIST_800`, `DIST_1000`, `DIST_1200`, `DIST_1400`, `DIST_1600`, `DIST_1800`, `DIST_2000`, `DIST_2400` | Cự ly đua (m) |
| 12 | `minHorseAge` | Number | ✅ | ≥ 0 | Tuổi ngựa tối thiểu |
| 13 | `maxHorseAge` | Number | ✅ | > `minHorseAge` | Tuổi ngựa tối đa |
| 14 | **`maxApprovedEntries`** | Number | ✅ | ≥ 1 | **Sức chứa** — Số ngựa tối đa được duyệt tham gia |

### Giao diện gợi ý

```
┌──────────────────────────────────────────────────────────────┐
│               🏇 TẠO GIẢI ĐẤU MỚI                          │
│                                                              │
│  ┌─ Thông tin cơ bản ─────────────────────────────────┐      │
│  │  Tên giải:     [________________________]          │      │
│  │  Mô tả:        [________________________]          │      │
│  │  Địa điểm:     [________________________]          │      │
│  │  Ngày bắt đầu: [____/____/________]                │      │
│  │  Ngày kết thúc:[____/____/________]                │      │
│  └────────────────────────────────────────────────────┘      │
│                                                              │
│  ┌─ Phí & Giải thưởng ───────────────────────────────┐      │
│  │  Phí đăng ký:         [____________] VNĐ          │      │
│  │  Phí hợp đồng hệ thống: [____________] VNĐ       │      │
│  │  Tổng giải thưởng:    [____________] VNĐ          │      │
│  └────────────────────────────────────────────────────┘      │
│                                                              │
│  ┌─ Điều kiện tham gia ──────────────────────────────┐      │
│  │  Giống ngựa:   [▼ Thoroughbred     ]               │      │
│  │  Hạng đua:     [▼ Class 1          ]               │      │
│  │  Cự ly:        [▼ 1200m            ]               │      │
│  │  Tuổi ngựa:    [3] → [10]                          │      │
│  └────────────────────────────────────────────────────┘      │
│                                                              │
│  ┌─ Sức chứa ────────────────────────────────────────┐      │
│  │                                                    │      │
│  │  ⭐ Số ngựa tối đa được duyệt:  [  48  ]          │      │
│  │                                                    │      │
│  │  💡 Sức chứa sẽ quyết định cấu hình thời gian     │      │
│  │     mặc định cho từng giai đoạn ở bước tiếp theo.  │      │
│  └────────────────────────────────────────────────────┘      │
│                                                              │
│                              [Hủy]  [ Tiếp tục → Bước 2 ]   │
└──────────────────────────────────────────────────────────────┘
```

### Hành vi khi nhấn "Tiếp tục"
- Validate client-side tất cả fields.
- Lưu dữ liệu Bước 1 vào state (chưa gọi API).
- Dùng `maxApprovedEntries` để gọi API lấy default phase configs cho Bước 2.

---

## Bước 2 — Cấu Hình Phase Timing (Dựa Trên Sức Chứa)

### Mục đích
Sau khi có sức chứa (`maxApprovedEntries`), hệ thống tra bảng default để đưa ra **thời gian mặc định (ngày)** cho từng phase. Admin có thể chỉnh sửa lại các giá trị default này.

### API gọi khi vào Bước 2

```http
GET /api/admin/phase-timing-defaults?capacity={maxApprovedEntries}
```

**Response:**
```json
{
  "code": 200,
  "result": {
    "REGISTRATION": 6,
    "REVIEW": 4,
    "JOCKEY_MATCHING": 7,
    "SCHEDULING": 4,
    "PRE_RACE_BUFFER": 2
  }
}
```

### Bảng Default Phase Timing theo Sức Chứa

> [!IMPORTANT]
> Bảng dưới đây thể hiện giá trị mặc định. Admin được phép thay đổi từng giá trị.

#### Giai đoạn ĐĂNG KÝ (REGISTRATION)

| Sức chứa | Thời gian mặc định |
|-----------|-------------------|
| 1 – 8 | 3 ngày |
| 9 – 16 | 4 ngày |
| 17 – 32 | 5 ngày |
| 33 – 64 | 6 ngày |
| 65 – 128 | 7 ngày |
| 129+ | 8 ngày |

#### Giai đoạn DUYỆT HỒ SƠ (REVIEW)

| Sức chứa | Thời gian mặc định |
|-----------|-------------------|
| Tất cả | 4 ngày |

#### Giai đoạn GHÉP NÀI (JOCKEY MATCHING)

| Sức chứa | Thời gian mặc định |
|-----------|-------------------|
| 1 – 8 | 3 ngày |
| 9 – 16 | 5 ngày |
| 17 – 32 | 6 ngày |
| 33 – 64 | 7 ngày |
| 65 – 128 | 8 ngày |
| 129+ | 9 ngày |

#### Giai đoạn XẾP LỊCH (SCHEDULING)

| Sức chứa | Thời gian mặc định |
|-----------|-------------------|
| Tất cả | 4 ngày |

#### Giai đoạn ĐỆM TRƯỚC ĐUA (PRE-RACE BUFFER)

| Sức chứa | Thời gian mặc định |
|-----------|-------------------|
| Tất cả | 2 ngày |

### Fields hiển thị (Có thể chỉnh sửa)

| # | Field (key) | Tên hiển thị | Default | Mô tả |
|---|-------------|-------------|---------|-------|
| 1 | `REGISTRATION` | Giai đoạn Đăng ký | Theo bảng trên | Thời gian mở đăng ký (ngày) |
| 2 | `REVIEW` | Giai đoạn Duyệt hồ sơ | 4 | Thời gian Admin duyệt đơn đăng ký (ngày) |
| 3 | `JOCKEY_MATCHING` | Giai đoạn Ghép nài | Theo bảng trên | Thời gian để Trainer ghép Jockey với ngựa (ngày) |
| 4 | `SCHEDULING` | Giai đoạn Xếp lịch | 4 | Thời gian để Admin xếp lịch thi đấu (ngày) |
| 5 | `PRE_RACE_BUFFER` | Đệm trước khi đua | 2 | Khoảng đệm từ khi xếp lịch xong → bắt đầu thi đấu (ngày) |

### Hiển thị Deadline Dự Tính

Khi Admin nhập/chỉnh sửa `registrationOpenAt`, hệ thống tính toán và hiển thị realtime các deadline dự tính:

```
registrationOpenAt      = Admin nhập (DateTime)
registrationCloseAt     = registrationOpenAt + REGISTRATION ngày
reviewDeadlineAt        = registrationCloseAt + REVIEW ngày
jockeyMatchingDeadlineAt= reviewDeadlineAt + JOCKEY_MATCHING ngày
schedulingDeadlineAt    = jockeyMatchingDeadlineAt + SCHEDULING ngày
competitionStartAt      = schedulingDeadlineAt + PRE_RACE_BUFFER ngày (lúc raceDayStartTime)
```

> [!TIP]
> Admin có thể nhập trực tiếp các deadline (override tính tự động). Hệ thống sẽ validate deadline thực tế ≥ deadline tính toán tối thiểu.

### Fields Deadline nhập liệu

| # | Field | Type | Required | Validation | Mô tả |
|---|-------|------|----------|-----------|-------|
| 1 | `registrationOpenAt` | DateTime | ✅ | Không được ở quá khứ | Thời gian mở đăng ký |
| 2 | `registrationCloseAt` | DateTime | ✅ | ≥ `registrationOpenAt` + REGISTRATION ngày | Thời gian đóng đăng ký |
| 3 | `reviewDeadlineAt` | DateTime | ✅ | ≥ `registrationCloseAt` + REVIEW ngày | Deadline duyệt hồ sơ |
| 4 | `jockeyMatchingDeadlineAt` | DateTime | ✅ | ≥ `reviewDeadlineAt` + JOCKEY_MATCHING ngày | Deadline ghép nài |
| 5 | `schedulingDeadlineAt` | DateTime | ✅ | ≥ `jockeyMatchingDeadlineAt` + SCHEDULING ngày | Deadline xếp lịch |

### Giao diện gợi ý

```
┌──────────────────────────────────────────────────────────────────┐
│          📅 CẤU HÌNH THỜI GIAN PHASE                            │
│                                                                  │
│  Sức chứa hiện tại: 48 ngựa                                     │
│                                                                  │
│  ┌─ Độ dài từng Phase (ngày) ─────────────────────────────┐     │
│  │                                                        │     │
│  │  📝 Đăng ký (REGISTRATION):      [ 6 ] ngày  (mặc định)│     │
│  │  🔍 Duyệt hồ sơ (REVIEW):        [ 4 ] ngày  (mặc định)│     │
│  │  🏇 Ghép nài (JOCKEY_MATCHING):   [ 7 ] ngày  (mặc định)│     │
│  │  📋 Xếp lịch (SCHEDULING):       [ 4 ] ngày  (mặc định)│     │
│  │  ⏳ Đệm trước đua (PRE_RACE):     [ 2 ] ngày  (mặc định)│     │
│  │                                                        │     │
│  │  Tổng thời gian chuẩn bị:  23 ngày                     │     │
│  └────────────────────────────────────────────────────────┘     │
│                                                                  │
│  ┌─ Mốc thời gian (Deadline) ─────────────────────────────┐     │
│  │                                                        │     │
│  │  🟢 Mở đăng ký:       [2026-07-24 08:00]               │     │
│  │  🔴 Đóng đăng ký:     2026-07-30 08:00 (tự tính)       │     │
│  │  📋 Deadline duyệt:   2026-08-03 08:00 (tự tính)       │     │
│  │  🏇 Deadline ghép nài: 2026-08-10 08:00 (tự tính)       │     │
│  │  📅 Deadline xếp lịch: 2026-08-14 08:00 (tự tính)       │     │
│  │  🏁 Dự kiến thi đấu:  2026-08-16 08:00 (tự tính)       │     │
│  │                                                        │     │
│  │  ✏️ [Tùy chỉnh thủ công deadline]                      │     │
│  └────────────────────────────────────────────────────────┘     │
│                                                                  │
│  ┌─ Timeline trực quan ──────────────────────────────────────┐   │
│  │  ──────────────────────────────────────────────────────── │   │
│  │  24/07  30/07   03/08   10/08   14/08   16/08            │   │
│  │   │      │       │       │       │       │                │   │
│  │   ├──────┤       │       │       │       │  Đăng ký       │   │
│  │          ├───────┤       │       │       │  Duyệt hồ sơ   │   │
│  │                  ├───────┤       │       │  Ghép nài       │   │
│  │                          ├───────┤       │  Xếp lịch      │   │
│  │                                  ├───────┤  Đệm           │   │
│  │                                          │  🏁 THI ĐẤU    │   │
│  └───────────────────────────────────────────────────────────┘   │
│                                                                  │
│                      [← Quay lại]  [ Tiếp tục → Bước 3 ]        │
└──────────────────────────────────────────────────────────────────┘
```

---

## Bước 3 — Cấu Hình Vận Hành Ngày Đua

### Mục đích
Admin cấu hình các thông số vận hành cho ngày thi đấu: khung giờ đua, khoảng cách giữa các trận, thời gian cho phép chậm/sớm, và thiết lập giờ nghỉ.

### Fields hiển thị

| # | Field | Type | Default | Required | Validation | Mô tả |
|---|-------|------|---------|----------|-----------|-------|
| 1 | `raceDayStartTime` | Time | `08:00` | ❌ | < `raceDayEndTime` | Giờ bắt đầu ngày đua |
| 2 | `raceDayEndTime` | Time | `18:00` | ❌ | > `raceDayStartTime` | Giờ kết thúc ngày đua |
| 3 | `minRaceIntervalMinutes` | Number | `30` | ❌ | 1 – 30 | Khoảng nghỉ tối thiểu giữa 2 trận (phút) |
| 4 | `defaultRaceOperationalMinutes` | Number | `5` | ❌ | ≥ 1 | Thời gian vận hành 1 trận đua (phút) |
| 5 | `startEarlyToleranceMinutes` | Number | `0` | ❌ | ≥ 0 | Cho phép bắt đầu sớm (phút) |
| 6 | `startLateToleranceMinutes` | Number | `30` | ❌ | ≥ 30 | Cho phép bắt đầu muộn (phút) |
| 7 | `applyBreakTime` | Toggle | `false` | ❌ | — | Có áp dụng giờ nghỉ không |
| 8 | `breakStartTime` | Time | — | Nếu `applyBreakTime = true` | > `raceDayStartTime` | Giờ bắt đầu nghỉ |
| 9 | `breakEndTime` | Time | — | Nếu `applyBreakTime = true` | > `breakStartTime` và < `raceDayEndTime` | Giờ kết thúc nghỉ |
| 10 | `qualifiersPerRace` | Number | `4` | ✅ | 1 – 16 | Số ngựa đi tiếp (qua vòng) mỗi trận |
| 11 | `inspectionOpenMinutesBefore` | Number | `60` | ❌ | 30 – 90 | Mở kiểm tra trước trận (phút) |
| 12 | `inspectionCloseMinutesBefore` | Number | `5` | ❌ | ≥ 1 và < `inspectionOpenMinutesBefore` | Đóng kiểm tra trước trận (phút) |

### Validation chuỗi kiểm tra trước trận
```
inspectionOpenMinutesBefore > inspectionCloseMinutesBefore ≥ predictionCloseMinutesBefore ≥ 0
```

### Giao diện gợi ý

```
┌──────────────────────────────────────────────────────────────────┐
│          ⚙️ CẤU HÌNH VẬN HÀNH NGÀY ĐUA                         │
│                                                                  │
│  ┌─ Khung giờ đua ───────────────────────────────────────┐      │
│  │  Giờ bắt đầu:  [08:00]      Giờ kết thúc:  [18:00]   │      │
│  │                                                       │      │
│  │  🔄 Áp dụng giờ nghỉ:  [  OFF  ]                      │      │
│  │     Giờ nghỉ:  [____] → [____]  (ẩn nếu OFF)          │      │
│  └───────────────────────────────────────────────────────┘      │
│                                                                  │
│  ┌─ Khoảng cách & Thời gian ─────────────────────────────┐      │
│  │  Khoảng nghỉ giữa 2 trận:     [ 30 ] phút             │      │
│  │  Thời gian vận hành 1 trận:    [  5 ] phút             │      │
│  │  Cho phép bắt đầu sớm:        [  0 ] phút             │      │
│  │  Cho phép bắt đầu muộn:       [ 30 ] phút             │      │
│  └───────────────────────────────────────────────────────┘      │
│                                                                  │
│  ┌─ Cấu hình vòng đấu ──────────────────────────────────┐      │
│  │  Số ngựa đi tiếp mỗi trận (qualifiers): [ 4 ]         │      │
│  └───────────────────────────────────────────────────────┘      │
│                                                                  │
│  ┌─ Kiểm tra trước trận (Inspection) ────────────────────┐      │
│  │  Mở kiểm tra trước:  [ 60 ] phút                      │      │
│  │  Đóng kiểm tra trước: [  5 ] phút                      │      │
│  └───────────────────────────────────────────────────────┘      │
│                                                                  │
│                      [← Quay lại]  [ Tiếp tục → Bước 4 ]        │
└──────────────────────────────────────────────────────────────────┘
```

---

## Bước 4 — Cấu Hình Nâng Cao

### Mục đích
Admin cấu hình các thiết lập nâng cao: handicap (tải trọng), prediction (dự đoán), và rating (chấm điểm).

### 4A. Handicap Settings

| # | Field | Type | Default | Required | Validation | Mô tả |
|---|-------|------|---------|----------|-----------|-------|
| 1 | `handicapEnabled` | Toggle | — | ✅ | — | Bật/tắt chế độ handicap |
| 2 | `topWeightLbs` | Number | `135` | Nếu `handicapEnabled = true` | > 0 và > `minWeightLbs` | Tải trọng tối đa (lbs) |
| 3 | `minWeightLbs` | Number | `115` | Nếu `handicapEnabled = true` | > 0 và < `topWeightLbs` | Tải trọng tối thiểu (lbs) |
| 4 | `equipmentWeightKg` | Number | `1.5` | Nếu `handicapEnabled = true` | > 0 | Trọng lượng trang bị (kg) |

> [!NOTE]
> Nếu `handicapEnabled = false`, ẩn 3 field tải trọng. Backend sẽ tự set giá trị = 0.

### 4B. Prediction Settings

| # | Field | Type | Default | Validation | Mô tả |
|---|-------|------|---------|-----------|-------|
| 1 | `predictionTop1CorrectPoints` | Number | `100` | ≥ 0 | Điểm dự đoán đúng TOP 1 |
| 2 | `predictionTop3ExactPositionPoints` | Number | `30` | ≥ 0 | Điểm đúng vị trí trong TOP 3 |
| 3 | `predictionTop3CorrectHorsePoints` | Number | `10` | ≥ 0 | Điểm đúng ngựa trong TOP 3 |
| 4 | `predictionTop3PerfectBonusPoints` | Number | `50` | ≥ 0 | Điểm bonus đoán hoàn hảo TOP 3 |
| 5 | `predictionOpenMinutesBefore` | Number | `120` | ≥ 1 | Mở dự đoán trước trận (phút) |
| 6 | `predictionCloseMinutesBefore` | Number | `5` | ≥ 0 | Đóng dự đoán trước trận (phút) |
| 7 | `predictionCardOpenHoursBeforeFirstRace` | Number | `24` | ≥ 1 | Mở phiếu dự đoán trước race đầu (giờ) |

### 4C. Rating Config (Optional)

> [!TIP]
> FE nên gọi API lấy default rating config để hiển thị giá trị mặc định. Admin có thể chỉnh sửa.

**API lấy default:**
```http
GET /api/admin/tournaments/rating-config/default
```

**Response:**
```json
{
  "code": 200,
  "result": {
    "firstMin": 6,  "firstMax": 12,
    "secondMin": 2, "secondMax": 5,
    "thirdMin": 1,  "thirdMax": 4,
    "fourthFifthMin": 0, "fourthFifthMax": 2,
    "otherMin": -8,  "otherMax": 0,
    "disqualifiedMin": -8, "disqualifiedMax": 0
  }
}
```

| Hạng | Min | Max | Mô tả |
|------|-----|-----|-------|
| 🥇 Hạng 1 | 6 | 12 | Điểm rating cộng cho ngựa về nhất |
| 🥈 Hạng 2 | 2 | 5 | Điểm rating cộng cho ngựa về nhì |
| 🥉 Hạng 3 | 1 | 4 | Điểm rating cộng cho ngựa về ba |
| Hạng 4-5 | 0 | 2 | Điểm rating cho ngựa hạng 4, 5 |
| Còn lại | -8 | 0 | Điểm rating cho ngựa không lọt TOP 5 |
| Bị loại (DQ) | -8 | 0 | Điểm rating cho ngựa bị truất quyền |

### Giao diện gợi ý

```
┌──────────────────────────────────────────────────────────────────┐
│          🔧 CẤU HÌNH NÂNG CAO                                   │
│                                                                  │
│  ┌─ Handicap (Tải trọng) ────────────────────────────────┐      │
│  │  Bật Handicap:  [ ✅ ON  ]                             │      │
│  │  Tải trọng tối đa:    [135] lbs                        │      │
│  │  Tải trọng tối thiểu:  [115] lbs                       │      │
│  │  Trọng lượng trang bị: [1.5] kg                        │      │
│  └───────────────────────────────────────────────────────┘      │
│                                                                  │
│  ┌─ Dự đoán (Prediction) ────────────────────────────────┐      │
│  │  Điểm TOP 1:            [100]                          │      │
│  │  Điểm đúng vị trí TOP3: [ 30]                          │      │
│  │  Điểm đúng ngựa TOP3:   [ 10]                          │      │
│  │  Bonus hoàn hảo TOP3:   [ 50]                          │      │
│  │  ──────────────────────────────────────                │      │
│  │  Mở dự đoán trước trận: [120] phút                     │      │
│  │  Đóng dự đoán trước:    [  5] phút                     │      │
│  │  Mở phiếu dự đoán trước: [24] giờ                      │      │
│  └───────────────────────────────────────────────────────┘      │
│                                                                  │
│  ┌─ Rating Config ────── [Mặc định ✅] ──────────────────┐      │
│  │  🥇 Hạng 1:     [6] → [12]                             │      │
│  │  🥈 Hạng 2:     [2] → [ 5]                             │      │
│  │  🥉 Hạng 3:     [1] → [ 4]                             │      │
│  │  4-5:            [0] → [ 2]                             │      │
│  │  Còn lại:       [-8] → [ 0]                             │      │
│  │  Bị loại (DQ):  [-8] → [ 0]                             │      │
│  └───────────────────────────────────────────────────────┘      │
│                                                                  │
│                      [← Quay lại]  [ Tiếp tục → Bước 5 ]        │
└──────────────────────────────────────────────────────────────────┘
```

---

## Bước 5 — Tạo Giải, Preview Bracket & Confirm

### Mục đích
Gửi request tạo giải → Xem trước cấu trúc Bracket (Round + Race) → Xác nhận.

### Bước 5A — Gọi API Tạo Giải

Gom tất cả dữ liệu từ Bước 1-4, gọi:

```http
POST /api/admin/tournaments
```

**Request Body (đầy đủ):**
```json
{
  // ─── Bước 1: Thông tin cơ bản ───
  "name": "Mùa giải 1",
  "description": "Giải đua ngựa mùa xuân",
  "startDate": "2026-08-01",
  "endDate": "2026-08-10",
  "location": "Trường đua Phú Thọ",
  "registrationFee": 500000,
  "systemContractFee": 200000,
  "totalPrizePool": 50000000,
  "allowedBreed": "THOROUGHBRED",
  "raceClass": "CLASS_1",
  "distance": "DIST_1200",
  "minHorseAge": 3,
  "maxHorseAge": 10,
  "maxApprovedEntries": 48,

  // ─── Bước 2: Phase Timing ───
  "phaseConfigs": {
    "REGISTRATION": 6,
    "REVIEW": 4,
    "JOCKEY_MATCHING": 7,
    "SCHEDULING": 4,
    "PRE_RACE_BUFFER": 2
  },
  "registrationOpenAt": "2026-07-24T08:00:00",
  "registrationCloseAt": "2026-07-30T08:00:00",
  "reviewDeadlineAt": "2026-08-03T08:00:00",
  "jockeyMatchingDeadlineAt": "2026-08-10T08:00:00",
  "schedulingDeadlineAt": "2026-08-14T08:00:00",

  // ─── Bước 3: Vận hành ───
  "raceDayStartTime": "08:00",
  "raceDayEndTime": "18:00",
  "minRaceIntervalMinutes": 30,
  "defaultRaceOperationalMinutes": 5,
  "startEarlyToleranceMinutes": 0,
  "startLateToleranceMinutes": 30,
  "applyBreakTime": false,
  "breakStartTime": null,
  "breakEndTime": null,
  "qualifiersPerRace": 4,
  "inspectionOpenMinutesBefore": 60,
  "inspectionCloseMinutesBefore": 5,

  // ─── Bước 4: Nâng cao ───
  "handicapEnabled": true,
  "topWeightLbs": 135,
  "minWeightLbs": 115,
  "equipmentWeightKg": 1.5,
  "predictionTop1CorrectPoints": 100,
  "predictionTop3ExactPositionPoints": 30,
  "predictionTop3CorrectHorsePoints": 10,
  "predictionTop3PerfectBonusPoints": 50,
  "predictionOpenMinutesBefore": 120,
  "predictionCloseMinutesBefore": 5,
  "predictionCardOpenHoursBeforeFirstRace": 24,
  "ratingConfig": {
    "firstMin": 6, "firstMax": 12,
    "secondMin": 2, "secondMax": 5,
    "thirdMin": 1, "thirdMax": 4,
    "fourthFifthMin": 0, "fourthFifthMax": 2,
    "otherMin": -8, "otherMax": 0,
    "disqualifiedMin": -8, "disqualifiedMax": 0
  }
}
```

**Response:**
```json
{
  "code": 200,
  "message": "Success",
  "result": {
    "tournamentId": "uuid-...",
    "name": "Mùa giải 1",
    "status": "DRAFT",
    "phase": "DRAFT",
    "maxApprovedEntries": 48,
    "qualifiersPerRace": 4,
    "maxEntriesPerRace": 16,
    "minEntriesPerRace": 8,
    "competitionStartAt": "2026-08-16T08:00:00",
    "phaseConfigs": {
      "REGISTRATION": 6,
      "REVIEW": 4,
      "JOCKEY_MATCHING": 7,
      "SCHEDULING": 4,
      "PRE_RACE_BUFFER": 2
    }
  }
}
```

### Bước 5B — Preview Bracket

Sau khi tạo giải thành công, gọi API preview:

```http
GET /api/admin/tournaments/{tournamentId}/bracket-preview?actualEntries=48
```

**Response:**
```json
{
  "code": 200,
  "result": {
    "bracket": {
      "totalEntries": 48,
      "roundCount": 2,
      "rounds": [
        {
          "sequenceOrder": 1,
          "roundName": "Vòng 1",
          "raceCount": 3,
          "entriesPerRace": 16,
          "qualifiersPerRace": 4,
          "isFinal": false,
          "estimatedStartDate": "2026-08-16T08:00:00",
          "estimatedEndDate": "2026-08-16T09:15:00",
          "races": [
            { "sequenceOrder": 1, "name": "Race 1", "startTime": "2026-08-16T08:00:00", "endTime": "2026-08-16T08:05:00" },
            { "sequenceOrder": 2, "name": "Race 2", "startTime": "2026-08-16T08:35:00", "endTime": "2026-08-16T08:40:00" },
            { "sequenceOrder": 3, "name": "Race 3", "startTime": "2026-08-16T09:10:00", "endTime": "2026-08-16T09:15:00" }
          ]
        },
        {
          "sequenceOrder": 2,
          "roundName": "Chung Kết",
          "raceCount": 1,
          "entriesPerRace": 12,
          "qualifiersPerRace": 0,
          "isFinal": true,
          "estimatedStartDate": "2026-08-18T08:00:00",
          "estimatedEndDate": "2026-08-18T08:05:00",
          "races": [
            { "sequenceOrder": 1, "name": "Race 1", "startTime": "2026-08-18T08:00:00", "endTime": "2026-08-18T08:05:00" }
          ]
        }
      ]
    },
    "phaseConfigs": {
      "REGISTRATION": 6,
      "REVIEW": 4,
      "JOCKEY_MATCHING": 7,
      "SCHEDULING": 4,
      "PRE_RACE_BUFFER": 2
    }
  }
}
```

### Bước 5C — Confirm Bracket

Admin xem xét cấu trúc bracket, nếu hài lòng → nhấn **Xác nhận**:

```http
POST /api/admin/tournaments/{tournamentId}/bracket-confirm
```

**Response:**
```json
{
  "code": 200,
  "message": "Bracket confirmed and rounds/races created"
}
```

> [!IMPORTANT]
> Sau khi confirm, hệ thống sẽ tạo Round + Race entities thực tế trong DB. Nếu muốn thay đổi, cần gọi API recalculate.

### Giao diện gợi ý

```
┌──────────────────────────────────────────────────────────────────────┐
│          🏆 XEM TRƯỚC CẤU TRÚC GIẢI ĐẤU                             │
│                                                                      │
│  Giải: Mùa giải 1  |  Sức chứa: 48  |  Qualifiers/Race: 4          │
│                                                                      │
│  ┌─ Bracket Preview ──────────────────────────────────────────┐      │
│  │                                                            │      │
│  │  📌 Vòng 1 — 3 trận (16 ngựa/trận, 4 đi tiếp)            │      │
│  │  ┌──────────────────────────────────────────────┐         │      │
│  │  │  Race 1  │  08:00 – 08:05  │  16/08/2026     │         │      │
│  │  │  Race 2  │  08:35 – 08:40  │  16/08/2026     │         │      │
│  │  │  Race 3  │  09:10 – 09:15  │  16/08/2026     │         │      │
│  │  └──────────────────────────────────────────────┘         │      │
│  │        ↓ 12 ngựa đi tiếp (3 × 4)                          │      │
│  │                                                            │      │
│  │  🏁 Chung Kết — 1 trận (12 ngựa)                          │      │
│  │  ┌──────────────────────────────────────────────┐         │      │
│  │  │  Race 1  │  08:00 – 08:05  │  18/08/2026     │         │      │
│  │  └──────────────────────────────────────────────┘         │      │
│  │                                                            │      │
│  │  Tổng: 2 vòng, 4 trận                                     │      │
│  └────────────────────────────────────────────────────────────┘      │
│                                                                      │
│  ┌─ Sơ đồ Bracket ───────────────────────────────────────────┐      │
│  │                                                            │      │
│  │  Race 1 (16) ──┐                                          │      │
│  │                 ├── 4 ──┐                                  │      │
│  │  Race 2 (16) ──┘        │                                  │      │
│  │                         ├── Chung Kết (12) ── 🏆           │      │
│  │  Race 3 (16) ───── 4 ──┘                                  │      │
│  │                                                            │      │
│  └────────────────────────────────────────────────────────────┘      │
│                                                                      │
│          [← Quay lại chỉnh sửa]     [ ✅ Xác nhận Bracket ]         │
└──────────────────────────────────────────────────────────────────────┘
```

---

## Bước Bổ Sung — Thay Đổi Bracket (Recalculate)

Nếu sau khi confirm, Admin muốn thay đổi số lượng entries:

```http
PUT /api/admin/tournaments/{id}/bracket-recalculate?actualEntries={n}
```

Hệ thống sẽ:
1. Cập nhật `maxApprovedEntries` = `n`
2. Xóa Round/Race cũ
3. Tạo lại bracket mới

---

## Tóm Tắt API Sử Dụng

| # | Bước | Method | Endpoint | Mô tả |
|---|------|--------|----------|-------|
| 1 | Bước 2 | `GET` | `/api/admin/phase-timing-defaults?capacity={n}` | Lấy default phase config theo sức chứa |
| 2 | Bước 4 | `GET` | `/api/admin/tournaments/rating-config/default` | Lấy default rating config |
| 3 | Bước 5A | `POST` | `/api/admin/tournaments` | Tạo giải đấu |
| 4 | Bước 5B | `GET` | `/api/admin/tournaments/{id}/bracket-preview?actualEntries={n}` | Xem trước bracket |
| 5 | Bước 5C | `POST` | `/api/admin/tournaments/{id}/bracket-confirm` | Xác nhận bracket |
| 6 | Bổ sung | `PUT` | `/api/admin/tournaments/{id}/bracket-recalculate?actualEntries={n}` | Tính lại bracket |

---

## Thuật Toán Tính Bracket (Tham Khảo)

```
Input:  totalEntries = 48, maxPerRace = 16, qualifiersPerRace = 4

Bước 1: races = ceil(48 / 16) = 3
Bước 2: entriesPerRace = ceil(48 / 3) = 16
         → Vòng 1: 3 race × 16 ngựa, 4 đi tiếp
         → qualifiers = 3 × 4 = 12
Bước 3: 12 ≤ 16
         → Chung Kết: 1 race × 12 ngựa

Kết quả: 2 vòng, 4 trận
```

**Ví dụ 2:** 100 entries

```
Bước 1: races = ceil(100 / 16) = 7
Bước 2: entriesPerRace = ceil(100 / 7) = 15
         → Vòng 1: 7 race × 15 ngựa, 4 đi tiếp
         → qualifiers = 7 × 4 = 28
Bước 3: 28 > 16 → tiếp
         races = ceil(28 / 16) = 2
         entriesPerRace = ceil(28 / 2) = 14
         → Vòng 2: 2 race × 14 ngựa, 4 đi tiếp
         → qualifiers = 2 × 4 = 8
Bước 4: 8 ≤ 16
         → Chung Kết: 1 race × 8 ngựa

Kết quả: 3 vòng, 10 trận
```

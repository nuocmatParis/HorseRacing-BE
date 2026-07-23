# Bộ luật nghiệp vụ Horse Racing

## 1. Mục đích

File này là nguồn tham chiếu tập trung cho các rule nghiệp vụ đã có trong code và các rule đã thống nhất cần triển khai.

Ký hiệu:

- **CURRENT:** code hiện đã thực hiện.
- **TARGET:** rule đã thống nhất nhưng cần triển khai hoặc hoàn thiện.

Check-in/QR không thuộc phạm vi triển khai hiện tại và được mô tả riêng trong `CHECK_IN_QR_DESIGN.md`.

---

## 2. Tournament lifecycle

**CURRENT**

```text
DRAFT
→ REGISTRATION_OPEN
→ REGISTRATION_REVIEW
→ JOCKEY_MATCHING
→ SCHEDULING
→ RACING
→ RESULT_PENDING
→ RESULT_PUBLISHED
```

Thứ tự deadline:

```text
registrationOpenAt
< registrationCloseAt
< reviewDeadlineAt
< jockeyMatchingDeadlineAt
< schedulingDeadlineAt
```

Tournament status tương ứng:

- `DRAFT`: đang cấu hình.
- `OPEN`: đang đăng ký hoặc review.
- `ONGOING`: matching, scheduling, racing hoặc chờ kết quả.
- `FINISHED`: đã công bố kết quả/kết thúc.
- `CANCELLED`: giải bị hủy.

---

## 3. Cấu hình lịch Race

### 3.1. Số Race tối đa mỗi ngày

**TARGET**

```java
int maxRacesPerDay;
```

```text
Mặc định: 9 race/ngày
Cho phép cấu hình: 1–9 race/ngày
```

Rule:

- Đếm trên toàn tournament trong cùng ngày.
- Không đếm riêng từng round.
- Không tính race `CANCELLED`.
- Khi update lịch phải loại trừ chính race đang sửa.
- `maxRacesPerDay` là mức trần, không bảo đảm luôn xếp đủ số race đó.

### 3.2. Khoảng cách giữa hai Race

**TARGET**

```java
int minRaceIntervalMinutes;
```

```text
Mặc định: 35 phút
Cho phép cấu hình: 30–60 phút
```

Rule:

```text
nextRace.startTime
>= previousRace.endTime + minRaceIntervalMinutes
```

Ví dụ:

```text
Race 1: 08:00–08:30
Interval: 35 phút
Race 2 bắt đầu sớm nhất: 09:05
```

Khoảng nghỉ phục vụ:

- Chuẩn bị horse và jockey.
- Kiểm tra đường đua.
- Cập nhật kết quả.
- Cho spectator nghỉ và chuẩn bị prediction.
- Đưa horse vào paddock và cổng xuất phát.

Rule conflict tổng quát:

```text
newStart >= existingEnd + interval
hoặc
newEnd + interval <= existingStart
```

Áp dụng giữa mọi race của tournament, kể cả khác round.

### 3.3. Khung giờ ngày đua và khoảng nghỉ

**TARGET**

```text
Khung ngày đua mặc định: 08:00–18:00
Khoảng nghỉ mặc định: không có
```

Các field:

```java
LocalTime raceDayStartTime;
LocalTime raceDayEndTime;
LocalTime breakStartTime; // nullable
LocalTime breakEndTime;   // nullable
```

Rule:

- Race phải nằm trọn trong khung ngày đua.
- Mặc định cho phép chạy xuyên trưa.
- Không bắt buộc chia ca sáng/chiều.
- Admin có thể cấu hình break; hai field break phải cùng null hoặc cùng có giá trị.
- Nếu có break, race không được overlap break.
- Interval tối thiểu 30 phút vẫn luôn áp dụng; mặc định 35 phút.

### 3.4. Số Race tối đa của Round

**CURRENT**

```java
Round.maxRaces >= 1;
```

Đây là giới hạn tổng race của round, độc lập với `maxRacesPerDay`.

---

## 4. Timeline Inspection và Prediction

**TARGET**

Với lịch có race đầu tiên lúc 15:00 ngày thi đấu:

```text
15:00 ngày hôm trước — Mở prediction cho toàn bộ race T-24 giờ
13:30 — Mở inspection             T-90
14:30 — Đóng inspection           T-30
14:55 — Khóa prediction           T-5
15:00 — Bắt đầu race              T-0
```

Cấu hình:

```text
predictionCardOpenHoursBeforeFirstRace = 24
inspectionOpenMinutesBefore  = 90
inspectionCloseMinutesBefore = 30
predictionCloseMinutesBefore = 5
```

Validation:

```text
inspectionOpenMinutesBefore
> inspectionCloseMinutesBefore
> predictionCloseMinutesBefore
>= 0
```

Toàn bộ race có cùng `predictionOpenAt`, tính từ race đầu tiên. Mỗi race tự có `predictionCloseAt = race.startTime - 5 phút`. Inspection vẫn tính riêng từ `Race.startTime`.

---

## 5. Round và Race

### 5.1. Round

**CURRENT**

- `maxRaces >= 1`.
- `maxEntries >= 1`.
- `minEntries >= 1`.
- `minEntries` phải không vượt `maxEntries`.
- Round nằm trong thời gian tournament.
- Sequence order không được trùng trong tournament.
- Prediction type là `TOP1` hoặc `TOP3`.

### 5.2. Race

**CURRENT**

- `endTime` không trước `startTime`.
- Race nằm trong thời gian round.
- Tên và sequence order không trùng trong round.
- Số race không vượt `Round.maxRaces`.
- Trước khi publish schedule phải đủ `minEntries` và có referee.

**TARGET**

- Race nằm trong operating hours và không overlap break tùy chọn.
- Không vượt daily limit.
- Không overlap và đủ interval.
- Prediction time do backend tính.
- Race cancelled không chiếm slot hoặc gây conflict.

---

## 6. Tournament registration

**CURRENT**

Registration status:

```text
PENDING_PAYMENT
→ PENDING_REVIEW
→ APPROVED hoặc REJECTED
```

Có thể chuyển thành `WITHDRAWN` theo nghiệp vụ rút đăng ký.

Horse registration:

- Tournament phải mở đăng ký.
- Horse thuộc đúng owner hiện tại.
- Horse phải `HEALTHY` tại thời điểm đăng ký.
- Breed phải phù hợp `allowedBreed`.
- Age phải nằm trong `minHorseAge–maxHorseAge`.
- Không vượt `maxApprovedHorses` khi admin approve.

Jockey registration:

- Jockey phải đủ điều kiện/available theo rule hiện có.
- Không được có registration active trùng tournament.
- Không vượt `maxApprovedJockeys` khi admin approve.

Registration active gồm:

```text
PENDING_PAYMENT,
PENDING_REVIEW,
APPROVED
```

---

## 7. Contract và Race Entry

**CURRENT**

- Chỉ registration `APPROVED` mới tham gia contract/race entry.
- Contract phải `APPROVED` trước khi xếp entry.
- Contract và race phải thuộc cùng tournament.
- Một contract không được xuất hiện hai lần trong cùng race.
- Lane number không được trùng và không vượt `Round.maxEntries`.
- Tổng entry trong race không vượt `Round.maxEntries`.
- Một horse chỉ xuất hiện một lần trong một round.
- Một jockey chỉ xuất hiện một lần trong một round.
- Entry mới có status `CONFIRMED`.

Entry bị loại trước khi xuất phát dùng `SCRATCHED` theo target rule mới.

---

## 8. Inspection

### 8.1. Quyền thực hiện

**CURRENT**

- Horse inspection do veterinarian được phân công vào race thực hiện.
- Jockey inspection do medical staff được phân công vào race thực hiện.
- Race phải có status `SCHEDULED`.
- Một entry chỉ có một horse inspection và một jockey inspection.
- Inspection lưu `CONFIRMED` ngay.

### 8.2. Cửa sổ Inspection

**TARGET**

```text
T-90 <= now <= T-30
```

- Trước T-90: chưa mở inspection.
- Sau T-30: inspection đã đóng.
- Race đã start: không cho inspection.
- Entry không còn `CONFIRMED`: không cho inspection.

### 8.3. Kết quả

**CURRENT/TARGET**

- Horse `FAIL` → entry `SCRATCHED`.
- Jockey `FAIL` → entry `SCRATCHED`.
- Khi start, entry active phải có cả hai `PASS + CONFIRMED`.
- Handicap tournament yêu cầu handicap weight được xác nhận.

**TARGET tại T-30**

- Thiếu horse inspection → `SCRATCHED`.
- Thiếu jockey inspection → `SCRATCHED`.
- Có inspection fail → `SCRATCHED`.
- Cả hai pass → entry tiếp tục active.

---

## 9. Start Race

**CURRENT**

- Race phải `SCHEDULED`.
- Người start là head referee hoặc assigned referee.
- Entry withdrawn/scratched/disqualified không được tính active.
- Mọi entry active phải có hai inspection pass/confirmed.
- Active entry count phải đạt `Round.minEntries`.
- Khi thành công: race `ONGOING`, lưu `startedAt` và `startedBy`.

**TARGET**

```text
startEarlyToleranceMinutes = 0
startLateToleranceMinutes  = 30
```

Với race 15:00:

```text
15:00 <= thời điểm start <= 15:30
```

Trước 15:00 bị từ chối; sau 15:30 phải chuyển `AWAITING_RESCHEDULE`, reschedule hoặc cancel.

---

## 10. Trạng thái sau khi xuất phát

**TARGET**

Phân biệt rõ:

| Tình huống | Status |
|---|---|
| Hoàn thành race | `FINISHED` |
| Đã xuất phát nhưng không hoàn thành | `DID_NOT_FINISH` |
| Đã xuất phát nhưng bị loại | `DISQUALIFIED` |

`RaceResultStatus` mục tiêu:

```java
FINISHED,
DID_NOT_FINISH,
DISQUALIFIED
```

Rule field:

- `FINISHED`: bắt buộc finish time và rank.
- `DID_NOT_FINISH`: finish time/rank có thể null.
- `DISQUALIFIED`: official rank có thể null.
- Duplicate rank chỉ kiểm tra result có rank.

Không dùng `SCRATCHED` cho entry đã xuất phát.

---

## 11. Prediction mở đồng loạt và thao tác

**TARGET mới**

- Không thêm entity `RaceCard` hoặc `PredictionSession`.
- Tiếp tục dùng `Race.predictionOpenAt`, `Race.predictionCloseAt` và `Prediction.race` hiện có.
- Khi publish lịch tournament, toàn bộ race có chung `predictionOpenAt` tại T-24 giờ của race đầu tiên.
- Nếu lịch publish sau T-24, prediction mở ngay khi publish.
- Mỗi spectator tạo một prediction TOP3 cho mỗi race.
- Có thể dự đoán trước toàn bộ race trong lịch.
- Mỗi race đóng tại T-5 của chính race đó.
- Race 1 bắt đầu không ảnh hưởng prediction Race 2–N.

**CURRENT cần chuyển đổi**

- Prediction hiện chỉ tạo/update trong window lưu trực tiếp trên Race.
- Create không cho race đã start.
- Mỗi spectator chỉ có một prediction không cancelled trên mỗi race.
- Spectator có thể chủ động cancel trước prediction close.
- Prediction mới có status `PENDING`.

**TARGET**

- Update cũng phải chặn race đã start.
- Không được chọn entry không còn `CONFIRMED`.
- Entry scratch trước closeAt: gửi notification và cho spectator sửa.
- Đến closeAt chưa sửa: prediction chứa entry scratched chuyển `VOIDED`.
- Không tự thay horse khác cho spectator.

Prediction status mục tiêu:

```text
PENDING  — đang chờ kết quả
SCORED   — đã chấm điểm
CANCELLED — spectator chủ động hủy
VOIDED   — hệ thống vô hiệu
```

Race bị cancel → toàn bộ prediction pending chuyển `VOIDED`.

DELAYED:

- Prediction đã đóng không tự mở lại.
- Giữ closeAt cũ để tránh lợi dụng thông tin phát sinh.

---

## 12. Prediction TOP1

**CURRENT**

- Prediction phải có đúng một entry.
- `predictedRank` phải bằng 1.
- Entry dự đoán về nhất chính xác: **100 điểm mặc định**.
- Dự đoán sai: 0 điểm.

Cấu hình:

```java
predictionTop1CorrectPoints = 100;
```

---

## 13. Prediction TOP3

**CURRENT**

- Prediction phải có đúng ba entry khác nhau.
- Rank dự đoán phải là 1, 2, 3 và không trùng nhau.

Điểm mặc định cho từng selection:

```text
Đúng horse và đúng vị trí: 30 điểm
Đúng horse trong TOP3 nhưng sai vị trí: 10 điểm
Không nằm trong TOP3: 0 điểm
```

Nếu cả ba selection đều đúng chính xác vị trí:

```text
3 × 30 + perfect bonus 50 = 140 điểm
```

Cấu hình:

```java
predictionTop3ExactPositionPoints = 30;
predictionTop3CorrectHorsePoints = 10;
predictionTop3PerfectBonusPoints = 50;
```

Rule đúng: perfect bonus chỉ được cộng khi cả ba điều kiện sau cùng đúng:

```text
predictedRank(entry1) = actualRank(entry1)
predictedRank(entry2) = actualRank(entry2)
predictedRank(entry3) = actualRank(entry3)
```

**Lỗi CURRENT cần sửa:** code hiện chỉ đặt `allExactPosition = false` khi detail trả điểm âm. Trường hợp đúng horse trong TOP3 nhưng sai vị trí trả 10 điểm, nên biến vẫn có thể là `true` và cộng nhầm perfect bonus. Phải kiểm tra trực tiếp predicted rank với actual rank.

---

## 14. Prediction với kết quả đặc biệt

**TARGET/CURRENT ALIGNMENT**

| Tình huống | Prediction |
|---|---|
| Entry scratch trước start | Cho sửa; chưa sửa thì `VOIDED` |
| Đã xuất phát nhưng DNF | Prediction vẫn `SCORED`, selection đó 0 điểm nếu không có top rank |
| Đã xuất phát nhưng disqualified | Prediction vẫn `SCORED`, selection đó 0 điểm theo official result |
| Race cancelled | Toàn bộ prediction `VOIDED` |

DNF/disqualified không làm void toàn prediction vì entry đã tham gia xuất phát.

---

## 15. Race result, report và scoring

**CURRENT**

- Referee được phân công ghi result.
- Không được trùng entry hoặc rank trong race.
- Result có thể sửa trước khi report publish.
- Report đi qua `Draft → Signed → Published`.
- Head referee ký report.
- Không ký khi còn appeal pending.
- Admin publish report sau khi đã signed.
- Publish report chuyển race sang `COMPLETED` và gọi scoring.
- Scoring chỉ xử lý prediction `PENDING`.
- Reward points được cộng vào spectator.
- Gửi notification sau scoring.

### 15.1. Khung vận hành Race và thời hạn nhận khiếu nại

**TARGET**

Mỗi race có khung vận hành mặc định 30 phút:

```text
defaultRaceOperationalMinutes = 30
```

Ví dụ:

```text
Race.startTime = 08:00
Race.endTime   = 08:30
```

`endTime` không chỉ là thời điểm ngựa chạy xong. Đây là thời điểm kết thúc khung vận hành của race, bao gồm:

- Race diễn ra.
- Ghi nhận kết quả ban đầu.
- Gửi đơn khiếu nại.
- Tiếp nhận bằng chứng ban đầu.

Rule nhận khiếu nại:

```text
race.startedAt != null
và now <= race.endTime
```

- Trước khi race start: chưa nhận đơn.
- Từ khi race start đến hết `endTime`: nhận đơn mới.
- Sau `endTime`: không nhận đơn mới.
- Đơn đã tạo trước deadline và còn `PENDING` vẫn tiếp tục được review sau `endTime`.
- Không được ký/publish report khi còn appeal `PENDING`.
- Sau khi mọi appeal được accepted/rejected/cancelled, report tiếp tục workflow.

Race 08:00–08:30 với interval 35 phút làm race tiếp theo bắt đầu sớm nhất:

```text
08:30 + 35 phút = 09:05
```

Nếu race start muộn nhưng vẫn trong tolerance, `endTime` không tự động kéo dài. Nếu việc start muộn làm thời gian tiếp nhận khiếu nại không còn đủ, admin/referee phải dùng reschedule thay vì ép race chạy trong slot cũ.

---

## 16. Race cancel và postpone

**TARGET**

### Cancel

- Race chuyển `CANCELLED`.
- Lưu `cancelledAt` và `cancellationReason`.
- Void prediction pending.
- Không scoring.
- Release referee/vet/medical staff.
- Không tính daily limit hoặc schedule conflict.

### Postpone/Reschedule

- Khi quá `startTime + 30 phút` mà chưa start, race chuyển `AWAITING_RESCHEDULE`.
- Lưu `postponedAt`, `postponeReason`, `postponedBy`.
- Hệ thống không tự động quyết định lịch mới hoàn toàn.
- Hệ thống đề xuất ngày và khung giờ khả dụng gần nhất; admin xác nhận rồi mới cập nhật.
- Ngày đề xuất phải nằm trong `Tournament.startDate–endDate`.
- Không vượt `maxRacesPerDay`.
- Validate operating hours, break, daily limit, interval và overlap.
- Không trùng lịch horse, jockey, referee, veterinarian và medical staff.
- Medical/vet conflict tính trên inspection window T-90 đến T-30.
- Horse phải đủ thời gian nghỉ giữa hai race.
- Không làm sai thứ tự round; race round trước không được dời sau khi round phụ thuộc đã bắt đầu.
- Tính lại inspection window.
- Prediction chưa đóng có thể cập nhật closeAt theo lịch mới.
- Prediction đã đóng giữ nguyên trạng thái đóng và không mở lại.
- Gửi notification lịch mới.
- Nếu inspection đã thực hiện, áp dụng policy tái khám được chốt sau.

Ví dụ daily limit:

```text
Ngày 1: 9/9 race
Ngày 2: 9/9 race
Ngày 3: 0/9 race
```

Race bị hoãn không thể dồn vào ngày 2; hệ thống đề xuất slot ngày 3. Nếu ngày 2 chỉ có 8/9 race thì có thể đề xuất ngày 2 khi mọi rule khác đều đạt.

API đề xuất chỉ đọc dữ liệu. Khi admin xác nhận, backend phải revalidate trong transaction vì slot có thể đã thay đổi.

---

## 17. Staff assignment

**CURRENT**

- Veterinarian/medical staff `SUSPENDED` không được assign.
- Staff `ASSIGNED` không được assign race khác theo logic hiện tại.

**TARGET**

- Release staff khi race start/finish/cancel theo policy cuối.
- Release khi thay/xóa assignment.
- Về sau kiểm tra overlap theo inspection window thay vì status toàn cục.

---

## 18. Handicap

**CURRENT**

- Handicap có thể bật/tắt ở tournament.
- Khi bật, `topWeightLbs`, `minWeightLbs`, `equipmentWeightKg` bắt buộc dương.
- `minWeightLbs < topWeightLbs`.
- Khi tắt, các cấu hình handicap được đưa về 0.
- Horse inspection tính ballast từ rating và jockey weight.
- Race không start nếu handicap weight yêu cầu nhưng chưa confirm.

Giá trị entity mặc định hiện tại khi bật/cấu hình:

```text
topWeightLbs = 135
minWeightLbs = 115
equipmentWeightKg = 1.5
```

---

## 19. Bracket lũy thừa của 2 và Top 4 đi tiếp

**TARGET**

### 19.1. Phân biệt ba khái niệm

Ba nghiệp vụ sau độc lập với nhau:

1. Mỗi Race không phải Final lấy **Top 4** để đi tiếp.
2. Spectator vẫn chỉ dự đoán **Top 3** của từng Race.
3. Chỉ **Top 3 của Race Final** nhận giải thưởng chung cuộc.

Entry đứng hạng 4 có thể đi tiếp ở Race không phải Final, nhưng:

- Không phải một vị trí dùng để chấm prediction.
- Không nhận giải thưởng chung cuộc nếu đứng hạng 4 ở Final.
- Final không lấy Top 4 đi tiếp vì không còn Round sau.

### 19.2. Cấu hình bracket

```java
int minMaxApprovedEntries = 8;
int minEntriesPerRace = 8;
int maxEntriesPerRace = 16;
int qualifiersPerRace = 4;
int predictionPositions = 3;
int finalPrizePositions = 3;
```

`maxApprovedEntries` phải thỏa mãn:

```text
maxApprovedEntries >= 8
và maxApprovedEntries là lũy thừa của 2
```

Ví dụ hợp lệ:

```text
8, 16, 32, 64, 128, 256, 512, ...
```

Không giới hạn nghiệp vụ ở một danh sách kết thúc tại 64 hoặc 128. Mọi lũy thừa của 2 từ 8 trở lên đều hợp lệ, trong giới hạn kiểu dữ liệu mà hệ thống có thể lưu và tính toán an toàn.

Validation có thể dùng:

```java
value >= 8 && (value & (value - 1)) == 0
```

`maxApprovedEntries` là sức chứa tối đa của Tournament, không bắt buộc số hồ sơ thực tế phải bằng đúng giá trị đó.

### 19.3. Tính cấu trúc Round/Race

Với `maxApprovedEntries >= 16`:

```text
firstRoundRaceCount = maxApprovedEntries / maxEntriesPerRace
minimumApprovedRequired = firstRoundRaceCount × minEntriesPerRace
```

Do `minEntriesPerRace = 8` và `maxEntriesPerRace = 16`:

```text
minimumApprovedRequired = maxApprovedEntries / 2
```

Số hồ sơ APPROVED thực tế `actualApprovedEntries` chỉ giữ được cấu trúc đã chọn khi:

```text
minimumApprovedRequired
<= actualApprovedEntries
<= maxApprovedEntries
```

Hai Race ở Round hiện tại cung cấp entry cho một Race ở Round tiếp theo:

```text
Race A lấy Top 4 ─┐
                  ├─> Race C có 8 entry
Race B lấy Top 4 ─┘
```

Vì vậy:

```text
nextRoundRaceCount = currentRoundRaceCount / 2
```

- Round đầu có 8–16 entry trong mỗi Race.
- Từ Round thứ hai trở đi, mỗi Race nhận đúng 8 entry.
- Số Race giảm một nửa qua mỗi Round cho đến khi còn một Final.
- Không cần DFS/backtracking để tìm cây giải vì cấu trúc được suy ra trực tiếp.

Ví dụ:

| Max approved | APPROVED thực tế hợp lệ | Cấu trúc | Tổng Race |
|---:|---:|---|---:|
| 8 | 8 | 1 Final | 1 |
| 16 | 8–16 | 1 Final | 1 |
| 32 | 16–32 | 2 Race vòng loại → 1 Final | 3 |
| 64 | 32–64 | 4 → 2 → 1 Final | 7 |
| 128 | 64–128 | 8 → 4 → 2 → 1 Final | 15 |
| 256 | 128–256 | 16 → 8 → 4 → 2 → 1 Final | 31 |

Với giá trị tổng quát:

```text
maxApprovedEntries = 2^n
APPROVED thực tế hợp lệ: từ 2^(n-1) đến 2^n
```

Dấu giữa hai giá trị trên biểu thị một khoảng, không phải phép trừ.

### 19.4. Trường hợp đặc biệt 8 và 16

Với `maxApprovedEntries = 8`:

```text
Phải có đúng 8 entry
→ Tournament có một Final
```

Với `maxApprovedEntries = 16`, theo mô hình hiện tại:

```text
Có từ 8 đến 16 entry
→ Tournament chạy thẳng một Final
```

Do đó Final của Tournament mức 16 có thể có tối đa 16 entry. Đây là ngoại lệ so với Final của cây giải lớn hơn, vốn nhận tám qualifier từ hai Race trước.

### 19.5. Phân bổ entry cân bằng

Với:

```text
N = actualApprovedEntries
r = firstRoundRaceCount
```

Tính:

```text
baseSize = N / r
remainder = N % r
```

Quy tắc:

- `remainder` Race đầu nhận `baseSize + 1` entry.
- Các Race còn lại nhận `baseSize` entry.
- Chênh lệch số entry giữa hai Race không quá một.
- Không dồn toàn bộ entry dư vào một Race.
- Một horse, jockey hoặc contract không được xuất hiện hai lần trong cùng Round.

Ví dụ với `N = 50` và `r = 4`:

```text
baseSize = 50 / 4 = 12
remainder = 50 % 4 = 2

Kết quả: 13, 13, 12, 12
```

Không chia thành `16, 16, 10, 8` dù từng Race vẫn nằm trong khoảng 8–16, vì mức cạnh tranh giữa các Race không cân bằng.

Công thức trên chỉ quyết định số lượng entry của từng Race. Để chọn entry cụ thể có thể dùng:

- Seeded shuffle và lưu seed để có thể tái hiện kết quả.
- Phân bổ theo horse rating bằng thuật toán cố định và có thể kiểm tra lại.

Không dùng thứ tự đăng ký làm lợi thế mặc định và không cho Admin tùy ý đưa entry vào Race thuận lợi.

### 19.6. Flow mẫu với sức chứa 64 và 50 entry

Admin cấu hình:

```text
maxApprovedEntries = 64
```

Hệ thống suy ra:

```text
Round 1: 4 Race
Round 2: 2 Race
Round 3: 1 Final
```

Khi đóng đăng ký có 50 entry hợp lệ:

```text
minimumApprovedRequired = 4 × 8 = 32
32 <= 50 <= 64
→ Giữ được cấu trúc
```

Phân bổ Round 1:

```text
Race 1: 13 entry
Race 2: 13 entry
Race 3: 12 entry
Race 4: 12 entry
```

Sau khi toàn bộ report Round 1 được Published:

```text
Top 4 Race 1 + Top 4 Race 2 → Race 5 có 8 entry
Top 4 Race 3 + Top 4 Race 4 → Race 6 có 8 entry
```

Sau khi toàn bộ report Round 2 được Published:

```text
Top 4 Race 5 + Top 4 Race 6 → Final có 8 entry
```

Final xác định Top 3 nhận giải chung cuộc. Prediction của từng Race, kể cả Race vòng loại và Final, vẫn chỉ chấm official Top 3 của chính Race đó.

### 19.7. Kiểm tra khi đóng đăng ký

1. Đếm số entry thực tế đủ điều kiện tham dự.
2. Không cho vượt `maxApprovedEntries`.
3. Tính lại khả năng phân bổ vào Round đầu.
4. Mỗi Race phải có từ 8 đến 16 entry.
5. Nếu hợp lệ, hiển thị preview và chờ Admin xác nhận.
6. Nếu không đủ, không tự tạo Race dưới tám entry và không tự đổi bracket.
7. Đề xuất mức lũy thừa của 2 thấp hơn phù hợp gần nhất.
8. Chỉ thay đổi cấu trúc sau khi Admin xác nhận.

Ví dụ:

```text
maxApprovedEntries = 64
actualApprovedEntries = 30
minimumApprovedRequired = 32

30 < 32
→ Không giữ được cấu trúc 64
→ Đề xuất chuyển xuống cấu trúc 32
→ Chờ Admin xác nhận
```

### 19.8. Chuyển Round

Chỉ chuyển Round khi:

- Tất cả Race của Round hiện tại đã `COMPLETED`.
- Tất cả Race Report của Round đã `Published`.
- Official result không còn được chỉnh sửa.

Với từng Race không phải Final:

1. Chỉ lấy RaceResult status `FINISHED`.
2. Sắp xếp theo official rank tăng dần.
3. Lấy bốn entry có rank tốt nhất.
4. Không lấy `DID_NOT_FINISH` hoặc `DISQUALIFIED` đi tiếp.
5. Ghép Top 4 của hai Race thành tám entry của Race kế tiếp.
6. Tạo RaceEntry cho Round tiếp theo.
7. Chuyển Round tiếp theo sang `SCHEDULING` đúng một lần.

Transition phải idempotent để request hoặc scheduler chạy lại không tạo RaceEntry trùng.

Nếu một Race không có đủ bốn entry `FINISHED`, hệ thống:

- Không dùng DNF/DQ để lấp chỗ.
- Tạm dừng chuyển Round.
- Thông báo Admin.
- Chờ áp dụng reserve/wildcard policy sau khi rule đó được chốt.

### 19.9. Prediction và giải thưởng

Official result ví dụ:

```text
Hạng 1: Horse A
Hạng 2: Horse B
Hạng 3: Horse C
Hạng 4: Horse D
```

Nếu đây không phải Final:

- A, B, C và D đi tiếp.
- Prediction chỉ chấm theo A, B và C.
- Horse D không tạo điểm cho vị trí prediction nào.

Nếu đây là Final:

- A, B và C là Top 3 chung cuộc và được xử lý giải thưởng.
- D không nhận giải chung cuộc.
- Không có entry nào đi tiếp.

Race không thuộc Final:

- Không payout giải thưởng chung cuộc.
- Publish report và chấm prediction Top 3.
- Dùng Top 4 để tạo Round tiếp theo.

Race Final:

- Chỉ payout khi Final Race Report đã Published.
- Chỉ payout official rank 1, 2 và 3.
- Payout phải idempotent và không được thực hiện hai lần.

### 19.10. Horse Rating do trọng tài nhập

- Hệ thống không tự tính điểm Rating từ đối thủ, khoảng cách về đích hoặc số ngựa trong Race.
- Race Referee phải nhập `ratingChange` cho từng RaceResult trước khi gửi Race Report.
- Head Referee được điều chỉnh điểm khi report ở trạng thái `SUBMITTED_TO_HEAD`.
- Nếu Head Referee đổi điểm, bắt buộc nhập `ratingAdjustmentReason`.
- Sau khi Head Referee ký, Rating bị khóa; Admin chỉ xem và publish.
- Chỉ khi Admin publish report, hệ thống mới cộng điểm đã ký vào `Horse.currentRating` và tạo lịch sử.
- Việc áp dụng phải idempotent, không được cộng hai lần cho cùng RaceResult.

| Kết quả | Khoảng Rating cho phép |
|---|---:|
| Hạng 1 | +6 đến +12 |
| Hạng 2 | +2 đến +5 |
| Hạng 3 | +1 đến +4 |
| Hạng 4–5 | 0 đến +2 |
| Hạng 6 trở xuống | -8 đến 0 |
| DID_NOT_FINISH | -8 đến 0 |
| DISQUALIFIED | -8 đến 0 |

---

## 20. Rule chưa chốt

Các quyết định cần chốt trước phase tương ứng:

1. Entry thiếu inspection tại T-30 tự scratch bằng scheduler hay cần head referee xác nhận.
2. Race reschedule sau khi inspection có bắt buộc tái khám không.
3. Disqualified có lưu original rank ngoài official rank không.
4. Có đồng bộ RaceResult status về RaceEntry status không.
5. Staff được release lúc start hay finish.
6. Nếu một Race không đủ bốn entry `FINISHED`, reserve/wildcard được chọn theo tiêu chí nào.

---

## 21. Giá trị mặc định tổng hợp

| Rule | Giá trị mặc định | Khoảng cấu hình |
|---|---:|---:|
| Race tối đa/ngày | 9 | 1–9 |
| Interval giữa race | 35 phút | 30–60 phút |
| Khung ngày đua | 08:00–18:00 | Admin cấu hình hợp lệ |
| Khoảng nghỉ | Không có | Nullable; nếu có phải nằm trong ngày đua |
| Mở prediction | T-24 giờ race đầu tiên | Tất cả race mở đồng loạt |
| Mở inspection | T-90 | Sau prediction open |
| Đóng inspection | T-30 | Trước prediction close |
| Đóng prediction | T-5 từng race | Race độc lập |
| Start sớm tối đa | 0 phút | Không âm |
| Start muộn tối đa | 30 phút | Không âm |
| Khung vận hành Race | 30 phút | Theo cấu hình/policy giải |
| Max approved entry | Lũy thừa của 2, tối thiểu 8 | `8, 16, 32, 64, ...` |
| Entry mỗi Race | Tối thiểu 8, tối đa 16 | Policy bracket |
| Qualifier mỗi Race thường | Top 4 | Chỉ entry `FINISHED` |
| Vị trí prediction | Top 3 | Độc lập với qualifier Top 4 |
| Vị trí nhận giải chung cuộc | Top 3 Final | Chỉ Race duy nhất thuộc Final Round |
| TOP1 đúng | 100 điểm | Từ 0 trở lên |
| TOP3 đúng vị trí | 30 điểm/selection | Từ 0 trở lên |
| TOP3 đúng horse sai vị trí | 10 điểm/selection | Từ 0 trở lên |
| TOP3 perfect bonus | 50 điểm | Từ 0 trở lên |

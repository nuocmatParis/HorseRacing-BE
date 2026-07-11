# Rà soát nghiệp vụ Horse Racing sau khi cập nhật code

## 1. Mục đích tài liệu

Tài liệu này được cập nhật sau khi pull code mới và là bản tổng hợp hiện tại cho các nghiệp vụ:

- Tournament, round và race.
- Xếp lịch race.
- Check-in và inspection trước race.
- Prediction trước race.
- Trạng thái entry trước và sau khi xuất phát.
- Race result, report và scoring prediction.
- Các cấu hình còn thiếu cần triển khai.

Tài liệu phân biệt rõ:

- **Đã có trong code:** chức năng hiện đã tồn tại.
- **Chưa có hoặc chưa đủ:** khoảng trống của code hiện tại.
- **Nghiệp vụ thống nhất:** hướng xử lý dự kiến sẽ triển khai.

---

## 2. Lifecycle tổng thể hiện tại

Tournament đang đi theo lifecycle:

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

Các deadline hiện có:

```text
registrationOpenAt
< registrationCloseAt
< reviewDeadlineAt
< jockeyMatchingDeadlineAt
< schedulingDeadlineAt
```

Scheduler hiện tự động:

- Chuyển `REGISTRATION_OPEN → REGISTRATION_REVIEW` khi hết hạn đăng ký.
- Chuyển `RACING → RESULT_PENDING` khi tất cả race đã hoàn thành.

Các bước review, matching và publish schedule vẫn do admin xác nhận thủ công. Cách này phù hợp nếu admin là người chịu trách nhiệm kiểm soát tiến trình giải.

---

## 3. Timeline chuẩn cho một Race

Giả sử race bắt đầu lúc **15:00**:

| Thời gian | Sự kiện | Offset |
|---|---|---:|
| 13:00 | Mở prediction | T-120 phút |
| 13:30 | Mở check-in và inspection | T-90 phút |
| 14:30 | Hạn hoàn tất inspection | T-30 phút |
| 14:55 | Khóa prediction | T-5 phút |
| 15:00 | Bắt đầu race | T-0 |

Thứ tự bắt buộc:

```text
predictionOpen
< inspectionOpen
< inspectionClose
< predictionClose
< raceStart
```

Khoảng từ 14:30 đến 14:55 dành cho người chơi cập nhật prediction sau khi danh sách entry đủ điều kiện thi đấu đã được xác định.

---

## 4. Các cấu hình timeline đã có

### 4.1. Prediction

`Tournament` hiện đã có:

```java
int predictionOpenMinutesBefore = 120;
int predictionCloseMinutesBefore = 5;
```

`Race` hiện đã có:

```java
LocalDateTime startTime;
LocalDateTime endTime;
LocalDateTime predictionOpenAt;
LocalDateTime predictionCloseAt;
```

`PredictionServiceImpl` đã kiểm tra thời điểm hiện tại nằm trong cửa sổ prediction khi create/update.

### 4.2. Vấn đề hai nguồn thời gian

Hiện cấu hình offset nằm ở `Tournament`, nhưng `CreateRaceRequest` và `UpdateRaceRequest` vẫn cho client truyền trực tiếp:

```java
predictionOpenAt;
predictionCloseAt;
```

Điều này tạo hai nguồn dữ liệu có thể mâu thuẫn.

Ví dụ:

```text
Tournament cấu hình mở trước 120 phút
nhưng client truyền predictionOpenAt trước 90 phút
```

### Nghiệp vụ thống nhất

Client chỉ nên truyền `startTime` và `endTime`. Backend tự tính:

```java
predictionOpenAt = startTime.minusMinutes(predictionOpenMinutesBefore);
predictionCloseAt = startTime.minusMinutes(predictionCloseMinutesBefore);
```

Khi cập nhật `startTime`, backend phải tính lại hai mốc prediction.

---

## 5. Các cấu hình còn thiếu

Code hiện tại chưa có bốn field sau:

```java
int inspectionOpenMinutesBefore = 90;
int inspectionCloseMinutesBefore = 30;
int maxRacesPerDay = 9;
int minRaceIntervalMinutes = 35;
```

Các field này nên đặt ở `Tournament` vì chúng là quy định chung của giải.

| Field | Ý nghĩa | Mặc định đề xuất |
|---|---|---:|
| `inspectionOpenMinutesBefore` | Mở check-in/inspection trước giờ race | 90 phút |
| `inspectionCloseMinutesBefore` | Đóng inspection trước giờ race | 30 phút |
| `maxRacesPerDay` | Số race tối đa trong một ngày của tournament | 9 |
| `minRaceIntervalMinutes` | Khoảng nghỉ tối thiểu giữa hai race | 35 phút |

Validation tổng thể:

```text
predictionOpenMinutesBefore
> inspectionOpenMinutesBefore
> inspectionCloseMinutesBefore
> predictionCloseMinutesBefore
>= 0
```

Với timeline đã thống nhất:

```text
120 > 90 > 30 > 5 >= 0
```

---

## 6. Inspection hiện tại

### Đã có trong code

- Horse inspection do veterinarian được phân công thực hiện.
- Jockey inspection do medical staff được phân công thực hiện.
- Chỉ cho khám khi race có status `SCHEDULED`.
- Một entry chỉ có một horse inspection và một jockey inspection.
- Inspection hiện được lưu thẳng thành `CONFIRMED`.
- Kết quả `FAIL` làm entry chuyển ngay thành `SCRATCHED`.
- Khi start race, mọi entry active phải có cả hai inspection `PASS + CONFIRMED`.
- Nếu bật handicap thì handicap weight phải được xác nhận.

### Chưa có trong code

- Chưa có thời gian mở inspection T-90.
- Chưa có thời gian đóng inspection T-30.
- Chưa kiểm tra entry đã bị scratch/withdraw/disqualified trước khi khám.
- Chưa có check-in riêng.
- Chưa có xử lý tự động entry thiếu inspection khi quá T-30.
- Chưa hỗ trợ tái khám hoặc cập nhật kết quả đã nhập sai.

### Nghiệp vụ thống nhất

Cho phép inspection khi:

```text
race.status = SCHEDULED
và inspectionOpenAt <= now <= inspectionCloseAt
và entry vẫn còn active
```

Các mốc được tính:

```java
inspectionOpenAt = race.startTime - 90 phút;
inspectionCloseAt = race.startTime - 30 phút;
```

Tại T-30:

- `PASS + CONFIRMED` cho cả ngựa và jockey: entry đủ điều kiện.
- Có một inspection `FAIL`: entry `SCRATCHED`.
- Thiếu một trong hai inspection: entry `SCRATCHED` hoặc head referee xử lý thủ công theo policy cuối cùng.

---

## 7. Check-in

Code hiện tại chưa có field, API hoặc workflow check-in. Phần này được xếp làm phase cuối và có tài liệu kỹ thuật riêng tại `docs/CHECK_IN_QR_DESIGN.md`.

### Người xác nhận check-in

Với các role hiện có, người xác nhận là:

- Head referee của round; hoặc
- Referee đã được phân công vào đúng race.

Owner và jockey mang ngựa/jockey đến quầy tiếp nhận. Referee trực quầy tìm entry hoặc quét QR để xác nhận có mặt. Owner/jockey không tự xác nhận check-in; veterinarian và medical staff chỉ chịu trách nhiệm khám chuyên môn.

### Check-in riêng Horse và Jockey

Một `RaceEntry` đại diện cho cặp horse–jockey, nhưng hai bên có thể đến khác thời điểm. Vì vậy phải lưu riêng:

```java
LocalDateTime horseCheckedInAt;
User horseCheckedInBy;
LocalDateTime jockeyCheckedInAt;
User jockeyCheckedInBy;
```

Không cần thêm `CHECKED_IN` vào `RaceEntryStatus`. Entry hoàn tất check-in khi cả hai timestamp khác null.

Luồng:

```text
T-90 mở check-in và inspection
→ horse đến: referee check-in HORSE, vet có thể khám ngay
→ jockey đến: referee check-in JOCKEY, medical staff có thể khám ngay
→ T-30 đóng check-in và inspection
```

Vet chỉ được khám khi `horseCheckedInAt != null`. Medical staff chỉ được khám khi `jockeyCheckedInAt != null`.

Tại T-30, thiếu một trong hai check-in hoặc inspection thì toàn bộ entry bị `SCRATCHED` theo lý do tương ứng.

### QR check-in

Sau khi schedule của race được publish và race chuyển `SCHEDULED`, hệ thống cho phép lấy hai signed QR token:

- Horse QR dành cho horse owner.
- Jockey QR dành cho jockey.

QR có thể được hiển thị ngay sau khi publish schedule, nhưng chỉ được scan trong cửa sổ T-90 đến T-30. QR chỉ nhận diện entry; referee vẫn phải đăng nhập và được backend kiểm tra assignment.

Backend nên sinh token theo yêu cầu, không cần lưu file ảnh QR. Token chứa `entryId`, `raceId`, `target`, `purpose`, `version` và chữ ký. Khi thay horse, jockey hoặc race, tăng QR version để vô hiệu QR cũ.

Để demo trên hai thiết bị, dùng hai tunnel:

```text
FE HTTPS tunnel → giao diện và camera QR
BE HTTPS tunnel → API, VNPay Return URL và IPN
```

Chi tiết endpoint, bảo mật, idempotency, reschedule và cấu hình tunnel nằm trong tài liệu check-in riêng.

---

## 8. Giới hạn số Race trong một ngày

### Hiện trạng

`Round` có:

```java
Integer maxRaces;
```

Field này chỉ giới hạn tổng số race trong một round, không giới hạn số race của tournament trong một ngày.

Code hiện chưa có `maxRacesPerDay`.

### Nghiệp vụ thống nhất

Khi tạo hoặc cập nhật race:

```text
1. Xác định tournament của race.
2. Lấy ngày từ race.startTime theo timezone hệ thống.
3. Đếm toàn bộ race không CANCELLED thuộc tournament trong ngày đó.
4. Khi update, loại trừ chính race đang cập nhật.
5. Nếu số lượng vượt maxRacesPerDay thì từ chối.
```

Không chỉ đếm trong cùng round vì nhiều round có thể diễn ra trong cùng một ngày.

Giá trị mặc định đề xuất:

```text
maxRacesPerDay = 9
```

---

## 9. Khoảng nghỉ giữa các Race

### Hiện trạng

Khi tạo race, code chỉ kiểm tra race mới không bắt đầu trước khi race cuối cùng kết thúc.

Do đó hiện vẫn hợp lệ:

```text
Race 1 kết thúc: 15:30
Race 2 bắt đầu: 15:30
Khoảng nghỉ: 0 phút
```

Code chưa có `minRaceIntervalMinutes`.

Kiểm tra update cũng chưa kiểm tra conflict đầy đủ với mọi race khác.

### Nghiệp vụ thống nhất

```text
nextRace.startTime
>= previousRace.endTime + minRaceIntervalMinutes
```

Ví dụ:

```text
Race 1: 15:00–15:30
minRaceIntervalMinutes = 35
Race 2 bắt đầu sớm nhất: 16:05
```

Điều kiện tổng quát giữa race mới và từng race hiện có:

```text
newStart >= existingEnd + gap
hoặc
newEnd + gap <= existingStart
```

Nếu không thỏa một trong hai điều kiện thì lịch bị conflict.

Race `CANCELLED` không được tính vào conflict và giới hạn race/ngày.

---

## 10. Module Prediction sau code mới

### Đã có trong code

Code mới đã có module prediction thực tế:

- Entity `Prediction`.
- Entity `PredictionDetail`.
- API create prediction.
- API update prediction.
- API cancel prediction.
- Prediction `TOP1` và `TOP3`.
- Validation số entry và predicted rank.
- Cửa sổ mở/đóng prediction.
- Scoring khi race report được publish.
- Cộng reward point cho spectator.
- Notification sau khi chấm điểm.
- AI prediction là module hỗ trợ riêng.

`PredictionStatus` hiện có:

```java
PENDING,
SCORED,
CANCELLED
```

`PredictionDetailStatus` hiện có:

```java
UNSCORED,
CORRECT,
INCORRECT
```

### Các điểm còn thiếu hoặc chưa đúng nghiệp vụ thống nhất

- Khi tạo/update prediction chưa loại entry `SCRATCHED`, withdrawn hoặc disqualified.
- Entry bị scratch sau khi người dùng dự đoán không tự động thông báo hoặc void prediction.
- Chưa có trạng thái `VOIDED`; hiện chỉ có `CANCELLED` do người dùng chủ động hủy.
- Chưa có `voidReason`, `voidedAt`.
- Chưa có xử lý void toàn bộ prediction khi race bị hủy.
- Prediction chứa entry scratch vẫn có thể đi vào scoring và nhận 0 điểm, thay vì được void.
- `updatePrediction()` kiểm tra cửa sổ nhưng chưa kiểm tra `race.startedAt` như create.
- Scoring hiện coi `Disqualified` là sai và 0 điểm. Cần xác nhận lại luật nếu muốn “đã xuất phát nhưng bị loại vẫn chấm prediction”.

---

## 11. Quy tắc Prediction khi Entry bị loại trước Race

### Trường hợp áp dụng

- Horse inspection fail.
- Jockey inspection fail.
- Thiếu inspection khi hết hạn.
- Chủ ngựa hoặc jockey rút trước khi start.
- Entry bị scratch vì lý do an toàn khác.

Entry status:

```text
SCRATCHED
```

### Trước predictionCloseAt

```text
Entry chuyển SCRATCHED
→ gửi notification cho spectator đã chọn entry
→ cho phép update prediction
→ không cho chọn lại entry đã SCRATCHED
```

### Đến predictionCloseAt mà người dùng chưa sửa

Phương án thống nhất cho scope hiện tại:

```text
Prediction chứa entry SCRATCHED
→ Prediction.status = VOIDED
→ rewardPoints = 0
→ không cộng hoặc trừ điểm
→ lưu voidReason
```

Không tự động thay entry khác cho người dùng.

Nên mở rộng enum:

```java
public enum PredictionStatus {
    PENDING,
    SCORED,
    CANCELLED,
    VOIDED
}
```

Và bổ sung vào `Prediction`:

```java
LocalDateTime voidedAt;
String voidReason;
```

---

## 12. Phân biệt trước và sau khi Race bắt đầu

Mốc nghiệp vụ phải dựa trên:

```text
race.startedAt != null
```

Không chỉ dựa vào `race.startTime`, vì race có thể bắt đầu trễ hoặc bị hoãn.

| Tình huống | Entry/Result status | Prediction |
|---|---|---|
| Không đạt inspection | `SCRATCHED` | Cho sửa trước close; sau đó `VOIDED` nếu chưa sửa |
| Rút trước khi start | `SCRATCHED` | Cho sửa trước close; sau đó `VOIDED` nếu chưa sửa |
| Đã xuất phát và hoàn thành | `FINISHED` | Chấm bình thường |
| Đã xuất phát nhưng không về đích | `DID_NOT_FINISH` | Prediction vẫn được chấm |
| Đã xuất phát nhưng bị loại vì vi phạm | `DISQUALIFIED` | Prediction vẫn được chấm theo luật đã công bố |
| Race bị hủy hoàn toàn | Race `CANCELLED` | Toàn bộ prediction `VOIDED` |

Không được chuyển entry thành `SCRATCHED` sau khi entry đã thực sự xuất phát.

---

## 13. Trạng thái Entry và Result hiện tại

### RaceEntryStatus hiện có

```java
CONFIRMED,
SCRATCHED,
DISQUALIFIED,
FINISHED,
WITHDRAWN_BEFORE_SCHEDULE,
WITHDRAWN_AFTER_SCHEDULE
```

### RaceResultStatus hiện có

```java
Finished,
Disqualified
```

### Khoảng trống

Code chưa biểu diễn trường hợp đã xuất phát nhưng không hoàn thành.

Nên bổ sung:

```java
RaceResultStatus.DidNotFinish
```

Có thể đồng thời thêm:

```java
RaceEntryStatus.DID_NOT_FINISH
```

Tuy nhiên, để tránh hai nguồn trạng thái mâu thuẫn, phương án rõ hơn là:

- `RaceEntryStatus` thể hiện eligibility/lifecycle của entry.
- `RaceResultStatus` thể hiện kết quả sau khi xuất phát.

Khi report được publish, có thể đồng bộ trạng thái cuối về RaceEntry nếu frontend cần hiển thị nhanh.

### Lưu ý schema hiện tại

`RaceResult.finishTime` và `rank` đang bắt buộc không null. Với `DidNotFinish`, hai field này không phải lúc nào cũng có giá trị hợp lệ.

Cần sửa theo một trong hai hướng:

1. Cho phép `finishTime` và `rank` nullable khi status là `DidNotFinish` hoặc `Disqualified`.
2. Dùng rank đặc biệt, nhưng cách này không được khuyến nghị.

---

## 14. Race bị hủy hoặc hoãn

`RoundStatus` đã có `CANCELLED`, nhưng chưa có workflow hủy race đầy đủ.

`Race` nên bổ sung:

```java
LocalDateTime cancelledAt;
String cancellationReason;
```

### Khi hủy hoàn toàn

```text
Race.status = CANCELLED
→ ghi cancelledAt và cancellationReason
→ mọi Prediction PENDING chuyển VOIDED
→ không scoring
→ release referee, veterinarian và medical staff
→ không tính race vào maxRacesPerDay
→ không tính race vào kiểm tra conflict lịch
```

### Khi hoãn

Không dùng `CANCELLED` nếu race vẫn sẽ diễn ra.

```text
Admin cập nhật startTime/endTime
→ backend tính lại predictionOpenAt/predictionCloseAt
→ backend tính lại inspection window
→ kiểm tra lại maxRacesPerDay và khoảng nghỉ
→ xác định inspection cũ còn hiệu lực hay cần tái khám
```

---

## 15. Start Race

### Đã có trong code

- Race phải là `SCHEDULED`.
- Người start phải là head referee hoặc referee được phân công.
- Mọi entry active phải có horse inspection `PASS + CONFIRMED`.
- Mọi entry active phải có jockey inspection `PASS + CONFIRMED`.
- Handicap phải được confirm nếu có.
- Số active entry phải đạt `minEntries`.

### Còn thiếu

- Chưa kiểm tra thời điểm hiện tại so với `race.startTime`.
- Chưa có tolerance bắt đầu sớm/muộn.
- Chưa chuyển từng active entry sang trạng thái đã xuất phát.
- Chưa tự động khóa/void prediction không hợp lệ trước khi start.

Đề xuất:

```text
Cho start sớm tối đa 5 phút.
Cho start muộn tối đa 30 phút.
```

```text
race.startTime - 5 phút <= now <= race.startTime + 30 phút
```

---

## 16. Scoring hiện tại và luật cần thống nhất

Scoring hiện chạy khi admin publish race report.

Logic hiện tại:

- Không có result: prediction detail sai, 0 điểm.
- Result `Disqualified`: prediction detail sai, 0 điểm.
- TOP1 đúng hạng 1: cộng điểm TOP1.
- TOP3 đúng vị trí: cộng điểm exact position.
- TOP3 đúng ngựa nhưng sai vị trí trong top 3: cộng điểm correct horse.
- Đúng toàn bộ TOP3: cộng perfect bonus.

Theo nghiệp vụ đã trao đổi:

- `DID_NOT_FINISH`: prediction vẫn được đưa vào chấm, thường sẽ không đạt top nên 0 điểm.
- `DISQUALIFIED` sau khi đã xuất phát: prediction vẫn được chấm theo kết quả chính thức; hiện code đang ép 0 điểm.

Cần chốt cách xếp hạng entry disqualified:

- Nếu không có rank chính thức: 0 điểm là hợp lý.
- Nếu luật giải vẫn công bố thứ tự trước penalty: phải lưu thêm original rank và official rank.

Trong scope hiện tại, khuyến nghị dùng **official result**: disqualified không có hạng hợp lệ và nhận 0 điểm, nhưng prediction vẫn được đánh dấu `SCORED`, không phải `VOIDED`.

---

## 17. Staff assignment

Medical staff và veterinarian hiện có trạng thái toàn cục `AVAILABLE/ASSIGNED/SUSPENDED`.

Khi được gán vào một race, họ chuyển thành `ASSIGNED`, nhưng chưa có workflow release rõ ràng khi race kết thúc/hủy.

Hệ quả:

- Có thể bị kẹt `ASSIGNED`.
- Không thể nhận nhiều race không trùng giờ.
- Availability không phản ánh lịch theo thời gian.

Hướng ngắn hạn:

```text
Release staff khi race start, finish, cancel hoặc assignment bị thay thế.
```

Hướng tốt hơn:

```text
Kiểm tra assignment overlap theo inspection window
thay vì dùng ASSIGNED toàn cục để biểu diễn lịch bận.
```

---

## 18. Danh sách thay đổi ưu tiên

### Phase 1 — Timeline và Scheduling

1. Thêm `inspectionOpenMinutesBefore`.
2. Thêm `inspectionCloseMinutesBefore`.
3. Thêm `maxRacesPerDay`.
4. Thêm `minRaceIntervalMinutes`.
5. Thêm field vào create/update/response DTO của tournament.
6. Validate `120 > 90 > 30 > 5` theo cấu hình thực tế.
7. Backend tự tính prediction time từ race start time.
8. Kiểm tra giới hạn race/ngày khi create/update.
9. Kiểm tra gap và overlap khi create/update.
10. Kiểm tra inspection window.

### Phase 2 — Entry lifecycle

1. Bổ sung check-in.
2. Không cho khám entry không active.
3. Bổ sung `DidNotFinish` cho race result.
4. Cho phép result DNF không có finish time/rank.
5. Phân biệt rõ scratch trước start và result sau start.
6. Bổ sung tolerance khi start race.

### Phase 3 — Prediction integration

1. Không cho prediction chọn entry không active.
2. Notification khi entry được chọn bị scratch.
3. Thêm `PredictionStatus.VOIDED`.
4. Thêm `voidedAt` và `voidReason`.
5. Void prediction chưa sửa sau prediction close.
6. Void toàn bộ prediction khi race bị hủy.
7. Không scoring prediction `CANCELLED/VOIDED`.
8. Chốt scoring cho DNF và disqualified.

### Phase 4 — Cancel/Postpone và staff

1. Bổ sung cancel race workflow.
2. Bổ sung postpone/reschedule workflow.
3. Tính lại mọi timeline sau reschedule.
4. Release staff khi race kết thúc/hủy.
5. Kiểm tra conflict nhân sự theo inspection window.

---

## 19. Cấu hình khuyến nghị cuối cùng

```text
predictionOpenMinutesBefore  = 120
inspectionOpenMinutesBefore  = 90
inspectionCloseMinutesBefore = 30
predictionCloseMinutesBefore = 5
maxRacesPerDay               = 9
minRaceIntervalMinutes       = 35
startEarlyToleranceMinutes   = 5
startLateToleranceMinutes    = 30
```

Timeline cho race lúc 15:00:

```text
13:00  Prediction mở
13:30  Check-in và inspection mở
14:30  Inspection đóng, chốt entry đủ điều kiện
14:55  Prediction đóng
15:00  Race bắt đầu
```

---

## 20. Kết luận

Sau code mới, module prediction đã tồn tại và có thể create/update/cancel/score. Vì vậy trọng tâm tiếp theo không phải xây prediction từ đầu mà là tích hợp prediction với lifecycle thực tế của race entry.

Các khoảng trống quan trọng nhất hiện tại là:

1. Chưa có inspection window T-90 đến T-30.
2. Chưa có giới hạn race/ngày.
3. Chưa có khoảng nghỉ giữa race.
4. Chưa có DNF.
5. Chưa void prediction khi entry scratch hoặc race cancel.
6. Chưa có workflow cancel/postpone race hoàn chỉnh.
7. Chưa quản lý availability nhân sự theo thời gian.

Nên triển khai theo bốn phase ở trên để tránh thay đổi đồng thời quá nhiều state machine.

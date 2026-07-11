# Kế hoạch chỉnh sửa nghiệp vụ Horse Racing

## 1. Phạm vi

Kế hoạch này được lập theo code hiện tại và các nghiệp vụ đã thống nhất.

Bao gồm:

- Timeline prediction và inspection.
- Giới hạn số race trong ngày.
- Khoảng nghỉ giữa các race.
- Validation khi tạo, cập nhật và bắt đầu race.
- Trạng thái entry/result sau khi race bắt đầu.
- Xử lý prediction khi entry scratch hoặc race bị hủy.
- Hủy/hoãn race.
- Giải phóng nhân sự sau race.
- Migration và automated test.


---

## 2. Cấu hình nghiệp vụ đã chốt

Các cấu hình đặt ở `Tournament`:

```text
inspectionOpenMinutesBefore  = 90
inspectionCloseMinutesBefore = 30
predictionCloseMinutesBefore = 5
maxRacesPerDay               = 9
minRaceIntervalMinutes       = 35
startEarlyToleranceMinutes   = 0
startLateToleranceMinutes    = 30
defaultRaceOperationalMinutes = 30
predictionCardOpenHoursBeforeFirstRace = 24
raceDayStartTime             = 08:00
raceDayEndTime               = 18:00
breakStartTime               = null
breakEndTime                 = null
```

Timeline ví dụ cho race lúc 15:00:

```text
Ngày hôm trước 15:00  Mở prediction cho toàn bộ race
13:30  Mở inspection
14:30  Đóng inspection, chốt entry đủ điều kiện
14:55  Khóa prediction
15:00  Bắt đầu race
```

Validation bắt buộc:

```text
inspectionOpenMinutesBefore
> inspectionCloseMinutesBefore
> predictionCloseMinutesBefore
>= 0
```

---

## 3. Phase 1 — Tournament configuration

### Mục tiêu

Bổ sung nguồn cấu hình duy nhất cho timeline và scheduling.

### Công việc

- [ ] Thêm vào `Tournament`:

```java
int predictionCardOpenHoursBeforeFirstRace;
int inspectionOpenMinutesBefore;
int inspectionCloseMinutesBefore;
int maxRacesPerDay;
int minRaceIntervalMinutes;
int startEarlyToleranceMinutes;
int startLateToleranceMinutes;
int defaultRaceOperationalMinutes;
LocalTime raceDayStartTime;
LocalTime raceDayEndTime;
Boolean (có áp dụng g nghỉ k, có thì mới cho nhập 2 cái dưới)
LocalTime breakStartTime;
LocalTime breakEndTime;
```

- [ ] Đặt `@Builder.Default` theo các giá trị đã chốt.
- [ ] Thêm field tương ứng vào `CreateTournamentRequest`.
- [ ] Thêm field tương ứng vào `UpdateTournamentRequest`.
- [ ] Thêm field tương ứng vào `TournamentResponse`.
- [ ] Kiểm tra `TournamentMapper` map đủ các field.
- [ ] Thêm validation annotation cơ bản (`@Min`, `@NotNull` khi create).
- [ ] Mở rộng validation trong `TournamentServiceImpl.create()`.
- [ ] Mở rộng validation partial update trong `TournamentServiceImpl.update()`.
- [ ] Thêm error code riêng cho timeline inspection không hợp lệ.
- [ ] Thêm error code riêng cho cấu hình scheduling không hợp lệ.

### Quy tắc

```text
1 <= maxRacesPerDay <= 9
30 <= minRaceIntervalMinutes <= 60
startEarlyToleranceMinutes >= 0
startLateToleranceMinutes >= 0
defaultRaceOperationalMinutes >= 1
predictionCardOpenHoursBeforeFirstRace >= 1
raceDayStartTime < raceDayEndTime
breakStartTime và breakEndTime cùng null
hoặc raceDayStartTime < breakStartTime < breakEndTime < raceDayEndTime
```

### Tiêu chí hoàn thành

- Tạo tournament thiếu cấu hình vẫn nhận default hợp lệ.
- Không tạo/update được tournament có timeline sai thứ tự.
- API tournament trả đủ cấu hình timeline, scheduling và session mới.

---

## 4. Phase 2 — Mở Prediction đồng loạt trước Race đầu tiên

### Mục tiêu

Khi lịch tournament được publish, toàn bộ race mở prediction tại cùng một thời điểm: trước race đầu tiên 24 giờ. Không thêm `RaceCard` hoặc `PredictionSession`; tiếp tục dùng `Race` và `Prediction` hiện có.

### Công việc

- [ ] Khi publish lịch tournament, bảo đảm toàn bộ race đã qua validation entry/referee/schedule.
- [ ] Lấy race có `startTime` sớm nhất trong toàn bộ lịch.
- [ ] Tính thời điểm mở chung:

```text
commonPredictionOpenAt = firstRace.startTime - 24 giờ
```

- [ ] Gán `predictionOpenAt` giống nhau cho toàn bộ race.
- [ ] Nếu lịch publish trước mốc mở, prediction chỉ nhận request từ mốc `predictionOpenAt`.
- [ ] Nếu lịch publish trễ hơn mốc mở, cho prediction mở ngay và ghi nhận late publish.
- [ ] Mỗi race có close time riêng:

```text
closeAt = race.startTime - 5 phút
```

- [ ] Không cho client truyền prediction open/close time tùy ý.
- [ ] Giữ `Prediction` tham chiếu trực tiếp `Race` như code hiện tại.
- [ ] Unique vẫn là một spectator có một prediction active trên mỗi race.
- [ ] Race 1 start/close không thay đổi prediction window của Race 2–N.
- [ ] Áp dụng cho số race thực tế, không hard-code 8.
- [ ] Cập nhật seed/Postman/API contract.

### Tiêu chí hoàn thành

- Lịch có 8 race thì cả 8 mở đồng loạt tại T-24 giờ của race đầu tiên.
- Mỗi race đóng riêng tại T-5 phút.
- Người dùng có thể dự đoán trước toàn bộ race.
- Prediction race sau không bị ảnh hưởng khi race trước bắt đầu.

---

## 5. Phase 3 — Giới hạn số Race mỗi ngày

### Mục tiêu

Không cho tournament vượt `maxRacesPerDay` trên cùng một ngày.

Một ngày đua thực tế thường có khoảng 6–11 race; ngày đặc biệt có thể nhiều hơn. Trong hệ thống, admin được cấu hình trong phạm vi an toàn:

```text
Mặc định: 9 race/ngày
Cho phép: 1–9 race/ngày
```

Giới hạn được tính trên toàn bộ tournament trong ngày, không phải riêng từng round.

### Công việc

- [ ] Thêm query trong `RaceRepository` để đếm race theo tournament và khoảng ngày.
- [ ] Query đi qua quan hệ `Race → Round → Tournament`.
- [ ] Không đếm race có status `CANCELLED`.
- [ ] Khi create race, tính `startOfDay/endOfDay` từ `startTime`.
- [ ] Khi update race, loại trừ chính race đang cập nhật.
- [ ] Áp dụng giới hạn trên toàn tournament, không chỉ trong round.
- [ ] Thêm `MAX_RACES_PER_DAY_EXCEEDED` vào `ErrorCode`.
- [ ] Validate create/update tournament chỉ nhận `maxRacesPerDay` từ 1 đến 9.

### Lưu ý timezone

- [ ] Chốt timezone ứng dụng là `Asia/Ho_Chi_Minh` hoặc cấu hình tập trung.
- [ ] Không phụ thuộc timezone mặc định khác nhau giữa máy dev/server.

### Tiêu chí hoàn thành

- Tournament có giới hạn 9 không tạo được race thứ 10 cùng ngày.
- Race thứ 10 vẫn tạo được ở ngày khác.
- Update race sang ngày đã đủ slot bị từ chối.
- Race cancelled không chiếm slot.

### Khung giờ tổ chức và khoảng nghỉ tùy chọn

Mặc định cho phép chạy xuyên trưa và không bắt buộc chia ca sáng/chiều.

```text
raceDayStartTime = 08:00
raceDayEndTime   = 18:00
breakStartTime   = null
breakEndTime     = null
```

Admin có thể cấu hình khoảng nghỉ nếu cần, ví dụ 12:00–13:30.

Công việc:

- [ ] Thêm bốn `LocalTime` trên vào entity/DTO/response tournament.
- [ ] Validate start < end.
- [ ] Hai field break phải cùng null hoặc cùng có giá trị.
- [ ] Nếu có break, bắt buộc nằm trọn trong race day.
- [ ] Race phải nằm trọn trong `raceDayStartTime–raceDayEndTime`.
- [ ] Nếu có break, race không được overlap khoảng break.
- [ ] Interval tối thiểu vẫn áp dụng giữa race trước và sau break.
- [ ] Thêm `RACE_OUTSIDE_OPERATING_HOURS` và `RACE_OVERLAPS_BREAK`.
- [ ] Daily max là mức trần; số race khả thi còn phụ thuộc duration, interval và break.

Tiêu chí:

- Mặc định race có thể chạy xuyên 12:00.
- Nếu break null, race 11:55–12:05 được phép xét tiếp.
- Nếu break 12:00–13:30, race 11:55–12:05 bị từ chối.
- Race kết thúc sau 18:00 bị từ chối.

---

## 6. Phase 4 — Khoảng cách và chống trùng lịch Race

### Mục tiêu

Kiểm tra conflict đầy đủ thay vì chỉ so race mới với race cuối của round. Khoảng cách được cấu hình bằng:

```java
int minRaceIntervalMinutes;
```

Giá trị nghiệp vụ:

```text
Mặc định: 35 phút
Cho phép cấu hình: 30–60 phút
```

Khoảng cách được tính từ `endTime` của race trước đến `startTime` của race tiếp theo, không phải từ hai `startTime`.

### Công việc

- [ ] Thêm repository query lấy các race khác của tournament trong khoảng liên quan.
- [ ] Khi create, kiểm tra với tất cả race không cancelled.
- [ ] Khi update, loại trừ chính race đang sửa.
- [ ] Áp dụng giữa các round khác nhau trong cùng tournament.
- [ ] Thay logic hiện tại chỉ đọc race cuối của round.
- [ ] Thêm `RACE_SCHEDULE_CONFLICT` vào `ErrorCode`.
- [ ] Trả message có race đang conflict nếu error response policy cho phép.
- [ ] Validate create/update tournament chỉ nhận `minRaceIntervalMinutes` từ 30 đến 60.

### Công thức

Race mới hợp lệ với một race đã có khi:

```text
newStart >= existingEnd + minRaceIntervalMinutes
hoặc
newEnd + minRaceIntervalMinutes <= existingStart
```

Nếu không thỏa thì conflict.

Ví dụ:

```text
Race 1: 08:00–08:30
minRaceIntervalMinutes = 35
Race 2 bắt đầu sớm nhất: 09:05
```

Khoảng nghỉ phục vụ:

- Chuẩn bị horse và jockey cho race tiếp theo.
- Kiểm tra và chuẩn bị lại đường đua.
- Cập nhật kết quả race trước.
- Cho spectator nghỉ và chuẩn bị prediction.
- Đưa horse vào paddock và cổng xuất phát.

### Tiêu chí hoàn thành

Với interval 35 phút:

```text
Race A: 15:00–15:30
Race B bắt đầu sớm nhất: 16:05
```

- Race cùng thời gian ở hai round khác nhau vẫn bị chặn.
- Update không thể tạo overlap.
- Race cancelled không gây conflict.
- Race mặc định chiếm một khung vận hành 30 phút trước khi cộng interval.

---

## 7. Phase 5 — Inspection window

### Mục tiêu

Chỉ cho phép khám trong T-90 đến T-30.

### Công việc

- [ ] Tạo helper/domain service tính `inspectionOpenAt` và `inspectionCloseAt`.
- [ ] Dùng chung helper cho horse inspection và jockey inspection.
- [ ] Trong `HorseInspectionServiceImpl`, kiểm tra thời gian trước khi lưu.
- [ ] Trong `JockeyInspectionServiceImpl`, kiểm tra thời gian trước khi lưu.
- [ ] Không cho khám nếu race đã start.
- [ ] Không cho khám entry không còn `CONFIRMED`.
- [ ] Giữ rule người khám phải được phân công đúng race.
- [ ] Thêm error code:

```text
INSPECTION_WINDOW_NOT_OPEN
INSPECTION_WINDOW_CLOSED
RACE_ENTRY_NOT_ACTIVE
```

### Rule hiện giữ nguyên

- Inspection được lưu `CONFIRMED` ngay.
- Horse/jockey inspection `FAIL` làm entry `SCRATCHED`.
- Một entry chỉ có một inspection mỗi loại trong phase này.

### Việc chưa làm trong phase này

- Không làm check-in.
- Không làm QR.
- Không làm tái khám/version inspection.

### Tiêu chí hoàn thành

- Race 15:00 chỉ cho khám từ 13:30 đến 14:30.
- Entry scratched/withdrawn/disqualified không được khám.
- Race đã start không nhận inspection mới.

---

## 8. Phase 6 — Chốt Entry tại Inspection deadline

### Mục tiêu

Đến T-30 phải xác định được danh sách entry đủ điều kiện.

### Phương án triển khai

Ưu tiên phương án service idempotent được gọi trước khi start race và có thể gọi bằng scheduler:

```text
finalizeRaceEntries(raceId)
```

### Công việc

- [ ] Tìm các race `SCHEDULED` đã qua inspection close time nhưng chưa finalize.
- [ ] Bổ sung vào `Race`:

```java
LocalDateTime inspectionFinalizedAt;
```

- [ ] Với mỗi entry active:
  - Thiếu horse inspection → `SCRATCHED`.
  - Thiếu jockey inspection → `SCRATCHED`.
  - Inspection fail → bảo đảm `SCRATCHED`.
  - Cả hai `PASS + CONFIRMED` → giữ active.
- [ ] Lưu `scratchedReason` theo nguyên nhân rõ ràng.
- [ ] Không finalize hai lần.
- [ ] Có scheduler chạy định kỳ hoặc gọi lazy-finalize trước start/prediction processing.
- [ ] Gửi notification tới owner, jockey và spectator bị ảnh hưởng nếu notification integration đã sẵn sàng.

### Tiêu chí hoàn thành

- Sau T-30 không còn entry active nhưng thiếu inspection.
- Chạy finalize nhiều lần không làm sai dữ liệu.
- Race không đủ `minEntries` không được start.

---

## 9. Phase 7 — Start Race đúng thời gian

### Mục tiêu

Không cho start quá sớm hoặc quá muộn ngoài tolerance.

### Công việc

- [ ] Trong `RaceServiceImpl.startRace()`, tính khoảng được phép:

```text
earliestStart = startTime - startEarlyToleranceMinutes
latestStart   = startTime + startLateToleranceMinutes
```

- [ ] Chặn trước `earliestStart`.
- [ ] Chặn sau `latestStart`; yêu cầu reschedule/postpone.
- [ ] Gọi finalize entry nếu đã qua T-30 mà chưa finalize.
- [ ] Giữ validation referee assignment.
- [ ] Giữ validation inspection và handicap.
- [ ] Thêm error code `RACE_START_TOO_EARLY` và `RACE_START_WINDOW_EXPIRED`.

### Tiêu chí hoàn thành

Với race 15:00 và tolerance `0/+30`:

- Trước 15:00 không start được.
- 15:00–15:30 start được nếu đủ điều kiện.
- Sau 15:30 phải reschedule hoặc cancel.

### Khung vận hành và Appeal deadline

- [ ] Mặc định race có `endTime = startTime + 30 phút` nếu admin không override duration theo policy.
- [ ] Xem `endTime` là hạn nhận appeal mới của race.
- [ ] Chỉ cho tạo appeal sau khi race đã start và không muộn hơn `endTime`.
- [ ] Sau `endTime`, từ chối appeal mới bằng `APPEAL_SUBMISSION_CLOSED`.
- [ ] Appeal đã tạo trước deadline vẫn được review sau `endTime`.
- [ ] Giữ rule không ký/publish report khi còn appeal `PENDING`.
- [ ] Không tự kéo dài `endTime` khi race start muộn; trường hợp không đủ thời gian vận hành phải reschedule.
- [ ] Interval race sau vẫn tính từ `endTime` của race trước.

---

## 10. Phase 8 — Race Result và DID_NOT_FINISH

### Mục tiêu

Biểu diễn được entry đã xuất phát nhưng không hoàn thành race.

### Công việc

- [ ] Chuẩn hóa tên enum `RaceResultStatus` thành uppercase thống nhất:

```java
FINISHED,
DID_NOT_FINISH,
DISQUALIFIED
```

- [ ] Cập nhật dữ liệu enum cũ `Finished/Disqualified` bằng migration.
- [ ] Cho `RaceResult.finishTime` nullable với DNF/disqualified.
- [ ] Cho `RaceResult.rank` nullable hoặc đổi từ `int` sang `Integer`.
- [ ] Cập nhật `CreateRaceResultRequest` và `UpdateRaceResultRequest`.
- [ ] Validation theo status:
  - `FINISHED`: bắt buộc finishTime và rank.
  - `DID_NOT_FINISH`: không bắt buộc finishTime/rank.
  - `DISQUALIFIED`: rank chính thức có thể null.
- [ ] Chỉ kiểm tra duplicate rank với result có rank.
- [ ] Cập nhật mapper/response.
- [ ] Chốt việc đồng bộ trạng thái cuối về `RaceEntryStatus`.

### Quy tắc prediction

- DNF vẫn được scoring; vì không có top rank nên nhận 0 điểm.
- Disqualified vẫn làm prediction được `SCORED`, nhưng detail chọn entry đó nhận 0 điểm theo official result.
- DNF/disqualified sau khi xuất phát không làm prediction `VOIDED`.

### Tiêu chí hoàn thành

- Lưu được DNF không có finish time/rank.
- Rank của các entry FINISHED vẫn unique.
- Scoring không nhầm DNF với entry scratch trước start.

---

## 11. Phase 9 — Prediction và Entry SCRATCHED

### Mục tiêu

Cho tạo/sửa một dự đoán TOP3 cho mỗi race đến T-5 và void prediction không còn hợp lệ.

### Công việc

- [ ] Mỗi spectator có một prediction TOP3 trên mỗi `Race`.
- [ ] Cho tạo dự đoán trước cho toàn bộ race sau thời điểm mở chung.
- [ ] Khi create/update prediction, chỉ cho chọn entry `CONFIRMED`.
- [ ] Trong `updatePrediction()`, kiểm tra cả `race.startedAt == null` giống create.
- [ ] Thêm `VOIDED` vào `PredictionStatus`.
- [ ] Thêm vào `Prediction`:

```java
LocalDateTime voidedAt;
String voidReason;
```

- [ ] Phân biệt:
  - `CANCELLED`: spectator chủ động hủy trước close.
  - `VOIDED`: hệ thống vô hiệu do entry scratch/race cancel.
- [ ] Khi entry scratch trước prediction close, gửi notification cho spectator đã chọn entry.
- [ ] Spectator được update prediction đến `predictionCloseAt`.
- [ ] Tại `closeAt` T-5, prediction còn chứa entry scratched chuyển `VOIDED`.
- [ ] Không tự động thay entry khác.
- [ ] `ScoringServiceImpl` chỉ lấy prediction `PENDING` nên tiếp tục không scoring `CANCELLED/VOIDED`.
- [ ] Cập nhật repository query để tìm prediction chứa entry cụ thể.
- [ ] Thêm `VOIDED` vào response và FE contract.
- [ ] Sửa perfect bonus TOP3: chỉ cộng khi cả ba horse đúng chính xác cả vị trí; không cộng khi chỉ đúng horse nhưng sai vị trí.
- [ ] Không suy luận `allExactPosition` chỉ từ `points >= 0`; so sánh trực tiếp `predictedRank == actualRank` cho cả ba detail.
- [ ] DELAYED: prediction đã đóng không tự mở lại và giữ nguyên close time cũ.

### Tiêu chí hoàn thành

- Không tạo prediction mới với entry scratched.
- Prediction có entry scratch được sửa trước close.
- Không sửa thì tự `VOIDED` tại close.
- Prediction voided không cộng điểm.

---

## 12. Phase 10 — Cancel và Postpone Race

### Mục tiêu

Phân biệt hủy hoàn toàn và đổi lịch.

### Thay đổi entity

- [ ] Thêm vào `Race`:

```java
LocalDateTime cancelledAt;
String cancellationReason;
LocalDateTime rescheduledAt;
String rescheduleReason;
```

### Cancel

- [ ] Tạo API/service cancel race.
- [ ] Chỉ admin hoặc role được chỉ định có quyền cancel.
- [ ] Không cancel race đã `COMPLETED`.
- [ ] Chuyển race sang `CANCELLED`.
- [ ] Void toàn bộ prediction `PENDING`.
- [ ] Không scoring race cancelled.
- [ ] Release referee/vet/medical staff.
- [ ] Gửi notification.
- [ ] Race cancelled không tính vào daily limit và schedule conflict.

### Postpone/Reschedule

- [ ] Tạo API/service đổi `startTime/endTime` với reason.
- [ ] Khi quá latest start mà chưa chạy, chuyển race sang `AWAITING_RESCHEDULE`.
- [ ] Lưu `postponedAt`, `postponeReason`, `postponedBy`.
- [ ] Tạo API read-only trả các slot khả dụng gần nhất.
- [ ] Không tự động cập nhật lịch theo suggestion.
- [ ] Admin chọn slot và gọi API xác nhận.
- [ ] Chạy lại validation ngày, daily limit, gap và overlap.
- [ ] Kiểm tra conflict horse, jockey, referee, veterinarian và medical staff.
- [ ] Kiểm tra medical/vet theo inspection window T-90 đến T-30.
- [ ] Bổ sung/check cấu hình thời gian nghỉ tối thiểu của horse.
- [ ] Không dời race round trước qua sau round phụ thuộc.
- [ ] Tính lại prediction window.
- [ ] Tính lại inspection window.
- [ ] Prediction đã đóng không được mở lại; prediction chưa đóng mới cập nhật closeAt.
- [ ] Nếu inspection đã diễn ra, chốt policy hết hiệu lực/tái khám trước khi cho reschedule.
- [ ] Gửi notification về lịch mới.
- [ ] Khi admin xác nhận, revalidate toàn bộ trong transaction để tránh tranh chấp slot.

### Tiêu chí hoàn thành

- Cancel void prediction và giải phóng tài nguyên.
- Reschedule không tạo conflict lịch.
- Timeline phụ thuộc được tính lại tự động.

---

## 13. Phase 11 — Staff availability

### Mục tiêu

Không để veterinarian và medical staff bị kẹt `ASSIGNED`.

### Công việc ngắn hạn

- [ ] Release staff khi race start, finish hoặc cancel.
- [ ] Release staff khi assignment bị xóa/thay thế.
- [ ] Bảo đảm transaction cập nhật assignment và status atomically.

### Cải tiến sau

- [ ] Không dùng `ASSIGNED` toàn cục để biểu diễn lịch bận.
- [ ] Kiểm tra assignment overlap theo inspection window T-90 đến T-30.
- [ ] Cho phép một staff nhận nhiều race không trùng thời gian.

### Tiêu chí hoàn thành

- Không còn staff bị kẹt sau lifecycle race.
- Không phân công một người cho hai inspection window trùng nhau.

---

## 14. Phase 12 — Database migration

### Mục tiêu

Không phụ thuộc hoàn toàn vào `ddl-auto=update` cho các thay đổi nghiệp vụ.

### Công việc

- [ ] Chọn Flyway hoặc Liquibase.
- [ ] Migration thêm sáu cấu hình tournament.
- [ ] Migration thêm `inspectionFinalizedAt` cho race.
- [ ] Migration thêm cancellation/reschedule fields.
- [ ] Migration thêm prediction void fields/status.
- [ ] Migration đổi `RaceResultStatus` và nullable result fields.
- [ ] Backfill default cho tournament hiện có.
- [ ] Có rollback/manual recovery note cho migration enum.
- [ ] Chuyển production profile khỏi `ddl-auto=update`.

---

## 15. Phase 13 — Automated tests

Project hiện chưa có thư mục `src/test`, nên cần tạo test từ đầu.

### Tournament tests

- [ ] Default config hợp lệ.
- [ ] Timeline offset sai bị reject.
- [ ] Partial update vẫn validate trên giá trị merge.

### Race scheduling tests

- [ ] Daily limit.
- [ ] Race nằm trong operating hours.
- [ ] Mặc định chạy xuyên trưa khi không có break.
- [ ] Race overlap break tùy chọn bị reject.
- [ ] Race cancelled không chiếm slot.
- [ ] Gap đúng biên.
- [ ] Overlap cùng round và khác round.
- [ ] Update loại trừ chính race.
- [ ] Prediction time tự tính lại.

### Inspection tests

- [ ] Trước T-90 bị reject.
- [ ] Trong T-90 đến T-30 thành công.
- [ ] Sau T-30 bị reject.
- [ ] Entry inactive bị reject.
- [ ] Người khám không được assign bị reject.
- [ ] Finalize thiếu inspection làm scratch.

### Start race tests

- [ ] Quá sớm/quá muộn bị reject.
- [ ] Thiếu inspection bị reject.
- [ ] Không đủ active entry bị reject.
- [ ] Đúng referee và đủ điều kiện thì start thành công.

### Result/scoring tests

- [ ] Finished bắt buộc rank/time.
- [ ] DNF cho phép null rank/time.
- [ ] Disqualified nhận 0 prediction point nhưng prediction vẫn SCORED.
- [ ] DNF nhận 0 nếu không có top rank.

### Prediction tests

- [ ] Không chọn entry scratched.
- [ ] Update trước close thành công.
- [ ] Update sau race started bị reject.
- [ ] Entry scratch gửi notification.
- [ ] Prediction chưa sửa chuyển VOIDED.
- [ ] Race cancel void toàn bộ prediction pending.
- [ ] TOP3 đúng cả ba vị trí nhận 140 điểm với cấu hình mặc định.
- [ ] TOP3 đúng cả ba horse nhưng sai ít nhất một vị trí không nhận perfect bonus.

---

## 16. Thứ tự thực hiện đề xuất

```text
1. Tournament configuration
2. Backend-calculated prediction window
3. Daily race limit
4. Race gap/overlap
5. Inspection window
6. Inspection deadline finalization
7. Start race time validation
8. DNF/result model
9. Prediction scratch/void integration
10. Cancel/postpone race
11. Staff availability
12. Database migration hoàn chỉnh
13. Automated tests và regression
```

Migration nhỏ nên đi cùng từng phase khi bắt đầu triển khai. Phase 12 là bước rà soát và hoàn thiện toàn bộ migration trước khi bàn giao.

---

## 17. Definition of Done chung

Một phase chỉ được xem là hoàn thành khi:

- [ ] Entity/DTO/mapper/service/controller nhất quán.
- [ ] Error code và message rõ ràng.
- [ ] Authorization đúng role.
- [ ] Transaction bảo vệ các state transition.
- [ ] Migration không làm mất dữ liệu hiện có.
- [ ] Test happy path và edge case đều pass.
- [ ] Postman/API documentation được cập nhật.
- [ ] Không phá luồng VNPay, registration, contract, report và scoring hiện có.

---

## 18. Các quyết định chưa chốt trước khi code

Các điểm sau cần chốt ngay trước phase tương ứng:

1. Sau T-30, entry thiếu inspection tự scratch hoàn toàn bằng scheduler hay cần head referee xác nhận.
2. Reschedule sau khi đã khám có bắt buộc tái khám hay dựa vào độ lệch thời gian.
3. Khi disqualified, official rank luôn null hay vẫn lưu rank trước khi bị loại.
4. Có đồng bộ result status về `RaceEntryStatus` hay chỉ đọc từ `RaceResult`.
5. Staff được release lúc race start hay lúc race finish.

Các quyết định này không ảnh hưởng việc bắt đầu Phase 1–5.

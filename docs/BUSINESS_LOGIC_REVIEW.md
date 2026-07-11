# Đánh giá nghiệp vụ hệ thống Horse Racing

## 1. Phạm vi rà soát

Tài liệu này tổng hợp kết quả rà soát các luồng nghiệp vụ chính của backend:

- Giải đấu (tournament).
- Đăng ký ngựa và jockey.
- Ghép hợp đồng ngựa - jockey.
- Tạo vòng đấu, cuộc đua và xếp làn.
- Phân công bác sĩ thú y và nhân viên y tế.
- Khám ngựa và jockey trước cuộc đua.
- Bắt đầu cuộc đua và chuyển trạng thái giải đấu.

Kết luận chung: khung nghiệp vụ tổng thể tương đối hợp lý, nhưng luồng khám trước đua hiện chưa hoàn chỉnh. Đặc biệt, hệ thống chưa cấu hình cửa sổ thời gian khám và đang thiếu bước xác nhận kết quả khám, khiến cuộc đua không thể bắt đầu theo đúng điều kiện hiện tại.

---

## 2. Luồng tổng thể hiện tại

Lifecycle của tournament đang được thiết kế như sau:

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

Các mốc thời gian đã có:

- `registrationOpenAt`: mở đăng ký.
- `registrationCloseAt`: đóng đăng ký.
- `reviewDeadlineAt`: hạn duyệt đăng ký.
- `jockeyMatchingDeadlineAt`: hạn ghép jockey với ngựa.
- `schedulingDeadlineAt`: hạn hoàn tất lịch thi đấu.

Thứ tự các mốc này đã được kiểm tra:

```text
registrationOpenAt
< registrationCloseAt
< reviewDeadlineAt
< jockeyMatchingDeadlineAt
< schedulingDeadlineAt
```

Scheduler hiện tự động thực hiện hai chuyển đổi:

- `REGISTRATION_OPEN → REGISTRATION_REVIEW` khi hết thời gian đăng ký.
- `RACING → RESULT_PENDING` khi tất cả race đã hoàn thành.

Các phase còn lại cần admin thao tác thủ công. Điều này hợp lý nếu hệ thống chủ đích để admin kiểm soát quy trình.

---

## 3. Các vấn đề cần ưu tiên xử lý

### 3.1. Chưa cấu hình thời gian khám trước cuộc đua

Hiện tại, điều kiện thời gian duy nhất để tạo kết quả khám là race phải có trạng thái `SCHEDULED`.

Hệ thống chưa kiểm tra:

```text
inspectionOpenAt <= thời điểm hiện tại <= inspectionCloseAt
```

Do đó, nếu lịch được publish trước cuộc đua một tuần thì bác sĩ có thể khám ngay từ thời điểm đó. Kết quả khám quá sớm có thể không còn phản ánh tình trạng sức khỏe tại thời điểm thi đấu.

#### Đề xuất

Thêm cấu hình mặc định ở cấp tournament:

```java
int inspectionOpenMinutesBefore = 180;
int inspectionCloseMinutesBefore = 15;
```

Ý nghĩa:

- Mở khám trước giờ đua 180 phút.
- Đóng khám trước giờ đua 15 phút.

Cửa sổ khám của từng race được tính từ `race.startTime`:

```text
inspectionOpenAt  = race.startTime - inspectionOpenMinutesBefore
inspectionCloseAt = race.startTime - inspectionCloseMinutesBefore
```

Không nhất thiết phải lưu hai datetime này trong database vì có thể tính trực tiếp từ giờ bắt đầu race. Khi admin thay đổi giờ đua, cửa sổ khám cũng tự động thay đổi.

Nếu cần linh hoạt cho từng race, có thể bổ sung hai giá trị override nullable ở entity `Race`.

#### Validation đề xuất

```text
inspectionOpenMinutesBefore > inspectionCloseMinutesBefore >= 0
```

Khi tạo kết quả khám:

```text
Nếu now < inspectionOpenAt  → chưa đến giờ khám.
Nếu now > inspectionCloseAt → đã hết giờ khám.
```

---

### 3.2. Race hiện không thể bắt đầu do thiếu bước confirm kết quả khám

Khi bác sĩ tạo kết quả khám, cả khám ngựa và jockey đều được lưu với trạng thái:

```java
InspectionStatus.SUBMITTED
```

Tuy nhiên, khi bắt đầu race, `RaceServiceImpl.startRace()` yêu cầu cả hai kết quả phải là:

```java
InspectionStatus.CONFIRMED
```

Trong project hiện không có service hoặc controller chuyển kết quả từ:

```text
SUBMITTED → CONFIRMED
```

Vì vậy, kể cả ngựa và jockey đều khám `PASS`, race vẫn không thể bắt đầu.

#### Đề xuất

Nên chọn một trong hai phương án:

**Phương án 1 — Có bước duyệt kết quả, nên dùng**

```text
Bác sĩ khám
→ SUBMITTED
→ Head referee kiểm tra và confirm
→ CONFIRMED
```

Ưu điểm: người khám không tự phê duyệt kết quả của chính mình.

**Phương án 2 — Không có bước duyệt**

Kết quả do đúng bác sĩ được phân công tạo ra sẽ được lưu thẳng thành `CONFIRMED`.

Phương án này đơn giản hơn nhưng enum `SUBMITTED` sẽ không còn nhiều ý nghĩa.

---

### 3.3. Có thể bắt đầu cuộc đua không đúng giờ

`startRace()` hiện chỉ kiểm tra race có trạng thái `SCHEDULED`. Hệ thống chưa so sánh thời điểm hiện tại với `race.startTime`.

Do đó, trọng tài có thể bắt đầu cuộc đua sớm hơn lịch nhiều giờ hoặc nhiều ngày.

#### Đề xuất

Cho phép bắt đầu race trong một khoảng tolerance, ví dụ:

```text
race.startTime - 5 phút <= now <= race.startTime + 30 phút
```

- Trước khoảng cho phép: từ chối vì chưa đến giờ đua.
- Quá muộn: yêu cầu admin cập nhật lịch hoặc đánh dấu race bị hoãn.

Ngoài ra nên lưu cả:

- `startTime`: thời gian dự kiến.
- `startedAt`: thời gian bắt đầu thực tế.

Project hiện đã có hai field này, nên chỉ cần bổ sung validation.

---

### 3.4. Nhân viên khám có thể bị kẹt ở trạng thái ASSIGNED

Khi được phân công vào một race:

- Medical staff chuyển thành `ASSIGNED`.
- Veterinarian chuyển thành `ASSIGNED`.

Project hiện chưa thấy luồng trả nhân viên về `AVAILABLE` khi:

- Hoàn thành khám.
- Race bắt đầu.
- Race kết thúc.
- Race bị hủy.
- Phân công bị xóa.

Điều này có thể khiến nhân viên bị kẹt ở `ASSIGNED` vĩnh viễn.

Ngoài ra, trạng thái toàn cục này không cho phép một nhân viên được phân công cho nhiều race không trùng thời gian.

#### Đề xuất ngắn hạn

Trả nhân viên về `AVAILABLE` sau khi race kết thúc hoặc phân công bị hủy.

#### Đề xuất tốt hơn

Không dùng `ASSIGNED` để thể hiện lịch bận. Thay vào đó, kiểm tra overlap theo thời gian của các assignment:

```text
existingInspectionWindow giao với newInspectionWindow
```

Một nhân viên có thể nhận nhiều race miễn là các cửa sổ khám không trùng nhau.

---

### 3.5. Kết quả FAIL làm scratch entry trước khi được confirm

Hiện tại, khi ngựa hoặc jockey có kết quả `FAIL`, race entry lập tức chuyển thành `SCRATCHED`, trong khi inspection vẫn mang trạng thái `SUBMITTED`.

Điều này không nhất quán nếu hệ thống có bước duyệt kết quả.

#### Đề xuất

Nếu áp dụng workflow có confirm:

```text
FAIL + SUBMITTED
→ chưa scratch chính thức
→ Head referee CONFIRM
→ chuyển entry thành SCRATCHED
```

Nếu kết quả bị từ chối hoặc được sửa trước khi confirm thì entry vẫn có thể tiếp tục tham gia.

---

### 3.6. Một entry chỉ được khám đúng một lần

Hai bảng `horse_inspections` và `jockey_inspections` đang đặt unique constraint theo `entry_id`.

Điều này không hỗ trợ:

- Tái khám.
- Lưu lịch sử kết quả.
- Sửa lỗi nhập sai bằng một bản ghi mới.
- Khám lại khi race bị hoãn.
- Làm mất hiệu lực kết quả khám cũ.

#### Phương án đơn giản

Giữ unique constraint nhưng cho phép update kết quả khi inspection chưa `CONFIRMED`.

#### Phương án đầy đủ

Cho phép nhiều inspection trên một entry và bổ sung:

```text
version
status
supersededAt
supersededBy
```

Khi đó, hệ thống sử dụng kết quả mới nhất đang có hiệu lực.

---

## 4. Luồng khám trước đua được đề xuất

```text
Admin publish lịch race
→ hệ thống tính cửa sổ khám từ race.startTime
→ đến inspectionOpenAt
→ vet khám ngựa
→ medical staff khám jockey
→ kết quả được lưu SUBMITTED
→ head referee xác nhận kết quả
→ PASS + CONFIRMED: entry đủ điều kiện
→ FAIL + CONFIRMED: entry chuyển SCRATCHED
→ đến inspectionCloseAt: đóng cửa sổ khám
→ kiểm tra số entry hợp lệ tối thiểu
→ đến giờ race: trọng tài bắt đầu race
→ race kết thúc: giải phóng nhân viên được phân công
```

---

## 5. Các quy tắc nên bổ sung

### Khi khám

- Race phải là `SCHEDULED`.
- Thời điểm hiện tại phải nằm trong cửa sổ khám.
- Người khám phải được phân công đúng race.
- Entry không được ở trạng thái withdrawn, scratched hoặc disqualified.
- Race chưa được bắt đầu.
- Không được sửa inspection đã `CONFIRMED`, trừ khi có quyền đặc biệt và lưu audit log.

### Khi confirm kết quả

- Chỉ head referee hoặc role được chỉ định mới được confirm.
- Inspection phải đang là `SUBMITTED`.
- Không confirm sau khi race đã bắt đầu.
- Kết quả `FAIL` sau khi confirm sẽ scratch entry.

### Khi bắt đầu race

- Race phải là `SCHEDULED`.
- Phải nằm trong thời gian được phép bắt đầu.
- Người thao tác phải là referee được phân công hoặc head referee.
- Mọi entry còn active phải có cả hai inspection `PASS + CONFIRMED`.
- Handicap phải được xác nhận nếu tournament bật handicap.
- Số entry active phải đạt `minEntries`.

### Khi race bị hoãn

- Cập nhật `startTime` mới.
- Tính lại cửa sổ khám.
- Nếu kết quả cũ quá xa giờ đua mới thì đánh dấu hết hiệu lực và yêu cầu tái khám.

---

## 6. Một số nhận xét kỹ thuật liên quan

### Thiếu test

Project hiện không có source test trong `src/test`. Các luồng trạng thái quan trọng nên có test, đặc biệt:

- Không được khám ngoài cửa sổ.
- Không được confirm hai lần.
- FAIL chỉ scratch sau khi confirm.
- Không được start race khi thiếu inspection.
- Không được start race quá sớm.
- Race đủ inspection PASS thì bắt đầu thành công.
- Nhân viên được release sau khi race hoàn thành.

### Dependency bị khai báo trùng

`pom.xml` đang khai báo `spring-boot-starter-validation` hai lần. Nên xóa một declaration để tránh Maven model warning.

### Hibernate schema update

Project đang dùng:

```properties
spring.jpa.hibernate.ddl-auto=update
```

Phù hợp khi phát triển, nhưng nếu triển khai thật nên dùng migration như Flyway hoặc Liquibase để kiểm soát các thay đổi database, đặc biệt khi thêm field cấu hình thời gian khám.

---

## 7. Thứ tự triển khai đề xuất

1. Bổ sung `inspectionOpenMinutesBefore` và `inspectionCloseMinutesBefore` ở tournament.
2. Validation cửa sổ thời gian trong cả horse inspection và jockey inspection.
3. Bổ sung endpoint confirm inspection hoặc lưu thẳng `CONFIRMED` nếu không cần duyệt.
4. Chỉ scratch entry sau khi kết quả FAIL được confirm.
5. Validation thời gian trong `startRace()`.
6. Xử lý release hoặc kiểm tra lịch trùng của medical staff và veterinarian.
7. Bổ sung update/tái khám và audit log nếu scope cho phép.
8. Viết test cho toàn bộ state transition trên.

---

## 8. Cấu hình khởi đầu được khuyến nghị

Đối với scope đồ án, có thể bắt đầu với:

```text
Mở khám: 180 phút trước giờ đua
Đóng khám: 15 phút trước giờ đua
Cho phép bắt đầu race sớm tối đa: 5 phút
Cho phép bắt đầu race muộn tối đa: 30 phút
Người confirm inspection: head referee
Kết quả FAIL chỉ scratch sau khi confirm
```

Các con số nên được cấu hình thay vì hard-code để sau này dễ điều chỉnh.

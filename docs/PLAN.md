# PLAN — Multi-round Lifecycle và các phần còn thiếu sau review

File này chỉ ghi các đầu việc còn thiếu sau lần review code mới nhất. Không đưa lại những phần đã hoàn thành và đang chạy đúng nếu không cần sửa.

---

## 1. Kết luận hiện trạng

### 1.1. Prediction scoring — đã đúng rule hiện tại

Không cần viết lại công thức chấm điểm:

- TOP3 đúng vị trí: 30 điểm/selection.
- Đúng horse trong TOP3 nhưng sai vị trí: 10 điểm/selection.
- Đúng cả ba horse và đúng cả ba vị trí: cộng perfect bonus 50 điểm.
- Điểm tối đa TOP3: `3 × 30 + 50 = 140`.
- DNF hoặc DISQUALIFIED: selection đó nhận 0 điểm, prediction vẫn chuyển `SCORED`.
- Perfect bonus hiện đã kiểm tra trực tiếp predicted rank với official rank.
- Prediction được chấm khi admin publish RaceReport.

### 1.2. Horse rating — công thức chính đã đúng

Không cần viết lại toàn bộ rating policy:

| Kết quả | Rating change hiện tại |
|---|---:|
| Hạng 1 | +6 đến +12 |
| Hạng 2 | +2 đến +5 |
| Hạng 3 | +1 đến +4 |
| Hạng 4–5 | 0 đến +2 |
| Hạng 6+ | 0 hoặc âm |
| DID_NOT_FINISH | -4 |
| DISQUALIFIED | -6 |

Đã có:

- Bonus theo sức mạnh đối thủ.
- Bonus theo khoảng cách về đích.
- Penalty khi horse có rating cao nhưng thi đấu kém.
- Clamp rating không nhỏ hơn 0.
- Tự động cập nhật `RaceClass` từ rating mới.
- Rating preview khi RaceReport là `Signed`.
- Lưu `HorseRatingHistory` khi publish.
- Chặn apply rating lần hai cho cùng RaceResult.

### 1.3. Multi-round lifecycle — chưa được triển khai

Hiện tại chưa có flow tự động:

```text
Round 1 hoàn thành
→ chọn horse đi tiếp
→ tạo entry cho Round 2
→ Round 2 chuyển sang SCHEDULING
→ admin hoàn thiện và publish lịch Round 2
```

Các vấn đề trong code hiện tại:

1. `advancementRule` mới chỉ được lưu dưới dạng chuỗi, chưa có service thực thi.
2. Không có `RoundAdvancementService`, `RoundLifecycleService` hoặc scheduler xử lý round kế tiếp.
3. Sau khi tạo round, hầu như không có logic cập nhật `Round.status` theo kết quả các race.
4. `publishSchedule(tournamentId)` lấy toàn bộ race của tất cả round, yêu cầu tất cả race đã có entry/referee, rồi chuyển toàn bộ race sang `SCHEDULED` cùng lúc.
5. Round 2 cần kết quả Round 1 mới biết horse đi tiếp, nên không thể bắt tạo entry Round 2 trước khi Round 1 chạy.
6. `RoundStatus` hiện đang được dùng chung cho cả `Round` và `Race`, làm lifecycle của hai entity bị lẫn nhau.

---

## 2. P0 — Chuẩn hóa trạng thái Round và Race

### Mục tiêu

Phân biệt lifecycle của round với lifecycle của từng race.

### Công việc

Khuyến nghị tách enum:

```java
enum RoundStatus {
    PENDING,
    SCHEDULING,
    SCHEDULED,
    ONGOING,
    COMPLETED,
    CANCELLED
}
```

```java
enum RaceStatus {
    SCHEDULING,
    SCHEDULED,
    ONGOING,
    FINISHED,
    COMPLETED,
    CANCELLED
}
```

Nếu chưa muốn migration tách enum ngay, tối thiểu phải bổ sung `PENDING` và không dùng cùng một đoạn logic để chuyển trạng thái Round và Race.

### Trạng thái ban đầu

Khi tạo tournament có nhiều round:

```text
Round có sequenceOrder = 1 → SCHEDULING
Round có sequenceOrder > 1 → PENDING
```

Admin được phép tạo trước skeleton của Round 2 và các race thuộc Round 2, nhưng chưa cần tạo `RaceEntry` cho đến khi Round 1 có kết quả chính thức.

### Tiêu chí hoàn thành

- Round 2 không ở `SCHEDULING` khi Round 1 chưa hoàn thành.
- Chỉ có một round không-final đang hoạt động tại một thời điểm, trừ khi business sau này cho phép chạy song song.
- Race status thay đổi không làm Round status thay đổi sai.
- Có migration cho dữ liệu status cũ nếu tách enum.

---

## 3. P0 — Publish schedule theo từng Round

### Vấn đề hiện tại

API publish schedule cấp tournament đang validate và schedule toàn bộ race của mọi round. Cách này chỉ phù hợp khi toàn bộ entry đã biết từ đầu; không phù hợp với tournament có vòng loại.

### Hướng sửa

Thêm API:

```http
POST /api/admin/rounds/{roundId}/publish-schedule
```

Điều kiện:

1. Round đang `SCHEDULING`.
2. Round trước đó đã `COMPLETED`, trừ Round 1.
3. Mỗi race trong round đủ `minEntries`.
4. Mỗi race có referee.
5. Mỗi race có thời gian hợp lệ và không conflict.
6. Các RaceEntry đã được xác định từ registration hoặc advancement.

Khi publish thành công:

```text
Race trong round: SCHEDULING → SCHEDULED
Round: SCHEDULING → SCHEDULED
```

API cũ:

```http
POST /api/admin/tournaments/{tournamentId}/publish-schedule
```

có thể giữ lại cho tournament chỉ có một round, hoặc đổi thành API điều phối gọi publish Round 1. Không được tiếp tục schedule toàn bộ future round chưa xác định entry.

### Prediction window

- Prediction chỉ mở cho các race thuộc round đã publish schedule.
- Mốc mở chung được tính từ race sớm nhất của round/race card hiện tại.
- Không mở prediction cho Round 2 khi entry Round 2 chưa được xác định.
- Race của Round 1 bắt đầu không ảnh hưởng prediction của các race khác trong chính race card đã publish.

### Tiêu chí hoàn thành

- Publish Round 1 không yêu cầu Round 2 có entry.
- Race Round 2 vẫn chưa nhận prediction khi Round 2 còn `PENDING`.
- Không thể publish Round 2 trước khi Round 1 hoàn thành.

---

## 4. P0 — Tự động hoàn thành Round hiện tại

### Điều kiện hoàn thành Round

Một round được coi là hoàn thành khi tất cả race thuộc round đều ở trạng thái terminal:

```text
COMPLETED hoặc CANCELLED
```

`FINISHED` chưa phải terminal cuối vì RaceReport mới chỉ được ký, admin vẫn chưa publish.

### Thời điểm kiểm tra

Gọi kiểm tra ngay sau:

```text
Admin publish RaceReport
hoặc
Admin cancel race
```

Không nhất thiết phải đợi scheduler nếu có thể xử lý đồng bộ sau transaction nghiệp vụ.

Pseudo flow:

```java
publishRaceReport(raceId)
    → publish report
    → score prediction
    → apply horse rating
    → payout nếu final
    → checkAndCompleteRound(roundId)
```

```java
checkAndCompleteRound(roundId)
    → nếu mọi race COMPLETED/CANCELLED
    → round.status = COMPLETED
    → nếu không phải final: prepareNextRound(roundId)
```

### Idempotency

- Gọi kiểm tra nhiều lần không được tạo entry Round 2 trùng.
- Hai race cuối của cùng round publish gần như đồng thời không được cùng tạo advancement hai lần.
- Nên lock Round hiện tại và Round kế tiếp khi chuyển trạng thái.

### Tiêu chí hoàn thành

- Một race còn `SCHEDULED`, `ONGOING` hoặc `FINISHED` thì round chưa được `COMPLETED`.
- Tất cả race `COMPLETED/CANCELLED` thì round chuyển `COMPLETED` đúng một lần.

---

## 5. P0 — Advancement từ Round 1 sang Round 2

### Vấn đề

`advancementRule` dạng text tự do không đủ an toàn để backend tự chọn horse đi tiếp.

### Hướng dữ liệu đề xuất

Thay vì chỉ dùng text, bổ sung cấu hình có cấu trúc, ví dụ:

```java
AdvancementType advancementType; // TOP_N_PER_RACE hoặc TOP_N_OVERALL
Integer qualifiersPerRace;
Integer totalQualifiers;
```

`advancementRule` có thể giữ lại làm mô tả cho người dùng.

### Luật advancement tối thiểu

Phiên bản đầu nên hỗ trợ:

```text
TOP_N_PER_RACE
```

Ví dụ:

```text
Round 1 có 4 race
qualifiersPerRace = 2
→ lấy hạng 1 và 2 của mỗi race
→ tổng cộng 8 entry vào Round 2
```

Chỉ lấy kết quả:

- RaceReport đã `Published`.
- RaceResult có status `FINISHED`.
- `rank` khác null.
- Entry không DNF hoặc DISQUALIFIED.

### Tạo RaceEntry Round kế tiếp

Khi Round 1 hoàn thành:

1. Xác định danh sách qualifier.
2. Kiểm tra không trùng horse, jockey hoặc contract.
3. Phân qualifier vào các race Round 2 theo rule đã cấu hình.
4. Tạo RaceEntry ở trạng thái `CONFIRMED`.
5. Chưa cần assign lane nếu muốn admin quyết định sau; nếu lane bắt buộc, hệ thống gán tạm rồi admin được chỉnh.
6. Chuyển Round 2 từ `PENDING` sang `SCHEDULING`.
7. Gửi notification cho owner, jockey và admin.

### Trường hợp không đủ qualifier

Nếu số qualifier nhỏ hơn `minEntries` của Round 2:

- Không tự publish Round 2.
- Round 2 vẫn ở `SCHEDULING` hoặc chuyển trạng thái lỗi cần xử lý.
- Thông báo admin chọn phương án: giảm số race, thay đổi seeding hoặc cancel round.

Không tự ý lấy horse đã bị loại để bù vào nếu chưa có rule tie-break/alternate rõ ràng.

### Tiêu chí hoàn thành

- Kết quả Round 1 xác định đúng entry Round 2.
- Không tạo entry từ DNF, DISQUALIFIED, SCRATCHED hoặc withdrawn.
- Retry không tạo duplicate.
- Round 2 chỉ chuyển `SCHEDULING` sau khi Round 1 thực sự `COMPLETED`.

---

## 6. P0 — Sửa `finishedAt` và Tournament `RESULT_PENDING`

### Vấn đề hiện tại

`TournamentPhaseScheduler` kiểm tra:

```text
Race.finishedAt != null
```

nhưng flow ký/publish RaceReport hiện chưa thấy gán `race.finishedAt`. Tournament có thể không bao giờ tự chuyển:

```text
RACING → RESULT_PENDING
```

### Hướng sửa

Theo nghiệp vụ hiện tại, RaceReport được head referee ký là lúc kết quả race được xác nhận và race chuyển `FINISHED`:

```java
signRaceReport()
    → race.status = FINISHED
    → race.finishedAt = now
```

Tuy nhiên điều kiện kết thúc toàn tournament nên dựa vào trạng thái cuối:

```text
Mọi round COMPLETED/CANCELLED
và mọi race COMPLETED/CANCELLED
```

Không chỉ dựa vào `finishedAt`, vì race cancel có thể không có thời điểm finish.

Sau khi final round hoàn thành:

```text
Tournament.phase = RESULT_PENDING
Tournament.status = ONGOING
```

Admin sau đó gọi publish result cấp tournament nếu nghiệp vụ vẫn cần bước này.

### Tiêu chí hoàn thành

- Ký report gán `finishedAt` đúng một lần.
- Race cancelled không chặn tournament kết thúc.
- Tournament chỉ vào `RESULT_PENDING` sau khi final round hoàn thành.

---

## 7. P1 — Cập nhật performance statistics của Horse

### Vấn đề hiện tại

Horse rating và RaceClass đã được cập nhật, nhưng chưa thấy flow cập nhật đầy đủ:

```text
totalRaces
totalWins
totalTop3Finishes
winRate
lastRaceAt
```

Nếu các field này dùng cho profile hoặc eligibility, dữ liệu sẽ bị cũ dù rating đã thay đổi.

### Hướng sửa

Khi publish RaceReport, cùng transaction với rating:

- Mọi horse thực sự xuất phát: `totalRaces + 1`.
- FINISHED rank 1: `totalWins + 1`.
- FINISHED rank 1–3: `totalTop3Finishes + 1`.
- DNF/DISQUALIFIED vẫn tính là đã xuất phát nên tăng `totalRaces`.
- SCRATCHED/withdrawn không tăng `totalRaces`.
- Tính lại `winRate = totalWins / totalRaces × 100`.
- Cập nhật `lastRaceAt` theo thời điểm race thực sự chạy.

Phải có idempotency để publish/retry không tăng statistic hai lần. Có thể dùng chính `HorseRatingHistory` hoặc bảng processing marker để xác định race đã apply.

### Tiêu chí hoàn thành

- Thống kê chỉ tăng đúng một lần.
- DNF/DISQUALIFIED tăng số lần xuất phát nhưng không tăng win/top3.
- Horse SCRATCHED không bị tính đã chạy.

---

## 8. Test cases bắt buộc cho multi-round

### 8.1. Round transition

- [ ] Round 1 có một race chưa publish report → Round 1 chưa completed.
- [ ] Mọi race Round 1 đã completed/cancelled → Round 1 completed.
- [ ] Round 2 chuyển `PENDING → SCHEDULING` đúng một lần.
- [ ] Không có Round 2 → final round hoàn thành và tournament vào `RESULT_PENDING`.

### 8.2. Advancement

- [ ] TOP 2 mỗi race được đưa vào Round 2.
- [ ] DNF không đi tiếp.
- [ ] DISQUALIFIED không đi tiếp.
- [ ] SCRATCHED/withdrawn không đi tiếp.
- [ ] Retry không tạo duplicate RaceEntry.
- [ ] Hai publish đồng thời không advancement hai lần.

### 8.3. Scheduling

- [ ] Publish Round 1 không yêu cầu entry Round 2.
- [ ] Không publish được Round 2 trước khi Round 1 completed.
- [ ] Sau advancement, admin có thể assign lane/referee/staff cho Round 2.
- [ ] Prediction Round 2 chưa mở khi Round 2 còn pending/scheduling.

### 8.4. Existing scoring/rating regression

- [ ] TOP3 perfect vẫn là 140 điểm với config mặc định.
- [ ] DNF/DISQUALIFIED selection nhận 0 nhưng prediction là `SCORED`.
- [ ] Rating chỉ apply khi publish report.
- [ ] Advancement dùng official result đã publish, không dùng rating preview hoặc draft result.

---

## 9. Thứ tự triển khai đề xuất

```text
1. Tách hoặc chuẩn hóa RoundStatus/RaceStatus
2. Sửa finishedAt và terminal-state check
3. Publish schedule theo từng round
4. Tự complete round
5. Thiết kế advancement config có cấu trúc
6. Tạo entry và mở Round kế tiếp
7. Điều chỉnh prediction window theo round
8. Cập nhật Horse performance statistics
9. Viết unit + integration test multi-round
10. Cập nhật Postman collection cho tournament nhiều round
```

---

## 10. Definition of Done

Multi-round chỉ được xem là hoàn thành khi chạy được flow:

```text
Round 1 SCHEDULING
→ publish Round 1
→ Round 1 SCHEDULED
→ chạy và publish toàn bộ RaceReport
→ Round 1 COMPLETED
→ chọn qualifier
→ tạo entry Round 2
→ Round 2 SCHEDULING
→ admin publish Round 2
→ lặp lại đến final
→ final COMPLETED
→ Tournament RESULT_PENDING
```

Prediction scoring và Horse Rating không được tính lại hoặc cộng hai lần trong quá trình chuyển round.

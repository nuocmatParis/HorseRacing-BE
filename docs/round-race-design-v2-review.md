# Phản biện đề xuất Round & Race Design v2

## 1. Kết luận tổng quan

Không nên triển khai nguyên bản thiết kế v2. Hướng tổng thể tốt và đơn giản hơn mô hình hiện tại, nhưng vẫn còn một số điểm chưa thống nhất. Nếu code trực tiếp theo tài liệu thì cây giải vẫn có thể không khả thi và lifecycle nhiều round sẽ khó quản lý.

Các ý tưởng nên giữ lại:

- Bỏ `maxRounds` do Admin nhập thủ công.
- Tự động tính số race từ số entry.
- Phân bổ entry cân bằng.
- Safe fail khi không tìm được cấu trúc phù hợp.
- Khai báo Round template trước.
- Không tự ý thay đổi cấu trúc khi số contract thực tế không phù hợp.

Các phần phải sửa trước khi code:

- Chuẩn hóa ý nghĩa của advancement.
- Sửa thuật toán để kiểm tra toàn bộ cây giải.
- Không bỏ hoàn toàn min/max entry khỏi Round.
- Giữ lifecycle riêng cho Round.
- Tách sức chứa của Final.
- Xử lý actual eligible contracts, DNF/DQ và transition sang round tiếp theo.

---

## 2. `advancementCount` chưa rõ nghĩa

`advancementCount` đang chưa được chốt là:

- Top N của từng race; hoặc
- Top N toàn round.

Đây là dữ liệu cốt lõi của bracket calculator nên không thể để dưới dạng một câu hỏi mở khi bắt đầu code.

Đề xuất thay bằng:

```java
int qualifiersPerRace;
int wildcardSlots;
```

Ý nghĩa:

- `qualifiersPerRace`: Số entry đứng đầu được lấy từ từng race.
- `wildcardSlots`: Số entry tốt nhất toàn round chưa được chọn trực tiếp.

Không nên dùng Top N toàn round làm mặc định vì finish time giữa các race có thể không công bằng khi thời tiết hoặc mặt đường khác nhau.

---

## 3. Ví dụ trong thiết kế đang xác định sai nguyên nhân

Thiết kế đang tính:

```text
Round 1: 3 race × Top 3 = 9 qualifier
Round 2: 1 race × Top 4 = 4 qualifier
Final yêu cầu tối thiểu 8 entry
```

Kết luận Final không đủ entry là đúng. Tuy nhiên, gợi ý tăng advancement của Vòng loại lên 10 là không phù hợp.

Nếu lấy Top 10 mỗi race:

```text
3 race × Top 10 = 30 qualifier
```

Round 1 gần như không loại entry nào.

Cách xử lý đúng phải là một trong các phương án:

- Round 2 lấy Top 8 để Final có tám entry.
- Giảm `finalMinEntries`.
- Thay đổi số race ở Round 1 hoặc Round 2.
- Để bracket calculator tìm một cây giải khác.

---

## 4. Trường hợp actual 28 không thất bại vì thiếu hai contract

Thiết kế cho rằng:

```text
Actual contracts = 28, cần ít nhất 30 để fit chain hiện tại
```

Nhận định này không đúng.

Cả hai trường hợp đều tạo ra chín qualifier sau Round 1:

```text
30 entry → 10, 10, 10 → 3 race × Top 3 = 9
28 entry → 10, 9, 9 → 3 race × Top 3 = 9
```

Cả hai đều thất bại vì Round 2 chỉ lấy Top 4 trong khi Final yêu cầu tối thiểu tám entry.

Nguyên nhân là advancement chain sai, không phải số contract thực tế thấp hơn `maxApprovedHorses`.

---

## 5. Không nên đưa toàn bộ min/max entry lên Tournament

Nếu chỉ có một cặp `minEntry` và `maxEntry` trên Tournament thì tất cả Round và Final phải dùng cùng giới hạn. Cách này quá cứng.

Vòng loại, bán kết và Final có thể cần kích thước race khác nhau. Chính ví dụ Final có bốn entry nhưng Tournament yêu cầu tối thiểu tám đã cho thấy hạn chế này.

Đề xuất dùng policy mặc định:

```java
TournamentBracketPolicy {
    int defaultMinEntriesPerRace;
    int defaultMaxEntriesPerRace;
    int defaultQualifiersPerRace;
    int finalMinEntries;
    int finalMaxEntries;
}
```

Round lưu snapshot hoặc override:

```java
int expectedEntries;
int minEntries;
int maxEntries;
int qualifiersPerRace;
int wildcardSlots;
```

Nếu sau này có `Track` hoặc `Venue`, giới hạn vật lý tối đa nên lấy từ số lane/cổng xuất phát của Track, không nên lấy từ `maxApprovedHorses`.

---

## 6. Mâu thuẫn về thời điểm biết cấu trúc giải

Thiết kế đặt ra hai yêu cầu:

```text
User biết trước số round và cấu trúc
Race count chỉ được tính tại SCHEDULING dựa trên actual contracts
```

Hai yêu cầu này chưa thống nhất. Nếu race count chỉ được biết sau matching thì trước đó user chỉ biết tên Round, chưa biết cấu trúc Race thực tế.

Flow hợp lý hơn:

1. Khi Tournament còn `DRAFT`, tính bracket plan dự kiến bằng `maxApprovedHorses`.
2. Admin chọn một plan hợp lệ.
3. User được xem cấu trúc dự kiến đã chọn.
4. Khi matching kết thúc, backend kiểm tra lại bằng eligible contract thực tế.
5. Nếu plan vẫn phù hợp thì giữ nguyên.
6. Nếu không phù hợp thì đánh dấu plan `STALE` và yêu cầu Admin xác nhận plan mới.

---

## 7. Thuật toán hiện tại chưa kiểm tra toàn chain

Thuật toán v2 trả về `raceCount` đầu tiên có thể chia được entry của Round hiện tại:

```java
return r;
```

Điều đó không bảo đảm Round tiếp theo và Final cũng hợp lệ.

Cần dùng DFS hoặc backtracking:

```text
Thử raceCount của Round hiện tại
→ phân bổ entry
→ tính qualifier
→ thử tạo Round tiếp theo
→ chỉ chấp nhận nếu đi được đến Final
```

Backend nên trả nhiều candidate nếu có:

- `COMPACT`: Ít round và race nhất.
- `BALANCED`: Phân bổ cân bằng, có thể nhiều round hơn.

Không cho Admin nhập trực tiếp số round hoặc race count tùy ý.

---

## 8. Không nên bỏ `Round.status`

Tournament chỉ có một phase toàn cục, ví dụ `RACING`. Với Tournament nhiều round, cùng một thời điểm có thể có:

```text
Round 1 = COMPLETED
Round 2 = SCHEDULING
Round 3 = WAITING
```

Tournament phase không thể biểu diễn đồng thời các trạng thái này. Race status cũng chưa đủ an toàn để quản lý:

- Round nào đang chờ Round trước.
- Round nào được phép xếp lịch.
- Round nào đã hoàn tất transition.
- Chống tạo entry cho Round tiếp theo hai lần.
- Khóa cấu hình của Round đã diễn ra.

Nên giữ lifecycle riêng và tách enum rõ nghĩa:

```java
public enum RoundLifecycleStatus {
    PLANNED,
    WAITING_PREVIOUS_ROUND,
    SCHEDULING,
    SCHEDULED,
    ONGOING,
    RESULT_PENDING,
    COMPLETED,
    CANCELLED
}
```

`RoundStatus` hiện đang được sử dụng cho cả Race và Round nên vấn đề là cần tách enum, không phải xóa hoàn toàn status của Round.

---

## 9. Auto-create Race chưa xử lý lịch và nhân sự

Race không chỉ cần số entry. Mỗi Race còn cần:

- `startTime` và `endTime`.
- Referee.
- Vet và Medical Staff.
- Khoảng cách tối thiểu giữa hai race.
- Giới hạn race trong ngày.
- Không trùng lịch Horse, Jockey và staff.
- Nằm trong thời gian hoạt động của Tournament.

Vì vậy backend chỉ nên tự động tạo skeleton hoặc suggestion:

```text
System tính cần ba race
→ tạo ba race skeleton
→ đề xuất khung giờ
→ Admin xác nhận lịch và phân công
→ publish schedule
```

Không nên tự động tạo Race hoàn chỉnh rồi coi như đã xếp lịch.

---

## 10. Không thể chỉ xóa min/max khỏi Round

Logic hiện tại đang dùng `round.maxEntries` để:

- Kiểm tra lane number.
- Chặn Race vượt sức chứa.

`round.minEntries` đang được dùng để:

- Kiểm tra khi publish schedule.
- Kiểm tra trước khi start Race.

Nếu xóa các field này thì phải thay bằng một nguồn dữ liệu mới, ví dụ:

```java
race.getPlannedMinEntries();
race.getPlannedMaxEntries();
```

hoặc giữ snapshot policy trên Round.

Không thể chỉ xóa field khỏi entity và giữ nguyên các service hiện tại.

---

## 11. Thiếu xử lý DNF, DQ và thiếu qualifier

Công thức:

```text
qualifiedCount = raceCount × qualifiersPerRace
```

chỉ đúng khi mỗi Race có đủ entry `FINISHED`.

Khi có `DID_NOT_FINISH` hoặc `DISQUALIFIED`, cần rule:

1. Lấy Top N entry `FINISHED` của từng Race.
2. Nếu thiếu slot, lấy best non-qualified FINISHED của toàn Round.
3. Có thể sử dụng `wildcardSlots` hoặc reserve list.
4. Nếu vẫn không đủ min entry cho Round sau thì dừng transition.
5. Admin xác nhận phương án gộp hoặc giảm Race nếu nghiệp vụ cho phép.

DNF/DQ không được tự động chiếm suất đi tiếp.

---

## 12. Phải xác định chính xác actual eligible contracts

Không được đếm mọi contract tồn tại trong Tournament.

Eligible contract cần thỏa:

```text
ContractStatus = APPROVED
HorseRegistration = APPROVED
Không CANCELLED
Horse và Jockey đang hợp lệ
Thuộc đúng Tournament
Chưa được gán trùng trong Round
```

Danh sách eligible contract nên được snapshot hoặc khóa tại `completeMatching`. Nếu không, số lượng có thể thay đổi trong lúc backend đang tạo Race và RaceEntry.

---

## 13. Phân bổ entry và tính công bằng

Không cần mỗi Race có số entry bằng nhau tuyệt đối. Chênh lệch tối đa một là hợp lý.

Ví dụ:

```text
28 entry, 3 race → 10, 9, 9
```

Vị trí của Race có 10 entry không quan trọng về số lượng. Tuy nhiên, cách gán contract vào Race cần minh bạch.

Có thể chọn:

- Seeded random để có thể audit và tái tạo kết quả.
- Serpentine seeding theo Horse Rating để cân bằng sức mạnh giữa các Race.
- Bốc thăm được lưu lại seed và thời gian thực hiện.

Lane number nên được random riêng sau khi đã chia group.

---

## 14. Trả lời các câu hỏi trong bản v2

### 14.1. Advancement tính theo Race hay Round?

Nên dùng Top N mỗi Race và đổi tên thành:

```java
qualifiersPerRace
```

Nếu cần chọn thêm entry toàn Round thì dùng:

```java
wildcardSlots
```

### 14.2. Có cho Admin override race count không?

Không cho override tùy ý.

Admin chỉ được:

- Chọn một bracket candidate do hệ thống sinh ra; hoặc
- Thay policy rồi yêu cầu backend tính lại toàn chain.

### 14.3. Có cần Round status không?

Có. Nên tách `RoundLifecycleStatus` khỏi status của Race.

### 14.4. Entry mỗi Race có cần bằng nhau tuyệt đối không?

Không. Chỉ cần:

```text
maxRaceSize - minRaceSize <= 1
```

---

## 15. Thiết kế điều chỉnh đề xuất

### Tournament

```java
int maxApprovedHorses;
Integer plannedRoundCount;
BracketPlanStatus bracketPlanStatus;
Integer bracketPlanVersion;
```

### TournamentBracketPolicy

```java
int defaultMinEntriesPerRace;
int defaultMaxEntriesPerRace;
int defaultQualifiersPerRace;
int finalMinEntries;
int finalMaxEntries;
```

### Round

```java
String roundName;
int sequenceOrder;
boolean isFinal;

int expectedEntries;
int minEntries;
int maxEntries;
int qualifiersPerRace;
int wildcardSlots;

RoundLifecycleStatus lifecycleStatus;
PredictionType predictionType;
```

### Race

Race nên có planned capacity hoặc lấy snapshot từ Round. Nếu mỗi Race có thể khác giới hạn thì dùng:

```java
int plannedMinEntries;
int plannedMaxEntries;
int plannedEntryCount;
```

---

## 16. Flow điều chỉnh

### DRAFT

1. Admin tạo Tournament và bracket policy.
2. Backend sinh các bracket candidate bằng `maxApprovedHorses`.
3. Admin chọn một candidate.
4. Backend lưu `plannedRoundCount` và plan version.
5. Backend tạo skeleton Round/Race hoặc khóa cấu trúc tạo thủ công theo plan.

### REGISTRATION VÀ MATCHING

1. Registration và contract tiếp tục theo flow hiện tại.
2. Tại `completeMatching`, xác định eligible contracts thực tế.
3. Kiểm tra actual count có phù hợp plan đã chọn không.
4. Nếu không phù hợp, chuyển plan sang `STALE` và yêu cầu Admin xác nhận plan mới.

### SCHEDULING

1. Phân bổ contract cân bằng vào Race của Round 1.
2. Random hoặc seed lane number.
3. Admin xếp thời gian và nhân sự.
4. Validate toàn bộ scheduling rule.
5. Publish schedule.

### ROUND TRANSITION

1. Chờ tất cả Race Report trong Round được Published.
2. Chọn qualifier và wildcard từ official result.
3. Tạo RaceEntry cho Round tiếp theo.
4. Chuyển Round tiếp theo sang `SCHEDULING`.
5. Chống transition trùng lặp.

---

## 17. Kết luận cuối

Có thể giữ khoảng 60–70% ý tưởng của bản v2:

- Giữ Round template.
- Bỏ `maxRounds` do Admin nhập.
- Auto-calculate race count.
- Safe fail.
- Phân bổ entry cân bằng.

Những phần cần thay đổi:

- `advancementCount` thành `qualifiersPerRace` và `wildcardSlots`.
- Không bỏ hoàn toàn min/max entry khỏi Round.
- Không bỏ Round lifecycle.
- Dùng DFS/backtracking để kiểm tra toàn chain.
- Tách capacity của Final.
- Revalidate eligible contracts thực tế.
- Chỉ tạo Race skeleton rồi để Admin xác nhận lịch.
- Bổ sung DNF/DQ, reserve, idempotency và transition sang Round tiếp theo.

Chỉ nên bắt đầu implement sau khi chốt các điểm trên.

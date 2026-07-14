# PLAN — Tự động sinh Round/Race theo bracket lũy thừa của 2

## 1. Mục tiêu

Khi tạo Tournament, Admin nhập giới hạn số hồ sơ có thể được duyệt. Giá trị phải là lũy thừa của 2 và bắt đầu từ 8:

```text
8, 16, 32, 64, 128, 256, 512, ...
```

Từ giới hạn này, hệ thống tự suy ra:

- Số Round dự kiến.
- Số Race trong từng Round.
- Số entry tối thiểu và tối đa trong Race đầu tiên.
- Số entry đi tiếp giữa hai Round.
- Race cuối cùng của Tournament.

Admin không nhập `maxRounds` và không tự quyết định số Race tùy ý.

Mô hình phải phân biệt rõ ba nghiệp vụ độc lập:

1. **Top 4 của mỗi Race không phải Final** được đi tiếp.
2. Spectator vẫn chỉ **dự đoán Top 3** của từng Race.
3. Race Final vẫn chỉ xác định **Top 3 chung cuộc nhận giải thưởng**.

Top 4 đi tiếp không đồng nghĩa với dự đoán Top 4 hoặc trao thưởng cho Top 4.

---

## 2. Policy cố định của bracket

Các giá trị nghiệp vụ:

```java
minMaxApprovedEntries = 8;
maxApprovedEntriesMustBePowerOfTwo = true;
minEntriesPerRace = 8;
maxEntriesPerRace = 16;
qualifiersPerRace = 4;
predictionPositions = 3;
finalPrizePositions = 3;
```

Ý nghĩa:

- `maxApprovedEntries`: sức chứa tối đa của Tournament, không phải số hồ sơ bắt buộc phải duyệt đủ.
- `minEntriesPerRace = 8`: một Race hợp lệ phải có ít nhất 8 entry tại thời điểm chốt danh sách.
- `maxEntriesPerRace = 16`: một Race không được có quá 16 entry.
- `qualifiersPerRace = 4`: Race không phải Final lấy bốn entry có official rank cao nhất đi tiếp.
- `predictionPositions = 3`: người dùng chọn hạng 1, hạng 2 và hạng 3 để dự đoán.
- `finalPrizePositions = 3`: chỉ ba vị trí đầu của Race Final được tính giải thưởng chung cuộc.

Các giá trị policy phải được khai báo tập trung, không rải magic number trong service.

---

## 3. Vì sao Top 4 tạo được cây giải ổn định

Hai Race ở Round trước cung cấp entry cho một Race ở Round sau:

```text
Race A lấy Top 4 ─┐
                  ├─> Race C có 8 entry
Race B lấy Top 4 ─┘
```

Do đó:

- Race ở Round đầu có thể có từ 8 đến 16 entry.
- Từ Round thứ hai trở đi, mỗi Race luôn nhận đúng 8 entry.
- Mỗi Round sau có số Race bằng một nửa Round trước.
- Round cuối có đúng một Race với 8 entry, trừ Tournament nhỏ chỉ có một Final ngay từ đầu.

Không cần dùng DFS/backtracking để thử nhiều cây giải. Cấu trúc được suy ra trực tiếp theo policy cố định.

---

## 4. Cấu trúc suy ra từ maxApprovedEntries

| Max approved | Số APPROVED hợp lệ để giữ cấu trúc | Cấu trúc | Tổng Race |
|---:|---:|---|---:|
| 8 | 8 | 1 Final | 1 |
| 16 | 8–16 | 1 Final | 1 |
| 32 | 16–32 | 2 Race vòng loại → 1 Final | 3 |
| 64 | 32–64 | 4 Race vòng đầu → 2 Race bán kết → 1 Final | 7 |
| 128 | 64–128 | 8 → 4 → 2 → 1 Final | 15 |
| 256 | 128–256 | 16 → 8 → 4 → 2 → 1 Final | 31 |
| `2^n` | `2^(n-1)`–`2^n` | Tiếp tục giảm một nửa số Race qua mỗi Round | Tự suy ra |

Bảng trên chỉ là ví dụ minh họa, không phải danh sách giá trị cố định. Mọi lũy thừa của 2 từ 8 trở lên đều hợp lệ.

Với `maxApprovedEntries >= 16`:

```text
firstRoundRaceCount = maxApprovedEntries / maxEntriesPerRace
minimumApprovedRequired = firstRoundRaceCount × minEntriesPerRace
```

Số Race của Round tiếp theo:

```text
nextRoundRaceCount = currentRoundRaceCount / 2
```

Điều kiện để cấu trúc hợp lệ:

```text
actualApprovedEntries >= minimumApprovedRequired
actualApprovedEntries <= maxApprovedEntries
```

Trường hợp `maxApprovedEntries = 8` là trường hợp đặc biệt: phải có đúng 8 entry và Tournament chỉ có một Final.

---

## 5. Ví dụ Tournament sức chứa 64

Admin chọn:

```text
maxApprovedEntries = 64
```

Hệ thống dự kiến:

```text
Round 1: 4 Race
Round 2: 2 Race
Round 3: 1 Final
```

Nếu số APPROVED thực tế là 50, Round 1 được chia:

```text
Race 1: 13 entry
Race 2: 13 entry
Race 3: 12 entry
Race 4: 12 entry
```

Sau khi toàn bộ report của Round 1 được Published:

```text
Top 4 Race 1 + Top 4 Race 2 → Race 5 có 8 entry
Top 4 Race 3 + Top 4 Race 4 → Race 6 có 8 entry
```

Sau khi toàn bộ report của Round 2 được Published:

```text
Top 4 Race 5 + Top 4 Race 6 → Final có 8 entry
```

Final xác định hạng chung cuộc:

```text
Hạng 1 → nhận giải nhất
Hạng 2 → nhận giải nhì
Hạng 3 → nhận giải ba
Hạng 4–8 → không nhận giải chung cuộc
```

Final không lấy Top 4 để đi tiếp vì không còn Round tiếp theo.

---

## 6. Phân bổ entry cân bằng

Với `N` entry và `r` Race ở Round đầu:

```text
baseSize = N / r
remainder = N % r
```

Quy tắc:

- `remainder` Race đầu nhận `baseSize + 1` entry.
- Các Race còn lại nhận `baseSize` entry.
- Chênh lệch số entry giữa hai Race không quá một.
- Không dồn toàn bộ entry dư vào Race cuối.
- Một horse, jockey hoặc contract không được xuất hiện hai lần trong cùng Round.
- Nên dùng seeded shuffle hoặc phương pháp phân bổ theo rating đã chốt để kết quả có thể kiểm tra lại.

Ví dụ:

```text
N = 50
r = 4
baseSize = 12
remainder = 2

Kết quả: 13, 13, 12, 12
```

Không được chia thành:

```text
16, 16, 10, 8
```

Mặc dù từng Race vẫn nằm trong giới hạn 8–16, cách chia này tạo chênh lệch không công bằng.

---

## 7. Kiểm tra khi đóng đăng ký

`maxApprovedEntries` là giới hạn trên. Khi đóng đăng ký, hệ thống phải đếm lại số hồ sơ thực tế ở trạng thái `APPROVED`.

Flow:

1. Đếm chính xác số registration hoặc contract đủ điều kiện tham dự.
2. Kiểm tra số lượng không vượt `maxApprovedEntries`.
3. Tính số Race Round đầu theo cấu trúc đã chọn.
4. Kiểm tra mỗi Race có thể nhận từ 8 đến 16 entry.
5. Nếu hợp lệ, chia entry cân bằng và cho Admin xem preview.
6. Admin xác nhận thì hệ thống mới tạo hoặc chốt Round/Race.
7. Nếu không đủ entry, không tự ý tạo Race dưới 8 entry và không tự đổi cấu trúc giải.

Ví dụ:

```text
maxApprovedEntries = 64
firstRoundRaceCount = 4
minimumApprovedRequired = 4 × 8 = 32
actualApprovedEntries = 30
```

Kết quả:

```text
30 < 32
→ Không đủ để giữ cấu trúc Tournament 64
→ Đề xuất chuyển xuống cấu trúc tối đa 32
→ Chờ Admin xác nhận
```

Sau khi chuyển sang mức 32:

```text
Round đầu có 2 Race: 15 và 15 entry
Top 4 mỗi Race → Final có 8 entry
```

---

## 8. Preview cấu trúc trước khi xác nhận

Khi Admin chọn `maxApprovedEntries`, hệ thống phải hiển thị cấu trúc dự kiến:

```json
{
  "maxApprovedEntries": 64,
  "actualApprovedEntries": 50,
  "minEntriesPerRace": 8,
  "maxEntriesPerRace": 16,
  "qualifiersPerRace": 4,
  "predictionPositions": 3,
  "finalPrizePositions": 3,
  "rounds": [
    {
      "sequenceOrder": 1,
      "raceCount": 4,
      "entriesPerRace": [13, 13, 12, 12],
      "isFinal": false
    },
    {
      "sequenceOrder": 2,
      "raceCount": 2,
      "entriesPerRace": [8, 8],
      "isFinal": false
    },
    {
      "sequenceOrder": 3,
      "raceCount": 1,
      "entriesPerRace": [8],
      "isFinal": true
    }
  ],
  "totalRaceCount": 7
}
```

API đề xuất:

```http
GET /api/admin/tournaments/{tournamentId}/bracket-preview
```

API này chỉ tính và trả preview, chưa tạo Round/Race và chưa cập nhật database.

API xác nhận:

```http
POST /api/admin/tournaments/{tournamentId}/bracket-confirm
```

Sau khi xác nhận, backend tạo skeleton Round/Race. Admin tiếp tục cấu hình lịch, tên Race và nhân sự phụ trách.

---

## 9. Quy tắc Top 4 đi tiếp

Chỉ thực hiện chuyển Round khi:

- Tất cả Race của Round hiện tại đã kết thúc.
- Tất cả Race Report của Round hiện tại đã được `Published`.
- Official result của từng Race đã được xác nhận và không còn được sửa.

Với mỗi Race không phải Final:

1. Lấy các RaceResult có status `FINISHED`.
2. Sắp xếp theo official rank tăng dần.
3. Chọn tối đa bốn entry có rank tốt nhất.
4. `DID_NOT_FINISH` và `DISQUALIFIED` không được đi tiếp.
5. Tạo `RaceEntry` cho Round tiếp theo.
6. Chuyển Round tiếp theo sang `SCHEDULING` đúng một lần.

Việc chuyển Round phải idempotent, không được tạo trùng entry khi API hoặc scheduler chạy lại.

### Trường hợp không đủ Top 4 FINISHED

Nếu một Race không có đủ bốn entry `FINISHED` do DNF hoặc disqualified:

- Không lấy DNF/DQ để lấp chỗ.
- Không tự động lấy entry hạng thấp từ Race khác nếu chưa có rule chính thức.
- Tạm dừng chuyển Round.
- Thông báo cho Admin xử lý theo policy reserve/wildcard được bổ sung sau.

Không nên tự động thay đổi cây giải vì việc này ảnh hưởng tính công bằng và lịch đã công bố.

---

## 10. Dự đoán Top 3 của từng Race

Prediction hoạt động độc lập với quy tắc Top 4 đi tiếp.

Mỗi spectator dự đoán:

```text
Vị trí 1
Vị trí 2
Vị trí 3
```

Quy tắc:

- Mỗi Race có prediction riêng, kể cả vòng loại, bán kết và Final.
- Spectator không dự đoán entry hạng 4.
- Khi report của Race được Published, hệ thống chấm dự đoán dựa trên official Top 3 của Race đó.
- Entry hạng 4 có thể đi tiếp nhưng không thuộc kết quả dùng để tính prediction Top 3.
- DNF/DQ không có official Top 3 nên prediction detail chọn entry đó nhận 0 điểm.
- Prediction của Race trước không tự động áp dụng cho Race sau.

Ví dụ kết quả một Race:

```text
Hạng 1: Horse A
Hạng 2: Horse B
Hạng 3: Horse C
Hạng 4: Horse D
```

Kết quả nghiệp vụ:

- A, B, C và D đều đi tiếp nếu đây không phải Final.
- Prediction chỉ chấm theo A, B và C.
- Horse D không tạo điểm cho một vị trí dự đoán nào.

---

## 11. Giải thưởng chung cuộc Top 3

Giải thưởng tiền chỉ được xử lý khi:

- Race thuộc Round có `isFinal = true`.
- Round Final chỉ có đúng một Race.
- Final Race Report đã được `Published`.
- Kết quả đã trở thành official result và không còn được sửa.

Chỉ ba entry đầu Final nhận giải:

```text
Official rank 1 → First prize
Official rank 2 → Second prize
Official rank 3 → Third prize
```

Official rank 4 vẫn có ý nghĩa kết quả thi đấu nhưng:

- Không đi tiếp vì đây là Final.
- Không nhận giải thưởng chung cuộc.
- Không làm thay đổi việc spectator chỉ dự đoán Top 3.

Các Race không thuộc Round Final:

- Không payout giải thưởng chung cuộc.
- Chỉ publish kết quả.
- Chấm prediction của spectator.
- Chọn Top 4 để tạo Round tiếp theo.

Payout phải chống thực hiện hai lần và chỉ chạy từ Final Race Report đã Published.

---

## 12. Trạng thái và dữ liệu nên lưu

Tournament nên lưu:

```java
Integer maxApprovedEntries;
Integer plannedRoundCount;
Integer plannedRaceCount;
BracketPlanStatus bracketPlanStatus;
Integer bracketPlanVersion;
```

Enum đề xuất:

```java
public enum BracketPlanStatus {
    NOT_GENERATED,
    PREVIEWED,
    CONFIRMED,
    STALE,
    LOCKED
}
```

Round nên lưu snapshot:

```java
Integer expectedEntries;
Integer plannedRaceCount;
Integer qualifiersPerRace; // 4 nếu không phải Final, 0 nếu là Final
Boolean isFinal;
```

Không dùng `advancementRule` dạng text làm nguồn dữ liệu để chuyển Round. Có thể giữ field này để hiển thị mô tả, nhưng backend phải dùng field số có cấu trúc.

---

## 13. Validation bắt buộc

### Khi tạo hoặc cập nhật Tournament

- `maxApprovedEntries >= 8`.
- `maxApprovedEntries` phải là lũy thừa của 2.
- Có thể kiểm tra bằng điều kiện `value > 0 && (value & (value - 1)) == 0` sau khi đã kiểm tra `value >= 8`.
- Không nhận `maxRounds` từ Admin.
- Không cho Admin sửa trực tiếp `plannedRoundCount`.

### Khi xác nhận bracket

- Số APPROVED thực tế nằm trong khoảng hợp lệ của cấu trúc.
- Mỗi Race Round đầu có 8–16 entry.
- Chênh lệch entry giữa hai Race không quá một.
- Số Race mỗi Round sau bằng một nửa Round trước.
- Mỗi Race Round sau nhận đúng 8 qualifier.
- Round cuối có đúng một Final.

### Khi publish lịch

- Bracket đã `CONFIRMED`.
- Round/Race thực tế khớp bracket version.
- Không có Round/Race thừa hoặc thiếu.
- Tất cả Race đáp ứng các rule lịch thi đấu hiện có.
- Sau khi publish, cấu trúc chuyển sang `LOCKED`.

### Khi chuyển Round

- Report của toàn bộ Race trong Round đã Published.
- Chỉ lấy official result.
- Mỗi Race cung cấp đúng Top 4 FINISHED.
- Không tạo entry trùng.
- Không chạy transition hai lần.

### Khi payout

- Chỉ Race duy nhất thuộc Final Round được payout.
- Chỉ official Top 3 được nhận giải.
- Không payout Race vòng loại hoặc bán kết.
- Không payout hai lần.

---

## 14. Error code đề xuất

```text
INVALID_MAX_APPROVED_ENTRIES
BRACKET_NOT_PREVIEWED
BRACKET_NOT_CONFIRMED
BRACKET_PLAN_STALE
BRACKET_PLAN_LOCKED
APPROVED_ENTRIES_BELOW_BRACKET_MINIMUM
APPROVED_ENTRIES_EXCEED_MAXIMUM
RACE_ENTRIES_OUT_OF_RANGE
ROUND_STRUCTURE_MISMATCH
RACE_STRUCTURE_MISMATCH
NEXT_ROUND_NOT_ENOUGH_QUALIFIERS
ROUND_REPORTS_NOT_FULLY_PUBLISHED
ROUND_TRANSITION_ALREADY_COMPLETED
FINAL_ROUND_MUST_HAVE_ONE_RACE
PRIZE_PAYOUT_ONLY_ALLOWED_FOR_FINAL
PRIZE_PAYOUT_ALREADY_COMPLETED
```

---

## 15. Kế hoạch triển khai

### Phase 1 — Chuẩn hóa cấu hình Tournament

- Cho phép mọi `maxApprovedEntries` là lũy thừa của 2 và từ 8 trở lên.
- Bỏ `maxRounds` khỏi request tạo/cập nhật Tournament.
- Khai báo tập trung policy 8–16 entry và Top 4 đi tiếp.
- Thêm các field trạng thái bracket vào Tournament.

### Phase 2 — Bracket calculator và preview

- Tính số Round/Race trực tiếp từ mức sức chứa.
- Chia số APPROVED thực tế cân bằng vào Round đầu.
- Thêm API preview không ghi database.
- Viết unit test cho các mức 8, 16, 32, 64, 128, 256 và các giá trị không phải lũy thừa của 2.

### Phase 3 — Xác nhận và sinh Round/Race

- Thêm API xác nhận bracket.
- Tạo skeleton Round/Race theo plan đã xác nhận.
- Chặn Admin tạo Round/Race ngoài plan.
- Khóa bracket sau khi publish.

### Phase 4 — Đóng đăng ký

- Đếm lại số APPROVED thực tế.
- Kiểm tra khoảng hợp lệ của cấu trúc.
- Nếu thiếu entry, đề xuất giảm xuống mức lũy thừa 2 phù hợp gần nhất.
- Chỉ thay đổi sau khi Admin xác nhận.

### Phase 5 — Chuyển Round bằng Top 4

- Chờ toàn bộ report của Round được Published.
- Lấy Top 4 FINISHED của mỗi Race.
- Ghép qualifier từ hai Race vào một Race kế tiếp.
- Tạo RaceEntry và chuyển Round tiếp theo sang `SCHEDULING`.
- Chống transition trùng lặp.

### Phase 6 — Tách biệt prediction và prize

- Giữ prediction Top 3 cho từng Race.
- Chấm prediction bằng official Top 3 khi report được Published.
- Chỉ payout official Top 3 của Race Final.
- Xác nhận Race hạng 4 chỉ đi tiếp ở Round không phải Final và không nhận giải chung cuộc.

---

## 16. Tiêu chí hoàn thành

1. Admin được nhập mọi `maxApprovedEntries` là lũy thừa của 2 và từ 8 trở lên.
2. Admin không nhập thủ công `maxRounds`.
3. Hệ thống tự suy ra đúng số Round và Race.
4. Round đầu có 8–16 entry mỗi Race và được chia cân bằng.
5. Từ Round thứ hai, mỗi Race có đúng 8 entry do hai Race trước cung cấp Top 4.
6. Chỉ Top 4 FINISHED của Race không phải Final được đi tiếp.
7. Prediction của mọi Race vẫn chỉ chấm Top 3.
8. Chỉ Top 3 của Race Final nhận giải thưởng chung cuộc.
9. Race không phải Final không payout giải thưởng chung cuộc.
10. Nếu số APPROVED không đủ cho cấu trúc, hệ thống đề xuất mức thấp hơn và chờ Admin xác nhận.
11. DNF/DQ không được dùng làm qualifier tự động.
12. Round transition và prize payout không thể chạy hai lần.
13. Bracket đã publish không thể thay đổi cấu trúc.

---

## 17. Các quyết định cần xác nhận trước khi sửa code

Phần này ghi lại kết quả audit implementation hiện tại và phương án sửa đề xuất. Chưa thực hiện sửa code cho đến khi từng quyết định được xác nhận.

Nguyên tắc tổng quát được đề xuất:

```text
Bracket chỉ quyết định cấu trúc Round/Race.
Scheduling mới quyết định ngày giờ thi đấu.
```

### 17.1. Hợp nhất giới hạn số entry

#### Hiện trạng

Tournament đang có đồng thời:

```java
maxApprovedEntries;
maxApprovedHorses;
```

- Quy trình duyệt horse registration dùng `maxApprovedHorses`.
- Bracket calculator dùng `maxApprovedEntries`.
- Hai field có thể mang giá trị khác nhau và tạo bracket không khả thi.

Ví dụ:

```text
maxApprovedEntries = 64
maxApprovedHorses = 30

Bracket 64 cần tối thiểu 32 entry,
nhưng hệ thống chỉ cho duyệt tối đa 30 horse.
```

#### Đề xuất

- Dùng `maxApprovedEntries` làm nguồn dữ liệu duy nhất cho sức chứa Tournament.
- Horse registration APPROVED không được vượt `maxApprovedEntries`.
- `actualApprovedEntries` được tính từ số contract `APPROVED` hợp lệ sau matching.
- Bỏ `maxApprovedHorses` khỏi create/update request và dần loại bỏ khỏi entity/database.
- Giữ `maxApprovedJockeys` nếu vẫn cần quản lý số jockey đăng ký, nhưng số contract cuối cùng phải đủ horse và jockey duy nhất theo rule của Round.

#### Cần xác nhận

```text
[ ] Đồng ý chỉ giữ maxApprovedEntries.
[ ] Muốn giữ cả hai field và thêm ràng buộc khác.
Trả lời:
```
bạn phải cái này là max đơn đăng ký ngựa ấy là 64 thì jockey ít nhất cũng phải 80 đồ cho horse owner đồ dễ chọn bạn hiểu kh
kh thì nếu lỡ 64 ngựa mà 64 jockey sao được 
---

### 17.2. Tách bracket khỏi scheduling

#### Hiện trạng

`confirmBracket()` đang vừa tạo cấu trúc vừa tự gán thời gian Race. Các thời gian mặc định được sinh cách nhau 30 phút, duration 25 phút, không phù hợp rule duration 30 phút và interval 35 phút.

#### Đề xuất

Khi confirm bracket chỉ tạo skeleton:

```text
Round
Race name
Race sequenceOrder
Race status = PLANNING hoặc SCHEDULING
```

Không tự gán giờ thi đấu giả.

Phương án đề xuất:

- Cho `Race.startTime` và `Race.endTime` nullable khi Race mới là skeleton.
- Admin xếp giờ sau khi bracket được confirm.
- Khi Admin nhập `startTime`, backend tự tính:

```text
endTime = startTime + defaultRaceOperationalMinutes
```

- Chỉ khi publish schedule mới bắt buộc mọi Race có đủ ngày giờ hợp lệ.

Phương án thay thế:

- Confirm bracket chỉ tạo Round và lưu `plannedRaceCount`.
- Chưa tạo Race.
- Khi bước vào scheduling, hệ thống tạo đúng số Race theo plan.

#### Cần xác nhận

```text
[ ] Tạo sẵn Race skeleton và cho startTime/endTime nullable. Khuyến nghị.
[ ] Chỉ tạo Round, đến scheduling mới tạo Race.
[ ] Muốn hệ thống tự xếp lịch hợp lệ hoàn toàn.

Trả lời:
```
khi mà admin nhập số 64 đi, thì hệ thống tự động là có bao nhiêu race round thôi, còn lịch là admin tự lên 
---

### 17.3. Xử lý bracket STALE sau matching

#### Hiện trạng

Ví dụ:

```text
Admin chọn maxApprovedEntries = 64
Matching hoàn tất có 30 contract APPROVED
Bracket 64 cần tối thiểu 32
→ Bracket chuyển STALE
```

Code hiện tại có thể đề xuất 32 nhưng Admin không có đường cập nhật và confirm lại hợp lệ sau khi Tournament không còn DRAFT.

#### Đề xuất

API confirm nhận request:

```http
POST /api/admin/tournaments/{id}/bracket-confirm
```

```json
{
  "maxApprovedEntries": 32,
  "expectedPlanVersion": 1
}
```

Flow:

```text
Bracket 64 chuyển STALE
→ Preview đề xuất 32
→ Admin xác nhận 32
→ Backend revalidate số contract
→ Regenerate skeleton
→ bracketPlanVersion tăng 1
→ bracketPlanStatus = CONFIRMED
→ Tournament chuyển SCHEDULING
```

Không tự động giảm bracket khi chưa có xác nhận của Admin.

Chỉ API bracket được phép đổi `maxApprovedEntries` sau khi Tournament đã publish; API update Tournament thông thường vẫn bị chặn.

#### Cần xác nhận

```text
[ ] Admin phải xác nhận mức đề xuất mới. Khuyến nghị.
[ ] Hệ thống tự động giảm về mức phù hợp gần nhất.
[ ] Phương án khác.

Trả lời:
```
[ ] Admin phải xác nhận mức đề xuất mới. Khuyến nghị
---

### 17.4. Khóa cấu trúc sau khi confirm bracket

#### Hiện trạng

Sau khi bracket đã CONFIRMED, API Round/Race cũ vẫn có thể làm cấu trúc lệch plan khi Tournament còn DRAFT:

- Xóa hoặc tạo thêm Round.
- Đổi `isFinal`.
- Đổi `sequenceOrder`.
- Đổi `maxRaces`, `minEntries`, `maxEntries`.
- Xóa hoặc tạo thêm Race.
- Đổi `maxApprovedEntries` nhưng không chuyển plan sang STALE.

#### Đề xuất

Khi bracket là `CONFIRMED` hoặc `LOCKED`, Admin không được thay đổi topology:

- Số Round.
- Số Race mỗi Round.
- `sequenceOrder`.
- `isFinal`.
- `qualifiersPerRace`.
- `minEntriesPerRace = 8`.
- `maxEntriesPerRace = 16`.

Admin chỉ được sửa thông tin vận hành:

- Tên Round/Race.
- Ngày giờ Race.
- Distance và track condition.
- Referee, veterinarian và medical staff.

Tạo hàm dùng chung:

```java
validateBracketStructure(tournament);
```

Hàm phải chạy trước khi publish Tournament và publish schedule, kiểm tra:

```text
Số Round = plannedRoundCount
Round sequence liên tục
Chỉ Round cuối isFinal
Số Race từng Round = plannedRaceCount
Round sau có một nửa số Race của Round trước
Final có đúng một Race
Round thường qualifiersPerRace = 4
Final qualifiersPerRace = 0
minEntries = 8
maxEntries = 16
Bracket version của Round khớp Tournament
```

#### Cần xác nhận

```text
[ ] Khóa topology ngay sau confirm bracket. Khuyến nghị.
[ ] Chỉ khóa sau khi publish Tournament.
[ ] Cho phép sửa topology nhưng bắt buộc regenerate toàn bộ plan.

Trả lời:
```
[ ] Khóa topology ngay sau confirm bracket, khóa luôn chỉ cho chỉnh ngày g của round, race đồ thôi bạn hiểu không
không cho phép thêm race hay round nữa, chỉ chỉnh ngày giờ thôi 
---

### 17.5. Validation lịch khi publish schedule

#### Hiện trạng

Race được tạo trực tiếp trong `confirmBracket()` nên bỏ qua scheduling validation của `RaceService`. `publishSchedule()` chưa revalidate đầy đủ daily limit, duration, interval và operating hours.

#### Đề xuất

Trước khi publish từng Round phải kiểm tra lại toàn bộ:

```text
Race đã có startTime/endTime
endTime = startTime + defaultRaceOperationalMinutes
Không vượt maxRacesPerDay
Nằm trong operating hours
Không overlap break
Đủ minRaceIntervalMinutes với mọi Race khác
Không conflict horse/jockey/referee/vet/medical
Đúng thứ tự Round
Đúng bracket structure và version
```

Ví dụ mặc định:

```text
Race 1: 08:00–08:30
minRaceIntervalMinutes = 35
Race 2 bắt đầu sớm nhất: 09:05
```

Với bracket lớn như 256, Round đầu có 16 Race. Do `maxRacesPerDay = 9`, lịch phải chia qua nhiều ngày nếu khoảng ngày của Tournament cho phép.

Không đặt trần nghiệp vụ cố định cho `maxApprovedEntries`; tính khả thi thực tế phụ thuộc:

```text
Tournament date range
maxRacesPerDay
operating hours
duration
interval
break
minRoundGapDays
```

Nếu không đủ slot, preview hoặc scheduling phải báo Tournament không đủ thời gian để tổ chức bracket đã chọn.

#### Cần xác nhận

```text
[ ] Admin tự xếp lịch, backend validate khi lưu và publish. Khuyến nghị.
[ ] Backend tự đề xuất slot, Admin xác nhận.
[ ] Backend tự xếp lịch hoàn toàn.

Trả lời:
```
Admin tự xếp lịch, backend validate khi lưu và publish. Khuyến nghị, hm cái này lúc mà admin lên lịch ấy, chọn 
max aprove vào ấy thì hệ thống validate cái thời gian để báo cho admin vd, nghiệp vụ quy định race cuối cùng của
round phải cách round sau ít nhất 7 ngày, rồi từ cái số lượng max mà admin nhập vào thì phải tinh là ít nhất bao nhiêu ngày ch tính thời gian để duyệt
---

### 17.6. Chuyển Top 4 phải atomic

#### Hiện trạng

Code hiện tạo entry cho từng cặp Race ngay lập tức. Nếu cặp đầu đủ Top 4 nhưng cặp sau thiếu, Round tiếp theo bị tạo entry dở dang. Lần chạy sau thấy đã có entry nên không xử lý tiếp.

#### Đề xuất

Chia transition thành hai giai đoạn trong cùng transaction.

Giai đoạn 1 — Validate toàn bộ:

```text
Lock Round hiện tại
Tất cả Race đã COMPLETED
Tất cả report đã Published
Round sau có đúng một nửa số Race
Mỗi Race hiện tại có đủ Top 4 FINISHED
DNF/DQ không được đi tiếp
Không trùng horse/jockey/contract trong Round sau
```

Giai đoạn 2 — Apply toàn bộ:

```text
Tạo tất cả RaceEntry của Round sau
Đánh dấu Round hiện tại đã advance
Chuyển Round sau sang SCHEDULING
Chuyển Tournament sang SCHEDULING
Cập nhật currentRoundName
```

Nếu một Race thiếu Top 4:

```text
Không insert bất kỳ entry nào
Đánh dấu transition bị block
Gửi notification cho Admin
Chờ reserve/wildcard policy
```

Thêm vào Round:

```java
LocalDateTime advancedAt;
RoundTransitionStatus transitionStatus;
```

Enum đề xuất:

```java
NOT_READY,
READY,
COMPLETED,
BLOCKED_NOT_ENOUGH_QUALIFIERS
```

Transition dùng pessimistic lock và `advancedAt` để chống chạy đồng thời hoặc chạy hai lần. Không dùng việc “Round sau đã có một vài entry” làm dấu hiệu hoàn thành.

#### Cần xác nhận

```text
[ ] Dừng toàn bộ transition nếu bất kỳ Race nào thiếu Top 4. Khuyến nghị.
[ ] Cho lấy reserve từ Race khác.
[ ] Cho Round sau chạy dưới 8 entry.
[ ] Phương án khác.

Trả lời:
```
Tất cả Race đủ Top 4
→ Tạo toàn bộ Round sau
→ Chuyển sang SCHEDULING

Chỉ một Race thiếu Top 4
→ Không tạo entry nào
→ Đánh dấu BLOCKED
→ Báo Admin
---

### 17.7. Tournament phase giữa hai Round

#### Hiện trạng

Sau khi Round 1 hoàn tất, code chỉ chuyển Round 2 sang `SCHEDULING` nhưng Tournament vẫn là `RACING`. API publish schedule lại yêu cầu Tournament đang `SCHEDULING`, nên giải có thể bị kẹt sau Round 1.

#### Đề xuất

Flow trạng thái:

```text
Publish schedule Round 1
→ Tournament RACING

Tất cả report Round 1 Published và transition thành công
→ Tournament SCHEDULING
→ Round 2 SCHEDULING

Publish schedule Round 2
→ Tournament RACING
```

`TournamentPhaseScheduler` không tự đổi `currentRoundName` chỉ vì Race đã `FINISHED`. Phải chờ report `Published` và transition Top 4 hoàn thành.

`RaceReportService` nên là nguồn chính thực hiện chuyển Round. Scheduler chỉ retry hoặc phát hiện dữ liệu bị kẹt, không tự bỏ qua điều kiện nghiệp vụ.

#### Cần xác nhận

```text
[ ] Dùng flow SCHEDULING ↔ RACING cho từng Round. Khuyến nghị.
[ ] Giữ Tournament luôn RACING và sửa publishSchedule cho phép publish Round sau.
[ ] Phương án khác.

Trả lời:
```
Dùng flow SCHEDULING ↔ RACING cho từng Round. Khuyến nghị
---

### 17.8. Sửa expectedEntries

#### Hiện trạng

Final đang bị hard-code:

```text
expectedEntries = 8
```

Nhưng Tournament mức 16 có thể chạy thẳng Final với 8–16 entry. Ngoài ra Round 1 được confirm theo sức chứa tối đa nhưng không cập nhật lại expected entry sau matching.

#### Đề xuất

Khi sinh Round từ preview:

```text
expectedEntries = tổng entriesPerRace của RoundPreviewDto
```

Sau matching:

- Round 1 dùng `actualApprovedEntries`.
- Các Round sau dùng `raceCount × 8`.
- Với Tournament mức 16, Final dùng đúng actual entry từ 8 đến 16.

#### Cần xác nhận

```text
[ ] expectedEntries phản ánh số entry thực tế sau matching. Khuyến nghị.
[ ] expectedEntries chỉ phản ánh sức chứa tối đa theo plan.

Trả lời:
```
expectedEntries phản ánh số entry thực tế sau matching
---

### 17.9. Cách phân bổ entry Round đầu

#### Hiện trạng

Số lượng được chia cân bằng đúng, nhưng contract và Race đang được lấy theo thứ tự database không xác định. Chưa có seeded shuffle hoặc rating seeding.

#### Đề xuất khuyến nghị — Serpentine theo Horse Rating

1. Chốt snapshot Horse Rating tại thời điểm phân bảng.
2. Sắp xếp rating giảm dần.
3. Nếu bằng rating, dùng contract ID làm tie-break ổn định.
4. Phân bổ theo đường zíc-zắc.

Ví dụ bốn Race:

```text
Lượt 1: Race 1 → Race 2 → Race 3 → Race 4
Lượt 2: Race 4 → Race 3 → Race 2 → Race 1
Lặp lại cho đến hết
```

Ưu điểm:

- Phân tán ngựa mạnh.
- Không phụ thuộc thứ tự đăng ký.
- Chạy lại cho cùng kết quả.
- Có thể audit.

Phương án khác — Seeded shuffle:

- Trộn ngẫu nhiên bằng seed.
- Lưu seed để có thể tái hiện kết quả.
- Mang tính bốc thăm hơn nhưng không chủ động cân bằng rating.

#### Cần xác nhận

```text
[ ] Serpentine theo Horse Rating. Khuyến nghị.
[ ] Seeded shuffle.
[ ] Admin tự phân bảng.
[ ] Phương án khác.

Trả lời:
```
] Serpentine theo Horse Rating
---

### 17.10. Đơn giản hóa BracketPlanStatus

#### Hiện trạng

`PREVIEWED` không được sử dụng. API preview là GET và theo rule không được cập nhật database. `bracketPlanVersion` hiện cũng chưa được tăng khi regenerate.

#### Đề xuất

Sử dụng flow:

```text
NOT_GENERATED
→ CONFIRMED
→ STALE
→ CONFIRMED
→ LOCKED
```

- `NOT_GENERATED`: chưa confirm bracket.
- `CONFIRMED`: skeleton đang khớp plan version hiện tại.
- `STALE`: số entry hoặc cấu hình làm plan không còn hợp lệ.
- `LOCKED`: schedule Round đầu đã được publish.

Bỏ `PREVIEWED` và `BRACKET_NOT_PREVIEWED` vì preview là read-only.

Mỗi lần confirm hoặc regenerate:

```text
bracketPlanVersion++
```

Round lưu snapshot:

```java
Integer bracketPlanVersion;
```

#### Cần xác nhận

```text
[ ] Bỏ PREVIEWED và dùng flow bốn trạng thái. Khuyến nghị.
[ ] Giữ PREVIEWED và cho preview cập nhật database.

Trả lời:
```
Bỏ PREVIEWED và dùng flow bốn trạng thái
---

### 17.11. Thời điểm kiểm tra actualApprovedEntries

#### Hiện trạng

PLAN cũ ghi kiểm tra tại thời điểm đóng registration. Tuy nhiên ở thời điểm đó chưa hoàn tất jockey matching nên chưa có danh sách contract APPROVED cuối cùng.

#### Đề xuất

Chia thành hai mốc:

```text
Đóng registration:
→ Chỉ chốt hồ sơ horse/jockey và ngừng nhận đơn mới

Hoàn tất matching:
→ Đếm contract APPROVED thực tế
→ Validate bracket
→ Phân bổ Round 1 hoặc chuyển STALE
```

Đây là thời điểm phù hợp hơn để xác định `actualApprovedEntries` chính thức.

#### Cần xác nhận

```text
[ ] Kiểm tra bracket chính thức sau completeMatching. Khuyến nghị.
[ ] Kiểm tra ngay khi đóng registration bằng số horse APPROVED.

Trả lời:
```
Kiểm tra bracket chính thức sau completeMatching
---

### 17.12. Bộ test bắt buộc trước khi coi là hoàn thành

Đề xuất bổ sung:

1. Power of 2 hợp lệ: 8, 16, 32, 64, 128, 256.
2. Giá trị sai: 7, 12, 24, 100.
3. Actual entry đúng biên `M/2`, `M`, dưới minimum và vượt maximum.
4. 50 entry chia thành `13, 13, 12, 12`.
5. Bracket 64 có 30 contract → STALE → Admin confirm 32.
6. Tournament mức 16 có Final từ 8 đến 16 entry.
7. Chặn sửa/xóa topology sau confirm.
8. Schedule tuân thủ duration, interval, operating hours và daily limit.
9. Bracket lớn được chia lịch qua nhiều ngày.
10. Chuyển Round thành công với Top 4.
11. Một Race thiếu Top 4 thì không insert bất kỳ entry nào.
12. DNF/DQ không đi tiếp.
13. Transition gọi hai lần không tạo trùng.
14. Hai request publish report đồng thời không tạo transition trùng.
15. Sau Round 1, Tournament chuyển về SCHEDULING và publish được Round 2.
16. Prediction vẫn chỉ chấm Top 3.
17. Chỉ Top 3 Final được payout.

#### Cần xác nhận

```text
[ ] Viết đủ unit test và integration test cho các case trên.
[ ] Chỉ viết các test ưu tiên: STALE, transition và publish Round 2.

Trả lời:
```

---

## 18. Thứ tự sửa code đề xuất

Sau khi các quyết định tại mục 17 được xác nhận, triển khai theo thứ tự:

1. Hợp nhất `maxApprovedEntries` và sửa registration validation.
2. Tách bracket topology khỏi scheduling time.
3. Khóa cấu trúc Round/Race theo bracket version.
4. Sửa flow `STALE → Admin confirm → regenerate`.
5. Sửa transition Top 4 thành atomic và idempotent.
6. Sửa Tournament phase giữa các Round.
7. Sửa `expectedEntries` và entry seeding.
8. Revalidate toàn bộ lịch tại publish schedule.
9. Bổ sung migration.
10. Viết unit/integration test end-to-end.

Các file dự kiến chịu ảnh hưởng:

- `TournamentServiceImpl.java`.
- `RoundServiceImpl.java`.
- `RaceServiceImpl.java`.
- `RaceReportServiceImpl.java`.
- `TournamentRegistrationServiceImpl.java`.
- `TournamentPhaseScheduler.java`.
- Tournament/Round/Race entity, request, response và repository.
- Database migration.
- Bracket, scheduling và round-transition tests.

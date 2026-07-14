# PLAN — Tự động đề xuất cây giải Round/Race

## 1. Mục tiêu

Khi Admin tạo Tournament, Admin chỉ cấu hình giới hạn hồ sơ được duyệt và policy tổ chức race. Hệ thống phải tự tính các cây giải Round/Race hợp lệ để Admin lựa chọn.

Hệ thống cần bảo đảm:

1. Không cho Admin nhập số round tùy ý.
2. Không tạo cây giải mà round sau thiếu entry.
3. Admin xem được số race, số entry và số người đi tiếp ở từng round trước khi xác nhận.
4. Cấu hình bracket được lưu riêng theo từng Tournament, không hard-code trong `application.properties`.
5. Số hồ sơ APPROVED thực tế được kiểm tra lại trước khi đóng đăng ký.
6. Sau khi round hoàn tất, hệ thống chọn đúng qualifier và tạo entry cho round tiếp theo.

---

## 2. Vấn đề hiện tại

Tournament đang cho Admin cấu hình `maxApprovedHorses` và `maxRounds` độc lập. Hai giá trị có thể tạo ra một cây giải không khả thi.

Ví dụ:

```text
maxApprovedHorses = 30
Round 1 có 3 race
Mỗi race lấy Top 3

Số entry đi tiếp = 3 × 3 = 9
```

Nếu Round 2 có hai race và mỗi race cần tối thiểu năm entry:

```text
Round 2 cần tối thiểu = 2 × 5 = 10 entry
Round 1 chỉ tạo ra = 9 entry
```

Cấu hình này phải bị chặn.

Các hạn chế trong code hiện tại:

- `maxRounds` do Admin nhập thủ công.
- `advancementRule` là text nên backend không thể dùng để tính người đi tiếp.
- `minEntries` và `maxEntries` của Round đang được dùng như giới hạn entry trên từng race.
- Chưa kiểm tra khả năng phân bổ entry giữa hai round liên tiếp.
- Chưa tự động tạo entry và chuyển round tiếp theo sang `SCHEDULING`.

---

## 3. Quyết định nghiệp vụ

- Bỏ `maxRounds` khỏi request tạo và cập nhật Tournament.
- Admin không được nhập trực tiếp số round tối đa.
- Số round được suy ra từ bracket plan do hệ thống tính.
- Admin chọn một plan hợp lệ, không tự nhập `plannedRoundCount`.
- Nếu cần lưu số round trong database thì dùng `plannedRoundCount`, chỉ backend được cập nhật.
- Không cho tạo round có `sequenceOrder` vượt quá `plannedRoundCount`.
- Không cho publish Tournament nếu cấu trúc Round/Race không khớp plan đã chọn.
- Sau khi Tournament được publish, cấu trúc bracket bị khóa.

---

## 4. TournamentBracketPolicy

Cấu hình có thể thay đổi theo từng giải phải được lưu trong database, không đặt trong `application.properties`.

Tạo entity one-to-one với Tournament:

```java
TournamentBracketPolicy {
    UUID policyId;
    Tournament tournament;

    int minEntriesPerRace;
    int maxEntriesPerRace;
    int defaultQualifiersPerRace;
    int finalMinEntries;
    int finalMaxEntries;
}
```

Tournament bổ sung:

```java
Integer plannedRoundCount;
BracketPlanStatus bracketPlanStatus;
Integer bracketPlanVersion;
```

Enum đề xuất:

```java
public enum BracketPlanStatus {
    NOT_GENERATED,
    GENERATED,
    SELECTED,
    STALE,
    LOCKED
}
```

Ý nghĩa:

- `NOT_GENERATED`: Chưa tính bracket.
- `GENERATED`: Đã có đề xuất nhưng Admin chưa chọn.
- `SELECTED`: Admin đã chọn một plan hợp lệ.
- `STALE`: Cấu hình đã thay đổi, plan cũ không còn hiệu lực.
- `LOCKED`: Tournament đã publish, không được thay cấu trúc bracket.

---

## 5. Cấu hình trên Round

Policy của Tournament được dùng để sinh đề xuất. Khi chọn plan, từng Round lưu snapshot:

```java
int expectedEntries;
int minEntries;
int maxEntries;
int qualifiersPerRace;
int wildcardSlots;
```

`advancementRule` có thể giữ lại để hiển thị mô tả, nhưng không được dùng làm nguồn dữ liệu chính. Backend phải dùng `qualifiersPerRace` và `wildcardSlots` dạng số.

Nếu Admin được phép sửa policy của một Round khi Tournament còn `DRAFT`, backend phải:

1. Đánh dấu bracket plan hiện tại thành `STALE`.
2. Tính lại Round đang sửa và toàn bộ Round phía sau.
3. Yêu cầu Admin chọn và xác nhận lại plan.

---

## 6. Thuật toán tìm bracket plan

Đầu vào:

```text
N = số entry dự kiến
min = minEntriesPerRace
max = maxEntriesPerRace
k = qualifiersPerRace
wildcards = wildcardSlots
```

Số race hợp lệ của một round:

```text
minimumRaceCount = ceil(N / max)
maximumRaceCount = floor(N / min)
```

Nếu:

```text
minimumRaceCount > maximumRaceCount
```

thì không thể phân bổ `N` entry theo policy hiện tại.

Với mỗi số race hợp lệ `r`:

```text
qualifiedCount = min(N, r × k + wildcards)
```

Điều kiện bắt buộc:

- Entry được chia cân bằng, chênh lệch giữa hai race không quá một.
- Mỗi race có số entry nằm trong `[min, max]`.
- `k` phải nhỏ hơn số entry của từng race.
- `qualifiedCount` phải nhỏ hơn `N`, trừ Final.
- Nếu `qualifiedCount` nằm trong `[finalMinEntries, finalMaxEntries]`, round tiếp theo có thể là Final với đúng một race.
- Nếu chưa tạo được Final thì tiếp tục tính với `N = qualifiedCount`.

Backend nên dùng DFS hoặc backtracking để thử các `raceCount` hợp lệ. Không chỉ chọn phương án đầu tiên vì một số race hợp lệ ở round hiện tại có thể làm round sau không thể chia.

Không sử dụng Java Stream trong logic calculator mới.

---

## 7. Các phương án đề xuất

Một Tournament có thể có nhiều bracket hợp lệ. Backend nên trả tối thiểu:

- `COMPACT`: Ít round và ít race nhất.
- `BALANCED`: Chia entry cân bằng và có thể nhiều round hơn.

Ví dụ policy:

```text
maxApprovedHorses = 30
minEntriesPerRace = 5
maxEntriesPerRace = 10
defaultQualifiersPerRace = 3
Final cho phép 6–10 entry
```

### 7.1. COMPACT

```text
Round 1:
3 race: 10, 10, 10
Top 3 mỗi race → 9

Final:
1 race: 9

plannedRoundCount = 2
```

### 7.2. BALANCED

```text
Round 1:
4 race: 8, 8, 7, 7
Top 3 mỗi race → 12

Round 2:
2 race: 6, 6
Top 3 mỗi race → 6

Final:
1 race: 6

plannedRoundCount = 3
```

Admin chỉ được chọn một phương án hợp lệ. Nếu muốn thay đổi số round, Admin phải thay policy và để hệ thống tính lại.

---

## 8. API đề xuất

### 8.1. Lấy bracket suggestion

```http
GET /api/admin/tournaments/{tournamentId}/bracket-suggestions
```

Response:

```json
{
  "maxApprovedHorses": 30,
  "policy": {
    "minEntriesPerRace": 5,
    "maxEntriesPerRace": 10,
    "defaultQualifiersPerRace": 3,
    "finalMinEntries": 6,
    "finalMaxEntries": 10
  },
  "suggestions": [
    {
      "planCode": "COMPACT",
      "roundCount": 2,
      "rounds": [
        {
          "sequenceOrder": 1,
          "expectedEntries": 30,
          "raceCount": 3,
          "entriesPerRace": [10, 10, 10],
          "qualifiersPerRace": 3,
          "qualifiedEntries": 9,
          "final": false
        },
        {
          "sequenceOrder": 2,
          "expectedEntries": 9,
          "raceCount": 1,
          "entriesPerRace": [9],
          "final": true
        }
      ]
    }
  ]
}
```

### 8.2. Chọn bracket plan

```http
POST /api/admin/tournaments/{tournamentId}/bracket-plans/select
```

```json
{
  "planCode": "BALANCED",
  "planVersion": 1
}
```

Backend nên tạo skeleton Round/Race từ plan đã chọn. Admin chỉ bổ sung:

- Ngày giờ.
- Tên Round/Race.
- Referee và staff.
- Các thông tin vận hành khác.

Cách này an toàn hơn việc để Admin tự tạo toàn bộ Round/Race rồi mới kiểm tra.

---

## 9. Quy tắc tạo và sửa Round

Khi tạo Round:

1. Tournament phải còn `DRAFT`.
2. Tournament phải có bracket plan `SELECTED`.
3. `sequenceOrder` không vượt `plannedRoundCount`.
4. Round cuối bắt buộc `isFinal = true`.
5. Round chưa cuối không được `isFinal = true`.
6. Số race và expected entry phải khớp plan.
7. Không tạo thêm Round khi đã đủ `plannedRoundCount`.

Nếu thay đổi một trong các giá trị sau thì plan thành `STALE`:

- `maxApprovedHorses`.
- `minEntriesPerRace` hoặc `maxEntriesPerRace`.
- `qualifiersPerRace` hoặc `wildcardSlots`.
- Số race.
- Cấu trúc Final.

Admin phải sinh và chọn lại plan trước khi tiếp tục.

---

## 10. Validation khi publish Tournament

Chỉ cho publish khi:

- Bracket plan đang `SELECTED`.
- Số Round thực tế bằng `plannedRoundCount`.
- Từng Round/Race khớp plan version hiện tại.
- Round cuối là Final.
- Final có đúng một race.
- Số entry đi tiếp từ mỗi round đủ cho round sau.
- Không tồn tại Round/Race dư ngoài plan.

Sau khi publish:

```text
bracketPlanStatus = LOCKED
```

Không cho sửa:

- `maxApprovedHorses`.
- Bracket policy.
- Số Round/Race.
- Quy tắc đi tiếp.
- Cấu trúc Final.

Reschedule chỉ thay đổi lịch thi đấu, không làm thay đổi bracket.

---

## 11. Kiểm tra lại khi đóng đăng ký

Plan ban đầu được tính theo `maxApprovedHorses`, nhưng số hồ sơ `APPROVED` thực tế có thể thấp hơn.

Khi đóng đăng ký:

1. Đếm chính xác Horse registration `APPROVED`.
2. Thử phân bổ số entry thực tế vào Round 1.
3. Mỗi race phải nằm trong `[minEntries, maxEntries]`.
4. Nếu plan vẫn hợp lệ thì phân bổ cân bằng và tiếp tục.
5. Nếu không hợp lệ thì không tự ý thay bracket.
6. Sinh bracket suggestion mới theo số APPROVED thực tế.
7. Admin xác nhận plan mới rồi mới được đóng đăng ký.

Ví dụ:

```text
Round 1 theo plan có 4 race
minEntriesPerRace = 5
Số entry tối thiểu = 4 × 5 = 20

APPROVED thực tế = 18
→ Không đủ để giữ plan bốn race
→ Sinh đề xuất mới và chờ Admin xác nhận
```

---

## 12. Phân bổ entry cân bằng

Với `N` entry và `r` race:

```text
baseSize = N / r
remainder = N % r
```

- `remainder` race đầu nhận `baseSize + 1` entry.
- Các race còn lại nhận `baseSize` entry.
- Chênh lệch giữa các race không quá một.
- Có thể shuffle bằng seed hoặc bốc thăm để tránh ưu tiên theo thứ tự đăng ký.
- Một horse, jockey hoặc contract không được xuất hiện hai lần trong cùng round.

Ví dụ:

```text
N = 30
r = 4
baseSize = 7
remainder = 2

Kết quả: 8, 8, 7, 7
```

---

## 13. Chuyển entry sang Round tiếp theo

Chỉ xử lý khi toàn bộ Race Report của round hiện tại đã `Published`.

Flow:

1. Kiểm tra tất cả race trong round đã `COMPLETED` và report đã `Published`.
2. Với mỗi race, lấy `qualifiersPerRace` entry có official rank cao nhất và trạng thái `FINISHED`.
3. `DID_NOT_FINISH` và `DISQUALIFIED` không được lấy trực tiếp.
4. Nếu có `wildcardSlots`, chọn entry FINISHED tốt nhất chưa được chọn trong toàn round.
5. Nếu thiếu slot vì DNF/DQ, tiếp tục lấy best non-qualified FINISHED làm reserve.
6. Nếu vẫn không đủ số entry tối thiểu, không tự động bắt đầu round tiếp theo.
7. Cảnh báo Admin và yêu cầu xác nhận phương án gộp hoặc giảm race nếu được phép.
8. Tạo `RaceEntry` cho round tiếp theo theo cách phân bổ cân bằng.
9. Chuyển round tiếp theo và các race sang `SCHEDULING`.

Phải chống chạy transition hai lần khi report hoặc request được gọi lại.

---

## 14. Error code đề xuất

```text
BRACKET_POLICY_INVALID
BRACKET_PLAN_NOT_GENERATED
BRACKET_PLAN_NOT_SELECTED
BRACKET_PLAN_STALE
BRACKET_PLAN_LOCKED
BRACKET_PLAN_NOT_FEASIBLE
ROUND_COUNT_EXCEEDS_PLANNED
ROUND_STRUCTURE_MISMATCH
RACE_STRUCTURE_MISMATCH
APPROVED_ENTRIES_NOT_FIT_BRACKET
NEXT_ROUND_NOT_ENOUGH_QUALIFIERS
ROUND_TRANSITION_ALREADY_COMPLETED
```

---

## 15. Phase triển khai

### Phase 1 — Chuẩn hóa dữ liệu

- Bỏ `maxRounds` khỏi `CreateTournamentRequest` và `UpdateTournamentRequest`.
- Không cho Admin cập nhật trực tiếp số round.
- Tạo `TournamentBracketPolicy`.
- Thêm `plannedRoundCount`, `bracketPlanStatus`, `bracketPlanVersion` vào Tournament.
- Thêm `expectedEntries`, `qualifiersPerRace`, `wildcardSlots` vào Round.
- Giữ `advancementRule` làm mô tả hoặc xóa sau khi FE đã chuyển sang field có cấu trúc.

### Phase 2 — Bracket calculator

- Tạo `BracketPlanningService`.
- Sinh các race count hợp lệ tại từng round.
- Dùng DFS/backtracking tìm cây giải hoàn chỉnh.
- Trả các plan `COMPACT` và `BALANCED`.
- Viết unit test cho các số entry biên.

### Phase 3 — Chọn và áp dụng plan

- Thêm API lấy suggestion.
- Thêm API chọn plan kèm `planVersion`.
- Tạo skeleton Round/Race từ plan.
- Chặn Round/Race vượt cấu trúc.

### Phase 4 — Publish và đóng đăng ký

- Validate toàn bộ bracket khi publish Tournament.
- Khóa policy sau publish.
- Khi đóng đăng ký, kiểm tra số APPROVED thực tế.
- Nếu không phù hợp thì sinh plan mới và chờ Admin xác nhận.

### Phase 5 — Tự động chuyển Round

- Chỉ chuyển khi tất cả report của round hiện tại đã Published.
- Chọn qualifier và wildcard theo official result.
- Tạo entry cho round tiếp theo.
- Chuyển round tiếp theo sang `SCHEDULING`.
- Chống transition trùng lặp.

---

## 16. Tiêu chí hoàn thành

1. Admin không còn nhập `maxRounds` khi tạo Tournament.
2. Hệ thống trả được plan hợp lệ hoặc lỗi rõ ràng từ `maxApprovedHorses` và bracket policy.
3. Admin xem được số entry, số race và số người đi tiếp ở từng round.
4. Không tạo được round vượt `plannedRoundCount`.
5. Không publish được Tournament có cây giải không khả thi.
6. Thay đổi policy làm plan cũ thành `STALE`.
7. Tournament đã publish không thể thay đổi cấu trúc bracket.
8. Số APPROVED thực tế được kiểm tra lại khi đóng đăng ký.
9. Entry được chia cân bằng, chênh lệch không quá một.
10. Chỉ official result đã Published được dùng để chọn người đi tiếp.
11. DNF/DQ không tự động chiếm suất đi tiếp.
12. Round tiếp theo chỉ chuyển sang `SCHEDULING` đúng một lần.

# Đề xuất: Hệ thống chia Round & Race

## 1. Vấn đề hiện tại

- `minEntry` / `maxEntry` để ở Round, nhưng Round template không biết trước totalContracts → khó validate
- `maxRaces` trên Round là nhập tay, không tự động
- Round có `status` nhưng không dùng (lifecycle qua tournament phase)
- Khi matching xong mới biết số contract → có thể không fit với config đã tạo

## 2. Nguyên tắc thiết kế

| Yêu cầu | Lý do |
|---|---|
| User biết trước số round + cấu trúc | Đặt cược, theo dõi giải |
| Mỗi race có **từ 8 đến 12** entry (VD) | Sân đua có sức chứa, không để 1-2 ngựa chạy lẻ |
| `maxApprovedHorses` kiểm soát quy mô | Giới hạn sức chứa sân |
| Round template khai báo từ DRAFT | Admin + user đều biết trước |
| Race count **auto-tính** tại SCHEDULING | Dựa trên actual contracts |
| **Safe fail**: nếu không fit → báo lỗi + gợi ý | Không tự ý thay đổi |

## 3. Thay đổi Entity

### Tournament (thêm)
```java
int minEntry;       // VD: 8 — mỗi race tối thiểu
int maxEntry;       // VD: 10 — mỗi race tối đa
// BỎ: maxRounds (auto-tính)
```

### Round (sửa)
```java
String roundName;             // "Vòng loại"
int sequenceOrder;            // 1, 2, 3...
Integer advancementCount;     // VD: 4, null nếu final
PredictionType predictionType;
// BỎ: minEntries, maxEntries, maxRaces, status
```

## 4. Flow hoạt động

### DRAFT: Admin khai báo + System validate ngay

Admin tạo tournament với `maxApprovedHorses=30`, `minEntry=8`, `maxEntry=10`.

Admin tạo 3 round templates:
- Round 1: "Vòng loại" — adv = 3
- Round 2: "Bán kết" — adv = 4
- Round 3: "Chung kết" — final

**System validate ngay lúc DRAFT** (dùng maxApprovedHorses = 30):

```
Round 1 (adv=3):
  30/10 = 3 races (10,10,10) ✓ → advance = 3×3 = 9
Round 2 (adv=4):
  9/10 = 1 race (9) ✓ (9 trong [8,10]) → advance = 4
Round 3 (final):
  4/10 = 1 race (4) ❌ (4 < minEntry=8)
→ FAIL! Báo lỗi + gợi ý tăng adv Vòng loại lên 10
```

### SCHEDULING: Execute với actual contracts

Giả sử actual = 28 contracts (thay vì 30):

```
Round 1 (adv=3):
  28/10 = 3 races (10, 9, 9) ✓ → advance = 3×3 = 9
Round 2 (adv=4):
  9/10 = 1 race (9) ✓ → advance = 4
Round 3 (final):
  4/10 = 1 race (4) ❌ (4 < minEntry=8)
→ FAIL! Báo lỗi: "Actual contracts = 28, cần ≥ 30 để fit chain hiện tại."
```

## 5. Thuật toán phân phối entries vào races

```java
// Với total entries = 28, minEntry = 8, maxEntry = 10
// Tìm raceCount khả thi:
for (int r = ceil(total / maxEntry); r <= floor(total / minEntry); r++) {
    int base = total / r;      // VD: 28/3 = 9
    int remainder = total % r;  // VD: 28%3 = 1
    // races - 1 race có 9 entries, 1 race có 10 entries
    if (base >= minEntry && (base + (remainder > 0 ? 1 : 0)) <= maxEntry) {
        return r;  // ✅ Hợp lệ
    }
}
```

Sau khi có raceCount, shuffle contracts + distribute bằng base+remainder, gán lane numbers random.

## 6. Câu hỏi cho nhóm

1. **advancementCount** là "top N mỗi race" hay "top N overall mỗi round"?
   - Nếu top N/race → advance = N × raceCount (dễ hiểu, dễ tính)
   - Nếu top N/round → advance = N (ít advance hơn, có thể cần nhiều round hơn)

2. **Có nên cho admin override raceCount** nếu auto-tính không ưng ý?
   - Hay bắt buộc sửa template (advancementCount) để system tính lại?

3. **Cần Round.status không?** Tournament phase + Race.status đã đủ quản lý lifecycle chưa?

4. **Số entry mỗi race có cần bằng nhau tuyệt đối không?**
   - VD: 28 entries → (9, 9, 10) OK? Hay phải (10, 9, 9)?

---

Sau khi thống nhất, implement gồm:
- Sửa Tournament, Round entity + DTOs + Mappers
- TournamentService — validate chain ở DRAFT
- TournamentService — auto-calculate + create races ở SCHEDULING
- RaceEntryService — autoAssignRound (giữ nguyên logic cũ)
- Bỏ Round.status, Round.minEntries, Round.maxEntries, Round.maxRaces

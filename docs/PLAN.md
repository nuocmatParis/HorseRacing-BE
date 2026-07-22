# CÂU HỎI CẦN CHỐT — TIMELINE TẠO TOURNAMENT

## 1. Hiện trạng code đã kiểm tra

Hiện tại hệ thống đang hiểu:

- `startDate`: ngày bắt đầu thi đấu, tức ngày có Race đầu tiên.
- `endDate`: ngày cuối cùng được phép tổ chức Race Final.
- Các mốc `registrationOpenAt`, `registrationCloseAt`, `reviewDeadlineAt`, `jockeyMatchingDeadlineAt`, `schedulingDeadlineAt` là giai đoạn chuẩn bị và phải diễn ra trước Race đầu tiên.
- BE mới chỉ kiểm tra thứ tự:

```text
registrationOpenAt
< registrationCloseAt
< reviewDeadlineAt
< jockeyMatchingDeadlineAt
< schedulingDeadlineAt
```

- BE chưa kiểm tra thời lượng tối thiểu của từng giai đoạn.
- BE chưa bắt buộc `schedulingDeadlineAt` phải trước Race đầu tiên.
- FE đang gợi ý timeline cố định bằng cách trừ `28, 14, 10, 6, 2` ngày từ `startDate`; chưa phụ thuộc `maxApprovedEntries`.
- Việc kiểm tra `endDate` hiện đã mô phỏng số Round, số Race, khung giờ đua và số Race tối đa mỗi ngày.

## 2. Câu hỏi 1 — Ý nghĩa của startDate

Có giữ nguyên định nghĩa sau không?

```text
startDate = ngày thi đấu của Round đầu tiên
endDate   = ngày muộn nhất Race Final phải kết thúc
```

Nếu giữ định nghĩa này thì thời gian đăng ký, duyệt hồ sơ, ghép jockey và xếp lịch không được cộng vào `endDate`. Các khoảng này dùng để xác định ngày bắt đầu sớm nhất có thể chọn.

Phương án đề xuất:

```text
[ ] Đồng ý: startDate là ngày đua đầu tiên.
[ ] Không đồng ý: startDate là ngày bắt đầu toàn bộ Tournament, bao gồm cả đăng ký.
```

Trả lời:
Không đồng ý: startDate là ngày bắt đầu toàn bộ Tournament, thôi chưa bao gồm đăng ký.
## 3. Câu hỏi 2 — Có chặn startDate quá gần hiện tại không?

Nếu `startDate` chỉ cần không ở quá khứ, Admin có thể chọn hôm nay. Khi đó timeline đăng ký, duyệt, ghép jockey và xếp lịch được tính ngược từ `startDate` sẽ rơi vào quá khứ.

Cần chọn một trong hai policy:

```text
[ ] A. Chặn startDate quá gần. startDate phải đủ xa để hoàn thành toàn bộ timeline chuẩn bị. Khuyến nghị.
[ ] B. Chỉ yêu cầu startDate không ở quá khứ; timeline chỉ là gợi ý và có thể rơi vào quá khứ.
```

Nếu chọn A, lỗi nên báo vào `startDate`:
Start day là ngày mà admin bắt đầu tạo cái giải này này, chìnhs là hôm mà admin tạo giải luôn ấy
## 4. Câu hỏi 3 — Mốc dùng để bắt đầu tính timeline chuẩn bị

Cần xác định `registrationOpenAt` được lấy từ đâu.

Phương án đề xuất:

```text
[ ] A. Lấy thời điểm hiện tại hoặc ngày kế tiếp lúc 08:00, rồi tính tiến về startDate. Khuyến nghị.
[ ] B. Admin tự chọn registrationOpenAt, hệ thống tính các deadline tiếp theo.
[ ] C. Tính ngược toàn bộ mốc từ startDate.
```

Khuyến nghị dùng B khi Admin cần chủ động ngày công bố giải. Sau khi Admin chọn `registrationOpenAt` và `maxApprovedEntries`, hệ thống sinh các deadline và ngày bắt đầu sớm nhất.

Trả lời:
Admin tự chọn registrationOpenAt, hệ thống tính các deadline tiếp theo
## 5. Câu hỏi 4 — Thời gian mở đăng ký theo maxApprovedEntries

Công thức đề xuất:

```text
registrationDays = 3 + log2(maxApprovedEntries / 8)
```

| maxApprovedEntries | Thời gian đăng ký đề xuất |
|---:|---:|
| 8 | 3 ngày |
| 16 | 4 ngày |
| 32 | 5 ngày |
| 64 | 6 ngày |
| 128 | 7 ngày |
| 256 | 8 ngày |
| 512 | 9 ngày |

Mỗi lần sức chứa tăng gấp đôi thì thời gian đăng ký tăng một ngày. Công thức tiếp tục áp dụng với các lũy thừa của 2 lớn hơn.

```text
[ ] Đồng ý công thức trên.
[ ] Muốn dùng bảng/công thức khác.
```

Trả lời:
Đồng ý công thức trên.
## 6. Câu hỏi 5 — Thời gian duyệt hồ sơ

Đề xuất:

```text
reviewDeadlineAt = registrationCloseAt + 4 ngày
```

Cần xác nhận 4 ngày là:

```text
[ ] 4 ngày theo lịch, gồm thứ Bảy và Chủ nhật. Khuyến nghị cho đồ án.
[ ] 4 ngày làm việc, bỏ thứ Bảy và Chủ nhật.
```

Trả lời:
4 ngày theo lịch, gồm thứ Bảy và Chủ nhật. Khuyến nghị cho đồ án.
## 7. Câu hỏi 6 — Thời gian ghép jockey theo sức chứa

Tại lúc tạo Tournament chưa có số ngựa APPROVED thực tế. Vì vậy hệ thống chỉ có thể gợi ý dựa trên `maxApprovedEntries`.

Công thức đề xuất:

```text
jockeyMatchingDays = 2 + log2(maxApprovedEntries / 8)
```

| maxApprovedEntries | Thời gian ghép jockey đề xuất |
|---:|------------------------------:|
| 8 |                        3 ngày |
| 16 |                        5 ngày |
| 32 |                        6 ngày |
| 64 |                        7 ngày |
| 128 |                        8 ngày |
| 256 |                        9 ngày |
| 512 |                       10 ngày |

Mốc được tính như sau:

```text
jockeyMatchingDeadlineAt = reviewDeadlineAt + jockeyMatchingDays
```

```text
[ ] Đồng ý dùng maxApprovedEntries và công thức trên.
[ ] Muốn dùng số ngày/công thức khác.
```

Trả lời:
dùng số tôi đa ghi ở trên đó
## 8. Câu hỏi 7 — Hạn hoàn tất lên lịch

Đề xuất theo yêu cầu đã nêu:

```text
schedulingDeadlineAt = jockeyMatchingDeadlineAt + 4 ngày
```

Cần xác nhận sau hạn hoàn tất lịch có cần thêm thời gian đệm trước Race đầu tiên không:

```text
[ ] A. Thêm 2 ngày đệm: startDate sớm nhất = schedulingDeadlineAt + 2 ngày. Khuyến nghị.
[ ] B. Thêm 1 ngày đệm.
[ ] C. Không thêm ngày đệm; Race đầu tiên có thể chạy ngay sau khi hoàn tất lịch.
```

Trả lời:
A. Thêm 2 ngày đệm: startDate sớm nhất = schedulingDeadlineAt + 2 ngày. Khuyến nghị.
## 9. Câu hỏi 8 — Gợi ý hay validation bắt buộc?

Cần tách hai mức xử lý:

1. Hệ thống tự điền các mốc đề xuất trên FE.
2. BE quyết định Admin có được sửa ngắn hơn đề xuất hay không.

Phương án đề xuất:

```text
[ ] A. Các khoảng thời gian trên là tối thiểu bắt buộc; Admin chỉ được kéo dài. Khuyến nghị.
[ ] B. Chỉ là gợi ý; Admin được sửa ngắn hơn, BE chỉ kiểm tra đúng thứ tự.
```

Trả lời:
Các khoảng thời gian trên là tối thiểu bắt buộc; Admin chỉ được kéo dài. Khuyến nghị.
## 10. Câu hỏi 9 — Khi đổi maxApprovedEntries

Ví dụ Admin đã chọn 16 entry rồi bấm tạo timeline, sau đó đổi lên 64 entry.

Phương án đề xuất:

```text
[ ] Tự tính lại toàn bộ timeline và minimum startDate, nhưng hiện cảnh báo trước khi ghi đè. Khuyến nghị.
[ ] Giữ nguyên các mốc cũ và chỉ báo lỗi cho Admin sửa thủ công.
```

Trả lời:
Tự tính lại toàn bộ timeline và minimum startDate, nhưng hiện cảnh báo trước khi ghi đè. Khuyến nghị.
## 11. Câu hỏi 10 — Cách tính endDate tối thiểu theo Round

Policy hiện tại là khoảng nghỉ chỉ tồn tại giữa hai Round liên tiếp.

Ví dụ `maxApprovedEntries = 32`:

```text
Round 1: 2 Race
Khoảng nghỉ: 7 ngày
Round 2: 1 Final
```

Nếu Round 1 diễn ra ngày 14/07 thì Final sớm nhất diễn ra ngày 21/07. Như vậy `endDate` tối thiểu là ngày Final kết thúc, không phải cộng hai lần 7 ngày.

Ví dụ tổng quát, nếu toàn bộ Race của mỗi Round chạy hết trong một ngày:

| maxApprovedEntries | Cấu trúc Race | Số Round | Ngày Final sớm nhất với gap 7 ngày |
|---:|---|---:|---:|
| 8 | 1 | 1 | startDate |
| 16 | 1 | 1 | startDate |
| 32 | 2 → 1 | 2 | startDate + 7 ngày |
| 64 | 4 → 2 → 1 | 3 | startDate + 14 ngày |
| 128 | 8 → 4 → 2 → 1 | 4 | startDate + 21 ngày |

Nếu một Round không thể chạy hết trong một ngày thì hệ thống phải tính từ thời điểm kết thúc Race cuối cùng của Round đó.

```text
[ ] Đồng ý cách tính trên. Khuyến nghị.
[ ] Mỗi Round, kể cả Round đầu, đều phải cộng thêm 7 ngày.
[ ] Cách khác.
```

Trả lĐồng ý cách tính trên

## 12. Câu hỏi 11 — Giờ mặc định của các mốc

Đề xuất:

```text
registrationOpenAt          = 08:00
registrationCloseAt         = 18:00
reviewDeadlineAt            = 18:00
jockeyMatchingDeadlineAt    = 18:00
schedulingDeadlineAt        = 18:00
Race đầu tiên               = raceDayStartTime, mặc định 08:00
```

```text
[ ] Đồng ý.
[ ] Muốn đổi giờ mặc định.
```

Trả lời:
Đồng ý.
## 13. Flow đề xuất sau khi chốt

Nếu đồng ý toàn bộ phương án khuyến nghị, flow sẽ là:

```text
Admin chọn maxApprovedEntries
→ hệ thống tính số Round/Race
→ hệ thống tính số ngày đăng ký
→ cộng 4 ngày duyệt hồ sơ
→ tính số ngày ghép jockey theo sức chứa
→ cộng 4 ngày hoàn tất lịch
→ cộng 2 ngày đệm
→ sinh minimumStartDate
→ từ startDate mô phỏng toàn bộ Round/Race
→ sinh minimumEndDate
→ FE khóa ngày nhỏ hơn minimumStartDate/minimumEndDate
→ BE kiểm tra lại toàn bộ khi create/update Tournament
```

BE phải là nguồn validation cuối cùng. FE chỉ dùng kết quả đề xuất để hiển thị sớm cho Admin.

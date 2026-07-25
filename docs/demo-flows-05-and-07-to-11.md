# Hướng dẫn demo Flow 05 và Flow 07–11

## 1. Các file cần chạy

File seed chính:

```text
docs/sql/demo-flows-05-and-07-to-11.sql
```

File mở khóa để trọng tài bắt đầu Final Race ngay:

```text
docs/sql/demo-full-flow-unlock-start.sql
```

Thứ tự:

1. Khởi động BE ít nhất một lần để Flyway tạo đủ schema mới nhất.
2. Chạy toàn bộ `demo-flows-05-and-07-to-11.sql`.
3. Demo Flow 05 độc lập.
4. Với Flow 07–11, thực hiện prediction và hai loại inspection trước.
5. Chạy `demo-full-flow-unlock-start.sql`.
6. Tiếp tục Start Race → Violation → Finish → Appeal → Report → Publish.

Không chạy file mở khóa trước khi Spectator dự đoán xong. Sau khi mở khóa, race đã đến giờ chạy và prediction sẽ đóng.

## 2. Thông tin chung

Mật khẩu của tất cả tài khoản:

```text
12345678
```

Các UUID cố định:

| Dữ liệu | UUID |
|---|---|
| Tournament Flow 05 | `e5000000-0000-0000-0000-000000000001` |
| Tournament Flow 07–11 | `e7000000-0000-0000-0000-000000000001` |
| Final Round | `e7010000-0000-0000-0000-000000000001` |
| Final Race | `e7020000-0000-0000-0000-000000000001` |

## 3. Flow 05 — Contract Matching

### Dữ liệu ban đầu

- Tournament đang ở phase `JOCKEY_MATCHING`.
- Có 4 ngựa thuộc đúng 4 Owner khác nhau.
- Có 4 Jockey khác nhau và registration đều `APPROVED`.
- Chưa có contract.
- Mỗi ví Owner có 30.000.000 VND để thanh toán.
- Matching deadline được tính động bằng `NOW() + 5 giờ`.

### Tài khoản

| Vai trò | Username |
|---|---|
| Owner | `mowner1`, `mowner2`, `mowner3`, `mowner4` |
| Jockey | `mjockey1`, `mjockey2`, `mjockey3`, `mjockey4` |

### Luồng demo đề xuất

1. Đăng nhập `mowner1`.
2. Mở giải `DEMO FLOW 05 - Jockey Matching`.
3. Chọn `Match Horse 1`, chọn `mjockey1`, rồi gửi lời mời.
4. Đăng nhập `mjockey1`, mở danh sách lời mời:
   - Có thể Reject để demo nhánh từ chối; hoặc
   - Accept để tiếp tục nhánh thành công.
5. Sau khi Jockey Accept, hệ thống tạo hóa đơn phí thuê Jockey.
6. Đăng nhập lại `mowner1`, thanh toán phí thuê:
   - Tiền thuê được chuyển vào `SYSTEM_ESCROW`.
   - Contract chuyển từ `ACCEPTED` sang `HIRING_PAID`.
   - Hệ thống tạo tiếp hóa đơn phí lập hợp đồng.
7. Owner thanh toán phí lập hợp đồng:
   - Phí lập hợp đồng vào `SYSTEM_REVENUE`.
   - Hệ thống tự kích hoạt contract, không có bước Admin duyệt.
   - 30% phí thuê được trả ngay vào ví Jockey.
   - 70% còn lại được giữ trong `SYSTEM_ESCROW`.
   - Contract chuyển sang `APPROVED`.
8. Kiểm tra `Match Horse 1` và `mjockey1` không còn xuất hiện trong danh sách có thể ghép của giải này.

Có thể dùng ba cặp còn lại để demo Reject, Cancel trước thanh toán và chống một Jockey/horse ký hai contract trong cùng giải.

## 4. Flow 07–11 — Full Final Race

### Dữ liệu ban đầu

- Tournament đang `ONGOING/RACING`.
- Có đúng một Final Round và một Final Race.
- Race có 8 entry `CONFIRMED`, lane từ 1 đến 8.
- 8 ngựa thuộc đúng 5 Owner khác nhau.
- Có 8 Jockey khác nhau.
- Có Race Referee, Head Referee, Veterinarian và Medical Staff.
- Có ba mức giải thưởng: hạng 1 là 50%, hạng 2 là 30%, hạng 3 là 20%.
- Có đủ tiền trong `SYSTEM_ESCROW` và `SYSTEM_PRIZE_POOL`.
- Mỗi contract đã ở trạng thái:
  - `APPROVED`;
  - 30% phí thuê đã trả;
  - 70% phí thuê đang chờ Final payout.
- Chưa có inspection, prediction, violation, appeal, result hoặc report.

### Tài khoản

| Vai trò | Username |
|---|---|
| Admin | `dmadmin` |
| Owner | `fowner1`, `fowner2`, `fowner3`, `fowner4`, `fowner5` |
| Jockey | `fjockey1` đến `fjockey8` |
| Race Referee | `frace_ref` |
| Head Referee | `fhead_ref` |
| Veterinarian | `fvet1` |
| Medical Staff | `fmed1` |
| Spectator | `fspec1` |

### Bước A — Prediction

1. Đăng nhập `fspec1`.
2. Mở race `DEMO FINAL RACE - Flow 07 to 11`.
3. Tạo prediction TOP3.
4. Có thể sửa prediction trước khi chạy file mở khóa.

Race ban đầu được đặt ở `NOW() + 5 giờ` để FE/API upcoming vẫn nhìn thấy race và cho Spectator dự đoán.

### Bước B — Inspection

1. Đăng nhập `fvet1`.
2. Khám đủ 8 ngựa và đánh dấu `PASS`.
3. Đăng nhập `fmed1`.
4. Khám đủ 8 Jockey và đánh dấu `PASS`.
5. Kiểm tra cả horse inspection và jockey inspection của mọi entry đều đã xác nhận và đạt.

Inspection window của seed được nới đủ rộng cho buổi demo. Nếu một trong hai phiếu của entry là `FAIL`, entry đó sẽ thành `SCRATCHED`.

### Bước C — Mở khóa Start

Sau khi prediction và inspection hoàn tất, chạy:

```text
docs/sql/demo-full-flow-unlock-start.sql
```

File này chỉ đổi timeline của đúng Final Race demo:

- `start_time = NOW() - 1 phút`;
- `end_time = NOW() + 5 giờ`;
- race vẫn `SCHEDULED`;
- Referee có thể Start ngay;
- Owner/Jockey còn khoảng 5 giờ để gửi appeal sau Finish.

### Bước D — Start, Violation và Finish

1. Đăng nhập `frace_ref`.
2. Kiểm tra readiness.
3. Start Race.
4. Khi race đang `ONGOING`, ghi một violation:
   - `WARNING` để chỉ cảnh cáo; hoặc
   - `DISQUALIFIED` để loại entry.
5. Bấm Finish Race.

BE sẽ tự sinh thời gian về đích và thứ hạng cho các entry không bị loại. Entry đã bị `DISQUALIFIED` nhận result `DISQUALIFIED` với `rank = null` và `finishTime = null`.

### Bước E — Appeal

1. Sau khi Finish, đăng nhập Owner hoặc Jockey có entry trong race.
2. Gửi appeal và đính kèm evidence nếu cần.
3. Đăng nhập `frace_ref`.
4. Xem và xử lý toàn bộ appeal `Pending`.
5. Không gửi report lên Head Referee khi vẫn còn appeal chưa xử lý.

Ví dụ dễ dùng:

- `fowner1` sở hữu `Final Horse 1` và `Final Horse 6`.
- `fjockey1` điều khiển `Final Horse 1`.

### Bước F — Result và Race Report

1. Đăng nhập `frace_ref`.
2. Kiểm tra kết quả random vừa được sinh.
3. Nhập `ratingChange` cho từng result theo cấu hình riêng của tournament:
   - Hạng 1: từ `+6` đến `+12`.
   - Hạng 2: từ `+2` đến `+5`.
   - Hạng 3: từ `+1` đến `+4`.
   - Hạng 4–5: từ `0` đến `+2`.
   - Các hạng còn lại: từ `-8` đến `0`.
   - `DISQUALIFIED`: từ `-8` đến `0`.
4. Tạo/cập nhật Draft Race Report.
5. Submit report lên Head Referee.
6. Đăng nhập `fhead_ref`.
7. Xem result, violation và appeal đã được Race Referee xử lý.
8. Head Referee có thể điều chỉnh result/rating trong khoảng hợp lệ rồi Sign report.

`ratingChange` là bắt buộc trước khi ký. Hệ thống không tự tính hoặc random điểm rating.

### Bước G — Admin Publish và Payout

1. Đăng nhập `dmadmin`.
2. Mở report đã `SIGNED`.
3. Publish Final Report.
4. Kiểm tra kết quả tự động:
   - Prediction của `fspec1` được chấm điểm.
   - Rating của từng horse được áp dụng đúng `ratingChange`.
   - Final Race và Final Round hoàn tất.
   - Top 3 được chia prize theo 50%/30%/20%.
   - Mỗi prize tiếp tục chia Owner 80% và Jockey 20%.
   - 70% phí thuê còn lại của cả 8 contract được chuyển từ escrow sang ví Jockey.
   - Payout có tính idempotent, publish/gọi payout lần hai không được trả tiền trùng.

## 5. Reset để demo lại

Chạy lại toàn bộ:

```text
docs/sql/demo-flows-05-and-07-to-11.sql
```

Script chỉ reset hai tournament demo có UUID cố định ở trên và cấp lại số dư ví demo. Nó không xóa các tournament khác.


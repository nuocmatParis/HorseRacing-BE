# Hướng dẫn demo HRTMS end-to-end

## 1. Chuẩn bị

Chỉ dùng database local/test. Các seed có đặt lại số dư ví demo và không được chạy trên production.

Khởi động BE một lần để Flyway/Hibernate tạo đúng schema rồi dừng BE trong lúc
cleanup/import, tránh scheduler thay đổi trạng thái giữa chừng. Chạy SQL theo đúng thứ tự:

```text
0. docs/sql/demo-cleanup.sql (chạy trước nếu database đã có seed demo cũ)
1. docs/sql/demo-test-data.sql
2. docs/sql/demo-full-coverage-extension.sql
3. docs/sql/demo-workflow-scenarios.sql
```

Import xong mới khởi động lại BE và FE để bắt đầu demo.

Kết quả chính sau khi import:

- 7 Tournament ứng với các phase/nghiệp vụ khác nhau.
- 32 ngựa và 32 Kỵ sĩ riêng cho bracket 32 entry.
- Một race upcoming có 8 entry và AI probability.
- Một Final Race đã FINISHED, report Signed và chờ Admin publish.
- Một Tournament SCHEDULING có 8 contract APPROVED nhưng chưa phân RaceEntry.
- Referee, Veterinarian và Medical Staff ở trạng thái AVAILABLE để phân công.

Mật khẩu chung: `admin123`.

| Tài khoản | Role | Mục đích |
|---|---|---|
| `admin1` | ADMIN | Dashboard, duyệt, scheduling, ví, publish report |
| `owner1` | HORSE_OWNER | Đăng ký ngựa, contract, appeal, ví |
| `spectator1` | SPECTATOR | Tạo/sửa dự đoán TOP3 |
| `spectator2` | SPECTATOR | Xem prediction được chấm sau publish |
| `referee1` | REFEREE/HEAD_REFEREE | Start race, result, violation, appeal, ký report |
| `referee2` | REFEREE | Staff AVAILABLE để Admin phân công |
| `vet1`, `medical1` | VET/MEDICAL | Khám race upcoming |
| `vet2`, `medical2` | VET/MEDICAL | Staff AVAILABLE để Admin phân công |
| `jockey1..8` | JOCKEY | Upcoming/final payout cũ |
| `jockey9..40` | JOCKEY | Contract và chuyển vòng bracket 32 |

## 3. Admin Dashboard và hồ sơ chờ xử lý

1. Đăng nhập `admin1`.
2. Mở **Bảng điều khiển**.
3. Kiểm tra bốn KPI: tổng Tournament, hồ sơ ngựa chờ duyệt, contract chờ duyệt, race đã lên lịch.
4. Bấm từng KPI để kiểm tra điều hướng.
5. Vào màn duyệt hồ sơ, chọn Tournament `DEMO 3 - Hồ sơ và contract chờ duyệt`.

Kỳ vọng:

- Có hồ sơ ngựa `PENDING_REVIEW`.
- Có contract `PENDING_ADMIN_REVIEW`.
- Một panel API lỗi không làm trắng toàn dashboard.

## 4. Owner và Jockey đăng ký Tournament

### Owner

1. Đăng nhập `owner1`.
2. Mở danh sách Tournament và chọn `DEMO 5 - Đang mở đăng ký`.
3. Chọn một ngựa chưa đăng ký vào Tournament này và gửi hồ sơ.
4. Kiểm tra trạng thái hồ sơ trong danh sách đăng ký của Owner.

### Jockey

1. Đăng nhập `jockey13`.
2. Chọn `DEMO 5 - Đang mở đăng ký`.
3. Đăng ký tham gia.

Kỳ vọng: Jockey đăng ký thành công ngay, không có bước Admin duyệt Jockey. Hồ sơ ngựa của Owner vẫn theo flow Admin duyệt.

## 5. Owner tìm Jockey và tạo contract

1. Đăng nhập `owner1`.
2. Mở Tournament `DEMO 6 - Đang ghép Kỵ sĩ`.
3. Chọn ngựa `Hồ Sơ Chờ Duyệt` — ngựa này đã APPROVED riêng trong Tournament demo matching.
4. Tìm một trong `jockey9..12`, gửi lời mời contract.
5. Đăng nhập đúng tài khoản Jockey được mời và thử chấp nhận hoặc từ chối.
6. Nếu chấp nhận, hoàn thành bước thanh toán/submit contract theo giao diện.
7. Đăng nhập `admin1`, duyệt contract `PENDING_ADMIN_REVIEW`.

Kỳ vọng: contract đi đúng các trạng thái mời → Jockey phản hồi → thanh toán/submit → Admin duyệt; không sửa trạng thái trực tiếp ở FE.

## 6. Admin Scheduling Board

1. Đăng nhập `admin1`.
2. Chọn `DEMO 7 - Đang xếp lịch` và Round `Vòng 1 (Chung Kết)`.
3. Bấm **Tự động phân entry**.
4. Kiểm tra 8 contract APPROVED được đưa vào `DEMO Scheduling Race`.
5. Bấm **Tự động phân lane**, xác nhận việc ghi đè lane.
6. Phân công:
   - Referee: `Trọng tài Available Demo`.
   - Veterinarian: `Thú y Available Demo`.
   - Medical Staff: `Y tế Available Demo`.
7. Kiểm tra checklist và publish schedule.

Kỳ vọng: race chuyển read-only sau publish; không thể double click publish; toàn bộ entry có lane duy nhất.

Chạy lại `demo-workflow-scenarios.sql` để xóa assignment/entry của riêng Scheduling Race và đưa scenario này về trạng thái ban đầu.

## 7. Spectator upcoming race và dự đoán TOP3

1. Đăng nhập `spectator1`.
2. Chọn Tournament `DEMO 1 - Race sắp diễn ra` và race `DEMO Upcoming Race`.
3. Kiểm tra 8 entry, lane, tên ngựa, Kỵ sĩ, xác suất thắng, Top 3 probability và confidence.
4. Chọn đúng ba entry cho Hạng 1, Hạng 2, Hạng 3.
5. Gửi prediction, đổi thứ tự rồi cập nhật.
6. Thử chọn trùng một entry hoặc entry không hợp lệ.

Kỳ vọng:

- Payload luôn là `TOP3`.
- Probability hiển thị thang 0–100, không nhân thêm 100.
- Prediction của race khác không bị khóa khi một race đã bắt đầu.

## 8. Vet và Medical kiểm tra trước race

Ngay sau khi chạy seed, race upcoming bắt đầu sau khoảng 60 phút, đúng cửa sổ inspection T-90 đến T-30.

1. Đăng nhập `vet1`, mở danh sách ngựa được phân công và submit inspection.
2. Đăng nhập `medical1`, mở danh sách Kỵ sĩ được phân công và submit medical inspection.
3. Cho phần lớn entry PASS; có thể chọn một entry FAIL để kiểm tra tự động SCRATCHED khi finalize.
4. Thử submit lại cùng một inspection để kiểm tra lỗi “đã tồn tại”.

Kỳ vọng: trang reload vẫn biết entry nào đã khám; lỗi hiển thị tiếng Việt.

## 9. Referee start race, violation và result

Để không phải chờ đủ 60 phút trong buổi demo, sau khi hoàn tất inspection có thể chạy đoạn SQL local/test sau:

```sql
SET @race_id = '90000000-0000-0000-0000-000000000001';
UPDATE races
SET start_time = NOW(),
    end_time = DATE_ADD(NOW(), INTERVAL 30 MINUTE),
    prediction_close_at = DATE_SUB(NOW(), INTERVAL 5 MINUTE)
WHERE race_id = @race_id;
```

Sau đó:

1. Đăng nhập `referee1`.
2. Start `DEMO Upcoming Race`.
3. Ghi một violation.
4. Nhập result: FINISHED có rank/finishTime; một DNF hoặc DQ không cần rank/finishTime.
5. Tạo report, xử lý appeal nếu có và ký report.

Đây là dữ liệu thao tác trực tiếp. Nếu muốn trả race về trạng thái ban đầu, chạy lại `demo-test-data.sql`.

## 10. Admin publish Final Report, rating và payout

1. Chạy lại `demo-test-data.sql` nếu Final đã từng được publish.
2. Đăng nhập `admin1`.
3. Mở **Biên bản cuộc đua**.
4. Chọn `DEMO 2 - Final chờ publish` → `DEMO Final Signed Race`.
5. Kiểm tra checklist, 6 FINISHED, 1 DNF, 1 DISQUALIFIED và rating preview.
6. Bấm publish và xác nhận một lần.

Kỳ vọng:

- Report chuyển `Published` và không còn nút publish lần hai.
- Prediction của `spectator2` chuyển `SCORED`.
- Horse rating được áp dụng đúng một lần.
- Top 3 được chia thưởng Owner/Jockey.
- 70% phí thuê còn lại được release cho Jockey khi đủ điều kiện.
- Transaction có tên Tournament, race, horse và Jockey.

## 11. Kiểm tra kết quả prediction và ví người tham gia

1. Đăng nhập `spectator2`, mở kết quả prediction.
2. Kiểm tra predicted rank, official rank, đúng/sai, điểm từng lựa chọn và tổng reward points.
3. Đăng nhập `owner1`, kiểm tra balance và `PRIZE_OWNER_SHARE`.
4. Đăng nhập `jockey1`, kiểm tra `PRIZE_JOCKEY_SHARE` và `JOCKEY_HIRING_FINAL_INCOME`.

Kỳ vọng: DNF/DQ không có official rank nhận 0 điểm nhưng không làm VOID toàn prediction; Jockey không có nút nạp tiền.

## 12. Admin nạp Quỹ giải thưởng qua VNPay

1. Ghi lại số dư ba ví hệ thống trước khi test.
2. Đăng nhập `admin1`, mở **Ví hệ thống**.
3. Tại **Quỹ giải thưởng**, bấm **Nạp quỹ giải thưởng**.
4. Nhập ví dụ:

```text
Số tiền: 100000000
Lý do: Bổ sung quỹ giải thưởng phục vụ demo
```

5. Bấm **Tiếp tục xác nhận** → **Thanh toán qua VNPay**.
6. Hoàn thành thanh toán bằng tài khoản/thẻ sandbox do VNPay cung cấp.
7. Chờ VNPay gọi return URL của BE và FE chuyển về `/admin/wallet`.

Kỳ vọng:

- Trước khi VNPay thành công: Quỹ chưa tăng, chỉ có `PaymentTransaction PENDING`.
- Sau callback hợp lệ: Quỹ tăng đúng số tiền và có transaction `SYSTEM_PRIZE_POOL_TOP_UP`.
- Doanh thu hệ thống và Tiền ký quỹ không thay đổi.
- Refresh callback hoặc IPN/return đến hai lần không cộng tiền hai lần.
- Hủy thanh toán hoặc mã VNPay khác `00`: Quỹ không đổi.

## 13. Demo bracket 32 và chuyển Top 4 atomic

1. Chạy lại `demo-full-coverage-extension.sql` để reset scenario bracket.
2. Đăng nhập `admin1`, mở **Biên bản cuộc đua**.
3. Chọn `DEMO 4 - Bracket 32 chuyển vòng`.
4. Publish report của `DEMO Bracket - Vòng 1 Race A`.
5. Kiểm tra Final vẫn chưa có entry vì Race B chưa Published.
6. Publish report của `DEMO Bracket - Vòng 1 Race B`.
7. Mở Round 2 (Chung Kết).

Kỳ vọng:

- Sau Race A: không tạo entry dở dang.
- Sau Race B: hệ thống tạo đúng 8 entry Final trong một transaction — Top 4 Race A và Top 4 Race B.
- Round 1 được đánh dấu đã advance; Round 2 và Tournament chuyển sang `SCHEDULING`.
- Không có DNF/DQ đi tiếp và chạy lại transition không tạo entry trùng.

## 14. Reset demo

- Xóa sạch toàn bộ dữ liệu demo của cả ba seed: chạy `demo-cleanup.sql`.
- Reset Final payout/prediction/upcoming: chạy lại `demo-test-data.sql`.
- Reset bracket 32: chạy lại `demo-full-coverage-extension.sql`.
- Reset Scheduling Board: chạy lại `demo-workflow-scenarios.sql`.
- Với flow đăng ký/contract đã tạo nhiều invoice hoặc transaction, cách sạch nhất là restore database local/test rồi chạy lại cả ba file theo thứ tự.

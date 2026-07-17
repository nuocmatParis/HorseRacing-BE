# Hướng dẫn test Luồng 06–11 bằng database sạch

## 1. Bộ dữ liệu được tạo

File chạy chính: [`demo_sample_data.sql`](../demo_sample_data.sql).

Seed chỉ tạo hai tournament:

| Tournament | Dữ liệu | Mục đích |
|---|---:|---|
| `DEMO FULL 8 - Luồng 07 đến 11` | 8 ngựa, 1 Final Race | Inspection, prediction, start/finish, violation, appeal, report, rating và payout |
| `DEMO BRACKET 16 - Chuyển Top 4` | 16 ngựa thực tế, 2 Race vòng 1 và 1 Final | Publish Race 2 để chuyển Top 4 của mỗi Race vào Final |

Phân công phần demo theo yêu cầu:

| Thành viên | Luồng phụ trách |
|---|---|
| Hưng | 2.6 — Bracket và chuyển Round Top 4 |
| Khung | 2.7 — Inspection ngựa và jockey |
| Hải | 2.8 — Start, Result, Report; 2.9 — Vi phạm và Khiếu nại |
| Tuấn | 2.10 — Spectator Prediction; 2.11 — Prize Payout |

Lưu ý về bracket 16 ngựa:

- Thuật toán BE hiện tại xem `maxApprovedEntries <= 16` là một race chung kết duy nhất.
- Để có hai race vòng 1 và test chuyển vòng, tournament được cấu hình sức chứa `32`.
- Seed chỉ tạo đúng `16 contract APPROVED`, chia đều `8 + 8`.
- Cấu trúc này hợp lệ theo BE: số tối thiểu cần cho bracket sức chứa 32 là `2 race × 8 = 16 entry`.

## 2. Nếu bạn chỉ xóa dữ liệu, không xóa bảng

Không cần dựng lại schema. Thực hiện:

1. Dừng BE.
2. Mở `demo_sample_data.sql` bằng DataGrip hoặc MySQL Workbench.
3. Chọn **Run Script**, chạy toàn bộ file.
4. Không bôi đen chạy từng đoạn vì file có procedure và `DELIMITER`.
5. Khi script hoàn tất, kiểm tra ba result set ở cuối file.
6. Khởi động lại BE.

File tự `DELETE` toàn bộ dữ liệu nghiệp vụ, nhưng giữ nguyên bảng và `flyway_schema_history`.

## 3. Nếu bạn đã drop toàn bộ bảng

Không chạy seed khi schema chưa có bảng. Chuỗi migration hiện tại không tự bootstrap được database hoàn toàn trống vì migration đầu tiên đã tham chiếu các bảng lõi.

Thực hiện một lần cho database local/demo:

1. Tạo schema rỗng `SWP391_Project_HRTMS` nếu schema cũng đã bị xóa.
2. Tạm chạy Hibernate để tạo schema hiện tại, không chạy Flyway:

   ```powershell
   mvn spring-boot:run "-Dspring-boot.run.arguments=--spring.flyway.enabled=false --spring.jpa.hibernate.ddl-auto=update"
   ```

3. Khi BE start thành công, nhấn `Ctrl+C`.
4. Hibernate vừa tạo schema đúng theo entity hiện tại, tương ứng trạng thái sau V15. Tạo Flyway baseline thẳng ở V15 để Flyway không chạy lại các migration lịch sử lên schema đã hoàn chỉnh:

   ```powershell
   mvn spring-boot:run "-Dspring-boot.run.arguments=--spring.flyway.enabled=true --spring.flyway.baseline-on-migrate=true --spring.flyway.baseline-version=15 --spring.jpa.hibernate.ddl-auto=update"
   ```

5. Khi BE start thành công, nhấn `Ctrl+C`.
6. Chạy toàn bộ `demo_sample_data.sql`.
7. Khởi động BE bình thường.

Quy trình baseline này chỉ dành cho database local/demo đã bị drop sạch, không dùng cho production.

## 4. Tài khoản demo

Mật khẩu chung:

```text
12345678
```

| Role | Username | Dùng cho |
|---|---|---|
| Admin | `admin1` | Publish report, rating preview, xem finance, tiếp tục lập lịch Final |
| Owner | `owner1` | Sở hữu `FullHorse01`–`FullHorse08`, gửi appeal, nhận prize |
| Owner | `owner2` | Sở hữu `BracketHorse01`–`BracketHorse16` |
| Jockey | `jockey1`–`jockey8` | Full Flow 8 |
| Jockey | `jockey9`–`jockey24` | Bracket 16 |
| Spectator | `spectator1`, `spectator2` | Tạo dự đoán Top 3 |
| Head Referee | `referee1` | Full Flow 8: xử lý appeal và ký report |
| Race Referee | `referee2` | Full Flow 8: start, violation, finish, report |
| Head Referee | `referee3` | Vòng 1 của Bracket 16 |
| Race Referee | `referee4` | Bracket Race 1 đã Published |
| Race Referee | `referee5` | Bracket Race 2 đang Signed |
| Head Referee | `referee6` | Final của Bracket 16 |
| Veterinarian | `vet1` | Khám ngựa Full Flow 8 |
| Medical Staff | `medical1` | Khám jockey Full Flow 8 |

`referee7`, `vet2` và `medical2` là tài khoản dự phòng/lịch sử.

## 5. Cửa sổ thời gian của DEMO FULL 8

Gọi thời điểm chạy seed là `S`.

| Chức năng | Thời gian |
|---|---|
| Prediction mở | `S - 1 phút` |
| Prediction đóng | `S + 180 phút` |
| Inspection mở | `S` |
| Inspection đóng | khoảng `S + 179 phút` |
| Có thể Start | từ `S` đến khoảng `S + 185 phút` |
| Thời lượng vận hành cấu hình | `180 phút` |

Prediction vẫn đóng trước `startTime` đúng 5 phút. Sau khi race đã Start, BE cũng chặn tạo/sửa prediction dù `predictionCloseAt` chưa tới.

Nếu cửa sổ hết hạn, dừng BE và chạy lại toàn bộ seed để nhận thêm ba giờ mới.

## 6. Test DEMO FULL 8 từ đầu đến cuối

### Bước 1 — Spectator dự đoán Top 3

1. Đăng nhập `spectator1`.
2. Mở `DEMO FULL 8 - Final Race`.
3. Chọn ba entry cho hạng 1, 2, 3 và gửi prediction.
4. Có thể sửa prediction trước khi referee Start race.

Seed không tạo sẵn prediction để bạn thao tác thật trên FE.

### Bước 2 — Inspection

1. Đăng nhập `vet1`, khám các ngựa của `DEMO FULL 8`.
2. Nhập giống thực tế, cân nặng thực tế, doping và kết luận PASS/FAIL.
3. Đăng nhập `medical1`, khám các jockey còn active.
4. Nhập cân nặng thực tế, doping và kết luận PASS/FAIL.

Quy tắc khi test FAIL:

- Chỉ cần một trong hai phía FAIL thì cặp ngựa/jockey chuyển `SCRATCHED`.
- Phía còn lại không cần khám tiếp và phải thấy đúng lý do bị loại.
- Final Race có runtime minimum là `2`, nên race vẫn Start được khi còn từ 2 cặp PASS trở lên.
- Để demo đủ Top 3 và payout cả ba hạng, nên giữ ít nhất `3` cặp PASS.
- Nếu còn định ghi một violation với hình phạt `Bị loại`, hãy giữ ít nhất `4` cặp PASS; sau khi loại một cặp vẫn còn ba kết quả FINISHED.
- Ví dụ FAIL 3 cặp, còn 5 cặp PASS: race vẫn Start bình thường.

### Bước 3 — Start, violation và Finish

1. Đăng nhập `referee2`.
2. Mở `DEMO FULL 8 - Final Race` và kiểm tra readiness.
3. Bấm Start sau khi các entry còn active đã PASS cả hai inspection.
4. Ghi một violation nếu muốn test Luồng 09.
5. Chọn hình phạt `Cảnh cáo` hoặc `Bị loại`.
6. Bấm kết thúc race. BE tự sinh thời gian về đích/thứ hạng cho entry hợp lệ.

Entry đã `SCRATCHED` không chặn race. Referee không cần chờ đủ 180 phút mới bấm Finish.

### Bước 4 — Owner/Jockey gửi appeal

Sau khi race đã Finish:

1. Đăng nhập `owner1` hoặc một trong `jockey1`–`jockey8`.
2. Mở trang Khiếu nại.
3. Chọn đúng race/entry, category và nhập mô tả.
4. Có thể thêm evidence text, image URL, video URL hoặc document URL.

Chỉ owner của ngựa hoặc jockey thuộc entry đó được appeal.

### Bước 5 — Race Report và Head Referee

1. Đăng nhập `referee2`.
2. Nhập nội dung Race Report.
3. Bấm **Lưu và gửi Head Referee**.
4. Đăng nhập `referee1`.
5. Nếu có appeal Pending, Accepted/Rejected appeal và nhập resolution.
6. Ký report sau khi không còn appeal Pending.

### Bước 6 — Admin publish, scoring, rating và payout

1. Đăng nhập `admin1`.
2. Mở Race Report đã `SIGNED`.
3. Xem trước rating của từng ngựa.
4. Publish report.

Một lần publish phải tự động:

- Chấm prediction của spectator.
- Cộng/trừ Horse Rating và tạo `horse_rating_histories`.
- Chỉ Top 3 official nhận prize.
- Chia prize theo contract: Owner 80%, Jockey 20%.
- Trả 70% hire fee còn lại cho toàn bộ jockey của Final.
- Ghi transaction vào ví Owner, Jockey, Quỹ giải thưởng và Ví ký quỹ.
- Đánh dấu Final Race và Final Round `COMPLETED`; scheduler sau đó chuyển Tournament sang `RESULT_PENDING` để chờ khâu kết thúc giải tiếp theo.

Kiểm tra lại bằng các tài khoản:

- `spectator1`: kết quả prediction và tổng điểm.
- `owner1`: số dư, lịch sử prize transaction.
- Jockey Top 3: prize share và final payout.
- Các jockey còn lại: final payout 70% hire fee.
- `admin1`: lịch sử Quỹ giải thưởng và Ví ký quỹ.

## 7. Test chuyển Round Top 4 với DEMO BRACKET 16

Trạng thái ngay sau seed:

- Round 1 có hai race, mỗi race đúng 8 entry.
- Race 1: `COMPLETED`, report `PUBLISHED`.
- Race 2: `FINISHED`, report `SIGNED`.
- Final Race: `SCHEDULING`, chưa có entry.
- Mỗi race vòng 1 có đủ rank 1–8 và đúng bốn qualifier đầu tiên.

Thao tác:

1. Đăng nhập `admin1`.
2. Mở phần Công bố kết quả.
3. Chọn `DEMO BRACKET 16 - Vòng 1 - Race 2`.
4. Publish report đã Signed.
5. Reload Scheduling Board hoặc trang qualifier.

Kết quả mong đợi:

- Race 2 chuyển `COMPLETED`.
- Round 1 chuyển `COMPLETED`.
- `transitionStatus = COMPLETED` và có `advancedAt`.
- Tournament chuyển về phase `SCHEDULING`.
- Final Race có đúng 8 entry:
  - `BracketHorse01`–`BracketHorse04` từ Race 1.
  - `BracketHorse09`–`BracketHorse12` từ Race 2.
- Không có `BracketHorse05`–`08` hoặc `BracketHorse13`–`16` trong Final.
- Entry Final chưa có lane; Admin phải bấm tự động phân làn hoặc chỉnh lane thủ công.

Sau đó Admin có thể gán `referee7` làm Race Referee, gán `vet2` và `medical2`, phân lane, publish lịch Final rồi tiếp tục vận hành như giải Full 8.

## 8. Query kiểm tra nhanh

### Kiểm tra Final chỉ có Top 4 của hai race

```sql
SELECT h.name, source_race.name AS source_race, rr.finish_position
FROM race_entries final_entry
JOIN races final_race ON final_race.race_id = final_entry.race_id
JOIN jockey_horse_contracts c ON c.contract_id = final_entry.contract_id
JOIN horses h ON h.horse_id = c.horse_id
JOIN race_entries source_entry ON source_entry.contract_id = c.contract_id
JOIN races source_race ON source_race.race_id = source_entry.race_id
JOIN race_results rr ON rr.entry_id = source_entry.entry_id
WHERE final_race.race_id = '30000000-0000-0000-0000-000000000023'
  AND source_race.race_id IN (
      '30000000-0000-0000-0000-000000000021',
      '30000000-0000-0000-0000-000000000022'
  )
ORDER BY source_race.sequence_order, rr.finish_position;
```

### Kiểm tra prize và final payout sau publish Full 8

```sql
SELECT h.name, rr.finish_position, rr.prize_money,
       rr.owner_prize_amount, rr.jockey_prize_amount,
       rr.prize_status, rr.is_prize_paid
FROM race_results rr
JOIN race_entries re ON re.entry_id = rr.entry_id
JOIN jockey_horse_contracts c ON c.contract_id = re.contract_id
JOIN horses h ON h.horse_id = c.horse_id
WHERE rr.race_id = '30000000-0000-0000-0000-000000000001'
ORDER BY rr.finish_position;

SELECT c.contract_id, h.name, c.escrow_status, c.final_payout_status,
       c.final_payout_at
FROM jockey_horse_contracts c
JOIN horses h ON h.horse_id = c.horse_id
WHERE c.tournament_id = '10000000-0000-0000-0000-000000000001'
ORDER BY h.name;
```

### Kiểm tra prediction scoring

```sql
SELECT u.username, p.prediction_type, p.status,
       p.reward_points, p.scored_at
FROM predictions p
JOIN spectators s ON s.spectator_id = p.spectator_id
JOIN users u ON u.user_id = s.user_id
WHERE p.race_id = '30000000-0000-0000-0000-000000000001';
```

## 9. Chạy lại khi hết thời gian

1. Dừng BE.
2. Chạy lại toàn bộ `demo_sample_data.sql`.
3. Khởi động BE.

Mỗi lần chạy lại sẽ xóa dữ liệu demo cũ và tạo cửa sổ mới gần ba giờ. Không sửa riêng `start_time`, vì inspection window, prediction window, end time và staff assignment có liên hệ với nhau.

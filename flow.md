# HRTMS — Luồng nghiệp vụ và luồng demo end-to-end

## 1. Chuẩn bị dữ liệu demo

Chỉ chạy trên database local/test. Dừng BE trước khi cleanup/import để scheduler
không tự đổi phase hoặc trạng thái race giữa chừng.

```text
Nếu database đã có dữ liệu demo cũ:
docs/sql/demo-cleanup.sql

Sau đó chạy lần lượt:
1. docs/sql/demo-test-data.sql
2. docs/sql/demo-full-coverage-extension.sql
3. docs/sql/demo-workflow-scenarios.sql
```

Import xong mới khởi động lại BE và FE.

Mật khẩu chung: `admin123`.

| Role | Tài khoản dùng demo |
|---|---|
| Admin | `admin1` |
| Owner | `owner1` |
| Spectator tạo prediction | `spectator1` |
| Spectator xem prediction đã chấm | `spectator2` |
| Head Referee/Referee chính | `referee1` |
| Referee khả dụng để phân công | `referee2` |
| Veterinarian | `vet1`, `vet2` |
| Medical Staff | `medical1`, `medical2` |
| Jockey của race mẫu | `jockey1` đến `jockey8` |
| Jockey phục vụ bracket 32 | `jockey9` đến `jockey40` |

## 2. Luồng tổng quát của một Tournament

```text
Admin tạo Tournament
→ xác nhận bracket, điều kiện và giải thưởng
→ publish Tournament
→ Owner/Jockey đăng ký
→ Admin duyệt hồ sơ ngựa
→ Owner tìm Jockey và tạo contract
→ Jockey chấp nhận
→ Owner thanh toán
→ Admin duyệt contract
→ hoàn tất matching
→ xếp lịch Round hiện tại
→ phân entry, lane và nhân sự
→ publish schedule
→ Spectator dự đoán
→ Vet/Medical kiểm tra
→ Referee start race
→ nhập result và violation
→ Owner/Jockey gửi appeal
→ Head Referee xử lý appeal và ký report
→ Admin publish report
→ chấm prediction và cập nhật rating
→ chuyển Top 4 sang Round sau hoặc kết thúc Tournament
→ nếu là Final: chia thưởng và giải phóng payout còn lại cho Jockey
```

Phase tổng quát:

```text
DRAFT
→ REGISTRATION_OPEN
→ REGISTRATION_REVIEW
→ JOCKEY_MATCHING
→ SCHEDULING
→ RACING
→ RESULT_PENDING
→ SCHEDULING của Round sau hoặc RESULT_PUBLISHED/FINISHED
```

## 3. Luồng Admin tạo và publish Tournament

1. Admin tạo Tournament ở phase `DRAFT`.
2. `maxApprovedEntries` phải là lũy thừa của 2 và bắt đầu từ 8.
3. Hệ thống đề xuất số Round, số Race và thời lượng tối thiểu của Tournament.
4. Admin cấu hình:
   - Thời gian đăng ký, duyệt hồ sơ, ghép Jockey và hoàn tất lịch.
   - Khung giờ đua trong ngày.
   - Tối đa 9 race/ngày.
   - Thời lượng vận hành mặc định 30 phút/race.
   - Khoảng nghỉ tối thiểu 35 phút sau khi race trước kết thúc.
   - Khoảng cách giữa hai Round tối thiểu 7 ngày theo ngày lịch, không so giờ.
5. Final Race phải kết thúc trong ngày kết thúc Tournament và trong khung giờ vận hành.
6. Admin xác nhận bracket, cấu hình điều kiện tham gia và cơ cấu giải thưởng.
7. Khi publish thành công:

```text
TournamentStatus: DRAFT → OPEN
TournamentPhase: DRAFT → REGISTRATION_OPEN
```

## 4. Luồng Owner đăng ký ngựa

```text
Owner chọn Tournament đang REGISTRATION_OPEN
→ chọn ngựa đủ điều kiện
→ gửi đăng ký
→ PENDING_PAYMENT
→ Owner thanh toán phí đăng ký
→ PENDING_REVIEW
→ Admin duyệt hoặc từ chối
→ APPROVED hoặc REJECTED
```

Quy tắc:

- Owner không được đăng ký trùng cùng một ngựa vào cùng Tournament.
- Chỉ hồ sơ ngựa `APPROVED` mới được dùng để tạo contract.
- Owner có thể rút hồ sơ khi còn đúng phase và trạng thái cho phép.
- Admin chịu trách nhiệm duyệt hồ sơ ngựa.

Demo bằng `owner1` với Tournament `DEMO 5 - Đang mở đăng ký`.

## 5. Luồng Jockey đăng ký Tournament

```text
Jockey chọn Tournament đang REGISTRATION_OPEN
→ nhập mức phí thuê
→ gửi đăng ký
→ đăng ký được APPROVED ngay
```

Jockey không qua bước Admin duyệt. Sau khi Tournament chuyển sang
`JOCKEY_MATCHING`, Jockey đã đăng ký mới xuất hiện cho Owner tìm kiếm.

Demo bằng `jockey13` với Tournament `DEMO 5 - Đang mở đăng ký`.

## 6. Luồng ghép Jockey và contract

```text
Owner chọn hồ sơ ngựa APPROVED
→ tìm Jockey APPROVED trong cùng Tournament
→ gửi lời mời
→ Contract PENDING_JOCKEY
```

Nhánh Jockey từ chối:

```text
PENDING_JOCKEY → REJECTED
```

Nhánh Jockey chấp nhận:

```text
PENDING_JOCKEY
→ ACCEPTED
→ hệ thống tạo invoice phí thuê
→ Owner thanh toán phí thuê
→ HIRING_PAID, tiền được giữ trong ví ký quỹ
→ hệ thống tạo invoice phí tạo contract
→ Owner thanh toán phí contract
→ PENDING_ADMIN_REVIEW
→ Admin duyệt
→ APPROVED
```

Khi Jockey chấp nhận một lời mời, các lời mời đang chờ khác của cùng ngựa hoặc
cùng Jockey được hủy để tránh một đối tượng xuất hiện hai lần trong cùng Round.

Demo bằng:

- Owner: `owner1`.
- Tournament: `DEMO 6 - Đang ghép Kỵ sĩ`.
- Jockey có sẵn: `jockey9` đến `jockey12`.

## 7. Luồng hoàn tất matching và xếp lịch

Khi Admin hoàn tất matching, hệ thống kiểm tra:

- Số contract `APPROVED` không vượt `maxApprovedEntries`.
- Số contract đủ để mỗi race Round đầu có tối thiểu 8 entry.
- Bracket vẫn ở trạng thái `CONFIRMED`.

Nếu hợp lệ:

```text
JOCKEY_MATCHING
→ tạo/phân entry cho Round đầu
→ SCHEDULING
```

Trong Scheduling Board, Admin thực hiện:

1. Phân contract vào các race của Round hiện tại.
2. Gán hoặc random `laneNumber`.
3. Phân công ít nhất một Referee cho mỗi race.
4. Phân công Veterinarian và Medical Staff.
5. Kiểm tra start/end time, số entry và lane không trùng.
6. Publish schedule của Round hiện tại.

Khi publish:

```text
Race: SCHEDULING → SCHEDULED
Round: SCHEDULING → SCHEDULED
TournamentPhase: SCHEDULING → RACING
```

Chỉ Round hiện tại được publish; Round tương lai chưa được mở lịch.

Demo bằng `admin1` với `DEMO 7 - Đang xếp lịch`.

## 8. Luồng prediction của Spectator

Khi schedule của Round được publish:

- Các race trong Round có chung thời điểm mở prediction, mặc định trước race đầu 24 giờ.
- Mỗi race có thời điểm đóng riêng, mặc định trước start time 5 phút.
- Spectator được dự đoán Top 3 cho từng race.
- Có thể sửa prediction cho tới `predictionCloseAt` của race đó.
- Race 1 bắt đầu không làm đóng prediction của Race 2 trở đi.

```text
Spectator chọn race
→ xem RaceEntry và WinProbability
→ chọn ba entry khác nhau cho hạng 1, 2, 3
→ gửi TOP3 prediction
→ PENDING
→ Admin publish report
→ SCORED hoặc VOIDED tùy trạng thái race
```

Quy tắc result:

- `DISQUALIFIED` sau khi xuất phát vẫn chấm prediction, lựa chọn đó nhận 0 điểm.
- Race bị hủy hoàn toàn làm prediction `VOIDED`.
- Horse bị scratched trước lúc đóng prediction thì người dùng được chọn lại.

Demo tạo/sửa prediction bằng `spectator1`; xem kết quả đã chấm bằng `spectator2`.

## 9. Luồng inspection

Cửa sổ mặc định:

```text
T-90: mở kiểm tra
T-30: đóng kiểm tra và finalize entry
T-0: race bắt đầu
```

Vet xử lý kiểm tra ngựa, Medical Staff xử lý kiểm tra Jockey.

```text
Vet/Medical mở danh sách race được phân công
→ chọn RaceEntry
→ nhập kết quả kiểm tra
→ PASS hoặc FAIL
```

Tại deadline T-30:

- Thiếu inspection ngựa hoặc Jockey: entry chuyển `SCRATCHED`.
- Có inspection nhưng không PASS/confirmed: entry chuyển `SCRATCHED`.
- Đủ hai inspection hợp lệ: entry tiếp tục `CONFIRMED`.
- Không được tạo inspection thứ hai cho cùng một entry.

Demo bằng `vet1` và `medical1` trên `DEMO Upcoming Race`.

## 10. Luồng Referee vận hành race

Referee chỉ được start race đã được phân công và đã đủ điều kiện inspection.

Thời gian mặc định:

```text
startEarlyToleranceMinutes = 0
startLateToleranceMinutes = 30
```

Luồng:

```text
Race SCHEDULED
→ Referee start race đúng cửa sổ cho phép
→ Race ONGOING
→ ghi violation nếu có
→ nhập result cho từng entry
→ hoàn tất race
→ Race FINISHED/COMPLETED
```

Result:

| Trường hợp | Entry status | Rank/finishTime |
|---|---|---|
| Về đích hợp lệ | `FINISHED` | Bắt buộc |
| Kết quả không được công nhận sau khi xuất phát | `DISQUALIFIED` | Null |
| Không đủ điều kiện trước start | `SCRATCHED` | Không tạo kết quả thi đấu |

Nếu quá thời gian start muộn cho phép, race phải được postpone/reschedule thay vì
Referee tự start trễ không giới hạn.

Demo bằng `referee1` trên `DEMO Upcoming Race`.

## 11. Luồng appeal và RaceReport

Trong thời gian vận hành 30 phút của race:

```text
Race diễn ra
→ result tạm thời được nhập
→ Owner/Jockey gửi appeal và evidence
→ appeal Pending/UnderReview
→ Head Referee xử lý
→ Accepted, Rejected hoặc Cancelled
```

Sau khi hết hạn nhận appeal:

- Không nhận appeal mới.
- Appeal đã gửi nhưng chưa xong vẫn tiếp tục được xử lý.
- Head Referee chỉ ký report khi result và appeal đã sẵn sàng.

```text
RaceReport Draft
→ Head Referee Signed
→ Admin kiểm tra rating preview
→ Admin Published
```

Report đã `Published` là kết quả chính thức của race và không được publish lần hai.

## 12. Luồng publish report, rating, prediction và payout

Khi Admin publish report:

```text
Khóa kết quả chính thức
→ chấm prediction
→ cập nhật Horse Rating đúng một lần
→ kiểm tra chuyển Round
```

Nếu là race thông thường:

- Không chia prize money chung cuộc.
- Prediction vẫn được chấm.
- Rating vẫn được cập nhật theo official result.

Nếu là race duy nhất của Final Round:

```text
Lấy Top 3 official result
→ tính phần Owner
→ tính phần Jockey
→ Quỹ giải thưởng trả Owner/Jockey
→ tạo PRIZE_OWNER_SHARE và PRIZE_JOCKEY_SHARE
→ giải phóng 70% phí thuê còn lại cho Jockey nếu Tournament đủ điều kiện
```

Tất cả payout phải idempotent: publish/callback lặp lại không được trả tiền hai lần.

Demo bằng `admin1` với `DEMO 2 - Final chờ publish`.

## 13. Luồng chuyển Top 4 sang Round sau

Mỗi race lấy Top 4 `FINISHED`; prediction và giải thưởng chung cuộc vẫn chỉ dùng Top 3.

Điều kiện chuyển Round:

- Tất cả race của Round hiện tại đã hoàn tất.
- Tất cả report đã `Published`.
- Mỗi race có đủ Top 4 hợp lệ.
- `DISQUALIFIED` không đi tiếp.
- Round sau có đúng số race theo bracket.

Chuyển Round phải atomic:

```text
Validate toàn bộ Round
→ nếu một race thiếu Top 4: không tạo bất kỳ entry nào, transition bị BLOCKED
→ nếu tất cả hợp lệ: tạo toàn bộ entry Round sau trong một transaction
→ Round hiện tại COMPLETED
→ Round sau SCHEDULING
→ TournamentPhase SCHEDULING
```

Demo bằng `DEMO 4 - Bracket 32 chuyển vòng`:

1. Publish report Race A: Final vẫn chưa có entry.
2. Publish report Race B: hệ thống tạo cùng lúc 8 Final entry.
3. Chạy lại transition không tạo entry trùng.

## 14. Luồng nạp tiền và ví

### Owner nạp ví cá nhân

```text
Owner nhập số tiền
→ BE tạo PaymentTransaction
→ chuyển sang VNPay
→ VNPay callback hợp lệ
→ cộng USER_MAIN wallet
→ tạo DEPOSIT transaction
→ FE chuyển về trang ví Owner
```

Jockey không có chức năng tự nạp tiền; Jockey nhận tiền từ hợp đồng và giải thưởng.

### Admin nạp Quỹ giải thưởng

```text
Admin nhập số tiền và lý do
→ PaymentTransaction PENDING
→ chuyển sang VNPay
→ VNPay callback hợp lệ
→ cộng duy nhất SYSTEM_PRIZE_POOL
→ tạo SYSTEM_PRIZE_POOL_TOP_UP
→ FE chuyển về /admin/wallet
```

Quy tắc:

- Chưa có callback thành công thì chưa cộng tiền.
- Sai chữ ký, sai amount, hủy thanh toán hoặc response code khác `00`: không cộng.
- IPN/return lặp lại không cộng hai lần.
- Không cộng khoản này vào Ví doanh thu hoặc Ví ký quỹ.

## 15. Luồng cancel và postpone race

### Cancel

```text
Admin cancel race
→ Race CANCELLED
→ prediction của race VOIDED
→ giải phóng nhân sự được phân công
→ gửi notification cho các bên liên quan
```

### Postpone/reschedule

Admin chọn lịch mới; hệ thống kiểm tra:

- Không vượt 9 race/ngày.
- Không trùng horse, Jockey, Referee, Vet hoặc Medical Staff.
- Nằm trong khung giờ vận hành.
- Đủ khoảng cách tối thiểu với race khác.
- Không vi phạm khoảng nghỉ của horse.
- Không làm sai thứ tự Round và khoảng cách tối thiểu 7 ngày.

Hệ thống có thể đề xuất ngày/slot gần nhất nhưng Admin phải xác nhận, không tự ý
tạo ngày thi đấu mới và chuyển lịch hoàn toàn tự động.

## 16. Luồng notification

Các sự kiện publish, schedule, contract, cancel/postpone, appeal và report tạo
notification cho đúng nhóm người liên quan.

```text
Nghiệp vụ phát sinh event
→ lưu notification trong database
→ gửi WebSocket để FE cập nhật thời gian thực
→ gửi email nếu preference cho phép
→ người dùng đọc/đánh dấu đã đọc trên web
```

Thông báo WebSocket không thay thế dữ liệu lưu trong database; reload trang vẫn phải
xem lại được lịch sử thông báo.

## 17. Thứ tự demo ngắn gọn

Nếu chỉ có một buổi demo, nên chạy theo thứ tự:

1. `admin1`: xem Dashboard.
2. `owner1` và `jockey13`: đăng ký Tournament.
3. `owner1` và `jockey9`: demo lời mời contract.
4. `admin1`: duyệt contract và Scheduling Board.
5. `spectator1`: dự đoán Top 3.
6. `vet1` và `medical1`: inspection.
7. `referee1`: start race, violation, result, appeal và ký report.
8. `admin1`: publish Final Report.
9. `spectator2`: xem điểm prediction.
10. `owner1` và `jockey1`: xem prize/payout transaction.
11. `admin1`: nạp Quỹ giải thưởng qua VNPay.
12. `admin1`: publish hai report của bracket 32 để kiểm tra chuyển Top 4 atomic.


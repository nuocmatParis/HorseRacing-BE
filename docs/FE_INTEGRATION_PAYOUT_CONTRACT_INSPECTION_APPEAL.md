# Hướng dẫn FE cập nhật theo các thay đổi Payout, Contract, Inspection Staff và Appeal

## 1. Phạm vi

Tài liệu này mô tả những thay đổi FE bắt buộc phải thực hiện sau khi BE sửa:

1. C-02 — Chỉ tự động release 70% hiring fee sau khi report của Final Race được publish.
2. C-03 — Jockey chỉ được reject lời mời đang chờ; chỉ được cancel contract khi Owner chưa thanh toán.
3. H-08 — Phân công Veterinarian/Medical Staff có kiểm tra vòng đời Race và khóa đồng thời.
4. H-09 — Referee chỉ được xem appeal thuộc Race/Round mình phụ trách; Race Referee trực tiếp mới được review.

Không có thay đổi đối với random lane/H-13 trong đợt này.

## 2. Response và xử lý lỗi chung

Response thành công:

```json
{
  "code": 200,
  "message": "Success",
  "result": {}
}
```

Response lỗi:

```json
{
  "code": 1610,
  "message": "Contract cannot be cancelled in its current state"
}
```

FE phải:

- Dùng `code` để mapping thông báo tiếng Việt.
- Không hiển thị nguyên văn lỗi tiếng Anh của BE cho người dùng.
- Không tự cập nhật trạng thái trước khi API thành công.
- Khi API thất bại, giữ nguyên dữ liệu hiện tại và refetch nếu có khả năng dữ liệu đã được thay đổi từ tab khác.
- Chặn double click trong lúc request đang chạy.

## 3. C-02 — Final Jockey payout

### 3.1. Endpoint đã bị xóa

Không được gọi endpoint sau nữa:

```http
POST /api/admin/contracts/{contractId}/release-final-payout
```

FE Admin phải xóa:

- Nút `Release final payout`.
- Service/hook gọi endpoint trên.
- Modal xác nhận giải ngân 70% thủ công.
- Logic cho phép Admin release theo từng contract.

### 3.2. Luồng mới

```text
Admin publish report của Race thường
→ Không release 70%.

Admin publish report của Final Race duy nhất
→ Report chuyển PUBLISHED
→ Final Race chuyển COMPLETED
→ Final Round chuyển COMPLETED
→ BE tự payout giải thưởng Top 3
→ BE tự release 70% hiring fee còn lại cho các Jockey đủ điều kiện
```

Final Race ở đây là Race duy nhất thuộc Round có `isFinal = true`.

Endpoint kích hoạt luồng:

```http
POST /api/admin/races/{raceId}/report/publish
```

Không có request body.

Response rút gọn:

```json
{
  "code": 200,
  "message": "Success",
  "result": {
    "reportId": "uuid",
    "raceId": "uuid",
    "roundName": "Chung kết",
    "status": "PUBLISHED",
    "publishedAt": "2026-07-23T16:30:00"
  }
}
```

### 3.3. FE cần làm sau khi publish Final Race

Khi publish thành công, FE phải invalidate/refetch:

- Race report.
- Race và Round hiện tại.
- Danh sách contract của Tournament.
- Transaction history liên quan.
- Wallet balance của Owner/Jockey nếu các màn hình này đang mở.

Trạng thái payout lấy từ `ContractResponse`:

```json
{
  "escrowStatus": "RELEASED",
  "finalPayoutStatus": "RELEASED",
  "finalPayoutAt": "2026-07-23T16:30:00",
  "escrowAmount": 0
}
```

FE chỉ hiển thị:

- `NOT_RELEASED` → `Chưa giải ngân phần còn lại`.
- `RELEASED` → `Đã giải ngân 70%`.
- `CANCELLED` → `Khoản giải ngân đã hủy`.

Không tự suy ra đã payout chỉ dựa vào việc report `PUBLISHED`; phải dùng dữ liệu contract/transaction trả về sau khi refetch.

## 4. C-03 — Jockey reject và cancel contract

### 4.1. Luồng contract hiện tại không có Admin review

FE không được xây màn hình hoặc nút Admin approve/reject contract.

Luồng đang được BE thực hiện:

```text
Owner gửi lời mời
→ PENDING_JOCKEY
→ Jockey accept
→ ACCEPTED
→ Owner thanh toán JOCKEY_HIRING_FEE
→ HIRING_PAID
→ Owner thanh toán CONTRACT_CREATION_FEE
→ BE tự động kích hoạt contract
→ APPROVED
→ BE tự động giải ngân 30% cho Jockey
→ Giữ 70% còn lại trong SYSTEM_ESCROW
```

`PENDING_ADMIN_REVIEW` là giá trị enum cũ còn tồn tại trong BE để tương thích dữ liệu/phòng thủ. Luồng hiện tại không tạo trạng thái này. FE không hiển thị trạng thái này như một bước nghiệp vụ và không tạo hàng chờ Admin duyệt contract.

### 4.2. Phân biệt hai hành động

| Trạng thái contract | Reject | Cancel |
|---|---:|---:|
| `PENDING_JOCKEY` | Có | Không |
| `ACCEPTED`, Owner chưa thanh toán | Không | Có |
| `HIRING_PAID` | Không | Không |
| `APPROVED` | Không | Không |
| `REJECTED` | Không | Không |
| `CANCELLED` | Không | Không |
| `TERMINATED` | Không | Không |

### 4.3. Reject lời mời

Endpoint giữ nguyên:

```http
POST /api/jockey/contracts/{contractId}/reject
Content-Type: application/json
```

Body:

```json
{
  "reason": "Tôi không thể tham gia giải đấu này"
}
```

Chỉ hiển thị nút `Từ chối lời mời` khi:

```text
contract.status === "PENDING_JOCKEY"
```

Nếu gọi ở trạng thái khác, BE trả:

```json
{
  "code": 1547,
  "message": "Invalid contract status"
}
```

Thông báo tiếng Việt:

```text
Không thể từ chối: lời mời này đã được xử lý hoặc hợp đồng không còn chờ phản hồi.
```

### 4.4. Cancel trước khi Owner thanh toán

Endpoint mới:

```http
POST /api/jockey/contracts/{contractId}/cancel
Content-Type: application/json
```

Body:

```json
{
  "reason": "Tôi không thể tiếp tục tham gia"
}
```

BE chỉ cho phép khi đồng thời thỏa mãn:

```text
contract.status = ACCEPTED
contract.paymentStatus = UNPAID
contract.escrowStatus = NOT_HELD
hiring invoice.status = UNPAID
```

Response thành công rút gọn:

```json
{
  "code": 200,
  "message": "Success",
  "result": {
    "contractId": "uuid",
    "status": "CANCELLED",
    "paymentStatus": "UNPAID",
    "escrowStatus": "NOT_HELD",
    "advancePayoutStatus": "CANCELLED",
    "finalPayoutStatus": "CANCELLED",
    "cancelReason": "Tôi không thể tiếp tục tham gia",
    "cancelledAt": "2026-07-23T16:30:00"
  }
}
```

Sau khi thành công:

- Đóng modal.
- Refetch danh sách contract và chi tiết contract.
- Xóa contract khỏi danh sách `Đang chờ Owner thanh toán`.
- Hiển thị toast `Đã hủy hợp đồng trước khi thanh toán`.

Không hiển thị nút cancel nếu FE đã biết Owner thanh toán. Tuy nhiên BE vẫn là nguồn kiểm tra cuối cùng vì Owner có thể thanh toán từ một tab khác.

### 4.5. Mapping lỗi contract

| Code | Ý nghĩa | Thông báo FE đề xuất |
|---:|---|---|
| `1010` | Body không hợp lệ | `Vui lòng nhập lý do hợp lệ, tối đa 500 ký tự.` |
| `1004` | Contract không thuộc Jockey | `Bạn không có quyền thao tác hợp đồng này.` |
| `1409` | Không tìm thấy hiring invoice | `Không tìm thấy hóa đơn thuê kỵ sĩ của hợp đồng.` |
| `1546` | Không tìm thấy contract | `Hợp đồng không tồn tại hoặc đã bị xóa.` |
| `1547` | Reject sai trạng thái | `Lời mời không còn ở trạng thái chờ phản hồi.` |
| `1610` | Không được cancel | `Không thể hủy vì Owner đã thanh toán hoặc hợp đồng không còn ở trạng thái cho phép.` |

## 5. H-08 — Phân công Inspection Staff

### 5.1. API giữ nguyên

Gán thủ công:

```http
POST /api/admin/races/{raceId}/inspection-staff/assign
Content-Type: application/json
```

```json
{
  "veterinarianId": "uuid",
  "medStaffId": "uuid"
}
```

Gán tự động:

```http
POST /api/admin/races/{raceId}/inspection-staff/auto-assign
```

Không có request body.

Response:

```json
{
  "code": 200,
  "message": "Success",
  "result": {
    "assignmentId": "uuid",
    "raceId": "uuid",
    "raceName": "Race 1",
    "veterinarianId": "uuid",
    "veterinarianName": "Bác sĩ thú y 1",
    "medStaffId": "uuid",
    "medicalStaffName": "Nhân viên y tế 1",
    "assignedById": "uuid",
    "assignedByName": "Admin",
    "assignedAt": "2026-07-23T16:30:00"
  }
}
```

### 5.2. Quy tắc mới

BE chỉ cho phân công khi:

- Race đang `SCHEDULING`; hoặc
- Race đang `SCHEDULED`, chưa mở inspection window và chưa có bất kỳ phiếu khám nào.

BE chặn khi:

- Race đã `ONGOING`, `FINISHED`, `COMPLETED` hoặc `CANCELLED`.
- Inspection window đã mở.
- Đã tồn tại Horse Inspection hoặc Jockey Inspection của bất kỳ entry nào.

Auto-assign hiện có khóa Race và staff. Nếu Race đã có đủ Vet và Medical Staff, gọi lại auto-assign trả assignment hiện tại, không tự thay người.

### 5.3. FE cần sửa

- Chỉ cho bấm `Phân công`/`Tự động phân công` khi Race còn có thể chỉnh sửa.
- Khi Race đã có phiếu khám, toàn bộ vùng phân công chuyển read-only.
- Không tự chuyển staff sang `ASSIGNED` trước khi API thành công.
- Sau khi thành công, refetch assignment và danh sách staff available.
- Hai request cùng lúc có thể khiến request sau nhận lỗi staff đã được Race khác chọn; FE phải refetch danh sách available.

Nếu FE có đủ `startTime` và `inspectionOpenMinutesBefore`, có thể cảnh báo trước:

```text
inspectionOpenAt = startTime - inspectionOpenMinutesBefore
```

Nhưng không dùng tính toán FE thay cho validation từ BE.

### 5.4. Mapping lỗi inspection staff

| Code | Thông báo FE đề xuất |
|---:|---|
| `1010` | `Vui lòng chọn đầy đủ bác sĩ thú y và nhân viên y tế.` |
| `1701` | `Không tìm thấy nhân viên y tế đã chọn.` |
| `1702` | `Nhân viên y tế vừa được phân công cho cuộc đua khác. Vui lòng chọn lại.` |
| `1703` | `Tài khoản nhân viên y tế đang bị đình chỉ.` |
| `1704` | `Không tìm thấy bác sĩ thú y hoặc tài khoản bác sĩ đang bị đình chỉ.` |
| `1705` | `Bác sĩ thú y vừa được phân công cho cuộc đua khác. Vui lòng chọn lại.` |
| `1706` | `Hiện không có nhân viên y tế khả dụng.` |
| `1707` | `Hiện không có bác sĩ thú y khả dụng.` |
| `1725` | `Không thể thay đổi nhân sự vì cuộc đua đã bắt đầu, đã mở khám hoặc đã có phiếu khám.` |

## 6. H-09 — Phạm vi Appeal của Referee

### 6.1. API giữ nguyên nhưng dữ liệu đã được lọc ở BE

Danh sách:

```http
GET /api/referee/appeals
```

BE chỉ trả:

- Appeal thuộc Race mà Referee được phân công trực tiếp; hoặc
- Appeal thuộc Round mà Referee đang là Head Referee.

FE không cần tải toàn bộ appeal rồi tự lọc.

Chi tiết:

```http
GET /api/referee/appeals/{appealId}
```

Referee không thuộc Race/Round nhận `1004`.

Review:

```http
POST /api/referee/appeals/{appealId}/review
Content-Type: application/json
```

Body chấp nhận:

```json
{
  "status": "Accepted",
  "resolution": "Chấp nhận sau khi xem lại video"
}
```

Body từ chối:

```json
{
  "status": "Rejected",
  "resolution": "Không tìm thấy bằng chứng làm thay đổi kết quả"
}
```

Lưu ý enum hiện tại phân biệt hoa/thường:

```text
Accepted
Rejected
```

Không gửi `ACCEPTED` hoặc `REJECTED`.

### 6.2. Phân quyền UI

Race Referee trực tiếp:

- Được xem appeal.
- Được xem evidence.
- Được bấm `Chấp nhận` hoặc `Từ chối`.

Head Referee của Round:

- Được xem appeal đã gửi và kết quả Race Referee xử lý.
- Không dùng endpoint review appeal.
- Xử lý tiếp trong luồng xem/duyệt Race Report.

Referee không liên quan:

- Không được thấy appeal trong danh sách.
- Nếu truy cập URL cũ hoặc URL được chia sẻ, hiển thị trang `Bạn không có quyền xem khiếu nại này`.

Để xác định có hiển thị nút review hay không, FE so sánh `appeal.raceId` với danh sách Race được phân công trực tiếp cho Referee hiện tại. Không suy ra quyền review chỉ vì API danh sách trả appeal, bởi danh sách của Head Referee cũng chứa appeal của Round.

### 6.3. Response appeal mẫu

```json
{
  "code": 200,
  "message": "Success",
  "result": {
    "appealId": "uuid",
    "entryId": "uuid",
    "raceId": "uuid",
    "raceName": "Race 1",
    "roundName": "Vòng 1",
    "tournamentName": "Summer Cup",
    "horseName": "Horse 01",
    "jockeyName": "Jockey 01",
    "categoryName": "Cản trở đường đua",
    "description": "Đề nghị xem lại tình huống ở đoạn cuối",
    "status": "Accepted",
    "reviewedByRefereeId": "uuid",
    "reviewedAt": "2026-07-23T16:30:00",
    "resolution": "Chấp nhận sau khi xem lại video"
  }
}
```

### 6.4. Mapping lỗi appeal

| Code | Thông báo FE đề xuất |
|---:|---|
| `1004` | `Bạn không được phân công vào cuộc đua hoặc vòng đấu của khiếu nại này.` |
| `1204` | `Tài khoản chưa có hồ sơ trọng tài.` |
| `2630` | `Không tìm thấy khiếu nại.` |
| `2632` | `Khiếu nại đã được xử lý hoặc không còn chờ duyệt.` |
| `2633` | `Chỉ có thể chấp nhận hoặc từ chối khiếu nại.` |

Khi nhận `2632`, FE phải đóng modal review và refetch appeal vì một Referee/request khác có thể vừa xử lý trước.

## 7. Checklist hoàn thành FE

- [ ] Không còn nút/call API Admin release 70% thủ công.
- [ ] Sau publish Final Race, refetch contract, transaction và wallet liên quan.
- [ ] Nút Reject chỉ xuất hiện với `PENDING_JOCKEY`.
- [ ] Có nút Cancel với `ACCEPTED + UNPAID + NOT_HELD`.
- [ ] Cancel gọi đúng `/api/jockey/contracts/{id}/cancel`.
- [ ] Không có màn hình/nút Admin approve hoặc reject contract.
- [ ] Không coi `PENDING_ADMIN_REVIEW` là một phase của flow contract.
- [ ] Không cho sửa inspection staff sau khi mở khám hoặc đã có phiếu khám.
- [ ] Sau lỗi cạnh tranh staff, refetch danh sách staff available.
- [ ] Trang Referee dùng danh sách appeal đã được BE lọc.
- [ ] Head Referee xem appeal ở chế độ read-only.
- [ ] Chỉ Race Referee trực tiếp thấy nút review.
- [ ] Appeal review gửi đúng enum `Accepted`/`Rejected`.
- [ ] Tất cả mã lỗi được hiển thị thành thông báo tiếng Việt.
- [ ] Không thay đổi UI/lệnh gọi API random lane trong đợt này.

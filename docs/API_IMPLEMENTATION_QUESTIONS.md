# Các vấn đề cần chốt trước khi code các API còn thiếu

File này chỉ dùng để làm rõ nghiệp vụ trước khi triển khai. Chưa thực hiện thay đổi code cho các API bên dưới.

## 1. Quy ước chung cần chốt

### 1.1. Prefix của endpoint

Một số endpoint được yêu cầu đang thiếu `/api`:

```text
GET /owner/race-schedule
GET /jockey/race-schedule
GET /owner/race-results
GET /jockey/race-results
GET /spectator/races/upcoming
GET /spectator/races/{raceId}
```

Đề xuất dùng thống nhất:

```text
GET /api/owner/race-schedule
GET /api/jockey/race-schedule
GET /api/owner/race-results
GET /api/jockey/race-results
GET /api/spectator/races/upcoming
GET /api/spectator/races/{raceId}
```

**Cần bạn chốt:** Có dùng prefix `/api` cho tất cả endpoint không? 
có 

### 1.2. Phân trang và sắp xếp

Các danh sách contract, lịch đua, kết quả và race được phân công có thể tăng nhiều theo thời gian.

Hai lựa chọn:

1. Trả toàn bộ danh sách, chưa phân trang để dễ làm và demo.
2. Dùng `page`, `size`, `sort` ngay từ đầu.

**Đề xuất:** Trước mắt trả toàn bộ, sắp xếp theo quy tắc ghi ở từng API; bổ sung phân trang sau nếu cần.

**Cần bạn chốt:** Có cần phân trang ngay trong đợt này không?
có 

### 1.3. Múi giờ

Project đang dùng `LocalDateTime`, không mang thông tin múi giờ trong JSON.

**Đề xuất:** Toàn bộ thời gian được hiểu theo múi giờ vận hành của hệ thống là `Asia/Ho_Chi_Minh`.

**Cần bạn chốt:** FE và BE có thống nhất hiểu mọi `LocalDateTime` là giờ Việt Nam không?
có 

---

## 2. API contract dành cho Admin

### 2.1. Contract đã được duyệt theo tournament

```http
GET /api/admin/contracts/approved/tournaments/{tournamentId}
```

Các điểm cần chốt:

1. “Approved” có nghĩa chính xác là chỉ lấy `ContractStatus.APPROVED` hay lấy cả contract đã hoàn thành/đã trả payout nếu sau này có thêm trạng thái? 
2. Nếu `tournamentId` không tồn tại thì trả `404`, hay đơn giản trả danh sách rỗng? trả không tồn tại
3. Có bao gồm contract `CANCELLED` hoặc `TERMINATED` từng được duyệt trước đó không?

**Đề xuất:**

- Chỉ lấy contract có trạng thái hiện tại là `APPROVED`.
- Tournament không tồn tại trả `404`.
- Không trả `CANCELLED`, `TERMINATED`, `REJECTED`.
- Sắp xếp `requestedAt` giảm dần.

**Cần bạn chốt:** Có đồng ý với cách hiểu trên không? có 

### 2.2. Danh sách contract theo trạng thái

```http
GET /api/admin/contracts?status=...
```

Các điểm cần chốt:

1. `status` là bắt buộc hay tùy chọn?
2. Nếu không truyền `status`, API trả tất cả contract hay báo lỗi?
3. Có cần lọc thêm theo `tournamentId`, owner hoặc jockey không?

**Đề xuất:**

- `status` không bắt buộc. 
- Không truyền `status` thì trả tất cả contract.
- Có truyền thì lọc chính xác theo enum `ContractStatus`.
- Chưa thêm các filter khác trong đợt này.

**Cần bạn chốt:** API không có `status` có được phép trả toàn bộ contract không? 
cái status là chọn 1 trong các enum trong contract status 

---

## 3. Admin đóng đăng ký tournament

```http
POST /api/admin/tournaments/{id}/close-registration
```

### 3.1. Chuyển phase

Hiện project đã có scheduler tự chuyển:

```text
REGISTRATION_OPEN -> REGISTRATION_REVIEW
```

khi quá `registrationCloseAt`.

**Đề xuất:** API thủ công cũng chỉ được gọi khi tournament đang ở `REGISTRATION_OPEN`, sau đó:

```text
phase  = REGISTRATION_REVIEW
status = OPEN
```

**Cần bạn chốt:** Admin có được đóng đăng ký sớm hơn `registrationCloseAt` không?
có 

### 3.2. Có sửa `registrationCloseAt` hay không?

Nếu Admin đóng sớm, có hai lựa chọn:

1. Giữ nguyên `registrationCloseAt` đã cấu hình, chỉ đổi phase.
2. Gán `registrationCloseAt = thời điểm Admin đóng thực tế` để lịch sử phản ánh đúng.

**Đề xuất:** Gán `registrationCloseAt` bằng thời điểm đóng thực tế.

**Cần bạn chốt:** Có cho phép cập nhật lại field này không? 
có 

### 3.3. Xử lý đăng ký và invoice đang dở

Khi đóng đăng ký có thể còn horse registration ở `PENDING_PAYMENT` hoặc invoice `UNPAID`.

Các cách xử lý:

1. Giữ nguyên và cho phép người dùng thanh toán tiếp.
2. Hủy toàn bộ invoice chưa thanh toán và chuyển registration liên quan sang `WITHDRAWN`/`REJECTED`.
3. Không cho tạo đăng ký mới nhưng vẫn cho thanh toán đến một deadline riêng.

**Đề xuất:** Khi đóng đăng ký, hủy invoice đăng ký chưa thanh toán và chuyển horse registration `PENDING_PAYMENT` sang `WITHDRAWN` với lý do hệ thống; nếu không làm vậy, người dùng có thể thanh toán sau khi cổng đăng ký đã đóng.

**Cần bạn chốt:** Chọn cách 1, 2 hay 3?
cho nó về hết auto rejected 

### 3.4. Gọi API nhiều lần

**Đề xuất:** Lần đầu thành công; gọi lại khi phase không còn là `REGISTRATION_OPEN` thì trả lỗi conflict, không âm thầm thành công.

**Cần bạn chốt:** Muốn strict như trên hay muốn API idempotent và trả lại tournament hiện tại?
cái 3.4 này là sao ta 
---

## 4. Owner hủy contract

```http
POST /api/owner/contracts/{id}/cancel
```

### 4.1. Trạng thái được phép hủy

Contract hiện có các trạng thái:

```text
PENDING_JOCKEY
ACCEPTED
REJECTED
HIRING_PAID
PENDING_ADMIN_REVIEW
APPROVED
CANCELLED
TERMINATED
```

**Đề xuất:** Owner chỉ được hủy ở:

```text
PENDING_JOCKEY
ACCEPTED
HIRING_PAID
PENDING_ADMIN_REVIEW
APPROVE
```

Không cho hủy khi đã `APPROVED`, vì lúc này 30% tiền thuê đã được trả cho Jockey và contract có thể đã được dùng tạo `RaceEntry`. Trường hợp sau duyệt phải đi qua nghiệp vụ `TERMINATED` riêng.

**Cần bạn chốt:** Có đồng ý chặn hoàn toàn cancel khi contract đã `APPROVED` không?
có cho hủy approve nhưng kkhông trả tiền phí đk contract, chỉ hoàn tiền 70% phí hiring paid th, còn 30% hiring fee của jockey giữ thì jockey vẫn được giữ 

### 4.2. Hoàn tiền

Khi hủy trước `APPROVED`, có thể tồn tại:

- Invoice phí thuê Jockey.
- Invoice phí tạo contract.
- Tiền thuê đang nằm trong system escrow.

**Đề xuất xử lý trong một transaction:**

- Invoice `UNPAID`: chuyển `CANCELLED`.
- Invoice `PAID`: refund về ví Owner.
- Nếu phí thuê đã được giữ trong escrow: cập nhật `paymentStatus = REFUNDED`, `escrowStatus = REFUNDED`.
- Sau khi tài chính xử lý thành công mới đặt contract `CANCELLED`.
- Nếu refund lỗi thì rollback toàn bộ, contract không bị đổi trạng thái nửa chừng.

**Cần bạn chốt:** Phí tạo contract đã thanh toán có được hoàn 100% khi Owner chủ động hủy không, hay đây là phí hệ thống không hoàn lại?
kkhông trả tiền phí đk contract, chỉ hoàn tiền 70% phí hiring paid th, còn 30% hiring fee của jockey giữ thì jockey vẫn được giữ  

### 4.3. Ảnh hưởng đến registration

Sau khi contract bị hủy, horse registration và jockey registration có thể được ghép contract khác.

**Đề xuất:** Không đổi trạng thái registration; chỉ giải phóng ràng buộc contract đang hoạt động để Owner/Jockey có thể ghép lại.

**Cần bạn chốt:** Có cho phép mời/tạo contract mới ngay sau khi hủy không?
có nhưng trong thời gian tạo hợp đồng mới được tạo contract mới 

### 4.4. Lý do hủy

**Đề xuất request body:**

```json
{
  "reason": "Owner no longer wishes to continue"
}
```

`reason` bắt buộc, không được để trống, có giới hạn độ dài.

**Cần bạn chốt:** Lý do hủy có bắt buộc không?
có 
---

## 5. Owner rút đăng ký ngựa

```http
POST /api/owners/registrations/{id}/withdraw
```

Endpoint này nằm dưới role Owner nên được hiểu là rút `HorseTournamentRegistration`, không phải đăng ký Jockey.

### 5.1. Phase được phép rút

**Đề xuất cho rút trong:**

```text
REGISTRATION_OPEN
REGISTRATION_REVIEW
JOCKEY_MATCHING
```

Chặn từ `SCHEDULING` trở đi vì đăng ký có thể đã ảnh hưởng contract và race entry.

**Cần bạn chốt:** Có cho rút trong `JOCKEY_MATCHING` không, hay chỉ cho đến hết `REGISTRATION_REVIEW`?
cho rút trong jockey matching, sceduling luôn, nhưng khi rút ở schedunling thì bắt buộc phải hủy hợp đồng với jockey trước khi rút 

### 5.2. Registration status được phép rút

**Đề xuất:** Cho rút khi registration đang là:

```text
PENDING_PAYMENT
PENDING_REVIEW
APPROVED
SCHEDULING
```

Chặn nếu đã `REJECTED` hoặc `WITHDRAWN`.

**Cần bạn chốt:** Registration đã `APPROVED` nhưng chưa có contract có được rút không?
cho rút trong jockey matching, sceduling luôn, nhưng khi rút ở schedunling thì bắt buộc phải hủy hợp đồng với jockey trước khi rút 

### 5.3. Contract liên quan

Một horse registration có thể đang có invitation hoặc contract đang hoạt động.

Hai lựa chọn:

1. Chặn withdraw cho đến khi Owner hủy contract trước.
2. Withdraw tự động hủy tất cả invitation/contract chưa `APPROVED` và xử lý refund.

**Đề xuất:** Chặn withdraw nếu còn contract ở một trong các trạng thái hoạt động; yêu cầu Owner gọi API cancel contract trước. Cách này rõ ràng và tránh một request gây nhiều thay đổi tài chính ngầm.

**Cần bạn chốt:** Chặn như đề xuất hay tự động cascade cancel contract?
yêu c ầu hủy contract trưoc, ccác cái lời mời mà owner gửi đến jockey khi mà ch có ai chấp nhận tự động hủy tất cả với lý do là owner rút

### 5.4. Hoàn phí đăng ký

**Đề xuất:**

- Invoice chưa trả: cancel.
- Invoice đã trả: refund 100% nếu rút trước khi `SCHEDULING`.
- Cập nhật `status = WITHDRAWN`, `withdrawnAt`, `withdrawReason`.

**Cần bạn chốt:** Phí đăng ký có được hoàn 100% ở mọi phase được phép rút, hay có phase không được hoàn?
không được hoàn 
### 5.5. Ownership và lý do

**Đề xuất:**

- Chỉ Owner sở hữu registration mới được rút.
- `reason` bắt buộc trong request body.

**Cần bạn chốt:** Lý do rút có bắt buộc không?
có 

---

## 6. Lịch race của Owner và Jockey

```http
GET /api/owner/race-schedule
GET /api/jockey/race-schedule
```

### 6.1. Race nào được xem là lịch thi đấu?

**Đề xuất:** Chỉ trả race đã publish schedule, tức có `schedulePublishedAt`, và status hiện tại là:

```text
SCHEDULED
ONGOING
```

Không trả race `SCHEDULING` vì đó là lịch nháp.

**Cần bạn chốt:** Có muốn danh sách schedule chứa cả race đã `FINISHED`, `COMPLETED`, `CANCELLED` không, hay các race đó chỉ nằm trong lịch sử/kết quả?
chỉ lấy lịch mà nó sẽ thi đấu trong tương lai và đã được admin public 

### 6.2. Owner có nhiều ngựa trong cùng race

Nếu một Owner có hai ngựa cùng tham gia một race, có hai cách trả:

1. Mỗi `RaceEntry` là một dòng, race có thể lặp lại.  
2. Mỗi race là một dòng và bên trong có danh sách entry của Owner.

**Đề xuất:** Mỗi race một object, bên trong có `myEntries`; FE dễ hiển thị lịch mà không bị race lặp.

**Cần bạn chốt:** Muốn response theo race hay theo entry?
theo race đi 

### 6.3. Dữ liệu cần trả

**Đề xuất gồm:**

- Tournament, round và race.
- Thời gian bắt đầu/kết thúc, track, distance, status.
- Entry ID, lane, entry status.
- Horse và Jockey của entry.
- Mốc inspection và prediction nếu FE cần hiển thị.

**Cần bạn chốt:** Owner/Jockey có cần thấy thông tin nhân sự được phân công và inspection ngay trong API lịch không?
không, chỉ khi ttới ngày thi đấu trước 1 ngày mới biết được ai là ng khám 
### 6.4. Sắp xếp

**Đề xuất:** Race gần nhất đứng trước, sắp xếp `startTime ASC`.

**Cần bạn chốt:** Có chỉ lấy các race từ thời điểm hiện tại trở đi không?
có, chỉ lấy lịch từ hiện tại đến tương lai 

---

## 7. Kết quả race của Owner và Jockey

```http
GET /api/owner/race-results
GET /api/jockey/race-results
```

### 7.1. Khi nào kết quả được phép hiển thị?

Trong nghiệp vụ đã chốt trước đó, Race Report được publish thì kết quả race đó trở thành kết quả chính thức và không sửa nữa.

**Đề xuất:** Chỉ trả kết quả khi `RaceReport.status = Published`. Không trả result thuộc report `Draft` hoặc `Signed`.

**Cần bạn chốt:** Đây có phải điều kiện chính xác không?
có 

### 7.2. Kết quả không được công nhận

**Đã chốt:** Bỏ `DID_NOT_FINISH`. Mọi trường hợp đã xuất phát nhưng không hoàn thành hoặc bị loại dùng `DISQUALIFIED`; `rank` và `finishTime` bằng `null`.

### 7.3. Tiền thưởng

**Đề xuất:**

- Owner thấy `prizeMoney`, `ownerPrizeAmount`, `prizeStatus` của entry thuộc mình.
- Jockey thấy `prizeMoney`, `jockeyPrizeAmount`, `prizeStatus`.
- Race thường không có prize thì các field bằng `0` hoặc `null` theo dữ liệu hiện tại.

**Cần bạn chốt:** Có trả thông tin chia thưởng trong endpoint kết quả không?

### 7.4. Response theo race hay entry

Tương tự schedule, một Owner có thể có nhiều entry trong một race.

**Đề xuất:** Mỗi race một object, chứa danh sách kết quả của các entry thuộc người đang đăng nhập.

**Cần bạn chốt:** Có dùng cấu trúc này không?
có 
---

## 8. Spectator xem race sắp diễn ra và chi tiết race

```http
GET /api/spectator/races/upcoming
GET /api/spectator/races/{raceId}
```

### 8.1. Upcoming được định nghĩa thế nào?

**Đề xuất:**

```text
schedulePublishedAt != null
status = SCHEDULED
startTime > now
```

Sắp xếp `startTime ASC`.

**Cần bạn chốt:** Race bị `DELAYED/POSTPONED` trong project hiện không có status riêng mà được reschedule về `SCHEDULED`; như vậy có đúng ý không?
hm tôi nghĩ nên để là đang lên lịch lại chứ kh nen để là scheduled vì scheduled là đã xếp lịch rồi 

### 8.2. Khoảng thời gian của upcoming

Các lựa chọn:

1. Trả tất cả race tương lai.
2. Chỉ trả trong ngày.
3. Hỗ trợ filter `from`, `to`, `tournamentId`.

**Đề xuất:** Đợt đầu trả tất cả race tương lai đã publish; chưa filter.

**Cần bạn chốt:** Có cần filter ngay không?
có 

### 8.3. Chi tiết race được xem ở trạng thái nào?

**Đề xuất:** Spectator chỉ xem race đã publish schedule. Có thể xem race `SCHEDULED`, `ONGOING`, `FINISHED`, `COMPLETED`, `CANCELLED`; không được xem `SCHEDULING`.

**Cần bạn chốt:** Race bị hủy có tiếp tục xem được chi tiết và lý do hủy không?
có 

### 8.4. Dữ liệu entry công khai

**Đề xuất trả:** lane, horse, jockey, entry status và scratched reason nếu có. Không trả contract, phí thuê hoặc phần trăm chia thưởng.

**Cần bạn chốt:** Có công khai `scratchedReason`, `disqualifiedReason` cho Spectator không?
có 

---

## 9. Spectator xem kết quả dự đoán của chính mình

```http
GET /api/spectator/races/{id}/predictions/me/result
```

Project đang có endpoint gần giống:

```http
GET /api/races/{raceId}/predictions/me
```

### 9.1. Có giữ endpoint cũ không?

**Đề xuất:** Giữ endpoint cũ để không làm hỏng FE/Postman hiện tại; thêm endpoint mới chuyên trả kết quả.

**Cần bạn chốt:** Giữ cả hai endpoint hay thay endpoint cũ?
giữ cả 2 

### 9.2. Khi prediction chưa được chấm

Các lựa chọn:

1. Trả prediction với `status = PENDING`, `rewardPoints = null`.
2. Trả lỗi “Prediction result is not available yet”.

**Đề xuất:** Endpoint `/result` chỉ có ý nghĩa sau khi xử lý kết quả; nếu còn `PENDING` thì trả lỗi nghiệp vụ. Endpoint cũ vẫn dùng để xem prediction trước khi chấm.

**Cần bạn chốt:** Có đồng ý tách hành vi như trên không?
tách ra 2 cacái đi

### 9.3. Prediction VOIDED/CANCELLED

**Đề xuất:** Vẫn trả response với status `VOIDED` hoặc `CANCELLED`, điểm nhận được bằng `0` hoặc giá trị hiện có, kèm lý do void. Không trả lỗi.

**Cần bạn chốt:** Prediction đã bị user cancel có cần xuất hiện như một “result” không?
có 

### 9.4. Thông tin so sánh dự đoán và kết quả thật

Response hiện tại có lựa chọn của user và reward point nhưng chưa chắc đã cho FE biết Top 3 chính thức.

**Đề xuất:** Endpoint result trả thêm:

- Top 3 user dự đoán.
- Top 3 kết quả chính thức.
- Điểm của từng lựa chọn.
- Tổng điểm và trạng thái.

**Cần bạn chốt:** Có cần mở rộng response như trên hay chỉ dùng `PredictionResponse` hiện tại?
có 

---

## 10. Danh sách race được gán cho Referee, Vet và Medical Staff

### 10.1. URL endpoint

**Đề xuất:**

```http
GET /api/referee/races/assigned
GET /api/vet/races/assigned
GET /api/medical/races/assigned
```

**Cần bạn chốt:** Có dùng ba URL này không?
có 

### 10.2. Phạm vi của Referee

Project có hai kiểu phân công:

- `Round.headReferee`: trọng tài chính của round.
- `RaceReferee`: trọng tài được gán trực tiếp vào race.

**Đề xuất:** API của Referee hợp nhất cả hai nguồn, không lặp race. Response ghi rõ vai trò `HEAD_REFEREE` hoặc `REFEREE`; nếu cùng lúc có cả hai thì ưu tiên `HEAD_REFEREE`.

**Cần bạn chốt:** Head Referee có mặc nhiên được xem là được gán cho tất cả race trong round không?
có 

### 10.3. Phạm vi của Vet và Medical Staff

Mỗi race hiện có một `RaceInspectionAssignment`, chứa một Vet và một Medical Staff.

**Đề xuất:** Mỗi người chỉ thấy các race có assignment trỏ đúng profile của họ.

**Cần bạn chốt:** Sau khi race start và nhân viên được chuyển lại `AVAILABLE`, assignment cũ có tiếp tục xuất hiện trong lịch sử không?
có 

### 10.4. Status nào được trả

Các lựa chọn:

1. Chỉ race sắp tới/đang diễn ra.
2. Tất cả race gồm cả lịch sử.
3. Có query `scope=upcoming|history|all`.

**Đề xuất:** Mặc định trả race chưa kết thúc (`SCHEDULED`, `ONGOING`); sau này thêm endpoint lịch sử riêng.

**Cần bạn chốt:** Muốn phương án 1, 2 hay 3?
1 

### 10.5. Race bị hủy

**Đề xuất:** Race `CANCELLED` không nằm trong danh sách công việc sắp tới, nhưng có thể nằm trong lịch sử nếu API hỗ trợ history.

**Cần bạn chốt:** Có cần hiển thị race bị hủy để nhân viên biết assignment đã bị hủy không?
có 

### 10.6. Dữ liệu response

**Đề xuất gồm:**

- Tournament, round, race và trạng thái.
- `startTime`, `endTime`, inspection open/close được tính từ config tournament.
- Loại assignment và `assignedAt`.
- Số entry cần xử lý.

**Cần bạn chốt:** Có cần trả luôn danh sách entry trong cùng API, hay chỉ trả race và FE gọi API entry riêng?
có 
---

## 11. Quy tắc cạnh tranh dữ liệu và transaction

Các API thay đổi trạng thái có thể bị gọi hai lần hoặc đồng thời từ nhiều request.

**Đề xuất:**

- Lock contract/registration/tournament khi cancel, withdraw hoặc close registration.
- Toàn bộ cập nhật trạng thái, invoice và refund nằm trong cùng transaction.
- Request thứ hai thấy trạng thái mới sẽ bị chặn bằng lỗi nghiệp vụ rõ ràng.

**Cần bạn chốt:** Có đồng ý xử lý strict như trên không?
có 

---

## 12. Bộ quy tắc mặc định mình đề xuất nếu bạn muốn triển khai nhanh

Nếu bạn đồng ý toàn bộ mặc định dưới đây, có thể trả lời ngắn là **“Chốt theo đề xuất”**:
 tôi đã note câu trả ở mỗi câu hỏi rồi 

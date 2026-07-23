# HRTMS Backend — Bản đồ dự án, nghiệp vụ và toàn bộ API

> Tài liệu được tổng hợp trực tiếp từ source BE hiện tại ngày **17/07/2026**.
> Phạm vi gồm cả code đang nằm trong working tree nhưng chưa commit, đặc biệt là module
> `simulation` thời gian thực. Khi source thay đổi, cần quét lại controller để cập nhật file này.

## 1. Đọc nhanh: BE hiện có những gì?

BE hiện là một ứng dụng Spring Boot dạng monolith, chia theo layer. Kết quả quét source:

| Thành phần | Số lượng hiện tại |
|---|---:|
| Java source | 547 file |
| Controller REST | 47 controller có endpoint |
| Mapping REST | 232 handler mapping |
| Route thực tế | 233 route, vì API generate AI có 2 URL alias |
| Entity JPA chính | 42 entity |
| Repository | 42 repository |
| Service interface + implementation | 118 file |
| Flyway migration | 13 migration |
| Test Java | 32 file |

Các nhóm nghiệp vụ đã có:

1. Xác thực bằng OTP email và JWT.
2. Hồ sơ theo role: Admin, Horse Owner, Jockey, Spectator, Referee, Veterinarian, Medical Staff.
3. Quản lý ngựa và điểm rating.
4. Tournament, bracket, round, race và lịch thi đấu.
5. Đăng ký ngựa/Kỵ sĩ vào Tournament.
6. Mời Kỵ sĩ, hợp đồng, hóa đơn, escrow và payout.
7. Race Entry, lane, Referee, Vet và Medical Staff assignment.
8. Inspection ngựa/Kỵ sĩ và readiness trước khi start.
9. Prediction Top 3 và AI Prediction.
10. Race result, violation, appeal, Race Report và công bố kết quả.
11. Ví, giao dịch và VNPay.
12. Notification lưu DB, email và WebSocket.
13. Live race simulation thời gian thực, cảnh báo và cờ trọng tài.
14. Homepage và portal API theo từng role.

## 2. Công nghệ và cách chạy

| Hạng mục | Công nghệ/cấu hình |
|---|---|
| Java | Java 21 trong `pom.xml` |
| Framework | Spring Boot 4.0.6 |
| REST | Spring Web MVC |
| Database | MySQL + Spring Data JPA |
| Migration | Flyway MySQL, V1 đến V13 |
| Authentication | JWT HS512, Spring OAuth2 Resource Server |
| Password | BCrypt strength 10 |
| Realtime | STOMP + SockJS + Spring WebSocket |
| API docs | Springdoc OpenAPI/Swagger |
| Email | Spring Mail |
| File/image | Cloudinary |
| AI | OpenAI Java SDK, model lấy từ `ai.model` |
| Payment | VNPay |
| Mapping | MapStruct + Lombok |

URL local mặc định:

```text
BE:      http://localhost:8080
Swagger: http://localhost:8080/swagger-ui/index.html
OpenAPI: http://localhost:8080/v3/api-docs
WS:      http://localhost:8080/ws
```

Lưu ý database hiện dùng đồng thời:

- `spring.jpa.hibernate.ddl-auto=update` để Hibernate tự cập nhật schema.
- Flyway để chạy migration có version.

Với production nên chọn một chiến lược schema chính, thường là Flyway, để tránh Hibernate
và migration thay đổi schema không đồng nhất.

## 3. Cấu trúc package: phần nào làm gì?

Source chính nằm tại:

```text
src/main/java/com/swp391/horseracing
```

| Package | Trách nhiệm |
|---|---|
| `config` | Security, JWT decoder, CORS, Swagger, WebSocket, Cloudinary, OpenAI, VNPay, rating config và khởi tạo ví hệ thống. |
| `controller` | Nhận HTTP request, validate request DTO, gọi service và bọc response. |
| `dto` | Request/response contract giữa FE và BE, chia theo module. |
| `entity` | 42 entity JPA ánh xạ sang database. |
| `enums` | Trạng thái và loại dữ liệu của toàn bộ nghiệp vụ. |
| `exception` | `AppException`, `ErrorCode`, global exception handler. |
| `mapper` | Chuyển entity sang DTO, chủ yếu bằng MapStruct hoặc mapper thủ công. |
| `policy` | Kiểm tra timeline Tournament và chính sách xếp lịch Round/Race. |
| `repository` | Truy vấn database bằng Spring Data JPA. |
| `scheduler` | Tự chuyển phase, finalize inspection, void prediction, xử lý notification và retention. |
| `security` | Tiện ích lấy user hiện tại từ Security Context. |
| `service` | Interface nghiệp vụ. |
| `service.impl` | Cài đặt nghiệp vụ, transaction và authorization theo ownership/assignment. |
| `simulation.api` | REST API cho live race và bảng điều khiển trọng tài. |
| `simulation.domain` | Enum/trạng thái của simulation, warning và flag. |
| `simulation.engine` | Thuật toán deterministic tạo timeline, telemetry và anomaly. |
| `simulation.persistence` | Session, participant, warning, flag và provisional result. |
| `simulation.realtime` | Prepare/start, scheduler phát frame, WebSocket publisher, reconnect snapshot và incident review. |
| `util` | Hàm dùng chung. |

Luồng xử lý chuẩn:

```text
FE/Postman
→ Controller
→ Service interface
→ ServiceImpl / Policy
→ Repository
→ MySQL
→ Mapper/DTO
→ ApiResponse trả về FE
```

## 4. Authentication, authorization và response chung

### 4.1. Các role

```text
ADMIN
HORSE_OWNER
JOCKEY
SPECTATOR
REFEREE
VETERINARIAN
MEDICAL_STAFF
```

JWT gửi trong header:

```http
Authorization: Bearer <token>
```

### 4.2. Endpoint thực sự public

Chỉ các endpoint sau được `SecurityConfig` cho phép không cần JWT:

| Method | Endpoint |
|---|---|
| POST | `/api/auth/register-otp` |
| POST | `/api/auth/register/verify` |
| POST | `/api/auth/register/resend-otp` |
| POST | `/api/auth/login` |
| GET | `/api/public/home` |
| GET | `/api/public/races/live` |
| GET | `/api/races/{raceId}/live-snapshot` |
| GET | `/api/payments/vnpay/return` |
| GET | `/api/payments/vnpay/ipn` |
| ANY | `/swagger-ui/**`, `/v3/api-docs/**`, `/ws/**` |

Tất cả URL còn lại cần JWT. Nếu controller không có `@PreAuthorize`, nó chỉ có nghĩa
là chưa khóa cứng theo role ở method; request vẫn phải authenticated vì
`anyRequest().authenticated()`.

### 4.3. Response thành công

```json
{
  "code": 200,
  "message": "Success",
  "result": {}
}
```

`result` sẽ bị bỏ khỏi JSON nếu null. API phân trang dùng:

```json
{
  "items": [],
  "page": 0,
  "size": 20,
  "totalElements": 0,
  "totalPages": 0,
  "first": true,
  "last": true
}
```

### 4.4. Response lỗi

```json
{
  "code": 1715,
  "message": "Race entry is missing a confirmed and passed horse inspection"
}
```

Nhóm mã lỗi chính:

| Khoảng/nhóm | Ý nghĩa |
|---|---|
| 1001–1010 | Lỗi chung, authentication, validation |
| 1101–1117 | User, login, token và OTP |
| 1201–1207 | Hồ sơ theo role |
| 1301–1306 | Horse và eligibility |
| 1401–1427 | Wallet, invoice, transaction và VNPay |
| 1501–1611 | Tournament, registration, scheduling, entry và contract |
| 1660–1678 | Prediction và AI Prediction |
| 1701–1722 | Staff assignment, inspection, start và violation window |
| 1801–1819 | Timeline, schedule, rating và result availability |
| 1901–1903 | Notification |
| 2001 | Upload file |
| 2101–2127 | Bracket, capacity và tournament timeline |
| 2201–2202 | Spectator follow horse |
| 2301–2310 | Live race simulation |
| 2601–2653 | Race result, report, appeal và violation |

Hiện có một số mã bị trùng trong `ErrorCode`: `1546`, `1673` và `1704`. Khi FE map
message theo code, các mã này có thể gây nhầm và nên được đổi thành code duy nhất.

## 5. Bản đồ dữ liệu nghiệp vụ

### 5.1. User và hồ sơ role

```text
Role ← User
       ├─ HorseOwner ─ Horse
       ├─ Jockey
       ├─ Spectator ─ SpectatorHorseFollow
       ├─ Referee
       ├─ Veterinarian
       └─ MedicalStaff
```

`User` giữ thông tin đăng nhập chung; bảng profile giữ dữ liệu nghề nghiệp theo role.

### 5.2. Tournament đến Race

```text
Tournament
├─ PrizeStructure
├─ TournamentEligibility
├─ HorseTournamentRegistration
├─ JockeyTournamentRegistration
└─ Round
   └─ Race
      ├─ RaceEntry ─ JockeyHorseContract ─ Horse + Jockey
      ├─ RaceReferee
      ├─ RaceInspectionAssignment
      ├─ HorseInspection
      ├─ JockeyInspection
      ├─ RaceResult
      ├─ Violation
      ├─ Appeal ─ AppealEvidence
      ├─ RaceReport
      ├─ Prediction ─ PredictionDetail
      └─ AIPrediction
```

### 5.3. Payment và notification

```text
User/System owner
└─ Wallet
   └─ Transaction

Invoice
└─ PaymentTransaction (VNPay hoặc thanh toán nội bộ)

NotificationEvent
└─ Notification
   └─ NotificationDelivery
      └─ WebSocket / Email
```

### 5.4. Simulation thời gian thực

```text
Race
└─ RaceSimulationSession
   ├─ RaceSimulationParticipant
   ├─ RaceSimulationWarning
   ├─ RaceSimulationFlag
   └─ RaceProvisionalResult
```

Frame telemetry không được ghi thành một row cho từng tick; session lưu timeline/current
snapshot, còn WebSocket phát frame theo thời gian.

## 6. Lifecycle và trạng thái quan trọng

### 6.1. Tournament

```text
Status: DRAFT → OPEN → ONGOING → FINISHED
                          └──────→ CANCELLED

Phase:
DRAFT
→ REGISTRATION_OPEN
→ REGISTRATION_REVIEW
→ JOCKEY_MATCHING
→ SCHEDULING
→ RACING
→ RESULT_PENDING
→ RESULT_PUBLISHED
→ FINISHED
```

### 6.2. Registration và contract

```text
Horse registration:
PENDING_PAYMENT → PENDING_REVIEW → APPROVED / REJECTED / WITHDRAWN

Jockey registration:
đăng ký hợp lệ → APPROVED trực tiếp

Contract:
PENDING_JOCKEY
→ ACCEPTED
→ HIRING_PAID
→ APPROVED

Nhánh khác: REJECTED / CANCELLED / TERMINATED
```

Theo code hiện tại, sau khi cả invoice phí thuê và phí tạo contract đều đã `PAID`,
`ContractActivationServiceImpl` tự giải ngân phần tạm ứng và chuyển contract từ
`HIRING_PAID` thẳng sang `APPROVED`. Enum `PENDING_ADMIN_REVIEW` vẫn còn nhưng không có
đường transition chính nào đang set trạng thái này.

### 6.3. Round, Race và Entry

```text
Round/Race:
SCHEDULING → SCHEDULED → ONGOING → FINISHED/COMPLETED
                                      └──────────────→ CANCELLED

RaceEntry:
CONFIRMED
├─ SCRATCHED
├─ WITHDRAWN_BEFORE_SCHEDULE
├─ WITHDRAWN_AFTER_SCHEDULE
├─ FINISHED
├─ DID_NOT_FINISH
└─ DISQUALIFIED
```

### 6.4. Inspection và start

Giá trị mặc định ở Tournament:

```text
T-120: mở prediction
T-90:  mở inspection
T-30:  đóng/finalize inspection
T-5:   đóng prediction
T-0:   race bắt đầu
T+30:  hết cửa sổ start muộn mặc định
```

Mỗi entry cần:

- Horse inspection: `CONFIRMED + PASS`.
- Jockey inspection: `CONFIRMED + PASS`.
- Handicap được xác nhận nếu Tournament bật handicap.
- Entry vẫn `CONFIRMED` và race còn đủ `minEntries`.

### 6.5. Result, report và prediction

```text
RaceResultStatus: FINISHED / DID_NOT_FINISH / DISQUALIFIED
RaceReport: DRAFT → SUBMITTED_TO_HEAD → SIGNED → PUBLISHED
Prediction: PENDING → SCORED / VOIDED / CANCELLED
```

- `SCRATCHED` trước start không có result thi đấu.
- DNF và DISQUALIFIED sau start vẫn nằm trong result và prediction vẫn được chấm.
- Cancel toàn race làm prediction `VOIDED`.
- Publish report kích hoạt scoring, rating, round transition và payout nếu là Final.

### 6.6. Simulation

```text
PREPARING → READY → RUNNING → FINISHED
                           └→ ABORTED
```

Warning do hệ thống phát hiện không tự trở thành violation. Referee phải bỏ qua hoặc chuyển
warning thành flag; flag được confirm/dismiss sau race, rồi mới dùng làm nháp tạo violation.

## 7. Tác vụ tự động không có API riêng

| Scheduler | Chu kỳ | Việc thực hiện |
|---|---:|---|
| `TournamentPhaseScheduler` | 60 giây | Đóng registration sang review; kiểm tra Final đã hoàn tất để chuyển `RESULT_PENDING`. |
| `RaceDeadlineScheduler` | 30 giây | Finalize entry tại inspection deadline và void prediction không hợp lệ lúc đóng dự đoán. |
| `NotificationEventScheduler` | mặc định 5 giây | Xử lý event nghiệp vụ đang chờ. |
| `NotificationDeliveryScheduler` | mặc định 5 giây | Gửi WebSocket/email theo delivery. |
| `NotificationRetentionScheduler` | 03:00 mỗi ngày | Xóa notification cũ hơn số ngày retention, mặc định 90. |
| `RaceSimulationScheduler` | theo session live | Phát frame telemetry server-side. |
| `RaceSimulationRecovery` | lúc BE khởi động | Khôi phục session đang RUNNING hoặc abort nếu timeline bị thiếu. |

## 8. Quy ước đọc danh mục API

Ký hiệu cột quyền:

| Ký hiệu | Nghĩa |
|---|---|
| Public | Không cần JWT theo `SecurityConfig`. |
| JWT | Cần token nhưng controller không khai báo role cứng. Service vẫn có thể kiểm tra ownership/profile. |
| Admin | `ROLE_ADMIN`. |
| Owner | `ROLE_HORSE_OWNER`. |
| Jockey | `ROLE_JOCKEY`. |
| Spectator | `ROLE_SPECTATOR`. |
| Referee | `ROLE_REFEREE`; nhiều API còn kiểm tra được phân công đúng race/head referee. |
| Vet | `ROLE_VETERINARIAN`. |
| Medical | `ROLE_MEDICAL_STAFF`. |

Các bảng dưới đây liệt kê theo route thực tế. URL đều tính từ `http://localhost:8080`.

## 9. API xác thực, public và tài khoản chung

### 9.1. AuthenticationController

| Method | Endpoint | Quyền | Chức năng |
|---|---|---|---|
| POST | `/api/auth/register-otp` | Public | Nhận `UserCreationRequest`, kiểm tra dữ liệu và gửi OTP đăng ký qua email. |
| POST | `/api/auth/register/verify` | Public | Xác minh OTP và tạo tài khoản chính thức. |
| POST | `/api/auth/register/resend-otp` | Public | Gửi lại OTP cho đăng ký đang chờ. |
| POST | `/api/auth/login` | Public | Đăng nhập, trả JWT và thông tin tài khoản. |
| GET | `/api/auth/me` | JWT | Lấy user/profile cơ bản của token hiện tại. |

### 9.2. Homepage, user và upload

| Controller | Method | Endpoint | Quyền | Chức năng |
|---|---|---|---|---|
| HomePageController | GET | `/api/public/home` | Public | Gom stats, tournament nổi bật, lịch, kết quả, ranking và prediction nổi bật cho Homepage. |
| UserController | GET | `/api/users` | JWT | Lấy danh sách user. |
| UserController | POST | `/api/users/avatar` | JWT | Upload avatar của user hiện tại. |
| CloudinaryController | POST | `/api/images/upload` | JWT | Upload ảnh/file ảnh chung lên Cloudinary. |

### 9.3. Public live race

| Method | Endpoint | Quyền | Chức năng |
|---|---|---|---|
| GET | `/api/public/races/live` | Public | Danh sách race đang RUNNING; nếu không có thì trả race vừa FINISHED trong 30 phút. |
| GET | `/api/races/{raceId}/live-snapshot` | Public | Snapshot hiện tại để viewer mở trang/reconnect. Không trả warning/flag riêng của trọng tài. |

## 10. API Admin: Tournament, bracket, scheduling và vận hành

### 10.1. AdminController — cấu hình Tournament

| Method | Endpoint | Quyền | Chức năng |
|---|---|---|---|
| POST | `/api/admin/tournaments` | Admin | Tạo Tournament ở `DRAFT`; body `CreateTournamentRequest`. |
| PUT | `/api/admin/tournaments/{id}` | Admin | Cập nhật Tournament khi nghiệp vụ cho phép. |
| DELETE | `/api/admin/tournaments/{id}` | Admin | Xóa Tournament chưa bị khóa bởi lifecycle. |
| GET | `/api/admin/tournaments/{id}/bracket-preview` | Admin | Xem cấu trúc bracket dự kiến, số round/race và version. |
| GET | `/api/admin/tournaments/{id}/schedule-proposal` | Admin | Xem đề xuất lịch dựa trên capacity, ngày, giờ, break và khoảng cách race. |
| POST | `/api/admin/tournaments/{id}/bracket-confirm` | Admin | Xác nhận bracket bằng capacity và `expectedPlanVersion`. |
| POST | `/api/admin/tournaments/{id}/prize-structures` | Admin | Tạo cơ cấu giải thưởng. |
| PUT | `/api/admin/prize-structures/{prizeStructureId}` | Admin | Sửa cơ cấu giải thưởng. |
| DELETE | `/api/admin/prize-structures/{prizeStructureId}` | Admin | Xóa cơ cấu giải thưởng. |
| POST | `/api/admin/tournaments/{id}/eligibility` | Admin | Tạo điều kiện ngựa tham gia. |
| PUT | `/api/admin/eligibility/{eligibilityId}` | Admin | Sửa điều kiện tham gia. |
| DELETE | `/api/admin/eligibility/{eligibilityId}` | Admin | Xóa điều kiện tham gia. |
| POST | `/api/admin/tournaments/{id}/rounds` | Admin | Tạo Round thủ công. |
| PUT | `/api/admin/rounds/{roundId}` | Admin | Sửa Round. |
| DELETE | `/api/admin/rounds/{roundId}` | Admin | Xóa Round. |
| POST | `/api/admin/rounds/{roundId}/races` | Admin | Tạo Race trong Round. |
| PUT | `/api/admin/races/{raceId}` | Admin | Sửa Race và lịch thi đấu. |
| DELETE | `/api/admin/races/{raceId}` | Admin | Xóa Race. |
| POST | `/api/admin/tournaments/{id}/publish` | Admin | Validate cấu hình rồi publish: `DRAFT → REGISTRATION_OPEN`. |
| POST | `/api/admin/tournaments/{id}/close-registration` | Admin | Đóng đăng ký có chủ đích trước/chạm deadline theo rule. |
| POST | `/api/admin/tournaments/{id}/complete-review` | Admin | Kết thúc duyệt ngựa và chuyển sang matching. |
| POST | `/api/admin/tournaments/{id}/complete-matching` | Admin | Validate contract/bracket, tạo/phân entry vòng đầu và chuyển scheduling. |
| POST | `/api/admin/tournaments/{id}/publish-schedule` | Admin | Publish toàn bộ lịch của Round đang active. |
| POST | `/api/admin/tournaments/{id}/publish-results` | Admin | Publish kết quả cấp Tournament theo lifecycle cũ/tổng hợp. |

### 10.2. AdminController — đăng ký và contract

| Method | Endpoint | Quyền | Chức năng |
|---|---|---|---|
| GET | `/api/admin/horse-registrations` | Admin | Danh sách đăng ký ngựa. |
| POST | `/api/admin/horse-registrations/{id}/approve` | Admin | Duyệt đăng ký ngựa sang `APPROVED`. |
| POST | `/api/admin/horse-registrations/{id}/reject` | Admin | Từ chối đăng ký; body có thể là chuỗi lý do. |
| GET | `/api/admin/jockey-registrations` | Admin | Danh sách đăng ký Jockey; hiện Jockey được approve trực tiếp. |
| GET | `/api/admin/contracts` | Admin | Lấy contract theo `status`, `page`, `size`. |
| GET | `/api/admin/contracts/approved/tournaments/{tournamentId}` | Admin | Contract `APPROVED` của Tournament; query `page`, `size`. |
| POST | `/api/admin/contracts/{id}/release-final-payout` | Admin | Giải phóng phần hiring fee còn giữ trong escrow khi đủ điều kiện. |

### 10.3. AdminController — entry, lane và nhân sự

| Method | Endpoint | Quyền | Chức năng |
|---|---|---|---|
| POST | `/api/admin/races/{raceId}/entries` | Admin | Tạo Race Entry từ contract đã duyệt. Alias nghiệp vụ của RaceEntryController. |
| DELETE | `/api/admin/race-entries/{entryId}` | Admin | Xóa entry khi race còn sửa được. |
| GET | `/api/admin/referees` | Admin | Danh sách Referee, query tùy chọn `status`. |
| POST | `/api/admin/races/{raceId}/referees` | Admin | Phân Referee vào Race. |
| DELETE | `/api/admin/races/{raceId}/referees/{refereeId}` | Admin | Gỡ Referee khỏi Race. |
| POST | `/api/admin/races/{raceId}/inspection-staff/assign` | Admin | Gán cụ thể Vet và Medical Staff. |
| POST | `/api/admin/races/{raceId}/inspection-staff/auto-assign` | Admin | Tự chọn staff đang khả dụng và không trùng lịch. |
| POST | `/api/admin/races/{raceId}/publish-schedule` | Admin | Publish lịch riêng một Race sau khi validate entry/lane/staff. |

### 10.4. AdminController — cancel và postpone

| Method | Endpoint | Quyền | Chức năng |
|---|---|---|---|
| GET | `/api/admin/races/{raceId}/reschedule-proposals` | Admin | Đề xuất các slot đổi lịch hợp lệ gần nhất. |
| POST | `/api/admin/races/{raceId}/postpone` | Admin | Đổi start/end bằng `RescheduleRaceRequest`; kiểm tra toàn bộ conflict. |
| POST | `/api/admin/races/{raceId}/cancel` | Admin | Hủy race, void prediction, giải phóng staff và phát notification. |

### 10.5. Admin dashboard, system wallet và transaction

| Controller | Method | Endpoint | Quyền | Chức năng |
|---|---|---|---|---|
| AdminDashboardController | GET | `/api/admin/dashboard/summary` | Admin | Số liệu tổng hợp cho Admin Dashboard. |
| AdminWalletController | POST | `/api/admin/wallets/system/initialize` | Admin | Tạo/khởi tạo các ví hệ thống còn thiếu. |
| AdminWalletController | GET | `/api/admin/wallets/system` | Admin | Xem các ví system: revenue, escrow, prize pool… |
| AdminWalletController | POST | `/api/admin/wallets/system/prize-pool/top-up` | Admin | Tạo giao dịch nạp quỹ giải thưởng qua VNPay. |
| AdminTransactionController | GET | `/api/admin/transactions/system` | Admin | Lịch sử transaction của toàn bộ ví hệ thống. |
| AdminTransactionController | GET | `/api/admin/transactions/system/{purpose}` | Admin | Transaction của một system wallet purpose. |

## 11. API đọc Tournament/Round/Race dùng chung

`TournamentController` không có role guard cho các API GET, nhưng vẫn cần JWT.

| Method | Endpoint | Quyền | Chức năng |
|---|---|---|---|
| GET | `/api/tournaments` | JWT | Danh sách Tournament. |
| GET | `/api/tournaments/{id}` | JWT | Chi tiết Tournament. |
| GET | `/api/tournaments/{id}/prizes` | JWT | Cơ cấu giải thưởng. |
| GET | `/api/tournaments/{id}/eligibility` | JWT | Điều kiện tham gia. |
| GET | `/api/tournaments/{id}/rounds` | JWT | Các Round của Tournament. |
| GET | `/api/tournaments/rounds/{roundId}/races` | JWT | Các Race trong Round. |
| GET | `/api/tournaments/rounds/{roundId}/qualifiers` | JWT | Danh sách ngựa/Jockey đủ điều kiện đi tiếp. |
| PUT | `/api/tournaments/rounds/{roundId}/head-referee` | Admin | Gán Head Referee cho Round; query bắt buộc `refereeId`. |
| DELETE | `/api/tournaments/rounds/{roundId}/head-referee` | Admin | Gỡ Head Referee. |

## 12. AI Prediction và Horse Rating

### 12.1. AIPredictionController

| Method | Endpoint | Quyền | Chức năng |
|---|---|---|---|
| POST | `/api/admin/races/{raceId}/ai-predictions` | Admin | Generate dự đoán AI; alias cũ. |
| POST | `/api/admin/races/{raceId}/ai-predictions/generate` | Admin | Generate/replace bản nháp AI Prediction. |
| GET | `/api/admin/races/{raceId}/ai-predictions` | Admin | Xem bản AI Prediction, kể cả chưa publish. |
| POST | `/api/admin/races/{raceId}/ai-predictions/publish` | Admin | Công khai bản AI Prediction cho Spectator. |
| POST | `/api/admin/races/{raceId}/ai-predictions/unpublish` | Admin | Thu hồi khỏi giao diện Spectator. |
| GET | `/api/spectator/races/{raceId}/ai-predictions` | Spectator | Chỉ trả bản đã `PUBLISHED`. |

### 12.2. HorseRatingController

| Method | Endpoint | Quyền | Chức năng |
|---|---|---|---|
| GET | `/api/admin/races/{raceId}/rating-preview` | Admin | Xem điểm rating thủ công đã được Head Referee ký trước khi publish report. |
| GET | `/api/admin/races/{raceId}/rating-changes` | Admin | Xem thay đổi rating đã áp dụng. |
| GET | `/api/horses/{horseId}/rating-history` | Owner/Admin | Lịch sử rating của một ngựa. |
| GET | `/api/admin/rounds/{roundId}/rating-summary` | Admin | Tổng hợp rating theo Round. |

## 13. API Horse Owner và ngựa

### 13.1. HorseController

Toàn bộ controller này bị khóa `HORSE_OWNER` và kiểm tra ngựa thuộc owner hiện tại.

| Method | Endpoint | Quyền | Chức năng |
|---|---|---|---|
| GET | `/api/horses/my-horses` | Owner | Danh sách ngựa của owner hiện tại. |
| GET | `/api/horses/{id}` | Owner | Chi tiết một ngựa thuộc owner. |
| POST | `/api/horses` | Owner | Tạo hồ sơ ngựa. |
| PUT | `/api/horses/{id}` | Owner | Sửa hồ sơ ngựa. |
| DELETE | `/api/horses/{id}` | Owner | Xóa ngựa khi chưa bị ràng buộc nghiệp vụ. |
| POST | `/api/horses/{id}/image` | Owner | Upload ảnh riêng cho ngựa. |

### 13.2. OwnerController — profile và đăng ký Tournament

Các route này cần JWT nhưng controller chưa có `@PreAuthorize("hasRole('HORSE_OWNER')")`.
Service lấy owner từ token và sẽ lỗi nếu không có Owner profile; tuy vậy nên thêm role guard
để quyền được thể hiện rõ ngay tại controller.

| Method | Endpoint | Quyền khai báo | Chức năng |
|---|---|---|---|
| POST | `/api/owners/profile` | JWT | Tạo Horse Owner profile cho user hiện tại. |
| PUT | `/api/owners/profile` | JWT | Cập nhật Owner profile. |
| GET | `/api/owners/me` | JWT | Lấy Owner profile hiện tại. |
| POST | `/api/owners/tournaments/{id}/register-horse` | JWT | Đăng ký một ngựa vào Tournament; body `RegisterHorseRequest`. |
| GET | `/api/owners/tournaments/{id}/accepted-jockeys` | JWT | Danh sách Jockey đã đăng ký/khả dụng trong Tournament để mời. |
| GET | `/api/owners/my-registrations` | JWT | Danh sách đăng ký ngựa của owner. |
| POST | `/api/owners/registrations/{id}/withdraw` | JWT | Rút đăng ký khi phase/status và contract cho phép. |

### 13.3. OwnerContractController

| Method | Endpoint | Quyền | Chức năng |
|---|---|---|---|
| GET | `/api/owner/contracts` | Owner | Danh sách contract owner đã tạo. |
| GET | `/api/owner/contracts/{id}` | Owner | Chi tiết contract thuộc owner. |
| POST | `/api/owner/contracts/invite` | Owner | Mời Jockey cho một horse registration; body `InviteRequest`. |
| POST | `/api/owner/contracts/{id}/cancel` | Owner | Hủy contract theo trạng thái cho phép; body `CancelContractRequest`. |

### 13.4. Lịch và kết quả của Owner

| Method | Endpoint | Quyền | Chức năng |
|---|---|---|---|
| GET | `/api/owner/race-schedule` | Owner | Lịch đã publish có entry của ngựa thuộc owner. |
| GET | `/api/owner/races` | Owner | Danh sách race của owner, hỗ trợ portal/filter. |
| GET | `/api/owner/race-results` | Owner | Kết quả chính thức đã đủ điều kiện hiển thị. |
| GET | `/api/owner/race-results/provisional` | Owner | Kết quả tạm thời sau simulation/race trước khi report publish. |

## 14. API Jockey

### 14.1. JockeyController

Các endpoint profile/directory cần JWT nhưng controller chưa khóa role cứng.

| Method | Endpoint | Quyền khai báo | Chức năng |
|---|---|---|---|
| GET | `/api/jockeys` | JWT | Danh bạ Jockey. |
| GET | `/api/jockeys/{id}` | JWT | Chi tiết Jockey. |
| POST | `/api/jockeys/profile` | JWT | Tạo Jockey profile cho user hiện tại. |
| GET | `/api/jockeys/me` | JWT | Lấy Jockey profile hiện tại. |
| PUT | `/api/jockeys/profile` | JWT | Cập nhật Jockey profile. |

### 14.2. Đăng ký Tournament và ngựa được ghép

| Controller | Method | Endpoint | Quyền | Chức năng |
|---|---|---|---|---|
| JockeyTournamentController | POST | `/api/jockey/tournaments/{id}/register` | Jockey | Đăng ký Tournament và khai báo hire fee. |
| JockeyTournamentController | GET | `/api/jockey/tournaments/{id}/accepted-horses` | Jockey | Ngựa đã được duyệt trong Tournament. |
| JockeyTournamentController | GET | `/api/jockey/my-registrations` | Jockey | Các Tournament Jockey đã đăng ký. |
| JockeyAssignedHorseController | GET | `/api/jockey/horses/assigned` | Jockey | Các ngựa đang được gán qua contract. |
| JockeyStatisticsController | GET | `/api/jockey/statistics` | Jockey | Win rate, Top 3, achievement và thống kê Jockey hiện tại. |

### 14.3. JockeyContractController

| Method | Endpoint | Quyền | Chức năng |
|---|---|---|---|
| GET | `/api/jockey/contracts` | Jockey | Danh sách contract liên quan đến Jockey. |
| GET | `/api/jockey/contracts/{id}` | Jockey | Chi tiết contract. |
| GET | `/api/jockey/contracts/invitations` | Jockey | Các lời mời `PENDING_JOCKEY`. |
| POST | `/api/jockey/contracts/{id}/accept` | Jockey | Chấp nhận lời mời, tạo bước invoice/hiring payment tiếp theo. |
| POST | `/api/jockey/contracts/{id}/reject` | Jockey | Từ chối lời mời; body `ContractRejectRequest`. |

### 14.4. Lịch và kết quả của Jockey

| Method | Endpoint | Quyền | Chức năng |
|---|---|---|---|
| GET | `/api/jockey/race-schedule` | Jockey | Lịch thi đấu đã publish của Jockey. |
| GET | `/api/jockey/races` | Jockey | Danh sách race của Jockey. |
| GET | `/api/jockey/race-results` | Jockey | Kết quả chính thức. |
| GET | `/api/jockey/race-results/provisional` | Jockey | Kết quả tạm thời. |

## 15. Invoice, contract payment, wallet và VNPay

### 15.1. InvoiceController và PaymentController

| Controller | Method | Endpoint | Quyền | Chức năng |
|---|---|---|---|---|
| InvoiceController | GET | `/api/invoices/my-invoices` | JWT | Hóa đơn của user hiện tại. |
| InvoiceController | POST | `/api/invoices/{id}/pay` | JWT | Thanh toán invoice bằng wallet theo ownership. |
| InvoiceController | POST | `/api/invoices/{id}/refund` | Admin | Hoàn invoice theo rule và trạng thái thanh toán. |
| PaymentController | POST | `/api/contracts/{id}/pay-hiring-fee` | Owner | Thanh toán phí thuê, đưa tiền vào escrow. |
| PaymentController | POST | `/api/contracts/{id}/pay-contract-fee` | Owner | Thanh toán phí hệ thống để contract vào bước admin review. |

### 15.2. Wallet và transaction của user

| Controller | Method | Endpoint | Quyền | Chức năng |
|---|---|---|---|---|
| WalletController | GET | `/api/wallets/my-wallet` | Owner/Jockey | Xem ví cá nhân. |
| WalletController | POST | `/api/wallets/deposit` | Owner | Nạp ví theo luồng deposit service hiện có. |
| TransactionController | GET | `/api/transactions/my-transactions` | Owner/Jockey | Lịch sử biến động số dư. |

### 15.3. VNPayController

| Method | Endpoint | Quyền | Chức năng |
|---|---|---|---|
| POST | `/api/payments/vnpay/deposit` | JWT | Tạo URL/thanh toán VNPay cho user hoặc use case được service cho phép. |
| GET | `/api/payments/vnpay/return` | Public | Browser return từ VNPay; xác minh và redirect về FE. |
| GET | `/api/payments/vnpay/ipn` | Public | IPN server-to-server, idempotent, chỉ cộng tiền khi chữ ký/amount/code hợp lệ. |

## 16. API Scheduling Board, Race Entry, lane và staff

### 16.1. RaceEntryController

| Method | Endpoint | Quyền | Chức năng |
|---|---|---|---|
| POST | `/api/race-entries` | Admin | Tạo entry bằng `CreateRaceEntryRequest`. |
| PUT | `/api/race-entries/{entryId}` | Admin | Cập nhật status/thông tin entry theo transition hợp lệ. |
| DELETE | `/api/race-entries/{entryId}` | Admin | Xóa entry khi Race còn ở scheduling. |
| GET | `/api/race-entries/race/{raceId}` | JWT | Danh sách entry theo Race. |
| GET | `/api/race-entries/{entryId}` | JWT | Chi tiết Race Entry. |
| POST | `/api/race-entries/rounds/{roundId}/auto-assign` | Admin | Phân contract/entry tự động, cân bằng cho các race trong Round. |
| POST | `/api/race-entries/races/{raceId}/auto-assign-lanes` | Admin | Random/gán lane còn trống cho Race. |
| PATCH | `/api/race-entries/{entryId}/lane` | Admin | Đổi lane bằng `UpdateLaneRequest`. |
| PATCH | `/api/race-entries/{entryId}/swap/{targetEntryId}` | Admin | Hoán đổi lane của hai entry cùng Race. |

### 16.2. API đọc entry và Referee assignment

| Controller | Method | Endpoint | Quyền | Chức năng |
|---|---|---|---|---|
| RaceEntryViewController | GET | `/api/races/{raceId}/entries` | JWT | Public-to-authenticated view của entry cho result/report/prediction UI. |
| RaceRefereeController | POST | `/api/race-referees` | Admin | Tạo assignment Referee bằng DTO trực tiếp. |
| RaceRefereeController | GET | `/api/race-referees/race/{raceId}` | JWT | Các Referee của Race. |
| RaceRefereeController | GET | `/api/race-referees/{raceRefereeId}` | JWT | Chi tiết assignment. |
| RaceRefereeController | DELETE | `/api/race-referees/{raceRefereeId}` | Admin | Xóa assignment. |

BE đang có hai bộ URL cho cùng nghiệp vụ entry/referee:

- `/api/admin/races/...` trong `AdminController`.
- `/api/race-entries...` và `/api/race-referees...` trong controller riêng.

Nên chọn một bộ canonical cho FE/Postman để tránh bảo trì hai contract song song.

### 16.3. Danh sách Race được phân công

| Method | Endpoint | Quyền | Chức năng |
|---|---|---|---|
| GET | `/api/referee/races/assigned` | Referee | Race Referee được phân công hoặc làm Head Referee. |
| GET | `/api/vet/races/assigned` | Vet | Race được phân công khám ngựa. |
| GET | `/api/medical/races/assigned` | Medical | Race được phân công khám Jockey. |

### 16.4. CRUD nhân sự

| Controller | Method | Endpoint | Quyền | Chức năng |
|---|---|---|---|---|
| RefereeController | POST | `/api/referees` | Admin | Tạo Referee profile. |
| RefereeController | GET | `/api/referees` | JWT | Danh sách Referee, có query filter trong controller/service. |
| RefereeController | GET | `/api/referees/{id}` | JWT | Chi tiết Referee. |
| RefereeController | PUT | `/api/referees/{id}` | Admin | Sửa Referee. |
| RefereeController | DELETE | `/api/referees/{id}` | Admin | Xóa/vô hiệu Referee theo rule. |
| VeterinarianController | POST | `/api/veterinarians` | Admin | Tạo Veterinarian profile. |
| VeterinarianController | GET | `/api/veterinarians` | JWT | Danh sách Veterinarian. |
| VeterinarianController | GET | `/api/veterinarians/{id}` | JWT | Chi tiết Veterinarian. |
| VeterinarianController | PUT | `/api/veterinarians/{id}` | Admin | Sửa Veterinarian. |
| VeterinarianController | DELETE | `/api/veterinarians/{id}` | Admin | Xóa/vô hiệu Veterinarian. |
| MedicalStaffController | POST | `/api/medical-staff` | Admin | Tạo Medical Staff profile. |
| MedicalStaffController | GET | `/api/medical-staff` | JWT | Danh sách Medical Staff. |
| MedicalStaffController | GET | `/api/medical-staff/{id}` | JWT | Chi tiết Medical Staff. |
| MedicalStaffController | PUT | `/api/medical-staff/{id}` | Admin | Sửa Medical Staff. |
| MedicalStaffController | DELETE | `/api/medical-staff/{id}` | Admin | Xóa/vô hiệu Medical Staff. |

## 17. API Inspection và Race readiness

### 17.1. Vet và Medical inspection

| Controller | Method | Endpoint | Quyền | Chức năng |
|---|---|---|---|---|
| VetInspectionController | POST | `/api/vet/race-entries/{entryId}/horse-inspection` | Vet | Tạo inspection ngựa; mỗi entry chỉ có một bản. |
| VetInspectionController | GET | `/api/vet/race-entries/{entryId}/horse-inspection` | Vet | Xem inspection ngựa. |
| MedicalInspectionController | POST | `/api/medical/race-entries/{entryId}/jockey-inspection` | Medical | Tạo inspection Jockey; mỗi entry chỉ có một bản. |
| MedicalInspectionController | GET | `/api/medical/race-entries/{entryId}/jockey-inspection` | Medical | Xem inspection Jockey. |

Body Horse inspection:

```json
{
  "result": "PASS",
  "note": "Đủ điều kiện",
  "handicapWeight": 54.5,
  "handicapConfirmed": true
}
```

Body Jockey inspection:

```json
{
  "result": "PASS",
  "note": "Đủ điều kiện"
}
```

Không gọi POST lần hai để sửa; service hiện trả `1708` hoặc `1709` nếu inspection đã tồn tại.

### 17.2. RefereeRaceController

| Method | Endpoint | Quyền | Chức năng |
|---|---|---|---|
| GET | `/api/referee/races/{raceId}/start-readiness` | Referee | Kiểm tra start window, entry, inspection, handicap và các blocking reason. |
| POST | `/api/referee/races/{raceId}/start` | Referee | Recheck readiness, chuyển Race sang `ONGOING`, ghi `startedAt/startedBy` và khởi động live simulation nếu đã prepare. |

Thứ tự an toàn:

```text
GET start-readiness
→ nếu canStart=true: POST simulation/prepare
→ POST start
→ viewer nhận frame WebSocket
```

## 18. API Referee: violation, result và Race Report

### 18.1. Violation

| Method | Endpoint | Quyền | Chức năng |
|---|---|---|---|
| POST | `/api/referee/race-entries/{entryId}/violations` | Referee | Tạo violation cho entry, body `CreateViolationRequest`. Hình phạt chỉ gồm cảnh cáo hoặc loại khỏi cuộc đua. |
| GET | `/api/races/{raceId}/violations` | JWT | Danh sách violation của Race. |

`ViolationType`: `FALSE_START`, `OBSTRUCTION`, `WRONG_LANE`, `EQUIPMENT`, `DOPING`, `OTHER`.

`PenaltyType`: `WARNING`, `DISQUALIFIED`.

### 18.2. RaceResultController

| Method | Endpoint | Quyền | Chức năng |
|---|---|---|---|
| POST | `/api/referee/races/{raceId}/results` | Referee | Tạo batch result lần đầu; body danh sách `CreateRaceResultRequest`. |
| PUT | `/api/referee/races/{raceId}/results` | Referee | Cập nhật batch result trước khi report bị khóa. |
| GET | `/api/referee/races/{raceId}/results` | Referee | Result kể cả trạng thái nội bộ/provisional cho Referee. |
| GET | `/api/races/{raceId}/results` | JWT | Result được phép hiển thị cho user đã đăng nhập. |

### 18.3. RaceReportController — Referee và Head Referee

| Method | Endpoint | Quyền | Chức năng |
|---|---|---|---|
| GET | `/api/referee/races/{raceId}/report` | Referee | Lấy hoặc mở report của Race được phân công. |
| PUT | `/api/referee/races/{raceId}/report` | Referee | Sửa summary/conclusion/appeal note khi còn `DRAFT`. |
| POST | `/api/referee/races/{raceId}/report/submit` | Referee | Gửi report lên Head Referee: `DRAFT → SUBMITTED_TO_HEAD`. |
| GET | `/api/head-referee/rounds/{roundId}/reports` | Referee | Head Referee xem report trong Round mình phụ trách. |
| GET | `/api/head-referee/races/{raceId}/report` | Referee | Head Referee xem chi tiết report. |
| PUT | `/api/head-referee/races/{raceId}/report` | Referee | Head Referee chỉnh nội dung trước khi ký. |
| POST | `/api/head-referee/races/{raceId}/report/return` | Referee | Trả report về Referee kèm lý do. |
| POST | `/api/head-referee/races/{raceId}/report/sign` | Referee | Ký report sau khi result và appeal hợp lệ. |
| POST | `/api/referee/races/{raceId}/report/sign` | Referee | Route sign legacy, nên dần bỏ để chỉ giữ head-referee flow. |
| GET | `/api/admin/races/{raceId}/report` | Admin | Admin xem report đã ký/chờ publish. |
| POST | `/api/admin/races/{raceId}/report/publish` | Admin | Publish official result; chạy scoring, rating, round transition và payout nếu Final. |
| GET | `/api/races/{raceId}/report` | JWT | Xem report đã công bố. |
| GET | `/api/races/{raceId}/ranking` | JWT | Bảng xếp hạng chính thức của Race. |

## 19. API Spectator

### 19.1. SpectatorRaceController

| Method | Endpoint | Quyền | Chức năng |
|---|---|---|---|
| GET | `/api/spectator/races/upcoming` | Spectator | Race sắp diễn ra đã publish lịch; query `page`, `size`. |
| GET | `/api/spectator/races/{raceId}` | Spectator | Chi tiết Race, entry, prediction window và dữ liệu cần cho trang dự đoán. |

Live overview/viewer dùng thêm hai Public API ở mục 9.3.

### 19.2. SpectatorPredictionController

| Method | Endpoint | Quyền | Chức năng |
|---|---|---|---|
| POST | `/api/spectator/races/{raceId}/predictions` | Spectator | Tạo prediction Top 3; body có ba entry và rank 1, 2, 3. |
| PUT | `/api/spectator/predictions/{predictionId}` | Spectator | Sửa prediction trước `predictionCloseAt`. |
| DELETE | `/api/spectator/predictions/{predictionId}` | Spectator | Hủy prediction khi còn được phép. |
| GET | `/api/spectator/predictions` | Spectator | Lịch sử prediction của user. |
| GET | `/api/spectator/predictions/{predictionId}` | Spectator | Chi tiết prediction. |
| GET | `/api/races/{raceId}/predictions/me` | Spectator | Prediction của user theo Race; FE dùng để biết đã tạo hay chưa. |
| GET | `/api/spectator/races/{raceId}/predictions/me/result` | Spectator | So sánh dự đoán với official result và điểm thưởng. |

Body tạo prediction mẫu:

```json
{
  "predictionType": "TOP3",
  "entries": [
    { "entryId": "<uuid-1>", "predictedRank": 1 },
    { "entryId": "<uuid-2>", "predictedRank": 2 },
    { "entryId": "<uuid-3>", "predictedRank": 3 }
  ]
}
```

### 19.3. SpectatorHorseController

| Method | Endpoint | Quyền | Chức năng |
|---|---|---|---|
| GET | `/api/spectator/horses` | Spectator | Tìm/lọc danh bạ ngựa. |
| GET | `/api/spectator/horses/following` | Spectator | Danh sách ngựa đang theo dõi. |
| POST | `/api/spectator/horses/{horseId}/follow` | Spectator | Theo dõi ngựa. |
| DELETE | `/api/spectator/horses/{horseId}/follow` | Spectator | Bỏ theo dõi. |
| GET | `/api/spectator/horses/{horseId}` | Spectator | Hồ sơ, thành tích và thông tin ngựa. |

## 20. API Appeal và evidence

### 20.1. AppealCategoryController

| Method | Endpoint | Quyền | Chức năng |
|---|---|---|---|
| POST | `/api/appeal-categories` | Admin | Tạo loại khiếu nại. |
| PUT | `/api/appeal-categories/{categoryId}` | Admin | Sửa loại khiếu nại. |
| PATCH | `/api/appeal-categories/{categoryId}/toggle` | Admin | Bật/tắt category. |
| GET | `/api/appeal-categories` | JWT | Danh sách category. |
| GET | `/api/appeal-categories/{categoryId}` | JWT | Chi tiết category. |

### 20.2. AppealController

| Method | Endpoint | Quyền | Chức năng |
|---|---|---|---|
| POST | `/api/appeals` | Owner/Jockey | Tạo appeal trong submission window của Race. |
| PUT | `/api/appeals/{appealId}` | Owner/Jockey | Sửa appeal khi còn pending và thuộc user. |
| DELETE | `/api/appeals/{appealId}` | Owner/Jockey | Hủy appeal. |
| GET | `/api/appeals/my` | Owner/Jockey | Appeal của user hiện tại. |
| GET | `/api/referee/appeals` | Referee | Danh sách appeal thuộc phạm vi Referee xử lý. |
| GET | `/api/referee/appeals/{appealId}` | Referee | Chi tiết appeal và evidence. |
| POST | `/api/referee/appeals/{appealId}/review` | Referee | Nhận xử lý/chấp nhận/từ chối theo `ReviewAppealRequest`. |

### 20.3. AppealEvidenceController

| Method | Endpoint | Quyền | Chức năng |
|---|---|---|---|
| POST | `/api/appeals/{appealId}/evidences` | Owner/Jockey | Thêm evidence bằng URL hoặc text. |
| GET | `/api/appeals/{appealId}/evidences` | Owner/Jockey/Referee | Danh sách evidence. |
| PUT | `/api/appeals/{appealId}/evidences/{evidenceId}` | Owner/Jockey | Sửa evidence thuộc appeal của mình. |
| DELETE | `/api/appeals/{appealId}/evidences/{evidenceId}` | Owner/Jockey | Xóa evidence. |
| POST | `/api/appeals/{appealId}/evidences/upload` | Owner/Jockey | Upload multipart evidence, kiểm tra loại file và dung lượng. |

## 21. Notification REST và WebSocket

### 21.1. NotificationController

| Method | Endpoint | Quyền | Chức năng |
|---|---|---|---|
| GET | `/api/notifications` | JWT | Notification của user hiện tại; query `page`, `size`, `unreadOnly`. |
| GET | `/api/notifications/unread-count` | JWT | Số notification chưa đọc. |
| PATCH | `/api/notifications/{notificationId}/read` | JWT | Đánh dấu một notification đã đọc. |
| PATCH | `/api/notifications/read-all` | JWT | Đánh dấu tất cả đã đọc. |
| DELETE | `/api/notifications/{notificationId}` | JWT | Archive/xóa khỏi inbox user. |
| GET | `/api/notifications/preferences` | JWT | Cấu hình nhận thông báo theo event/channel. |
| PUT | `/api/notifications/preferences/{eventType}` | JWT | Bật/tắt preference cho một event type. |

### 21.2. WebSocket/STOMP

Handshake:

```text
/ws (SockJS)
```

Destination:

| Destination | Quyền | Dữ liệu |
|---|---|---|
| `/user/queue/notifications` | JWT | Notification riêng của user. |
| `/topic/races/{raceId}/live` | Public | Telemetry, sự kiện công khai và kết quả tạm thời. |
| `/user/queue/races/{raceId}/control` | Referee được phân công/Head Referee | Warning và control event riêng. |

Client không được phép `SEND` STOMP lên server. Tất cả command thay đổi dữ liệu phải đi qua
REST API; WebSocket chỉ dùng subscribe/nhận dữ liệu.

## 22. Live Race Simulation API

### 22.1. RefereeRaceSimulationController

Prefix chung:

```text
/api/referee/races/{raceId}/simulation
```

| Method | Endpoint đầy đủ | Quyền | Chức năng |
|---|---|---|---|
| POST | `/api/referee/races/{raceId}/simulation/prepare` | Referee | Chụp danh sách participant/profile, tạo session `READY`. Idempotent với session hợp lệ đã có. |
| GET | `/api/referee/races/{raceId}/simulation/warnings` | Referee | Danh sách anomaly warning và trạng thái review. |
| GET | `/api/referee/races/{raceId}/simulation/flags` | Referee | Danh sách flag hệ thống/thủ công. |
| POST | `/api/referee/races/{raceId}/simulation/warnings/{warningId}/ignore` | Referee | Bỏ qua warning: `PENDING → IGNORED`. |
| POST | `/api/referee/races/{raceId}/simulation/warnings/{warningId}/flag` | Referee | Chuyển warning thành flag: `PENDING → FLAGGED`. |
| POST | `/api/referee/races/{raceId}/simulation/flags/manual` | Referee | Gắn cờ thủ công cho entry tại thời điểm race hiện tại. |
| POST | `/api/referee/races/{raceId}/simulation/flags/{flagId}/dismiss` | Referee | Bác bỏ flag sau khi đủ điều kiện review. |
| POST | `/api/referee/races/{raceId}/simulation/flags/{flagId}/confirm` | Referee | Xác nhận flag và trả `violationDraft` để mở form violation chính thức. |
| GET | `/api/referee/races/{raceId}/simulation/provisional-results` | Referee | Kết quả mô phỏng tạm thời để prefill form Race Result. |

Start simulation không có endpoint `/simulation/start` riêng. Sau `prepare`, Referee gọi:

```text
POST /api/referee/races/{raceId}/start
```

`RaceServiceImpl` start Race thật, sau đó lifecycle simulation tạo timeline deterministic và
scheduler phát frame.

### 22.2. Loại event WebSocket live

```text
SESSION_READY
RACE_STARTED
TELEMETRY_FRAME
RACE_EVENT
SYSTEM_WARNING       // chỉ private Referee queue
RACE_FINISHED
SESSION_ABORTED
```

Loại anomaly hiện có:

```text
ABNORMAL_SPEED_SPIKE
UNREALISTIC_ACCELERATION
STAMINA_DROP_TOO_FAST
CURVE_SPEED_ABNORMAL
PERFORMANCE_OUTLIER
```

## 23. DTO request chính cần biết khi dùng Postman

| Module | Request DTO |
|---|---|
| Auth | `UserCreationRequest`, `VerifyEmail`, `ResendOtp`, `AuthRequest` |
| Tournament | `CreateTournamentRequest`, `UpdateTournamentRequest`, `ConfirmBracketRequest` |
| Round/Race | `CreateRoundRequest`, `UpdateRoundRequest`, `CreateRaceRequest`, `UpdateRaceRequest` |
| Prize/Eligibility | `CreatePrizeStructureRequest`, `UpdatePrizeStructureRequest`, `CreateEligibilityRequest`, `UpdateEligibilityRequest` |
| Cancel/Postpone | `CancelRaceRequest`, `RescheduleRaceRequest` |
| Registration | `RegisterHorseRequest`, `RegisterJockeyRequest`, `WithdrawRegistrationRequest` |
| Contract | `InviteRequest`, `ContractRejectRequest`, `CancelContractRequest` |
| Race Entry | `CreateRaceEntryRequest`, `UpdateRaceEntryRequest`, `UpdateLaneRequest` |
| Staff | `CreateRaceRefereeRequest`, `AssignInspectionStaffRequest` |
| Inspection | `HorseInspectionRequest`, `JockeyInspectionRequest` |
| Prediction | `CreatePredictionRequest`, `PredictionEntryRequest`, `UpdatePredictionRequest` |
| Result | `CreateRaceResultRequest`, `UpdateRaceResultRequest` |
| Violation | `CreateViolationRequest` |
| Report | `UpdateRaceReportRequest`, `ReturnRaceReportRequest`, `SignRaceReportRequest` |
| Appeal | `CreateAppealRequest`, `ReviewAppealRequest`, `AddAppealEvidenceRequest` |
| Wallet/VNPay | `DepositRequest`, `AdminPrizePoolTopUpRequest` |
| Simulation | `ManualFlagRequest`, `IncidentReviewRequest` |

Field chi tiết và validation nên xem tại Swagger hoặc package `dto/<module>/request`.

## 24. Luồng API end-to-end nên test

### 24.1. Tạo Tournament và mở đăng ký

```text
POST /api/admin/tournaments
→ GET  /api/admin/tournaments/{id}/bracket-preview
→ POST /api/admin/tournaments/{id}/bracket-confirm
→ POST /api/admin/tournaments/{id}/prize-structures
→ POST /api/admin/tournaments/{id}/eligibility
→ POST /api/admin/tournaments/{id}/publish
```

### 24.2. Owner/Jockey registration và contract

```text
Owner:  POST /api/owners/tournaments/{id}/register-horse
Owner:  POST /api/invoices/{invoiceId}/pay
Admin:  POST /api/admin/horse-registrations/{id}/approve

Jockey: POST /api/jockey/tournaments/{id}/register

Owner:  POST /api/owner/contracts/invite
Jockey: POST /api/jockey/contracts/{id}/accept
Owner:  POST /api/contracts/{id}/pay-hiring-fee
Owner:  POST /api/contracts/{id}/pay-contract-fee
System: tự activate contract → APPROVED sau khi đủ hai invoice PAID
Admin:  POST /api/admin/tournaments/{id}/complete-matching
```

Vì vậy danh mục controller không có endpoint `approve contract`. Nếu nghiệp vụ cuối cùng
muốn Admin duyệt thủ công, BE hiện chưa triển khai bước đó; còn nếu chấp nhận auto-activation
thì nên bỏ `PENDING_ADMIN_REVIEW` và sửa tài liệu/UI cũ để tránh hiểu sai.

### 24.3. Scheduling Board

```text
GET  /api/admin/contracts/approved/tournaments/{tournamentId}
GET  /api/tournaments/{id}/rounds
GET  /api/tournaments/rounds/{roundId}/races
POST /api/race-entries/rounds/{roundId}/auto-assign
POST /api/race-entries/races/{raceId}/auto-assign-lanes
POST /api/admin/races/{raceId}/referees
POST /api/admin/races/{raceId}/inspection-staff/assign
POST /api/admin/tournaments/{id}/publish-schedule
```

### 24.4. Prediction, inspection và Live Race

```text
Spectator: POST /api/spectator/races/{raceId}/predictions

Vet:     POST /api/vet/race-entries/{entryId}/horse-inspection
Medical: POST /api/medical/race-entries/{entryId}/jockey-inspection

Referee: GET  /api/referee/races/{raceId}/start-readiness
Referee: POST /api/referee/races/{raceId}/simulation/prepare
Referee: POST /api/referee/races/{raceId}/start

Public:  GET  /api/races/{raceId}/live-snapshot
Public:  SUB  /topic/races/{raceId}/live
```

### 24.5. Kết thúc, appeal và công bố

```text
Referee: POST /api/referee/races/{raceId}/results
Referee: PUT  /api/referee/races/{raceId}/results
Owner/Jockey: POST /api/appeals
Referee: POST /api/referee/appeals/{appealId}/review
Referee: PUT  /api/referee/races/{raceId}/report
Referee: POST /api/referee/races/{raceId}/report/submit
Head:    GET  /api/head-referee/races/{raceId}/results
Head:    PUT  /api/head-referee/races/{raceId}/results
Head:    POST /api/head-referee/races/{raceId}/report/sign
Admin:   GET  /api/admin/races/{raceId}/rating-preview
Admin:   POST /api/admin/races/{raceId}/report/publish
```

Sau publish, kiểm tra:

- `GET /api/races/{raceId}/results`
- `GET /api/races/{raceId}/ranking`
- `GET /api/spectator/races/{raceId}/predictions/me/result`
- `GET /api/admin/races/{raceId}/rating-changes`
- `GET /api/transactions/my-transactions`
- Round sau được tạo entry nếu đây là Round trung gian đủ Top 4.

## 25. Database migration hiện có

| Migration | Nội dung chính |
|---|---|
| `V1__create_horse_rating_history.sql` | Lịch sử rating ngựa. |
| `V2__add_image_url.sql` | Image URL cho user và horse. |
| `V3__rebuild_notification_system.sql` | Event, notification, delivery và preference. |
| `V4__add_bracket_plan_fields.sql` | Capacity và bracket plan. |
| `V5__harden_bracket_round_transition.sql` | Field và constraint phục vụ chuyển Round. |
| `V6__normalize_bracket_defaults.sql` | Chuẩn hóa default bracket/capacity cũ. |
| `V7__add_tournament_distance_and_enum_conversions.sql` | Distance Tournament và chuyển enum. |
| `V8__add_tournament_competition_start.sql` | `competition_start_at`. |
| `V16__replace_automatic_horse_rating_with_referee_rating.sql` | Bỏ thành phần tự tính; lưu điểm Rating do trọng tài nhập và lý do Head Referee điều chỉnh. |
| `V9__add_transaction_performer_audit.sql` | Audit user thực hiện transaction. |
| `V10__increase_payment_purpose_length.sql` | Mở rộng payment purpose. |
| `V11__create_spectator_horse_follows.sql` | Follow horse của Spectator. |
| `V12__add_ai_prediction_publication_and_report_workflow.sql` | Publication AI và Race Report workflow. |
| `V13__create_realtime_race_simulation.sql` | Session, participant, warning, flag và provisional result của Live Race. |

## 26. Các điểm cần chú ý sau khi đọc source

Đây không phải lỗi khẳng định toàn hệ thống hỏng, nhưng là những chỗ dễ gây nhầm khi FE,
Postman và tài liệu nghiệp vụ làm việc với BE hiện tại.

### 26.1. Contract đang auto-approve, không có Admin approve API

- Sau hai invoice đã thanh toán, `ContractActivationServiceImpl` chuyển thẳng contract sang
  `APPROVED` và giải ngân advance.
- `PENDING_ADMIN_REVIEW` còn trong enum và một số query nhưng không được set trong main flow.
- `AdminDashboardServiceImpl` đang trả `pendingContracts = 0L` cố định.

Cần chọn một trong hai:

1. Giữ auto-approve: bỏ trạng thái/admin UI duyệt contract cũ.
2. Duyệt thủ công: thêm endpoint Admin approve/reject và chuyển sang
   `PENDING_ADMIN_REVIEW` sau khi thanh toán.

### 26.2. Có route trùng nghiệp vụ

- Tạo/xóa entry có cả `/api/admin/races/...` và `/api/race-entries...`.
- Referee assignment có cả `/api/admin/races/.../referees` và `/api/race-referees`.
- AI generate có `/ai-predictions` và `/ai-predictions/generate`.
- Race report sign còn route legacy `/api/referee/.../sign` bên cạnh `/api/head-referee/.../sign`.

Nên ghi một route là canonical và đánh dấu deprecate route còn lại.

### 26.3. Một số endpoint chỉ khóa JWT, chưa khóa role tại controller

Ví dụ `OwnerController`, `JockeyController`, các API directory staff và một số API đọc
race/result/report. Service thường kiểm tra profile/ownership, nhưng thêm `@PreAuthorize`
sẽ rõ contract bảo mật hơn và tránh role khác gọi nhầm.

### 26.4. `Race.status` đang dùng `RoundStatus`

Race và Round cùng dùng `RoundStatus`, nên xuất hiện cả `FINISHED` và `COMPLETED` và rất dễ
nhầm trạng thái nào áp dụng ở cấp nào. Tách `RaceStatus` riêng sẽ rõ hơn.

### 26.5. Direct wallet deposit và VNPay tồn tại song song

`POST /api/wallets/deposit` cộng số dư trực tiếp và tạo transaction `SUCCESS` mà không qua
callback cổng thanh toán. Endpoint này phù hợp seed/demo nhưng không nên mở trong production.
Luồng tiền thật nên dùng `/api/payments/vnpay/deposit` và chỉ credit sau callback hợp lệ.

### 26.6. ErrorCode chưa duy nhất

- `1546`: vừa `RACE_NAME_ALREADY_EXISTS`, vừa `CONTRACT_NOT_FOUND`.
- `1673`: vừa `INVALID_PREDICTION_TYPE`, vừa `AI_PREDICTION_GENERATION_FAILED`.
- `1704`: vừa `VETERINARIAN_NOT_FOUND`, vừa `VETERINARIAN_SUSPENDED`.

FE không thể map chính xác theo code nếu các lỗi trùng số.

### 26.7. Schema và secret configuration

- JPA `ddl-auto=update` chạy cùng Flyway.
- `application.properties` có các key datasource, JWT, mail, VNPay, OpenAI và Cloudinary.
- Không nên commit giá trị secret thật; chuyển sang environment variables hoặc profile local
  không được đưa lên Git.

### 26.8. README không khớp hoàn toàn với build

- `pom.xml` dùng Java 21.
- README vừa ghi Java 17+, vừa nhắc JDK 25.

Nên chuẩn hóa README thành Java 21 để thành viên mới setup đúng.

### 26.9. API GET “public data” vẫn cần đăng nhập

Các route như `/api/tournaments`, `/api/races/{id}/results`, `/api/races/{id}/report` và
`/api/races/{id}/ranking` không nằm trong `permitAll`, nên khách chưa đăng nhập không gọi được.
Nếu Homepage/public site cần dùng chúng trực tiếp thì phải mở có chọn lọc hoặc tiếp tục lấy
dữ liệu qua `/api/public/home`.

## 27. Controller coverage checklist

Danh mục API ở trên đã rà các controller sau:

```text
AdminController
AdminDashboardController
AdminTransactionController
AdminWalletController
AIPredictionController
AppealCategoryController
AppealController
AppealEvidenceController
AssignedRaceController
AuthenticationController
CloudinaryController
HomePageController
HorseController
HorseRatingController
InvoiceController
JockeyAssignedHorseController
JockeyContractController
JockeyController
JockeyStatisticsController
JockeyTournamentController
MedicalInspectionController
MedicalStaffController
NotificationController
OwnerContractController
OwnerController
ParticipantRaceController
PaymentController
RaceEntryController
RaceEntryViewController
RaceRefereeController
RaceReportController
RaceResultController
RefereeController
RefereeRaceController
RefereeViolationController
SpectatorHorseController
SpectatorPredictionController
SpectatorRaceController
TournamentController
TransactionController
UserController
VeterinarianController
VetInspectionController
VnpayController
WalletController
PublicLiveRaceController
RefereeRaceSimulationController
```

## 28. File nên mở tiếp khi cần đào sâu

| Muốn tìm hiểu | File/package |
|---|---|
| Luật nghiệp vụ chi tiết | `docs/RULES.md` |
| Luồng demo | `flow.md`, `docs/demo-flow.md` |
| Timeline/bracket | `policy/TournamentTimelinePolicy.java`, `policy/RoundSchedulePolicy.java` |
| Tournament lifecycle | `service/impl/TournamentServiceImpl.java` |
| Contract/payment | `ContractServiceImpl`, `ContractActivationServiceImpl`, `PaymentServiceImpl` |
| Start readiness | `RaceServiceImpl.java` |
| Result/report/payout | `RaceResultServiceImpl`, `RaceReportServiceImpl`, `ScoringServiceImpl` |
| Prediction | `PredictionServiceImpl.java` |
| Rating | `HorseRatingServiceImpl.java`, `HorseRatingProperties.java` |
| Live simulation | package `simulation` và `docs/REALTIME_RACE_SIMULATION.md` |
| Notification | các service `Notification*` và migration V3 |
| Error contract | `exception/ErrorCode.java`, `GlobalExceptionHandler.java` |
| Authorization | `config/SecurityConfig.java`, `WebSocketJwtChannelInterceptor.java` |

---

Nếu dùng file này để test Postman, nên bắt đầu từ mục **24. Luồng API end-to-end**,
sau đó quay lại danh mục API theo role khi cần test nhánh riêng.

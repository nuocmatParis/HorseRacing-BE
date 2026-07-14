# Báo cáo tổng hợp phần việc (Tác giả: nuocmatParis)

> Tổng hợp dựa trên lịch sử Git (`git log --author="nuocmatParis"`, email `tranphuhung1510@gmail.com`).
> Liệt kê các mảng chức năng đã làm, danh sách API và luồng chạy qua các class Controller → Service → Repository.

---

## 1. Phạm vi công việc (theo commit)

Các nhóm chức năng chính do bạn thực hiện:

| Nhóm chức năng | Commit tiêu biểu |
|----------------|------------------|
| Authentication & phân quyền | `add authentication`, `func find all users` |
| Xác thực email OTP | `verification email otp code`, `fix verification` |
| Ví & thanh toán VNPay | `deposti Vnpay`, `fix api vnpay return`, `api get my wallet`, `api system wallet` |
| Invoice & giao dịch | `api pay invoice, my invoice, system invoice`, `refund`, `get system transactions` |
| Đăng ký ngựa / nài (Horse–Jockey registration) | `fix api create tournament, jockey register tournament, horse, jockey` |
| Tournament / Round / Race + Bracket | `refactor bussiness logic tournament, round, race`, `fix round and race`, `fix bussiness logic tournament` |
| Hợp đồng nài–chủ ngựa (Contract) | `api invite jockey`, `api jockey accept, reject`, `api owner pay hiring fee, contract fee`, `admin approve contract`, `release money when win in final race` |
| Race Entry & phân công trọng tài | `feature/race-entry-and-referee-assignment` |
| Khám ngựa/nài & bắt đầu đua | `api vet submit inspection, medical staff submit inspection, referee start race` |
| Phân công vet / medical staff | `api admin assgin vet, medical staff, api admin auto assgin vet, medical staff` |
| Kết quả đua, khiếu nại, báo cáo | `feature/race-result-report-management` |
| Dự đoán & AI Prediction & chấm điểm | `implement prediction, AI prediction and scoring APIs`, `fix score rating` |
| Thông báo & gửi email | `send noti to email` |
| Homepage, Horse rating, Handicap | `add api get race, for jockey, owner, api view prediction` |

---

## 2. Kiến trúc chung

- **Mẫu phân tầng:** `Controller` (REST, phân quyền `@PreAuthorize`) → `Service` (interface) → `ServiceImpl` (business logic) → `Repository` (Spring Data JPA) → `Entity`.
- **Response chuẩn:** mọi API trả về `ApiResponse<T>` (builder), phân trang dùng `PageResponse<T>`.
- **Phân quyền theo role:** `ADMIN`, `HORSE_OWNER`, `JOCKEY`, `REFEREE`, `VETERINARIAN`, `MEDICAL_STAFF`, `SPECTATOR`.
- **Xử lý lỗi:** `AppException` + `ErrorCode` (mã lỗi nghiệp vụ).

---

## 3. Danh sách API theo Controller

### 3.1 AdminController — `/api/admin` (ROLE ADMIN)
Service liên quan: `TournamentService`, `RoundService`, `RaceService`, `PrizeStructureService`, `TournamentEligibilityService`, `TournamentRegistrationService`, `RaceEntryService`, `RefereeService`, `RaceRefereeService`, `ContractService`, `RaceInspectionStaffService`.

CRUD & vòng đời giải đấu:
- `POST /tournaments` — tạo giải
- `POST /tournaments/{id}/rounds` — tạo vòng
- `POST /rounds/{roundId}/races` — tạo race
- `POST /tournaments/{id}/prize-structures` — tạo cơ cấu giải thưởng
- `POST /tournaments/{id}/eligibility` — tạo điều kiện tham gia
- `POST /tournaments/{id}/publish` — công bố giải
- `POST /tournaments/{id}/complete-review` — hoàn tất duyệt
- `POST /tournaments/{id}/complete-matching` — hoàn tất ghép
- `POST /tournaments/{id}/publish-schedule` — công bố lịch
- `POST /tournaments/{id}/publish-results` — công bố kết quả
- `POST /tournaments/{id}/close-registration` — đóng đăng ký
- `PUT /tournaments/{id}`, `PUT /rounds/{roundId}`, `PUT /races/{raceId}`, `PUT /prize-structures/{id}`, `PUT /eligibility/{id}`
- `DELETE /tournaments/{id}`, `DELETE /rounds/{roundId}`, `DELETE /races/{raceId}`, `DELETE /prize-structures/{id}`, `DELETE /eligibility/{id}`

Bracket (tự sinh Round + Race từ maxApprovedEntries):
- `GET /tournaments/{id}/bracket-preview` — xem trước cấu trúc bracket
- `POST /tournaments/{id}/bracket-confirm` — xác nhận & tạo thật Round/Race

Đăng ký (registration):
- `GET /horse-registrations`, `GET /jockey-registrations`
- `POST /horse-registrations/{id}/approve`, `POST /horse-registrations/{id}/reject`

Race entry & trọng tài:
- `POST /races/{raceId}/entries`, `DELETE /race-entries/{entryId}`
- `GET /referees`
- `POST /races/{raceId}/referees`, `DELETE /races/{raceId}/referees/{refereeId}`
- `POST /races/{raceId}/publish-schedule`
- `POST /races/{raceId}/postpone`, `POST /races/{raceId}/cancel`, `GET /races/{raceId}/reschedule-proposals`

Phân công vet/medical:
- `POST /races/{raceId}/inspection-staff/assign` — gán thủ công
- `POST /races/{raceId}/inspection-staff/auto-assign` — tự động gán

Hợp đồng (Contract):
- `GET /contracts/pending`, `GET /contracts?status=`, `GET /contracts/approved/tournaments/{tournamentId}`
- `POST /contracts/{id}/approve`, `POST /contracts/{id}/reject`
- `POST /contracts/{id}/release-final-payout` — giải ngân tiền thưởng chung kết

### 3.2 OwnerController — `/api/owners` (ROLE HORSE_OWNER)
Service: `OwnerService`, `TournamentRegistrationService`.
- `POST /tournaments/{id}/register-horse` — đăng ký ngựa vào giải
- `GET /tournaments/{id}/accepted-jockeys` — nài đã duyệt
- `GET /my-registrations` — đăng ký ngựa của tôi
- `POST /profile`, `PUT /profile`, `GET /me` — hồ sơ chủ ngựa
- `POST /registrations/{id}/withdraw` — rút đăng ký

### 3.3 OwnerContractController — `/api/owner/contracts` (ROLE HORSE_OWNER)
Service: `ContractService`.
- `GET /` — hợp đồng của tôi
- `GET /{id}` — chi tiết hợp đồng
- `POST /invite` — mời nài
- `POST /{id}/cancel` — hủy hợp đồng

### 3.4 JockeyContractController — `/api/jockey/contracts` (ROLE JOCKEY)
Service: `ContractService`.
- `GET /` — hợp đồng của tôi
- `GET /{id}` — chi tiết
- `GET /invitations` — lời mời nhận được
- `POST /{id}/accept` — chấp nhận
- `POST /{id}/reject` — từ chối

### 3.5 JockeyTournamentController — `/api/jockey` (ROLE JOCKEY)
Service: `TournamentRegistrationService`.
- `POST /tournaments/{id}/register` — nài đăng ký giải
- `GET /tournaments/{id}/accepted-horses` — ngựa đã duyệt
- `GET /my-registrations` — đăng ký của tôi

### 3.6 PaymentController — `/api/contracts` (ROLE HORSE_OWNER)
Service: `ContractService`.
- `POST /{id}/pay-hiring-fee` — trả phí thuê nài
- `POST /{id}/pay-contract-fee` — trả phí tạo hợp đồng

### 3.7 InvoiceController — `/api/invoices`
Service: `InvoiceService`, `PaymentService`.
- `GET /my-invoices` — hóa đơn của tôi
- `POST /{id}/pay` — thanh toán hóa đơn
- `POST /{id}/refund` (ROLE ADMIN) — hoàn tiền

### 3.8 RefereeRaceController — `/api/referee/races` (ROLE REFEREE)
Service: `RaceService`.
- `POST /{raceId}/start` — bắt đầu đua

### 3.9 RaceResultController (ROLE REFEREE + xem chung)
Service: `RaceResultService`.
- `POST /api/referee/races/{raceId}/results` — nhập kết quả
- `PUT /api/referee/races/{raceId}/results` — cập nhật kết quả
- `GET /api/referee/races/{raceId}/results` — trọng tài xem kết quả
- `GET /api/races/{raceId}/results` — xem kết quả công khai

### 3.10 RefereeViolationController (ROLE REFEREE + xem chung)
Service: `ViolationService`.
- `POST /api/referee/race-entries/{entryId}/violations` — ghi vi phạm
- `GET /api/races/{raceId}/violations` — danh sách vi phạm

### 3.11 VetInspectionController — `/api/vet/race-entries` (ROLE VETERINARIAN)
Service: `HorseInspectionService`.
- `POST /{entryId}/horse-inspection` — nộp kết quả khám ngựa

### 3.12 MedicalInspectionController — `/api/medical/race-entries` (ROLE MEDICAL_STAFF)
Service: `JockeyInspectionService`.
- `POST /{entryId}/jockey-inspection` — nộp kết quả khám nài

### 3.13 AssignedRaceController (race được phân công)
Service: `RacePortalService`.
- `GET /api/referee/races/assigned` (REFEREE)
- `GET /api/vet/races/assigned` (VETERINARIAN)
- `GET /api/medical/races/assigned` (MEDICAL_STAFF)

### 3.14 ParticipantRaceController (lịch & kết quả cho người tham gia)
Service: `RacePortalService`.
- `GET /api/owner/race-schedule`, `GET /api/owner/race-results` (HORSE_OWNER)
- `GET /api/jockey/race-schedule`, `GET /api/jockey/race-results` (JOCKEY)

### 3.15 SpectatorRaceController (ROLE SPECTATOR)
Service: `RacePortalService`.
- `GET /api/spectator/races/upcoming` — race sắp diễn ra (lọc thời gian/giải)
- `GET /api/spectator/races/{raceId}` — chi tiết race

### 3.16 SpectatorPredictionController (ROLE SPECTATOR)
Service: `PredictionService`.
- `POST /api/spectator/races/{raceId}/predictions` — tạo dự đoán
- `PUT /api/spectator/predictions/{predictionId}` — sửa
- `DELETE /api/spectator/predictions/{predictionId}` — hủy
- `GET /api/spectator/predictions` — danh sách dự đoán
- `GET /api/spectator/predictions/{predictionId}` — chi tiết
- `GET /api/races/{raceId}/predictions/me` — dự đoán của tôi cho race
- `GET /api/spectator/races/{raceId}/predictions/me/result` — kết quả dự đoán

### 3.17 AIPredictionController
Service: `AIPredictionService`.
- `POST /api/admin/races/{raceId}/ai-predictions` (ADMIN) — sinh dự đoán AI
- `GET /api/admin/races/{raceId}/ai-predictions` (ADMIN)
- `GET /api/spectator/races/{raceId}/ai-predictions` (SPECTATOR)

### 3.18 HorseRatingController
Service: `HorseRatingService`.
- `GET /api/admin/races/{raceId}/rating-preview` (ADMIN)
- `GET /api/admin/races/{raceId}/rating-changes` (ADMIN)
- `GET /api/horses/{horseId}/rating-history` (HORSE_OWNER/ADMIN)
- `GET /api/admin/rounds/{roundId}/rating-summary` (ADMIN)

### 3.19 NotificationController — `/api/notifications`
Service: `NotificationService`.
- `GET /` — danh sách thông báo (lọc isRead/type + phân trang)
- `GET /unread-count` — số chưa đọc
- `PATCH /{notificationId}/read`, `PATCH /read-all` — đánh dấu đã đọc
- `DELETE /{notificationId}` — lưu trữ
- `GET /preferences`, `PUT /preferences/{eventType}` — cấu hình nhận thông báo

---

## 4. Các luồng nghiệp vụ chính (qua class)

### 4.1 Luồng tạo giải & sinh bracket tự động
```
AdminController.createTournament
  → TournamentServiceImpl.createTournament → TournamentRepository
AdminController.getBracketPreview / confirmBracket
  → TournamentServiceImpl.getBracketPreview()   (tính Round + số Race từ maxApprovedEntries)
  → TournamentServiceImpl.confirmBracket()       (lưu Round + Race vào DB)
     ├─ calculateFirstRoundRaceCount(maxEntries) → maxEntries / MAX_ENTRIES_PER_RACE(16)
     ├─ RoundRepository.save(Round)
     └─ RaceRepository.save(Race) cho từng vòng
```
Ví dụ maxEntries = 64 → Vòng 1: 4 race, Vòng 2: 2 race, Vòng 3 (Chung Kết): 1 race → tổng 7 race.

### 4.2 Luồng đăng ký ngựa/nài
```
OwnerController.registerHorse → TournamentRegistrationServiceImpl.registerHorse → repository
JockeyTournamentController.register → TournamentRegistrationServiceImpl.registerJockey
AdminController.approveHorseRegistration / rejectHorseRegistration → TournamentRegistrationServiceImpl
```

### 4.3 Luồng hợp đồng nài–chủ ngựa + tiền
```
OwnerContractController.inviteJockey → ContractServiceImpl.inviteJockey (tạo hợp đồng PENDING)
JockeyContractController.acceptContract/rejectContract → ContractServiceImpl (đổi trạng thái)
PaymentController.payHiringFee / payContractFee → ContractServiceImpl → InvoiceServiceImpl / PaymentServiceImpl
AdminController.approveContract → ContractServiceImpl.approveContract
AdminController.releaseFinalPayout → ContractServiceImpl.releaseFinalPayout (giải ngân escrow khi thắng chung kết)
```

### 4.4 Luồng ngày đua: phân công → khám → bắt đầu → kết quả → chấm điểm
```
AdminController.assignReferee / assign(inspection-staff) → RaceRefereeService / RaceInspectionStaffServiceImpl
VetInspectionController.createHorseInspection → HorseInspectionServiceImpl
MedicalInspectionController.createJockeyInspection → JockeyInspectionServiceImpl
RefereeRaceController.startRace → RaceServiceImpl.startRace
RaceResultController.createResults → RaceResultServiceImpl
  → ScoringServiceImpl (chấm điểm dự đoán) + HorseRatingServiceImpl (cập nhật rating)
RefereeViolationController.createViolation → ViolationServiceImpl
```

### 4.5 Luồng dự đoán & AI
```
SpectatorPredictionController → PredictionServiceImpl (CRUD dự đoán, tính kết quả)
AIPredictionController.generatePredictions → AIPredictionServiceImpl (gọi OpenAI/OpenAIConfig)
RaceResultServiceImpl (khi có kết quả) → ScoringServiceImpl.tính điểm cho prediction
```

### 4.6 Luồng thông báo & email
```
Nghiệp vụ (contract/race/...) → NotificationEventPublisherImpl.publish
  → NotificationEventProcessingServiceImpl → NotificationRecipientResolverImpl + NotificationPolicyServiceImpl
  → NotificationServiceImpl.create (lưu Notification)
  → NotificationDeliveryProcessingServiceImpl → NotificationEmailServiceImpl (gửi email)
NotificationController → NotificationServiceImpl (đọc/đánh dấu/preferences)
```

### 4.7 Luồng ví & VNPay
```
Deposit/VNPay → PaymentServiceImpl + VnpayCallbackServiceImpl (xử lý return)
InvoiceController.payInvoice → PaymentServiceImpl.payInvoice → InvoiceServiceImpl
InvoiceController.refundInvoice (ADMIN) → PaymentServiceImpl.refundInvoice
```

---

## 5. Các ServiceImpl chính đã triển khai

`TournamentServiceImpl`, `TournamentRegistrationServiceImpl`, `Round`/`RaceServiceImpl`, `RaceEntryServiceImpl`,
`ContractServiceImpl`, `ContractPaymentServiceImpl`, `InvoiceServiceImpl`, `PaymentServiceImpl`, `VnpayCallbackServiceImpl`,
`HorseInspectionServiceImpl`, `JockeyInspectionServiceImpl`, `RaceInspectionStaffServiceImpl`,
`RaceResultServiceImpl`, `RaceReportServiceImpl`, `ViolationServiceImpl`,
`PredictionServiceImpl`, `AIPredictionServiceImpl`, `ScoringServiceImpl`, `HorseRatingServiceImpl`, `HandicapServiceImpl`,
`RacePortalServiceImpl`, `HomePageServiceImpl`, `HorseServiceImpl`, `UserCurrentServiceImpl`,
và cụm Notification: `NotificationServiceImpl`, `NotificationEventPublisherImpl`, `NotificationEventProcessingServiceImpl`,
`NotificationRecipientResolverImpl`, `NotificationPolicyServiceImpl`, `NotificationTemplateServiceImpl`,
`NotificationDeliveryProcessingServiceImpl`, `NotificationEmailServiceImpl`, `BusinessNotificationEventServiceImpl`.

---

## 6. Tóm tắt

Bạn là người xây dựng phần lớn hệ thống backend HRTMS: từ xác thực, ví/thanh toán VNPay, đăng ký ngựa–nài,
quản lý giải đấu và **tự động sinh Round/Race theo bracket**, hợp đồng nài–chủ ngựa kèm dòng tiền (escrow/payout),
ngày đua (phân công trọng tài/vet/medical, khám, bắt đầu, kết quả, vi phạm), dự đoán + AI + chấm điểm,
rating ngựa, và hệ thống thông báo/email. Toàn bộ theo kiến trúc Controller → Service → Repository với `ApiResponse` chuẩn hóa.

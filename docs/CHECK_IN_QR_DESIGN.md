# Thiết kế Check-in bằng QR cho Race Entry

## 1. Trạng thái tài liệu

Chức năng này được lưu lại để triển khai ở phase cuối, sau khi hoàn tất timeline, scheduling, entry lifecycle và prediction integration.

Code hiện tại chưa có check-in hoặc QR check-in.

---

## 2. Timeline áp dụng

Ví dụ race bắt đầu lúc 15:00:

| Thời gian | Sự kiện | Offset |
|---|---|---:|
| 13:00 | Mở prediction | T-120 |
| 13:30 | Mở check-in và inspection | T-90 |
| 14:30 | Đóng check-in và inspection | T-30 |
| 14:55 | Khóa prediction | T-5 |
| 15:00 | Bắt đầu race | T-0 |

Cửa sổ check-in/inspection dài 60 phút:

```text
T-90 đến T-30 = 60 phút
```

Hai cấu hình thuộc `Tournament`:

```java
int inspectionOpenMinutesBefore = 90;
int inspectionCloseMinutesBefore = 30;
```

---

## 3. Người thực hiện check-in

Người xác nhận check-in là:

- Head referee của round; hoặc
- Referee được phân công vào đúng race.

Luồng tại địa điểm thi đấu:

```text
Owner đưa horse đến quầy
→ referee quét Horse QR
→ horse được xác nhận có mặt
→ vet được phép khám horse

Jockey đến quầy
→ referee quét Jockey QR
→ jockey được xác nhận có mặt
→ medical staff được phép khám jockey
```

Owner/jockey không tự xác nhận check-in. Vet và medical staff không chịu trách nhiệm check-in; họ chỉ kiểm tra timestamp check-in trước khi khám.

---

## 4. Check-in riêng Horse và Jockey

Một entry chứa cả horse và jockey, nhưng hai bên có thể đến khác thời điểm. Vì vậy không dùng một `checkedInAt` chung.

Thêm vào `RaceEntry`:

```java
@Column(name = "horse_checked_in_at")
LocalDateTime horseCheckedInAt;

@ManyToOne(fetch = FetchType.LAZY)
@JoinColumn(name = "horse_checked_in_by")
User horseCheckedInBy;

@Column(name = "jockey_checked_in_at")
LocalDateTime jockeyCheckedInAt;

@ManyToOne(fetch = FetchType.LAZY)
@JoinColumn(name = "jockey_checked_in_by")
User jockeyCheckedInBy;

@Builder.Default
@Column(name = "check_in_qr_version", nullable = false)
Integer checkInQrVersion = 1;
```

Entry hoàn tất check-in khi:

```java
public boolean isFullyCheckedIn() {
    return horseCheckedInAt != null
            && jockeyCheckedInAt != null;
}
```

Không cần thêm `CHECKED_IN` vào `RaceEntryStatus`.

---

## 5. Thời điểm QR trở nên khả dụng

QR chỉ khả dụng sau khi:

```text
Admin hoàn tất RaceEntry
→ xác định race, horse, jockey và lane
→ publish schedule của race
→ race.status = SCHEDULED
→ owner/jockey có thể lấy QR
```

Không sinh QR khi mới đăng ký tournament hoặc khi mới ký contract vì lúc đó chưa biết entry thuộc race nào.

QR có thể hiển thị ngay sau publish schedule, nhưng scan check-in chỉ hợp lệ trong khoảng T-90 đến T-30.

---

## 6. Hai QR riêng

### Horse QR

- Người được xem: owner sở hữu horse của entry.
- Target trong token: `HORSE`.

### Jockey QR

- Người được xem: jockey của entry.
- Target trong token: `JOCKEY`.

Enum:

```java
public enum CheckInTarget {
    HORSE,
    JOCKEY
}
```

Không cần `BOTH` trong QR vì hai người có thể đến riêng và QR thuộc hai tài khoản khác nhau.

---

## 7. Nội dung QR

Không đặt JSON hoặc UUID thuần trong QR vì payload có thể bị sửa.

QR chứa signed token với các claim:

```json
{
  "entryId": "uuid",
  "raceId": "uuid",
  "target": "HORSE",
  "purpose": "RACE_CHECK_IN",
  "version": 1,
  "issuedAt": "...",
  "expiresAt": "..."
}
```

Nên ký token bằng secret riêng:

```properties
app.check-in-qr.secret=${CHECK_IN_QR_SECRET}
```

Không dùng chung secret với access JWT.

QR có thể chứa chuỗi:

```text
HRTMS_CHECKIN:<signed-token>
```

QR chỉ dùng để tìm và xác minh entry. Người scan vẫn phải có access token referee hợp lệ.

---

## 8. API lấy QR

Horse owner:

```http
GET /api/owner/race-entries/{entryId}/horse-check-in-qr
```

Jockey:

```http
GET /api/jockey/race-entries/{entryId}/jockey-check-in-qr
```

Response:

```json
{
  "entryId": "uuid",
  "raceId": "uuid",
  "target": "HORSE",
  "qrToken": "signed-token",
  "qrContent": "HRTMS_CHECKIN:signed-token"
}
```

Backend chỉ trả token/content. Frontend render thành QR, không cần backend lưu PNG.

Validation khi lấy QR:

- Race tồn tại và đã publish schedule.
- Race đang `SCHEDULED`.
- Entry còn `CONFIRMED`.
- Người gọi đúng owner hoặc jockey tương ứng.
- Entry chưa check-in target đó.

---

## 9. API scan QR

```http
POST /api/referee/check-ins/scan
Authorization: Bearer <referee-access-token>
Content-Type: application/json
```

Body:

```json
{
  "qrToken": "signed-token"
}
```

Backend thực hiện theo thứ tự:

```text
1. Verify chữ ký và hạn token.
2. Verify purpose = RACE_CHECK_IN.
3. Tìm entry theo entryId.
4. Kiểm tra raceId trong token khớp entry.
5. Kiểm tra QR version.
6. Kiểm tra race SCHEDULED và chưa start.
7. Kiểm tra thời gian T-90 đến T-30.
8. Kiểm tra entry còn CONFIRMED.
9. Kiểm tra referee là head referee hoặc assigned referee.
10. Ghi timestamp và checkedInBy theo target.
```

---

## 10. Idempotency

Nếu cùng QR bị scan hai lần do mạng chậm, API không nên tạo lỗi nghiệp vụ nghiêm trọng.

Nếu target đã check-in, trả trạng thái hiện tại:

```json
{
  "entryId": "uuid",
  "target": "HORSE",
  "alreadyCheckedIn": true,
  "checkedInAt": "2026-07-11T13:35:00",
  "checkedInBy": "Referee A"
}
```

---

## 11. Ràng buộc với Inspection

Trong horse inspection:

```java
if (raceEntry.getHorseCheckedInAt() == null) {
    throw new AppException(ErrorCode.HORSE_NOT_CHECKED_IN);
}
```

Trong jockey inspection:

```java
if (raceEntry.getJockeyCheckedInAt() == null) {
    throw new AppException(ErrorCode.JOCKEY_NOT_CHECKED_IN);
}
```

Không cần chờ cả entry check-in hoàn tất. Horse check-in xong thì vet khám ngay; jockey check-in xong thì medical staff khám ngay.

---

## 12. Xử lý tại deadline T-30

| Trường hợp | Kết quả |
|---|---|
| Horse chưa check-in | Entry `SCRATCHED`, reason `HORSE_NOT_PRESENT` |
| Jockey chưa check-in | Entry `SCRATCHED`, reason `JOCKEY_NOT_PRESENT` |
| Đã check-in nhưng thiếu inspection | Entry `SCRATCHED`, reason `INSPECTION_NOT_COMPLETED` |
| Horse inspection fail | Entry `SCRATCHED`, reason `HORSE_INSPECTION_FAILED` |
| Jockey inspection fail | Entry `SCRATCHED`, reason `JOCKEY_INSPECTION_FAILED` |

Vì entry là cặp horse–jockey, chỉ cần một bên không đủ điều kiện thì toàn bộ entry bị scratch.

Prediction chứa entry bị scratch được cho sửa đến T-5; nếu chưa sửa thì chuyển `VOIDED` theo đặc tả nghiệp vụ chính.

---

## 13. Thu hồi và tạo lại QR

Token chứa `checkInQrVersion`.

Khi cần thu hồi QR:

```java
entry.setCheckInQrVersion(entry.getCheckInQrVersion() + 1);
```

QR cũ không còn hợp lệ.

Tăng version khi:

- Entry chuyển sang race khác.
- Thay horse hoặc jockey.
- QR bị lộ.
- Admin yêu cầu phát hành lại.

Nếu chỉ reschedule giờ race mà không thay thành phần entry, có thể giữ QR cũ. Backend kiểm tra cửa sổ theo `race.startTime` mới trong database.

---

## 14. Màn hình Referee

API danh sách:

```http
GET /api/referee/races/{raceId}/check-in-list
```

Response mỗi entry nên có:

```json
{
  "entryId": "uuid",
  "laneNumber": 1,
  "horseName": "Thunderbolt",
  "jockeyName": "Nguyen Van A",
  "horseCheckedIn": true,
  "horseCheckedInAt": "2026-07-11T13:35:00",
  "jockeyCheckedIn": false,
  "jockeyCheckedInAt": null,
  "horseInspectionResult": "PASS",
  "jockeyInspectionResult": null,
  "fullyCheckedIn": false
}
```

Referee có thể quét QR hoặc tìm thủ công theo entry ID, lane, horse name và jockey name.

---

## 15. Demo bằng hai Ngrok tunnel

Vì FE và BE chạy cùng laptop nhưng VNPay cần public BE, cấu hình demo dùng hai tunnel:

```text
FE tunnel → localhost:5173
BE tunnel → localhost:8080
```

Ví dụ:

```text
https://fe-demo.ngrok-free.app
https://be-demo.ngrok-free.app
```

Frontend:

```properties
VITE_API_BASE_URL=https://be-demo.ngrok-free.app
```

Backend VNPay:

```properties
vnpay.return-url=https://be-demo.ngrok-free.app/api/payments/vnpay/return
vnpay.ipn-url=https://be-demo.ngrok-free.app/api/payments/vnpay/ipn
vnpay.frontend-return-url=https://fe-demo.ngrok-free.app/vnpay/return
```

CORS backend phải cho phép chính xác FE tunnel origin.

Luồng demo:

```text
Laptop hoặc điện thoại 1 hiển thị Horse/Jockey QR qua FE tunnel
→ điện thoại referee đăng nhập FE tunnel
→ camera quét QR
→ FE gọi BE tunnel
→ BE xác thực và ghi check-in
```

Không ghi domain ngrok cố định vào QR; QR chỉ chứa signed token. Vì vậy đổi tunnel không cần phát hành lại QR.

---

## 16. Bảo mật khi demo

- Chỉ dùng database test.
- Tắt hoặc bảo vệ endpoint seed công khai.
- Không commit QR secret, JWT secret hoặc VNPay hash secret.
- Cấu hình secret bằng environment variable.
- Dừng tunnel sau khi demo.
- QR không thay thế JWT và authorization của referee.
- Backend luôn kiểm tra assignment, thời gian và trạng thái entry.

---

## 17. Thứ tự triển khai phase Check-in/QR

1. Thêm bốn field check-in và `checkInQrVersion` vào `RaceEntry`.
2. Thêm DTO/response check-in.
3. Thêm service check-in thủ công cho referee.
4. Ràng buộc inspection với timestamp check-in.
5. Thêm API danh sách check-in theo race.
6. Thêm signed QR token service.
7. Thêm API owner/jockey lấy QR.
8. Thêm API referee scan QR.
9. Thêm idempotency và QR version.
10. Xử lý deadline T-30.
11. Tích hợp notification và prediction void.
12. Viết test authorization, window, duplicate scan và token giả mạo.

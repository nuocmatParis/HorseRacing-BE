# API tích hợp FE - Horse Rating thủ công

## 1. Mục đích

Tài liệu này mô tả các API FE cần dùng cho luồng:

```text
Race Referee nhập kết quả và Rating
→ gửi Race Report
→ Head Referee xem và có thể điều chỉnh
→ Head Referee ký
→ Admin xem Rating Preview
→ Admin publish Race Report
→ BE mới cập nhật Rating của Horse
```

Rating không được tự động tính từ đối thủ, khoảng cách về đích, field size hoặc hiệu suất. `ratingChange` là điểm thủ công do Race Referee nhập và Head Referee xác nhận.

---

## 2. Cấu trúc response chung

### Thành công

```json
{
  "code": 200,
  "message": "Success",
  "result": {}
}
```

### Lỗi nghiệp vụ

```json
{
  "code": 1819,
  "message": "Horse rating change is outside the allowed range for this result"
}
```

### Lỗi validation request

```json
{
  "code": 1010,
  "message": "Validation failed",
  "result": [
    {
      "field": "[0].ratingChange",
      "message": "Horse rating change is required"
    }
  ]
}
```

Tên `field` có thể chứa thêm tên method hoặc index phần tử tùy request. FE nên đọc toàn bộ `result[]`, không kiểm tra bằng một chuỗi field cố định.

---

## 3. Quy tắc Rating

| Kết quả | Khoảng `ratingChange` |
|---|---:|
| Hạng 1 | +6 đến +12 |
| Hạng 2 | +2 đến +5 |
| Hạng 3 | +1 đến +4 |
| Hạng 4-5 | 0 đến +2 |
| Hạng 6 trở xuống | -8 đến 0 |
| `DID_NOT_FINISH` | -8 đến 0 |
| `DISQUALIFIED` | -8 đến 0 |

Quy tắc dữ liệu kết quả:

- `FINISHED`: bắt buộc có `finishTime` và `rank` hợp lệ.
- `DID_NOT_FINISH`: `finishTime = null`, `rank = null`.
- `DISQUALIFIED`: `finishTime` và `rank` có thể là `null`.
- Mọi kết quả đều phải có `ratingChange` trước khi report được gửi hoặc ký.
- `ratingChange` phải là số nguyên.
- Rank của các entry `FINISHED` không được trùng nhau.

---

## 4. Tổng hợp API theo role

### Race Referee

| Method | Endpoint | Chức năng |
|---|---|---|
| `GET` | `/api/referee/races/{raceId}/results` | Lấy kết quả và Rating để chỉnh sửa |
| `POST` | `/api/referee/races/{raceId}/results` | Tạo kết quả và Rating lần đầu |
| `PUT` | `/api/referee/races/{raceId}/results` | Cập nhật kết quả và Rating khi report còn Draft |
| `GET` | `/api/referee/races/{raceId}/report` | Lấy Race Report |
| `PUT` | `/api/referee/races/{raceId}/report` | Lưu nội dung Race Report |
| `POST` | `/api/referee/races/{raceId}/report/submit` | Gửi report cho Head Referee |

### Head Referee

| Method | Endpoint | Chức năng |
|---|---|---|
| `GET` | `/api/head-referee/rounds/{roundId}/reports?status=SUBMITTED_TO_HEAD` | Lấy report đang chờ duyệt trong Round phụ trách |
| `GET` | `/api/head-referee/races/{raceId}/report` | Lấy chi tiết report |
| `GET` | `/api/head-referee/races/{raceId}/results` | Lấy kết quả và Rating đã được Race Referee gửi |
| `PUT` | `/api/head-referee/races/{raceId}/results` | Điều chỉnh kết quả hoặc Rating |
| `PUT` | `/api/head-referee/races/{raceId}/report` | Cập nhật nội dung report |
| `POST` | `/api/head-referee/races/{raceId}/report/return` | Trả report về Race Referee |
| `POST` | `/api/head-referee/races/{raceId}/report/sign` | Ký report |

### Admin

| Method | Endpoint | Chức năng |
|---|---|---|
| `GET` | `/api/admin/races/{raceId}/report` | Lấy report đã ký để chuẩn bị công bố |
| `GET` | `/api/admin/races/{raceId}/rating-preview` | Xem Rating sẽ được áp dụng |
| `POST` | `/api/admin/races/{raceId}/report/publish` | Công bố report và áp dụng Rating |
| `GET` | `/api/admin/races/{raceId}/rating-changes` | Xem Rating thực tế sau publish |
| `GET` | `/api/admin/rounds/{roundId}/rating-summary` | Xem tổng hợp Rating của Round |

### Owner và Admin

| Method | Endpoint | Chức năng |
|---|---|---|
| `GET` | `/api/horses/{horseId}/rating-history` | Xem lịch sử Rating của Horse |

---

## 5. Race Referee - lấy kết quả hiện tại

### Request

```http
GET /api/referee/races/90000000-0000-0000-0000-000000000001/results
Authorization: Bearer <access-token>
```

### Response mẫu

```json
{
  "code": 200,
  "message": "Success",
  "result": [
    {
      "resultId": "c0000000-0000-0000-0000-000000000201",
      "raceId": "90000000-0000-0000-0000-000000000001",
      "entryId": "a0000000-0000-0000-0000-000000000201",
      "laneNumber": 1,
      "horseId": "40000000-0000-0000-0000-000000000201",
      "horseName": "Horse 01",
      "jockeyId": "21000000-0000-0000-0000-000000000201",
      "jockeyName": "Jockey 01",
      "finishTime": 95.21,
      "rank": 1,
      "status": "FINISHED",
      "ratingChange": 8,
      "ratingAdjustmentReason": null,
      "recordedById": "10000000-0000-0000-0000-000000000005",
      "recordedAt": "2026-07-23T10:00:00",
      "updatedAt": "2026-07-23T10:00:00"
    }
  ]
}
```

### FE cần làm

- Map `ratingChange` vào input Rating của từng entry.
- Race Referee không được nhập `ratingAdjustmentReason`.
- Nếu chưa có kết quả, có thể load danh sách participant bằng API hiện có rồi khởi tạo `ratingChange = ""` ở state FE.
- Không fallback sang kết quả public nếu API Referee trả lỗi quyền hoặc lỗi trạng thái.

---

## 6. Race Referee - tạo kết quả và Rating

### Request

```http
POST /api/referee/races/90000000-0000-0000-0000-000000000001/results
Authorization: Bearer <access-token>
Content-Type: application/json
```

```json
[
  {
    "raceId": "90000000-0000-0000-0000-000000000001",
    "entryId": "a0000000-0000-0000-0000-000000000201",
    "finishTime": 95.21,
    "rank": 1,
    "status": "FINISHED",
    "ratingChange": 8
  },
  {
    "raceId": "90000000-0000-0000-0000-000000000001",
    "entryId": "a0000000-0000-0000-0000-000000000202",
    "finishTime": null,
    "rank": null,
    "status": "DID_NOT_FINISH",
    "ratingChange": -4
  }
]
```

### Response

Response là danh sách `RaceResultResponse`, cùng cấu trúc với mục 5.

### Validation FE

- `ratingChange` không được rỗng.
- Phải là số nguyên.
- Phải thuộc khoảng theo status/rank.
- `FINISHED` phải có thời gian không âm và rank từ 1 trở lên.
- Không được có hai entry `FINISHED` cùng rank.
- Với DNF/DQ, gửi `finishTime = null` và `rank = null`.

---

## 7. Race Referee - cập nhật kết quả và Rating

### Request

```http
PUT /api/referee/races/90000000-0000-0000-0000-000000000001/results
Authorization: Bearer <access-token>
Content-Type: application/json
```

```json
[
  {
    "entryId": "a0000000-0000-0000-0000-000000000201",
    "finishTime": 95.21,
    "rank": 1,
    "status": "FINISHED",
    "ratingChange": 9
  }
]
```

FE nên gửi toàn bộ các dòng đang hiển thị để đồng bộ kết quả, rank và Rating trong một lần lưu.

Khi Race Referee cập nhật, BE xóa `ratingAdjustmentReason` cũ vì đây lại là đề xuất của Race Referee.

Không được cập nhật khi report đã `SUBMITTED_TO_HEAD`, `SIGNED` hoặc `PUBLISHED`.

---

## 8. Race Referee - lưu và gửi Race Report

### Lưu nội dung report

```http
PUT /api/referee/races/{raceId}/report
Authorization: Bearer <access-token>
Content-Type: application/json
```

```json
{
  "summary": "Cuộc đua đã kết thúc và kết quả đã được nhập đầy đủ.",
  "appealNote": "Tất cả khiếu nại đã được xử lý."
}
```

### Gửi Head Referee

```http
POST /api/referee/races/{raceId}/report/submit
Authorization: Bearer <access-token>
```

Không có request body.

### Response report mẫu

```json
{
  "code": 200,
  "message": "Success",
  "result": {
    "reportId": "d0000000-0000-0000-0000-000000000001",
    "raceId": "90000000-0000-0000-0000-000000000001",
    "raceName": "Race 01",
    "tournamentId": "50000000-0000-0000-0000-000000000001",
    "tournamentName": "Demo Tournament",
    "roundId": "80000000-0000-0000-0000-000000000001",
    "roundName": "Vòng 1",
    "summary": "Cuộc đua đã kết thúc và kết quả đã được nhập đầy đủ.",
    "appealNote": "Tất cả khiếu nại đã được xử lý.",
    "status": "SUBMITTED_TO_HEAD",
    "submittedById": "10000000-0000-0000-0000-000000000005",
    "submittedByName": "Race Referee 01",
    "submittedAt": "2026-07-23T10:15:00",
    "signedAt": null,
    "publishedAt": null
  }
}
```

FE chỉ bật nút gửi khi:

- Có kết quả cho tất cả entry cần kết quả.
- Tất cả Rating hợp lệ.
- Summary không rỗng.
- Không còn appeal `PENDING`.
- Report đang `DRAFT`.

---

## 9. Head Referee - lấy report chờ duyệt

### Request

```http
GET /api/head-referee/rounds/80000000-0000-0000-0000-000000000001/reports?status=SUBMITTED_TO_HEAD
Authorization: Bearer <access-token>
```

Response là danh sách `RaceReportResponse`.

Chỉ Head Referee được gán vào Round mới được truy cập các report của Round đó.

---

## 10. Head Referee - lấy kết quả và Rating

### Request

```http
GET /api/head-referee/races/90000000-0000-0000-0000-000000000001/results
Authorization: Bearer <access-token>
```

Response là danh sách `RaceResultResponse` như mục 5.

### FE cần lưu state

```javascript
{
  entryId: result.entryId,
  resultStatus: result.status,
  finishPosition: result.rank ?? "",
  finishTime: result.finishTime ?? "",
  ratingChange: result.ratingChange ?? "",
  originalRatingChange: result.ratingChange ?? "",
  ratingAdjustmentReason: result.ratingAdjustmentReason || ""
}
```

`originalRatingChange` chỉ là state FE dùng để biết Head Referee có thay đổi điểm hay không; BE không có field này.

---

## 11. Head Referee - điều chỉnh kết quả và Rating

### Giữ nguyên Rating

```http
PUT /api/head-referee/races/{raceId}/results
Authorization: Bearer <access-token>
Content-Type: application/json
```

```json
[
  {
    "entryId": "a0000000-0000-0000-0000-000000000201",
    "finishTime": 95.21,
    "rank": 1,
    "status": "FINISHED",
    "ratingChange": 8,
    "ratingAdjustmentReason": null
  }
]
```

### Thay đổi Rating

```json
[
  {
    "entryId": "a0000000-0000-0000-0000-000000000201",
    "finishTime": 95.21,
    "rank": 1,
    "status": "FINISHED",
    "ratingChange": 10,
    "ratingAdjustmentReason": "Điều chỉnh sau khi xem lại camera và toàn bộ kết quả"
  }
]
```

### FE cần làm

- Hiển thị điểm Race Referee đã nhập.
- Cho Head Referee sửa điểm khi report là `SUBMITTED_TO_HEAD`.
- Nếu điểm mới khác `originalRatingChange`, bắt buộc nhập lý do.
- Nếu Head Referee đổi rank hoặc status, phải kiểm tra lại khoảng Rating.
- Disable nút ký nếu có thay đổi chưa lưu.
- Sau khi lưu thành công, cập nhật lại `originalRatingChange` từ response.
- Khi report là `SIGNED` hoặc `PUBLISHED`, toàn bộ bảng là read-only.

---

## 12. Head Referee - trả về hoặc ký report

### Trả report

```http
POST /api/head-referee/races/{raceId}/report/return
Authorization: Bearer <access-token>
Content-Type: application/json
```

```json
{
  "reason": "Race Referee cần kiểm tra lại kết quả entry ở lane 3."
}
```

Report chuyển:

```text
SUBMITTED_TO_HEAD → DRAFT
```

### Ký report

```http
POST /api/head-referee/races/{raceId}/report/sign
Authorization: Bearer <access-token>
```

Không có request body.

Report chuyển:

```text
SUBMITTED_TO_HEAD → SIGNED
```

BE kiểm tra lại toàn bộ kết quả, Rating và appeal trước khi ký.

---

## 13. Admin - Rating Preview

### Request

```http
GET /api/admin/races/90000000-0000-0000-0000-000000000001/rating-preview
Authorization: Bearer <access-token>
```

Chỉ gọi được khi report là `SIGNED`.

### Response mẫu

```json
{
  "code": 200,
  "message": "Success",
  "result": {
    "raceId": "90000000-0000-0000-0000-000000000001",
    "reportStatus": "SIGNED",
    "policyVersion": 1,
    "changes": [
      {
        "horseId": "40000000-0000-0000-0000-000000000201",
        "horseName": "Horse 01",
        "finishPosition": 1,
        "resultStatus": "FINISHED",
        "oldRating": 80,
        "minimumAllowedChange": 6,
        "maximumAllowedChange": 12,
        "finalChange": 10,
        "adjustmentReason": "Điều chỉnh sau khi xem lại camera và toàn bộ kết quả",
        "newRating": 90,
        "oldRaceClass": "CLASS_3",
        "newRaceClass": "CLASS_2"
      },
      {
        "horseId": "40000000-0000-0000-0000-000000000202",
        "horseName": "Horse 02",
        "finishPosition": null,
        "resultStatus": "DID_NOT_FINISH",
        "oldRating": 75,
        "minimumAllowedChange": -8,
        "maximumAllowedChange": 0,
        "finalChange": -4,
        "adjustmentReason": null,
        "newRating": 71,
        "oldRaceClass": "CLASS_3",
        "newRaceClass": "CLASS_3"
      }
    ]
  }
}
```

### FE cần hiển thị

| Ngựa | Hạng | Kết quả | Rating cũ | Khoảng | Thay đổi | Lý do | Rating mới | Class |
|---|---:|---|---:|---|---:|---|---:|---|
| Horse 01 | 1 | Hoàn thành | 80 | +6 đến +12 | +10 | Xem lại camera | 90 | CLASS_2 |
| Horse 02 | - | DNF | 75 | -8 đến 0 | -4 | Không điều chỉnh | 71 | CLASS_3 |

Admin không được chỉnh Rating tại màn preview.

Không sử dụng các field công thức tự động cũ như `baseChange`, `opponentStrengthBonus`, `finishPerformanceBonus`, `fieldSizeBonus` hoặc `underperformancePenalty`.

---

## 14. Admin - publish Race Report

### Request

```http
POST /api/admin/races/90000000-0000-0000-0000-000000000001/report/publish
Authorization: Bearer <access-token>
```

Không có request body.

### Response

Response là `RaceReportResponse` với:

```json
{
  "code": 200,
  "message": "Success",
  "result": {
    "raceId": "90000000-0000-0000-0000-000000000001",
    "status": "PUBLISHED",
    "signedByName": "Head Referee 01",
    "signedAt": "2026-07-23T10:30:00",
    "publishedByName": "System Admin",
    "publishedAt": "2026-07-23T10:45:00"
  }
}
```

Khi publish thành công, BE mới:

1. Chấm prediction.
2. Cập nhật `Horse.currentRating`.
3. Cập nhật `Horse.highestRating` và `RaceClass`.
4. Tạo `HorseRatingHistory`.
5. Xử lý payout nếu đây là Final.
6. Xử lý chuyển Round nếu đủ điều kiện.

FE phải khóa double-click và refetch report, Rating changes, race result và dữ liệu Horse sau khi publish.

---

## 15. Admin - Rating changes sau publish

### Request

```http
GET /api/admin/races/{raceId}/rating-changes
Authorization: Bearer <access-token>
```

Chỉ gọi được khi report là `PUBLISHED`.

### Response mẫu

```json
{
  "code": 200,
  "message": "Success",
  "result": {
    "raceId": "90000000-0000-0000-0000-000000000001",
    "reportStatus": "PUBLISHED",
    "policyVersion": 1,
    "changes": [
      {
        "ratingHistoryId": "uuid",
        "horseId": "40000000-0000-0000-0000-000000000201",
        "horseName": "Horse 01",
        "raceId": "90000000-0000-0000-0000-000000000001",
        "raceName": "Race 01",
        "roundId": "80000000-0000-0000-0000-000000000001",
        "finishPosition": 1,
        "oldRating": 80,
        "minimumAllowedChange": 6,
        "maximumAllowedChange": 12,
        "finalChange": 10,
        "adjustmentReason": "Điều chỉnh sau khi xem lại camera và toàn bộ kết quả",
        "newRating": 90,
        "oldRaceClass": "CLASS_3",
        "newRaceClass": "CLASS_2",
        "policyVersion": 1,
        "calculatedAt": "2026-07-23T10:45:00"
      }
    ]
  }
}
```

Preview là dữ liệu chưa áp dụng. Rating changes là lịch sử thực tế đã lưu sau publish. FE không được dùng lẫn hai API này.

---

## 16. Lịch sử Rating của Horse

### Request

```http
GET /api/horses/{horseId}/rating-history
Authorization: Bearer <access-token>
```

Role được phép:

- `HORSE_OWNER`
- `ADMIN`

Response là danh sách `HorseRatingHistoryResponse`, cùng cấu trúc từng phần tử trong `rating-changes.changes`.

---

## 17. Xử lý lỗi trên FE

### Hàm lấy lỗi đề xuất

```javascript
export function getBackendError(error) {
  const data = error?.response?.data;

  if (!data) {
    return {
      code: null,
      message: "Không thể kết nối đến máy chủ.",
      fieldErrors: [],
    };
  }

  const fieldErrors = Array.isArray(data.result)
    ? data.result.map((item) => ({
        field: item.field,
        message: item.message,
      }))
    : [];

  return {
    code: data.code,
    message: data.message,
    fieldErrors,
  };
}
```

FE không hiển thị JSON thô và không chỉ dựa vào HTTP status. Phải ưu tiên `response.data.code`.

### Mapping lỗi Rating

| Code | Ý nghĩa | Thông báo tiếng Việt đề xuất |
|---:|---|---|
| `1814` | Thiếu Rating | Mỗi kết quả cuộc đua phải có điểm Rating do trọng tài nhập. |
| `1816` | Rating đã được áp dụng | Điểm Rating của kết quả này đã được áp dụng trước đó. |
| `1819` | Rating ngoài khoảng | Điểm Rating nằm ngoài khoảng cho phép của thứ hạng hoặc trạng thái kết quả. |
| `1821` | Thiếu lý do điều chỉnh | Phải nhập lý do khi Head Referee điều chỉnh điểm Rating. |

### Mapping lỗi kết quả

| Code | Ý nghĩa | Thông báo tiếng Việt đề xuất |
|---:|---|---|
| `2601` | Không tìm thấy kết quả | Không tìm thấy kết quả cuộc đua. |
| `2602` | Kết quả đã tồn tại | Entry này đã có kết quả. Hãy sử dụng thao tác cập nhật. |
| `2604` | Status/rank/time không phù hợp | Trạng thái kết quả, thứ hạng hoặc thời gian không hợp lệ. |
| `2605` | Trùng rank | Hai ngựa hoàn thành không được có cùng thứ hạng. |
| `2606` | Finish time âm | Thời gian hoàn thành phải lớn hơn hoặc bằng 0. |
| `2607` | Rank nhỏ hơn 1 | Thứ hạng phải bắt đầu từ 1. |

### Mapping lỗi report

| Code | Ý nghĩa | Thông báo tiếng Việt đề xuất |
|---:|---|---|
| `2612` | Report đã ký | Race Report đã được ký và không thể chỉnh sửa. |
| `2613` | Report đã publish | Race Report đã được công bố. |
| `2614` | Chưa ký | Head Referee phải ký Race Report trước khi Admin công bố. |
| `2617` | Chưa gửi Head Referee | Race Report chưa được gửi cho Head Referee. |
| `2618` | Đã gửi Head Referee | Race Report đã được gửi và đang chờ Head Referee xử lý. |
| `2623` | Còn appeal pending | Phải xử lý toàn bộ khiếu nại trước khi ký hoặc công bố Race Report. |

### Mapping lỗi quyền và request

| Code | Ý nghĩa | Xử lý FE |
|---:|---|---|
| `1004` | Không có quyền | Hiển thị thông báo không có quyền, không tự chuyển sang dữ liệu public. |
| `1005` | Chưa đăng nhập/token hết hạn | Xử lý refresh token hoặc chuyển về đăng nhập. |
| `1010` | Validation failed | Hiển thị các lỗi trong `result[]` cạnh field tương ứng và popup tóm tắt. |
| `1508` | Không tìm thấy Race | Hiển thị trạng thái không tìm thấy cuộc đua. |
| `1713` | Không phải Race Referee được phân công | Không cho phép nhập hoặc sửa kết quả của Race này. |

---

## 18. Utility validation FE đề xuất

```javascript
export function getHorseRatingRange(status, rank) {
  if (status === "DID_NOT_FINISH") {
    return { minimum: -8, maximum: 0 };
  }

  if (status === "DISQUALIFIED") {
    return { minimum: -8, maximum: 0 };
  }

  const numericRank = Number(rank);

  if (numericRank === 1) return { minimum: 6, maximum: 12 };
  if (numericRank === 2) return { minimum: 2, maximum: 5 };
  if (numericRank === 3) return { minimum: 1, maximum: 4 };
  if (numericRank === 4 || numericRank === 5) {
    return { minimum: 0, maximum: 2 };
  }

  if (numericRank >= 6) return { minimum: -8, maximum: 0 };

  return null;
}

export function validateHorseRating(status, rank, value) {
  if (value === "" || value === null || value === undefined) {
    return "Vui lòng nhập điểm Rating.";
  }

  const ratingChange = Number(value);
  if (!Number.isInteger(ratingChange)) {
    return "Điểm Rating phải là số nguyên.";
  }

  const range = getHorseRatingRange(status, rank);
  if (!range) {
    return "Vui lòng nhập trạng thái và thứ hạng hợp lệ trước.";
  }

  if (ratingChange < range.minimum || ratingChange > range.maximum) {
    return `Điểm Rating phải nằm trong khoảng ${range.minimum} đến ${range.maximum}.`;
  }

  return null;
}
```

BE luôn là nguồn validation cuối cùng. Nếu cấu hình BE thay đổi, utility FE cũng phải được cập nhật vì hiện chưa có API public trả Rating policy trước khi nhập kết quả.

---

## 19. Checklist hoàn thành FE

### Race Referee

- [ ] Hiển thị input Rating cho từng kết quả.
- [ ] Hiển thị khoảng điểm theo status/rank.
- [ ] Gửi `ratingChange` trong POST/PUT results.
- [ ] Không cho gửi report nếu Rating thiếu hoặc sai.
- [ ] Khóa kết quả khi report không còn Draft.

### Head Referee

- [ ] Dùng API `/api/head-referee/races/{raceId}/results`.
- [ ] Hiển thị điểm Race Referee đã nhập.
- [ ] Cho điều chỉnh trong khoảng quy định.
- [ ] Bắt buộc lý do khi thay đổi điểm.
- [ ] Không cho ký khi thay đổi chưa lưu hoặc còn appeal pending.
- [ ] Khóa bảng sau khi ký.

### Admin

- [ ] Preview chỉ dùng field thủ công mới.
- [ ] Bỏ các cột công thức Rating tự động cũ.
- [ ] Không cho Admin chỉnh điểm.
- [ ] Publish có modal xác nhận tác động.
- [ ] Sau publish gọi `rating-changes` để hiển thị lịch sử đã áp dụng.

### Xử lý lỗi

- [ ] Map lỗi theo `response.data.code`.
- [ ] Hiển thị lỗi `1010.result[]` cạnh field phù hợp.
- [ ] Popup lỗi bằng tiếng Việt.
- [ ] Không hiển thị raw JSON hoặc message tiếng Anh trực tiếp cho người dùng.

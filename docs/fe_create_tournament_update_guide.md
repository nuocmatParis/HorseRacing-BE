# Hướng dẫn cập nhật FE — Trang Tạo Giải Đấu

> **Mục đích**: Đồng bộ `CreateTournamentPage.jsx` và `tournamentService.js` với các thay đổi BE đã triển khai (xóa 5 field cũ, thêm 2 field mới, sửa logic timeline).
>
> **Nguyên tắc**: Giữ nguyên 100% template/bố cục wizard 4 bước, chỉ thay đổi nội dung field bên trong.

---

## Tổng quan thay đổi

| Hạng mục | Xóa | Thêm mới |
|:---|:---|:---|
| **Phase config** | `PRE_RACE_BUFFER` | — |
| **Điều kiện (Bước 2)** | — | `trackCondition` |
| **Thông tin giải (Bước 0)** | — | `maxEntriesPerRace` |
| **Vận hành (Bước 3)** | `startEarlyToleranceMinutes`, `applyBreakTime`, `breakStartTime`, `breakEndTime` | `predictionOpenMinutesBefore`, `predictionCloseMinutesBefore` |

---

## 1. File `CreateTournamentPage.jsx`

### 1.1. Thêm constant `TRACK_CONDITIONS` (cạnh `DISTANCES`)

Thêm ngay sau mảng `DISTANCES` (khoảng dòng 88):

```js
/** Options mặt đường đua — hiển thị bước Điều kiện. */
const TRACK_CONDITIONS = [
  ["TURF", "Cỏ tự nhiên (Turf)"],
  ["DIRT", "Đất (Dirt)"],
  ["SYNTHETIC", "Nhân tạo (Synthetic)"],
  ["SAND", "Cát (Sand)"],
];
```

### 1.2. Xóa `PRE_RACE_BUFFER` khỏi `PHASE_KEYS`

**Trước** (dòng 94–100):
```js
const PHASE_KEYS = [
  ["REGISTRATION", "Đăng ký"],
  ["REVIEW", "Duyệt hồ sơ"],
  ["JOCKEY_MATCHING", "Ghép kỵ sĩ"],
  ["SCHEDULING", "Xếp lịch"],
  ["PRE_RACE_BUFFER", "Buffer trước đua"],  // ← XÓA DÒNG NÀY
];
```

**Sau**:
```js
const PHASE_KEYS = [
  ["REGISTRATION", "Đăng ký"],
  ["REVIEW", "Duyệt hồ sơ"],
  ["JOCKEY_MATCHING", "Ghép kỵ sĩ"],
  ["SCHEDULING", "Xếp lịch"],
];
```

### 1.3. Xóa `PRE_RACE_BUFFER` khỏi `EMPTY_PHASE_CONFIGS`

**Trước** (dòng 103–109):
```js
const EMPTY_PHASE_CONFIGS = {
  REGISTRATION: "",
  REVIEW: "",
  JOCKEY_MATCHING: "",
  SCHEDULING: "",
  PRE_RACE_BUFFER: "",  // ← XÓA DÒNG NÀY
};
```

**Sau**:
```js
const EMPTY_PHASE_CONFIGS = {
  REGISTRATION: "",
  REVIEW: "",
  JOCKEY_MATCHING: "",
  SCHEDULING: "",
};
```

### 1.4. Sửa hàm `buildTimelineFromPhases` — bỏ `PRE_RACE_BUFFER`

**Trước** (dòng 167–185):
```js
const buildTimelineFromPhases = (startDate, phaseConfigs) => {
  if (!hasCompletePhaseConfigs(phaseConfigs)) return { ...EMPTY_TIMELINE };
  const open = registrationOpenFromStartDate(startDate);
  const close = addDaysToDateTimeLocal(open, phaseConfigs.REGISTRATION);
  const review = addDaysToDateTimeLocal(close, phaseConfigs.REVIEW);
  const matching = addDaysToDateTimeLocal(review, phaseConfigs.JOCKEY_MATCHING);
  const scheduling = addDaysToDateTimeLocal(matching, phaseConfigs.SCHEDULING);
  const competitionHint = addDaysToDateTimeLocal(scheduling, phaseConfigs.PRE_RACE_BUFFER);  // ← XÓA
  const endDate = competitionHint ? competitionHint.slice(0, 10) : ...;  // ← SỬA
  return { ... };
};
```

**Sau**:
```js
const buildTimelineFromPhases = (startDate, phaseConfigs) => {
  if (!hasCompletePhaseConfigs(phaseConfigs)) return { ...EMPTY_TIMELINE };
  const open = registrationOpenFromStartDate(startDate);
  const close = addDaysToDateTimeLocal(open, phaseConfigs.REGISTRATION);
  const review = addDaysToDateTimeLocal(close, phaseConfigs.REVIEW);
  const matching = addDaysToDateTimeLocal(review, phaseConfigs.JOCKEY_MATCHING);
  const scheduling = addDaysToDateTimeLocal(matching, phaseConfigs.SCHEDULING);
  const endDate = scheduling ? scheduling.slice(0, 10) : "";
  return {
    registrationOpenAt: open,
    registrationCloseAt: close,
    reviewDeadlineAt: review,
    jockeyMatchingDeadlineAt: matching,
    schedulingDeadlineAt: scheduling,
    endDate,
  };
};
```

### 1.5. Sửa `INITIAL_FORM` — xóa field cũ, thêm field mới

**Xóa** các dòng sau khỏi `INITIAL_FORM` (dòng 208–245):
```diff
- startEarlyToleranceMinutes: "0",
- applyBreakTime: false,
- breakStartTime: "12:00",
- breakEndTime: "13:30",
```

**Thêm** các field mới vào `INITIAL_FORM`:
```diff
+ trackCondition: "TURF",
+ maxEntriesPerRace: "16",
+ predictionOpenMinutesBefore: "120",
+ predictionCloseMinutesBefore: "5",
```

> **Vị trí gợi ý**:
> - `trackCondition` đặt cạnh `distance`
> - `maxEntriesPerRace` đặt cạnh `qualifiersPerRace`
> - `predictionOpenMinutesBefore` và `predictionCloseMinutesBefore` đặt cạnh `inspectionOpenMinutesBefore`

### 1.6. Sửa `STEP_FIELDS` — cập nhật field thuộc từng bước

**Bước 0** (dòng 249): thêm `"maxEntriesPerRace"`:
```js
["name", "description", "location", "startDate", "registrationFee", "systemContractFee",
 "totalPrizePool", "maxApprovedEntries", "qualifiersPerRace", "maxEntriesPerRace"],
```

**Bước 2** (dòng 251): thêm `"trackCondition"`:
```js
["allowedBreed", "raceClass", "distance", "trackCondition", "minHorseAge", "maxHorseAge",
 "topWeightKg", "minWeightKg", "equipmentWeightKg"],
```

**Bước 3** (dòng 252–258): xóa `breakEndTime`, thêm prediction fields:
```js
["raceDayStartTime", "raceDayEndTime", "minRaceIntervalMinutes",
 "defaultRaceOperationalMinutes", "predictionOpenMinutesBefore", "predictionCloseMinutesBefore"],
```

### 1.7. Sửa hàm `validate()` — xóa validate cũ, thêm validate mới

**Xóa** đoạn validate break time (dòng 359–361):
```diff
- if (values.applyBreakTime && (!values.breakStartTime || !values.breakEndTime
-     || values.breakStartTime <= values.raceDayStartTime
-     || values.breakStartTime >= values.breakEndTime
-     || values.breakEndTime >= values.raceDayEndTime)) {
-   errors.breakEndTime = "Giờ nghỉ phải nằm trọn trong khung giờ thi đấu.";
- }
```

**Thêm** validate cho field mới (cạnh validate `minRaceIntervalMinutes`, khoảng dòng 357):
```js
const maxEntries = Number(values.maxEntriesPerRace);
if (!Number.isInteger(maxEntries) || maxEntries < 1 || maxEntries > 16) {
  errors.maxEntriesPerRace = "Số ngựa tối đa mỗi trận từ 1 đến 16.";
}

if (!values.trackCondition) {
  errors.trackCondition = "Vui lòng chọn mặt đường đua.";
}

const predOpen = Number(values.predictionOpenMinutesBefore);
if (!Number.isInteger(predOpen) || predOpen < 1) {
  errors.predictionOpenMinutesBefore = "Mở dự đoán phải từ 1 phút trở lên.";
}
const predClose = Number(values.predictionCloseMinutesBefore);
if (!Number.isInteger(predClose) || predClose < 0) {
  errors.predictionCloseMinutesBefore = "Đóng dự đoán phải từ 0 phút trở lên.";
}
```

### 1.8. Sửa `useEffect` preload phase defaults (dòng 398–419)

Xóa dòng gán `PRE_RACE_BUFFER`:
```diff
  const phaseConfigs = {
    REGISTRATION: String(defaults.REGISTRATION || 6),
    REVIEW: String(defaults.REVIEW || 4),
    JOCKEY_MATCHING: String(defaults.JOCKEY_MATCHING || 7),
    SCHEDULING: String(defaults.SCHEDULING || 4),
-   PRE_RACE_BUFFER: String(defaults.PRE_RACE_BUFFER || 2),
  };
```

### 1.9. Sửa UI Bước 0 — thêm field `maxEntriesPerRace`

Thêm ngay **sau** field `qualifiersPerRace` (dòng 603–605):

```jsx
<Field
  label="Số ngựa tối đa mỗi trận"
  name="maxEntriesPerRace"
  error={errors.maxEntriesPerRace}
  hint="Từ 1 đến 16 ngựa. Dùng khi sinh bracket."
>
  {input("maxEntriesPerRace", "number", { min: 1, max: 16 })}
</Field>
```

### 1.10. Sửa UI Bước 2 — thêm select `trackCondition`

Thêm ngay **sau** field `distance` (dòng 682):

```jsx
<Field label="Mặt đường đua" name="trackCondition" error={errors.trackCondition}>
  <select
    id="trackCondition"
    value={form.trackCondition}
    onChange={(event) => update("trackCondition", event.target.value)}
  >
    {TRACK_CONDITIONS.map(([value, label]) => (
      <option key={value} value={value}>{label}</option>
    ))}
  </select>
</Field>
```

### 1.11. Sửa UI Bước 3 — xóa field cũ, thêm field mới

**Xóa hoàn toàn** các dòng sau (dòng 704–707):

```diff
- <Field label="Cho phép bắt đầu sớm, phút" name="startEarlyToleranceMinutes">
-   {input("startEarlyToleranceMinutes", "number", { min: 0 })}
- </Field>
```

```diff
- <div className="tournament-create-v2__toggle is-wide">
-   <div><strong>Áp dụng giờ nghỉ</strong>
-   <small>Giờ nghỉ phải nằm trong khung giờ thi đấu.</small></div>
-   <button type="button" className={form.applyBreakTime ? "is-on" : ""} ...>
-     <span />
-   </button>
- </div>
- {form.applyBreakTime ? <>
-   <Field label="Bắt đầu nghỉ" name="breakStartTime">{input("breakStartTime", "time")}</Field>
-   <Field label="Kết thúc nghỉ" name="breakEndTime" error={errors.breakEndTime}>{input("breakEndTime", "time")}</Field>
- </> : null}
```

**Thêm** 2 field prediction (đặt sau `startLateToleranceMinutes`):

```jsx
<Field
  label="Mở dự đoán trước cuộc đua, phút"
  name="predictionOpenMinutesBefore"
  error={errors.predictionOpenMinutesBefore}
  hint="Mặc định 120 phút (2 giờ) trước giờ xuất phát."
>
  {input("predictionOpenMinutesBefore", "number", { min: 1 })}
</Field>
<Field
  label="Đóng dự đoán trước cuộc đua, phút"
  name="predictionCloseMinutesBefore"
  error={errors.predictionCloseMinutesBefore}
  hint="Mặc định 5 phút trước giờ xuất phát."
>
  {input("predictionCloseMinutesBefore", "number", { min: 0 })}
</Field>
```

---

## 2. File `tournamentService.js` — hàm `buildTournamentPayload`

### 2.1. Xóa các field cũ khỏi payload

```diff
  const payload = {
    ...
-   applyBreakTime,
    ...
-   startEarlyToleranceMinutes: toInteger(form.startEarlyToleranceMinutes) ?? 0,
    ...
  };

- if (applyBreakTime) {
-   payload.breakStartTime = toApiTime(form.breakStartTime);
-   payload.breakEndTime = toApiTime(form.breakEndTime);
- }
```

Cũng xóa dòng khai báo biến ở đầu hàm:
```diff
- const applyBreakTime = Boolean(form.applyBreakTime);
```

### 2.2. Thêm các field mới vào payload

Thêm vào object `payload` (cạnh `qualifiersPerRace`):

```js
trackCondition: form.trackCondition || "TURF",
maxEntriesPerRace: toInteger(form.maxEntriesPerRace) ?? 16,
predictionOpenMinutesBefore: toInteger(form.predictionOpenMinutesBefore) ?? 120,
predictionCloseMinutesBefore: toInteger(form.predictionCloseMinutesBefore) ?? 5,
```

### 2.3. Xóa `PRE_RACE_BUFFER` khỏi filter phaseConfigs (nếu có)

Kiểm tra xem khi build `phaseConfigs` cho payload, nếu FE gửi key `PRE_RACE_BUFFER` thì BE sẽ báo lỗi. Đảm bảo `PHASE_KEYS` đã không chứa `PRE_RACE_BUFFER` (đã sửa ở bước 1.2) thì phần này tự được loại.

---

## 3. Tóm tắt mapping FE ↔ BE API

### Payload `POST /api/admin/tournaments` sau khi sửa:

```json
{
  "name": "string",
  "description": "string",
  "startDate": "2026-07-25",
  "endDate": "2026-08-20",
  "location": "string",
  "registrationFee": 500000,
  "systemContractFee": 100000,
  "totalPrizePool": 50000000,
  "allowedBreed": "THOROUGHBRED",
  "raceClass": "CLASS_5",
  "distance": "SPRINT_1200M",
  "trackCondition": "TURF",              // ← MỚI
  "minHorseAge": 3,
  "maxHorseAge": 8,
  "handicapEnabled": false,
  "maxApprovedEntries": 32,
  "qualifiersPerRace": 4,
  "maxEntriesPerRace": 16,               // ← MỚI
  "raceDayStartTime": "08:00:00",
  "raceDayEndTime": "18:00:00",
  "minRaceIntervalMinutes": 30,
  "defaultRaceOperationalMinutes": 5,
  "inspectionOpenMinutesBefore": 60,
  "inspectionCloseMinutesBefore": 5,
  "startLateToleranceMinutes": 30,
  "predictionOpenMinutesBefore": 120,     // ← MỚI
  "predictionCloseMinutesBefore": 5,      // ← MỚI
  "registrationOpenAt": "2026-07-25T08:00:00",
  "registrationCloseAt": "2026-07-31T08:00:00",
  "reviewDeadlineAt": "2026-08-04T08:00:00",
  "jockeyMatchingDeadlineAt": "2026-08-11T08:00:00",
  "schedulingDeadlineAt": "2026-08-15T08:00:00",
  "phaseConfigs": {
    "REGISTRATION": 6,
    "REVIEW": 4,
    "JOCKEY_MATCHING": 7,
    "SCHEDULING": 4
  },
  "ratingConfig": { ... }
}
```

### Các field đã bị xóa hoàn toàn (KHÔNG được gửi):

| Field | Lý do |
|:---|:---|
| `PRE_RACE_BUFFER` (trong phaseConfigs) | BE đã xóa, endDate = ngày sau schedulingDeadlineAt |
| `startEarlyToleranceMinutes` | BE đã xóa, không cho phép bắt đầu sớm |
| `applyBreakTime` | BE đã xóa |
| `breakStartTime` | BE đã xóa |
| `breakEndTime` | BE đã xóa |
| `minEntriesPerRace` | BE đã xóa, tính tự động từ qualifiersPerRace |
| `predictionCardOpenHoursBeforeFirstRace` | BE đã xóa, thay bằng predictionOpenMinutesBefore |

---

## 4. Enum values tham khảo

### `trackCondition`
| Value | Hiển thị |
|:---|:---|
| `TURF` | Cỏ tự nhiên (Turf) |
| `DIRT` | Đất (Dirt) |
| `SYNTHETIC` | Nhân tạo (Synthetic) |
| `SAND` | Cát (Sand) |

### `allowedBreed`, `raceClass`, `distance`
Giữ nguyên như hiện tại, không thay đổi.

---

## 5. Checklist kiểm tra sau khi sửa

- [ ] Bước 0: Có hiện field "Số ngựa tối đa mỗi trận" (maxEntriesPerRace), validate 1–16
- [ ] Bước 1: Không còn ô nhập "Buffer trước đua" (PRE_RACE_BUFFER), chỉ còn 4 phase
- [ ] Bước 1: Mốc thời gian tự động tính đúng (endDate = ngày của schedulingDeadlineAt)
- [ ] Bước 2: Có hiện select "Mặt đường đua" (trackCondition) với 4 options
- [ ] Bước 3: Không còn "Cho phép bắt đầu sớm" và toggle "Áp dụng giờ nghỉ"
- [ ] Bước 3: Có 2 field dự đoán: "Mở dự đoán trước cuộc đua" và "Đóng dự đoán trước cuộc đua"
- [ ] Submit: Payload gửi BE không chứa 5 field đã xóa
- [ ] Submit: Payload gửi BE có chứa `trackCondition`, `maxEntriesPerRace`, `predictionOpenMinutesBefore`, `predictionCloseMinutesBefore`
- [ ] Tạo giải thành công, navigate sang trang config

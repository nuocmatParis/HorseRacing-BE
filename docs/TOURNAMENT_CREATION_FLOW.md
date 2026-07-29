# Luồng tạo giải đấu (Tournament) — Chi tiết

## 1. Admin tạo giải

**`POST /api/admin/tournaments`**

### Request body

```json
{
  "name": "Mùa giải 1",
  "description": "Giải đua ngựa mùa xuân",
  "startDate": "2026-08-01",
  "endDate": "2026-08-10",
  "location": "Trường đua Phú Thọ",
  "registrationFee": 500000,
  "systemContractFee": 200000,
  "totalPrizePool": 50000000,
  "allowedBreed": "THOROUGHBRED",
  "raceClass": "CLASS_1",
  "distance": "DIST_1200",
  "minHorseAge": 3,
  "maxHorseAge": 10,
  "handicapEnabled": true,
  "topWeightLbs": 135,
  "minWeightLbs": 115,
  "equipmentWeightKg": 1.5,
  "maxApprovedEntries": 48,

  "predictionTop1CorrectPoints": 100,
  "predictionTop3ExactPositionPoints": 30,
  "predictionTop3CorrectHorsePoints": 10,
  "predictionTop3PerfectBonusPoints": 50,

  "predictionOpenMinutesBefore": 120,
  "predictionCloseMinutesBefore": 5,
  "predictionCardOpenHoursBeforeFirstRace": 24,
  "inspectionOpenMinutesBefore": 60,
  "inspectionCloseMinutesBefore": 5,

  "maxRacesPerDay": 9,
  "minRaceIntervalMinutes": 30,
  "startEarlyToleranceMinutes": 0,
  "startLateToleranceMinutes": 30,
  "defaultRaceOperationalMinutes": 5,
  "raceDayStartTime": "08:00",
  "raceDayEndTime": "18:00",
  "applyBreakTime": false,
  "breakStartTime": null,
  "breakEndTime": null,

  "qualifiersPerRace": 4,

  "registrationOpenAt": "2026-07-01T08:00:00",
  "registrationCloseAt": "2026-07-05T08:00:00",
  "reviewDeadlineAt": "2026-07-10T08:00:00",
  "jockeyMatchingDeadlineAt": "2026-07-14T08:00:00",
  "schedulingDeadlineAt": "2026-07-19T08:00:00",

  "phaseConfigs": null
}
```

### Backend xử lý

#### Bước 1 — Validate cơ bản
- `minHorseAge < maxHorseAge`
- Kiểm tra tên không trùng
- Validate handicap settings

#### Bước 2 — Validate scheduling
Dùng `validateSchedulingAndTimeline()`:
- `inspectionOpenMinutesBefore` (30-90) `> inspectionCloseMinutesBefore >= predictionCloseMinutesBefore >= 0`
- `minRaceIntervalMinutes` trong `[1, 30]`
- `raceDayStartTime < raceDayEndTime`
- Break time phải nằm trong `[raceDayStartTime, raceDayEndTime]`

#### Bước 3 — Validate timeline thứ tự
```
registrationOpenAt < registrationCloseAt
    < reviewDeadlineAt < jockeyMatchingDeadlineAt < schedulingDeadlineAt
```

#### Bước 4 — Resolve phase configs
Nếu `request.phaseConfigs != null` → dùng của request.  
Nếu `null` → gọi `getDefaultPhaseConfigs()`:
```
REGISTRATION     → DB PhaseTimingConfig (fallback 0)
REVIEW           → DB PhaseTimingConfig (fallback 0)
JOCKEY_MATCHING  → DB PhaseTimingConfig (fallback 0)
SCHEDULING       → DB PhaseTimingConfig (fallback 0)
PRE_RACE_BUFFER  → DB PhaseTimingConfig (fallback 0)
```

#### Bước 5 — Validate timeline duration
```
registrationCloseAt  ≥ registrationOpenAt + REGISTRATION days
reviewDeadlineAt     ≥ registrationCloseAt + REVIEW days
jockeyMatchingAt     ≥ reviewDeadlineAt + JOCKEY_MATCHING days
schedulingDeadlineAt ≥ jockeyMatchingAt + SCHEDULING days
```

#### Bước 6 — Tính competitionStartAt
```
competitionStartAt = schedulingDeadlineAt.toLocalDate()
    + PRE_RACE_BUFFER days
    .atTime(raceDayStartTime)
```

#### Bước 7 — Map request → Entity + set defaults
```java
Tournament tournament = tournamentMapper.toTournament(request);
// MapStruct tự động map qualifiersPerRace, maxEntriesPerRace, minEntriesPerRace

tournament.setStatus(DRAFT);
tournament.setPhase(DRAFT);
tournament.setMaxApprovedEntries(maxEntries);
tournament.setMaxApprovedHorses(maxEntries);
tournament.setMaxApprovedJockeys(maxEntries);   // = maxEntries
tournament.setCreatedBy(currentUser);
tournament.setCreatedAt(now);
tournament.setCompetitionStartAt(competitionStartAt);
```

#### Bước 8 — Lưu DB
```java
tournamentRepository.save(tournament);
savePhaseConfigs(tournamentId, phaseConfigs);
```

### Response
```json
{
  "code": 200,
  "message": "Success",
  "result": {
    "tournamentId": "uuid-...",
    "name": "Mùa giải 1",
    "maxApprovedEntries": 48,
    "qualifiersPerRace": 4,
    "maxEntriesPerRace": 16,
    "minEntriesPerRace": 8,
    "status": "DRAFT",
    "phase": "DRAFT",
    "competitionStartAt": "2026-07-21T08:00:00",
    "phaseConfigs": {
      "REGISTRATION": 3,
      "REVIEW": 4,
      "JOCKEY_MATCHING": 3,
      "SCHEDULING": 4,
      "PRE_RACE_BUFFER": 2
    }
  }
}
```

---

## 2. Admin preview bracket

**`GET /api/admin/tournaments/{id}/bracket-preview?actualEntries={n}`**

### Backend xử lý

#### Bước 1 — Đọc tournament + validate
```java
Tournament tournament = tournamentRepository.findById(id);
validateStatus(DRAFT);
validateMaxApprovedEntries(actualEntries);  // chỉ cần >= 1
```

#### Bước 2 — Đọc config từ tournament
```java
int maxEntriesPerRace = tournament.getMaxEntriesPerRace();  // default 16
int qualifiersPerRace = tournament.getQualifiersPerRace();   // default 4
```

#### Bước 3 — Tính bracket structure

**`BracketCalculator.calculate(entries, maxPerRace, qpr)`**

```
races = ceil(remaining / maxEntriesPerRace)

while true:
  entriesPerRace = ceil(remaining / races)

  nếu races == 1 && remaining <= maxEntriesPerRace:
    → 1 race chung kết, dừng

  thêm vòng: races race × entriesPerRace, qpr = qualifiersPerRace
  qualifiers = races × qualifiersPerRace

  nếu qualifiers ≤ maxEntriesPerRace:
    → 1 race chung kết, dừng

  remaining = qualifiers
  races = ceil(qualifiers / maxEntriesPerRace)
  seq++
```

**Ví dụ:** 48 entries, maxPerRace=16, qpr=4

| Vòng | races | entries/race | qpr | qualifiers | Tiếp |
|------|-------|-------------|-----|-----------|------|
| Vòng 1 | 3 | 16 | 4 | 12 | 12 ≤ 16 |
| CK | 1 | 12 | 0 | — | hết |

→ **2 vòng, 4 races**

**Ví dụ 2:** 100 entries, maxPerRace=16, qpr=4

| Vòng | races | entries/race | qpr | qualifiers | Tiếp |
|------|-------|-------------|-----|-----------|------|
| Vòng 1 | 7 | 15 | 4 | 28 | 28 > 16 |
| Vòng 2 | 2 | 14 | 4 | 8 | 8 ≤ 16 |
| CK | 1 | 8 | 0 | — | hết |

→ **3 vòng, 10 races**

#### Bước 4 — Xếp lịch chi tiết

**`RaceScheduleCalculator.scheduleRounds(rounds, tournament, preRaceBufferDays)`**

- Đọc `preRaceBufferDays` từ `PhaseTimingConfig` (fallback 0)
- Mỗi vòng: sinh các `RacePlan` với `startTime`/`endTime` dựa trên:
  - `competitionStartAt`
  - `raceDayStartTime` / `raceDayEndTime`
  - `maxRacesPerDay`
  - `minRaceIntervalMinutes`
  - `defaultRaceOperationalMinutes`
  - `applyBreakTime` / `breakStartTime` / `breakEndTime`
- Giữa các vòng: cách nhau `preRaceBufferDays` ngày

**Ví dụ:** competitionStartAt=2026-07-21 08:00, maxRacesPerDay=4, interval=30p, operational=5p

```
Vòng 1 (3 races):
  Race 1: 21/07 08:00 – 08:05
  Race 2: 21/07 08:35 – 08:40
  Race 3: 21/07 09:10 – 09:15
→ roundEnd = 09:15

preRaceBufferDays = 1 → 22/07 08:00

Chung Kết (1 race):
  Race 4: 22/07 08:00 – 08:05
```

#### Bước 5 — Trả về FE

```json
{
  "bracket": {
    "totalEntries": 48,
    "roundCount": 2,
    "rounds": [
      {
        "sequenceOrder": 1,
        "roundName": "Vòng 1",
        "raceCount": 3,
        "entriesPerRace": 16,
        "qualifiersPerRace": 4,
        "isFinal": false,
        "estimatedStartDate": "2026-07-21T08:00:00",
        "estimatedEndDate": "2026-07-21T09:15:00",
        "races": [
          { "sequenceOrder": 1, "name": "Race 1", "startTime": "...", "endTime": "..." },
          { "sequenceOrder": 2, "name": "Race 2", "startTime": "...", "endTime": "..." },
          { "sequenceOrder": 3, "name": "Race 3", "startTime": "...", "endTime": "..." }
        ]
      },
      {
        "sequenceOrder": 2,
        "roundName": "Chung Kết",
        "raceCount": 1,
        "entriesPerRace": 12,
        "qualifiersPerRace": 0,
        "isFinal": true,
        "estimatedStartDate": "2026-07-22T08:00:00",
        "estimatedEndDate": "2026-07-22T08:05:00",
        "races": [
          { "sequenceOrder": 1, "name": "Race 1", "startTime": "...", "endTime": "..." }
        ]
      }
    ]
  },
  "phaseConfigs": {
    "REGISTRATION": 3,
    "REVIEW": 4,
    "JOCKEY_MATCHING": 3,
    "SCHEDULING": 4,
    "PRE_RACE_BUFFER": 2
  }
}
```

---

## 3. Admin confirm bracket

**`POST /api/admin/tournaments/{id}/bracket-confirm`**

- Tính toán lại bracket giống preview
- Xóa các Round + Race cũ (nếu có)
- Tạo mới **Round** entities:
  - `roundName`, `sequenceOrder`, `isFinal`
  - `qualifiersPerRace` từ kết quả tính toán
  - `maxEntries` = `tournament.maxEntriesPerRace`
  - `minEntries` = `tournament.minEntriesPerRace`
  - `advancementRule`: `"Chung kết"` hoặc `"Top {n} mỗi race đi tiếp"`
- Tạo mới **Race** entities:
  - `name`: `"{roundName} - Race {seq}"`
  - `startTime`, `endTime` từ kết quả xếp lịch
  - `trackCondition`: `"Tốt"`
  - `distance`: từ tournament
- Chuyển các Round sang status `SCHEDULING`, transition `NOT_READY`

---

## 4. Admin update tournament

**`PUT /api/admin/tournaments/{id}`**

Có thể update `qualifiersPerRace` (cùng các field khác).  
Nếu update `maxApprovedEntries` → validate chỉ cần ≥ 1.

---

## 5. Admin recalculate bracket

**`PUT /api/admin/tournaments/{id}/bracket-recalculate?actualEntries={n}`**

- Update `maxApprovedEntries` + `maxApprovedHorses` lên giá trị mới
- Gọi `confirm()` → xóa bracket cũ + tạo bracket mới với entries mới

---

## Sơ đồ tổng thể

```
┌─────────────────────────────────────────────────────────────────┐
│  POST /api/admin/tournaments                                    │
│  → Validate fields                                              │
│  → Map request → Tournament entity (kèm qualifiersPerRace)      │
│  → Tính competitionStartAt                                      │
│  → Lưu Tournament + PhaseConfigs vào DB                         │
│  → Trả về TournamentResponse                                    │
└─────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│  GET /api/admin/tournaments/{id}/bracket-preview?entries=N      │
│  → BracketCalculator.calculate(entries, maxPerRace, qpr)        │
│  → RaceScheduleCalculator.scheduleRounds(...)                   │
│  → Trả về BracketPreviewResponse (bracket + phaseConfigs)       │
└─────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│  POST /api/admin/tournaments/{id}/bracket-confirm               │
│  → Tính bracket + schedule lại                                   │
│  → Xóa Round/Race cũ                                            │
│  → Tạo Round + Race entities mới → lưu DB                       │
└─────────────────────────────────────────────────────────────────┘
```

## Tham khảo

| File | Mô tả |
|------|-------|
| `policy/BracketCalculator.java` | Thuật toán tính bracket structure |
| `policy/RaceScheduleCalculator.java` | Xếp lịch race theo khung giờ |
| `policy/TournamentTimelinePolicy.java` | Policy tính timeline các phase |
| `entity/Tournament.java` | Entity tournament (qualifiersPerRace, max/minEntriesPerRace) |
| `dto/.../CreateTournamentRequest.java` | Request DTO tạo tournament |
| `dto/.../UpdateTournamentRequest.java` | Request DTO update tournament |
| `dto/.../TournamentResponse.java` | Response DTO tournament |
| `service/impl/TournamentServiceImpl.java` | Service xử lý tạo tournament |
| `service/impl/BracketServiceImpl.java` | Service xử lý bracket preview/confirm |

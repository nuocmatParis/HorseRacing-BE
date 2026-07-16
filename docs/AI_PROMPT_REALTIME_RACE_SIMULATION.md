# Prompt tích hợp Race Simulation thời gian thực vào HRTMS

Sao chép toàn bộ phần bên dưới và gửi cho AI coding agent.

---

## PROMPT

Bạn là senior full-stack engineer. Hãy triển khai tính năng mô phỏng đua ngựa thời gian thực vào hệ thống HRTMS hiện có, đồng thời redesign giao diện simulation để đồng bộ với giao diện chính.

### 1. Workspace và source cần đọc trước

Có ba source liên quan:

```text
D:\FPTU\Semester-5\SWP391\HorseRacing-BE
D:\FPTU\Semester-5\SWP391\HorseRacing_FE
D:\FPTU\Semester-5\SWP391\Simulate
```

Vai trò:

- `HorseRacing-BE`: backend chính Spring Boot, là nguồn dữ liệu và nghiệp vụ chính thức.
- `HorseRacing_FE`: frontend chính React/Vite, chứa router, auth, layout theo role và homepage.
- `Simulate`: prototype simulation gồm React frontend và Spring Boot backend độc lập. Chỉ dùng làm nguồn tham khảo/di chuyển thuật toán và UI; không giữ nó thành backend production thứ hai.

Trước khi sửa code, phải đọc đầy đủ các file liên quan trực tiếp, tối thiểu:

#### Backend chính

```text
src/main/java/com/swp391/horseracing/entity/Race.java
src/main/java/com/swp391/horseracing/entity/RaceEntry.java
src/main/java/com/swp391/horseracing/entity/Horse.java
src/main/java/com/swp391/horseracing/entity/Jockey.java
src/main/java/com/swp391/horseracing/entity/Violation.java
src/main/java/com/swp391/horseracing/entity/RaceResult.java
src/main/java/com/swp391/horseracing/service/impl/RaceServiceImpl.java
src/main/java/com/swp391/horseracing/service/impl/ViolationServiceImpl.java
src/main/java/com/swp391/horseracing/service/impl/RaceResultServiceImpl.java
src/main/java/com/swp391/horseracing/service/impl/RaceReportServiceImpl.java
src/main/java/com/swp391/horseracing/controller/RefereeRaceController.java
src/main/java/com/swp391/horseracing/controller/RefereeViolationController.java
src/main/java/com/swp391/horseracing/config/WebSocketConfig.java
src/main/java/com/swp391/horseracing/config/WebSocketJwtChannelInterceptor.java
src/main/java/com/swp391/horseracing/config/SecurityConfig.java
src/main/java/com/swp391/horseracing/repository/*
src/main/resources/db/migration/*
```

#### Frontend chính

```text
src/App.jsx
src/layouts/DashboardLayout.jsx
src/layouts/RolePortal.css
src/pages/referee/RefereeRaceDetail.jsx
src/pages/referee/RefereeRacesPage.jsx
src/pages/spectator/SpectatorRaceDetail.jsx
src/pages/public/HomePage.jsx
src/pages/public/HomePage.css
src/services/refereeService.js
src/services/spectatorRaceService.js
src/services/notificationSocketService.js
src/services/api/axios.js
src/routes/ProtectedRoute.jsx
src/constants/menuByRole.js
```

`src/App.jsx` là router đang hoạt động. Không chỉnh router legacy nếu không được sử dụng.

#### Prototype Simulate

```text
src/pages/RaceSimulationPage.jsx
src/components/*
src/hooks/useRacePlayback.js
src/api/*
backend/src/main/java/com/example/horseracing/service/RaceSimulationService.java
backend/src/main/java/com/example/horseracing/service/RaceWarningService.java
backend/src/main/java/com/example/horseracing/service/RaceFlagService.java
backend/src/main/java/com/example/horseracing/service/RaceCatalogService.java
backend/src/main/java/com/example/horseracing/model/*
```

Không được hành động dựa trên giả định cũ nếu code hiện tại đã thay đổi. Sau khi đọc, tóm tắt ngắn kiến trúc hiện tại và danh sách file dự kiến sửa rồi mới triển khai.

### 2. Mục tiêu nghiệp vụ

Luồng chính:

```text
Referee mở race được phân công
→ xem start-readiness
→ nhấn Chuẩn bị
→ backend tạo simulation session và snapshot các entry đủ điều kiện
→ FE hiển thị ngựa tại đúng làn ở vạch xuất phát
→ referee nhấn Start
→ backend gọi nghiệp vụ start race chính thức
→ simulation chạy tại backend
→ backend phát từng frame qua WebSocket
→ referee và spectator xem cùng một race đồng bộ
→ hệ thống phát hiện dữ liệu bất thường
→ warning chỉ gửi cho referee
→ referee Flag hoặc Ignore
→ race kết thúc với provisional result
→ referee review warning/flag
→ flag được xác nhận mới có thể tạo Violation
→ referee nhập/xác nhận result và report theo workflow hiện có
→ admin publish kết quả chính thức
```

Nguyên tắc bắt buộc:

1. Simulation engine chạy tại backend.
2. Frontend chỉ hiển thị và nội suy animation.
3. Không gửi toàn bộ timeline/kết quả tương lai cho client khi race vừa start.
4. Tất cả người xem cùng nhận một session, một random seed và một kết quả.
5. Warning của hệ thống không tự động trở thành violation.
6. Warning không được tự động disqualify entry.
7. Referee flag chỉ là yêu cầu review; violation là kết luận chính thức.
8. Dữ liệu cảnh báo nội bộ không được gửi cho homepage hoặc spectator.
9. Không phá workflow inspection, start race, RaceResult, RaceReport, prediction và rating hiện có.

### 3. Kiến trúc simulation được yêu cầu

Sử dụng kiến trúc hybrid phù hợp đồ án:

```text
Khi start:
Backend dùng engine để tính timeline deterministic với randomSeed cố định
→ timeline được giữ ở server
→ scheduler phát từng frame theo thời gian
→ client không nhận trước frame tương lai
```

Không để frontend tự tính speed/ranking/result.

Không chạy `Simulate/backend` như một service production riêng. Di chuyển/adapt engine vào backend chính, ví dụ:

```text
com.swp391.horseracing.simulation
├── api
├── domain
├── engine
├── realtime
├── anomaly
├── persistence
└── mapper
```

Giữ engine càng pure/deterministic càng tốt để có thể unit test mà không cần Spring context.

### 4. Simulation session

Tạo persistence model phù hợp với convention hiện tại:

```java
RaceSimulationSession {
    UUID sessionId;
    Race race;
    SimulationStatus status;
    Long randomSeed;
    Double currentRaceTimeSeconds;
    Long currentSequence;
    LocalDateTime preparedAt;
    LocalDateTime startedAt;
    LocalDateTime finishedAt;
    User preparedBy;
    User startedBy;
}
```

Status tối thiểu:

```java
PREPARING,
READY,
RUNNING,
FINISHED,
ABORTED
```

Yêu cầu:

- Mỗi race chỉ có tối đa một session active.
- `prepare` phải idempotent: nếu session đang `READY`, trả lại snapshot hiện có.
- `start` phải chống double-click/concurrency.
- Session phải gắn với UUID thật của race.
- Warning, flag và quyết định review phải lưu database.
- Không lưu từng telemetry frame thành một row riêng.
- Có thể giữ timeline/frame hiện tại trong memory đối với một-instance demo, nhưng session/final result/warning/flag phải persist.
- Nếu cần lưu timeline để replay, dùng JSON/LONGTEXT hoặc thiết kế snapshot hợp lý, không tạo hàng chục nghìn row.

Tạo Flyway migration với version tiếp theo đang còn trống. Không sửa migration cũ đã tồn tại.

### 5. Participant phải lấy từ dữ liệu thật

Loại bỏ `RaceCatalogService` hard-code khỏi luồng production.

Participant được lấy:

```text
Race
→ RaceEntry
→ JockeyHorseContract
→ Horse + Jockey
→ laneNumber
```

Chỉ đưa vào simulation các entry active theo nghiệp vụ hiện tại. Không đưa entry:

```text
SCRATCHED
WITHDRAWN_BEFORE_SCHEDULE
WITHDRAWN_AFTER_SCHEDULE
DISQUALIFIED
```

Trước `prepare`, tái sử dụng readiness/finalize inspection hiện có thay vì viết lại logic khác.

DTO snapshot ban đầu cần tối thiểu:

```json
{
  "sessionId": "uuid",
  "raceId": "uuid",
  "status": "READY",
  "participants": [
    {
      "entryId": "uuid",
      "horseId": "uuid",
      "horseName": "Thunder Bolt",
      "horseImageUrl": "...",
      "jockeyId": "uuid",
      "jockeyName": "...",
      "laneNumber": 1
    }
  ]
}
```

### 6. Mapping thuộc tính simulation

Backend chính hiện không nhất thiết có đúng các field prototype:

```text
Horse: baseSpeed, acceleration, stamina, consistency
Jockey: skill, aggressiveness, corneringSkill, staminaManagement
```

Không hard-code profile theo ID.

Trong phase này, ưu tiên tạo mapper deterministic từ dữ liệu hiện có:

- Horse `currentRating`, `raceClass`, `weight`, `age`, `totalRaces`, `totalWins`, `winRate`.
- Jockey `jockeyTier`, `experienceYears`, `totalRaces`, `totalWins`, `weight`, `specialization`.
- Handicap weight và track condition nếu nghiệp vụ hiện tại có dữ liệu.

Các công thức phải:

- Bounded trong khoảng hợp lệ.
- Cùng dữ liệu đầu vào và random seed cho cùng kết quả.
- Không tạo ưu thế chỉ vì lane number.
- Có comment hoặc tài liệu giải thích.
- Không dùng `Math.random()` rải rác; dùng một seeded random source.

Nếu code hiện tại đã có profile simulation tốt hơn, tái sử dụng thay vì tạo trùng.

### 7. API backend cần có

Tái sử dụng:

```http
GET  /api/referee/races/{raceId}/start-readiness
POST /api/referee/races/{raceId}/start
```

Bổ sung tối thiểu:

```http
POST /api/referee/races/{raceId}/simulation/prepare
GET  /api/races/{raceId}/live-snapshot
GET  /api/referee/races/{raceId}/simulation/warnings
GET  /api/referee/races/{raceId}/simulation/flags
POST /api/referee/races/{raceId}/simulation/warnings/{warningId}/ignore
POST /api/referee/races/{raceId}/simulation/warnings/{warningId}/flag
POST /api/referee/races/{raceId}/simulation/flags/manual
POST /api/referee/races/{raceId}/simulation/flags/{flagId}/dismiss
POST /api/referee/races/{raceId}/simulation/flags/{flagId}/confirm
GET  /api/referee/races/{raceId}/simulation/provisional-results
GET  /api/public/races/live
```

Có thể điều chỉnh URL để phù hợp convention hiện tại, nhưng phải nhất quán.

Authorization:

- `prepare/start/warning/flag/review`: chỉ referee được phân công hoặc head referee.
- `live-snapshot`: public DTO đã lọc.
- Endpoint referee không nhận `refereeId` từ client để tin tưởng; lấy referee từ JWT/current user.
- Không cho referee thao tác race không được phân công.

### 8. Kết nối start race với simulation

Trình tự bắt buộc:

```text
FE gọi start
→ RaceService.startRace() validate thành công
→ Race.status = ONGOING
→ session READY chuyển RUNNING
→ scheduler bắt đầu publish
```

Không chạy animation nếu start nghiệp vụ thất bại.

Không duplicate toàn bộ validation start race trong simulation service. Tái sử dụng service chính hoặc orchestration service có transaction boundary rõ ràng.

Nếu start thành công nhưng scheduler khởi động lỗi:

- Không giấu lỗi.
- Đánh session `ABORTED` hoặc xử lý rollback/compensation hợp lý.
- Ghi log có raceId và sessionId.

### 9. WebSocket real-time

Backend hiện đã có STOMP/SockJS và JWT channel interceptor cho notification.

Mở broker:

```java
registry.enableSimpleBroker("/queue", "/topic");
```

Public stream:

```text
/topic/races/{raceId}/live
```

Private referee stream:

```text
/user/queue/races/{raceId}/control
```

Public stream chứa:

- Race state.
- Telemetry đã lọc.
- Ranking tạm thời.
- Lap.
- Event công khai.
- Finish event.

Không chứa:

- Warning/risk score.
- Nghi ngờ doping.
- Ghi chú referee.
- Flag pending.
- Kết quả tương lai.

Private referee stream có thêm:

- System warning.
- Risk score.
- Telemetry chi tiết.
- Flag/review state.

Message envelope:

```json
{
  "type": "TELEMETRY_FRAME",
  "raceId": "uuid",
  "sessionId": "uuid",
  "sequence": 125,
  "raceTimeSeconds": 62.5,
  "serverTime": "2026-07-16T15:01:02",
  "payload": {}
}
```

Event type tối thiểu:

```text
SESSION_READY
RACE_STARTED
TELEMETRY_FRAME
RANKING_UPDATED
RACE_EVENT
SYSTEM_WARNING
WARNING_IGNORED
REFEREE_FLAGGED
RACE_FINISHED
SESSION_ABORTED
```

WebSocket security:

- Homepage phải xem được public live stream mà không cần login.
- Anonymous client chỉ được subscribe public destination.
- Referee control stream bắt buộc JWT role `REFEREE` và assignment hợp lệ.
- Không cho client gửi command để tự thay telemetry.
- Kiểm tra `WebSocketJwtChannelInterceptor` hiện tại và sửa có kiểm soát; không làm hỏng notification socket.

### 10. Tần suất frame và animation

Backend publish 1–2 telemetry frame/giây.

Frontend dùng `requestAnimationFrame` hoặc interpolation hook để animate khoảng cách giữa hai server frame lên khoảng 60 FPS.

Không publish 60 WebSocket message/giây.

Frame mẫu:

```json
{
  "type": "TELEMETRY_FRAME",
  "raceTimeSeconds": 12.5,
  "payload": {
    "horses": [
      {
        "entryId": "uuid",
        "horseId": "uuid",
        "horseName": "Thunder Bolt",
        "horseImageUrl": "...",
        "jockeyName": "...",
        "laneNumber": 1,
        "lapNumber": 1,
        "distance": 194.7,
        "speed": 16.2,
        "energy": 72.0,
        "rank": 1,
        "status": "RUNNING"
      }
    ]
  }
}
```

### 11. Reconnect và snapshot

Khi client refresh hoặc mất mạng:

```text
GET live-snapshot
→ render state hiện tại
→ subscribe WebSocket
→ tiếp tục từ sequence mới
```

Snapshot tối thiểu:

```json
{
  "raceId": "uuid",
  "sessionId": "uuid",
  "status": "RUNNING",
  "sequence": 125,
  "raceTimeSeconds": 62.5,
  "horses": [],
  "ranking": [],
  "publicEvents": []
}
```

Frontend phải:

- Bỏ message trùng hoặc sequence cũ.
- Hiển thị trạng thái reconnecting.
- Không reset ngựa về vạch xuất phát khi reconnect.
- Có fallback REST snapshot nếu WebSocket chưa kết nối.

### 12. Anomaly detector

Chuyển/adapt detector từ prototype nhưng bỏ việc quyết định warning dựa trên xác suất ngẫu nhiên.

Random được phép dùng trong simulation performance, không được dùng để biến cùng telemetry lúc cảnh báo lúc không.

Detector deterministic tối thiểu:

- `ABNORMAL_SPEED_SPIKE`
- `UNREALISTIC_ACCELERATION`
- `STAMINA_DROP_TOO_FAST`
- `CURVE_SPEED_ABNORMAL`
- `PERFORMANCE_OUTLIER`

Risk score bounded `0..1`.

Ngưỡng gợi ý:

```text
risk < 0.45       → không warning
0.45–0.70         → MEDIUM
0.70–0.90         → HIGH
>= 0.90           → CRITICAL
```

Mỗi warning lưu:

```text
raceId
sessionId
entryId
horseId
warningType
severity
riskScore
raceTimeSeconds
message
suggestedAction
createdAt
reviewStatus
reviewedBy
reviewedAt
reviewNote
```

Không tự sinh cảnh báo “chơi xấu” nếu engine chưa có lateral position/lane-change evidence.

Nếu triển khai `LANE_VIOLATION`, `DANGEROUS_CROSSING` hoặc `OBSTRUCTION`, phải thêm dữ liệu thật vào frame:

```text
lanePosition
lateralOffset
previousLane
distanceToNearbyHorse
heading/change direction
```

Không fake cảnh báo loại này chỉ để UI đẹp.

### 13. Warning, Flag và Violation

Tách rõ:

```text
SYSTEM_WARNING = máy phát hiện
REFEREE_FLAG = trọng tài yêu cầu xem xét
VIOLATION = kết luận chính thức có penalty
```

Luồng:

```text
Warning
→ referee Ignore hoặc Flag
→ race kết thúc
→ review flag
→ Dismiss hoặc Confirm
→ khi Confirm mới tạo/chuyển sang workflow Violation hiện có
```

Manual flag không cần warningId.

Không tin `refereeId` client gửi. Lấy current user từ security context.

Flag phải lưu:

```text
source: SYSTEM_WARNING | MANUAL
status: PENDING_REVIEW | DISMISSED | CONFIRMED
severity
raceTimeSeconds
note
flaggedBy
flaggedAt
reviewedBy
reviewedAt
```

Khi confirm flag:

- Có thể tạo draft violation hoặc mở form đã prefill.
- Không tự chọn penalty nguy hiểm nếu referee chưa xác nhận.
- Tái sử dụng enum/validation `ViolationType`, `PenaltyType` hiện có.

### 14. Race finish và provisional result

Khi timeline kết thúc:

```text
session.status = FINISHED
→ phát RACE_FINISHED
→ lưu provisional ranking
→ không tự publish official result
```

Referee page hiển thị:

- Provisional ranking.
- Finish time.
- DNF.
- Pending warning/flag.
- Nút review.
- Nút dùng provisional result để prefill form result hiện có.

Không bỏ qua workflow:

```text
RaceResult
→ RaceReport
→ referee sign
→ admin publish
→ prediction scoring/rating update
```

### 15. Frontend integration

Không tạo một React app production thứ hai.

Di chuyển/adapt các component hữu ích từ `Simulate` vào `HorseRacing_FE`, ví dụ:

```text
src/features/live-race/
├── api/
├── hooks/
├── components/
├── pages/
├── utils/
└── styles/
```

Các component nên tách:

```text
LiveRaceTrack
RaceControlPanel
LiveRankingBoard
HorseTelemetryPanel
RaceEventTimeline
SystemWarningPanel
RefereeFlagModal
IncidentReviewPanel
ProvisionalResultPanel
LiveConnectionBadge
```

Tạo service riêng:

```text
raceSimulationService.js
raceLiveSocketService.js
raceIncidentService.js
```

Không nhồi tất cả logic vào `RefereeRaceDetail.jsx`.

Tái sử dụng:

- Axios instance hiện có.
- JWT trong localStorage theo convention hiện tại.
- `@stomp/stompjs` và `sockjs-client` đã cài.
- Common loading/error/toast/status badge.
- Design variables/layout hiện tại.

### 16. Routes cần bổ sung

Trong router active `src/App.jsx`:

```text
/referee/races/:raceId/live
/races/:raceId/live
```

Ý nghĩa:

- `/referee/races/:raceId/live`: protected role referee, có control/warning/flag.
- `/races/:raceId/live`: public live viewer dùng cho homepage và spectator.

Trong `RefereeRaceDetail`, thêm CTA “Mở phòng điều khiển trực tiếp” khi race phù hợp.

Trong `SpectatorRaceDetail`, thêm CTA “Xem trực tiếp” khi race `ONGOING`.

Homepage live card dẫn tới `/races/:raceId/live`.

### 17. Redesign giao diện simulation

Prototype hiện có nhiều chức năng nhưng visual chưa đồng bộ hoàn toàn với HRTMS. Hãy redesign theo design system của FE chính:

```text
Nền: navy/teal rất tối
Surface: #081d25 / #0a222b hoặc CSS variable hiện có
Gold accent: màu vàng đang dùng ở homepage/portal
Positive: xanh lá
Warning: amber
Critical: đỏ
Typography và border radius đồng bộ RolePortal/HomePage
```

Không dùng Bootstrap mặc định trông như trang admin thô.

Không tạo gradient tím hoặc neon game. Giao diện phải mang cảm giác “race operations console” chuyên nghiệp, không phải trò chơi casino.

#### Referee desktop layout

```text
┌─────────────────────────────────────────────────────────────┐
│ Race header | LIVE badge | clock | connection | controls   │
├──────────────────────────────────────┬──────────────────────┤
│                                      │ Live ranking         │
│ Oval race track                      ├──────────────────────┤
│                                      │ Selected horse       │
│                                      │ telemetry            │
├──────────────────────────────────────┼──────────────────────┤
│ Speed/energy/lap charts              │ Warning/flags        │
├──────────────────────────────────────┴──────────────────────┤
│ Event timeline / provisional result                         │
└─────────────────────────────────────────────────────────────┘
```

#### Trạng thái giao diện

```text
SCHEDULED:
    readiness checklist + nút Chuẩn bị

READY:
    ngựa đứng tại gate + countdown-ready + nút Start

RUNNING:
    track animation + ranking + telemetry + warning/flag

FINISHED:
    provisional result + pending review

ABORTED:
    error state + retry/recovery instruction
```

#### Horse rendering

- Mỗi horse có màu/lane rõ ràng.
- Hiển thị tên viết tắt hoặc số lane.
- Horse đang chọn có highlight.
- Horse bị referee flag có icon cờ nhưng chỉ trong referee UI.
- Không render warning nội bộ trong spectator UI.
- Tooltip phải có horse, jockey, lane, speed, energy, lap, rank.
- Dùng ảnh ngựa nếu phù hợp nhưng không để ảnh che đường đua.

#### Responsive

- Desktop ưu tiên control room.
- Tablet chuyển side panels xuống dưới track.
- Mobile dùng simplified live view; referee control vẫn usable.
- Không overflow ngang ngoài track area.
- Tôn trọng `prefers-reduced-motion`; khi bật, giảm animation và cho phép cập nhật theo frame.

#### Accessibility

- Button có label rõ.
- Warning không chỉ phân biệt bằng màu.
- Có text severity.
- Focus state rõ.
- Modal trap focus.
- Track/ranking có fallback text/table cho screen reader.

### 18. Homepage live card

Homepage chỉ hiển thị bản rút gọn:

```text
● LIVE
Tournament + race name
Top 3 hiện tại
Race progress
Ngựa đang dẫn đầu
Nút “Xem trực tiếp”
```

Homepage:

1. Gọi `GET /api/public/races/live`.
2. Nếu có race live, lấy snapshot.
3. Subscribe public topic của đúng race đang hiển thị.
4. Không subscribe toàn bộ race.
5. Khi race kết thúc, chuyển card sang “Kết quả tạm thời”.

Không làm homepage rerender toàn bộ mỗi telemetry tick. Cô lập state live card.

### 19. Spectator live page

Public/spectator page hiển thị:

- Track.
- Horse/jockey/lane.
- Lap.
- Race clock.
- Top ranking.
- Public events.
- Provisional/final status.

Không hiển thị:

- System warning.
- Risk score.
- Doping suspicion.
- Pending referee flag.
- Private note.

Khi race kết thúc:

```text
Kết quả tạm thời – đang chờ trọng tài xác nhận
```

Chỉ hiển thị “Kết quả chính thức” sau workflow publish report hiện có.

### 20. Không làm hỏng các chức năng hiện tại

Phải giữ nguyên:

- Notification WebSocket.
- JWT login/logout.
- Existing role routes/layout.
- Inspection flow.
- Race start readiness.
- Prediction close/start behavior.
- Result/report workflow.
- Rating/prize/scoring.
- Admin scheduling.

Không sửa unrelated code chỉ để format.

Không xóa hoặc overwrite thay đổi của người khác trong dirty worktree.

Không dùng destructive Git command.

### 21. Seed/demo data

Bổ sung dữ liệu demo hoặc script phù hợp để có ít nhất:

- Một race `SCHEDULED` đủ 8 entry, đủ inspection và đúng start window.
- Một referee được phân công/head referee.
- Horse/jockey/profile đủ để prepare.
- Một deterministic seed luôn sinh ít nhất một warning có thể review trong demo.
- Một case DNF.
- Một case không warning.

Không hard-code warning trực tiếp trong frontend.

Nếu cần “demo mode”, nó phải được bật bằng profile/property rõ ràng và không ảnh hưởng production behavior.

### 22. Kiểm thử backend

Viết test tối thiểu:

1. Simulation cùng input + seed cho cùng result.
2. Participant mapping lấy đúng RaceEntry/lane.
3. Không lấy scratched/withdrawn/disqualified entry.
4. Referee không được phân công không prepare/start/flag được.
5. Một race không tạo hai active session.
6. Double start không tạo hai scheduler.
7. Frame sequence tăng đơn điệu.
8. Snapshot phản ánh frame mới nhất.
9. Anomaly detector deterministic.
10. Warning không tự tạo violation.
11. Confirm flag mới tạo/đi vào violation flow.
12. Public DTO không chứa warning/private note.
13. Race finish lưu provisional result nhưng chưa publish report.

Tái sử dụng test profile hiện có.

### 23. Kiểm thử frontend

Ít nhất phải chạy:

```text
npm run build
npm run lint
```

Kiểm tra thủ công:

- Referee chuẩn bị race.
- READY hiển thị đúng entry/lane.
- Start thất bại thì animation không chạy.
- Start thành công thì LIVE.
- Warning chỉ referee thấy.
- Flag/ignore cập nhật UI và persist.
- Spectator và referee cùng ranking.
- Refresh giữa race reconnect đúng.
- Homepage live card cập nhật.
- Race finish hiển thị provisional.
- Mobile không vỡ layout.

### 24. Documentation

Cập nhật hoặc tạo tài liệu:

```text
docs/REALTIME_RACE_SIMULATION.md
```

Nội dung:

- Kiến trúc.
- API.
- WebSocket destinations.
- Message schema.
- State machine.
- Cách chạy demo.
- Cách reconnect.
- Warning/flag/violation semantics.
- Known limitations.

### 25. Cách thực hiện

Làm theo phase và giữ project buildable sau mỗi phase:

```text
Phase 1: domain + migration + engine adapter
Phase 2: prepare/start/session lifecycle
Phase 3: frame scheduler + WebSocket + snapshot
Phase 4: warning/flag/review persistence
Phase 5: referee live UI
Phase 6: spectator/public live UI + homepage card
Phase 7: tests + docs + polish
```

Trước khi bắt đầu:

1. Kiểm tra git status ở cả BE và FE.
2. Không ghi đè thay đổi không liên quan.
3. Xác định API response wrapper và naming convention.
4. Viết plan ngắn.

Sau mỗi phase:

1. Compile/test phần vừa làm.
2. Sửa lỗi trước khi chuyển phase.
3. Báo file đã thay đổi.

Nếu gặp một điểm chưa rõ nhưng có thể suy ra an toàn từ code hiện tại, hãy tự chọn giải pháp phù hợp và ghi lại assumption. Chỉ hỏi người dùng khi lựa chọn đó làm thay đổi đáng kể nghiệp vụ.

### 26. Definition of Done

Tính năng hoàn thành khi:

- Referee mở race thật từ database.
- Nhấn Chuẩn bị và thấy đúng entry/lane.
- Nhấn Start và race thật chuyển `ONGOING`.
- Backend chạy một simulation session duy nhất.
- Referee và spectator xem cùng chuyển động/ranking theo thời gian.
- Client không nhận trước full timeline/result.
- Warning xuất hiện đúng thời điểm và chỉ referee thấy.
- Referee có thể flag/ignore và dữ liệu không mất khi restart.
- Warning không tự thành violation.
- Race finish sinh provisional result.
- Existing result/report publish flow vẫn hoạt động.
- Homepage có live card.
- Public live page không lộ private warning.
- FE responsive và đồng bộ visual HRTMS.
- BE tests pass.
- FE build và lint pass.
- Có tài liệu chạy demo.

Hãy triển khai thực tế, không chỉ viết pseudo-code hoặc tài liệu. Khi hoàn thành, cung cấp:

1. Tóm tắt kiến trúc đã triển khai.
2. Danh sách file chính đã thay đổi.
3. Migration mới.
4. API và WebSocket destinations.
5. Lệnh chạy BE/FE.
6. Kết quả test/build.
7. Các giới hạn còn lại.

## END PROMPT


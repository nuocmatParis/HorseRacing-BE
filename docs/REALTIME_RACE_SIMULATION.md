# Real-time Race Simulation

## Architecture

The production simulation is part of `HorseRacing-BE`; `Simulate/backend` is not a second production service.

```text
Race + RaceEntry + inspections
        |
        v
RaceSimulationLifecycleService -- snapshots active participants --> MySQL
        |
        v
DeterministicRaceEngine (pure, seeded, 0.5-second frames)
        |
        +-- full future timeline: server-side LONGTEXT only
        |
        v
RaceSimulationScheduler (2 telemetry frames/second)
        |
        +-- /topic/races/{raceId}/live              public filtered data
        +-- /user/queue/races/{raceId}/control      assigned referee data
```

The frontend never calculates speed, ranking, warnings, or results. It only interpolates between the latest two server frames with `requestAnimationFrame`.

Persistent data:

- `race_simulation_sessions`: lifecycle, seed, current sequence/snapshot, server-side timeline.
- `race_simulation_participants`: the immutable entry/horse/jockey/lane/profile snapshot.
- `race_simulation_warnings`: deterministic detector output persisted when its frame is published.
- `race_simulation_flags`: manual/system-warning flags and review decisions.
- `race_provisional_results`: simulation finish order; separate from official `race_results`.

Telemetry frames are not stored as individual rows.

## State machine

```text
SCHEDULED race
  -> PREPARING
  -> READY
  -> RUNNING
  -> FINISHED

PREPARING/READY/RUNNING -> ABORTED (startup, recovery, or scheduler failure)
```

- `prepare` is idempotent for an existing `READY` session.
- The race row and session row are pessimistically locked around start transitions.
- The existing `RaceService.startRace()` remains the business authority. Only after it succeeds and changes the race to `ONGOING` does the simulation become `RUNNING`.
- A scheduler startup failure is logged with race/session identifiers and marks the session `ABORTED`.
- A running session is resumed from its last persisted sequence after a single-instance application restart.

## REST API

All responses use the existing `{ code, message, result }` wrapper.

### Referee (JWT role `REFEREE`, assigned referee or head referee)

| Method | Endpoint | Purpose |
|---|---|---|
| GET | `/api/referee/races/{raceId}/start-readiness` | Existing readiness source |
| POST | `/api/referee/races/{raceId}/simulation/prepare` | Create/return participant snapshot |
| POST | `/api/referee/races/{raceId}/start` | Existing business start + simulation scheduler |
| GET | `/api/referee/races/{raceId}/simulation/warnings` | Private system warnings |
| POST | `/api/referee/races/{raceId}/simulation/warnings/{warningId}/ignore` | Review warning as ignored |
| POST | `/api/referee/races/{raceId}/simulation/warnings/{warningId}/flag` | Convert warning to pending flag |
| GET | `/api/referee/races/{raceId}/simulation/flags` | Private flag list |
| POST | `/api/referee/races/{raceId}/simulation/flags/manual` | Create manual flag |
| POST | `/api/referee/races/{raceId}/simulation/flags/{flagId}/dismiss` | Dismiss after finish |
| POST | `/api/referee/races/{raceId}/simulation/flags/{flagId}/confirm` | Confirm and return safe violation-form draft |
| GET | `/api/referee/races/{raceId}/simulation/provisional-results` | Full provisional ranking/DNF data |

The server always resolves the current referee from the JWT/security context; no referee ID is accepted from the client.

### Public

| Method | Endpoint | Purpose |
|---|---|---|
| GET | `/api/races/{raceId}/live-snapshot` | Reconnect/current public state |
| GET | `/api/public/races/live` | Current live race, or a result card finished in the last 30 minutes |

Public DTOs do not contain warnings, risk scores, referee notes, or pending flags.

## WebSocket

SockJS/STOMP endpoint: `/ws`

Destinations:

- Public/anonymous: `/topic/races/{raceId}/live`
- Private/assigned referee: `/user/queue/races/{raceId}/control`
- Existing notification destination remains `/user/queue/notifications`

Clients cannot send STOMP commands. Anonymous clients may subscribe only to a public live-race topic. A private control subscription requires a valid referee JWT and a database assignment/head-referee relationship for that race.

Envelope:

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

Published event types include `SESSION_READY`, `RACE_STARTED`, `TELEMETRY_FRAME`, `RANKING_UPDATED`, `RACE_EVENT`, `SYSTEM_WARNING`, `WARNING_IGNORED`, `REFEREE_FLAGGED`, `RACE_FINISHED`, and `SESSION_ABORTED`.

## Reconnect behavior

The frontend sequence is:

1. `GET /api/races/{raceId}/live-snapshot`.
2. Render current distance/ranking without returning horses to the gate.
3. Subscribe to the public topic (and the private queue for referees).
4. Drop any message whose sequence is older than or equal to the rendered sequence.
5. On reconnect, fetch another REST snapshot before continuing.

The connection badge displays connecting, connected, reconnecting, and disconnected states. Reduced-motion users receive direct frame updates without interpolation.

## Warning, flag, and violation semantics

```text
SYSTEM_WARNING (machine observation)
  -> IGNORE
  -> FLAG (request human review)
      -> DISMISS
      -> CONFIRM -> prefilled existing violation form
```

- Detection is rule-based and deterministic; there is no random decision to warn.
- A warning never disqualifies an entry and never creates a `Violation`.
- Confirming a flag returns a conservative draft (`OTHER` + `WARNING`) for the existing violation workflow. The referee must still submit the official violation and penalty.
- No lateral/lane-crossing warnings are generated because the engine does not model lateral position evidence.

## Demo

Use the normal Admin/inspection/referee workflow to create a scheduled race with at least eight entries, passed/confirmed horse and jockey inspections, lanes, and an assigned referee. Then start both applications.

Backend:

```powershell
cd D:\FPTU\Semester-5\SWP391\HorseRacing-BE
$env:RACE_SIMULATION_DEMO_MODE="true"
$env:RACE_SIMULATION_DEMO_SEED="3912026"
mvn spring-boot:run
```

Frontend:

```powershell
cd D:\FPTU\Semester-5\SWP391\HorseRacing_FE
npm run dev
```

Demo mode is disabled by default. When enabled, it changes deterministic telemetry so the normal detector receives one reviewable speed spike and one runner receives a DNF. It does not insert a warning in the frontend or bypass the detector.

Referee flow:

1. Open `/referee/races/{raceId}` and verify readiness.
2. Open `/referee/races/{raceId}/live`.
3. Select **Chuẩn bị**, verify horses/lanes at the gate, then select **Start**.
4. Open `/races/{raceId}/live` in another browser to verify identical public ranking.
5. Review warning/flag data after finish, then use the provisional result in the existing result/report workflow.

## Tests and build

```powershell
cd HorseRacing-BE
mvn test

cd ..\HorseRacing_FE
npm run lint
npm run build
```

The simulation tests cover seeded determinism, monotonic sequences, participant mapping/bounds, lane neutrality, active-entry filtering, one-session prepare idempotency, deterministic anomaly decisions, public DTO privacy, warning/flag separation, safe violation draft behavior, demo warning, and DNF behavior.

## Known limitations

- The scheduler and Spring simple broker are designed for a single backend instance. A multi-instance deployment needs a distributed lock, shared scheduler ownership, and an external STOMP broker.
- Recovery resumes the next frame after process restart; it does not fast-forward by wall-clock downtime.
- No lateral movement model exists, so obstruction, dangerous crossing, and lane-violation warnings are intentionally absent.
- The project uses the existing admin/inspection workflows for demo data rather than automatically inserting production-like accounts and contracts through Flyway.
- The frontend bundle still has the pre-existing large-chunk warning; route-level code splitting is a future optimization.

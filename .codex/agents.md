# Horse Racing Management System - Review Rules

## Project scope

This project contains these modules:

1. Auth, User, Role and Wallet
2. Invoice and Payment
3. Owner-Jockey-Horse Contract Finance
4. Horse and Jockey Inspection
5. Race Violation
6. Race Result and Race Report
7. Prize and Jockey Payout
8. Automatic Lane Assignment
9. Notification
10. Tournament, Round and Race Management
11. Frontend API integration

## Review mode

- Read the whole project before making conclusions.
- Do not modify source code during the audit.
- Do not assume undocumented behavior.
- Use the actual source code as evidence.
- Report file path and line number for every issue.
- Compare controller, service, repository, entity, DTO, mapper and frontend service together.
- Check both API correctness and business workflow correctness.

## Required checks

For every API, verify:

- HTTP method and URL
- Role authorization
- Authentication
- Request validation
- Resource ownership
- Entity relationship
- Service logic
- Transaction boundary
- Error handling
- Response format
- Duplicate request handling
- Invalid state transition handling
- Frontend integration
- Loading, empty and error states

## Important business rules

### Wallet and payment

- Every payment creates:
   - UserWallet debit
   - SystemRevenueWallet credit
- Every refund creates:
   - SystemRevenueWallet debit
   - UserWallet credit
- Payment requires sufficient balance.
- Paid invoice cannot be paid again.
- Refunded invoice cannot be refunded again.
- System wallet must be checked before refund or payout.
- Wallet and transaction updates must be atomic.

### Contract

- Owner can send multiple invitations.
- Jockey can receive multiple invitations.
- After jockey accepts:
   - Lock TournamentRegistration.
   - Lock JockeyTournamentRegistration.
   - Cancel remaining pending contracts for the same matching slot.
- HireFee payment sets EscrowStatus = HELD.
- ContractCreationFee must be paid before admin approval.
- Admin approval pays 30% advance to the jockey.
- The remaining 70% is released only after the tournament is finished.
- Payout must be idempotent and cannot happen twice.

### Inspection and race start

- Horse inspection and jockey inspection must pass before race start.
- Only authorized inspection staff can create inspection results.
- Only Head Referee can start the race.
- A Disqualified violation changes RaceEntry status to DISQUALIFIED.
- A started race cannot be started again.

### Race report and payout

- Prize payout is allowed only when the final race report is published.
- The race must belong to a final round.
- Only valid race results with PrizeMoney > 0 are included.
- Owner and jockey prize shares must be calculated correctly.
- Publishing and payout must not be executed twice.
- Prediction scoring must be executed consistently with report publishing.

### Automatic lane assignment

- Every confirmed race entry must receive one unique lane.
- A lane cannot be assigned to two entries in the same race.
- Lane assignment must respect the configured lane range.
- Automatic assignment must be deterministic or safely repeatable.
- Re-running auto-assignment must not create duplicate or conflicting lanes.
- Started races cannot change lane assignments unless explicitly allowed by business rules.

### Notification

- Notifications must be created for important state changes:
   - Contract invitation
   - Contract accepted/rejected
   - Payment success/failure
   - Admin approval/rejection
   - Inspection result
   - Violation/disqualification
   - Race report returned/signed/published
   - Prize or payout completed
- Notification recipients must be checked.
- Duplicate notifications must be prevented where necessary.
- Notification failure must not corrupt the main business transaction unless explicitly required.
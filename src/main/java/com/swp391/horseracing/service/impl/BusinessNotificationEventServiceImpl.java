package com.swp391.horseracing.service.impl;

import com.swp391.horseracing.dto.notification.NotificationEventCommand;
import com.swp391.horseracing.entity.*;
import com.swp391.horseracing.enums.NotificationEventType;
import com.swp391.horseracing.service.BusinessNotificationEventService;
import com.swp391.horseracing.service.NotificationEventPublisher;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class BusinessNotificationEventServiceImpl implements BusinessNotificationEventService {
    NotificationEventPublisher eventPublisher;

    @Override
    public void tournamentPublished(Tournament tournament) {
        Map<String, Object> payload = tournamentPayload(tournament);
        publish(NotificationEventType.TOURNAMENT_PUBLISHED, "TOURNAMENT", tournament.getTournamentId(), null, payload);
    }

    @Override
    public void schedulePublished(Tournament tournament) {
        Map<String, Object> payload = tournamentPayload(tournament);
        publish(NotificationEventType.SCHEDULE_PUBLISHED, "TOURNAMENT", tournament.getTournamentId(), null, payload);
    }

    @Override
    public void horseRegistrationApproved(HorseTournamentRegistration registration) {
        publishRegistration(NotificationEventType.REGISTRATION_APPROVED, "HORSE_REGISTRATION",
                registration.getHorseRegistrationId(), registration.getTournament(), null);
    }

    @Override
    public void horseRegistrationRejected(HorseTournamentRegistration registration, String reason) {
        publishRegistration(NotificationEventType.REGISTRATION_REJECTED, "HORSE_REGISTRATION",
                registration.getHorseRegistrationId(), registration.getTournament(), reason);
    }

    @Override
    public void jockeyRegistrationApproved(JockeyTournamentRegistration registration) {
        publishRegistration(NotificationEventType.REGISTRATION_APPROVED, "JOCKEY_REGISTRATION",
                registration.getJockeyTournamentRegId(), registration.getTournament(), null);
    }

    @Override
    public void jockeyRegistrationRejected(JockeyTournamentRegistration registration, String reason) {
        publishRegistration(NotificationEventType.REGISTRATION_REJECTED, "JOCKEY_REGISTRATION",
                registration.getJockeyTournamentRegId(), registration.getTournament(), reason);
    }

    @Override
    public void horseRegistrationWithdrawn(HorseTournamentRegistration registration, String reason) {
        publishRegistration(NotificationEventType.REGISTRATION_WITHDRAWN, "HORSE_REGISTRATION",
                registration.getHorseRegistrationId(), registration.getTournament(), reason);
    }

    @Override
    public void contractInvited(JockeyHorseContract contract) {
        publishContract(NotificationEventType.CONTRACT_INVITED, contract, null, null);
    }

    @Override
    public void contractAccepted(JockeyHorseContract contract) {
        publishContract(NotificationEventType.CONTRACT_ACCEPTED, contract, null, null);
    }

    @Override
    public void contractRejected(JockeyHorseContract contract, String reason) {
        publishContract(NotificationEventType.CONTRACT_REJECTED, contract, reason, null);
    }

    @Override
    public void contractApproved(JockeyHorseContract contract) {
        publishContract(NotificationEventType.CONTRACT_APPROVED, contract, null, null);
    }

    @Override
    public void contractCancelled(JockeyHorseContract contract, String reason) {
        publishContract(NotificationEventType.CONTRACT_CANCELLED, contract, reason, null);
    }

    @Override
    public void raceRescheduled(Race race, LocalDateTime oldStartTime, String reason) {
        Map<String, Object> payload = racePayload(race);
        payload.put("reason", reason);
        payload.put("oldStartTime", String.valueOf(oldStartTime));
        payload.put("newStartTime", String.valueOf(race.getStartTime()));
        publish(NotificationEventType.RACE_RESCHEDULED, "RACE", race.getRaceId(),
                String.valueOf(race.getRescheduledAt()), payload);
    }

    @Override
    public void raceCancelled(Race race, String reason) {
        Map<String, Object> payload = racePayload(race);
        payload.put("reason", reason);
        publish(NotificationEventType.RACE_CANCELLED, "RACE", race.getRaceId(), null, payload);
    }

    @Override
    public void raceStarted(Race race) {
        publish(NotificationEventType.RACE_STARTED, "RACE", race.getRaceId(), null, racePayload(race));
    }

    @Override
    public void entryScratched(RaceEntry entry) {
        Map<String, Object> payload = entryPayload(entry);
        payload.put("reason", entry.getScratchedReason());
        String scheduleVersion = entry.getRace().getRescheduledAt() == null
                ? "INITIAL" : String.valueOf(entry.getRace().getRescheduledAt());
        publish(NotificationEventType.ENTRY_SCRATCHED, "RACE_ENTRY", entry.getEntryId(), scheduleVersion, payload);
    }

    @Override
    public void horseInspectionFailed(RaceEntry entry) {
        publish(NotificationEventType.HORSE_INSPECTION_FAILED, "RACE_ENTRY", entry.getEntryId(), null, entryPayload(entry));
    }

    @Override
    public void jockeyInspectionFailed(RaceEntry entry) {
        publish(NotificationEventType.JOCKEY_INSPECTION_FAILED, "RACE_ENTRY", entry.getEntryId(), null, entryPayload(entry));
    }

    @Override
    public void predictedEntryScratched(RaceEntry entry) {
        publish(NotificationEventType.PREDICTED_ENTRY_SCRATCHED, "RACE_ENTRY", entry.getEntryId(), null, entryPayload(entry));
    }

    @Override
    public void resultPublished(Race race) {
        publish(NotificationEventType.RACE_RESULT_PUBLISHED, "RACE", race.getRaceId(), null, racePayload(race));
    }

    @Override
    public void predictionScored(Prediction prediction) {
        Map<String, Object> payload = racePayload(prediction.getRace());
        payload.put("points", prediction.getRewardPoints());
        publish(NotificationEventType.PREDICTION_SCORED, "PREDICTION", prediction.getPredictionId(), null, payload);
    }

    @Override
    public void predictionVoided(Prediction prediction, String reason) {
        Map<String, Object> payload = racePayload(prediction.getRace());
        payload.put("reason", reason);
        publish(NotificationEventType.PREDICTION_VOIDED, "PREDICTION", prediction.getPredictionId(), null, payload);
    }

    @Override
    public void prizeReceived(RaceResult result) {
        Map<String, Object> payload = racePayload(result.getRace());
        payload.put("prizeMoney", result.getPrizeMoney());
        publish(NotificationEventType.PRIZE_RECEIVED, "RACE_RESULT", result.getResultId(), null, payload);
    }

    @Override
    public void jockeyPayoutReleased(JockeyHorseContract contract) {
        publishContract(NotificationEventType.JOCKEY_PAYOUT_RELEASED, contract, null, String.valueOf(contract.getFinalPayoutAt()));
    }

    @Override
    public void roundTransitionBlocked(Round round) {
        Map<String, Object> payload = tournamentPayload(round.getTournament());
        payload.put("roundName", round.getRoundName());
        publish(NotificationEventType.ROUND_TRANSITION_BLOCKED, "ROUND", round.getRoundId(), null, payload);
    }

    @Override
    public void appealSubmitted(Appeal appeal) {
        publish(NotificationEventType.APPEAL_SUBMITTED, "APPEAL", appeal.getAppealId(), null, appealPayload(appeal));
    }

    @Override
    public void appealReviewed(Appeal appeal) {
        Map<String, Object> payload = appealPayload(appeal);
        payload.put("status", String.valueOf(appeal.getStatus()));
        payload.put("resolution", appeal.getResolution());
        publish(NotificationEventType.APPEAL_REVIEWED, "APPEAL", appeal.getAppealId(), null, payload);
    }

    private void publishRegistration(NotificationEventType type, String aggregateType, UUID registrationId,
                                     Tournament tournament, String reason) {
        Map<String, Object> payload = tournamentPayload(tournament);
        if (reason != null) {
            payload.put("reason", reason);
        }
        publish(type, aggregateType, registrationId, null, payload);
    }

    private void publishContract(NotificationEventType type, JockeyHorseContract contract, String reason, String suffix) {
        Map<String, Object> payload = tournamentPayload(contract.getTournament());
        payload.put("horseName", contract.getHorse().getName());
        payload.put("jockeyName", contract.getJockey().getUser().getFullName());
        if (reason != null) {
            payload.put("reason", reason);
        }
        publish(type, "CONTRACT", contract.getContractId(), suffix, payload);
    }

    private Map<String, Object> tournamentPayload(Tournament tournament) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("tournamentName", tournament.getName());
        return payload;
    }

    private Map<String, Object> racePayload(Race race) {
        Map<String, Object> payload = tournamentPayload(race.getRound().getTournament());
        payload.put("raceName", race.getName());
        return payload;
    }

    private Map<String, Object> entryPayload(RaceEntry entry) {
        Map<String, Object> payload = racePayload(entry.getRace());
        payload.put("horseName", entry.getContract().getHorse().getName());
        payload.put("jockeyName", entry.getContract().getJockey().getUser().getFullName());
        return payload;
    }

    private Map<String, Object> appealPayload(Appeal appeal) {
        Map<String, Object> payload = entryPayload(appeal.getEntry());
        payload.put("appealId", appeal.getAppealId());
        return payload;
    }

    private void publish(NotificationEventType type, String aggregateType, UUID aggregateId,
                         String deduplicationSuffix, Map<String, Object> payload) {
        String key = type.name() + ":" + aggregateId;
        if (deduplicationSuffix != null) {
            key += ":" + deduplicationSuffix;
        }
        eventPublisher.publish(NotificationEventCommand.builder()
                .eventType(type)
                .aggregateType(aggregateType)
                .aggregateId(aggregateId)
                .deduplicationKey(key)
                .payload(payload)
                .build());
    }
}

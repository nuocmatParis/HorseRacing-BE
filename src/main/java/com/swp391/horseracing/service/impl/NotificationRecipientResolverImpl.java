package com.swp391.horseracing.service.impl;

import com.swp391.horseracing.entity.*;
import com.swp391.horseracing.enums.AccountStatus;
import com.swp391.horseracing.enums.NotificationEventType;
import com.swp391.horseracing.enums.RoleName;
import com.swp391.horseracing.exception.AppException;
import com.swp391.horseracing.exception.ErrorCode;
import com.swp391.horseracing.repository.*;
import com.swp391.horseracing.service.NotificationRecipientResolver;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class NotificationRecipientResolverImpl implements NotificationRecipientResolver {
    UserRepository userRepository;
    HorseTournamentRegistrationRepository horseRegistrationRepository;
    JockeyTournamentRegistrationRepository jockeyRegistrationRepository;
    JockeyHorseContractRepository contractRepository;
    RaceRepository raceRepository;
    RaceEntryRepository raceEntryRepository;
    RaceRefereeRepository raceRefereeRepository;
    RaceInspectionStaffAssignmentRepository inspectionAssignmentRepository;
    PredictionRepository predictionRepository;
    RaceResultRepository raceResultRepository;
    AppealRepository appealRepository;

    @Override
    @Transactional(readOnly = true)
    public Set<UUID> resolve(NotificationEvent event) {
        Set<UUID> recipients = new LinkedHashSet<>();
        NotificationEventType type = event.getEventType();
        if (type == NotificationEventType.TOURNAMENT_PUBLISHED) {
            addActiveRoleUsers(recipients, Arrays.asList(RoleName.SPECTATOR, RoleName.HORSE_OWNER, RoleName.JOCKEY));
        } else if (type == NotificationEventType.SCHEDULE_PUBLISHED) {
            addTournamentScheduleRecipients(recipients, event.getAggregateId());
        } else if (isRegistrationEvent(type)) {
            addRegistrationRecipients(recipients, event);
        } else if (isContractEvent(type)) {
            addContractRecipients(recipients, event.getAggregateId(), type);
        } else if (isRaceEvent(type)) {
            addRaceRecipients(recipients, event.getAggregateId(), type);
        } else if (type == NotificationEventType.HORSE_INSPECTION_FAILED
                || type == NotificationEventType.JOCKEY_INSPECTION_FAILED) {
            addEntryParticipants(recipients, event.getAggregateId());
        } else if (type == NotificationEventType.ENTRY_SCRATCHED) {
            addEntryParticipants(recipients, event.getAggregateId());
            addEntryPredictors(recipients, event.getAggregateId());
        } else if (type == NotificationEventType.PREDICTED_ENTRY_SCRATCHED) {
            addEntryPredictors(recipients, event.getAggregateId());
        } else if (type == NotificationEventType.PREDICTION_SCORED
                || type == NotificationEventType.PREDICTION_VOIDED) {
            addPredictionRecipient(recipients, event.getAggregateId());
        } else if (type == NotificationEventType.PRIZE_RECEIVED) {
            addPrizeRecipients(recipients, event.getAggregateId());
        } else if (type == NotificationEventType.JOCKEY_PAYOUT_RELEASED) {
            JockeyHorseContract contract = findContract(event.getAggregateId());
            addIfActive(recipients, contract.getJockey().getUser());
        } else if (type == NotificationEventType.ROUND_TRANSITION_BLOCKED) {
            addActiveRoleUsers(recipients, Arrays.asList(RoleName.ADMIN));
        } else if (type == NotificationEventType.APPEAL_SUBMITTED
                || type == NotificationEventType.APPEAL_REVIEWED) {
            addAppealRecipients(recipients, event.getAggregateId(), type);
        }
        return recipients;
    }

    private boolean isRegistrationEvent(NotificationEventType type) {
        return type == NotificationEventType.REGISTRATION_APPROVED
                || type == NotificationEventType.REGISTRATION_REJECTED
                || type == NotificationEventType.REGISTRATION_WITHDRAWN;
    }

    private boolean isContractEvent(NotificationEventType type) {
        return type == NotificationEventType.CONTRACT_INVITED
                || type == NotificationEventType.CONTRACT_ACCEPTED
                || type == NotificationEventType.CONTRACT_REJECTED
                || type == NotificationEventType.CONTRACT_APPROVED
                || type == NotificationEventType.CONTRACT_CANCELLED;
    }

    private boolean isRaceEvent(NotificationEventType type) {
        return type == NotificationEventType.RACE_RESCHEDULED
                || type == NotificationEventType.RACE_CANCELLED
                || type == NotificationEventType.RACE_STARTED
                || type == NotificationEventType.RACE_RESULT_PUBLISHED;
    }

    private void addActiveRoleUsers(Set<UUID> recipients, List<RoleName> roles) {
        List<User> users = userRepository.findByStatusAndRole_RoleNameIn(AccountStatus.ACTIVE, roles);
        for (User user : users) {
            recipients.add(user.getUserId());
        }
    }

    private void addRegistrationRecipients(Set<UUID> recipients, NotificationEvent event) {
        if ("HORSE_REGISTRATION".equals(event.getAggregateType())) {
            HorseTournamentRegistration registration = horseRegistrationRepository.findById(event.getAggregateId())
                    .orElseThrow(() -> new AppException(ErrorCode.TOURNAMENT_REGISTRATION_NOT_FOUND));
            addIfActive(recipients, registration.getOwner().getUser());
        } else if ("JOCKEY_REGISTRATION".equals(event.getAggregateType())) {
            JockeyTournamentRegistration registration = jockeyRegistrationRepository.findById(event.getAggregateId())
                    .orElseThrow(() -> new AppException(ErrorCode.JOCKEY_TOURNAMENT_REGISTRATION_NOT_FOUND));
            addIfActive(recipients, registration.getJockey().getUser());
        } else {
            throw new AppException(ErrorCode.NOTIFICATION_EVENT_INVALID);
        }
        if (event.getEventType() == NotificationEventType.REGISTRATION_WITHDRAWN) {
            addActiveRoleUsers(recipients, List.of(RoleName.ADMIN));
        }
    }

    private void addContractRecipients(Set<UUID> recipients, UUID contractId, NotificationEventType type) {
        JockeyHorseContract contract = findContract(contractId);
        if (type != NotificationEventType.CONTRACT_INVITED) {
            addIfActive(recipients, contract.getOwner().getUser());
        }
        if (type != NotificationEventType.CONTRACT_ACCEPTED) {
            addIfActive(recipients, contract.getJockey().getUser());
        }
    }

    private JockeyHorseContract findContract(UUID contractId) {
        return contractRepository.findById(contractId)
                .orElseThrow(() -> new AppException(ErrorCode.CONTRACT_NOT_FOUND));
    }

    private void addTournamentScheduleRecipients(Set<UUID> recipients, UUID tournamentId) {
        addActiveRoleUsers(recipients, List.of(RoleName.SPECTATOR));
        List<Race> races = raceRepository.findByRound_Tournament_TournamentId(tournamentId);
        for (Race race : races) {
            addRaceParticipants(recipients, race.getRaceId());
            addRaceStaff(recipients, race);
        }
    }

    private void addRaceRecipients(Set<UUID> recipients, UUID raceId, NotificationEventType type) {
        Race race = raceRepository.findById(raceId)
                .orElseThrow(() -> new AppException(ErrorCode.RACE_NOT_FOUND));
        if (type != NotificationEventType.RACE_STARTED) {
            addRaceParticipants(recipients, raceId);
            if (type != NotificationEventType.RACE_RESULT_PUBLISHED) {
                addRaceStaff(recipients, race);
            }
        }
        addRacePredictors(recipients, raceId);
    }

    private void addRaceParticipants(Set<UUID> recipients, UUID raceId) {
        List<RaceEntry> entries = raceEntryRepository.findByRace_RaceIdOrderByLaneNumberAsc(raceId);
        for (RaceEntry entry : entries) {
            addIfActive(recipients, entry.getContract().getOwner().getUser());
            addIfActive(recipients, entry.getContract().getJockey().getUser());
        }
    }

    private void addRaceStaff(Set<UUID> recipients, Race race) {
        if (race.getRound().getHeadReferee() != null) {
            addIfActive(recipients, race.getRound().getHeadReferee().getUser());
        }
        List<RaceReferee> referees = raceRefereeRepository.findByRace_RaceId(race.getRaceId());
        for (RaceReferee referee : referees) {
            addIfActive(recipients, referee.getReferee().getUser());
        }
        java.util.Optional<RaceInspectionAssignment> assignment = inspectionAssignmentRepository
                .findByRace_RaceId(race.getRaceId());
        if (assignment.isPresent()) {
            addIfActive(recipients, assignment.get().getVeterinarian().getUser());
            addIfActive(recipients, assignment.get().getMedicalStaff().getUser());
        }
    }

    private void addRacePredictors(Set<UUID> recipients, UUID raceId) {
        List<Prediction> predictions = predictionRepository.findByRace_RaceId(raceId);
        for (Prediction prediction : predictions) {
            addIfActive(recipients, prediction.getSpectator().getUser());
        }
    }

    private void addEntryParticipants(Set<UUID> recipients, UUID entryId) {
        RaceEntry entry = raceEntryRepository.findById(entryId)
                .orElseThrow(() -> new AppException(ErrorCode.RACE_ENTRY_NOT_FOUND));
        addIfActive(recipients, entry.getContract().getOwner().getUser());
        addIfActive(recipients, entry.getContract().getJockey().getUser());
    }

    private void addEntryPredictors(Set<UUID> recipients, UUID entryId) {
        RaceEntry entry = raceEntryRepository.findById(entryId)
                .orElseThrow(() -> new AppException(ErrorCode.RACE_ENTRY_NOT_FOUND));
        List<Prediction> predictions = predictionRepository.findByRace_RaceId(entry.getRace().getRaceId());
        for (Prediction prediction : predictions) {
            boolean selected = false;
            for (PredictionDetail detail : prediction.getPredictionDetails()) {
                if (detail.getEntry().getEntryId().equals(entryId)) {
                    selected = true;
                    break;
                }
            }
            if (selected) {
                addIfActive(recipients, prediction.getSpectator().getUser());
            }
        }
    }

    private void addPredictionRecipient(Set<UUID> recipients, UUID predictionId) {
        Prediction prediction = predictionRepository.findById(predictionId)
                .orElseThrow(() -> new AppException(ErrorCode.PREDICTION_NOT_FOUND));
        addIfActive(recipients, prediction.getSpectator().getUser());
    }

    private void addPrizeRecipients(Set<UUID> recipients, UUID raceResultId) {
        RaceResult result = raceResultRepository.findById(raceResultId)
                .orElseThrow(() -> new AppException(ErrorCode.RACE_RESULT_NOT_FOUND));
        addIfActive(recipients, result.getEntry().getContract().getOwner().getUser());
        addIfActive(recipients, result.getEntry().getContract().getJockey().getUser());
    }

    private void addAppealRecipients(
            Set<UUID> recipients, UUID appealId, NotificationEventType type) {
        Appeal appeal = appealRepository.findById(appealId)
                .orElseThrow(() -> new AppException(ErrorCode.APPEAL_NOT_FOUND));
        if (type == NotificationEventType.APPEAL_SUBMITTED) {
            Referee headReferee = appeal.getEntry().getRace().getRound().getHeadReferee();
            if (headReferee != null) {
                addIfActive(recipients, headReferee.getUser());
            }
        } else {
            addIfActive(recipients, appeal.getSubmittedBy());
        }
    }

    private void addIfActive(Set<UUID> recipients, User user) {
        if (user != null && user.getStatus() == AccountStatus.ACTIVE) {
            recipients.add(user.getUserId());
        }
    }
}

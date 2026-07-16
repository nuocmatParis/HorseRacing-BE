package com.swp391.horseracing.simulation.realtime;

import com.swp391.horseracing.entity.RaceEntry;
import com.swp391.horseracing.entity.User;
import com.swp391.horseracing.exception.AppException;
import com.swp391.horseracing.exception.ErrorCode;
import com.swp391.horseracing.repository.RaceEntryRepository;
import com.swp391.horseracing.simulation.api.*;
import com.swp391.horseracing.simulation.domain.*;
import com.swp391.horseracing.simulation.persistence.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RaceIncidentService {
    private final SimulationAccessService accessService;
    private final RaceSimulationSessionRepository sessionRepository;
    private final RaceSimulationWarningRepository warningRepository;
    private final RaceSimulationFlagRepository flagRepository;
    private final RaceEntryRepository raceEntryRepository;
    private final RaceControlPublisher publisher;

    @Transactional(readOnly = true)
    public List<SimulationWarningResponse> warnings(UUID raceId) {
        accessService.requireAssignedReferee(raceId);
        return warningRepository.findByRace_RaceIdOrderByCreatedAtDesc(raceId).stream()
                .map(this::warningResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<SimulationFlagResponse> flags(UUID raceId) {
        accessService.requireAssignedReferee(raceId);
        return flagRepository.findByRace_RaceIdOrderByFlaggedAtDesc(raceId).stream()
                .map(flag -> flagResponse(flag, false)).toList();
    }

    @Transactional
    public SimulationWarningResponse ignore(UUID raceId, UUID warningId, IncidentReviewRequest request) {
        User reviewer = accessService.requireAssignedReferee(raceId).user();
        RaceSimulationWarning warning = requireWarning(raceId, warningId);
        requirePending(warning);
        warning.setReviewStatus(WarningReviewStatus.IGNORED);
        warning.setReviewedBy(reviewer);
        warning.setReviewedAt(LocalDateTime.now());
        warning.setReviewNote(request == null ? null : request.note());
        RaceSimulationWarning saved = warningRepository.save(warning);
        publisher.publishPrivate(saved.getRace(), incidentMessage(
                "WARNING_IGNORED", saved.getSession(), saved.getSequence(),
                saved.getRaceTimeSeconds(), warningResponse(saved)));
        return warningResponse(saved);
    }

    @Transactional
    public SimulationFlagResponse flagWarning(UUID raceId, UUID warningId, IncidentReviewRequest request) {
        User reviewer = accessService.requireAssignedReferee(raceId).user();
        RaceSimulationWarning warning = requireWarning(raceId, warningId);
        requirePending(warning);
        if (flagRepository.existsByWarning_WarningId(warningId)) {
            throw new AppException(ErrorCode.SIMULATION_WARNING_ALREADY_REVIEWED);
        }
        warning.setReviewStatus(WarningReviewStatus.FLAGGED);
        warning.setReviewedBy(reviewer);
        warning.setReviewedAt(LocalDateTime.now());
        warning.setReviewNote(request == null ? null : request.note());
        warningRepository.save(warning);
        RaceSimulationFlag flag = flagRepository.save(RaceSimulationFlag.builder()
                .session(warning.getSession())
                .race(warning.getRace())
                .warning(warning)
                .entry(warning.getEntry())
                .horseId(warning.getHorseId())
                .source(FlagSource.SYSTEM_WARNING)
                .status(FlagReviewStatus.PENDING_REVIEW)
                .severity(warning.getSeverity())
                .raceTimeSeconds(warning.getRaceTimeSeconds())
                .note(request != null && request.note() != null && !request.note().isBlank()
                        ? request.note().trim() : warning.getMessage())
                .flaggedBy(reviewer)
                .flaggedAt(LocalDateTime.now())
                .build());
        SimulationFlagResponse response = flagResponse(flag, false);
        publisher.publishPrivate(flag.getRace(), incidentMessage(
                "REFEREE_FLAGGED", flag.getSession(), warning.getSequence(),
                flag.getRaceTimeSeconds(), response));
        return response;
    }

    @Transactional
    public SimulationFlagResponse manualFlag(UUID raceId, ManualFlagRequest request) {
        User reviewer = accessService.requireAssignedReferee(raceId).user();
        RaceSimulationSession session = requireSession(raceId);
        RaceEntry entry = raceEntryRepository.findById(request.entryId())
                .filter(candidate -> candidate.getRace().getRaceId().equals(raceId))
                .orElseThrow(() -> new AppException(ErrorCode.SIMULATION_ENTRY_NOT_FOUND));
        RaceSimulationFlag flag = flagRepository.save(RaceSimulationFlag.builder()
                .session(session)
                .race(session.getRace())
                .entry(entry)
                .horseId(entry.getContract().getHorse().getHorseId())
                .source(FlagSource.MANUAL)
                .status(FlagReviewStatus.PENDING_REVIEW)
                .severity(request.severity())
                .raceTimeSeconds(Math.max(0.0, request.raceTimeSeconds()))
                .note(request.note().trim())
                .flaggedBy(reviewer)
                .flaggedAt(LocalDateTime.now())
                .build());
        SimulationFlagResponse response = flagResponse(flag, false);
        publisher.publishPrivate(flag.getRace(), incidentMessage(
                "REFEREE_FLAGGED", flag.getSession(), flag.getSession().getCurrentSequence(),
                flag.getRaceTimeSeconds(), response));
        return response;
    }

    @Transactional
    public SimulationFlagResponse reviewFlag(
            UUID raceId,
            UUID flagId,
            IncidentReviewRequest request,
            boolean confirm) {
        User reviewer = accessService.requireAssignedReferee(raceId).user();
        RaceSimulationFlag flag = flagRepository.findById(flagId)
                .filter(candidate -> candidate.getRace().getRaceId().equals(raceId))
                .orElseThrow(() -> new AppException(ErrorCode.SIMULATION_FLAG_NOT_FOUND));
        if (flag.getStatus() != FlagReviewStatus.PENDING_REVIEW) {
            throw new AppException(ErrorCode.SIMULATION_FLAG_ALREADY_REVIEWED);
        }
        if (flag.getSession().getStatus() != SimulationStatus.FINISHED) {
            throw new AppException(ErrorCode.SIMULATION_SESSION_NOT_READY);
        }
        flag.setStatus(confirm ? FlagReviewStatus.CONFIRMED : FlagReviewStatus.DISMISSED);
        flag.setReviewedBy(reviewer);
        flag.setReviewedAt(LocalDateTime.now());
        flag.setReviewNote(request == null ? null : request.note());
        return flagResponse(flagRepository.save(flag), confirm);
    }

    private RaceSimulationWarning requireWarning(UUID raceId, UUID warningId) {
        return warningRepository.findById(warningId)
                .filter(warning -> warning.getRace().getRaceId().equals(raceId))
                .orElseThrow(() -> new AppException(ErrorCode.SIMULATION_WARNING_NOT_FOUND));
    }

    private RaceSimulationSession requireSession(UUID raceId) {
        return sessionRepository.findByRace_RaceId(raceId)
                .orElseThrow(() -> new AppException(ErrorCode.SIMULATION_SESSION_NOT_FOUND));
    }

    private void requirePending(RaceSimulationWarning warning) {
        if (warning.getReviewStatus() != WarningReviewStatus.PENDING) {
            throw new AppException(ErrorCode.SIMULATION_WARNING_ALREADY_REVIEWED);
        }
    }

    private SimulationWarningResponse warningResponse(RaceSimulationWarning warning) {
        return new SimulationWarningResponse(
                warning.getWarningId(),
                warning.getEntry().getEntryId(),
                warning.getHorseId(),
                warning.getEntry().getContract().getHorse().getName(),
                warning.getWarningType(),
                warning.getSeverity(),
                warning.getRiskScore(),
                warning.getRaceTimeSeconds(),
                warning.getMessage(),
                warning.getSuggestedAction(),
                warning.getReviewStatus(),
                warning.getCreatedAt(),
                warning.getReviewedAt(),
                warning.getReviewNote());
    }

    private SimulationFlagResponse flagResponse(RaceSimulationFlag flag, boolean includeDraft) {
        ViolationDraftResponse draft = includeDraft ? new ViolationDraftResponse(
                flag.getEntry().getEntryId(),
                "OTHER",
                "Confirmed simulation flag: " + flag.getNote(),
                "WARNING") : null;
        return new SimulationFlagResponse(
                flag.getFlagId(),
                flag.getWarning() == null ? null : flag.getWarning().getWarningId(),
                flag.getEntry().getEntryId(),
                flag.getHorseId(),
                flag.getEntry().getContract().getHorse().getName(),
                flag.getSource(),
                flag.getStatus(),
                flag.getSeverity(),
                flag.getRaceTimeSeconds(),
                flag.getNote(),
                flag.getFlaggedBy().getFullName(),
                flag.getFlaggedAt(),
                flag.getReviewedBy() == null ? null : flag.getReviewedBy().getFullName(),
                flag.getReviewedAt(),
                flag.getReviewNote(),
                draft);
    }

    private LiveRaceMessage incidentMessage(
            String type,
            RaceSimulationSession session,
            long sequence,
            double raceTimeSeconds,
            Object payload) {
        return new LiveRaceMessage(
                type,
                session.getRace().getRaceId(),
                session.getSessionId(),
                sequence,
                raceTimeSeconds,
                LocalDateTime.now(),
                payload);
    }
}

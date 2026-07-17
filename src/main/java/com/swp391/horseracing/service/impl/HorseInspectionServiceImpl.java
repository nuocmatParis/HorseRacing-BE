package com.swp391.horseracing.service.impl;

import com.swp391.horseracing.dto.handicap.response.HandicapResult;
import com.swp391.horseracing.dto.horseinspection.request.HorseInspectionRequest;
import com.swp391.horseracing.dto.horseinspection.response.HorseInspectionResponse;
import com.swp391.horseracing.entity.*;
import com.swp391.horseracing.enums.*;
import com.swp391.horseracing.exception.AppException;
import com.swp391.horseracing.exception.ErrorCode;
import com.swp391.horseracing.mapper.HorseInspectionMapper;
import com.swp391.horseracing.repository.HorseInspectionRepository;
import com.swp391.horseracing.repository.RaceEntryRepository;
import com.swp391.horseracing.repository.RaceInspectionStaffAssignmentRepository;
import com.swp391.horseracing.repository.VeterinarianRepository;
import com.swp391.horseracing.service.HandicapService;
import com.swp391.horseracing.service.HorseInspectionService;
import com.swp391.horseracing.service.UserCurrentService;
import com.swp391.horseracing.service.PredictionService;
import com.swp391.horseracing.service.BusinessNotificationEventService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class HorseInspectionServiceImpl implements HorseInspectionService {

    RaceEntryRepository raceEntryRepository;
    HorseInspectionRepository horseInspectionRepository;
    VeterinarianRepository veterinarianRepository;
    RaceInspectionStaffAssignmentRepository raceInspectionStaffAssignmentRepository;
    UserCurrentService userCurrentService;
    HandicapService handicapService;
    HorseInspectionMapper horseInspectionMapper;
    PredictionService predictionService;
    BusinessNotificationEventService notificationEventService;

    @Override
    @Transactional(readOnly = true)
    public HorseInspectionResponse getInspection(UUID entryId) {
        RaceEntry raceEntry = raceEntryRepository.findById(entryId)
                .orElseThrow(() -> new AppException(ErrorCode.RACE_ENTRY_NOT_FOUND));
        User currentUser = userCurrentService.getCurrentUser();
        Veterinarian veterinarian = veterinarianRepository.findByUser_UserId(currentUser.getUserId())
                .orElseThrow(() -> new AppException(ErrorCode.VETERINARIAN_PROFILE_NOT_FOUND));
        RaceInspectionAssignment assignment = raceInspectionStaffAssignmentRepository
                .findByRace_RaceId(raceEntry.getRace().getRaceId())
                .orElseThrow(() -> new AppException(ErrorCode.VET_NOT_ASSIGNED_TO_RACE));
        if (!assignment.getVeterinarian().getVetId().equals(veterinarian.getVetId())) {
            throw new AppException(ErrorCode.VET_NOT_ASSIGNED_TO_RACE);
        }
        HorseInspection inspection = horseInspectionRepository.findByRaceEntry_EntryId(entryId)
                .orElseThrow(() -> new AppException(ErrorCode.HORSE_INSPECTION_NOT_FOUND));
        return horseInspectionMapper.toResponse(inspection);
    }

    @Override
    @Transactional
    public HorseInspectionResponse createInspection(UUID entryId, HorseInspectionRequest request) {
        RaceEntry raceEntry = raceEntryRepository.findById(entryId)
                .orElseThrow(() -> new AppException(ErrorCode.RACE_ENTRY_NOT_FOUND));

        Race race = raceEntry.getRace();
        if (race.getStatus() != RoundStatus.SCHEDULED) {
            throw new AppException(ErrorCode.RACE_NOT_IN_SCHEDULED_STATUS);
        }

        if (raceEntry.getStatus() != RaceEntryStatus.CONFIRMED) {
            throw new AppException(ErrorCode.RACE_ENTRY_NOT_ACTIVE);
        }

        if (race.getStartedAt() != null) {
            throw new AppException(ErrorCode.INSPECTION_WINDOW_CLOSED);
        }

        Tournament tournament = race.getRound().getTournament();
        int openMin = tournament.getInspectionOpenMinutesBefore();
        int closeMin = tournament.getInspectionCloseMinutesBefore();
        LocalDateTime inspectionOpenAt = race.getStartTime().minusMinutes(openMin);
        LocalDateTime inspectionCloseAt = race.getStartTime().minusMinutes(closeMin);
        LocalDateTime now = LocalDateTime.now();

        if (now.isBefore(inspectionOpenAt)) {
            throw new AppException(ErrorCode.INSPECTION_WINDOW_NOT_OPEN);
        }
        if (now.isAfter(inspectionCloseAt)) {
            throw new AppException(ErrorCode.INSPECTION_WINDOW_CLOSED);
        }

        if (horseInspectionRepository.existsByRaceEntry_EntryId(entryId)) {
            throw new AppException(ErrorCode.HORSE_INSPECTION_ALREADY_EXISTS);
        }

        User currentUser = userCurrentService.getCurrentUser();
        Veterinarian vet = veterinarianRepository.findByUser_UserId(currentUser.getUserId())
                .orElseThrow(() -> new AppException(ErrorCode.VETERINARIAN_PROFILE_NOT_FOUND));

        RaceInspectionAssignment assignment = raceInspectionStaffAssignmentRepository
                .findByRace_RaceId(race.getRaceId())
                .orElseThrow(() -> new AppException(ErrorCode.VET_NOT_ASSIGNED_TO_RACE));

        if (!assignment.getVeterinarian().getVetId().equals(vet.getVetId())) {
            throw new AppException(ErrorCode.VET_NOT_ASSIGNED_TO_RACE);
        }

        Horse horse = raceEntry.getContract().getHorse();
        boolean findingsRequireFailure = Boolean.TRUE.equals(request.getDopingDetected())
                || request.getActualBreed() != horse.getBreed();
        if (request.getResult() == InspectionResult.PASS && findingsRequireFailure) {
            throw new AppException(ErrorCode.HORSE_INSPECTION_FINDINGS_REQUIRE_FAILURE);
        }

        Float handicapWeight = null;
        boolean handicapConfirmed = Boolean.TRUE.equals(request.getHandicapConfirmed());
        LocalDateTime confirmedAt = null;

        if (tournament.isHandicapEnabled()) {
            if (request.getResult() == InspectionResult.PASS && !handicapConfirmed) {
                throw new AppException(ErrorCode.ENTRY_HANDICAP_NOT_CONFIRMED);
            }
            List<RaceEntry> entries = raceEntryRepository.findByRace_RaceIdOrderByLaneNumberAsc(race.getRaceId());
            int topRatingInRace = 0;
            for (RaceEntry entry : entries) {
                int currentRating = entry.getContract().getHorse().getCurrentRating();
                if (currentRating > topRatingInRace) {
                    topRatingInRace = currentRating;
                }
            }

            int horseRating = raceEntry.getContract().getHorse().getCurrentRating();
            double jockeyWeightKg = raceEntry.getContract().getJockey().getWeight();

            HandicapResult handicapResult = handicapService.calculateHandicap(
                    tournament, topRatingInRace, horseRating, jockeyWeightKg);

            handicapWeight = (float) handicapResult.getBallastKg();

            if (handicapConfirmed) {
                confirmedAt = LocalDateTime.now();
            }
        } else {
            handicapConfirmed = false;
        }

        HorseInspection inspection = HorseInspection.builder()
                .raceEntry(raceEntry)
                .veterinarian(vet)
                .result(request.getResult())
                .note(request.getNote())
                .inspectedAt(LocalDateTime.now())
                .handicapWeight(handicapWeight)
                .registeredWeight(horse.getWeight())
                .registeredBreed(horse.getBreed())
                .actualWeight(request.getActualWeight())
                .actualBreed(request.getActualBreed())
                .dopingDetected(request.getDopingDetected())
                .isHandicapConfirmed(handicapConfirmed)
                .confirmedAt(confirmedAt)
                .status(InspectionStatus.CONFIRMED)
                .build();

        if (request.getResult() == InspectionResult.FAIL) {
            raceEntry.setStatus(RaceEntryStatus.SCRATCHED);
            raceEntry.setScratchedReason("Failed horse inspection. Note: " + request.getNote());
            raceEntryRepository.save(raceEntry);
            predictionService.notifySpectatorsForScratchedEntry(
                    race.getRaceId(), entryId, raceEntry.getContract().getHorse().getName());
        }

        HorseInspection savedInspection = horseInspectionRepository.save(inspection);
        if (request.getResult() == InspectionResult.FAIL) {
            notificationEventService.horseInspectionFailed(raceEntry);
        }
        return horseInspectionMapper.toResponse(savedInspection);
    }
}

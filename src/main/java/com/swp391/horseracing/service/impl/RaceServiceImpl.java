package com.swp391.horseracing.service.impl;

import com.swp391.horseracing.dto.tournament.request.CreateRaceRequest;
import com.swp391.horseracing.dto.tournament.request.UpdateRaceRequest;
import com.swp391.horseracing.dto.tournament.response.RaceResponse;
import com.swp391.horseracing.entity.Race;
import com.swp391.horseracing.entity.Round;
import com.swp391.horseracing.entity.User;
import com.swp391.horseracing.entity.Referee;
import com.swp391.horseracing.entity.RaceEntry;
import com.swp391.horseracing.entity.HorseInspection;
import com.swp391.horseracing.entity.JockeyInspection;
import com.swp391.horseracing.enums.RoundStatus;
import com.swp391.horseracing.enums.TournamentStatus;
import com.swp391.horseracing.enums.RaceEntryStatus;
import com.swp391.horseracing.enums.InspectionStatus;
import com.swp391.horseracing.enums.InspectionResult;
import com.swp391.horseracing.exception.AppException;
import com.swp391.horseracing.exception.ErrorCode;
import com.swp391.horseracing.mapper.RaceMapper;
import com.swp391.horseracing.repository.RaceEntryRepository;
import com.swp391.horseracing.repository.RaceRefereeRepository;
import com.swp391.horseracing.repository.RaceRepository;
import com.swp391.horseracing.repository.RoundRepository;
import com.swp391.horseracing.repository.UserRepository;
import com.swp391.horseracing.repository.RefereeRepository;
import com.swp391.horseracing.repository.HorseInspectionRepository;
import com.swp391.horseracing.repository.JockeyInspectionRepository;
import com.swp391.horseracing.service.RaceService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequiredArgsConstructor
public class RaceServiceImpl implements RaceService {

    RaceRepository raceRepository;
    RoundRepository roundRepository;
    UserRepository userRepository;
    RaceEntryRepository raceEntryRepository;
    RaceRefereeRepository raceRefereeRepository;
    RaceMapper raceMapper;
    RefereeRepository refereeRepository;
    HorseInspectionRepository horseInspectionRepository;
    JockeyInspectionRepository jockeyInspectionRepository;

    @Override
    @Transactional
    public RaceResponse create(UUID roundId, CreateRaceRequest request) {
        if (request.getEndTime().isBefore(request.getStartTime())) {
            throw new AppException(ErrorCode.INVALID_RACE_DATES);
        }

        Round round = roundRepository.findById(roundId)
                .orElseThrow(() -> new AppException(ErrorCode.ROUND_NOT_FOUND));

        if (round.getTournament().getStatus() != TournamentStatus.DRAFT) {
            throw new AppException(ErrorCode.TOURNAMENT_NOT_IN_DRAFT);
        }

        if (request.getPredictionOpenAt().isAfter(request.getPredictionCloseAt())
                || request.getPredictionCloseAt().isAfter(request.getStartTime())) {
            throw new AppException(ErrorCode.INVALID_PREDICTION_TIMES);
        }

        if (request.getStartTime().isBefore(round.getStartDate())
                || request.getEndTime().isAfter(round.getEndDate())) {
            throw new AppException(ErrorCode.RACE_DATES_OUT_OF_ROUND);
        }

        if (raceRepository.existsByRound_RoundIdAndName(roundId, request.getName())) {
            throw new AppException(ErrorCode.RACE_NAME_ALREADY_EXISTS);
        }

        if (raceRepository.existsByRound_RoundIdAndSequenceOrder(roundId, request.getSequenceOrder())) {
            throw new AppException(ErrorCode.DUPLICATE_RACE_SEQUENCE);
        }

        if(round.getMaxRaces() <= round.getRaces().size()){
            throw new AppException(ErrorCode.MAX_RACES_REACHED);
        }

        List<Race> existingRaces = raceRepository.findByRound_RoundIdOrderByStartTimeDesc(roundId);
        if (!existingRaces.isEmpty()) {
            Race lastRace = existingRaces.get(0);
            if (request.getStartTime().isBefore(lastRace.getEndTime())) {
                throw new AppException(ErrorCode.RACE_DATES_OUT_OF_ROUND);
            }
        }

        User currentUser = getCurrentUser();

        Race race = raceMapper.toRace(request);
        race.setRound(round);
        race.setCreatedBy(currentUser);

        return raceMapper.toRaceResponse(raceRepository.save(race));
    }

    @Override
    @Transactional
    public RaceResponse update(UUID raceId, UpdateRaceRequest request) {
        Race race = raceRepository.findById(raceId)
                .orElseThrow(() -> new AppException(ErrorCode.RACE_NOT_FOUND));

        Round round = race.getRound();
        if (round.getTournament().getStatus() != TournamentStatus.DRAFT) {
            throw new AppException(ErrorCode.TOURNAMENT_NOT_IN_DRAFT);
        }

        if (request.getName() != null && !request.getName().equals(race.getName())
                && raceRepository.existsByRound_RoundIdAndName(round.getRoundId(), request.getName())) {
            throw new AppException(ErrorCode.RACE_NAME_ALREADY_EXISTS);
        }

        if (request.getSequenceOrder() != null && request.getSequenceOrder() != race.getSequenceOrder()
                && raceRepository.existsByRound_RoundIdAndSequenceOrder(round.getRoundId(), request.getSequenceOrder())) {
            throw new AppException(ErrorCode.DUPLICATE_RACE_SEQUENCE);
        }

        if (request.getStartTime() != null && request.getEndTime() != null
                && request.getEndTime().isBefore(request.getStartTime())) {
            throw new AppException(ErrorCode.INVALID_RACE_DATES);
        }

        if (request.getPredictionOpenAt() != null && request.getPredictionCloseAt() != null
                && request.getPredictionOpenAt().isAfter(request.getPredictionCloseAt())) {
            throw new AppException(ErrorCode.INVALID_PREDICTION_TIMES);
        }

        LocalDateTime startTime = request.getStartTime() != null ? request.getStartTime() : race.getStartTime();
        LocalDateTime endTime = request.getEndTime() != null ? request.getEndTime() : race.getEndTime();
        if (startTime.isBefore(round.getStartDate()) || endTime.isAfter(round.getEndDate())) {
            throw new AppException(ErrorCode.RACE_DATES_OUT_OF_ROUND);
        }

        Integer oldSequence = race.getSequenceOrder();
        Integer newSequence = request.getSequenceOrder();

        raceMapper.updateRace(request, race);

        if (newSequence != null && !newSequence.equals(oldSequence)) {
            reorderRaces(round.getRoundId(), race.getRaceId(), newSequence);
        }

        return raceMapper.toRaceResponse(raceRepository.save(race));
    }

    @Override
    @Transactional
    public void delete(UUID raceId) {
        Race race = raceRepository.findById(raceId)
                .orElseThrow(() -> new AppException(ErrorCode.RACE_NOT_FOUND));

        if (race.getRound().getTournament().getStatus() != TournamentStatus.DRAFT) {
            throw new AppException(ErrorCode.TOURNAMENT_NOT_IN_DRAFT);
        }

        raceRepository.delete(race);
    }

    @Override
    @Transactional
    public RaceResponse publishSchedule(UUID raceId) {
        Race race = raceRepository.findById(raceId)
                .orElseThrow(() -> new AppException(ErrorCode.RACE_NOT_FOUND));

        if (race.getStatus() != RoundStatus.SCHEDULING) {
            throw new AppException(ErrorCode.RACE_NOT_IN_SCHEDULING);
        }
        if (race.getSchedulePublishedAt() != null) {
            throw new AppException(ErrorCode.RACE_ALREADY_PUBLISHED);
        }

        int entryCount = raceEntryRepository.countByRace_RaceId(raceId);
        if (entryCount < race.getRound().getMinEntries()) {
            throw new AppException(ErrorCode.RACE_NOT_ENOUGH_ENTRIES);
        }

        int refereeCount = raceRefereeRepository.countByRace_RaceId(raceId);
        if (refereeCount < 1) {
            throw new AppException(ErrorCode.RACE_MISSING_REFEREES);
        }

        race.setStatus(RoundStatus.SCHEDULED);
        race.setSchedulePublishedAt(LocalDateTime.now());

        return raceMapper.toRaceResponse(raceRepository.save(race));
    }

    private void reorderRaces(UUID roundId, UUID raceId, int newSequence) {
        List<Race> otherRaces = raceRepository.findByRound_RoundIdAndRaceIdNotOrderBySequenceOrderAsc(roundId, raceId);
        int seq = 1;
        for (Race r : otherRaces) {
            if (seq == newSequence) {
                seq++;
            }
            r.setSequenceOrder(seq);
            seq++;
        }
    }

    @Override
    public List<RaceResponse> getRacesByRoundId(UUID roundId) {
        return raceRepository.findByRound_RoundId(roundId)
                .stream()
                .map(raceMapper::toRaceResponse)
                .toList();
    }

    @Override
    @Transactional
    public RaceResponse startRace(UUID raceId) {
        Race race = raceRepository.findById(raceId)
                .orElseThrow(() -> new AppException(ErrorCode.RACE_NOT_FOUND));

        if (race.getStatus() != RoundStatus.SCHEDULED) {
            throw new AppException(ErrorCode.RACE_NOT_IN_SCHEDULED_STATUS);
        }

        User currentUser = getCurrentUser();
        Referee referee = refereeRepository.findByUser_UserId(currentUser.getUserId())
                .orElseThrow(() -> new AppException(ErrorCode.REFEREE_PROFILE_NOT_FOUND));

        boolean isAuthorized = false;
        if (race.getRound().getHeadReferee() != null 
                && race.getRound().getHeadReferee().getRefereeId().equals(referee.getRefereeId())) {
            isAuthorized = true;
        }
        if (!isAuthorized) {
            isAuthorized = raceRefereeRepository.existsByRace_RaceIdAndReferee_RefereeId(
                    race.getRaceId(), referee.getRefereeId());
        }
        if (!isAuthorized) {
            throw new AppException(ErrorCode.REFEREE_NOT_ASSIGNED_TO_RACE);
        }

        List<RaceEntry> entries = raceEntryRepository.findByRace_RaceIdOrderByLaneNumberAsc(raceId);
        int activeEntryCount = 0;
        for (RaceEntry entry : entries) {
            if (entry.getStatus() == RaceEntryStatus.WITHDRAWN_BEFORE_SCHEDULE
                    || entry.getStatus() == RaceEntryStatus.WITHDRAWN_AFTER_SCHEDULE
                    || entry.getStatus() == RaceEntryStatus.SCRATCHED
                    || entry.getStatus() == RaceEntryStatus.DISQUALIFIED) {
                continue;
            }

            HorseInspection horseInspection = horseInspectionRepository.findByRaceEntry_EntryId(entry.getEntryId())
                    .orElseThrow(() -> new AppException(ErrorCode.ENTRY_MISSING_HORSE_INSPECTION));

            if (horseInspection.getStatus() != InspectionStatus.CONFIRMED 
                    || horseInspection.getResult() != InspectionResult.PASS) {
                throw new AppException(ErrorCode.ENTRY_MISSING_HORSE_INSPECTION);
            }

            JockeyInspection jockeyInspection = jockeyInspectionRepository.findByRaceEntry_EntryId(entry.getEntryId())
                    .orElseThrow(() -> new AppException(ErrorCode.ENTRY_MISSING_JOCKEY_INSPECTION));

            if (jockeyInspection.getStatus() != InspectionStatus.CONFIRMED 
                    || jockeyInspection.getResult() != InspectionResult.PASS) {
                throw new AppException(ErrorCode.ENTRY_MISSING_JOCKEY_INSPECTION);
            }

            if (horseInspection.getHandicapWeight() != null && horseInspection.getHandicapWeight() > 0) {
                if (!Boolean.TRUE.equals(horseInspection.getIsHandicapConfirmed())) {
                    throw new AppException(ErrorCode.ENTRY_HANDICAP_NOT_CONFIRMED);
                }
            }

            activeEntryCount++;
        }

        if (activeEntryCount < race.getRound().getMinEntries()) {
            throw new AppException(ErrorCode.RACE_NOT_ENOUGH_ACTIVE_ENTRIES);
        }

        race.setStatus(RoundStatus.ONGOING);
        race.setStartedAt(LocalDateTime.now());
        race.setStartedBy(currentUser);

        return raceMapper.toRaceResponse(raceRepository.save(race));
    }

    private User getCurrentUser() {
        var context = SecurityContextHolder.getContext();
        String username = context.getAuthentication().getName();
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
    }
}

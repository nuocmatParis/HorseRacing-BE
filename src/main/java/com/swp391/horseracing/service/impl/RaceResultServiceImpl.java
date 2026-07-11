package com.swp391.horseracing.service.impl;

import com.swp391.horseracing.dto.race_result.request.CreateRaceResultRequest;
import com.swp391.horseracing.dto.race_result.request.UpdateRaceResultRequest;
import com.swp391.horseracing.dto.race_result.response.RaceResultResponse;
import com.swp391.horseracing.entity.*;
import com.swp391.horseracing.enums.RefereeStatus;
import com.swp391.horseracing.enums.ReportStatus;
import com.swp391.horseracing.enums.RoundStatus;
import com.swp391.horseracing.exception.AppException;
import com.swp391.horseracing.exception.ErrorCode;
import com.swp391.horseracing.mapper.RaceResultMapper;
import com.swp391.horseracing.repository.*;
import com.swp391.horseracing.service.RaceResultService;
import com.swp391.horseracing.service.UserCurrentService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequiredArgsConstructor
public class RaceResultServiceImpl implements RaceResultService {

    RaceResultRepository raceResultRepository;
    RaceRepository raceRepository;
    RaceEntryRepository raceEntryRepository;
    RaceReportRepository raceReportRepository;
    RefereeRepository refereeRepository;
    RaceRefereeRepository raceRefereeRepository;
    RaceResultMapper raceResultMapper;
    UserCurrentService userCurrentService;

    @Override
    @Transactional
    public List<RaceResultResponse> createResults(UUID raceId, List<CreateRaceResultRequest> requests) {
        Race race = raceRepository.findById(raceId)
                .orElseThrow(() -> new AppException(ErrorCode.RACE_NOT_FOUND));

        if (race.getStatus() != RoundStatus.ONGOING && race.getStatus() != RoundStatus.FINISHED) {
            throw new AppException(ErrorCode.INVALID_RACE_RESULT_STATUS);
        }

        if (raceReportRepository.existsByRace_RaceId(raceId)) {
            RaceReport report = raceReportRepository.findByRace_RaceId(raceId).get();
            if (report.getStatus() == ReportStatus.Published) {
                throw new AppException(ErrorCode.RACE_REPORT_ALREADY_PUBLISHED);
            }
        }

        Referee referee = validateAndGetReferee(raceId);

        if (requests == null || requests.isEmpty()) {
            throw new AppException(ErrorCode.INVALID_REQUEST);
        }

        Set<Integer> ranks = new HashSet<>();
        Set<UUID> entryIds = new HashSet<>();

        List<RaceResult> results = new ArrayList<>();

        for (CreateRaceResultRequest req : requests) {
            if (!entryIds.add(req.getEntryId())) {
                throw new AppException(ErrorCode.INVALID_REQUEST);
            }

            if (!ranks.add(req.getRank())) {
                throw new AppException(ErrorCode.DUPLICATE_RACE_RESULT_RANK);
            }

            if (raceResultRepository.existsByRace_RaceIdAndRank(raceId, req.getRank())) {
                throw new AppException(ErrorCode.DUPLICATE_RACE_RESULT_RANK);
            }

            if (raceResultRepository.existsByRace_RaceIdAndEntry_EntryId(raceId, req.getEntryId())) {
                throw new AppException(ErrorCode.RACE_RESULT_ALREADY_EXISTS);
            }

            RaceEntry entry = raceEntryRepository.findById(req.getEntryId())
                    .orElseThrow(() -> new AppException(ErrorCode.RACE_ENTRY_NOT_FOUND));

            if (!entry.getRace().getRaceId().equals(raceId)) {
                throw new AppException(ErrorCode.INVALID_REQUEST);
            }

            RaceResult result = RaceResult.builder()
                    .race(race)
                    .entry(entry)
                    .finishTime(req.getFinishTime())
                    .rank(req.getRank())
                    .status(req.getStatus())
                    .recordedBy(referee.getUser())
                    .build();

            results.add(result);
        }

        return raceResultRepository.saveAll(results)
                .stream()
                .map(raceResultMapper::toRaceResultResponse)
                .toList();
    }

    @Override
    @Transactional
    public List<RaceResultResponse> updateResults(UUID raceId, List<UpdateRaceResultRequest> requests) {
        Race race = raceRepository.findById(raceId)
                .orElseThrow(() -> new AppException(ErrorCode.RACE_NOT_FOUND));

        if (race.getStatus() != RoundStatus.ONGOING && race.getStatus() != RoundStatus.FINISHED) {
            throw new AppException(ErrorCode.INVALID_RACE_RESULT_STATUS);
        }

        if (raceReportRepository.existsByRace_RaceId(raceId)) {
            RaceReport report = raceReportRepository.findByRace_RaceId(raceId).get();
            if (report.getStatus() == ReportStatus.Published) {
                throw new AppException(ErrorCode.RACE_REPORT_ALREADY_PUBLISHED);
            }
        }

        Referee referee = validateAndGetReferee(raceId);

        List<RaceResult> existingResults = raceResultRepository.findByRace_RaceId(raceId);

        if (existingResults.size() != requests.size()) {
            throw new AppException(ErrorCode.INVALID_REQUEST);
        }

        Map<UUID, RaceResult> resultByEntryId = new HashMap<>();
        for (RaceResult r : existingResults) {
            resultByEntryId.put(r.getEntry().getEntryId(), r);
        }

        Set<Integer> ranks = new HashSet<>();

        for (UpdateRaceResultRequest req : requests) {
            if (req.getRank() != null && !ranks.add(req.getRank())) {
                throw new AppException(ErrorCode.DUPLICATE_RACE_RESULT_RANK);
            }
        }

        for (UpdateRaceResultRequest req : requests) {
            RaceResult result = resultByEntryId.get(req.getEntryId());
            if (result == null) {
                throw new AppException(ErrorCode.RACE_RESULT_NOT_FOUND);
            }

            if (req.getFinishTime() != null) {
                result.setFinishTime(req.getFinishTime());
            }
            if (req.getRank() != null) {
                result.setRank(req.getRank());
            }
            if (req.getStatus() != null) {
                result.setStatus(req.getStatus());
            }
        }

        return raceResultRepository.saveAll(existingResults)
                .stream()
                .map(raceResultMapper::toRaceResultResponse)
                .toList();
    }

    @Override
    public List<RaceResultResponse> getResultsByRaceId(UUID raceId) {
        if (!raceRepository.existsById(raceId)) {
            throw new AppException(ErrorCode.RACE_NOT_FOUND);
        }
        return raceResultRepository.findByRace_RaceIdOrderByRankAsc(raceId)
                .stream()
                .map(raceResultMapper::toRaceResultResponse)
                .toList();
    }

    @Override
    public List<RaceResultResponse> getRefereeResultsByRaceId(UUID raceId) {
        Race race = raceRepository.findById(raceId)
                .orElseThrow(() -> new AppException(ErrorCode.RACE_NOT_FOUND));

        Referee referee = validateAndGetReferee(raceId);

        return raceResultRepository.findByRace_RaceIdOrderByRankAsc(raceId)
                .stream()
                .map(raceResultMapper::toRaceResultResponse)
                .toList();
    }

    private Referee validateAndGetReferee(UUID raceId) {
        User currentUser = userCurrentService.getCurrentUser();

        Referee referee = refereeRepository.findByUser_UserId(currentUser.getUserId())
                .orElseThrow(() -> new AppException(ErrorCode.REFEREE_PROFILE_NOT_FOUND));

        if (referee.getStatus() == RefereeStatus.SUSPENDED) {
            throw new AppException(ErrorCode.ACCESS_DENIED);
        }

        if (!raceRefereeRepository.existsByRace_RaceIdAndReferee_RefereeId(raceId, referee.getRefereeId())) {
            throw new AppException(ErrorCode.RACE_REFEREE_NOT_FOUND);
        }

        return referee;
    }
}

package com.swp391.horseracing.service.impl;

import com.swp391.horseracing.dto.race_result.request.CreateRaceResultRequest;
import com.swp391.horseracing.dto.race_result.request.UpdateRaceResultRequest;
import com.swp391.horseracing.dto.race_result.response.RaceResultResponse;
import com.swp391.horseracing.entity.*;
import com.swp391.horseracing.enums.*;
import com.swp391.horseracing.exception.AppException;
import com.swp391.horseracing.exception.ErrorCode;
import com.swp391.horseracing.mapper.RaceResultMapper;
import com.swp391.horseracing.repository.*;
import com.swp391.horseracing.service.RaceResultService;
import com.swp391.horseracing.service.HorseRatingService;
import com.swp391.horseracing.service.UserCurrentService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

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
    HorseRatingService horseRatingService;

    @Override
    @Transactional
    public List<RaceResultResponse> createResults(UUID raceId, List<CreateRaceResultRequest> requests) {
        Race race = requireRace(raceId);

        if (!isResultEditableRaceStatus(race.getStatus())) {
            throw new AppException(ErrorCode.INVALID_RACE_RESULT_STATUS);
        }

        Optional<RaceReport> reportOpt = raceReportRepository.findByRace_RaceId(raceId);
        if (reportOpt.isPresent()) {
            ReportStatus status = reportOpt.get().getStatus();
            if (status == ReportStatus.PUBLISHED) {
                throw new AppException(ErrorCode.RACE_REPORT_ALREADY_PUBLISHED);
            }
            if (status == ReportStatus.SIGNED || status == ReportStatus.SUBMITTED_TO_HEAD) {
                throw new AppException(ErrorCode.RACE_REPORT_ALREADY_SIGNED);
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

            Optional<RaceEntry> entryOptional = raceEntryRepository.findById(req.getEntryId());
            if (entryOptional.isEmpty()) {
                throw new AppException(ErrorCode.RACE_ENTRY_NOT_FOUND);
            }
            RaceEntry entry = entryOptional.get();

            if (!entry.getRace().getRaceId().equals(raceId)) {
                throw new AppException(ErrorCode.INVALID_REQUEST);
            }

            if (!isActualStarter(entry)) {
                throw new AppException(ErrorCode.RACE_ENTRY_DID_NOT_START);
            }

            if (raceResultRepository.existsByRace_RaceIdAndEntry_EntryId(raceId, req.getEntryId())) {
                throw new AppException(ErrorCode.RACE_RESULT_ALREADY_EXISTS);
            }

            Float finishTime = req.getFinishTime();
            Integer rank = req.getRank();
            RaceResultStatus status = req.getStatus();

            if (status == RaceResultStatus.FINISHED) {
                if (finishTime == null || rank == null) {
                    throw new AppException(ErrorCode.INVALID_REQUEST);
                }
                if (finishTime < 0) {
                    throw new AppException(ErrorCode.FINISH_TIME_MUST_BE_POSITIVE);
                }
                if (rank < 1) {
                    throw new AppException(ErrorCode.RANK_MUST_BE_POSITIVE);
                }
                if (!ranks.add(rank)) {
                    throw new AppException(ErrorCode.DUPLICATE_RACE_RESULT_RANK);
                }
                if (raceResultRepository.existsByRace_RaceIdAndRank(raceId, rank)) {
                    throw new AppException(ErrorCode.DUPLICATE_RACE_RESULT_RANK);
                }
                
                entry.setStatus(RaceEntryStatus.FINISHED);
            } else if (status == RaceResultStatus.DISQUALIFIED) {
                finishTime = null;
                rank = null;
                entry.setStatus(RaceEntryStatus.DISQUALIFIED);
            } else {
                throw new AppException(ErrorCode.INVALID_RACE_RESULT_STATUS);
            }

            horseRatingService.validateRatingChange(
                    race, status, rank, req.getRatingChange());
            
            raceEntryRepository.save(entry);

            RaceResult result = RaceResult.builder()
                    .race(race)
                    .entry(entry)
                    .finishTime(finishTime)
                    .rank(rank)
                    .status(status)
                    .ratingChange(req.getRatingChange())
                    .ratingAdjustmentReason(null)
                    .recordedBy(referee.getUser())
                    .build();

            results.add(result);
        }

        return mapResults(raceResultRepository.saveAll(results));
    }

    @Override
    @Transactional
    public List<RaceResultResponse> updateResults(UUID raceId, List<UpdateRaceResultRequest> requests) {
        Race race = requireRace(raceId);

        if (!isResultEditableRaceStatus(race.getStatus())) {
            throw new AppException(ErrorCode.INVALID_RACE_RESULT_STATUS);
        }

        Optional<RaceReport> reportOpt = raceReportRepository.findByRace_RaceId(raceId);
        if (reportOpt.isPresent()) {
            ReportStatus status = reportOpt.get().getStatus();
            if (status == ReportStatus.PUBLISHED) {
                throw new AppException(ErrorCode.RACE_REPORT_ALREADY_PUBLISHED);
            }
            if (status == ReportStatus.SIGNED || status == ReportStatus.SUBMITTED_TO_HEAD) {
                throw new AppException(ErrorCode.RACE_REPORT_ALREADY_SIGNED);
            }
        }

        validateAndGetReferee(raceId);
        return applyResultUpdates(race, requests, false);
    }

    @Override
    public List<RaceResultResponse> getResultsByRaceId(UUID raceId) {
        if (!raceRepository.existsById(raceId)) {
            throw new AppException(ErrorCode.RACE_NOT_FOUND);
        }
        return mapResults(raceResultRepository.findByRace_RaceIdOrderByRankAsc(raceId));
    }

    @Override
    @Transactional
    public List<RaceResultResponse> finishRaceWithRandomResults(UUID raceId) {
        Optional<Race> raceOptional = raceRepository.findForUpdateByRaceId(raceId);
        if (raceOptional.isEmpty()) {
            raceOptional = raceRepository.findById(raceId);
        }
        if (raceOptional.isEmpty()) {
            throw new AppException(ErrorCode.RACE_NOT_FOUND);
        }
        Race race = raceOptional.get();

        if (race.getStatus() != RoundStatus.ONGOING) {
            throw new AppException(ErrorCode.INVALID_RACE_RESULT_STATUS);
        }

        Referee referee = validateAndGetReferee(raceId);
        List<RaceEntry> entries = raceEntryRepository.findByRace_RaceIdOrderByLaneNumberAsc(raceId);
        List<RaceResult> existingResults = raceResultRepository.findByRace_RaceId(raceId);
        Map<UUID, RaceResult> existingByEntryId = new HashMap<>();
        Set<Integer> usedRanks = new HashSet<>();
        for (RaceResult existingResult : existingResults) {
            existingByEntryId.put(existingResult.getEntry().getEntryId(), existingResult);
            if (existingResult.getStatus() == RaceResultStatus.FINISHED
                    && existingResult.getRank() != null) {
                usedRanks.add(existingResult.getRank());
            }
        }

        List<RaceEntry> finishers = new ArrayList<>();
        boolean hasDisqualifiedEntryWithoutResult = false;
        for (RaceEntry entry : entries) {
            if (!isActualStarter(entry)) {
                continue;
            }
            if (existingByEntryId.containsKey(entry.getEntryId())) {
                continue;
            }
            if (entry.getStatus() == RaceEntryStatus.DISQUALIFIED) {
                hasDisqualifiedEntryWithoutResult = true;
            } else {
                finishers.add(entry);
            }
        }
        if (finishers.isEmpty() && existingResults.isEmpty() && !hasDisqualifiedEntryWithoutResult) {
            throw new AppException(ErrorCode.RACE_NOT_ENOUGH_ACTIVE_ENTRIES);
        }

        double distanceMeters = race.getDistance().getMeters();
        List<GeneratedTiming> generatedTimings = new ArrayList<>();
        for (RaceEntry entry : finishers) {
            double speedMetersPerSecond = ThreadLocalRandom.current().nextDouble(15.0, 18.0);
            double generatedTime = distanceMeters / speedMetersPerSecond;
            generatedTimings.add(new GeneratedTiming(entry, generatedTime));
        }
        generatedTimings.sort(Comparator.comparingDouble(GeneratedTiming::finishTime));

        float previousFinishTime = 0f;
        int rank = 1;
        List<RaceResult> newResults = new ArrayList<>();

        for (GeneratedTiming generatedTiming : generatedTimings) {
            float roundedTime = roundToHundredth(generatedTiming.finishTime());
            if (roundedTime <= previousFinishTime) {
                roundedTime = roundToHundredth(previousFinishTime + 0.01d);
            }
            while (usedRanks.contains(rank)) {
                rank++;
            }
            RaceEntry entry = generatedTiming.entry();
            entry.setStatus(RaceEntryStatus.FINISHED);
            newResults.add(RaceResult.builder()
                    .race(race)
                    .entry(entry)
                    .finishTime(roundedTime)
                    .rank(rank)
                    .status(RaceResultStatus.FINISHED)
                    .recordedBy(referee.getUser())
                    .build());
            previousFinishTime = roundedTime;
            rank++;
        }

        for (RaceEntry entry : entries) {
            if (entry.getStatus() == RaceEntryStatus.DISQUALIFIED
                    && !existingByEntryId.containsKey(entry.getEntryId())) {
                newResults.add(RaceResult.builder()
                        .race(race)
                        .entry(entry)
                        .finishTime(null)
                        .rank(null)
                        .status(RaceResultStatus.DISQUALIFIED)
                        .recordedBy(referee.getUser())
                        .build());
            }
        }

        raceEntryRepository.saveAll(entries);
        race.setStatus(RoundStatus.FINISHED);
        race.setFinishedAt(java.time.LocalDateTime.now());
        raceRepository.save(race);

        List<RaceResult> allResults = new ArrayList<>(existingResults);
        if (!newResults.isEmpty()) {
            allResults.addAll(raceResultRepository.saveAll(newResults));
        }
        allResults.sort(Comparator.comparing(
                RaceResult::getRank,
                Comparator.nullsLast(Comparator.naturalOrder())));
        List<RaceResultResponse> responses = new ArrayList<>();
        for (RaceResult result : allResults) {
            responses.add(raceResultMapper.toRaceResultResponse(result));
        }
        return responses;
    }

    @Override
    public List<RaceResultResponse> getRefereeResultsByRaceId(UUID raceId) {
        Race race = requireRace(raceId);

        Referee referee = validateAndGetReferee(raceId);

        return mapResults(raceResultRepository.findByRace_RaceIdOrderByRankAsc(raceId));
    }

    @Override
    public List<RaceResultResponse> getHeadRefereeResultsByRaceId(UUID raceId) {
        Race race = requireRace(raceId);
        validateAndGetHeadReferee(race);
        return mapResults(raceResultRepository.findByRace_RaceIdOrderByRankAsc(raceId));
    }

    @Override
    @Transactional
    public List<RaceResultResponse> updateHeadRefereeResults(
            UUID raceId, List<UpdateRaceResultRequest> requests) {
        Race race = requireRace(raceId);
        validateAndGetHeadReferee(race);
        Optional<RaceReport> reportOptional = raceReportRepository.findForUpdateByRace_RaceId(raceId);
        if (reportOptional.isEmpty()) {
            throw new AppException(ErrorCode.RACE_REPORT_NOT_FOUND);
        }
        RaceReport report = reportOptional.get();
        if (report.getStatus() != ReportStatus.SUBMITTED_TO_HEAD) {
            throw new AppException(ErrorCode.RACE_REPORT_NOT_SUBMITTED);
        }
        return applyResultUpdates(race, requests, true);
    }

    private List<RaceResultResponse> applyResultUpdates(
            Race race, List<UpdateRaceResultRequest> requests, boolean updatedByHeadReferee) {
        if (requests == null || requests.isEmpty()) {
            throw new AppException(ErrorCode.INVALID_REQUEST);
        }

        List<RaceResult> existingResults = raceResultRepository.findForUpdateByRace_RaceId(race.getRaceId());
        if (existingResults.size() != requests.size()) {
            throw new AppException(ErrorCode.INVALID_REQUEST);
        }

        Map<UUID, RaceResult> resultsByEntryId = new HashMap<>();
        for (RaceResult result : existingResults) {
            resultsByEntryId.put(result.getEntry().getEntryId(), result);
        }

        Set<Integer> ranks = new HashSet<>();
        for (UpdateRaceResultRequest request : requests) {
            RaceResult existing = resultsByEntryId.get(request.getEntryId());
            if (existing == null) {
                throw new AppException(ErrorCode.RACE_RESULT_NOT_FOUND);
            }
            RaceResultStatus status = request.getStatus() == null
                    ? existing.getStatus() : request.getStatus();
            Integer rank = request.getRank() == null ? existing.getRank() : request.getRank();
            Float finishTime = request.getFinishTime() == null
                    ? existing.getFinishTime() : request.getFinishTime();
            if (status == RaceResultStatus.FINISHED) {
                if (rank == null || finishTime == null) {
                    throw new AppException(ErrorCode.INVALID_REQUEST);
                }
                if (rank < 1) {
                    throw new AppException(ErrorCode.RANK_MUST_BE_POSITIVE);
                }
                if (finishTime < 0) {
                    throw new AppException(ErrorCode.FINISH_TIME_MUST_BE_POSITIVE);
                }
                if (!ranks.add(rank)) {
                    throw new AppException(ErrorCode.DUPLICATE_RACE_RESULT_RANK);
                }
            }
        }

        for (UpdateRaceResultRequest request : requests) {
            RaceResult result = resultsByEntryId.get(request.getEntryId());
            RaceResultStatus status = request.getStatus() == null
                    ? result.getStatus() : request.getStatus();
            Float finishTime = request.getFinishTime() == null
                    ? result.getFinishTime() : request.getFinishTime();
            Integer rank = request.getRank() == null ? result.getRank() : request.getRank();
            Integer ratingChange = request.getRatingChange() == null
                    ? result.getRatingChange() : request.getRatingChange();

            RaceEntry entry = result.getEntry();
            if (!isActualStarter(entry)) {
                throw new AppException(ErrorCode.RACE_ENTRY_DID_NOT_START);
            }

            if (status == RaceResultStatus.FINISHED) {
                result.setFinishTime(finishTime);
                result.setRank(rank);
                entry.setStatus(RaceEntryStatus.FINISHED);
            } else if (status == RaceResultStatus.DISQUALIFIED) {
                result.setFinishTime(null);
                result.setRank(null);
                rank = null;
                entry.setStatus(RaceEntryStatus.DISQUALIFIED);
            } else {
                throw new AppException(ErrorCode.INVALID_RACE_RESULT_STATUS);
            }

            horseRatingService.validateRatingChange(race, status, rank, ratingChange);
            if (updatedByHeadReferee && !Objects.equals(result.getRatingChange(), ratingChange)) {
                String reason = request.getRatingAdjustmentReason();
                if (reason == null || reason.isBlank()) {
                    throw new AppException(ErrorCode.HORSE_RATING_ADJUSTMENT_REASON_REQUIRED);
                }
                result.setRatingAdjustmentReason(reason.trim());
            } else if (!updatedByHeadReferee) {
                result.setRatingAdjustmentReason(null);
            }

            result.setStatus(status);
            result.setRatingChange(ratingChange);
            raceEntryRepository.save(entry);
        }

        return mapResults(raceResultRepository.saveAll(existingResults));
    }

    private List<RaceResultResponse> mapResults(List<RaceResult> results) {
        List<RaceResultResponse> responses = new ArrayList<>();
        for (RaceResult result : results) {
            responses.add(raceResultMapper.toRaceResultResponse(result));
        }
        return responses;
    }

    private Referee validateAndGetHeadReferee(Race race) {
        User currentUser = userCurrentService.getCurrentUser();
        Referee referee = requireReferee(currentUser.getUserId());
        if (referee.getStatus() == RefereeStatus.SUSPENDED) {
            throw new AppException(ErrorCode.REFEREE_NOT_AVAILABLE);
        }
        Round round = race.getRound();
        if (round == null || round.getHeadReferee() == null
                || !round.getHeadReferee().getRefereeId().equals(referee.getRefereeId())) {
            throw new AppException(ErrorCode.ACCESS_DENIED);
        }
        return referee;
    }

    private Referee validateAndGetReferee(UUID raceId) {
        User currentUser = userCurrentService.getCurrentUser();

        Referee referee = requireReferee(currentUser.getUserId());

        if (referee.getStatus() == RefereeStatus.SUSPENDED) {
            throw new AppException(ErrorCode.ACCESS_DENIED);
        }

        if (!raceRefereeRepository.existsByRace_RaceIdAndReferee_RefereeId(raceId, referee.getRefereeId())) {
            throw new AppException(ErrorCode.RACE_REFEREE_NOT_FOUND);
        }

        return referee;
    }

    private boolean isActualStarter(RaceEntry entry) {
        return entry.getStatus() != RaceEntryStatus.SCRATCHED
                && entry.getStatus() != RaceEntryStatus.WITHDRAWN_BEFORE_SCHEDULE
                && entry.getStatus() != RaceEntryStatus.WITHDRAWN_AFTER_SCHEDULE;
    }

    private boolean isResultEditableRaceStatus(RoundStatus status) {
        return status == RoundStatus.ONGOING || status == RoundStatus.FINISHED;
    }

    private float roundToHundredth(double value) {
        return (float) (Math.round(value * 100d) / 100d);
    }

    private Race requireRace(UUID raceId) {
        Optional<Race> race = raceRepository.findById(raceId);
        if (race.isEmpty()) {
            throw new AppException(ErrorCode.RACE_NOT_FOUND);
        }
        return race.get();
    }

    private Referee requireReferee(UUID userId) {
        Optional<Referee> referee = refereeRepository.findByUser_UserId(userId);
        if (referee.isEmpty()) {
            throw new AppException(ErrorCode.REFEREE_PROFILE_NOT_FOUND);
        }
        return referee.get();
    }

    private record GeneratedTiming(RaceEntry entry, double finishTime) {
    }
}

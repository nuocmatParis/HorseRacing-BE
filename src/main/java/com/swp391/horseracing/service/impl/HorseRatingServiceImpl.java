package com.swp391.horseracing.service.impl;

import com.swp391.horseracing.dto.horse.HorseRatingHistoryResponse;
import com.swp391.horseracing.dto.horse.HorseRatingPreviewItem;
import com.swp391.horseracing.dto.horse.RaceRatingChangesResponse;
import com.swp391.horseracing.dto.horse.RaceRatingPreviewResponse;
import com.swp391.horseracing.dto.horse.RoundRatingSummaryResponse;
import com.swp391.horseracing.entity.Horse;
import com.swp391.horseracing.entity.HorseOwner;
import com.swp391.horseracing.entity.HorseRatingHistory;
import com.swp391.horseracing.entity.Race;
import com.swp391.horseracing.entity.RaceReport;
import com.swp391.horseracing.entity.RaceResult;
import com.swp391.horseracing.entity.Round;
import com.swp391.horseracing.entity.Tournament;
import com.swp391.horseracing.entity.User;
import com.swp391.horseracing.enums.RaceClass;
import com.swp391.horseracing.enums.RaceResultStatus;
import com.swp391.horseracing.enums.ReportStatus;
import com.swp391.horseracing.enums.RoleName;
import com.swp391.horseracing.enums.RoundStatus;
import com.swp391.horseracing.exception.AppException;
import com.swp391.horseracing.exception.ErrorCode;
import com.swp391.horseracing.repository.HorseOwnerRepository;
import com.swp391.horseracing.repository.HorseRatingHistoryRepository;
import com.swp391.horseracing.repository.HorseRepository;
import com.swp391.horseracing.repository.RaceReportRepository;
import com.swp391.horseracing.repository.RaceRepository;
import com.swp391.horseracing.repository.RaceResultRepository;
import com.swp391.horseracing.repository.RoundRepository;
import com.swp391.horseracing.service.HorseRatingService;
import com.swp391.horseracing.service.UserCurrentService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class HorseRatingServiceImpl implements HorseRatingService {

    RaceRepository raceRepository;
    RaceResultRepository raceResultRepository;
    HorseRepository horseRepository;
    HorseRatingHistoryRepository ratingHistoryRepository;
    RaceReportRepository raceReportRepository;
    RoundRepository roundRepository;
    UserCurrentService userCurrentService;
    HorseOwnerRepository horseOwnerRepository;

    @Override
    public void validateRatingChange(
            Race race, RaceResultStatus status, Integer rank, Integer ratingChange) {
        if (ratingChange == null) {
            throw new AppException(ErrorCode.HORSE_RATING_CHANGE_REQUIRED);
        }

        RatingRange allowedRange = determineAllowedRange(race, status, rank);
        if (ratingChange < allowedRange.minimum || ratingChange > allowedRange.maximum) {
            throw new AppException(ErrorCode.HORSE_RATING_CHANGE_OUT_OF_RANGE);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public RaceRatingPreviewResponse previewForRace(UUID raceId) {
        Race race = requireRace(raceId);
        RaceReport report = requireRaceReport(raceId);
        if (report.getStatus() != ReportStatus.SIGNED) {
            throw new AppException(ErrorCode.RACE_REPORT_NOT_SIGNED);
        }

        List<RaceResult> results = raceResultRepository.findByRace_RaceIdOrderByRankAsc(raceId);
        List<HorseRatingPreviewItem> changes = buildPreviewItems(results);
        return RaceRatingPreviewResponse.builder()
                .raceId(raceId)
                .reportStatus(report.getStatus().name())
                .policyVersion(race.getRound().getTournament().getRatingPolicyVersion())
                .changes(changes)
                .build();
    }

    @Override
    @Transactional
    public List<HorseRatingHistory> applyManualRatingsForPublish(UUID raceId) {
        Race race = requireRace(raceId);
        List<RaceResult> results = raceResultRepository.findForUpdateByRace_RaceId(raceId);
        if (results.isEmpty()) {
            return Collections.emptyList();
        }

        List<UUID> horseIds = new ArrayList<>();
        for (RaceResult result : results) {
            validateRatingChange(
                    race, result.getStatus(), result.getRank(), result.getRatingChange());
            UUID horseId = result.getEntry().getContract().getHorse().getHorseId();
            horseIds.add(horseId);
        }
        Collections.sort(horseIds);

        List<Horse> lockedHorses = horseRepository.findAllForUpdateByHorseIdIn(horseIds);
        Map<UUID, Horse> horsesById = new HashMap<>();
        for (Horse horse : lockedHorses) {
            horsesById.put(horse.getHorseId(), horse);
        }

        List<HorseRatingHistory> histories = new ArrayList<>();
        for (RaceResult result : results) {
            if (ratingHistoryRepository.existsByRaceResult_ResultId(result.getResultId())) {
                throw new AppException(ErrorCode.HORSE_RATING_ALREADY_APPLIED);
            }

            UUID horseId = result.getEntry().getContract().getHorse().getHorseId();
            Horse horse = horsesById.get(horseId);
            if (horse == null) {
                throw new AppException(ErrorCode.HORSE_NOT_FOUND);
            }

            int oldRating = horse.getCurrentRating();
            int newRating = Math.max(0, oldRating + result.getRatingChange());
            RaceClass oldRaceClass = RaceClass.fromRating(oldRating);
            RaceClass newRaceClass = RaceClass.fromRating(newRating);

            horse.setCurrentRating(newRating);
            horse.setHighestRating(Math.max(horse.getHighestRating(), newRating));
            horse.setRaceClass(newRaceClass);
            horse.setRatingUpdatedAt(LocalDateTime.now());
            horseRepository.save(horse);

            HorseRatingHistory history = HorseRatingHistory.builder()
                    .horse(horse)
                    .race(race)
                    .raceResult(result)
                    .oldRating(oldRating)
                    .finalChange(result.getRatingChange())
                    .adjustmentReason(result.getRatingAdjustmentReason())
                    .newRating(newRating)
                    .oldRaceClass(oldRaceClass)
                    .newRaceClass(newRaceClass)
                    .policyVersion(race.getRound().getTournament().getRatingPolicyVersion())
                    .calculatedAt(LocalDateTime.now())
                    .build();
            histories.add(ratingHistoryRepository.save(history));
        }
        return histories;
    }

    @Override
    @Transactional(readOnly = true)
    public RaceRatingChangesResponse getRatingChangesForRace(UUID raceId) {
        Race race = requireRace(raceId);
        RaceReport report = requireRaceReport(raceId);
        if (report.getStatus() != ReportStatus.PUBLISHED) {
            throw new AppException(ErrorCode.RACE_REPORT_NOT_PUBLISHED);
        }

        List<HorseRatingHistory> histories = ratingHistoryRepository.findByRace_RaceId(raceId);
        List<HorseRatingHistoryResponse> responses = new ArrayList<>();
        for (HorseRatingHistory history : histories) {
            responses.add(mapToHistoryResponse(history));
        }

        int policyVersion = race.getRound().getTournament().getRatingPolicyVersion();
        if (!responses.isEmpty()) {
            policyVersion = responses.get(0).getPolicyVersion();
        }
        return RaceRatingChangesResponse.builder()
                .raceId(raceId)
                .reportStatus(report.getStatus().name())
                .policyVersion(policyVersion)
                .changes(responses)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public List<HorseRatingHistoryResponse> getRatingHistoryForHorse(UUID horseId) {
        Optional<Horse> horseOptional = horseRepository.findById(horseId);
        if (horseOptional.isEmpty()) {
            throw new AppException(ErrorCode.HORSE_NOT_FOUND);
        }
        Horse horse = horseOptional.get();
        User currentUser = userCurrentService.getCurrentUser();
        if (currentUser.getRole().getRoleName() != RoleName.ADMIN) {
            Optional<HorseOwner> ownerOptional =
                    horseOwnerRepository.findByUser_UserId(currentUser.getUserId());
            if (ownerOptional.isEmpty()) {
                throw new AppException(ErrorCode.OWNER_PROFILE_NOT_FOUND);
            }
            HorseOwner owner = ownerOptional.get();
            if (!horse.getOwner().getOwnerId().equals(owner.getOwnerId())) {
                throw new AppException(ErrorCode.ACCESS_DENIED);
            }
        }

        List<HorseRatingHistory> histories =
                ratingHistoryRepository.findByHorse_HorseIdOrderByCalculatedAtAsc(horseId);
        List<HorseRatingHistoryResponse> responses = new ArrayList<>();
        for (HorseRatingHistory history : histories) {
            responses.add(mapToHistoryResponse(history));
        }
        return responses;
    }

    @Override
    @Transactional(readOnly = true)
    public RoundRatingSummaryResponse getRoundRatingSummary(UUID roundId) {
        Optional<Round> roundOptional = roundRepository.findById(roundId);
        if (roundOptional.isEmpty()) {
            throw new AppException(ErrorCode.ROUND_NOT_FOUND);
        }
        Round round = roundOptional.get();
        List<Race> races = round.getRaces();
        if (races == null) {
            races = Collections.emptyList();
        }

        int publishedCount = 0;
        int cancelledCount = 0;
        List<RaceRatingChangesResponse> summaries = new ArrayList<>();
        for (Race race : races) {
            Optional<RaceReport> report = raceReportRepository.findByRace_RaceId(race.getRaceId());
            if (report.isPresent() && report.get().getStatus() == ReportStatus.PUBLISHED) {
                publishedCount++;
                summaries.add(getRatingChangesForRace(race.getRaceId()));
            } else if (race.getStatus() == RoundStatus.CANCELLED) {
                cancelledCount++;
                summaries.add(emptyRaceSummary(race, "CANCELLED"));
            } else {
                String status = "NO_REPORT";
                if (report.isPresent()) {
                    status = report.get().getStatus().name();
                }
                summaries.add(emptyRaceSummary(race, status));
            }
        }

        int processedCount = publishedCount + cancelledCount;
        String summaryStatus = "PARTIAL";
        if (processedCount == 0) {
            summaryStatus = "NOT_STARTED";
        } else if (processedCount == races.size()) {
            summaryStatus = "COMPLETED";
        }
        return RoundRatingSummaryResponse.builder()
                .roundId(roundId)
                .summaryStatus(summaryStatus)
                .publishedRaces(publishedCount)
                .totalRaces(races.size())
                .races(summaries)
                .build();
    }

    private List<HorseRatingPreviewItem> buildPreviewItems(List<RaceResult> results) {
        List<HorseRatingPreviewItem> items = new ArrayList<>();
        for (RaceResult result : results) {
            validateRatingChange(
                    result.getRace(), result.getStatus(), result.getRank(), result.getRatingChange());
            Horse horse = result.getEntry().getContract().getHorse();
            int oldRating = horse.getCurrentRating();
            int newRating = Math.max(0, oldRating + result.getRatingChange());
            RatingRange range = determineAllowedRange(
                    result.getRace(), result.getStatus(), result.getRank());
            items.add(HorseRatingPreviewItem.builder()
                    .horseId(horse.getHorseId())
                    .horseName(horse.getName())
                    .finishPosition(result.getRank())
                    .resultStatus(result.getStatus())
                    .oldRating(oldRating)
                    .minimumAllowedChange(range.minimum)
                    .maximumAllowedChange(range.maximum)
                    .finalChange(result.getRatingChange())
                    .adjustmentReason(result.getRatingAdjustmentReason())
                    .newRating(newRating)
                    .oldRaceClass(RaceClass.fromRating(oldRating))
                    .newRaceClass(RaceClass.fromRating(newRating))
                    .build());
        }
        return items;
    }

    private RaceRatingChangesResponse emptyRaceSummary(Race race, String reportStatus) {
        return RaceRatingChangesResponse.builder()
                .raceId(race.getRaceId())
                .reportStatus(reportStatus)
                .policyVersion(race.getRound().getTournament().getRatingPolicyVersion())
                .changes(Collections.emptyList())
                .build();
    }

    private HorseRatingHistoryResponse mapToHistoryResponse(HorseRatingHistory history) {
        RatingRange range = determineAllowedRange(
                history.getRace(),
                history.getRaceResult().getStatus(),
                history.getRaceResult().getRank());
        return HorseRatingHistoryResponse.builder()
                .ratingHistoryId(history.getRatingHistoryId())
                .horseId(history.getHorse().getHorseId())
                .horseName(history.getHorse().getName())
                .raceId(history.getRace().getRaceId())
                .raceName(history.getRace().getName())
                .roundId(history.getRace().getRound().getRoundId())
                .finishPosition(history.getRaceResult().getRank())
                .oldRating(history.getOldRating())
                .minimumAllowedChange(range.minimum)
                .maximumAllowedChange(range.maximum)
                .finalChange(history.getFinalChange())
                .adjustmentReason(history.getAdjustmentReason())
                .newRating(history.getNewRating())
                .oldRaceClass(history.getOldRaceClass())
                .newRaceClass(history.getNewRaceClass())
                .policyVersion(history.getPolicyVersion())
                .calculatedAt(history.getCalculatedAt())
                .build();
    }

    private RatingRange determineAllowedRange(
            Race race, RaceResultStatus status, Integer rank) {
        Tournament tournament = requireRatingTournament(race);
        if (status == RaceResultStatus.DISQUALIFIED) {
            return new RatingRange(
                    tournament.getRatingDisqualifiedMin(),
                    tournament.getRatingDisqualifiedMax());
        }
        if (status != RaceResultStatus.FINISHED || rank == null || rank < 1) {
            throw new AppException(ErrorCode.INVALID_RACE_RESULT_STATUS);
        }
        if (rank == 1) {
            return new RatingRange(
                    tournament.getRatingFirstMin(), tournament.getRatingFirstMax());
        }
        if (rank == 2) {
            return new RatingRange(
                    tournament.getRatingSecondMin(), tournament.getRatingSecondMax());
        }
        if (rank == 3) {
            return new RatingRange(
                    tournament.getRatingThirdMin(), tournament.getRatingThirdMax());
        }
        if (rank == 4 || rank == 5) {
            return new RatingRange(
                    tournament.getRatingFourthFifthMin(),
                    tournament.getRatingFourthFifthMax());
        }
        return new RatingRange(
                tournament.getRatingOtherMin(), tournament.getRatingOtherMax());
    }

    private Tournament requireRatingTournament(Race race) {
        if (race == null
                || race.getRound() == null
                || race.getRound().getTournament() == null) {
            throw new AppException(ErrorCode.INVALID_HORSE_RATING_CONFIG);
        }
        Tournament tournament = race.getRound().getTournament();
        if (tournament.getRatingPolicyVersion() < 1) {
            throw new AppException(ErrorCode.INVALID_HORSE_RATING_CONFIG);
        }
        return tournament;
    }

    private Race requireRace(UUID raceId) {
        Optional<Race> race = raceRepository.findById(raceId);
        if (race.isEmpty()) {
            throw new AppException(ErrorCode.RACE_NOT_FOUND);
        }
        return race.get();
    }

    private RaceReport requireRaceReport(UUID raceId) {
        Optional<RaceReport> report = raceReportRepository.findByRace_RaceId(raceId);
        if (report.isEmpty()) {
            throw new AppException(ErrorCode.RACE_REPORT_NOT_FOUND);
        }
        return report.get();
    }

    private static final class RatingRange {
        private final int minimum;
        private final int maximum;

        private RatingRange(int minimum, int maximum) {
            this.minimum = minimum;
            this.maximum = maximum;
        }
    }
}

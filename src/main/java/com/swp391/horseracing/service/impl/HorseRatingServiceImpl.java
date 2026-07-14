package com.swp391.horseracing.service.impl;

import com.swp391.horseracing.config.HorseRatingProperties;
import com.swp391.horseracing.dto.horse.*;
import com.swp391.horseracing.entity.*;
import com.swp391.horseracing.enums.*;
import com.swp391.horseracing.exception.AppException;
import com.swp391.horseracing.exception.ErrorCode;
import com.swp391.horseracing.repository.*;
import com.swp391.horseracing.service.HorseRatingService;
import com.swp391.horseracing.service.UserCurrentService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class HorseRatingServiceImpl implements HorseRatingService {

    HorseRatingProperties properties;
    RaceRepository raceRepository;
    RaceResultRepository raceResultRepository;
    HorseRepository horseRepository;
    HorseRatingHistoryRepository ratingHistoryRepository;
    RaceReportRepository raceReportRepository;
    RoundRepository roundRepository;
    UserCurrentService userCurrentService;
    HorseOwnerRepository horseOwnerRepository;

    @Override
    public List<HorseRatingCalculation> calculateForRace(UUID raceId, Map<UUID, Integer> ratingSnapshot) {
        Race race = raceRepository.findById(raceId)
                .orElseThrow(() -> new AppException(ErrorCode.RACE_NOT_FOUND));

        List<RaceResult> allResults = raceResultRepository.findByRace_RaceId(raceId);

        for (RaceResult result : allResults) {
            if (!isActualStarter(result.getEntry())) {
                throw new AppException(ErrorCode.INVALID_RACE_RESULT_STATUS);
            }
            if (result.getStatus() != RaceResultStatus.FINISHED 
                    && result.getStatus() != RaceResultStatus.DID_NOT_FINISH 
                    && result.getStatus() != RaceResultStatus.DISQUALIFIED) {
                throw new AppException(ErrorCode.INVALID_RACE_RESULT_STATUS);
            }
        }

        List<RaceResult> starters = allResults;

        Float winnerTime = starters.stream()
                .filter(r -> r.getStatus() == RaceResultStatus.FINISHED && r.getRank() != null && r.getRank() == 1)
                .map(RaceResult::getFinishTime)
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(null);

        Float secondTime = starters.stream()
                .filter(r -> r.getStatus() == RaceResultStatus.FINISHED && r.getRank() != null && r.getRank() == 2)
                .map(RaceResult::getFinishTime)
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(null);

        List<HorseRatingCalculation> calculations = new ArrayList<>();

        for (RaceResult result : allResults) {
            Horse horse = result.getEntry().getContract().getHorse();
            UUID horseId = horse.getHorseId();
            

            int oldRating = ratingSnapshot.getOrDefault(horseId, horse.getCurrentRating());
            RaceClass oldRaceClass = RaceClass.fromRating(oldRating);
            double sumOpponentRating = 0;
            int countOpponents = 0;
            for (RaceResult s : starters) {
                UUID oppId = s.getEntry().getContract().getHorse().getHorseId();
                if (!oppId.equals(horseId)) {
                    sumOpponentRating += ratingSnapshot.getOrDefault(oppId, s.getEntry().getContract().getHorse().getCurrentRating());
                    countOpponents++;
                }
            }
            double averageOpponentRating = countOpponents > 0 ? sumOpponentRating / countOpponents : 0;
            double ratingDifference = averageOpponentRating - oldRating;

            Integer rank = result.getRank();
            RaceResultStatus status = result.getStatus();

            int baseChange = 0;
            if (status == RaceResultStatus.FINISHED && rank != null) {
                if (rank == 1) {
                    baseChange = properties.getFirstBase();
                } else if (rank == 2) {
                    baseChange = properties.getSecondBase();
                } else if (rank == 3) {
                    baseChange = properties.getThirdBase();
                }
            } else if (status == RaceResultStatus.DID_NOT_FINISH) {
                baseChange = properties.getDnfChange();
            } else if (status == RaceResultStatus.DISQUALIFIED) {
                baseChange = properties.getDisqualifiedChange();
            }

            int opponentStrengthBonus = 0;
            if (status == RaceResultStatus.FINISHED && rank != null && rank >= 1 && rank <= 5) {
                if (ratingDifference >= properties.getStrongOpponentDifference()) {
                    opponentStrengthBonus = (rank == 1) ? properties.getFirstStrongOpponentBonus() : 
                            ((rank <= 3) ? properties.getTopThreeStrongOpponentBonus() : properties.getFourthFifthOpponentBonus());
                } else if (ratingDifference >= properties.getMediumOpponentDifference()) {
                    opponentStrengthBonus = (rank == 1) ? properties.getFirstMediumOpponentBonus() : 
                            ((rank <= 3) ? properties.getTopThreeMediumOpponentBonus() : properties.getFourthFifthOpponentBonus());
                } else if (ratingDifference >= properties.getWeakOpponentDifference()) {
                    opponentStrengthBonus = (rank == 1) ? properties.getFirstWeakOpponentBonus() : 0;
                }
            }

            int finishPerformanceBonus = 0;
            Float myTime = result.getFinishTime();
            if (status == RaceResultStatus.FINISHED && rank != null && winnerTime != null && myTime != null) {
                if (rank == 1 && secondTime != null) {
                    double winGapPercent = (secondTime - winnerTime) / winnerTime * 100.0;
                    if (winGapPercent >= properties.getWinnerLargeGapPercent()) {
                        finishPerformanceBonus = properties.getWinnerLargeGapBonus();
                    } else if (winGapPercent >= properties.getWinnerMediumGapPercent()) {
                        finishPerformanceBonus = properties.getWinnerMediumGapBonus();
                    }
                } else if (rank == 2 || rank == 3) {
                    double gapFromWinnerPercent = (myTime - winnerTime) / winnerTime * 100.0;
                    if (gapFromWinnerPercent <= properties.getTopThreeCloseGapPercent()) {
                        finishPerformanceBonus = properties.getCloseFinishBonus();
                    }
                } else if (rank == 4 || rank == 5) {
                    double gapFromWinnerPercent = (myTime - winnerTime) / winnerTime * 100.0;
                    if (gapFromWinnerPercent <= properties.getFourthFifthCloseGapPercent()) {
                        finishPerformanceBonus = properties.getCloseFinishBonus();
                    }
                }
            }

            int fieldSizeBonus = 0;
            if (status == RaceResultStatus.FINISHED && rank != null && rank == 1) {
                if (starters.size() >= properties.getLargeFieldSize()) {
                    fieldSizeBonus = properties.getLargeFieldBonus();
                }
            }
            int underperformancePenalty = 0;
            if (status == RaceResultStatus.FINISHED && rank != null && rank >= 6) {
                if (winnerTime != null && myTime != null) {
                    double gapFromWinnerPercent = (myTime - winnerTime) / winnerTime * 100.0;
                    if (gapFromWinnerPercent > properties.getSevereUnderperformanceGapPercent()) {
                        underperformancePenalty = properties.getSevereUnderperformancePenalty();
                    } else if (gapFromWinnerPercent > properties.getMediumUnderperformanceGapPercent()) {
                        underperformancePenalty = properties.getMediumUnderperformancePenalty();
                    } else if (gapFromWinnerPercent > properties.getSmallUnderperformanceGapPercent()) {
                        underperformancePenalty = properties.getSmallUnderperformancePenalty();
                    }
                }

                boolean inBottomHalf = rank > (starters.size() / 2);
                if ((oldRating - averageOpponentRating) >= properties.getHighRatedUnderperformanceDifference() && inBottomHalf) {
                    underperformancePenalty += properties.getHighRatedUnderperformanceExtraPenalty();
                }
                underperformancePenalty = Math.max(properties.getMaxDecrease(), underperformancePenalty);
            }

            int finalChange = baseChange + opponentStrengthBonus + finishPerformanceBonus + fieldSizeBonus + underperformancePenalty;
            if (status == RaceResultStatus.FINISHED && rank != null) {
                if (rank == 1) {
                    finalChange = Math.max(properties.getFirstBase(), Math.min(properties.getFirstMax(), finalChange));
                } else if (rank == 2) {
                    finalChange = Math.max(properties.getSecondBase(), Math.min(properties.getSecondMax(), finalChange));
                } else if (rank == 3) {
                    finalChange = Math.max(properties.getThirdBase(), Math.min(properties.getThirdMax(), finalChange));
                } else if (rank == 4 || rank == 5) {
                    finalChange = Math.max(0, Math.min(properties.getFourthFifthMax(), finalChange));
                } else {
                    finalChange = Math.max(properties.getMaxDecrease(), Math.min(0, finalChange));
                }
            } else if (status == RaceResultStatus.DID_NOT_FINISH) {
                finalChange = properties.getDnfChange();
            } else if (status == RaceResultStatus.DISQUALIFIED) {
                finalChange = properties.getDisqualifiedChange();
            }

            int newRating = Math.max(0, oldRating + finalChange);
            RaceClass newRaceClass = RaceClass.fromRating(newRating);

            calculations.add(HorseRatingCalculation.builder()
                    .horseId(horse.getHorseId())
                    .horseName(horse.getName())
                    .finishPosition(rank)
                    .oldRating(oldRating)
                    .baseChange(baseChange)
                    .opponentStrengthBonus(opponentStrengthBonus)
                    .finishPerformanceBonus(finishPerformanceBonus)
                    .fieldSizeBonus(fieldSizeBonus)
                    .underperformancePenalty(underperformancePenalty)
                    .finalChange(finalChange)
                    .newRating(newRating)
                    .oldRaceClass(oldRaceClass)
                    .newRaceClass(newRaceClass)
                    .build());
        }

        return calculations;
    }

    @Override
    public RaceRatingPreviewResponse previewForRace(UUID raceId) {
        raceRepository.findById(raceId)
                .orElseThrow(() -> new AppException(ErrorCode.RACE_NOT_FOUND));

        RaceReport report = raceReportRepository.findByRace_RaceId(raceId)
                .orElseThrow(() -> new AppException(ErrorCode.RACE_REPORT_NOT_FOUND));

        if (report.getStatus() != ReportStatus.Signed) {
            throw new AppException(ErrorCode.RACE_REPORT_NOT_SIGNED);
        }

        List<RaceResult> allResults = raceResultRepository.findByRace_RaceId(raceId);
        Map<UUID, Integer> ratingSnapshot = allResults.stream()
                .collect(Collectors.toMap(
                        r -> r.getEntry().getContract().getHorse().getHorseId(),
                        r -> r.getEntry().getContract().getHorse().getCurrentRating()
                ));

        List<HorseRatingCalculation> calculations = calculateForRace(raceId, ratingSnapshot);

        return RaceRatingPreviewResponse.builder()
                .raceId(raceId)
                .reportStatus(report.getStatus().name())
                .policyVersion(properties.getPolicyVersion())
                .changes(calculations)
                .build();
    }

    @Override
    @Transactional
    public List<HorseRatingHistory> calculateAndApplyForPublish(UUID raceId) {
        Race race = raceRepository.findById(raceId)
                .orElseThrow(() -> new AppException(ErrorCode.RACE_NOT_FOUND));

        List<RaceResult> allResults = raceResultRepository.findByRace_RaceId(raceId);
        if (allResults.isEmpty()) {
            return Collections.emptyList();
        }

        // Lock all participating horses in a deterministic order to prevent deadlock
        List<UUID> horseIds = allResults.stream()
                .map(r -> r.getEntry().getContract().getHorse().getHorseId())
                .sorted()
                .collect(Collectors.toList());

        List<Horse> lockedHorses = horseRepository.findAllForUpdateByHorseIdIn(horseIds);
        Map<UUID, Horse> horseMap = lockedHorses.stream()
                .collect(Collectors.toMap(Horse::getHorseId, h -> h));

        Map<UUID, Integer> ratingSnapshot = lockedHorses.stream()
                .collect(Collectors.toMap(Horse::getHorseId, Horse::getCurrentRating));

        // Perform calculation based on snapshot
        List<HorseRatingCalculation> calculations = calculateForRace(raceId, ratingSnapshot);

        List<HorseRatingHistory> histories = new ArrayList<>();

        for (HorseRatingCalculation calc : calculations) {
            RaceResult result = allResults.stream()
                    .filter(r -> r.getEntry().getContract().getHorse().getHorseId().equals(calc.getHorseId()))
                    .findFirst()
                    .orElse(null);

            if (result == null) {
                continue;
            }

            // Check if already applied
            if (ratingHistoryRepository.existsByRaceResult_ResultId(result.getResultId())) {
                throw new AppException(ErrorCode.HORSE_RATING_ALREADY_APPLIED);
            }

            Horse horse = horseMap.get(calc.getHorseId());
            if (horse == null) {
                throw new AppException(ErrorCode.HORSE_NOT_FOUND);
            }

            // Protect against concurrent rating changes: double-check current rating
            if (horse.getCurrentRating() != calc.getOldRating()) {
                throw new AppException(ErrorCode.HORSE_RATING_CHANGED_RETRY_REQUIRED);
            }

            // Update Horse rating
            horse.setCurrentRating(calc.getNewRating());
            horse.setHighestRating(Math.max(horse.getHighestRating(), calc.getNewRating()));
            horse.setRaceClass(calc.getNewRaceClass());
            horse.setRatingUpdatedAt(LocalDateTime.now());
            horseRepository.save(horse);

            // Create rating history record
            HorseRatingHistory history = HorseRatingHistory.builder()
                    .horse(horse)
                    .race(race)
                    .raceResult(result)
                    .oldRating(calc.getOldRating())
                    .baseChange(calc.getBaseChange())
                    .opponentStrengthBonus(calc.getOpponentStrengthBonus())
                    .finishPerformanceBonus(calc.getFinishPerformanceBonus())
                    .fieldSizeBonus(calc.getFieldSizeBonus())
                    .underperformancePenalty(calc.getUnderperformancePenalty())
                    .finalChange(calc.getFinalChange())
                    .newRating(calc.getNewRating())
                    .oldRaceClass(calc.getOldRaceClass())
                    .newRaceClass(calc.getNewRaceClass())
                    .policyVersion(properties.getPolicyVersion())
                    .calculatedAt(LocalDateTime.now())
                    .build();

            histories.add(ratingHistoryRepository.save(history));
        }

        return histories;
    }

    @Override
    public RaceRatingChangesResponse getRatingChangesForRace(UUID raceId) {
        raceRepository.findById(raceId)
                .orElseThrow(() -> new AppException(ErrorCode.RACE_NOT_FOUND));

        RaceReport report = raceReportRepository.findByRace_RaceId(raceId)
                .orElseThrow(() -> new AppException(ErrorCode.RACE_REPORT_NOT_FOUND));

        if (report.getStatus() != ReportStatus.Published) {
            throw new AppException(ErrorCode.RACE_REPORT_NOT_PUBLISHED);
        }

        List<HorseRatingHistory> histories = ratingHistoryRepository.findByRace_RaceId(raceId);
        List<HorseRatingHistoryResponse> responses = histories.stream()
                .map(this::mapToHistoryResponse)
                .collect(Collectors.toList());

        int policyVer = responses.isEmpty() ? properties.getPolicyVersion() : responses.get(0).getPolicyVersion();

        return RaceRatingChangesResponse.builder()
                .raceId(raceId)
                .reportStatus(report.getStatus().name())
                .policyVersion(policyVer)
                .changes(responses)
                .build();
    }

    @Override
    public List<HorseRatingHistoryResponse> getRatingHistoryForHorse(UUID horseId) {
        Horse horse = horseRepository.findById(horseId)
                .orElseThrow(() -> new AppException(ErrorCode.HORSE_NOT_FOUND));

        User currentUser = userCurrentService.getCurrentUser();
        boolean isAdmin = currentUser.getRole().getRoleName() == RoleName.ADMIN;
        if (!isAdmin) {
            HorseOwner owner = horseOwnerRepository.findByUser_UserId(currentUser.getUserId())
                    .orElseThrow(() -> new AppException(ErrorCode.OWNER_PROFILE_NOT_FOUND));
            if (!horse.getOwner().getOwnerId().equals(owner.getOwnerId())) {
                throw new AppException(ErrorCode.ACCESS_DENIED);
            }
        }

        List<HorseRatingHistory> histories = ratingHistoryRepository.findByHorse_HorseIdOrderByCalculatedAtAsc(horseId);
        return histories.stream()
                .map(this::mapToHistoryResponse)
                .collect(Collectors.toList());
    }

    @Override
    public RoundRatingSummaryResponse getRoundRatingSummary(UUID roundId) {
        Round round = roundRepository.findById(roundId)
                .orElseThrow(() -> new AppException(ErrorCode.ROUND_NOT_FOUND));

        List<Race> roundRaces = round.getRaces();
        if (roundRaces == null) {
            roundRaces = Collections.emptyList();
        }

        int totalRaces = roundRaces.size();
        int publishedCount = 0;
        int cancelledCount = 0;

        List<RaceRatingChangesResponse> racesSummary = new ArrayList<>();

        for (Race race : roundRaces) {
            Optional<RaceReport> reportOpt = raceReportRepository.findByRace_RaceId(race.getRaceId());
            boolean isPublished = reportOpt.isPresent() && reportOpt.get().getStatus() == ReportStatus.Published;
            boolean isCancelled = race.getStatus() == RoundStatus.CANCELLED;

            if (isPublished) {
                publishedCount++;
                racesSummary.add(getRatingChangesForRace(race.getRaceId()));
            } else if (isCancelled) {
                cancelledCount++;
                racesSummary.add(RaceRatingChangesResponse.builder()
                        .raceId(race.getRaceId())
                        .reportStatus("CANCELLED")
                        .policyVersion(properties.getPolicyVersion())
                        .changes(Collections.emptyList())
                        .build());
            } else {
                String reportStatusName = reportOpt.map(r -> r.getStatus().name()).orElse("NO_REPORT");
                racesSummary.add(RaceRatingChangesResponse.builder()
                        .raceId(race.getRaceId())
                        .reportStatus(reportStatusName)
                        .policyVersion(properties.getPolicyVersion())
                        .changes(Collections.emptyList())
                        .build());
            }
        }

        String summaryStatus;
        int processed = publishedCount + cancelledCount;
        if (processed == 0) {
            summaryStatus = "NOT_STARTED";
        } else if (processed == totalRaces) {
            summaryStatus = "COMPLETED";
        } else {
            summaryStatus = "PARTIAL";
        }

        return RoundRatingSummaryResponse.builder()
                .roundId(roundId)
                .summaryStatus(summaryStatus)
                .publishedRaces(publishedCount)
                .totalRaces(totalRaces)
                .races(racesSummary)
                .build();
    }

    private HorseRatingHistoryResponse mapToHistoryResponse(HorseRatingHistory h) {
        return HorseRatingHistoryResponse.builder()
                .ratingHistoryId(h.getRatingHistoryId())
                .horseId(h.getHorse().getHorseId())
                .horseName(h.getHorse().getName())
                .raceId(h.getRace().getRaceId())
                .raceName(h.getRace().getName())
                .roundId(h.getRace().getRound().getRoundId())
                .finishPosition(h.getRaceResult().getRank())
                .oldRating(h.getOldRating())
                .baseChange(h.getBaseChange())
                .opponentStrengthBonus(h.getOpponentStrengthBonus())
                .finishPerformanceBonus(h.getFinishPerformanceBonus())
                .fieldSizeBonus(h.getFieldSizeBonus())
                .underperformancePenalty(h.getUnderperformancePenalty())
                .finalChange(h.getFinalChange())
                .newRating(h.getNewRating())
                .oldRaceClass(h.getOldRaceClass())
                .newRaceClass(h.getNewRaceClass())
                .policyVersion(h.getPolicyVersion())
                .calculatedAt(h.getCalculatedAt())
                .build();
    }

    private boolean isActualStarter(RaceEntry entry) {
        return entry.getStatus() != RaceEntryStatus.SCRATCHED
                && entry.getStatus() != RaceEntryStatus.WITHDRAWN_BEFORE_SCHEDULE
                && entry.getStatus() != RaceEntryStatus.WITHDRAWN_AFTER_SCHEDULE;
    }
}

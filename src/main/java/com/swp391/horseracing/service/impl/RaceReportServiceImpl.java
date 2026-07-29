package com.swp391.horseracing.service.impl;

import com.swp391.horseracing.dto.race_report.response.RaceReportResponse;
import com.swp391.horseracing.dto.race_report.request.UpdateRaceReportRequest;
import com.swp391.horseracing.dto.race_report.request.ReturnRaceReportRequest;
import com.swp391.horseracing.dto.race_result.response.RaceResultResponse;
import com.swp391.horseracing.dto.tournament.response.RoundQualifierResponse;
import com.swp391.horseracing.entity.*;
import com.swp391.horseracing.enums.*;
import com.swp391.horseracing.service.*;
import com.swp391.horseracing.exception.AppException;
import com.swp391.horseracing.exception.ErrorCode;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Optional;
import java.util.Map;
import com.swp391.horseracing.mapper.RaceReportMapper;
import com.swp391.horseracing.mapper.RaceResultMapper;
import com.swp391.horseracing.repository.*;
import com.swp391.horseracing.service.RaceReportService;
import com.swp391.horseracing.service.ScoringService;
import com.swp391.horseracing.service.UserCurrentService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequiredArgsConstructor
public class RaceReportServiceImpl implements RaceReportService {

    RaceReportRepository raceReportRepository;
    RaceRepository raceRepository;
    RaceResultRepository raceResultRepository;
    RaceEntryRepository raceEntryRepository;
    RaceRefereeRepository raceRefereeRepository;
    RefereeRepository refereeRepository;
    AppealRepository appealRepository;
    RaceReportMapper raceReportMapper;
    RaceResultMapper raceResultMapper;
    UserCurrentService userCurrentService;
    RoundRepository roundRepository;
    BusinessNotificationEventService notificationEventService;
    ScoringService scoringService;
    PrizeStructureRepository prizeStructureRepository;
    WalletRepository walletRepository;
    WalletTransactionRepository walletTransactionRepository;
    ContractService contractService;
    JockeyHorseContractRepository jockeyHorseContractRepository;
    HorseRatingService horseRatingService;
    TournamentRepository tournamentRepository;

    @Override
    @Transactional
    public RaceReportResponse getRefereeReport(UUID raceId) {
        Race race = raceRepository.findById(raceId)
                .orElseThrow(() -> new AppException(ErrorCode.RACE_NOT_FOUND));

        Referee referee = getCurrentReferee();
        validateAssignedRaceReferee(race, referee);

        if (!raceResultRepository.existsByRace_RaceId(raceId)) {
            throw new AppException(ErrorCode.RACE_RESULT_NOT_FOUND);
        }

        RaceReport report = raceReportRepository.findByRace_RaceId(raceId)
                .orElseGet(() -> {
                    RaceReport newReport = RaceReport.builder()
                            .race(race)
                            .referee(referee)
                            .summary("")
                            .status(ReportStatus.DRAFT)
                            .build();
                    return raceReportRepository.save(newReport);
                });

        return raceReportMapper.toRaceReportResponse(report);
    }

    @Override
    @Transactional
    public RaceReportResponse updateRefereeReport(UUID raceId, UpdateRaceReportRequest request) {
        Race race = raceRepository.findById(raceId)
                .orElseThrow(() -> new AppException(ErrorCode.RACE_NOT_FOUND));
        Referee referee = getCurrentReferee();
        validateAssignedRaceReferee(race, referee);
        if (!raceResultRepository.existsByRace_RaceId(raceId)) {
            throw new AppException(ErrorCode.RACE_RESULT_NOT_FOUND);
        }
        RaceReport report = raceReportRepository.findByRace_RaceId(raceId)
                .orElseGet(() -> raceReportRepository.save(RaceReport.builder()
                        .race(race)
                        .referee(referee)
                        .summary("")
                        .status(ReportStatus.DRAFT)
                        .build()));
        if (report.getStatus() != ReportStatus.DRAFT) {
            throw new AppException(ErrorCode.RACE_REPORT_NOT_IN_DRAFT);
        }
        if (!report.getReferee().getRefereeId().equals(referee.getRefereeId())) {
            throw new AppException(ErrorCode.ACCESS_DENIED);
        }
        applyReportContent(report, request);
        return raceReportMapper.toRaceReportResponse(raceReportRepository.save(report));
    }

    @Override
    @Transactional
    public RaceReportResponse submitReport(UUID raceId) {
        Race race = raceRepository.findById(raceId)
                .orElseThrow(() -> new AppException(ErrorCode.RACE_NOT_FOUND));
        if (race.getStatus() != RoundStatus.FINISHED) {
            throw new AppException(ErrorCode.INVALID_RACE_RESULT_STATUS);
        }
        Referee referee = getCurrentReferee();
        validateAssignedRaceReferee(race, referee);
        validateRaceResultsBeforeSigning(raceId);

        RaceReport report = raceReportRepository.findForUpdateByRace_RaceId(raceId)
                .orElseThrow(() -> new AppException(ErrorCode.RACE_REPORT_NOT_FOUND));
        if (report.getStatus() == ReportStatus.SUBMITTED_TO_HEAD) {
            throw new AppException(ErrorCode.RACE_REPORT_ALREADY_SUBMITTED);
        }
        if (report.getStatus() != ReportStatus.DRAFT) {
            throw new AppException(ErrorCode.RACE_REPORT_NOT_IN_DRAFT);
        }
        if (!report.getReferee().getRefereeId().equals(referee.getRefereeId())) {
            throw new AppException(ErrorCode.ACCESS_DENIED);
        }
        if (report.getSummary() == null || report.getSummary().isBlank()) {
            throw new AppException(ErrorCode.INVALID_REQUEST);
        }
        report.setStatus(ReportStatus.SUBMITTED_TO_HEAD);
        report.setSubmittedAt(LocalDateTime.now());
        report.setSubmittedBy(referee);
        return raceReportMapper.toRaceReportResponse(raceReportRepository.save(report));
    }

    @Override
    @Transactional(readOnly = true)
    public List<RaceReportResponse> getHeadRefereeReports(UUID roundId, String statusValue) {
        Round round = roundRepository.findById(roundId)
                .orElseThrow(() -> new AppException(ErrorCode.ROUND_NOT_FOUND));
        Referee referee = getCurrentReferee();
        validateHeadReferee(round, referee);
        ReportStatus status;
        try {
            status = ReportStatus.valueOf(statusValue == null
                    ? ReportStatus.SUBMITTED_TO_HEAD.name()
                    : statusValue.trim().toUpperCase());
        } catch (IllegalArgumentException exception) {
            throw new AppException(ErrorCode.INVALID_REQUEST);
        }
        List<RaceReport> reports =
                raceReportRepository.findByRace_Round_RoundIdAndStatusOrderBySubmittedAtAsc(roundId, status);
        List<RaceReportResponse> responses = new ArrayList<>();
        for (RaceReport report : reports) {
            responses.add(raceReportMapper.toRaceReportResponse(report));
        }
        return responses;
    }

    @Override
    @Transactional(readOnly = true)
    public RaceReportResponse getHeadRefereeReport(UUID raceId) {
        RaceReport report = getHeadManagedReport(raceId, getCurrentReferee());
        return raceReportMapper.toRaceReportResponse(report);
    }

    @Override
    @Transactional
    public RaceReportResponse updateHeadRefereeReport(UUID raceId, UpdateRaceReportRequest request) {
        Referee referee = getCurrentReferee();
        RaceReport report = getHeadManagedReportForUpdate(raceId, referee);
        if (report.getStatus() != ReportStatus.SUBMITTED_TO_HEAD) {
            throw new AppException(ErrorCode.RACE_REPORT_NOT_SUBMITTED);
        }
        applyReportContent(report, request);
        return raceReportMapper.toRaceReportResponse(raceReportRepository.save(report));
    }

    @Override
    @Transactional
    public RaceReportResponse returnReport(UUID raceId, ReturnRaceReportRequest request) {
        throw new AppException(ErrorCode.RACE_REPORT_RETURN_NOT_ALLOWED);
    }

    @Override
    @Transactional
    public RaceReportResponse signReport(UUID raceId) {
        Race race = raceRepository.findById(raceId)
                .orElseThrow(() -> new AppException(ErrorCode.RACE_NOT_FOUND));
        if (race.getStatus() != RoundStatus.FINISHED) {
            throw new AppException(ErrorCode.INVALID_RACE_RESULT_STATUS);
        }

        Referee referee = getCurrentReferee();
        validateHeadReferee(race.getRound(), referee);
        validateRaceResultsBeforeSigning(raceId);

        if (appealRepository.existsByEntry_Race_RaceIdAndStatus(raceId, AppealStatus.Pending)) {
            throw new AppException(ErrorCode.RACE_REPORT_PENDING_APPEAL);
        }

        RaceReport report = raceReportRepository.findForUpdateByRace_RaceId(raceId)
                .orElseThrow(() -> new AppException(ErrorCode.RACE_REPORT_NOT_FOUND));
        if (report.getStatus() != ReportStatus.SUBMITTED_TO_HEAD) {
            throw new AppException(ErrorCode.RACE_REPORT_NOT_SUBMITTED);
        }
        report.setStatus(ReportStatus.SIGNED);
        report.setSignedBy(referee);
        report.setSignedAt(LocalDateTime.now());
        raceReportRepository.save(report);

        race.setStatus(RoundStatus.FINISHED);
        if (race.getFinishedAt() == null) {
            race.setFinishedAt(LocalDateTime.now());
        }
        raceRepository.save(race);
        markRoundFinishedIfAllRacesFinished(race.getRound());
        return raceReportMapper.toRaceReportResponse(report);
    }

    private void applyReportContent(RaceReport report, UpdateRaceReportRequest request) {
        if (request.getSummary() != null) {
            report.setSummary(request.getSummary().trim());
        }
        if (request.getAppealNote() != null) {
            report.setAppealNote(request.getAppealNote().trim());
        }
    }

    private RaceReport getHeadManagedReport(UUID raceId, Referee referee) {
        RaceReport report = raceReportRepository.findByRace_RaceId(raceId)
                .orElseThrow(() -> new AppException(ErrorCode.RACE_REPORT_NOT_FOUND));
        validateHeadReferee(report.getRace().getRound(), referee);
        return report;
    }

    private RaceReport getHeadManagedReportForUpdate(UUID raceId, Referee referee) {
        RaceReport report = raceReportRepository.findForUpdateByRace_RaceId(raceId)
                .orElseThrow(() -> new AppException(ErrorCode.RACE_REPORT_NOT_FOUND));
        validateHeadReferee(report.getRace().getRound(), referee);
        return report;
    }

    private Referee getCurrentReferee() {
        User currentUser = userCurrentService.getCurrentUser();
        Referee referee = refereeRepository.findByUser_UserId(currentUser.getUserId())
                .orElseThrow(() -> new AppException(ErrorCode.REFEREE_PROFILE_NOT_FOUND));
        if (referee.getStatus() == RefereeStatus.SUSPENDED) {
            throw new AppException(ErrorCode.REFEREE_NOT_AVAILABLE);
        }
        return referee;
    }

    private void validateAssignedRaceReferee(Race race, Referee referee) {
        if (!raceRefereeRepository.existsByRace_RaceIdAndReferee_RefereeId(
                race.getRaceId(), referee.getRefereeId())) {
            throw new AppException(ErrorCode.REFEREE_NOT_ASSIGNED_TO_RACE);
        }
    }

    private void validateHeadReferee(Round round, Referee referee) {
        if (round == null
                || round.getHeadReferee() == null
                || !round.getHeadReferee().getRefereeId().equals(referee.getRefereeId())) {
            throw new AppException(ErrorCode.ACCESS_DENIED);
        }
    }

    private void validateRaceResultsBeforeSigning(UUID raceId) {
        List<RaceEntry> entries = raceEntryRepository.findByRace_RaceIdOrderByLaneNumberAsc(raceId);
        if (entries.isEmpty()) {
            throw new AppException(ErrorCode.INVALID_RACE_RESULT_STATUS);
        }
        
        List<RaceResult> results = raceResultRepository.findByRace_RaceId(raceId);
        Map<UUID, RaceResult> resultMap = new HashMap<>();
        for (RaceResult result : results) {
            resultMap.putIfAbsent(result.getEntry().getEntryId(), result);
        }

        for (RaceEntry entry : entries) {
            RaceEntryStatus status = entry.getStatus();
            if (status != RaceEntryStatus.SCRATCHED
                    && status != RaceEntryStatus.WITHDRAWN_BEFORE_SCHEDULE
                    && status != RaceEntryStatus.WITHDRAWN_AFTER_SCHEDULE) {

                RaceResult result = resultMap.get(entry.getEntryId());
                if (result == null) {
                    throw new AppException(ErrorCode.RACE_RESULT_NOT_FOUND);
                }

                RaceResultStatus resultStatus = result.getStatus();
                if (resultStatus != RaceResultStatus.FINISHED
                        && resultStatus != RaceResultStatus.DISQUALIFIED) {
                    throw new AppException(ErrorCode.INVALID_RACE_RESULT_STATUS);
                }

                if (resultStatus == RaceResultStatus.FINISHED) {
                    if (result.getFinishTime() == null || result.getRank() == null) {
                        throw new AppException(ErrorCode.INVALID_RACE_RESULT_STATUS);
                    }
                }
                horseRatingService.validateRatingChange(
                        result.getRace(), resultStatus,
                        result.getRank(), result.getRatingChange());
            } else {
                RaceResult result = resultMap.get(entry.getEntryId());
                if (result != null) {
                    throw new AppException(ErrorCode.INVALID_RACE_RESULT_STATUS);
                }
            }
        }
    }

    @Override
    @Transactional
    public RaceReportResponse getAdminReport(UUID raceId) {
        raceRepository.findById(raceId)
                .orElseThrow(() -> new AppException(ErrorCode.RACE_NOT_FOUND));

        RaceReport report = raceReportRepository.findByRace_RaceId(raceId)
                .orElseThrow(() -> new AppException(ErrorCode.RACE_REPORT_NOT_FOUND));

        return raceReportMapper.toRaceReportResponse(report);
    }

    @Override
    @Transactional
    public RaceReportResponse publishReport(UUID raceId) {
        Race race = raceRepository.findById(raceId)
                .orElseThrow(() -> new AppException(ErrorCode.RACE_NOT_FOUND));

        if (race.getStatus() != RoundStatus.FINISHED) {
            throw new AppException(ErrorCode.INVALID_RACE_RESULT_STATUS);
        }

        RaceReport report = raceReportRepository.findForUpdateByRace_RaceId(raceId)
                .orElseThrow(() -> new AppException(ErrorCode.RACE_REPORT_NOT_FOUND));

        if (report.getStatus() == ReportStatus.PUBLISHED) {
            throw new AppException(ErrorCode.RACE_REPORT_ALREADY_PUBLISHED);
        }

        if (report.getStatus() != ReportStatus.SIGNED) {
            throw new AppException(ErrorCode.RACE_REPORT_NOT_SIGNED);
        }

        if (appealRepository.existsByEntry_Race_RaceIdAndStatus(raceId, AppealStatus.Pending)) {
            throw new AppException(ErrorCode.RACE_REPORT_PENDING_APPEAL);
        }

        User currentUser = userCurrentService.getCurrentUser();

        report.setStatus(ReportStatus.PUBLISHED);
        report.setPublishedBy(currentUser);
        report.setPublishedAt(LocalDateTime.now());
        raceReportRepository.save(report);

        race.setStatus(RoundStatus.COMPLETED);
        raceRepository.save(race);

        // 1. Chấm điểm prediction cho spectator (với mọi race)
        scoringService.scoreRace(raceId);

        // 2. Áp dụng điểm Rating thủ công đã được Head Referee xác nhận.
        horseRatingService.applyManualRatingsForPublish(raceId);

        // Chốt Final Round trước khi thực hiện các khoản chi cuối giải.
        completeFinalRoundIfPossible(race.getRound());

        // 3. Chia tiền thưởng (nếu là final round)
        payoutPrizeIfFinal(race);

        // 4. Chỉ giải ngân 70% khi report của đúng Final Race đã được publish.
        releaseJockeyFinalPayoutAfterFinalRacePublished(race);

        notificationEventService.resultPublished(race);

        advanceRoundIfPossible(race.getRound());

        return raceReportMapper.toRaceReportResponse(report);
    }

    private void payoutPrizeIfFinal(Race race) {
        if (race.getRound() == null || !race.getRound().isFinal()) {
            return;
        }

        if (race.getRound().getRaces() == null || race.getRound().getRaces().size() != 1) {
            throw new AppException(ErrorCode.INVALID_FINAL_ROUND_CONFIGURATION);
        }

        Tournament tournament = race.getRound().getTournament();
        List<PrizeStructure> prizes = prizeStructureRepository.findByTournament_TournamentId(tournament.getTournamentId());
        
        // Khóa các bản ghi RaceResult bi quan
        List<RaceResult> results = raceResultRepository.findForUpdateByRace_RaceId(race.getRaceId());

        for (RaceResult result : results) {
            if (result.getStatus() == RaceResultStatus.FINISHED && result.getRank() != null && result.getRank() <= 3 && !result.isPrizePaid()) {
                Optional<PrizeStructure> prizeOpt = Optional.empty();
                for (PrizeStructure candidate : prizes) {
                    if (candidate.getRank() == result.getRank()) {
                        prizeOpt = Optional.of(candidate);
                        break;
                    }
                }

                if (prizeOpt.isPresent()) {
                    PrizeStructure prize = prizeOpt.get();
                    BigDecimal totalPrizeAmount = BigDecimal.ZERO;
                    if (prize.getPercentage() != null && prize.getPercentage() > 0) {
                        totalPrizeAmount = tournament.getTotalPrizePool()
                                .multiply(BigDecimal.valueOf(prize.getPercentage()))
                                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
                    } else {
                        totalPrizeAmount = prize.getFixedAmount();
                    }

                    if (totalPrizeAmount.compareTo(BigDecimal.ZERO) > 0) {
                        JockeyHorseContract contract = jockeyHorseContractRepository.findForUpdateByContractId(result.getEntry().getContract().getContractId())
                                .orElseThrow(() -> new AppException(ErrorCode.CONTRACT_NOT_FOUND));

                        // Kiểm tra lại lớp bảo vệ: RaceResult chưa được trả thưởng
                        if (result.isPrizePaid()) {
                            continue;
                        }

                        BigDecimal ownerAmount = totalPrizeAmount.multiply(BigDecimal.valueOf(contract.getOwnerPrizeSharePercent()))
                                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
                        // Tính toán bằng cách trừ đi phần của Owner để luôn khớp tổng số tiền, tránh sai lệch do làm tròn
                        BigDecimal jockeyAmount = totalPrizeAmount.subtract(ownerAmount);

                        Wallet systemPrizeWallet = walletRepository.findForUpdateByOwnerTypeAndWalletPurpose(
                                WalletOwnerType.SYSTEM, WalletPurpose.SYSTEM_PRIZE_POOL)
                                .orElseThrow(() -> new AppException(ErrorCode.SYSTEM_WALLET_NOT_FOUND));

                        if (systemPrizeWallet.getBalance().compareTo(totalPrizeAmount) < 0) {
                            throw new AppException(ErrorCode.INSUFFICIENT_BALANCE);
                        }

                        Wallet ownerWallet = walletRepository.findForUpdateByUser_UserIdAndWalletPurpose(
                                contract.getOwner().getUser().getUserId(), WalletPurpose.USER_MAIN)
                                .orElseThrow(() -> new AppException(ErrorCode.WALLET_NOT_FOUND));

                        Wallet jockeyWallet = walletRepository.findForUpdateByUser_UserIdAndWalletPurpose(
                                contract.getJockey().getUser().getUserId(), WalletPurpose.USER_MAIN)
                                .orElseThrow(() -> new AppException(ErrorCode.WALLET_NOT_FOUND));

                        systemPrizeWallet.setBalance(systemPrizeWallet.getBalance().subtract(totalPrizeAmount));
                        ownerWallet.setBalance(ownerWallet.getBalance().add(ownerAmount));
                        jockeyWallet.setBalance(jockeyWallet.getBalance().add(jockeyAmount));

                        walletRepository.save(systemPrizeWallet);
                        walletRepository.save(ownerWallet);
                        walletRepository.save(jockeyWallet);

                        UUID txGroup = UUID.randomUUID();

                        Transaction debitTx = Transaction.builder()
                                .wallet(systemPrizeWallet)
                                .contractId(contract.getContractId())
                                .raceResultId(result.getResultId())
                                .type(TransactionType.PRIZE_OWNER_SHARE)
                                .direction(TransactionDirection.DEBIT)
                                .amount(totalPrizeAmount)
                                .balanceBefore(systemPrizeWallet.getBalance().add(totalPrizeAmount))
                                .balanceAfter(systemPrizeWallet.getBalance())
                                .counterpartyType(CounterpartyType.USER)
                                .transactionGroupId(txGroup)
                                .status(TransactionStatus.SUCCESS)
                                .note("Prize pool payout for rank " + result.getRank())
                                .build();

                        Transaction ownerTx = Transaction.builder()
                                .wallet(ownerWallet)
                                .contractId(contract.getContractId())
                                .raceResultId(result.getResultId())
                                .type(TransactionType.PRIZE_OWNER_SHARE)
                                .direction(TransactionDirection.CREDIT)
                                .amount(ownerAmount)
                                .balanceBefore(ownerWallet.getBalance().subtract(ownerAmount))
                                .balanceAfter(ownerWallet.getBalance())
                                .counterpartyWalletId(systemPrizeWallet.getWalletId())
                                .counterpartyType(CounterpartyType.SYSTEM)
                                .transactionGroupId(txGroup)
                                .status(TransactionStatus.SUCCESS)
                                .note("Owner prize share for rank " + result.getRank())
                                .build();

                        Transaction jockeyTx = Transaction.builder()
                                .wallet(jockeyWallet)
                                .contractId(contract.getContractId())
                                .raceResultId(result.getResultId())
                                .type(TransactionType.PRIZE_JOCKEY_SHARE)
                                .direction(TransactionDirection.CREDIT)
                                .amount(jockeyAmount)
                                .balanceBefore(jockeyWallet.getBalance().subtract(jockeyAmount))
                                .balanceAfter(jockeyWallet.getBalance())
                                .counterpartyWalletId(systemPrizeWallet.getWalletId())
                                .counterpartyType(CounterpartyType.SYSTEM)
                                .transactionGroupId(txGroup)
                                .status(TransactionStatus.SUCCESS)
                                .note("Jockey prize share for rank " + result.getRank())
                                .build();

                        walletTransactionRepository.save(debitTx);
                        walletTransactionRepository.save(ownerTx);
                        walletTransactionRepository.save(jockeyTx);

                        result.setPrizeMoney(totalPrizeAmount);
                        result.setOwnerPrizeAmount(ownerAmount);
                        result.setJockeyPrizeAmount(jockeyAmount);
                        result.setPrizeStatus(PrizeStatus.Paid);
                        result.setPrizePaid(true);
                        result.setPrizePaidAt(LocalDateTime.now());
                        RaceResult savedResult = raceResultRepository.save(result);
                        notificationEventService.prizeReceived(savedResult);
                    }
                }
            }
        }
    }

    private void releaseJockeyFinalPayoutAfterFinalRacePublished(Race race) {
        if (race.getRound() == null || !race.getRound().isFinal()) {
            return;
        }

        if (race.getRound().getRaces() == null || race.getRound().getRaces().size() != 1) {
            throw new AppException(ErrorCode.INVALID_FINAL_ROUND_CONFIGURATION);
        }

        Tournament tournament = race.getRound().getTournament();
        List<JockeyHorseContract> tournamentContracts = jockeyHorseContractRepository.findByTournament_TournamentIdAndStatusAndEscrowStatus(
                tournament.getTournamentId(), ContractStatus.APPROVED, EscrowStatus.PARTIALLY_RELEASED);

        for (JockeyHorseContract c : tournamentContracts) {
            JockeyHorseContract contract = jockeyHorseContractRepository.findForUpdateByContractId(c.getContractId())
                    .orElseThrow(() -> new AppException(ErrorCode.CONTRACT_NOT_FOUND));

            // Kiểm tra bảo vệ chống giải ngân trùng lặp (finalPayoutStatus != RELEASED)
            if (contract.getFinalPayoutStatus() != FinalPayoutStatus.RELEASED && contract.getEscrowStatus() == EscrowStatus.PARTIALLY_RELEASED) {
                contractService.releaseFinalPayoutAfterFinalRacePublished(
                        contract.getContractId(), race.getRaceId());
            }
        }
    }

    @Override
    public RaceReportResponse getPublishedReport(UUID raceId) {
        RaceReport report = raceReportRepository.findByRace_RaceId(raceId)
                .orElseThrow(() -> new AppException(ErrorCode.RACE_REPORT_NOT_FOUND));

        if (report.getStatus() != ReportStatus.PUBLISHED) {
            throw new AppException(ErrorCode.RACE_REPORT_NOT_PUBLISHED);
        }

        return raceReportMapper.toRaceReportResponse(report);
    }

    private void advanceRoundIfPossible(Round round) {
        if (round == null || round.isFinal()) {
            return;
        }

        Round lockedRound = roundRepository.findForUpdateByRoundId(round.getRoundId())
                .orElseThrow(() -> new AppException(ErrorCode.ROUND_NOT_FOUND));
        if (lockedRound.getAdvancedAt() != null
                || lockedRound.getTransitionStatus() == RoundTransitionStatus.COMPLETED) {
            return;
        }

        List<Race> roundRaces = raceRepository
                .findByRound_RoundIdOrderBySequenceOrderAsc(lockedRound.getRoundId());
        if (roundRaces.isEmpty()) {
            throw new AppException(ErrorCode.ROUND_STRUCTURE_MISMATCH);
        }
        for (Race race : roundRaces) {
            if (race.getStatus() == RoundStatus.CANCELLED) {
                blockRoundTransition(lockedRound);
                return;
            }
            if (race.getStatus() != RoundStatus.COMPLETED) {
                lockedRound.setTransitionStatus(RoundTransitionStatus.NOT_READY);
                roundRepository.save(lockedRound);
                return;
            }
            RaceReport report = raceReportRepository.findByRace_RaceId(race.getRaceId()).orElse(null);
            if (report == null || report.getStatus() != ReportStatus.PUBLISHED) {
                lockedRound.setTransitionStatus(RoundTransitionStatus.NOT_READY);
                roundRepository.save(lockedRound);
                return;
            }
        }

        lockedRound.setStatus(RoundStatus.COMPLETED);

        Round nextRound = roundRepository.findByTournament_TournamentIdAndSequenceOrder(
                lockedRound.getTournament().getTournamentId(), lockedRound.getSequenceOrder() + 1
        ).orElseThrow(() -> new AppException(ErrorCode.ROUND_STRUCTURE_MISMATCH));
        List<Race> nextRoundRaces = raceRepository
                .findByRound_RoundIdOrderBySequenceOrderAsc(nextRound.getRoundId());
        if (roundRaces.size() % 2 != 0 || nextRoundRaces.size() != roundRaces.size() / 2) {
            throw new AppException(ErrorCode.ROUND_STRUCTURE_MISMATCH);
        }
        if (raceEntryRepository.countByRace_Round_RoundId(nextRound.getRoundId()) != 0) {
            throw new AppException(ErrorCode.ROUND_STRUCTURE_MISMATCH);
        }

        List<List<JockeyHorseContract>> qualifiersByRace = new ArrayList<>();
        Set<UUID> contractIds = new HashSet<>();
        Set<UUID> horseIds = new HashSet<>();
        Set<UUID> jockeyIds = new HashSet<>();
        for (Race race : roundRaces) {
            List<RaceResult> qualifiers = getQualifiers(race);
            if (qualifiers.size() < 4) {
                blockRoundTransition(lockedRound);
                return;
            }
            List<JockeyHorseContract> contracts = new ArrayList<>();
            for (int index = 0; index < 4; index++) {
                JockeyHorseContract contract = qualifiers.get(index).getEntry().getContract();
                if (!contractIds.add(contract.getContractId())
                        || !horseIds.add(contract.getHorse().getHorseId())
                        || !jockeyIds.add(contract.getJockey().getJockeyId())) {
                    throw new AppException(ErrorCode.ROUND_STRUCTURE_MISMATCH);
                }
                contracts.add(contract);
            }
            qualifiersByRace.add(contracts);
        }

        lockedRound.setTransitionStatus(RoundTransitionStatus.READY);
        User admin = lockedRound.getCreatedBy();
        for (int targetIndex = 0; targetIndex < nextRoundRaces.size(); targetIndex++) {
            Race targetRace = nextRoundRaces.get(targetIndex);
            List<JockeyHorseContract> leftQualifiers = qualifiersByRace.get(targetIndex * 2);
            List<JockeyHorseContract> rightQualifiers = qualifiersByRace.get(targetIndex * 2 + 1);
            for (int qualifierIndex = 0; qualifierIndex < 4; qualifierIndex++) {
                raceEntryRepository.save(buildAdvancedEntry(
                        targetRace, leftQualifiers.get(qualifierIndex), admin));
                raceEntryRepository.save(buildAdvancedEntry(
                        targetRace, rightQualifiers.get(qualifierIndex), admin));
            }
        }

        lockedRound.setAdvancedAt(LocalDateTime.now());
        lockedRound.setTransitionStatus(RoundTransitionStatus.COMPLETED);
        roundRepository.save(lockedRound);
        nextRound.setStatus(RoundStatus.SCHEDULING);
        roundRepository.save(nextRound);

        Tournament tournament = lockedRound.getTournament();
        tournament.setPhase(TournamentPhase.SCHEDULING);
        tournament.setStatus(TournamentStatus.ONGOING);
        tournament.setCurrentRoundName(nextRound.getRoundName());
        tournamentRepository.save(tournament);
    }

    private List<RaceResult> getQualifiers(Race race) {
        List<RaceResult> qualifiers = new ArrayList<>();
        for (RaceResult result : raceResultRepository.findByRace_RaceIdOrderByRankAsc(race.getRaceId())) {
            if (result.getStatus() == RaceResultStatus.FINISHED && result.getRank() != null) {
                qualifiers.add(result);
                if (qualifiers.size() == 4) {
                    break;
                }
            }
        }
        return qualifiers;
    }

    private void markRoundFinishedIfAllRacesFinished(Round round) {
        if (round == null || round.getStatus() == RoundStatus.COMPLETED) {
            return;
        }

        List<Race> races = raceRepository.findByRound_RoundIdOrderBySequenceOrderAsc(round.getRoundId());
        if (races.isEmpty()) {
            return;
        }

        for (Race race : races) {
            if (race.getStatus() != RoundStatus.FINISHED
                    && race.getStatus() != RoundStatus.COMPLETED) {
                return;
            }
        }

        round.setStatus(RoundStatus.FINISHED);
        roundRepository.save(round);
    }

    private void completeFinalRoundIfPossible(Round round) {
        if (round == null || !round.isFinal() || round.getStatus() == RoundStatus.COMPLETED) {
            return;
        }

        Round lockedRound = roundRepository.findForUpdateByRoundId(round.getRoundId())
                .orElseThrow(() -> new AppException(ErrorCode.ROUND_NOT_FOUND));
        List<Race> races = raceRepository
                .findByRound_RoundIdOrderBySequenceOrderAsc(lockedRound.getRoundId());
        if (races.isEmpty()) {
            return;
        }

        for (Race race : races) {
            if (race.getStatus() != RoundStatus.COMPLETED) {
                return;
            }
            RaceReport report = raceReportRepository.findByRace_RaceId(race.getRaceId()).orElse(null);
            if (report == null || report.getStatus() != ReportStatus.PUBLISHED) {
                return;
            }
        }

        lockedRound.setStatus(RoundStatus.COMPLETED);
        roundRepository.save(lockedRound);

        Tournament tournament = lockedRound.getTournament();
        tournament.setPhase(TournamentPhase.RESULT_PENDING);
        tournament.setStatus(TournamentStatus.ONGOING);
        tournamentRepository.save(tournament);
    }

    private RaceEntry buildAdvancedEntry(Race race, JockeyHorseContract contract,
                                          User assignedBy) {
        return RaceEntry.builder()
                .race(race)
                .contract(contract)
                .laneNumber(null)
                .status(RaceEntryStatus.CONFIRMED)
                .assignedBy(assignedBy)
                .assignedAt(LocalDateTime.now())
                .build();
    }

    private void blockRoundTransition(Round round) {
        round.setTransitionStatus(RoundTransitionStatus.BLOCKED_NOT_ENOUGH_QUALIFIERS);
        roundRepository.save(round);
        notificationEventService.roundTransitionBlocked(round);
    }

    @Override
    @Transactional(readOnly = true)
    public List<RoundQualifierResponse> getRoundQualifiers(UUID roundId) {
        Round round = roundRepository.findById(roundId)
                .orElseThrow(() -> new AppException(ErrorCode.ROUND_NOT_FOUND));

        List<RoundQualifierResponse> responses = new ArrayList<>();
        if (round.isFinal()) {
            return responses;
        }
        if (round.getTransitionStatus() == RoundTransitionStatus.BLOCKED_NOT_ENOUGH_QUALIFIERS) {
            throw new AppException(ErrorCode.NEXT_ROUND_NOT_ENOUGH_QUALIFIERS);
        }
        if (round.getTransitionStatus() != RoundTransitionStatus.COMPLETED) {
            throw new AppException(ErrorCode.ROUND_REPORTS_NOT_FULLY_PUBLISHED);
        }

        List<Race> sourceRaces = raceRepository
                .findByRound_RoundIdOrderBySequenceOrderAsc(roundId);
        validatePublishedRoundForQualifiers(sourceRaces);

        Round nextRound = roundRepository.findByTournament_TournamentIdAndSequenceOrder(
                        round.getTournament().getTournamentId(), round.getSequenceOrder() + 1)
                .orElseThrow(() -> new AppException(ErrorCode.ROUND_STRUCTURE_MISMATCH));

        List<RaceEntry> nextRoundEntries = raceEntryRepository
                .findByRace_Round_RoundId(nextRound.getRoundId());
        Map<UUID, RaceEntry> nextEntryByContractId = new HashMap<>();
        for (RaceEntry entry : nextRoundEntries) {
            UUID contractId = entry.getContract().getContractId();
            if (nextEntryByContractId.put(contractId, entry) != null) {
                throw new AppException(ErrorCode.ROUND_STRUCTURE_MISMATCH);
            }
        }

        for (Race sourceRace : sourceRaces) {
            List<RaceResult> qualifiers = getQualifiers(sourceRace);
            if (qualifiers.size() != 4) {
                throw new AppException(ErrorCode.NEXT_ROUND_NOT_ENOUGH_QUALIFIERS);
            }

            for (RaceResult qualifier : qualifiers) {
                JockeyHorseContract contract = qualifier.getEntry().getContract();
                RaceEntry nextEntry = nextEntryByContractId.get(contract.getContractId());
                if (nextEntry == null) {
                    throw new AppException(ErrorCode.ROUND_STRUCTURE_MISMATCH);
                }

                responses.add(RoundQualifierResponse.builder()
                        .sourceRoundId(round.getRoundId())
                        .sourceRaceId(sourceRace.getRaceId())
                        .sourceRaceName(sourceRace.getName())
                        .sourceRaceSequence(sourceRace.getSequenceOrder())
                        .sourceEntryId(qualifier.getEntry().getEntryId())
                        .rank(qualifier.getRank())
                        .contractId(contract.getContractId())
                        .horseId(contract.getHorse().getHorseId())
                        .horseName(contract.getHorse().getName())
                        .jockeyId(contract.getJockey().getJockeyId())
                        .jockeyName(contract.getJockey().getUser().getFullName())
                        .nextRoundId(nextRound.getRoundId())
                        .nextRaceId(nextEntry.getRace().getRaceId())
                        .nextLaneNumber(nextEntry.getLaneNumber())
                        .build());
            }
        }

        return responses;
    }

    private void validatePublishedRoundForQualifiers(List<Race> races) {
        if (races.isEmpty()) {
            throw new AppException(ErrorCode.ROUND_STRUCTURE_MISMATCH);
        }

        for (Race race : races) {
            if (race.getStatus() != RoundStatus.COMPLETED) {
                throw new AppException(ErrorCode.ROUND_REPORTS_NOT_FULLY_PUBLISHED);
            }
            RaceReport report = raceReportRepository.findByRace_RaceId(race.getRaceId()).orElse(null);
            if (report == null || report.getStatus() != ReportStatus.PUBLISHED) {
                throw new AppException(ErrorCode.ROUND_REPORTS_NOT_FULLY_PUBLISHED);
            }
        }
    }

    @Override
    public List<RaceResultResponse> getRaceRanking(UUID raceId) {
        raceRepository.findById(raceId)
                .orElseThrow(() -> new AppException(ErrorCode.RACE_NOT_FOUND));

        List<RaceResultResponse> responses = new ArrayList<>();
        List<RaceResult> results =
                raceResultRepository.findByRace_RaceIdOrderByRankAsc(raceId);
        for (RaceResult result : results) {
            responses.add(raceResultMapper.toRaceResultResponse(result));
        }
        return responses;
    }

}

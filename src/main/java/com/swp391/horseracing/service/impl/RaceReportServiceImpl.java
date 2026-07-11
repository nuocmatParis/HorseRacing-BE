package com.swp391.horseracing.service.impl;

import com.swp391.horseracing.dto.race_report.response.RaceReportResponse;
import com.swp391.horseracing.dto.race_result.response.RaceResultResponse;
import com.swp391.horseracing.entity.*;
import com.swp391.horseracing.enums.*;
import com.swp391.horseracing.service.*;
import com.swp391.horseracing.exception.AppException;
import com.swp391.horseracing.exception.ErrorCode;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Optional;
import com.swp391.horseracing.mapper.RaceReportMapper;
import com.swp391.horseracing.mapper.RaceResultMapper;
import com.swp391.horseracing.repository.*;
import com.swp391.horseracing.service.NotificationService;
import com.swp391.horseracing.service.RaceReportService;
import com.swp391.horseracing.service.ScoringService;
import com.swp391.horseracing.service.UserCurrentService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
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
    NotificationService notificationService;
    ScoringService scoringService;
    PrizeStructureRepository prizeStructureRepository;
    WalletRepository walletRepository;
    WalletTransactionRepository walletTransactionRepository;
    ContractService contractService;
    JockeyHorseContractRepository jockeyHorseContractRepository;

    @Override
    @Transactional
    public RaceReportResponse getRefereeReport(UUID raceId) {
        Race race = raceRepository.findById(raceId)
                .orElseThrow(() -> new AppException(ErrorCode.RACE_NOT_FOUND));

        User currentUser = userCurrentService.getCurrentUser();
        Referee referee = refereeRepository.findByUser_UserId(currentUser.getUserId())
                .orElseThrow(() -> new AppException(ErrorCode.REFEREE_PROFILE_NOT_FOUND));

        if (!raceRefereeRepository.existsByRace_RaceIdAndReferee_RefereeId(raceId, referee.getRefereeId())) {
            throw new AppException(ErrorCode.RACE_REFEREE_NOT_FOUND);
        }

        if (!raceResultRepository.existsByRace_RaceId(raceId)) {
            throw new AppException(ErrorCode.RACE_RESULT_NOT_FOUND);
        }

        RaceReport report = raceReportRepository.findByRace_RaceId(raceId)
                .orElseGet(() -> {
                    RaceReport newReport = RaceReport.builder()
                            .race(race)
                            .referee(referee)
                            .summary("")
                            .status(ReportStatus.Draft)
                            .build();
                    return raceReportRepository.save(newReport);
                });

        return raceReportMapper.toRaceReportResponse(report);
    }

    @Override
    @Transactional
    public RaceReportResponse signReport(UUID raceId, UUID refereeId) {
        Race race = raceRepository.findById(raceId)
                .orElseThrow(() -> new AppException(ErrorCode.RACE_NOT_FOUND));

        if (race.getStatus() != RoundStatus.FINISHED && race.getStatus() != RoundStatus.ONGOING) {
            throw new AppException(ErrorCode.INVALID_RACE_RESULT_STATUS);
        }

        User currentUser = userCurrentService.getCurrentUser();
        Referee referee = refereeRepository.findByUser_UserId(currentUser.getUserId())
                .orElseThrow(() -> new AppException(ErrorCode.REFEREE_PROFILE_NOT_FOUND));

        Round round = race.getRound();
        if (round.getHeadReferee() == null || !round.getHeadReferee().getRefereeId().equals(referee.getRefereeId())) {
            throw new AppException(ErrorCode.ACCESS_DENIED);
        }

        if (!raceResultRepository.existsByRace_RaceId(raceId)) {
            throw new AppException(ErrorCode.RACE_RESULT_NOT_FOUND);
        }

        boolean hasPendingAppeals = appealRepository.existsByEntry_Race_RaceIdAndStatus(
                raceId, AppealStatus.Pending);
        if (hasPendingAppeals) {
            throw new AppException(ErrorCode.APPEAL_NOT_PENDING);
        }

        RaceReport report = raceReportRepository.findByRace_RaceId(raceId)
                .orElseThrow(() -> new AppException(ErrorCode.RACE_REPORT_NOT_FOUND));

        if (report.getStatus() != ReportStatus.Draft) {
            throw new AppException(ErrorCode.RACE_REPORT_ALREADY_SIGNED);
        }

        report.setStatus(ReportStatus.Signed);
        report.setSignedBy(referee);
        report.setSignedAt(LocalDateTime.now());

        return raceReportMapper.toRaceReportResponse(raceReportRepository.save(report));
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

        RaceReport report = raceReportRepository.findByRace_RaceId(raceId)
                .orElseThrow(() -> new AppException(ErrorCode.RACE_REPORT_NOT_FOUND));

        if (report.getStatus() != ReportStatus.Signed) {
            throw new AppException(ErrorCode.RACE_REPORT_NOT_SIGNED);
        }

        User currentUser = userCurrentService.getCurrentUser();

        report.setStatus(ReportStatus.Published);
        report.setPublishedBy(currentUser);
        report.setPublishedAt(LocalDateTime.now());
        raceReportRepository.save(report);

        race.setStatus(RoundStatus.COMPLETED);
        raceRepository.save(race);

        // Prize Payout & Release Jockey Escrow
        if (race.getRound().isFinal()) {
            Tournament tournament = race.getRound().getTournament();
            List<PrizeStructure> prizes = prizeStructureRepository.findByTournament_TournamentId(tournament.getTournamentId());
            List<RaceResult> results = raceResultRepository.findByRace_RaceId(raceId);

            for (RaceResult result : results) {
                if (result.getStatus() == RaceResultStatus.FINISHED && result.getRank() != null && !result.isPrizePaid()) {
                    Optional<PrizeStructure> prizeOpt = prizes.stream()
                            .filter(p -> p.getRank() == result.getRank())
                            .findFirst();

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
                            JockeyHorseContract contract = result.getEntry().getContract();
                            BigDecimal ownerAmount = totalPrizeAmount.multiply(BigDecimal.valueOf(contract.getOwnerPrizeSharePercent()))
                                    .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
                            BigDecimal jockeyAmount = totalPrizeAmount.multiply(BigDecimal.valueOf(contract.getJockeyPrizeSharePercent()))
                                    .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);

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
                            raceResultRepository.save(result);
                        }
                    }
                }

                JockeyHorseContract contract = result.getEntry().getContract();
                if (contract.getStatus() == ContractStatus.APPROVED && contract.getEscrowStatus() == EscrowStatus.PARTIALLY_RELEASED) {
                    contractService.releaseFinalPayout(contract.getContractId());
                }
            }

            boolean allFinalRacesCompleted = true;
            for (Race r : race.getRound().getRaces()) {
                RoundStatus rStatus = r.getRaceId().equals(raceId) ? RoundStatus.COMPLETED : r.getStatus();
                if (rStatus != RoundStatus.COMPLETED && rStatus != RoundStatus.CANCELLED) {
                    allFinalRacesCompleted = false;
                    break;
                }
            }
            if (allFinalRacesCompleted) {
                List<JockeyHorseContract> tournamentContracts = jockeyHorseContractRepository.findByTournament_TournamentIdAndStatusAndEscrowStatus(
                        tournament.getTournamentId(), ContractStatus.APPROVED, EscrowStatus.PARTIALLY_RELEASED);
                for (JockeyHorseContract c : tournamentContracts) {
                    contractService.releaseFinalPayout(c.getContractId());
                }
            }
        }

        scoringService.scoreRace(raceId);

        sendNotificationsForPublishedReport(race);

        return raceReportMapper.toRaceReportResponse(report);
    }

    @Override
    public RaceReportResponse getPublishedReport(UUID raceId) {
        RaceReport report = raceReportRepository.findByRace_RaceId(raceId)
                .orElseThrow(() -> new AppException(ErrorCode.RACE_REPORT_NOT_FOUND));

        if (report.getStatus() != ReportStatus.Published) {
            throw new AppException(ErrorCode.RACE_REPORT_NOT_FOUND);
        }

        return raceReportMapper.toRaceReportResponse(report);
    }

    @Override
    public List<RaceResultResponse> getRaceRanking(UUID raceId) {
        raceRepository.findById(raceId)
                .orElseThrow(() -> new AppException(ErrorCode.RACE_NOT_FOUND));

        return raceResultRepository.findByRace_RaceIdOrderByRankAsc(raceId)
                .stream()
                .map(raceResultMapper::toRaceResultResponse)
                .toList();
    }

    private void sendNotificationsForPublishedReport(Race race) {
        List<RaceEntry> entries = raceEntryRepository.findByRace_RaceIdOrderByLaneNumberAsc(
                race.getRaceId());

        for (RaceEntry entry : entries) {
            UUID ownerUserId = entry.getContract().getOwner().getUser().getUserId();
            UUID jockeyUserId = entry.getContract().getJockey().getUser().getUserId();

            notificationService.sendNotification(
                    ownerUserId,
                    "Race Result Published",
                    "Race \"" + race.getName() + "\" results have been published.",
                    NotificationType.ResultPublished,
                    "Race",
                    race.getRaceId()
            );

            notificationService.sendNotification(
                    jockeyUserId,
                    "Race Result Published",
                    "Race \"" + race.getName() + "\" results have been published.",
                    NotificationType.ResultPublished,
                    "Race",
                    race.getRaceId()
            );
        }
    }
}

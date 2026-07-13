package com.swp391.horseracing.service.impl;

import com.swp391.horseracing.dto.contract.request.InviteRequest;
import com.swp391.horseracing.dto.contract.response.ContractResponse;
import com.swp391.horseracing.dto.invoice.response.PaymentResponse;
import com.swp391.horseracing.entity.*;
import com.swp391.horseracing.enums.*;
import com.swp391.horseracing.exception.AppException;
import com.swp391.horseracing.exception.ErrorCode;
import com.swp391.horseracing.mapper.ContractMapper;
import com.swp391.horseracing.repository.*;
import com.swp391.horseracing.service.ContractService;
import com.swp391.horseracing.service.InvoiceService;
import com.swp391.horseracing.service.PaymentService;
import com.swp391.horseracing.service.UserCurrentService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;

@Service
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequiredArgsConstructor
public class ContractServiceImpl implements ContractService {

    JockeyHorseContractRepository contractRepository;
    HorseTournamentRegistrationRepository horseTournamentRegistrationRepository;
    JockeyTournamentRegistrationRepository jockeyTournamentRegistrationRepository;
    ContractMapper contractMapper;
    UserCurrentService userCurrentService;
    InvoiceService invoiceService;
    InvoiceRepository invoiceRepository;
    PaymentService paymentService;
    WalletRepository walletRepository;
    WalletTransactionRepository walletTransactionRepository;
    TournamentRepository tournamentRepository;
    RaceEntryRepository raceEntryRepository;

    @Override
    @Transactional
    public ContractResponse inviteJockey(InviteRequest request) {
        HorseOwner currentOwner = userCurrentService.getCurrentOwner();

        HorseTournamentRegistration horseTournamentRegistration = horseTournamentRegistrationRepository
                .findById(request.getTournamentRegistrationId()).orElseThrow(()
                        -> new AppException(ErrorCode.TOURNAMENT_REGISTRATION_NOT_FOUND));

        JockeyTournamentRegistration jockeyTournamentRegistration = jockeyTournamentRegistrationRepository
                .findById(request.getJockeyTournamentRegistrationId()).orElseThrow(()
                        -> new AppException(ErrorCode.TOURNAMENT_REGISTRATION_NOT_FOUND));

        validateInvite(currentOwner, horseTournamentRegistration, jockeyTournamentRegistration, request);

        Tournament tournament = horseTournamentRegistration.getTournament();

        if (tournament.getPhase() != TournamentPhase.JOCKEY_MATCHING) {
            throw new AppException(ErrorCode.INVALID_PHASE_TRANSITION);
        }

        JockeyHorseContract contract = JockeyHorseContract.builder()
                        .tournament(tournament)
                        .horseTournamentRegistration(horseTournamentRegistration)
                        .jockeyTournamentRegistration(jockeyTournamentRegistration)
                        .owner(currentOwner)
                        .horse(horseTournamentRegistration.getHorse())
                        .jockey(jockeyTournamentRegistration.getJockey())
                        .hireFee(jockeyTournamentRegistration.getHireFee())
                        .systemContractFee(tournament.getSystemContractFee())
                        .advancePercent(30F)
                        .finalPercent(70F)
                        .ownerPrizeSharePercent(request.getOwnerPrizeSharePercent())
                        .jockeyPrizeSharePercent(request.getJockeyPrizeSharePercent())
                        .advancePaidAmount(BigDecimal.ZERO)
                        .escrowAmount(BigDecimal.ZERO)
                        .paymentStatus(ContractPaymentStatus.UNPAID)
                        .escrowStatus(EscrowStatus.NOT_HELD)
                        .advancePayoutStatus(AdvancePayoutStatus.NOT_PAID)
                        .finalPayoutStatus(FinalPayoutStatus.NOT_RELEASED)
                        .status(ContractStatus.PENDING_JOCKEY)
                        .requestedAt(LocalDateTime.now())
                        .contractNote(request.getContractNote())
                        .build();

        JockeyHorseContract savedContract = contractRepository.save(contract);

        return contractMapper.toContractResponse(savedContract);
    }

    @Override
    public List<ContractResponse> getMyInvitations() {
        User currentUser = userCurrentService.getCurrentUser();

        List<JockeyHorseContract> list = contractRepository.findByJockey_User_UserIdAndStatusOrderByRequestedAtDesc(
                currentUser.getUserId(), ContractStatus.PENDING_JOCKEY);

        List<ContractResponse> responseList = new ArrayList<>();

        for(JockeyHorseContract contract : list){
            responseList.add(contractMapper.toContractResponse(contract));
        }

        return responseList;
    }

    @Override
    @Transactional
    public ContractResponse acceptContract(UUID contractId) {
        User currentUser = userCurrentService.getCurrentUser();

        JockeyHorseContract contract = contractRepository.findForUpdateByContractId(contractId).orElseThrow(()
                -> new AppException(ErrorCode.CONTRACT_NOT_FOUND));

        if(!contract.getJockey().getUser().getUserId().equals(currentUser.getUserId()))
            throw new AppException(ErrorCode.UNAUTHENTICATED);

        if(contract.getStatus() != ContractStatus.PENDING_JOCKEY)
            throw new AppException(ErrorCode.INVALID_CONTRACT_STATUS);

        HorseTournamentRegistration horseTournamentRegistration = horseTournamentRegistrationRepository.findForUpdateById(
                contract.getHorseTournamentRegistration().getHorseRegistrationId()).orElseThrow(()
                -> new AppException(ErrorCode.TOURNAMENT_REGISTRATION_NOT_FOUND));

        JockeyTournamentRegistration jockeyTournamentRegistration = jockeyTournamentRegistrationRepository.findForUpdateById(
                contract.getJockeyTournamentRegistration().getJockeyTournamentRegId()).orElseThrow(()
                -> new AppException(ErrorCode.TOURNAMENT_REGISTRATION_NOT_FOUND));

        if(horseTournamentRegistration.getStatus() != RegistrationStatus.APPROVED)
            throw new AppException(ErrorCode.INVALID_REGISTRATION_STATUS);

        if(jockeyTournamentRegistration.getStatus() != RegistrationStatus.APPROVED)
            throw new AppException(ErrorCode.INVALID_REGISTRATION_STATUS);

        LocalDateTime now = LocalDateTime.now();
        contract.setStatus(ContractStatus.ACCEPTED);
        contract.setRespondedAt(now);
        contract.setAcceptedAt(now);

        cancelOtherInvite(contract);

        JockeyHorseContract savedContract = contractRepository.save(contract);

        invoiceService.createHiringFeeInvoice(horseTournamentRegistration.getOwner().getUser().getUserId(),
                savedContract.getContractId(), jockeyTournamentRegistration.getHireFee());

        return contractMapper.toContractResponse(savedContract);
    }

    private void cancelOtherInvite(JockeyHorseContract contract){
        List<JockeyHorseContract> sameHorseContracts = contractRepository.findByHorseTournamentRegistration_HorseRegistrationIdAndStatus(
                contract.getHorseTournamentRegistration().getHorseRegistrationId(), ContractStatus.PENDING_JOCKEY);

        List<JockeyHorseContract> sameJockeyContracts = contractRepository.findByJockeyTournamentRegistration_JockeyTournamentRegIdAndStatus(
                contract.getJockeyTournamentRegistration().getJockeyTournamentRegId(), ContractStatus.PENDING_JOCKEY);

        Map<UUID, JockeyHorseContract> cancelContracts = new LinkedHashMap<>();

        for(JockeyHorseContract horseContract : sameHorseContracts){
            cancelContracts.put(horseContract.getContractId(), horseContract);
        }

        for(JockeyHorseContract jockeyContract : sameJockeyContracts){
            cancelContracts.put(jockeyContract.getContractId(), jockeyContract);
        }

        cancelContracts.remove(contract.getContractId());

        for(JockeyHorseContract contract1 : cancelContracts.values()){
            contract1.setStatus(ContractStatus.CANCELLED);
            contract1.setCancelledAt(LocalDateTime.now());
            contract1.setCancelReason("Jockey was accept with another contract");
        }

        contractRepository.saveAll(cancelContracts.values());
    }
    @Override
    @Transactional
    public ContractResponse rejectContractByJockey(UUID contractId, String reason) {
        User currentUser = userCurrentService.getCurrentUser();

        JockeyHorseContract contract = contractRepository.findForUpdateByContractId(contractId).orElseThrow(()
                -> new AppException(ErrorCode.CONTRACT_NOT_FOUND));

        if(!contract.getJockey().getUser().getUserId().equals(currentUser.getUserId()))
            throw new AppException(ErrorCode.UNAUTHENTICATED);

        contract.setStatus(ContractStatus.REJECTED);
        contract.setRespondedAt(LocalDateTime.now());

        contract.setRejectedReason(reason);

        return contractMapper.toContractResponse(contractRepository.save(contract));
    }

    @Override
    public PaymentResponse payHiringFee(UUID contractId) {
        return null;
    }

    @Override
    @Transactional
    public PaymentResponse payContractCreationFee(UUID contractId) {
        JockeyHorseContract contract = contractRepository.findById(contractId).orElseThrow(
                () -> new AppException(ErrorCode.CONTRACT_NOT_FOUND));

        HorseOwner owner = userCurrentService.getCurrentOwner();

        if(!contract.getOwner().getUser().getUserId().equals(owner.getOwnerId()))
            throw new AppException(ErrorCode.UNAUTHENTICATED);

        if (contract.getStatus() != ContractStatus.HIRING_PAID)
            throw new AppException(ErrorCode.INVALID_CONTRACT_STATUS);

        Invoice invoice = invoiceRepository.findByContractIdAndInvoiceType(contractId, InvoiceType.CONTRACT_CREATION_FEE).orElseThrow(
                () -> new AppException(ErrorCode.INVOICE_NOT_FOUND));

        return paymentService.payInvoice(invoice.getInvoiceId());

    }

    @Override
    public List<ContractResponse> getPendingContracts() {
        List<ContractResponse> responseList = new ArrayList<>();

        List<JockeyHorseContract> contracts = contractRepository.findByStatusOrderByRequestedAtDesc(ContractStatus.PENDING_ADMIN_REVIEW);

        for(JockeyHorseContract contract : contracts){
            responseList.add(contractMapper.toContractResponse(contract));
        }

        return responseList;
    }

    /*
    *
    SYSTEM_ESCROW debit 30%
    Jockey USER_MAIN credit 30%

    Contract:
    status = APPROVED
    advancePaidAmount = 30%
    escrowAmount = 70%
    escrowStatus = PARTIALLY_RELEASED
    advancePayoutStatus = PAID
    *
    **/
    @Override
    public ContractResponse approveContract(UUID contractId) {
        User admin = userCurrentService.getCurrentUser();

        JockeyHorseContract contract = contractRepository.findForUpdateByContractId(contractId).orElseThrow(()
                -> new AppException(ErrorCode.CONTRACT_NOT_FOUND));

        if (contract.getStatus() != ContractStatus.PENDING_ADMIN_REVIEW)
            throw new AppException(ErrorCode.INVALID_CONTRACT_STATUS);

        if (contract.getPaymentStatus() != ContractPaymentStatus.PAID)
            throw new AppException(ErrorCode.CONTRACT_HIRING_FEE_NOT_PAID);

        if (contract.getEscrowStatus() != EscrowStatus.HELD)
            throw new AppException(ErrorCode.INVALID_ESCROW_STATUS);

        BigDecimal advanceAmount = calculateAdvanceAmount(contract);

        releaseAdvancePayoutToJockey(contract, advanceAmount);

        BigDecimal remainingEscrow = contract.getHireFee().subtract(advanceAmount);

        LocalDateTime now = LocalDateTime.now();

        contract.setStatus(ContractStatus.APPROVED);

        contract.setAdvancePaidAmount(advanceAmount);

        contract.setEscrowAmount(remainingEscrow);

        contract.setEscrowStatus(EscrowStatus.PARTIALLY_RELEASED);

        contract.setAdvancePayoutStatus(AdvancePayoutStatus.PAID);

        contract.setAdvancePayoutAt(now);

        contract.setReviewedBy(admin);
        contract.setReviewedAt(now);

        return contractMapper.toContractResponse(contractRepository.save(contract));
    }

    private BigDecimal calculateAdvanceAmount(JockeyHorseContract contract){
        BigDecimal percent = BigDecimal.valueOf(contract.getAdvancePercent());

        return contract.getHireFee().multiply(percent).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
    }

    private void releaseAdvancePayoutToJockey(JockeyHorseContract contract, BigDecimal amount){
        Wallet systemEscrowWallet = walletRepository.findForUpdateByOwnerTypeAndWalletPurpose(
                WalletOwnerType.SYSTEM, WalletPurpose.SYSTEM_ESCROW).orElseThrow(()
                -> new AppException(ErrorCode.SYSTEM_WALLET_NOT_FOUND));

        Wallet jockeyWallet =
                walletRepository.findForUpdateByUser_UserIdAndWalletPurpose(
                        contract.getJockey().getUser().getUserId(), WalletPurpose.USER_MAIN).orElseThrow(()
                        -> new AppException(ErrorCode.WALLET_NOT_FOUND));

        if (systemEscrowWallet.getBalance().compareTo(amount) < 0)
            throw new AppException(ErrorCode.INSUFFICIENT_BALANCE);

        UUID transactionGroupId = UUID.randomUUID();

        BigDecimal systemBalanceBefore = systemEscrowWallet.getBalance();
        BigDecimal systemBalanceAfter = systemBalanceBefore.subtract(amount);

        BigDecimal jockeyBalanceBefore = jockeyWallet.getBalance();
        BigDecimal jockeyBalanceAfter = jockeyBalanceBefore.add(amount);

        systemEscrowWallet.setBalance(systemBalanceAfter);

        jockeyWallet.setBalance(jockeyBalanceAfter);

        walletRepository.save(systemEscrowWallet);
        walletRepository.save(jockeyWallet);

        Transaction systemTransaction = Transaction.builder()
                .wallet(systemEscrowWallet)
                .contractId(contract.getContractId())
                .type(TransactionType.JOCKEY_HIRING_ADVANCE_PAYOUT)
                .direction(TransactionDirection.DEBIT)
                .amount(amount)
                .balanceBefore(systemBalanceBefore)
                .balanceAfter(systemBalanceAfter)
                .counterpartyWalletId(jockeyWallet.getWalletId())
                .counterpartyType(CounterpartyType.USER)
                .transactionGroupId(transactionGroupId)
                .status(TransactionStatus.SUCCESS)
                .note("Jockey advance payout")
                .build();

        Transaction jockeyTransaction = Transaction.builder()
                .wallet(jockeyWallet)
                .contractId(contract.getContractId())
                .type(TransactionType.JOCKEY_HIRING_ADVANCE_INCOME)
                .direction(TransactionDirection.CREDIT)
                .amount(amount)
                .balanceBefore(jockeyBalanceBefore)
                .balanceAfter(jockeyBalanceAfter)
                .counterpartyWalletId(systemEscrowWallet.getWalletId())
                .counterpartyType(CounterpartyType.SYSTEM)
                .transactionGroupId(transactionGroupId)
                .status(TransactionStatus.SUCCESS)
                .note("Advance income from contract")
                .build();

        walletTransactionRepository.save(systemTransaction);

        walletTransactionRepository.save(jockeyTransaction);
    }

    @Override
    @Transactional
    public ContractResponse rejectContractByAdmin(UUID contractId, String reason) {
        User admin = userCurrentService.getCurrentUser();

        JockeyHorseContract contract = contractRepository.findForUpdateByContractId(contractId).orElseThrow(()
                -> new AppException(ErrorCode.CONTRACT_NOT_FOUND));

        if (contract.getStatus() != ContractStatus.PENDING_ADMIN_REVIEW)
            throw new AppException(ErrorCode.INVALID_CONTRACT_STATUS);

        // Refund hiring fee invoice
        Optional<Invoice> hiringInvoice = invoiceRepository.findByContractIdAndInvoiceType(
                contract.getContractId(), InvoiceType.JOCKEY_HIRING_FEE);
        boolean hiringRefunded = false;
        if (hiringInvoice.isPresent() && hiringInvoice.get().getStatus() == InvoiceStatus.PAID) {
            paymentService.refundInvoice(hiringInvoice.get().getInvoiceId());
            hiringRefunded = true;
        }

        // Refund contract creation fee invoice
        Optional<Invoice> contractFeeInvoice = invoiceRepository.findByContractIdAndInvoiceType(
                contract.getContractId(), InvoiceType.CONTRACT_CREATION_FEE);
        if (contractFeeInvoice.isPresent() && contractFeeInvoice.get().getStatus() == InvoiceStatus.PAID) {
            paymentService.refundInvoice(contractFeeInvoice.get().getInvoiceId());
        }

        contract.setStatus(ContractStatus.REJECTED);
        if (hiringRefunded) {
            contract.setPaymentStatus(ContractPaymentStatus.REFUNDED);
            contract.setEscrowStatus(EscrowStatus.REFUNDED);
        }

        contract.setRejectedReason(reason);
        contract.setReviewedBy(admin);
        contract.setReviewedAt(LocalDateTime.now());

        return contractMapper.toContractResponse(contractRepository.save(contract));
    }

    private void validateInvite(HorseOwner currentOwner, HorseTournamentRegistration horseTournamentRegistration,
                                JockeyTournamentRegistration jockeyTournamentRegistration, InviteRequest request){
        if(!horseTournamentRegistration.getOwner().getOwnerId().equals(currentOwner.getOwnerId()))
            throw new AppException(ErrorCode.HORSE_NOT_BELONG_TO_OWNER);

        if(horseTournamentRegistration.getStatus() != RegistrationStatus.APPROVED)
            throw new AppException(ErrorCode.INVALID_REGISTRATION_STATUS);

        if(jockeyTournamentRegistration.getStatus() != RegistrationStatus.APPROVED)
            throw new AppException(ErrorCode.INVALID_REGISTRATION_STATUS);

        if(!jockeyTournamentRegistration.getTournament().getTournamentId().equals(horseTournamentRegistration.getTournament().getTournamentId()))
            throw new AppException(ErrorCode.TOURNAMENT_NOT_MATCH);

        if(jockeyTournamentRegistration.getHireFee() == null || jockeyTournamentRegistration.getHireFee().compareTo(BigDecimal.ZERO) <= 0)
            throw new AppException(ErrorCode.INVALID_HIRE_FEE);

        validatePrizeShare(request);

        boolean contractExists = contractRepository
                .existsByJockeyTournamentRegistration_JockeyTournamentRegIdAndHorseTournamentRegistration_HorseRegistrationIdAndStatusIn(
                        jockeyTournamentRegistration.getJockeyTournamentRegId(),
                        horseTournamentRegistration.getHorseRegistrationId(),
                        List.of(ContractStatus.PENDING_JOCKEY,
                                ContractStatus.ACCEPTED,
                                ContractStatus.HIRING_PAID,
                                ContractStatus.PENDING_ADMIN_REVIEW,
                                ContractStatus.APPROVED)
                );

        if(contractExists)
            throw new AppException(ErrorCode.CONTRACT_ALREADY_EXISTS);
    }

    private void validatePrizeShare(InviteRequest request){
        Float ownerShared = request.getOwnerPrizeSharePercent();
        Float jockeyShared = request.getJockeyPrizeSharePercent();

        if(ownerShared == null || jockeyShared == null)
            throw new AppException(ErrorCode.INVALID_PRIZE_SHARE);

        if(ownerShared < 0 || ownerShared > 100 || jockeyShared < 0 || jockeyShared > 100)
            throw new AppException(ErrorCode.INVALID_PRIZE_SHARE);

        float total = ownerShared + jockeyShared;

        if(Math.abs(total - 100F) > 0.0001F)
            throw new AppException(ErrorCode.INVALID_PRIZE_SHARE);
    }

    @Override
    public List<ContractResponse> getOwnerContracts() {
        User currentUser = userCurrentService.getCurrentUser();

        List<JockeyHorseContract> contracts = contractRepository
                .findByOwner_User_UserIdOrderByRequestedAtDesc(currentUser.getUserId());

        List<ContractResponse> responseList = new ArrayList<>();
        for (JockeyHorseContract contract : contracts) {
            responseList.add(contractMapper.toContractResponse(contract));
        }
        return responseList;
    }

    @Override
    public ContractResponse getOwnerContractById(UUID contractId) {
        User currentUser = userCurrentService.getCurrentUser();

        JockeyHorseContract contract = contractRepository.findById(contractId).orElseThrow(()
                -> new AppException(ErrorCode.CONTRACT_NOT_FOUND));

        if (!contract.getOwner().getUser().getUserId().equals(currentUser.getUserId()))
            throw new AppException(ErrorCode.UNAUTHENTICATED);

        return contractMapper.toContractResponse(contract);
    }

    @Override
    public List<ContractResponse> getJockeyContracts() {
        User currentUser = userCurrentService.getCurrentUser();

        List<JockeyHorseContract> contracts = contractRepository
                .findByJockey_User_UserIdOrderByRequestedAtDesc(currentUser.getUserId());

        List<ContractResponse> responseList = new ArrayList<>();
        for (JockeyHorseContract contract : contracts) {
            responseList.add(contractMapper.toContractResponse(contract));
        }
        return responseList;
    }

    @Override
    public ContractResponse getJockeyContractById(UUID contractId) {
        User currentUser = userCurrentService.getCurrentUser();

        JockeyHorseContract contract = contractRepository.findById(contractId).orElseThrow(()
                -> new AppException(ErrorCode.CONTRACT_NOT_FOUND));

        if (!contract.getJockey().getUser().getUserId().equals(currentUser.getUserId()))
            throw new AppException(ErrorCode.UNAUTHENTICATED);

        return contractMapper.toContractResponse(contract);
    }

    @Override
    @Transactional
    public ContractResponse releaseFinalPayout(UUID contractId) {
        User admin = userCurrentService.getCurrentUser();

        JockeyHorseContract contract = contractRepository.findForUpdateByContractId(contractId).orElseThrow(()
                -> new AppException(ErrorCode.CONTRACT_NOT_FOUND));

        if (contract.getStatus() != ContractStatus.APPROVED)
            throw new AppException(ErrorCode.CONTRACT_NOT_APPROVED);

        if (contract.getEscrowStatus() != EscrowStatus.PARTIALLY_RELEASED)
            throw new AppException(ErrorCode.ESCROW_NOT_PARTIALLY_RELEASED);

        if (contract.getFinalPayoutStatus() == FinalPayoutStatus.RELEASED)
            throw new AppException(ErrorCode.FINAL_PAYOUT_ALREADY_RELEASED);

        BigDecimal finalAmount = contract.getEscrowAmount();

        releaseFinalPayoutToJockey(contract, finalAmount);

        LocalDateTime now = LocalDateTime.now();

        contract.setEscrowAmount(BigDecimal.ZERO);
        contract.setEscrowStatus(EscrowStatus.RELEASED);
        contract.setFinalPayoutStatus(FinalPayoutStatus.RELEASED);
        contract.setFinalPayoutAt(now);

        return contractMapper.toContractResponse(contractRepository.save(contract));
    }

    @Override
    @Transactional(readOnly = true)
    public com.swp391.horseracing.dto.common.PageResponse<ContractResponse> getContractsByStatus(
            ContractStatus status, int page, int size) {
        validatePage(page, size);
        Page<JockeyHorseContract> contracts = contractRepository.findByStatusOrderByRequestedAtDesc(
                status, PageRequest.of(page, size));
        return toContractPage(contracts);
    }

    @Override
    @Transactional(readOnly = true)
    public com.swp391.horseracing.dto.common.PageResponse<ContractResponse> getApprovedContractsByTournament(
            UUID tournamentId, int page, int size) {
        validatePage(page, size);
        if (!tournamentRepository.existsById(tournamentId)) {
            throw new AppException(ErrorCode.TOURNAMENT_NOT_FOUND);
        }
        Page<JockeyHorseContract> contracts = contractRepository
                .findByTournament_TournamentIdAndStatusOrderByRequestedAtDesc(
                        tournamentId, ContractStatus.APPROVED, PageRequest.of(page, size));
        return toContractPage(contracts);
    }

    @Override
    @Transactional
    public ContractResponse cancelByOwner(UUID contractId, String reason) {
        User currentUser = userCurrentService.getCurrentUser();
        JockeyHorseContract contract = contractRepository.findForUpdateByContractId(contractId)
                .orElseThrow(() -> new AppException(ErrorCode.CONTRACT_NOT_FOUND));

        if (!contract.getOwner().getUser().getUserId().equals(currentUser.getUserId())) {
            throw new AppException(ErrorCode.ACCESS_DENIED);
        }
        if (!isOwnerCancellable(contract.getStatus())) {
            throw new AppException(ErrorCode.CONTRACT_CANCELLATION_NOT_ALLOWED);
        }

        TournamentPhase phase = contract.getTournament().getPhase();
        if (phase == TournamentPhase.RACING || phase == TournamentPhase.RESULT_PENDING
                || phase == TournamentPhase.RESULT_PUBLISHED || phase == TournamentPhase.FINISHED) {
            throw new AppException(ErrorCode.CONTRACT_CANCELLATION_NOT_ALLOWED);
        }

        List<RaceEntry> entries = raceEntryRepository.findByContract_ContractId(contractId);
        for (RaceEntry entry : entries) {
            if (entry.getRace().getStatus() != RoundStatus.SCHEDULING) {
                throw new AppException(ErrorCode.CONTRACT_HAS_ACTIVE_RACE);
            }
        }

        cancelOrRefundHiringInvoice(contract);
        cancelUnpaidContractFeeInvoice(contractId);

        if (!entries.isEmpty()) {
            raceEntryRepository.deleteAll(entries);
        }

        contract.setStatus(ContractStatus.CANCELLED);
        contract.setCancelledAt(LocalDateTime.now());
        contract.setCancelReason(reason.trim());
        contract.setFinalPayoutStatus(FinalPayoutStatus.CANCELLED);
        if (contract.getAdvancePayoutStatus() == AdvancePayoutStatus.NOT_PAID) {
            contract.setAdvancePayoutStatus(AdvancePayoutStatus.CANCELLED);
        }
        return contractMapper.toContractResponse(contractRepository.save(contract));
    }

    private boolean isOwnerCancellable(ContractStatus status) {
        return status == ContractStatus.PENDING_JOCKEY
                || status == ContractStatus.ACCEPTED
                || status == ContractStatus.HIRING_PAID
                || status == ContractStatus.PENDING_ADMIN_REVIEW
                || status == ContractStatus.APPROVED;
    }

    private void cancelOrRefundHiringInvoice(JockeyHorseContract contract) {
        Optional<Invoice> optionalInvoice = invoiceRepository.findByContractIdAndInvoiceType(
                contract.getContractId(), InvoiceType.JOCKEY_HIRING_FEE);
        if (optionalInvoice.isEmpty()) {
            return;
        }

        Invoice invoice = optionalInvoice.get();
        if (invoice.getStatus() == InvoiceStatus.UNPAID) {
            invoiceService.cancelInvoice(invoice.getInvoiceId());
            return;
        }
        if (invoice.getStatus() != InvoiceStatus.PAID) {
            throw new AppException(ErrorCode.CONTRACT_CANCELLATION_NOT_ALLOWED);
        }

        if (contract.getStatus() == ContractStatus.APPROVED) {
            BigDecimal remainingEscrow = contract.getEscrowAmount();
            if (remainingEscrow == null || remainingEscrow.compareTo(BigDecimal.ZERO) <= 0) {
                throw new AppException(ErrorCode.INVALID_ESCROW_STATUS);
            }
            paymentService.refundInvoiceAmount(invoice.getInvoiceId(), remainingEscrow);
            contract.setPaymentStatus(ContractPaymentStatus.PARTIALLY_REFUNDED);
        } else {
            paymentService.refundInvoice(invoice.getInvoiceId());
            contract.setPaymentStatus(ContractPaymentStatus.REFUNDED);
        }
        contract.setEscrowAmount(BigDecimal.ZERO);
        contract.setEscrowStatus(EscrowStatus.REFUNDED);
    }

    private void cancelUnpaidContractFeeInvoice(UUID contractId) {
        Optional<Invoice> invoice = invoiceRepository.findByContractIdAndInvoiceType(
                contractId, InvoiceType.CONTRACT_CREATION_FEE);
        if (invoice.isPresent() && invoice.get().getStatus() == InvoiceStatus.UNPAID) {
            invoiceService.cancelInvoice(invoice.get().getInvoiceId());
        }
    }

    private com.swp391.horseracing.dto.common.PageResponse<ContractResponse> toContractPage(
            Page<JockeyHorseContract> source) {
        List<ContractResponse> items = new ArrayList<>();
        for (JockeyHorseContract contract : source.getContent()) {
            items.add(contractMapper.toContractResponse(contract));
        }
        return new com.swp391.horseracing.dto.common.PageResponse<>(items, source.getNumber(), source.getSize(),
                source.getTotalElements(), source.getTotalPages(), source.isFirst(), source.isLast());
    }

    private void validatePage(int page, int size) {
        if (page < 0 || size < 1 || size > 100) {
            throw new AppException(ErrorCode.INVALID_PAGE_REQUEST);
        }
    }

    private void releaseFinalPayoutToJockey(JockeyHorseContract contract, BigDecimal amount) {
        Wallet systemEscrowWallet = walletRepository.findForUpdateByOwnerTypeAndWalletPurpose(
                WalletOwnerType.SYSTEM, WalletPurpose.SYSTEM_ESCROW).orElseThrow(()
                -> new AppException(ErrorCode.SYSTEM_WALLET_NOT_FOUND));

        Wallet jockeyWallet = walletRepository.findForUpdateByUser_UserIdAndWalletPurpose(
                contract.getJockey().getUser().getUserId(), WalletPurpose.USER_MAIN).orElseThrow(()
                -> new AppException(ErrorCode.WALLET_NOT_FOUND));

        if (systemEscrowWallet.getBalance().compareTo(amount) < 0)
            throw new AppException(ErrorCode.INSUFFICIENT_BALANCE);

        UUID transactionGroupId = UUID.randomUUID();

        BigDecimal systemBalanceBefore = systemEscrowWallet.getBalance();
        BigDecimal systemBalanceAfter = systemBalanceBefore.subtract(amount);

        BigDecimal jockeyBalanceBefore = jockeyWallet.getBalance();
        BigDecimal jockeyBalanceAfter = jockeyBalanceBefore.add(amount);

        systemEscrowWallet.setBalance(systemBalanceAfter);
        jockeyWallet.setBalance(jockeyBalanceAfter);

        walletRepository.save(systemEscrowWallet);
        walletRepository.save(jockeyWallet);

        Transaction systemTransaction = Transaction.builder()
                .wallet(systemEscrowWallet)
                .contractId(contract.getContractId())
                .type(TransactionType.JOCKEY_HIRING_FINAL_PAYOUT)
                .direction(TransactionDirection.DEBIT)
                .amount(amount)
                .balanceBefore(systemBalanceBefore)
                .balanceAfter(systemBalanceAfter)
                .counterpartyWalletId(jockeyWallet.getWalletId())
                .counterpartyType(CounterpartyType.USER)
                .transactionGroupId(transactionGroupId)
                .status(TransactionStatus.SUCCESS)
                .note("Jockey final payout")
                .build();

        Transaction jockeyTransaction = Transaction.builder()
                .wallet(jockeyWallet)
                .contractId(contract.getContractId())
                .type(TransactionType.JOCKEY_HIRING_FINAL_INCOME)
                .direction(TransactionDirection.CREDIT)
                .amount(amount)
                .balanceBefore(jockeyBalanceBefore)
                .balanceAfter(jockeyBalanceAfter)
                .counterpartyWalletId(systemEscrowWallet.getWalletId())
                .counterpartyType(CounterpartyType.SYSTEM)
                .transactionGroupId(transactionGroupId)
                .status(TransactionStatus.SUCCESS)
                .note("Final income from contract")
                .build();

        walletTransactionRepository.save(systemTransaction);
        walletTransactionRepository.save(jockeyTransaction);
    }
}


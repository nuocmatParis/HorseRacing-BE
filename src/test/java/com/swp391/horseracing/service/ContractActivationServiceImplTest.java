package com.swp391.horseracing.service;

import com.swp391.horseracing.entity.Invoice;
import com.swp391.horseracing.entity.Jockey;
import com.swp391.horseracing.entity.JockeyHorseContract;
import com.swp391.horseracing.entity.Transaction;
import com.swp391.horseracing.entity.User;
import com.swp391.horseracing.entity.Wallet;
import com.swp391.horseracing.enums.AdvancePayoutStatus;
import com.swp391.horseracing.enums.ContractPaymentStatus;
import com.swp391.horseracing.enums.ContractStatus;
import com.swp391.horseracing.enums.EscrowStatus;
import com.swp391.horseracing.enums.InvoiceStatus;
import com.swp391.horseracing.enums.InvoiceType;
import com.swp391.horseracing.enums.TransactionDirection;
import com.swp391.horseracing.enums.TransactionType;
import com.swp391.horseracing.enums.WalletOwnerType;
import com.swp391.horseracing.enums.WalletPurpose;
import com.swp391.horseracing.exception.AppException;
import com.swp391.horseracing.exception.ErrorCode;
import com.swp391.horseracing.repository.InvoiceRepository;
import com.swp391.horseracing.repository.JockeyHorseContractRepository;
import com.swp391.horseracing.repository.WalletRepository;
import com.swp391.horseracing.repository.WalletTransactionRepository;
import com.swp391.horseracing.service.impl.ContractActivationServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class ContractActivationServiceImplTest {
    private final UUID contractId = UUID.randomUUID();
    private JockeyHorseContractRepository contractRepository;
    private InvoiceRepository invoiceRepository;
    private WalletRepository walletRepository;
    private WalletTransactionRepository transactionRepository;
    private BusinessNotificationEventService notificationService;
    private ContractActivationServiceImpl service;
    private JockeyHorseContract contract;
    private Invoice hiringInvoice;
    private Invoice creationInvoice;
    private Wallet escrowWallet;
    private Wallet jockeyWallet;

    @BeforeEach
    void setUp() {
        contractRepository = mock(JockeyHorseContractRepository.class);
        invoiceRepository = mock(InvoiceRepository.class);
        walletRepository = mock(WalletRepository.class);
        transactionRepository = mock(WalletTransactionRepository.class);
        notificationService = mock(BusinessNotificationEventService.class);
        service = new ContractActivationServiceImpl(
                contractRepository,
                invoiceRepository,
                walletRepository,
                transactionRepository,
                notificationService);

        User jockeyUser = User.builder().userId(UUID.randomUUID()).fullName("Jockey Test").build();
        Jockey jockey = Jockey.builder().jockeyId(UUID.randomUUID()).user(jockeyUser).build();
        contract = JockeyHorseContract.builder()
                .contractId(contractId)
                .jockey(jockey)
                .hireFee(new BigDecimal("1000000.00"))
                .advancePercent(30F)
                .finalPercent(70F)
                .paymentStatus(ContractPaymentStatus.PAID)
                .escrowStatus(EscrowStatus.HELD)
                .escrowAmount(new BigDecimal("1000000.00"))
                .advancePayoutStatus(AdvancePayoutStatus.NOT_PAID)
                .status(ContractStatus.HIRING_PAID)
                .build();
        hiringInvoice = Invoice.builder()
                .invoiceId(UUID.randomUUID())
                .contractId(contractId)
                .invoiceType(InvoiceType.JOCKEY_HIRING_FEE)
                .status(InvoiceStatus.PAID)
                .build();
        creationInvoice = Invoice.builder()
                .invoiceId(UUID.randomUUID())
                .contractId(contractId)
                .invoiceType(InvoiceType.CONTRACT_CREATION_FEE)
                .status(InvoiceStatus.PAID)
                .build();
        escrowWallet = Wallet.builder()
                .walletId(UUID.randomUUID())
                .ownerType(WalletOwnerType.SYSTEM)
                .walletPurpose(WalletPurpose.SYSTEM_ESCROW)
                .balance(new BigDecimal("2000000.00"))
                .build();
        jockeyWallet = Wallet.builder()
                .walletId(UUID.randomUUID())
                .ownerType(WalletOwnerType.USER)
                .walletPurpose(WalletPurpose.USER_MAIN)
                .balance(new BigDecimal("100000.00"))
                .build();

        when(contractRepository.findForUpdateByContractId(contractId)).thenReturn(Optional.of(contract));
        when(invoiceRepository.findForUpdateByContractIdAndInvoiceType(contractId, InvoiceType.JOCKEY_HIRING_FEE))
                .thenReturn(Optional.of(hiringInvoice));
        when(invoiceRepository.findForUpdateByContractIdAndInvoiceType(contractId, InvoiceType.CONTRACT_CREATION_FEE))
                .thenReturn(Optional.of(creationInvoice));
        when(walletRepository.findForUpdateByOwnerTypeAndWalletPurpose(
                WalletOwnerType.SYSTEM, WalletPurpose.SYSTEM_ESCROW)).thenReturn(Optional.of(escrowWallet));
        when(walletRepository.findForUpdateByUser_UserIdAndWalletPurpose(
                jockeyUser.getUserId(), WalletPurpose.USER_MAIN)).thenReturn(Optional.of(jockeyWallet));
        when(contractRepository.save(contract)).thenReturn(contract);
    }

    @Test
    void activatesContractAndReleasesThirtyPercentExactlyOnce() {
        service.activateAfterFullPayment(contractId);

        assertEquals(ContractStatus.APPROVED, contract.getStatus());
        assertEquals(new BigDecimal("300000.00"), contract.getAdvancePaidAmount());
        assertEquals(new BigDecimal("700000.00"), contract.getEscrowAmount());
        assertEquals(EscrowStatus.PARTIALLY_RELEASED, contract.getEscrowStatus());
        assertEquals(AdvancePayoutStatus.PAID, contract.getAdvancePayoutStatus());
        assertNotNull(contract.getAdvancePayoutAt());
        assertNull(contract.getReviewedBy());
        assertNull(contract.getReviewedAt());
        assertEquals(new BigDecimal("1700000.00"), escrowWallet.getBalance());
        assertEquals(new BigDecimal("400000.00"), jockeyWallet.getBalance());

        ArgumentCaptor<Transaction> transactionCaptor = ArgumentCaptor.forClass(Transaction.class);
        verify(transactionRepository, times(2)).save(transactionCaptor.capture());
        List<Transaction> transactions = transactionCaptor.getAllValues();
        assertEquals(TransactionType.JOCKEY_HIRING_ADVANCE_PAYOUT, transactions.get(0).getType());
        assertEquals(TransactionDirection.DEBIT, transactions.get(0).getDirection());
        assertEquals(TransactionType.JOCKEY_HIRING_ADVANCE_INCOME, transactions.get(1).getType());
        assertEquals(TransactionDirection.CREDIT, transactions.get(1).getDirection());
        assertEquals(transactions.get(0).getTransactionGroupId(), transactions.get(1).getTransactionGroupId());
        verify(notificationService).contractApproved(contract);
    }

    @Test
    void repeatedActivationDoesNotMoveMoneyOrCreateNotifications() {
        contract.setStatus(ContractStatus.APPROVED);
        contract.setAdvancePayoutStatus(AdvancePayoutStatus.PAID);

        service.activateAfterFullPayment(contractId);

        verifyNoInteractions(invoiceRepository, walletRepository, transactionRepository, notificationService);
        verify(contractRepository, never()).save(any(JockeyHorseContract.class));
    }

    @Test
    void rejectsContractThatHasNotReachedHiringPaid() {
        contract.setStatus(ContractStatus.ACCEPTED);

        AppException exception = assertThrows(AppException.class,
                () -> service.activateAfterFullPayment(contractId));

        assertEquals(ErrorCode.INVALID_CONTRACT_STATUS, exception.getErrorCode());
        verifyNoInteractions(invoiceRepository, walletRepository, transactionRepository, notificationService);
    }

    @Test
    void requiresPaidHiringInvoice() {
        hiringInvoice.setStatus(InvoiceStatus.UNPAID);

        AppException exception = assertThrows(AppException.class,
                () -> service.activateAfterFullPayment(contractId));

        assertEquals(ErrorCode.INVOICE_NOT_PAID, exception.getErrorCode());
        verifyNoInteractions(walletRepository, notificationService);
    }

    @Test
    void requiresPaidContractCreationInvoice() {
        creationInvoice.setStatus(InvoiceStatus.UNPAID);

        AppException exception = assertThrows(AppException.class,
                () -> service.activateAfterFullPayment(contractId));

        assertEquals(ErrorCode.INVOICE_NOT_PAID, exception.getErrorCode());
        verifyNoInteractions(walletRepository, notificationService);
    }

    @Test
    void rejectsAnInconsistentInitialEscrowAmount() {
        contract.setEscrowAmount(new BigDecimal("900000.00"));

        AppException exception = assertThrows(AppException.class,
                () -> service.activateAfterFullPayment(contractId));

        assertEquals(ErrorCode.INVALID_ESCROW_STATUS, exception.getErrorCode());
        verifyNoInteractions(walletRepository, notificationService);
    }

    @Test
    void rejectsAReplayWhenAnAdvanceTransactionAlreadyExists() {
        when(transactionRepository.existsByContractIdAndType(
                contractId, TransactionType.JOCKEY_HIRING_ADVANCE_PAYOUT)).thenReturn(true);

        AppException exception = assertThrows(AppException.class,
                () -> service.activateAfterFullPayment(contractId));

        assertEquals(ErrorCode.INVALID_CONTRACT_STATUS, exception.getErrorCode());
        verifyNoInteractions(walletRepository, notificationService);
    }

    @Test
    void insufficientEscrowStopsBeforeAnyBalanceIsSaved() {
        escrowWallet.setBalance(new BigDecimal("299999.00"));

        AppException exception = assertThrows(AppException.class,
                () -> service.activateAfterFullPayment(contractId));

        assertEquals(ErrorCode.INSUFFICIENT_BALANCE, exception.getErrorCode());
        verify(walletRepository, never()).save(any(Wallet.class));
        verify(transactionRepository, never()).save(any(Transaction.class));
        verify(notificationService, never()).contractApproved(any(JockeyHorseContract.class));
    }
}

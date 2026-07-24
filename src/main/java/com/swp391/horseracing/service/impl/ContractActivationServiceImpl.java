package com.swp391.horseracing.service.impl;

import com.swp391.horseracing.entity.Invoice;
import com.swp391.horseracing.entity.JockeyHorseContract;
import com.swp391.horseracing.entity.Transaction;
import com.swp391.horseracing.entity.Wallet;
import com.swp391.horseracing.enums.AdvancePayoutStatus;
import com.swp391.horseracing.enums.ContractPaymentStatus;
import com.swp391.horseracing.enums.ContractStatus;
import com.swp391.horseracing.enums.CounterpartyType;
import com.swp391.horseracing.enums.EscrowStatus;
import com.swp391.horseracing.enums.InvoiceStatus;
import com.swp391.horseracing.enums.InvoiceType;
import com.swp391.horseracing.enums.TransactionDirection;
import com.swp391.horseracing.enums.TransactionStatus;
import com.swp391.horseracing.enums.TransactionType;
import com.swp391.horseracing.enums.WalletOwnerType;
import com.swp391.horseracing.enums.WalletPurpose;
import com.swp391.horseracing.exception.AppException;
import com.swp391.horseracing.exception.ErrorCode;
import com.swp391.horseracing.repository.InvoiceRepository;
import com.swp391.horseracing.repository.JockeyHorseContractRepository;
import com.swp391.horseracing.repository.WalletRepository;
import com.swp391.horseracing.repository.WalletTransactionRepository;
import com.swp391.horseracing.service.BusinessNotificationEventService;
import com.swp391.horseracing.service.ContractActivationService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ContractActivationServiceImpl implements ContractActivationService {
    JockeyHorseContractRepository contractRepository;
    InvoiceRepository invoiceRepository;
    WalletRepository walletRepository;
    WalletTransactionRepository walletTransactionRepository;
    BusinessNotificationEventService notificationEventService;

    @Override
    @Transactional
    public void activateAfterFullPayment(UUID contractId) {
        JockeyHorseContract contract = contractRepository.findForUpdateByContractId(contractId)
                .orElseThrow(() -> new AppException(ErrorCode.CONTRACT_NOT_FOUND));

        if (contract.getStatus() == ContractStatus.APPROVED) {
            return;
        }
        if (contract.getStatus() != ContractStatus.HIRING_PAID) {
            throw new AppException(ErrorCode.INVALID_CONTRACT_STATUS);
        }

        Invoice hiringInvoice = getPaidInvoice(contractId, InvoiceType.JOCKEY_HIRING_FEE);
        getPaidInvoice(contractId, InvoiceType.CONTRACT_CREATION_FEE);

        // Kiểm tra xem số tiền đang được tạm khóa trong ví Escrow của
        // hợp đồng này có khớp với số tiền thuê Jockey ban đầu
        validateEscrowState(contract);
        // Kiểm tra lịch sử giao dịch xem hợp đồng này đã
        // từng có giao dịch chuyển tiền cọc chưa
        validateNoAdvanceTransactions(contractId);

        Wallet systemEscrowWallet = walletRepository.findForUpdateByOwnerTypeAndWalletPurpose(
                        WalletOwnerType.SYSTEM, WalletPurpose.SYSTEM_ESCROW)
                .orElseThrow(() -> new AppException(ErrorCode.SYSTEM_WALLET_NOT_FOUND));

        Wallet jockeyWallet = walletRepository.findForUpdateByUser_UserIdAndWalletPurpose(
                        contract.getJockey().getUser().getUserId(), WalletPurpose.USER_MAIN)
                .orElseThrow(() -> new AppException(ErrorCode.WALLET_NOT_FOUND));

        BigDecimal advanceAmount = calculateAdvanceAmount(contract);
        if (systemEscrowWallet.getBalance().compareTo(advanceAmount) < 0) {
            throw new AppException(ErrorCode.INSUFFICIENT_BALANCE);
        }

        // Chuyển tiền cọc
        releaseAdvance(contract, hiringInvoice, systemEscrowWallet, jockeyWallet, advanceAmount);

        LocalDateTime now = LocalDateTime.now();
        contract.setStatus(ContractStatus.APPROVED);
        contract.setPaymentStatus(ContractPaymentStatus.PAID);
        contract.setAdvancePaidAmount(advanceAmount);
        contract.setEscrowAmount(contract.getHireFee().subtract(advanceAmount)); // Số tiền còn giữ hộ
        contract.setEscrowStatus(EscrowStatus.PARTIALLY_RELEASED);
        contract.setAdvancePayoutStatus(AdvancePayoutStatus.PAID);
        contract.setAdvancePayoutAt(now);

        contract.setSubmittedAt(null);
        contract.setReviewedBy(null);
        contract.setReviewedAt(null);

        JockeyHorseContract savedContract = contractRepository.save(contract);
        notificationEventService.contractApproved(savedContract);
    }

    private Invoice getPaidInvoice(UUID contractId, InvoiceType invoiceType) {
        Invoice invoice = invoiceRepository.findForUpdateByContractIdAndInvoiceType(contractId, invoiceType)
                .orElseThrow(() -> new AppException(ErrorCode.INVOICE_NOT_FOUND));
        if (invoice.getStatus() != InvoiceStatus.PAID) {
            throw new AppException(ErrorCode.INVOICE_NOT_PAID);
        }
        return invoice;
    }

    private void validateEscrowState(JockeyHorseContract contract) {
        if (contract.getPaymentStatus() != ContractPaymentStatus.PAID) {
            throw new AppException(ErrorCode.CONTRACT_HIRING_FEE_NOT_PAID);
        }
        if (contract.getEscrowStatus() != EscrowStatus.HELD
                || contract.getEscrowAmount() == null
                || contract.getEscrowAmount().compareTo(contract.getHireFee()) != 0) {
            throw new AppException(ErrorCode.INVALID_ESCROW_STATUS);
        }
    }

    private void validateNoAdvanceTransactions(UUID contractId) {
        if (walletTransactionRepository.existsByContractIdAndType(
                contractId, TransactionType.JOCKEY_HIRING_ADVANCE_PAYOUT)
                || walletTransactionRepository.existsByContractIdAndType(
                contractId, TransactionType.JOCKEY_HIRING_ADVANCE_INCOME)) {
            throw new AppException(ErrorCode.INVALID_CONTRACT_STATUS);
        }
    }

    // Tính tiền cọc
    private BigDecimal calculateAdvanceAmount(JockeyHorseContract contract) {
        BigDecimal percent = BigDecimal.valueOf(contract.getAdvancePercent());
        return contract.getHireFee()
                .multiply(percent)
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
    }

    // Chuyển tiền cọc
    private void releaseAdvance(JockeyHorseContract contract, Invoice hiringInvoice,
                                Wallet systemEscrowWallet, Wallet jockeyWallet, BigDecimal amount) {
        BigDecimal systemBalanceBefore = systemEscrowWallet.getBalance();
        BigDecimal systemBalanceAfter = systemBalanceBefore.subtract(amount);

        BigDecimal jockeyBalanceBefore = jockeyWallet.getBalance();
        BigDecimal jockeyBalanceAfter = jockeyBalanceBefore.add(amount);

        systemEscrowWallet.setBalance(systemBalanceAfter);
        jockeyWallet.setBalance(jockeyBalanceAfter);

        walletRepository.save(systemEscrowWallet);
        walletRepository.save(jockeyWallet);

        UUID transactionGroupId = UUID.randomUUID();
        Transaction systemTransaction = Transaction.builder()
                .wallet(systemEscrowWallet)
                .invoice(hiringInvoice)
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
                .note("Chi tạm ứng khi hợp đồng có hiệu lực")
                .build();
        Transaction jockeyTransaction = Transaction.builder()
                .wallet(jockeyWallet)
                .invoice(hiringInvoice)
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
                .note("Nhận tạm ứng từ hợp đồng có hiệu lực")
                .build();

        walletTransactionRepository.save(systemTransaction);
        walletTransactionRepository.save(jockeyTransaction);
    }
}

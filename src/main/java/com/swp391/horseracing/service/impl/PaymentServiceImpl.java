package com.swp391.horseracing.service.impl;

import com.swp391.horseracing.dto.invoice.response.PaymentResponse;
import com.swp391.horseracing.entity.*;
import com.swp391.horseracing.enums.RegistrationStatus;
import com.swp391.horseracing.enums.*;
import com.swp391.horseracing.exception.AppException;
import com.swp391.horseracing.exception.ErrorCode;
import com.swp391.horseracing.mapper.InvoiceMapper;
import com.swp391.horseracing.mapper.TransactionMapper;
import com.swp391.horseracing.repository.HorseTournamentRegistrationRepository;
import com.swp391.horseracing.repository.InvoiceRepository;
import com.swp391.horseracing.repository.WalletRepository;
import com.swp391.horseracing.repository.WalletTransactionRepository;
import com.swp391.horseracing.service.InvoicePaymentCompleteService;
import com.swp391.horseracing.service.InvoiceService;
import com.swp391.horseracing.service.PaymentService;
import com.swp391.horseracing.service.UserCurrentService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import static com.swp391.horseracing.enums.WalletStatus.FROZEN;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class PaymentServiceImpl implements PaymentService {
    InvoiceService invoiceService;
    UserCurrentService userCurrentService;
    InvoiceRepository invoiceRepository;
    WalletRepository walletRepository;
    WalletTransactionRepository walletTransactionRepository;
    InvoiceMapper invoiceMapper;
    TransactionMapper transactionMapper;
    HorseTournamentRegistrationRepository horseRegistrationRepository;
    InvoicePaymentCompleteService invoicePaymentCompleteService;

    @Override
    @Transactional
    public PaymentResponse payInvoice(UUID invoiceId) {
        User currentUser = userCurrentService.getCurrentUser();

        Invoice invoice = invoiceRepository.findForUpdateByInvoiceId(invoiceId).orElseThrow(()
                -> new AppException(ErrorCode.INVOICE_NOT_FOUND));

        validateInvoiceBelongsToCurrentUser(invoice, currentUser);
        validateInvoiceCanBePaid(invoice);

        Wallet userWallet = walletRepository.findForUpdateByUser_UserIdAndWalletPurpose(currentUser.getUserId(),
                WalletPurpose.USER_MAIN).orElseThrow(()
                -> new AppException(ErrorCode.WALLET_NOT_FOUND));

        validateWalletActive(userWallet);

        Wallet systemWallet = walletRepository.findForUpdateByOwnerTypeAndWalletPurpose(WalletOwnerType.SYSTEM,
                WalletPurpose.SYSTEM_REVENUE).orElseThrow(()
                -> new AppException(ErrorCode.SYSTEM_WALLET_NOT_FOUND));

        validateWalletActive(systemWallet);

        BigDecimal amount = invoice.getAmount();

        if(userWallet.getBalance().compareTo(amount) < 0)
            throw new AppException(ErrorCode.INSUFFICIENT_BALANCE);

        BigDecimal userBalanceBefore = userWallet.getBalance();
        BigDecimal userBalanceAfter = userBalanceBefore.subtract(amount);
        userWallet.setBalance(userBalanceAfter);
        Wallet savedUserWallet = walletRepository.save(userWallet);

        BigDecimal systemBalanceBefore = systemWallet.getBalance();
        BigDecimal systemBalanceAfter = systemBalanceBefore.add(amount);
        systemWallet.setBalance(systemBalanceAfter);
        Wallet savedSystemWallet = walletRepository.save(systemWallet);

        TransactionType transactionType = getTransactionType(invoice.getInvoiceType());
        UUID transactionGroupId = UUID.randomUUID();

        Transaction userTransaction = Transaction.builder()
                .wallet(savedUserWallet)
                .invoice(invoice)
                .raceResultId(null)
                .contractId(invoice.getContractId())
                .type(transactionType)
                .direction(TransactionDirection.DEBIT)
                .amount(amount)
                .balanceBefore(userBalanceBefore)
                .balanceAfter(userBalanceAfter)
                .counterpartyWalletId(savedSystemWallet.getWalletId())
                .counterpartyType(CounterpartyType.SYSTEM)
                .transactionGroupId(transactionGroupId)
                .status(TransactionStatus.SUCCESS)
                .note("Pay invoice: " + invoice.getInvoiceType().name())
                .build();


        Transaction systemTransaction = Transaction.builder()
                .wallet(savedSystemWallet)
                .invoice(invoice)
                .raceResultId(null)
                .contractId(invoice.getContractId())
                .type(transactionType)
                .direction(TransactionDirection.CREDIT)
                .amount(amount)
                .balanceBefore(systemBalanceBefore)
                .balanceAfter(systemBalanceAfter)
                .counterpartyWalletId(savedUserWallet.getWalletId())
                .counterpartyType(CounterpartyType.USER)
                .transactionGroupId(transactionGroupId)
                .status(TransactionStatus.SUCCESS)
                .note("Receive invoice payment: " + invoice.getInvoiceType().name())
                .build();

        Transaction savedUserTransaction = walletTransactionRepository.save(userTransaction);
        Transaction savedSystemtransaction = walletTransactionRepository.save(systemTransaction);

        invoice.setStatus(InvoiceStatus.PAID);
        invoice.setPaidAt(LocalDateTime.now());

        Invoice savedInvoice = invoiceRepository.save(invoice);

        invoicePaymentCompleteService.handleAfterPaid(savedInvoice);

        return PaymentResponse.builder()
                .invoiceResponse(invoiceMapper.toInvoiceResponse(savedInvoice))
                .userTransaction(transactionMapper.toTransactionResponse(savedUserTransaction))
                .systemTransaction(transactionMapper.toTransactionResponse(savedSystemtransaction))
                .build();
    }

    @Override
    @Transactional
    public PaymentResponse refundInvoice(UUID invoiceId) {
        Invoice invoice = invoiceRepository.findForUpdateByInvoiceId(invoiceId).orElseThrow(()
                -> new AppException(ErrorCode.INVOICE_NOT_FOUND));

        validateInvoiceCanBeRefund(invoice);

        Wallet userWallet = walletRepository.findForUpdateByUser_UserIdAndWalletPurpose(
                invoice.getPayerUser().getUserId(), WalletPurpose.USER_MAIN).orElseThrow(()
                -> new AppException(ErrorCode.WALLET_NOT_FOUND) );

        Wallet systemWallet = walletRepository.findForUpdateByOwnerTypeAndWalletPurpose(WalletOwnerType.SYSTEM,
                WalletPurpose.SYSTEM_REVENUE).orElseThrow(()
                -> new AppException(ErrorCode.SYSTEM_WALLET_NOT_FOUND));

        validateWalletActive(userWallet);
        validateWalletActive(systemWallet);

        BigDecimal amount = invoice.getAmount();

        if(systemWallet.getBalance().compareTo(amount) < 0)
            throw new AppException(ErrorCode.INSUFFICIENT_BALANCE);

        BigDecimal systemBalanceBefore = systemWallet.getBalance();
        BigDecimal systemBalanceAfter = systemBalanceBefore.subtract(amount);
        systemWallet.setBalance(systemBalanceAfter);
        Wallet savedSystemWallet = walletRepository.save(systemWallet);

        BigDecimal userBalanceBefore = userWallet.getBalance();
        BigDecimal userBalanceAfter = userBalanceBefore.add(amount);
        userWallet.setBalance(userBalanceAfter);
        Wallet savedUserWallet = walletRepository.save(userWallet);

        UUID transactionGroupId = UUID.randomUUID();

        Transaction systemTransaction = Transaction.builder()
                .wallet(savedSystemWallet)
                .invoice(invoice)
                .raceResultId(null)
                .contractId(invoice.getContractId())
                .type(TransactionType.REFUND)
                .direction(TransactionDirection.DEBIT)
                .amount(amount)
                .balanceBefore(systemBalanceBefore)
                .balanceAfter(systemBalanceAfter)
                .counterpartyWalletId(savedUserWallet.getWalletId())
                .counterpartyType(CounterpartyType.USER)
                .transactionGroupId(transactionGroupId)
                .status(TransactionStatus.SUCCESS)
                .note("Refund invoice: " + invoice.getInvoiceType().name())
                .build();

        Transaction userTransaction = Transaction.builder()
                .wallet(savedUserWallet)
                .invoice(invoice)
                .raceResultId(null)
                .contractId(invoice.getContractId())
                .type(TransactionType.REFUND)
                .direction(TransactionDirection.CREDIT)
                .amount(amount)
                .balanceBefore(userBalanceBefore)
                .balanceAfter(userBalanceAfter)
                .counterpartyWalletId(savedSystemWallet.getWalletId())
                .counterpartyType(CounterpartyType.SYSTEM)
                .transactionGroupId(transactionGroupId)
                .status(TransactionStatus.SUCCESS)
                .note("Receive refund: " + invoice.getInvoiceType().name())
                .build();

        Transaction savedSystemTransaction = walletTransactionRepository.save(systemTransaction);
        Transaction savedUserTransaction = walletTransactionRepository.save(userTransaction);

        invoice.setStatus(InvoiceStatus.REFUNDED);
        invoice.setRefundedAt(LocalDateTime.now());

        Invoice savedInvoice = invoiceRepository.save(invoice);
        return PaymentResponse.builder()
                .invoiceResponse(invoiceMapper.toInvoiceResponse(invoice))
                .userTransaction(transactionMapper.toTransactionResponse(savedUserTransaction))
                .systemTransaction(transactionMapper.toTransactionResponse(systemTransaction))
                .build();
    }

    private void validateInvoiceCanBeRefund(Invoice invoice){
        if(invoice.getStatus() == InvoiceStatus.REFUNDED)
            throw new AppException(ErrorCode.INVOICE_ALREADY_REFUNDED);

        if(invoice.getStatus() == InvoiceStatus.UNPAID)
            throw new AppException(ErrorCode.INVOICE_NOT_PAID);

        if(invoice.getStatus() == InvoiceStatus.CANCELLED)
            throw new AppException(ErrorCode.INVOICE_CANCELLED);
    }


    private void validateInvoiceBelongsToCurrentUser(Invoice invoice, User currentUser){
        if(!invoice.getPayerUser().getUserId().equals(currentUser.getUserId())){
            throw new AppException(ErrorCode.INVOICE_ACCESS_DENIED);
        }
    }

    private void validateInvoiceCanBePaid(Invoice invoice){
        if(invoice.getStatus() == InvoiceStatus.PAID)
            throw new AppException(ErrorCode.INVOICE_ALREADY_PAID);

        if(invoice.getStatus() == InvoiceStatus.CANCELLED)
            throw new AppException(ErrorCode.INVOICE_CANCELLED);

        if(invoice.getStatus() == InvoiceStatus.REFUNDED)
            throw new AppException(ErrorCode.INVOICE_ALREADY_REFUNDED);

        if(invoice.getDueDate() != null && invoice.getDueDate().isBefore(LocalDateTime.now()))
            throw new AppException(ErrorCode.INVOICE_EXPIRED);
    }

    private void validateWalletActive(Wallet wallet){
        if(wallet.getStatus() == WalletStatus.CLOSED)
            throw new AppException(ErrorCode.WALLET_CLOSED);

        if(wallet.getStatus() == FROZEN)
            throw new AppException(ErrorCode.WALLET_FROZEN);
    }

    private TransactionType getTransactionType(InvoiceType invoiceType) {
        if (invoiceType == InvoiceType.OWNER_TOURNAMENT_REGISTRATION_FEE)
            return TransactionType.OWNER_REGISTRATION_FEE;
        else if (invoiceType == InvoiceType.JOCKEY_HIRING_FEE)
            return TransactionType.JOCKEY_HIRING_FEE;
        else if (invoiceType == InvoiceType.CONTRACT_CREATION_FEE)
            return TransactionType.CONTRACT_CREATION_FEE;

        return null;
    }
}

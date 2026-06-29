package com.swp391.horseracing.service.impl;

import com.swp391.horseracing.entity.Transaction;
import com.swp391.horseracing.repository.WalletTransactionRepository;
import com.swp391.horseracing.service.VnpayCallbackService;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.swp391.horseracing.config.VnpayProperties;
import com.swp391.horseracing.entity.PaymentTransaction;
import com.swp391.horseracing.entity.Wallet;
import com.swp391.horseracing.enums.*;
import com.swp391.horseracing.exception.AppException;
import com.swp391.horseracing.exception.ErrorCode;
import com.swp391.horseracing.repository.PaymentTransactionRepository;
import com.swp391.horseracing.repository.WalletRepository;
import com.swp391.horseracing.util.VnpayUtil;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.UUID;

import static com.swp391.horseracing.enums.WalletStatus.FROZEN;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class VnpayCallbackServiceImpl implements VnpayCallbackService {
    VnpayProperties vnpayProperties;
    PaymentTransactionRepository paymentTransactionRepository;
    WalletRepository walletRepository;
    WalletTransactionRepository transactionRepository;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final DateTimeFormatter VNPAY_DATE_FORMATTER =
            DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    @Transactional
    public void handleReturn(Map<String, String> params) {
        verifySecureHash(params);

        String txnRef = params.get("vnp_TxnRef");

        PaymentTransaction paymentTransaction = paymentTransactionRepository
                .findByVnpTxnRefForUpdate(txnRef)
                .orElseThrow(() -> new AppException(ErrorCode.PAYMENT_TRANSACTION_NOT_FOUND));

        paymentTransaction.setReturnReceivedAt(LocalDateTime.now());
        paymentTransaction.setReturnPayload(toJson(params));

        applyVnpayFields(paymentTransaction, params);

        if (paymentTransaction.getStatus() == PaymentTransactionStatus.SUCCESS) {
            paymentTransactionRepository.save(paymentTransaction);
            return;
        }

        if (!isAmountMatched(paymentTransaction, params)) {
            paymentTransaction.setStatus(PaymentTransactionStatus.FAILED);
            paymentTransaction.setFailureReason("Amount mismatch");
            paymentTransactionRepository.save(paymentTransaction);
            throw new AppException(ErrorCode.INVALID_PAYMENT_AMOUNT);
        }

        String responseCode = params.get("vnp_ResponseCode");
        String transactionStatus = params.get("vnp_TransactionStatus");

        if ("00".equals(responseCode) && "00".equals(transactionStatus)) {
            completeDeposit(paymentTransaction);
            return;
        }

        paymentTransaction.setStatus(PaymentTransactionStatus.FAILED);
        paymentTransaction.setFailureReason(
                "VNPAY failed: responseCode=" + responseCode
                        + ", transactionStatus=" + transactionStatus
        );

        paymentTransactionRepository.save(paymentTransaction);
    }

    @Transactional
    public void handleIpn(Map<String, String> params) {
        verifySecureHash(params);

        String txnRef = params.get("vnp_TxnRef");

        PaymentTransaction paymentTransaction = paymentTransactionRepository
                .findByVnpTxnRefForUpdate(txnRef)
                .orElseThrow(() -> new AppException(ErrorCode.PAYMENT_TRANSACTION_NOT_FOUND));

        paymentTransaction.setIpnReceivedAt(LocalDateTime.now());
        paymentTransaction.setIpnPayload(toJson(params));

        applyVnpayFields(paymentTransaction, params);

        if (paymentTransaction.getStatus() == PaymentTransactionStatus.SUCCESS) {
            paymentTransactionRepository.save(paymentTransaction);
            return;
        }

        if (!isAmountMatched(paymentTransaction, params)) {
            paymentTransaction.setStatus(PaymentTransactionStatus.FAILED);
            paymentTransaction.setFailureReason("Amount mismatch");
            paymentTransactionRepository.save(paymentTransaction);

            throw new AppException(ErrorCode.INVALID_PAYMENT_AMOUNT);
        }

        String responseCode = params.get("vnp_ResponseCode");
        String transactionStatus = params.get("vnp_TransactionStatus");

        if ("00".equals(responseCode) && "00".equals(transactionStatus)) {
            completeDeposit(paymentTransaction);
        } else {
            paymentTransaction.setStatus(PaymentTransactionStatus.FAILED);
            paymentTransaction.setFailureReason(
                    "VNPAY failed: responseCode=" + responseCode
                            + ", transactionStatus=" + transactionStatus
            );

            paymentTransactionRepository.save(paymentTransaction);
        }
    }

    public void completeDeposit(PaymentTransaction paymentTransaction) {
        Wallet wallet = walletRepository.findByWalletIdForUpdate(
                paymentTransaction.getWallet().getWalletId()
        ).orElseThrow(() -> new AppException(ErrorCode.WALLET_NOT_FOUND));

        validateWalletActive(wallet);

        BigDecimal balanceBefore = wallet.getBalance();
        BigDecimal balanceAfter = balanceBefore.add(paymentTransaction.getAmount());

        wallet.setBalance(balanceAfter);
        Wallet savedWallet = walletRepository.save(wallet);

        Transaction walletTransaction = Transaction.builder()
                .wallet(savedWallet)
                .invoice(null)
                .raceResultId(null)
                .contractId(null)
                .type(TransactionType.DEPOSIT)
                .direction(TransactionDirection.CREDIT)
                .amount(paymentTransaction.getAmount())
                .balanceBefore(balanceBefore)
                .balanceAfter(balanceAfter)
                .counterpartyWalletId(null)
                .counterpartyType(CounterpartyType.EXTERNAL)
                .transactionGroupId(UUID.randomUUID())
                .status(TransactionStatus.SUCCESS)
                .note("VNPAY deposit: " + paymentTransaction.getVnpTxnRef())
                .build();

        Transaction savedWalletTransaction = transactionRepository.save(walletTransaction);

        paymentTransaction.setWalletTransactionId(savedWalletTransaction.getTransactionId());
        paymentTransaction.setStatus(PaymentTransactionStatus.SUCCESS);
        paymentTransaction.setCompletedAt(LocalDateTime.now());

        paymentTransactionRepository.save(paymentTransaction);
    }

    public void verifySecureHash(Map<String, String> params) {
        String receivedHash = params.get("vnp_SecureHash");

        if (receivedHash == null || receivedHash.isBlank()) {
            throw new AppException(ErrorCode.INVALID_VNPAY_SIGNATURE);
        }

        String hashData = VnpayUtil.buildHashData(params);

        String calculatedHash = VnpayUtil.hmacSHA512(
                vnpayProperties.getHashSecret(),
                hashData
        );

        if (!calculatedHash.equalsIgnoreCase(receivedHash)) {
            throw new AppException(ErrorCode.INVALID_VNPAY_SIGNATURE);
        }
    }

    private boolean isAmountMatched(
            PaymentTransaction paymentTransaction,
            Map<String, String> params
    ) {
        String rawVnpAmount = params.get("vnp_Amount");

        if (rawVnpAmount == null || rawVnpAmount.isBlank()) {
            return false;
        }

        long receivedAmount = Long.parseLong(rawVnpAmount);

        return receivedAmount == paymentTransaction.getVnpAmount();
    }

    private void applyVnpayFields(
            PaymentTransaction paymentTransaction,
            Map<String, String> params
    ) {
        paymentTransaction.setVnpResponseCode(params.get("vnp_ResponseCode"));
        paymentTransaction.setVnpTransactionStatus(params.get("vnp_TransactionStatus"));
        paymentTransaction.setVnpTransactionNo(params.get("vnp_TransactionNo"));
        paymentTransaction.setVnpBankCode(params.get("vnp_BankCode"));
        paymentTransaction.setVnpBankTranNo(params.get("vnp_BankTranNo"));
        paymentTransaction.setVnpCardType(params.get("vnp_CardType"));
        paymentTransaction.setVnpSecureHash(params.get("vnp_SecureHash"));

        String payDate = params.get("vnp_PayDate");

        if (payDate != null && !payDate.isBlank()) {
            paymentTransaction.setVnpPayDate(
                    LocalDateTime.parse(payDate, VNPAY_DATE_FORMATTER)
            );
        }
    }

    private String toJson(Map<String, String> params) {
        try {
            return objectMapper.writeValueAsString(params);
        } catch (Exception exception) {
            return params.toString();
        }
    }

    private void validateWalletActive(Wallet wallet){
        if(wallet.getStatus() == WalletStatus.CLOSED)
            throw new AppException(ErrorCode.WALLET_CLOSED);

        if(wallet.getStatus() == FROZEN)
            throw new AppException(ErrorCode.WALLET_FROZEN);
    }
}

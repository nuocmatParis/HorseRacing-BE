package com.swp391.horseracing.service.impl;

import com.swp391.horseracing.config.VnpayProperties;
import com.swp391.horseracing.dto.wallet.request.DepositRequest;
import com.swp391.horseracing.dto.wallet.response.VnpayDepositResponse;
import com.swp391.horseracing.entity.PaymentTransaction;
import com.swp391.horseracing.entity.User;
import com.swp391.horseracing.entity.Wallet;
import com.swp391.horseracing.enums.PaymentProvider;
import com.swp391.horseracing.enums.PaymentPurpose;
import com.swp391.horseracing.enums.PaymentTransactionStatus;
import com.swp391.horseracing.enums.WalletPurpose;
import com.swp391.horseracing.exception.AppException;
import com.swp391.horseracing.exception.ErrorCode;
import com.swp391.horseracing.repository.PaymentTransactionRepository;
import com.swp391.horseracing.repository.WalletRepository;
import com.swp391.horseracing.service.UserCurrentService;
import com.swp391.horseracing.service.VnpayPaymentService;
import com.swp391.horseracing.util.VnpayUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class VnpayPaymentServiceImpl implements VnpayPaymentService {
    VnpayProperties vnpayProperties;
    PaymentTransactionRepository paymentTransactionRepository;
    WalletRepository walletRepository;
    UserCurrentService userCurrentService;

    static DateTimeFormatter VNPAY_DATE_FORMATTER =
            DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    @Transactional
    public VnpayDepositResponse createDepositPayment(
            DepositRequest request,
            HttpServletRequest servletRequest
    ) {
        User currentUser = userCurrentService.getCurrentUser();

        Wallet wallet = walletRepository.findByUser_UserIdAndWalletPurpose(
                currentUser.getUserId(),
                WalletPurpose.USER_MAIN
        ).orElseThrow(() -> new AppException(ErrorCode.WALLET_NOT_FOUND));

        BigDecimal amount = request.getAmount();

        if (amount == null || amount.compareTo(BigDecimal.valueOf(1000)) < 0) {
            throw new AppException(ErrorCode.INVALID_AMOUNT);
        }

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime expireAt = now.plusMinutes(15);

        String vnpTxnRef = generateVnpTxnRef();

        String orderInfo = request.getDescription();

        if (orderInfo == null || orderInfo.isBlank()) {
            orderInfo = "Deposit wallet via VNPAY";
        }

        long vnpAmount = amount.multiply(BigDecimal.valueOf(100)).longValue();

        PaymentTransaction paymentTransaction = PaymentTransaction.builder()
                .user(currentUser)
                .wallet(wallet)
                .provider(PaymentProvider.VNPAY)
                .purpose(PaymentPurpose.WALLET_DEPOSIT)
                .amount(amount)
                .currency("VND")
                .status(PaymentTransactionStatus.CREATED)
                .vnpTxnRef(vnpTxnRef)
                .vnpOrderInfo(orderInfo)
                .clientIp(VnpayUtil.getIpAddress(servletRequest))
                .expireAt(expireAt)
                .vnpAmount(vnpAmount)
                .build();

        paymentTransactionRepository.save(paymentTransaction);

        String paymentUrl = buildPaymentUrl(paymentTransaction, now, expireAt);

        paymentTransaction.setPaymentUrl(paymentUrl);
        paymentTransaction.setStatus(PaymentTransactionStatus.PENDING);

        PaymentTransaction savedPayment = paymentTransactionRepository.save(paymentTransaction);

        return VnpayDepositResponse.builder()
                .paymentTransactionId(savedPayment.getPaymentTransactionId())
                .vnpTxnRef(savedPayment.getVnpTxnRef())
                .amount(savedPayment.getAmount())
                .currency(savedPayment.getCurrency())
                .status(savedPayment.getStatus().name())
                .paymentUrl(savedPayment.getPaymentUrl())
                .build();
    }

    private String buildPaymentUrl(
            PaymentTransaction paymentTransaction,
            LocalDateTime createDate,
            LocalDateTime expireDate
    ) {
        Map<String, String> params = new HashMap<>();

        params.put("vnp_Version", "2.1.0");
        params.put("vnp_Command", "pay");
        params.put("vnp_TmnCode", vnpayProperties.getTmnCode());

        params.put("vnp_Amount", String.valueOf(paymentTransaction.getVnpAmount()));
        params.put("vnp_CurrCode", "VND");

        params.put("vnp_TxnRef", paymentTransaction.getVnpTxnRef());
        params.put("vnp_OrderInfo", paymentTransaction.getVnpOrderInfo());
        params.put("vnp_OrderType", "other");

        params.put("vnp_Locale", "vn");
        params.put("vnp_ReturnUrl", vnpayProperties.getReturnUrl());
        params.put("vnp_IpAddr", paymentTransaction.getClientIp());

        params.put("vnp_CreateDate", createDate.format(VNPAY_DATE_FORMATTER));
        params.put("vnp_ExpireDate", expireDate.format(VNPAY_DATE_FORMATTER));

        String hashData = VnpayUtil.buildHashData(params);

        String secureHash = VnpayUtil.hmacSHA512(
                vnpayProperties.getHashSecret(),
                hashData
        );

        params.put("vnp_SecureHash", secureHash);

        return vnpayProperties.getPayUrl() + "?" + VnpayUtil.buildQueryUrl(params);
    }

    private String generateVnpTxnRef() {
        return "DEP_" + UUID.randomUUID().toString().replace("-", "");
    }
}

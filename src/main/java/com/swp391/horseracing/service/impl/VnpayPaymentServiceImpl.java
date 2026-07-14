package com.swp391.horseracing.service.impl;

import com.swp391.horseracing.config.VnpayProperties;
import com.swp391.horseracing.dto.wallet.request.DepositRequest;
import com.swp391.horseracing.dto.wallet.request.AdminPrizePoolTopUpRequest;
import com.swp391.horseracing.dto.wallet.response.VnpayDepositResponse;
import com.swp391.horseracing.entity.PaymentTransaction;
import com.swp391.horseracing.entity.User;
import com.swp391.horseracing.entity.Wallet;
import com.swp391.horseracing.enums.PaymentProvider;
import com.swp391.horseracing.enums.PaymentPurpose;
import com.swp391.horseracing.enums.PaymentTransactionStatus;
import com.swp391.horseracing.enums.WalletPurpose;
import com.swp391.horseracing.enums.WalletOwnerType;
import com.swp391.horseracing.enums.WalletStatus;
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

        String orderInfo = request.getDescription();
        if (orderInfo == null || orderInfo.isBlank()) {
            orderInfo = "Nạp tiền vào ví HRTMS";
        }

        return createPayment(
                currentUser,
                wallet,
                request.getAmount(),
                orderInfo,
                PaymentPurpose.WALLET_DEPOSIT,
                "DEP_",
                servletRequest);
    }

    @Override
    @Transactional
    public VnpayDepositResponse createPrizePoolTopUpPayment(
            AdminPrizePoolTopUpRequest request,
            HttpServletRequest servletRequest) {
        validatePrizePoolRequest(request);

        User currentAdmin = userCurrentService.getCurrentUser();
        Wallet prizePoolWallet = walletRepository.findByOwnerTypeAndUserIsNullAndWalletPurpose(
                        WalletOwnerType.SYSTEM,
                        WalletPurpose.SYSTEM_PRIZE_POOL)
                .orElseGet(() -> walletRepository.save(Wallet.builder()
                        .ownerType(WalletOwnerType.SYSTEM)
                        .walletPurpose(WalletPurpose.SYSTEM_PRIZE_POOL)
                        .balance(BigDecimal.ZERO)
                        .currency("VND")
                        .status(WalletStatus.ACTIVE)
                        .user(null)
                        .build()));

        validateWalletAvailable(prizePoolWallet);

        String orderInfo = "Bổ sung Quỹ giải thưởng: " + request.getReason().trim();
        if (orderInfo.length() > 250) {
            orderInfo = orderInfo.substring(0, 250);
        }

        return createPayment(
                currentAdmin,
                prizePoolWallet,
                request.getAmount(),
                orderInfo,
                PaymentPurpose.SYSTEM_PRIZE_POOL_TOP_UP,
                "PRIZE_",
                servletRequest);
    }

    private VnpayDepositResponse createPayment(
            User user,
            Wallet wallet,
            BigDecimal amount,
            String orderInfo,
            PaymentPurpose purpose,
            String transactionPrefix,
            HttpServletRequest servletRequest) {
        if (amount == null || amount.compareTo(BigDecimal.valueOf(1000)) < 0) {
            throw new AppException(ErrorCode.INVALID_AMOUNT);
        }

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime expireAt = now.plusMinutes(15);

        String vnpTxnRef = generateVnpTxnRef(transactionPrefix);

        long vnpAmount = amount.multiply(BigDecimal.valueOf(100)).longValue();

        PaymentTransaction paymentTransaction = PaymentTransaction.builder()
                .user(user)
                .wallet(wallet)
                .provider(PaymentProvider.VNPAY)
                .purpose(purpose)
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

    private void validatePrizePoolRequest(AdminPrizePoolTopUpRequest request) {
        if (request == null || request.getAmount() == null
                || request.getAmount().compareTo(BigDecimal.valueOf(1000)) < 0
                || request.getAmount().stripTrailingZeros().scale() > 0) {
            throw new AppException(ErrorCode.INVALID_AMOUNT);
        }
        String reason = request.getReason();
        if (reason == null || reason.trim().length() < 10 || reason.trim().length() > 500) {
            throw new AppException(ErrorCode.VALIDATION_FAILED);
        }
    }

    private void validateWalletAvailable(Wallet wallet) {
        if (wallet.getStatus() == WalletStatus.FROZEN) {
            throw new AppException(ErrorCode.WALLET_FROZEN);
        }
        if (wallet.getStatus() == WalletStatus.CLOSED) {
            throw new AppException(ErrorCode.WALLET_CLOSED);
        }
    }

    private String generateVnpTxnRef(String prefix) {
        return prefix + UUID.randomUUID().toString().replace("-", "");
    }
}

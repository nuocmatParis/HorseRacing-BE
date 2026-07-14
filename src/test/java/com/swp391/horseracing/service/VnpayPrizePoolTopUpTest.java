package com.swp391.horseracing.service;

import com.swp391.horseracing.config.VnpayProperties;
import com.swp391.horseracing.dto.wallet.request.AdminPrizePoolTopUpRequest;
import com.swp391.horseracing.dto.wallet.response.VnpayDepositResponse;
import com.swp391.horseracing.entity.PaymentTransaction;
import com.swp391.horseracing.entity.Transaction;
import com.swp391.horseracing.entity.User;
import com.swp391.horseracing.entity.Wallet;
import com.swp391.horseracing.enums.PaymentPurpose;
import com.swp391.horseracing.enums.PaymentTransactionStatus;
import com.swp391.horseracing.enums.TransactionType;
import com.swp391.horseracing.enums.WalletOwnerType;
import com.swp391.horseracing.enums.WalletPurpose;
import com.swp391.horseracing.enums.WalletStatus;
import com.swp391.horseracing.repository.PaymentTransactionRepository;
import com.swp391.horseracing.repository.WalletRepository;
import com.swp391.horseracing.repository.WalletTransactionRepository;
import com.swp391.horseracing.service.impl.VnpayCallbackServiceImpl;
import com.swp391.horseracing.service.impl.VnpayPaymentServiceImpl;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class VnpayPrizePoolTopUpTest {

    @Test
    void adminTopUpCreatesPendingVnpayPaymentForPrizePool() {
        VnpayProperties properties = properties();
        PaymentTransactionRepository paymentRepository = mock(PaymentTransactionRepository.class);
        WalletRepository walletRepository = mock(WalletRepository.class);
        UserCurrentService userCurrentService = mock(UserCurrentService.class);
        HttpServletRequest servletRequest = mock(HttpServletRequest.class);
        User admin = User.builder().userId(UUID.randomUUID()).fullName("Admin Demo").build();
        Wallet prizePool = prizePool("0");

        when(userCurrentService.getCurrentUser()).thenReturn(admin);
        when(walletRepository.findByOwnerTypeAndUserIsNullAndWalletPurpose(
                WalletOwnerType.SYSTEM, WalletPurpose.SYSTEM_PRIZE_POOL))
                .thenReturn(Optional.empty());
        when(walletRepository.save(any(Wallet.class))).thenReturn(prizePool);
        when(servletRequest.getHeader("X-FORWARDED-FOR")).thenReturn(null);
        when(servletRequest.getRemoteAddr()).thenReturn("127.0.0.1");
        when(paymentRepository.save(any(PaymentTransaction.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        VnpayPaymentServiceImpl service = new VnpayPaymentServiceImpl(
                properties, paymentRepository, walletRepository, userCurrentService);
        AdminPrizePoolTopUpRequest request = AdminPrizePoolTopUpRequest.builder()
                .amount(new BigDecimal("100000000"))
                .reason("Bổ sung quỹ giải thưởng phục vụ demo")
                .build();

        VnpayDepositResponse response = service.createPrizePoolTopUpPayment(request, servletRequest);

        assertEquals(PaymentTransactionStatus.PENDING.name(), response.getStatus());
        assertTrue(response.getVnpTxnRef().startsWith("PRIZE_"));
        assertTrue(response.getPaymentUrl().contains("vnp_TxnRef=PRIZE_"));
        ArgumentCaptor<PaymentTransaction> captor = ArgumentCaptor.forClass(PaymentTransaction.class);
        verify(paymentRepository, org.mockito.Mockito.atLeastOnce()).save(captor.capture());
        PaymentTransaction saved = captor.getValue();
        assertEquals(PaymentPurpose.SYSTEM_PRIZE_POOL_TOP_UP, saved.getPurpose());
        assertSame(prizePool, saved.getWallet());
        assertSame(admin, saved.getUser());
    }

    @Test
    void successfulPrizePoolCallbackCreditsOnlyPrizePoolAndCreatesAuditTransaction() {
        PaymentTransactionRepository paymentRepository = mock(PaymentTransactionRepository.class);
        WalletRepository walletRepository = mock(WalletRepository.class);
        WalletTransactionRepository transactionRepository = mock(WalletTransactionRepository.class);
        Wallet prizePool = prizePool("50000000");
        User admin = User.builder().userId(UUID.randomUUID()).fullName("Admin Demo").build();
        PaymentTransaction payment = PaymentTransaction.builder()
                .user(admin)
                .wallet(prizePool)
                .purpose(PaymentPurpose.SYSTEM_PRIZE_POOL_TOP_UP)
                .amount(new BigDecimal("100000000"))
                .status(PaymentTransactionStatus.PENDING)
                .vnpTxnRef("PRIZE_test")
                .vnpOrderInfo("Bổ sung Quỹ giải thưởng: phục vụ demo")
                .build();

        when(walletRepository.findByWalletIdForUpdate(prizePool.getWalletId()))
                .thenReturn(Optional.of(prizePool));
        when(walletRepository.save(prizePool)).thenReturn(prizePool);
        when(transactionRepository.save(any(Transaction.class)))
                .thenAnswer(invocation -> {
                    Transaction transaction = invocation.getArgument(0);
                    transaction.setTransactionId(UUID.randomUUID());
                    return transaction;
                });

        VnpayCallbackServiceImpl service = new VnpayCallbackServiceImpl(
                properties(), paymentRepository, walletRepository, transactionRepository);
        service.completeDeposit(payment);

        assertEquals(new BigDecimal("150000000"), prizePool.getBalance());
        assertEquals(PaymentTransactionStatus.SUCCESS, payment.getStatus());
        ArgumentCaptor<Transaction> captor = ArgumentCaptor.forClass(Transaction.class);
        verify(transactionRepository).save(captor.capture());
        assertEquals(TransactionType.SYSTEM_PRIZE_POOL_TOP_UP, captor.getValue().getType());
        assertSame(admin, captor.getValue().getPerformedBy());
    }

    private Wallet prizePool(String balance) {
        return Wallet.builder()
                .walletId(UUID.randomUUID())
                .ownerType(WalletOwnerType.SYSTEM)
                .walletPurpose(WalletPurpose.SYSTEM_PRIZE_POOL)
                .status(WalletStatus.ACTIVE)
                .balance(new BigDecimal(balance))
                .currency("VND")
                .build();
    }

    private VnpayProperties properties() {
        VnpayProperties properties = new VnpayProperties();
        properties.setTmnCode("DEMO");
        properties.setHashSecret("demo-secret");
        properties.setPayUrl("https://sandbox.vnpayment.vn/paymentv2/vpcpay.html");
        properties.setReturnUrl("https://backend.example/api/payments/vnpay/return");
        return properties;
    }
}

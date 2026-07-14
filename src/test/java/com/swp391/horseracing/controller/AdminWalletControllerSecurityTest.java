package com.swp391.horseracing.controller;

import com.swp391.horseracing.dto.wallet.request.AdminPrizePoolTopUpRequest;
import com.swp391.horseracing.service.WalletService;
import com.swp391.horseracing.service.VnpayPaymentService;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

class AdminWalletControllerSecurityTest {

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void nonAdminCannotTopUpSystemPrizePool() {
        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(TestConfig.class)) {
            WalletService walletService = context.getBean(WalletService.class);
            VnpayPaymentService paymentService = context.getBean(VnpayPaymentService.class);
            AdminWalletController controller = context.getBean(AdminWalletController.class);
            UsernamePasswordAuthenticationToken ownerAuthentication =
                    new UsernamePasswordAuthenticationToken(
                            "owner",
                            "not-used",
                            List.of(new SimpleGrantedAuthority("ROLE_OWNER")));
            SecurityContextHolder.getContext().setAuthentication(ownerAuthentication);
            AdminPrizePoolTopUpRequest request = AdminPrizePoolTopUpRequest.builder()
                    .amount(new BigDecimal("5000000"))
                    .reason("Bổ sung quỹ giải thưởng tháng 7")
                    .build();

            HttpServletRequest servletRequest = mock(HttpServletRequest.class);
            assertThrows(AccessDeniedException.class, () -> controller.topUpPrizePool(request, servletRequest));
            verifyNoInteractions(walletService);
            verifyNoInteractions(paymentService);
        }
    }

    @Configuration(proxyBeanMethods = false)
    @EnableMethodSecurity
    static class TestConfig {

        @Bean
        WalletService walletService() {
            return mock(WalletService.class);
        }

        @Bean
        VnpayPaymentService vnpayPaymentService() {
            return mock(VnpayPaymentService.class);
        }

        @Bean
        AdminWalletController adminWalletController(
                WalletService walletService,
                VnpayPaymentService vnpayPaymentService) {
            return new AdminWalletController(walletService, vnpayPaymentService);
        }
    }
}

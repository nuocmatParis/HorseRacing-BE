package com.swp391.horseracing.controller;

import com.swp391.horseracing.config.VnpayProperties;
import com.swp391.horseracing.exception.AppException;
import com.swp391.horseracing.exception.ErrorCode;
import com.swp391.horseracing.service.VnpayCallbackService;
import com.swp391.horseracing.service.VnpayPaymentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class VnpayControllerTest {
    private VnpayCallbackService callbackService;
    private VnpayController controller;

    @BeforeEach
    void setUp() {
        VnpayPaymentService paymentService = mock(VnpayPaymentService.class);
        callbackService = mock(VnpayCallbackService.class);

        VnpayProperties properties = new VnpayProperties();
        properties.setFrontendReturnUrl("http://localhost:5173/vnpay/return");

        controller = new VnpayController(
                paymentService,
                callbackService,
                properties
        );
    }

    @Test
    void returnRedirectsToFrontendAfterSuccessfulCallback() {
        Map<String, String> params = successfulParams();

        ResponseEntity<Void> response = controller.vnpayReturn(params);

        assertEquals(HttpStatus.FOUND, response.getStatusCode());
        URI location = response.getHeaders().getLocation();
        assertNotNull(location);
        assertEquals(
                "/vnpay/return",
                location.getPath()
        );
        assertEquals(
                "success",
                queryValue(location, "paymentStatus")
        );
        assertEquals(
                "100000000",
                queryValue(location, "amount")
        );
        assertEquals(
                "DEP_test",
                queryValue(location, "transactionRef")
        );
        assertFalse(location.toString().contains("vnp_OrderInfo"));
        verify(callbackService).handleReturn(params);
    }

    @Test
    void returnRedirectsWithFailedStatusWhenCallbackCannotBeVerified() {
        Map<String, String> params = successfulParams();
        doThrow(new AppException(ErrorCode.INVALID_VNPAY_SIGNATURE))
                .when(callbackService)
                .handleReturn(params);

        ResponseEntity<Void> response = controller.vnpayReturn(params);

        URI location = response.getHeaders().getLocation();
        assertNotNull(location);
        assertEquals(
                "failed",
                queryValue(location, "paymentStatus")
        );
    }

    @Test
    void returnRedirectsWithFailedStatusWhenVnpayDeclinesPayment() {
        Map<String, String> params = successfulParams();
        params.put("vnp_ResponseCode", "24");

        ResponseEntity<Void> response = controller.vnpayReturn(params);

        URI location = response.getHeaders().getLocation();
        assertNotNull(location);
        assertEquals(
                "failed",
                queryValue(location, "paymentStatus")
        );
        assertEquals("24", queryValue(location, "responseCode"));
    }

    @Test
    void prizePoolReturnIncludesAdminPaymentTarget() {
        Map<String, String> params = successfulParams();
        params.put("vnp_TxnRef", "PRIZE_test");

        ResponseEntity<Void> response = controller.vnpayReturn(params);

        URI location = response.getHeaders().getLocation();
        assertNotNull(location);
        assertEquals("SYSTEM_PRIZE_POOL", queryValue(location, "paymentTarget"));
    }

    private Map<String, String> successfulParams() {
        Map<String, String> params = new LinkedHashMap<>();
        params.put("vnp_Amount", "100000000");
        params.put("vnp_OrderInfo", "Nạp tiền vào ví HRTMS");
        params.put("vnp_ResponseCode", "00");
        params.put("vnp_TxnRef", "DEP_test");
        params.put("vnp_SecureHash", "test_hash");
        return params;
    }

    private String queryValue(URI uri, String name) {
        return UriComponentsBuilder.fromUri(uri)
                .build()
                .getQueryParams()
                .getFirst(name);
    }
}

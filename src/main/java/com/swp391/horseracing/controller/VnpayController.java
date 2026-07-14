package com.swp391.horseracing.controller;

import com.swp391.horseracing.config.VnpayProperties;
import com.swp391.horseracing.dto.common.ApiResponse;
import com.swp391.horseracing.dto.wallet.request.DepositRequest;
import com.swp391.horseracing.dto.wallet.response.VnpayDepositResponse;
import com.swp391.horseracing.exception.AppException;
import com.swp391.horseracing.service.VnpayCallbackService;
import com.swp391.horseracing.service.VnpayPaymentService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/payments/vnpay")
@RequiredArgsConstructor
public class VnpayController {
    private static final String DEFAULT_FRONTEND_RETURN_URL =
            "http://localhost:5173/vnpay/return";

    private final VnpayPaymentService vnpayPaymentService;
    private final VnpayCallbackService vnpayCallbackService;
    private final VnpayProperties vnpayProperties;

    @PostMapping("/deposit")
    public ApiResponse<VnpayDepositResponse> createDepositPayment(
            @RequestBody @Valid DepositRequest request,
            HttpServletRequest servletRequest) {
        return ApiResponse.<VnpayDepositResponse>builder()
                .result(vnpayPaymentService.createDepositPayment(request, servletRequest))
                .build();
    }

    @GetMapping("/return")
    public ResponseEntity<Void> vnpayReturn(
            @RequestParam Map<String, String> params) {
        boolean callbackProcessed = true;

        try {
            vnpayCallbackService.handleReturn(params);
        } catch (AppException exception) {
            callbackProcessed = false;
            log.warn(
                    "Cannot process VNPay return for transaction {}: {}",
                    params.get("vnp_TxnRef"),
                    exception.getMessage()
            );
        } catch (Exception exception) {
            callbackProcessed = false;
            log.error(
                    "Unexpected error while processing VNPay return for transaction {}",
                    params.get("vnp_TxnRef"),
                    exception
            );
        }

        URI frontendReturnUri = buildFrontendReturnUri(params, callbackProcessed);

        return ResponseEntity.status(HttpStatus.FOUND)
                .location(frontendReturnUri)
                .build();
    }

    @GetMapping("/ipn")
    public Map<String, String> vnpayIpn(
            @RequestParam Map<String, String> params) {
        try {
            vnpayCallbackService.handleIpn(params);

            return Map.of(
                    "RspCode", "00",
                    "Message", "Confirm Success"
            );
        } catch (AppException exception) {
            return Map.of(
                    "RspCode", "99",
                    "Message", exception.getMessage()
            );
        } catch (Exception exception) {
            return Map.of(
                    "RspCode", "99",
                    "Message", "Unknown error"
            );
        }
    }

    private URI buildFrontendReturnUri(
            Map<String, String> params,
            boolean callbackProcessed) {
        String responseCode = params.get("vnp_ResponseCode");
        String transactionStatus = params.get("vnp_TransactionStatus");
        boolean vnpaySuccessful = "00".equals(responseCode)
                && (transactionStatus == null
                || transactionStatus.isBlank()
                || "00".equals(transactionStatus));
        boolean paymentSuccessful = callbackProcessed && vnpaySuccessful;

        UriComponentsBuilder uriBuilder = UriComponentsBuilder
                .fromUriString(resolveFrontendReturnUrl())
                .queryParam(
                        "paymentStatus",
                        paymentSuccessful ? "success" : "failed"
                );

        addQueryParamIfPresent(uriBuilder, "responseCode", responseCode);
        addQueryParamIfPresent(
                uriBuilder,
                "transactionStatus",
                transactionStatus
        );
        addQueryParamIfPresent(
                uriBuilder,
                "transactionRef",
                params.get("vnp_TxnRef")
        );
        addQueryParamIfPresent(
                uriBuilder,
                "amount",
                params.get("vnp_Amount")
        );
        String transactionRef = params.get("vnp_TxnRef");
        if (transactionRef != null && transactionRef.startsWith("PRIZE_")) {
            uriBuilder.queryParam("paymentTarget", "SYSTEM_PRIZE_POOL");
        }

        return uriBuilder.build()
                .encode()
                .toUri();
    }

    private String resolveFrontendReturnUrl() {
        String configuredUrl = vnpayProperties.getFrontendReturnUrl();

        if (configuredUrl == null || configuredUrl.isBlank()) {
            return DEFAULT_FRONTEND_RETURN_URL;
        }

        return configuredUrl.trim();
    }

    private void addQueryParamIfPresent(
            UriComponentsBuilder uriBuilder,
            String name,
            String value) {
        if (value != null && !value.isBlank()) {
            uriBuilder.queryParam(name, value);
        }
    }
}

package com.swp391.horseracing.controller;
import com.swp391.horseracing.dto.common.ApiResponse;
import com.swp391.horseracing.dto.wallet.request.DepositRequest;
import com.swp391.horseracing.dto.wallet.response.VnpayDepositResponse;
import com.swp391.horseracing.exception.AppException;
import com.swp391.horseracing.service.VnpayCallbackService;
import com.swp391.horseracing.service.VnpayPaymentService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/payments/vnpay")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class VnpayController {
    private final VnpayPaymentService vnpayPaymentService;
    private final VnpayCallbackService vnpayCallbackService;

    @PostMapping("/deposit")
    public ApiResponse<VnpayDepositResponse> createDepositPayment(
            @RequestBody @Valid DepositRequest request,
            HttpServletRequest servletRequest) {
        return ApiResponse.<VnpayDepositResponse>builder()
                .result(vnpayPaymentService.createDepositPayment(request, servletRequest))
                .build();
    }

    @GetMapping("/return")
    public ApiResponse<Void> vnpayReturn(@RequestParam Map<String, String> params) {
        vnpayCallbackService.handleReturn(params);

        return ApiResponse.<Void>builder()
                .build();
    }

    @GetMapping("/ipn")
    public Map<String, String> vnpayIpn(@RequestParam Map<String, String> params) {
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
}

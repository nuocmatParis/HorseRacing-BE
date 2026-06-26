package com.swp391.horseracing.controller;
import com.swp391.horseracing.dto.common.ApiResponse;
import com.swp391.horseracing.dto.wallet.request.DepositRequest;
import com.swp391.horseracing.dto.wallet.response.VnpayDepositResponse;
import com.swp391.horseracing.exception.AppException;
import com.swp391.horseracing.service.VnpayCallbackService;
import com.swp391.horseracing.service.VnpayPaymentService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse; // <-- Thêm import này
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.NonFinal; // <-- Thêm import này nếu dùng @Value với @RequiredArgsConstructor
import org.springframework.beans.factory.annotation.Value; // <-- Thêm import này
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/payments/vnpay")
@RequiredArgsConstructor
public class VnpayController {
    private final VnpayPaymentService vnpayPaymentService;
    private final VnpayCallbackService vnpayCallbackService;

    @Value("${vnpay.frontend-url:http://localhost:5173/vnpay/return}")
    @NonFinal
    private String frontendUrl;

    @PostMapping("/deposit")
    public ApiResponse<VnpayDepositResponse> createDepositPayment(
            @RequestBody @Valid DepositRequest request,
            HttpServletRequest servletRequest) {
        return ApiResponse.<VnpayDepositResponse>builder()
                .result(vnpayPaymentService.createDepositPayment(request, servletRequest))
                .build();
    }

    // Thay đổi API này để redirect thay vì trả JSON trống
    @GetMapping("/return")
    public void vnpayReturn(@RequestParam Map<String, String> params, HttpServletResponse response) throws IOException {
        try {
            vnpayCallbackService.handleReturn(params);
        } catch (Exception e) {
            // Log lỗi nếu cần thiết
        }

        // Chuyển query parameters gốc từ VNPay sang URL frontend
        String queryString = params.entrySet().stream()
                .map(entry -> entry.getKey() + "=" + entry.getValue())
                .collect(Collectors.joining("&"));

        // Redirect thẳng về Frontend
        response.sendRedirect(frontendUrl + "?" + queryString);
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

package com.swp391.horseracing.service;

import com.swp391.horseracing.dto.wallet.request.DepositRequest;
import com.swp391.horseracing.dto.wallet.request.AdminPrizePoolTopUpRequest;
import com.swp391.horseracing.dto.wallet.response.VnpayDepositResponse;
import jakarta.servlet.http.HttpServletRequest;

public interface VnpayPaymentService {
    public VnpayDepositResponse createDepositPayment(DepositRequest request,
                                                     HttpServletRequest servletRequest);

    VnpayDepositResponse createPrizePoolTopUpPayment(
            AdminPrizePoolTopUpRequest request,
            HttpServletRequest servletRequest);
}

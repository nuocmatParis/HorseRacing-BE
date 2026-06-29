package com.swp391.horseracing.service;

import com.swp391.horseracing.entity.PaymentTransaction;

import java.util.Map;

public interface VnpayCallbackService {
    void handleReturn(Map<String, String> params);

    void handleIpn(Map<String, String> params);

    void completeDeposit(PaymentTransaction paymentTransaction);

    void verifySecureHash(Map<String, String> params);
}

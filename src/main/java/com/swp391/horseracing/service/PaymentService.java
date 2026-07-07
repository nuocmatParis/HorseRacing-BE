package com.swp391.horseracing.service;

import com.swp391.horseracing.dto.invoice.response.PaymentResponse;

import java.util.UUID;

public interface PaymentService {
    PaymentResponse payInvoice(UUID invoiceId);

    PaymentResponse refundInvoice(UUID invoiceId);

    PaymentResponse payHiringFee(UUID contractId);
}

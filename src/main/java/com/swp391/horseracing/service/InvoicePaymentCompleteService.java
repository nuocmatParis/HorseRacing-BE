package com.swp391.horseracing.service;

import com.swp391.horseracing.entity.Invoice;

public interface InvoicePaymentCompleteService {
    void handleAfterPaid(Invoice invoice);
}

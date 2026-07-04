package com.swp391.horseracing.service;

import com.swp391.horseracing.dto.invoice.response.InvoiceResponse;
import com.swp391.horseracing.entity.HorseTournamentRegistration;
import com.swp391.horseracing.entity.Invoice;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public interface InvoiceService {
    List<InvoiceResponse> getMyInvoices();

    Invoice createOwnerRegistrationInvoice(UUID payerUserId, HorseTournamentRegistration registration, BigDecimal amount);
}

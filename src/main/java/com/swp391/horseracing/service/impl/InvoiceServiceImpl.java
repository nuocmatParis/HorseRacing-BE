package com.swp391.horseracing.service.impl;

import com.swp391.horseracing.dto.invoice.response.InvoiceResponse;
import com.swp391.horseracing.entity.HorseTournamentRegistration;
import com.swp391.horseracing.entity.Invoice;
import com.swp391.horseracing.entity.User;
import com.swp391.horseracing.enums.InvoiceStatus;
import com.swp391.horseracing.enums.InvoiceType;
import com.swp391.horseracing.exception.AppException;
import com.swp391.horseracing.exception.ErrorCode;
import com.swp391.horseracing.mapper.InvoiceMapper;
import com.swp391.horseracing.repository.InvoiceRepository;
import com.swp391.horseracing.repository.UserRepository;
import com.swp391.horseracing.service.InvoiceService;
import com.swp391.horseracing.service.UserCurrentService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class InvoiceServiceImpl implements InvoiceService {
    InvoiceMapper invoiceMapper;
    InvoiceRepository invoiceRepository;
    UserCurrentService userCurrentService;
    UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public List<InvoiceResponse> getMyInvoices() {
        User currentUser = userCurrentService.getCurrentUser();

        List<Invoice> list = invoiceRepository.findAllByPayerUser_UserIdOrderByCreatedAtDesc(currentUser.getUserId());

        List<InvoiceResponse> responses = new ArrayList<>();

        for (Invoice invoice : list){
            responses.add(invoiceMapper.toInvoiceResponse(invoice));
        }

        return responses;
    }

    @Override
    @Transactional
    public Invoice createOwnerRegistrationInvoice(UUID payerUserId, HorseTournamentRegistration registration, BigDecimal amount) {
        if(invoiceRepository.existsByHorseTournamentRegistration_HorseRegistrationIdAndInvoiceType(
                registration.getHorseRegistrationId(), InvoiceType.OWNER_TOURNAMENT_REGISTRATION_FEE))
            throw new AppException(ErrorCode.INVOICE_ALREADY_EXISTS);

        User payer = userRepository.findByUserId(payerUserId).orElseThrow(()
                -> new AppException(ErrorCode.USER_NOT_FOUND));

        Invoice invoice = Invoice.builder()
                .payerUser(payer)
                .amount(amount)
                .horseTournamentRegistration(registration)
                .contractId(null)
                .invoiceType(InvoiceType.OWNER_TOURNAMENT_REGISTRATION_FEE)
                .status(InvoiceStatus.UNPAID)
                .dueDate(LocalDateTime.now().plusDays(3))
                .note("Owner tournament registration fee")
                .build();

        return invoiceRepository.save(invoice);
    }

    @Override
    @Transactional
    public void cancelInvoice(UUID invoiceId) {
        Invoice invoice = invoiceRepository.findForUpdateByInvoiceId(invoiceId).orElseThrow(()
                -> new AppException(ErrorCode.INVOICE_NOT_FOUND));

        if(invoice.getStatus() == InvoiceStatus.PAID)
            throw new AppException(ErrorCode.PAID_INVOICE_CANNOT_BE_CANCELLED);

        if(invoice.getStatus() == InvoiceStatus.REFUNDED)
            throw new AppException(ErrorCode.INVOICE_ALREADY_REFUNDED);

        if(invoice.getStatus() == InvoiceStatus.CANCELLED)
            return;

        invoice.setStatus(InvoiceStatus.CANCELLED);
        invoiceRepository.save(invoice);
    }

    @Override
    public Invoice createContractCreationInvoice(UUID payerUserId, UUID contractId, BigDecimal amount) {
        return null;
    }

    @Override
    public Invoice createHiringFeeInvoice(UUID payerUserId, UUID contractId, BigDecimal amount) {
        return null;
    }

    @Override
    public Invoice getByContractIdAndType(UUID contractId) {
        return null;
    }
}

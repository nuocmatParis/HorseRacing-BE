package com.swp391.horseracing.service;

import com.swp391.horseracing.entity.HorseTournamentRegistration;
import com.swp391.horseracing.entity.Invoice;
import com.swp391.horseracing.enums.InvoiceType;
import com.swp391.horseracing.service.impl.InvoicePaymentCompleteServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

class InvoicePaymentCompleteServiceImplTest {
    private RegistrationPaymentService registrationPaymentService;
    private ContractPaymentService contractPaymentService;
    private ContractActivationService contractActivationService;
    private InvoicePaymentCompleteServiceImpl service;

    @BeforeEach
    void setUp() {
        registrationPaymentService = mock(RegistrationPaymentService.class);
        contractPaymentService = mock(ContractPaymentService.class);
        contractActivationService = mock(ContractActivationService.class);
        service = new InvoicePaymentCompleteServiceImpl(
                registrationPaymentService,
                contractPaymentService,
                contractActivationService);
    }

    @Test
    void ownerRegistrationPaymentKeepsItsExistingFlow() {
        UUID registrationId = UUID.randomUUID();
        HorseTournamentRegistration registration = HorseTournamentRegistration.builder()
                .horseRegistrationId(registrationId)
                .build();
        Invoice invoice = Invoice.builder()
                .invoiceType(InvoiceType.OWNER_TOURNAMENT_REGISTRATION_FEE)
                .horseTournamentRegistration(registration)
                .build();

        service.handleAfterPaid(invoice);

        verify(registrationPaymentService).markOwnerRegistrationPaid(registrationId);
        verifyNoInteractions(contractPaymentService, contractActivationService);
    }

    @Test
    void hiringFeePaymentMovesContractToHiringPaid() {
        UUID contractId = UUID.randomUUID();
        Invoice invoice = Invoice.builder()
                .invoiceType(InvoiceType.JOCKEY_HIRING_FEE)
                .contractId(contractId)
                .build();

        service.handleAfterPaid(invoice);

        verify(contractPaymentService).markHiringFeePaid(contractId);
        verifyNoInteractions(contractActivationService);
    }

    @Test
    void contractCreationFeeActivatesWithoutAdminReview() {
        UUID contractId = UUID.randomUUID();
        Invoice invoice = Invoice.builder()
                .invoiceType(InvoiceType.CONTRACT_CREATION_FEE)
                .contractId(contractId)
                .build();

        service.handleAfterPaid(invoice);

        verify(contractActivationService).activateAfterFullPayment(contractId);
        verify(contractPaymentService, never()).markHiringFeePaid(contractId);
    }

    @Test
    void contractInvoiceWithoutContractIdDoesNotCallDownstreamServices() {
        Invoice invoice = Invoice.builder()
                .invoiceType(InvoiceType.CONTRACT_CREATION_FEE)
                .build();

        service.handleAfterPaid(invoice);

        verifyNoInteractions(contractPaymentService, contractActivationService);
    }
}

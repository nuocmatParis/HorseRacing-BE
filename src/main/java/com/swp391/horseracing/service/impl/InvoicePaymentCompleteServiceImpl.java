package com.swp391.horseracing.service.impl;

import com.swp391.horseracing.entity.Invoice;
import com.swp391.horseracing.enums.InvoiceType;
import com.swp391.horseracing.service.ContractPaymentService;
import com.swp391.horseracing.service.ContractActivationService;
import com.swp391.horseracing.service.InvoicePaymentCompleteService;
import com.swp391.horseracing.service.RegistrationPaymentService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;

@Service
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequiredArgsConstructor
public class InvoicePaymentCompleteServiceImpl implements InvoicePaymentCompleteService {
    RegistrationPaymentService registrationPaymentService;
    ContractPaymentService contractPaymentService;
    ContractActivationService contractActivationService;

    @Override
    public void handleAfterPaid(Invoice invoice) {
        if (invoice.getInvoiceType() == InvoiceType.OWNER_TOURNAMENT_REGISTRATION_FEE) {
            if (invoice.getHorseTournamentRegistration() != null) {
                registrationPaymentService.markOwnerRegistrationPaid(
                        invoice.getHorseTournamentRegistration().getHorseRegistrationId());
            }

        }

        if(invoice.getInvoiceType() == InvoiceType.JOCKEY_HIRING_FEE){
            if(invoice.getContractId() != null)
                contractPaymentService.markHiringFeePaid(invoice.getContractId());
        }

        if(invoice.getInvoiceType() == InvoiceType.CONTRACT_CREATION_FEE) {
            if(invoice.getContractId() != null) {
                contractActivationService.activateAfterFullPayment(invoice.getContractId());
            }
        }
    }
}

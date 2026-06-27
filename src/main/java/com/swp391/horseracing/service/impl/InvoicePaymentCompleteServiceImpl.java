package com.swp391.horseracing.service.impl;

import com.swp391.horseracing.entity.Invoice;
import com.swp391.horseracing.enums.InvoiceType;
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

    @Override
    public void handleAfterPaid(Invoice invoice){
        if (invoice.getInvoiceType() == InvoiceType.OWNER_TOURNAMENT_REGISTRATION_FEE) {
            if (invoice.getHorseTournamentRegistration() != null) {
                registrationPaymentService.markOwnerRegistrationPaid(
                        invoice.getHorseTournamentRegistration().getRegistrationId());
            } else if (invoice.getJockeyTournamentRegistration() != null) {
                registrationPaymentService.markJockeyRegistrationPaid(
                        invoice.getJockeyTournamentRegistration().getJockeyTournamentRegId());
            }
        }
    }

}

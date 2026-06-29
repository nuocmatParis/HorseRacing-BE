package com.swp391.horseracing.service;

import java.util.UUID;

public interface RegistrationPaymentService {
    void markOwnerRegistrationPaid(UUID tournamentRegId);

}

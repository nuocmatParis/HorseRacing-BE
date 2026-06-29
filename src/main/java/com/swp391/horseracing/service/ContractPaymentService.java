package com.swp391.horseracing.service;

import java.util.UUID;

public interface ContractPaymentService {
    void markContractFeePaid(UUID contractId);

    void markHiringFeePaid(UUID contractId);
}

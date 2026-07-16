package com.swp391.horseracing.service;

import java.util.UUID;

public interface ContractActivationService {
    void activateAfterFullPayment(UUID contractId);
}

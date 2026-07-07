package com.swp391.horseracing.service;

import com.swp391.horseracing.dto.contract.request.InviteRequest;
import com.swp391.horseracing.dto.contract.response.ContractResponse;
import com.swp391.horseracing.dto.invoice.response.PaymentResponse;

import java.util.List;
import java.util.UUID;

public interface ContractService {
    ContractResponse inviteJockey(InviteRequest request);

    List<ContractResponse> getMyInvitations();

    ContractResponse acceptContract(UUID contractId);

    ContractResponse rejectContractByJockey(UUID contractId, String reason);

    PaymentResponse payHiringFee(UUID contractId);

    PaymentResponse payContractCreationFee(UUID contractId);

    List<ContractResponse> getPendingContracts();

    ContractResponse approveContract(UUID contractId);

    ContractResponse rejectContractByAdmin(UUID contractId, String reason);
}

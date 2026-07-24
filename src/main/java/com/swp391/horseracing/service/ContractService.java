package com.swp391.horseracing.service;

import com.swp391.horseracing.dto.contract.request.InviteRequest;
import com.swp391.horseracing.dto.contract.response.ContractResponse;
import com.swp391.horseracing.dto.invoice.response.PaymentResponse;
import com.swp391.horseracing.dto.common.PageResponse;
import com.swp391.horseracing.enums.ContractStatus;

import java.util.List;
import java.util.UUID;

public interface ContractService {
    ContractResponse inviteJockey(InviteRequest request);

    List<ContractResponse> getOwnerContracts();

    ContractResponse getOwnerContractById(UUID contractId);

    List<ContractResponse> getMyInvitations();

    List<ContractResponse> getJockeyContracts();

    ContractResponse getJockeyContractById(UUID contractId);

    ContractResponse acceptContract(UUID contractId);

    ContractResponse rejectContractByJockey(UUID contractId, String reason);

    ContractResponse cancelByJockey(UUID contractId, String reason);

    PaymentResponse payHiringFee(UUID contractId);

    PaymentResponse payContractCreationFee(UUID contractId);

    ContractResponse releaseFinalPayoutAfterFinalRacePublished(UUID contractId, UUID finalRaceId);

    PageResponse<ContractResponse> getContractsByStatus(ContractStatus status, int page, int size);

    PageResponse<ContractResponse> getApprovedContractsByTournament(UUID tournamentId, int page, int size);

    ContractResponse cancelByOwner(UUID contractId, String reason);
}

package com.swp391.horseracing.service;

import com.swp391.horseracing.dto.contract.request.CreateContractRequest;
import com.swp391.horseracing.dto.contract.request.UpdateContractRequest;
import com.swp391.horseracing.dto.contract.response.ContractResponse;

import java.util.List;
import java.util.UUID;

public interface ContractService {

    ContractResponse create(CreateContractRequest request);

    ContractResponse updateStatus(UUID contractId, UpdateContractRequest request);

    ContractResponse getContractById(UUID contractId);

    List<ContractResponse> getContractsByTournament(UUID tournamentId);

    List<ContractResponse> getContractsByOwner(UUID ownerId);

    List<ContractResponse> getContractsByJockey(UUID jockeyId);

    List<ContractResponse> getApprovedContractsByTournament(UUID tournamentId);
}

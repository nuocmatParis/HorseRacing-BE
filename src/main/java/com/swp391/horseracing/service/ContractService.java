package com.swp391.horseracing.service;

import com.swp391.horseracing.dto.contract.request.CreateContractRequest;
import com.swp391.horseracing.dto.contract.request.InviteRequest;
import com.swp391.horseracing.dto.contract.request.UpdateContractRequest;
import com.swp391.horseracing.dto.contract.response.ContractResponse;

import java.util.List;
import java.util.UUID;

public interface ContractService {
    ContractResponse inviteJockey(InviteRequest request);
}

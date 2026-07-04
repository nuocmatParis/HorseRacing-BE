package com.swp391.horseracing.service.impl;

import com.swp391.horseracing.dto.contract.request.InviteRequest;
import com.swp391.horseracing.dto.contract.response.ContractResponse;
import com.swp391.horseracing.dto.invoice.response.PaymentResponse;
import com.swp391.horseracing.service.JockeyHorseContractService;
import com.swp391.horseracing.service.UserCurrentService;

import java.util.List;
import java.util.UUID;

public class JockeyHorseContractServiceImpl implements JockeyHorseContractService {
    UserCurrentService userCurrentService;



    @Override
    public ContractResponse inviteJockey(InviteRequest request) {
        return null;
    }

    @Override
    public List<ContractResponse> getAllInvitations() {
        return List.of();
    }

    @Override
    public ContractResponse acceptContract(InviteRequest request) {
        return null;
    }

    @Override
    public ContractResponse rejectContractByJockey(InviteRequest request, String reason) {
        return null;
    }

    @Override
    public PaymentResponse payHiringFee(UUID contractId) {
        return null;
    }

    @Override
    public PaymentResponse payContractCreationFee(UUID contractId) {
        return null;
    }

    @Override
    public List<ContractResponse> getPendingContracts() {
        return List.of();
    }

    @Override
    public ContractResponse approveContract(UUID contractId) {
        return null;
    }

    @Override
    public ContractResponse rejectContractByAdmin(UUID contractId, String reason) {
        return null;
    }
}

package com.swp391.horseracing.service.impl;

import com.swp391.horseracing.entity.JockeyHorseContract;
import com.swp391.horseracing.enums.ContractPaymentStatus;
import com.swp391.horseracing.enums.ContractStatus;
import com.swp391.horseracing.enums.EscrowStatus;
import com.swp391.horseracing.exception.AppException;
import com.swp391.horseracing.exception.ErrorCode;
import com.swp391.horseracing.repository.JockeyHorseContractRepository;
import com.swp391.horseracing.repository.JockeyTournamentRegistrationRepository;
import com.swp391.horseracing.service.ContractPaymentService;
import com.swp391.horseracing.service.InvoiceService;
import com.swp391.horseracing.service.UserCurrentService;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
public class ContractPaymentServiceImpl implements ContractPaymentService {
    InvoiceService invoiceService;
    JockeyHorseContractRepository jockeyHorseContractRepository;
    UserCurrentService userCurrentService;

    @Override
    @Transactional
    public void markContractFeePaid(UUID contractId) {
        JockeyHorseContract contract = jockeyHorseContractRepository.findById(contractId).orElseThrow(()
                -> new AppException(ErrorCode.CONTRACT_NOT_FOUND));

        if(contract.getStatus() != ContractStatus.HIRING_PAID)
            throw new AppException(ErrorCode.INVALID_CONTRACT_STATUS);

        contract.setStatus(ContractStatus.PENDING_ADMIN_REVIEW);
        contract.setSubmittedAt(LocalDateTime.now());
        jockeyHorseContractRepository.save(contract);
    }

    @Override
    @Transactional
    public void markHiringFeePaid(UUID contractId) {
        JockeyHorseContract contract = jockeyHorseContractRepository.findById(contractId).orElseThrow(
                () -> new AppException(ErrorCode.CONTRACT_NOT_FOUND));

        if(contract.getStatus() != ContractStatus.ACCEPTED)
            throw new AppException(ErrorCode.INVALID_CONTRACT_STATUS);

        contract.setPaymentStatus(ContractPaymentStatus.PAID);
        contract.setEscrowStatus(EscrowStatus.HELD);
        contract.setEscrowAmount(contract.getHireFee());
        contract.setStatus(ContractStatus.HIRING_PAID);

        jockeyHorseContractRepository.save(contract);

        invoiceService.createContractCreationInvoice(contract.getOwner().getUser().getUserId(),
                contractId,
                contract.getSystemContractFee());
    }
}

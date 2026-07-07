package com.swp391.horseracing.service.impl;

import com.swp391.horseracing.dto.contract.request.CreateContractRequest;
import com.swp391.horseracing.dto.contract.request.UpdateContractRequest;
import com.swp391.horseracing.dto.contract.response.ContractResponse;
import com.swp391.horseracing.entity.*;
import com.swp391.horseracing.enums.ContractStatus;
import com.swp391.horseracing.enums.RegistrationStatus;
import com.swp391.horseracing.exception.AppException;
import com.swp391.horseracing.exception.ErrorCode;
import com.swp391.horseracing.mapper.ContractMapper;
import com.swp391.horseracing.repository.*;
import com.swp391.horseracing.service.ContractService;
import com.swp391.horseracing.service.UserCurrentService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequiredArgsConstructor
public class ContractServiceImpl implements ContractService {

    JockeyHorseContractRepository contractRepository;
    TournamentRepository tournamentRepository;
    HorseTournamentRegistrationRepository tournamentRegistrationRepository;
    JockeyTournamentRegistrationRepository jockeyTournamentRegistrationRepository;
    HorseOwnerRepository ownerRepository;
    HorseRepository horseRepository;
    JockeyRepository jockeyRepository;
    UserRepository userRepository;
    ContractMapper contractMapper;
    UserCurrentService userCurrentService;


}

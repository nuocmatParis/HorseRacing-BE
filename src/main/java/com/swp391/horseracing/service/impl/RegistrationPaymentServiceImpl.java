package com.swp391.horseracing.service.impl;

import com.swp391.horseracing.entity.HorseTournamentRegistration;
import com.swp391.horseracing.entity.JockeyTournamentRegistration;
import com.swp391.horseracing.enums.RegistrationStatus;
import com.swp391.horseracing.exception.AppException;
import com.swp391.horseracing.exception.ErrorCode;
import com.swp391.horseracing.repository.HorseTournamentRegistrationRepository;
import com.swp391.horseracing.repository.JockeyTournamentRegistrationRepository;
import com.swp391.horseracing.service.RegistrationPaymentService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
@RequiredArgsConstructor
public class RegistrationPaymentServiceImpl implements RegistrationPaymentService {
    HorseTournamentRegistrationRepository horseTournamentRegistrationRepository;
    JockeyTournamentRegistrationRepository jockeyTournamentRegistrationRepository;

    @Override
    public void markOwnerRegistrationPaid(UUID tournamentRegId) {
        if(tournamentRegId == null)
            throw new AppException(ErrorCode.TOURNAMENT_REGISTRATION_NOT_FOUND);

        HorseTournamentRegistration horseTournamentRegistration = horseTournamentRegistrationRepository.findById(
                tournamentRegId).orElseThrow(() -> new AppException(ErrorCode.TOURNAMENT_REGISTRATION_NOT_FOUND));

        if(horseTournamentRegistration.getStatus() != RegistrationStatus.PENDING_PAYMENT)
            throw new AppException(ErrorCode.INVALID_REGISTRATION_STATUS);


        horseTournamentRegistration.setStatus(RegistrationStatus.PENDING_REVIEW);

        horseTournamentRegistrationRepository.save(horseTournamentRegistration);
    }

    @Override
    public void markJockeyRegistrationPaid(UUID jockeyTournamentRegId) {
        if (jockeyTournamentRegId == null) {
            throw new AppException(ErrorCode.JOCKEY_TOURNAMENT_REGISTRATION_NOT_FOUND);
        }

        JockeyTournamentRegistration registration = jockeyTournamentRegistrationRepository
                .findById(jockeyTournamentRegId)
                .orElseThrow(() -> new AppException(ErrorCode.JOCKEY_TOURNAMENT_REGISTRATION_NOT_FOUND));

        if (registration.getStatus() != RegistrationStatus.PENDING_PAYMENT) {
            throw new AppException(ErrorCode.INVALID_REGISTRATION_STATUS);
        }

        registration.setStatus(RegistrationStatus.PENDING_REVIEW);
        jockeyTournamentRegistrationRepository.save(registration);
    }
}

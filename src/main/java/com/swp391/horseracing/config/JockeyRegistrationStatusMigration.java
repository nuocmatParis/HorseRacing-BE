package com.swp391.horseracing.config;

import com.swp391.horseracing.enums.RegistrationStatus;
import com.swp391.horseracing.repository.JockeyTournamentRegistrationRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class JockeyRegistrationStatusMigration implements CommandLineRunner {

    JockeyTournamentRegistrationRepository jockeyTournamentRegistrationRepository;

    @Override
    public void run(String... args) {
        jockeyTournamentRegistrationRepository.findByStatus(RegistrationStatus.PENDING_PAYMENT)
                .forEach(registration -> {
                    registration.setStatus(RegistrationStatus.PENDING_REVIEW);
                    jockeyTournamentRegistrationRepository.save(registration);
                });
    }
}

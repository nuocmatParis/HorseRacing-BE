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
        java.util.List<RegistrationStatus> statusesToMigrate = java.util.List.of(
                RegistrationStatus.PENDING_PAYMENT,
                RegistrationStatus.PENDING_REVIEW
        );
        for (RegistrationStatus status : statusesToMigrate) {
            jockeyTournamentRegistrationRepository.findByStatus(status)
                    .forEach(registration -> {
                        registration.setStatus(RegistrationStatus.APPROVED);
                        jockeyTournamentRegistrationRepository.save(registration);
                    });
        }
    }
}

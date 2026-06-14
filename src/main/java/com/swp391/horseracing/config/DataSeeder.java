package com.swp391.horseracing.config;

import com.swp391.horseracing.entity.Role;
import com.swp391.horseracing.entity.User;
import com.swp391.horseracing.enums.AccountStatus;
import com.swp391.horseracing.enums.Gender;
import com.swp391.horseracing.enums.RoleName;
import com.swp391.horseracing.repository.RoleRepository;
import com.swp391.horseracing.repository.UserRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

@Component
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class DataSeeder implements CommandLineRunner {

    RoleRepository roleRepository;
    UserRepository userRepository;
    PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        seedRoles();
        seedUsers();
    }

    void seedRoles() {
        if (roleRepository.count() > 0) return;

        for (RoleName name : RoleName.values()) {
            roleRepository.save(Role.builder()
                    .roleName(name)
                    .description(toDescription(name))
                    .build());
        }
    }

    void seedUsers() {
        if (userRepository.existsByUsername("admin")) return;

        Role adminRole = roleRepository.findByRoleName(RoleName.ADMIN).orElseThrow();
        Role ownerRole = roleRepository.findByRoleName(RoleName.HORSE_OWNER).orElseThrow();
        Role jockeyRole = roleRepository.findByRoleName(RoleName.JOCKEY).orElseThrow();
        Role spectatorRole = roleRepository.findByRoleName(RoleName.SPECTATOR).orElseThrow();
        Role refereeRole = roleRepository.findByRoleName(RoleName.REFEREE).orElseThrow();
        Role vetRole = roleRepository.findByRoleName(RoleName.VETERINARIAN).orElseThrow();
        Role medStaffRole = roleRepository.findByRoleName(RoleName.MEDICAL_STAFF).orElseThrow();

        String encodedPassword = passwordEncoder.encode("12345678");

        List<User> users = List.of(
                User.builder()
                        .username("admin")
                        .password(encodedPassword)
                        .email("admin@horseracing.com")
                        .fullName("System Admin")
                        .gender(Gender.MALE)
                        .dob(LocalDate.of(1990, 1, 1))
                        .phoneNumber("0900000001")
                        .status(AccountStatus.ACTIVE)
                        .role(adminRole)
                        .build(),
                User.builder()
                        .username("owner1")
                        .password(encodedPassword)
                        .email("owner1@horseracing.com")
                        .fullName("Nguyen Van A")
                        .gender(Gender.MALE)
                        .dob(LocalDate.of(1990, 1, 1))
                        .phoneNumber("0900000002")
                        .status(AccountStatus.ACTIVE)
                        .role(ownerRole)
                        .build(),
                User.builder()
                        .username("jockey1")
                        .password(encodedPassword)
                        .email("jockey1@horseracing.com")
                        .fullName("Tran Van B")
                        .gender(Gender.MALE)
                        .dob(LocalDate.of(1990, 1, 1))
                        .phoneNumber("0900000003")
                        .status(AccountStatus.ACTIVE)
                        .role(jockeyRole)
                        .build(),
                User.builder()
                        .username("spectator1")
                        .password(encodedPassword)
                        .email("spectator1@horseracing.com")
                        .fullName("Le Van C")
                        .gender(Gender.MALE)
                        .dob(LocalDate.of(1990, 1, 1))
                        .phoneNumber("0900000004")
                        .status(AccountStatus.ACTIVE)
                        .role(spectatorRole)
                        .build(),
                User.builder()
                        .username("referee1")
                        .password(encodedPassword)
                        .email("referee1@horseracing.com")
                        .fullName("Pham Van D")
                        .gender(Gender.MALE)
                        .dob(LocalDate.of(1990, 1, 1))
                        .phoneNumber("0900000005")
                        .status(AccountStatus.ACTIVE)
                        .role(refereeRole)
                        .build(),
                User.builder()
                        .username("vet1")
                        .password(encodedPassword)
                        .email("vet1@horseracing.com")
                        .fullName("Hoang Van E")
                        .gender(Gender.MALE)
                        .dob(LocalDate.of(1990, 1, 1))
                        .phoneNumber("0900000006")
                        .status(AccountStatus.ACTIVE)
                        .role(vetRole)
                        .build(),
                User.builder()
                        .username("medstaff1")
                        .password(encodedPassword)
                        .email("medstaff1@horseracing.com")
                        .fullName("Vo Van F")
                        .gender(Gender.MALE)
                        .dob(LocalDate.of(1990, 1, 1))
                        .phoneNumber("0900000007")
                        .status(AccountStatus.ACTIVE)
                        .role(medStaffRole)
                        .build()
        );

        userRepository.saveAll(users);
    }

    String toDescription(RoleName name) {
        return switch (name) {
            case ADMIN -> "System administrator";
            case HORSE_OWNER -> "Horse owner";
            case JOCKEY -> "Jockey";
            case SPECTATOR -> "Spectator";
            case REFEREE -> "Referee";
            case VETERINARIAN -> "Veterinarian";
            case MEDICAL_STAFF -> "Medical staff";
        };
    }
}

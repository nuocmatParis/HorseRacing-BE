package com.swp391.horseracing.config;

import com.swp391.horseracing.entity.Horse;
import com.swp391.horseracing.entity.HorseOwner;
import com.swp391.horseracing.entity.Jockey;
import com.swp391.horseracing.entity.Role;
import com.swp391.horseracing.entity.User;
import com.swp391.horseracing.enums.AccountStatus;
import com.swp391.horseracing.enums.Gender;
import com.swp391.horseracing.enums.HealthStatus;
import com.swp391.horseracing.enums.HorseBreed;
import com.swp391.horseracing.enums.JockeyStatus;
import com.swp391.horseracing.enums.RoleName;
import com.swp391.horseracing.repository.HorseOwnerRepository;
import com.swp391.horseracing.repository.HorseRepository;
import com.swp391.horseracing.repository.JockeyRepository;
import com.swp391.horseracing.repository.RoleRepository;
import com.swp391.horseracing.repository.UserRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Component
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class DataSeeder implements CommandLineRunner {

    RoleRepository roleRepository;
    UserRepository userRepository;
    HorseOwnerRepository horseOwnerRepository;
    JockeyRepository jockeyRepository;
    HorseRepository horseRepository;
    PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        seedRoles();
        seedUsers();
        seedHorseOwners();
        seedJockeys();
        seedHorses();
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

    void seedHorseOwners() {
        if (horseOwnerRepository.count() > 0) return;

        User user1 = userRepository.findByUsername("owner1").orElseThrow();

        List<HorseOwner> owners = List.of(
                HorseOwner.builder()
                        .user(user1)
                        .farmName("Thunder Stables")
                        .address("123 Horse Street, District 1")
                        .licenseNumber("LIC-OWN-001")
                        .build()
        );

        horseOwnerRepository.saveAll(owners);
    }

    void seedJockeys() {
        if (jockeyRepository.count() > 0) return;

        User user1 = userRepository.findByUsername("jockey1").orElseThrow();

        List<Jockey> jockeys = List.of(
                Jockey.builder()
                        .user(user1)
                        .height(new BigDecimal("165"))
                        .weight(new BigDecimal("52"))
                        .experienceYears(5)
                        .licenseNumber("LIC-JOC-001")
                        .specialization("Flat Racing")
                        .hireFee(new BigDecimal("1000000"))
                        .status(JockeyStatus.AVAILABLE)
                        .build()
        );

        jockeyRepository.saveAll(jockeys);
    }

    void seedHorses() {
        if (horseRepository.count() > 0) return;

        HorseOwner owner1 = horseOwnerRepository.findByUser_Username("owner1").orElseThrow();

        List<Horse> horses = List.of(
                Horse.builder()
                        .owner(owner1)
                        .name("Lightning Bolt")
                        .breed(HorseBreed.THOROUGHBRED)
                        .gender(Gender.MALE)
                        .age(4)
                        .weight(480)
                        .color("Bay")
                        .healthStatus(HealthStatus.HEALTHY)
                        .raceClass("Class A")
                        .totalRaces(10)
                        .totalWins(5)
                        .winRate(50.0)
                        .build(),
                Horse.builder()
                        .owner(owner1)
                        .name("Midnight Star")
                        .breed(HorseBreed.ARABIAN)
                        .gender(Gender.FEMALE)
                        .age(3)
                        .weight(450)
                        .color("Black")
                        .healthStatus(HealthStatus.HEALTHY)
                        .raceClass("Class B")
                        .totalRaces(6)
                        .totalWins(2)
                        .winRate(33.33)
                        .build()
        );

        horseRepository.saveAll(horses);
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

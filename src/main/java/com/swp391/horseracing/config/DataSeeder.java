//package com.swp391.horseracing.config;
//
//import com.swp391.horseracing.entity.Horse;
//import com.swp391.horseracing.entity.HorseOwner;
//import com.swp391.horseracing.entity.HorseTournamentRegistration;
//import com.swp391.horseracing.entity.Jockey;
//import com.swp391.horseracing.entity.JockeyTournamentRegistration;
//import com.swp391.horseracing.entity.PrizeStructure;
//import com.swp391.horseracing.entity.Race;
//import com.swp391.horseracing.entity.Role;
//import com.swp391.horseracing.entity.Round;
//import com.swp391.horseracing.entity.Tournament;
//import com.swp391.horseracing.entity.User;
//import com.swp391.horseracing.enums.AccountStatus;
//import com.swp391.horseracing.enums.Gender;
//import com.swp391.horseracing.enums.HealthStatus;
//import com.swp391.horseracing.enums.HorseBreed;
//import com.swp391.horseracing.enums.JockeyStatus;
//import com.swp391.horseracing.enums.PredictionType;
//import com.swp391.horseracing.enums.RegistrationStatus;
//import com.swp391.horseracing.enums.RoleName;
//import com.swp391.horseracing.enums.RoundStatus;
//import com.swp391.horseracing.enums.TournamentPhase;
//import com.swp391.horseracing.enums.TournamentStatus;
//import com.swp391.horseracing.repository.HorseOwnerRepository;
//import com.swp391.horseracing.repository.HorseRepository;
//import com.swp391.horseracing.repository.HorseTournamentRegistrationRepository;
//import com.swp391.horseracing.repository.JockeyRepository;
//import com.swp391.horseracing.repository.JockeyTournamentRegistrationRepository;
//import com.swp391.horseracing.repository.PrizeStructureRepository;
//import com.swp391.horseracing.repository.RaceRepository;
//import com.swp391.horseracing.repository.RoleRepository;
//import com.swp391.horseracing.repository.RoundRepository;
//import com.swp391.horseracing.repository.TournamentRepository;
//import com.swp391.horseracing.repository.UserRepository;
//import lombok.AccessLevel;
//import lombok.RequiredArgsConstructor;
//import lombok.experimental.FieldDefaults;
//import org.springframework.boot.CommandLineRunner;
//import org.springframework.security.crypto.password.PasswordEncoder;
//import org.springframework.stereotype.Component;
//
//import java.math.BigDecimal;
//import java.time.LocalDate;
//import java.time.LocalDateTime;
//import java.util.List;
//
//@Component
//@RequiredArgsConstructor
//@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
//public class DataSeeder implements CommandLineRunner {
//
//    RoleRepository roleRepository;
//    UserRepository userRepository;
//    HorseOwnerRepository horseOwnerRepository;
//    JockeyRepository jockeyRepository;
//    HorseRepository horseRepository;
//    TournamentRepository tournamentRepository;
//    PrizeStructureRepository prizeStructureRepository;
//    RoundRepository roundRepository;
//    RaceRepository raceRepository;
//    HorseTournamentRegistrationRepository horseTournamentRegistrationRepository;
//    JockeyTournamentRegistrationRepository jockeyTournamentRegistrationRepository;
//    PasswordEncoder passwordEncoder;
//
//    @Override
//    public void run(String... args) {
//        seedRoles();
//        seedUsers();
//        seedHorseOwners();
//        seedJockeys();
//        seedHorses();
//        seedTournaments();
//        seedPrizeStructures();
//        seedRounds();
//        seedRaces();
//        seedHorseRegistrations();
//        seedJockeyRegistrations();
//    }
//
//    void seedRoles() {
//        if (roleRepository.count() > 0) return;
//
//        for (RoleName name : RoleName.values()) {
//            roleRepository.save(Role.builder()
//                    .roleName(name)
//                    .description(toDescription(name))
//                    .build());
//        }
//    }
//
//    void seedUsers() {
//        if (userRepository.existsByUsername("admin")) return;
//
//        Role adminRole = roleRepository.findByRoleName(RoleName.ADMIN).orElseThrow();
//        Role ownerRole = roleRepository.findByRoleName(RoleName.HORSE_OWNER).orElseThrow();
//        Role jockeyRole = roleRepository.findByRoleName(RoleName.JOCKEY).orElseThrow();
//        Role spectatorRole = roleRepository.findByRoleName(RoleName.SPECTATOR).orElseThrow();
//        Role refereeRole = roleRepository.findByRoleName(RoleName.REFEREE).orElseThrow();
//        Role vetRole = roleRepository.findByRoleName(RoleName.VETERINARIAN).orElseThrow();
//        Role medStaffRole = roleRepository.findByRoleName(RoleName.MEDICAL_STAFF).orElseThrow();
//
//        String encodedPassword = passwordEncoder.encode("12345678");
//
//        List<User> users = List.of(
//                User.builder()
//                        .username("admin")
//                        .password(encodedPassword)
//                        .email("admin@horseracing.com")
//                        .fullName("System Admin")
//                        .gender(Gender.MALE)
//                        .dob(LocalDate.of(1990, 1, 1))
//                        .phoneNumber("0900000001")
//                        .status(AccountStatus.ACTIVE)
//                        .role(adminRole)
//                        .build(),
//                User.builder()
//                        .username("owner1")
//                        .password(encodedPassword)
//                        .email("owner1@horseracing.com")
//                        .fullName("Nguyen Van A")
//                        .gender(Gender.MALE)
//                        .dob(LocalDate.of(1990, 1, 1))
//                        .phoneNumber("0900000002")
//                        .status(AccountStatus.ACTIVE)
//                        .role(ownerRole)
//                        .build(),
//                User.builder()
//                        .username("jockey1")
//                        .password(encodedPassword)
//                        .email("jockey1@horseracing.com")
//                        .fullName("Tran Van B")
//                        .gender(Gender.MALE)
//                        .dob(LocalDate.of(1990, 1, 1))
//                        .phoneNumber("0900000003")
//                        .status(AccountStatus.ACTIVE)
//                        .role(jockeyRole)
//                        .build(),
//                User.builder()
//                        .username("spectator1")
//                        .password(encodedPassword)
//                        .email("spectator1@horseracing.com")
//                        .fullName("Le Van C")
//                        .gender(Gender.MALE)
//                        .dob(LocalDate.of(1990, 1, 1))
//                        .phoneNumber("0900000004")
//                        .status(AccountStatus.ACTIVE)
//                        .role(spectatorRole)
//                        .build(),
//                User.builder()
//                        .username("referee1")
//                        .password(encodedPassword)
//                        .email("referee1@horseracing.com")
//                        .fullName("Pham Van D")
//                        .gender(Gender.MALE)
//                        .dob(LocalDate.of(1990, 1, 1))
//                        .phoneNumber("0900000005")
//                        .status(AccountStatus.ACTIVE)
//                        .role(refereeRole)
//                        .build(),
//                User.builder()
//                        .username("vet1")
//                        .password(encodedPassword)
//                        .email("vet1@horseracing.com")
//                        .fullName("Hoang Van E")
//                        .gender(Gender.MALE)
//                        .dob(LocalDate.of(1990, 1, 1))
//                        .phoneNumber("0900000006")
//                        .status(AccountStatus.ACTIVE)
//                        .role(vetRole)
//                        .build(),
//                User.builder()
//                        .username("medstaff1")
//                        .password(encodedPassword)
//                        .email("medstaff1@horseracing.com")
//                        .fullName("Vo Van F")
//                        .gender(Gender.MALE)
//                        .dob(LocalDate.of(1990, 1, 1))
//                        .phoneNumber("0900000007")
//                        .status(AccountStatus.ACTIVE)
//                        .role(medStaffRole)
//                        .build()
//        );
//
//        userRepository.saveAll(users);
//    }
//
//    void seedHorseOwners() {
//        if (horseOwnerRepository.count() > 0) return;
//
//        User user1 = userRepository.findByUsername("owner1").orElseThrow();
//
//        List<HorseOwner> owners = List.of(
//                HorseOwner.builder()
//                        .user(user1)
//                        .farmName("Thunder Stables")
//                        .address("123 Horse Street, District 1")
//                        .licenseNumber("LIC-OWN-001")
//                        .build()
//        );
//
//        horseOwnerRepository.saveAll(owners);
//    }
//
//    void seedJockeys() {
//        if (jockeyRepository.count() > 0) return;
//
//        User user1 = userRepository.findByUsername("jockey1").orElseThrow();
//
//        List<Jockey> jockeys = List.of(
//                Jockey.builder()
//                        .user(user1)
//                        .height(165f)
//                        .weight(52f)
//                        .experienceYears(5)
//                        .licenseNumber("LIC-JOC-001")
//                        .specialization("Flat Racing")
//                        .hireFee(new BigDecimal("1000000"))
//                        .status(JockeyStatus.AVAILABLE)
//                        .build()
//        );
//
//        jockeyRepository.saveAll(jockeys);
//    }
//
//    void seedHorses() {
//        if (horseRepository.count() > 0) return;
//
//        HorseOwner owner1 = horseOwnerRepository.findByUser_Username("owner1").orElseThrow();
//
//        List<Horse> horses = List.of(
//                Horse.builder()
//                        .owner(owner1)
//                        .name("Lightning Bolt")
//                        .breed(HorseBreed.THOROUGHBRED)
//                        .gender(Gender.MALE)
//                        .age(4)
//                        .weight(480)
//                        .color("Bay")
//                        .healthStatus(HealthStatus.HEALTHY)
//                        .raceClass("Class A")
//                        .totalRaces(10)
//                        .totalWins(5)
//                        .winRate(50.0)
//                        .build(),
//                Horse.builder()
//                        .owner(owner1)
//                        .name("Midnight Star")
//                        .breed(HorseBreed.ARABIAN)
//                        .gender(Gender.FEMALE)
//                        .age(3)
//                        .weight(450)
//                        .color("Black")
//                        .healthStatus(HealthStatus.HEALTHY)
//                        .raceClass("Class B")
//                        .totalRaces(6)
//                        .totalWins(2)
//                        .winRate(33.33)
//                        .build()
//        );
//
//        horseRepository.saveAll(horses);
//    }
//
//    void seedTournaments() {
//        if (tournamentRepository.count() > 0) return;
//
//        User admin = userRepository.findByUsername("admin").orElseThrow();
//
//        Tournament tournament = Tournament.builder()
//                .name("Summer Championship 2026")
//                .description("Annual summer horse racing championship")
//                .startDate(LocalDate.of(2026, 7, 1))
//                .endDate(LocalDate.of(2026, 7, 15))
//                .location("Hanoi Horse Racing Center")
//                .registrationFee(new BigDecimal("500000.00"))
//                .systemContractFee(new BigDecimal("100000.00"))
//                .totalPrizePool(new BigDecimal("50000000.00"))
//                .allowedBreed("Thoroughbred, Arabian")
//                .raceClass("Class A")
//                .weightClass("Open")
//                .minHorseAge(3)
//                .maxHorseAge(10)
//                .tournamentDivision("Professional")
//                .handicapRule("Standard handicap based on past performance")
//                .predictionRewardRule("Top 3 predictors share 10% of total prize pool")
//                .predictionOpenMinutesBefore(120)
//                .predictionCloseMinutesBefore(5)
//                .status(TournamentStatus.DRAFT)
//                .phase(TournamentPhase.REGISTRATION)
//                .createdBy(admin)
//                .createdAt(LocalDateTime.now())
//                .build();
//
//        tournamentRepository.save(tournament);
//    }
//
//    void seedPrizeStructures() {
//        if (prizeStructureRepository.count() > 0) return;
//
//        Tournament tournament = tournamentRepository.findAllByOrderByCreatedAtDesc().get(0);
//
//        List<PrizeStructure> prizes = List.of(
//                PrizeStructure.builder()
//                        .tournament(tournament)
//                        .rank(1)
//                        .percentage(50.0f)
//                        .isActive(true)
//                        .build(),
//                PrizeStructure.builder()
//                        .tournament(tournament)
//                        .rank(2)
//                        .percentage(30.0f)
//                        .isActive(true)
//                        .build(),
//                PrizeStructure.builder()
//                        .tournament(tournament)
//                        .rank(3)
//                        .percentage(20.0f)
//                        .isActive(true)
//                        .build()
//        );
//
//        prizeStructureRepository.saveAll(prizes);
//    }
//
//    void seedRounds() {
//        if (roundRepository.count() > 0) return;
//
//        Tournament tournament = tournamentRepository.findAllByOrderByCreatedAtDesc().get(0);
//        User admin = userRepository.findByUsername("admin").orElseThrow();
//
//        List<Round> rounds = List.of(
//                Round.builder()
//                        .tournament(tournament)
//                        .roundName("Qualification Round")
//                        .sequenceOrder(1)
//                        .isFinal(false)
//                        .predictionType(PredictionType.TOP1)
//                        .advancementRule("Top 50% advance to final")
//                        .startDate(LocalDateTime.of(2026, 7, 3, 8, 0))
//                        .endDate(LocalDateTime.of(2026, 7, 8, 18, 0))
//                        .description("Qualification round to select finalists")
//                        .maxRaces(2)
//                        .status(RoundStatus.SCHEDULED)
//                        .createdBy(admin)
//                        .createdAt(LocalDateTime.now())
//                        .build(),
//                Round.builder()
//                        .tournament(tournament)
//                        .roundName("Final Round")
//                        .sequenceOrder(2)
//                        .isFinal(true)
//                        .predictionType(PredictionType.TOP3)
//                        .advancementRule("Winner takes all")
//                        .startDate(LocalDateTime.of(2026, 7, 10, 8, 0))
//                        .endDate(LocalDateTime.of(2026, 7, 14, 18, 0))
//                        .description("Final round to determine champion")
//                        .maxRaces(1)
//                        .status(RoundStatus.SCHEDULED)
//                        .createdBy(admin)
//                        .createdAt(LocalDateTime.now())
//                        .build()
//        );
//
//        roundRepository.saveAll(rounds);
//    }
//
//    void seedRaces() {
//        if (raceRepository.count() > 0) return;
//
//        User admin = userRepository.findByUsername("admin").orElseThrow();
//
//        Round qualRound = roundRepository.findAll().stream()
//                .filter(r -> r.getSequenceOrder() == 1)
//                .findFirst().orElseThrow();
//        Round finalRound = roundRepository.findAll().stream()
//                .filter(r -> r.getSequenceOrder() == 2)
//                .findFirst().orElseThrow();
//
//        List<Race> races = List.of(
//                Race.builder()
//                        .round(qualRound)
//                        .name("Qualification Heat 1")
//                        .startTime(LocalDateTime.of(2026, 7, 4, 9, 0))
//                        .endTime(LocalDateTime.of(2026, 7, 4, 10, 0))
//                        .trackCondition("Firm")
//                        .distance(1600.0f)
//                        .maxEntries(8)
//                        .status(RoundStatus.SCHEDULED)
//                        .schedulePublishedAt(LocalDateTime.now())
//                        .predictionOpenAt(LocalDateTime.of(2026, 7, 1, 0, 0))
//                        .predictionCloseAt(LocalDateTime.of(2026, 7, 3, 23, 59))
//                        .createdBy(admin)
//                        .build(),
//                Race.builder()
//                        .round(qualRound)
//                        .name("Qualification Heat 2")
//                        .startTime(LocalDateTime.of(2026, 7, 5, 9, 0))
//                        .endTime(LocalDateTime.of(2026, 7, 5, 10, 0))
//                        .trackCondition("Firm")
//                        .distance(1600.0f)
//                        .maxEntries(8)
//                        .status(RoundStatus.SCHEDULED)
//                        .schedulePublishedAt(LocalDateTime.now())
//                        .predictionOpenAt(LocalDateTime.of(2026, 7, 1, 0, 0))
//                        .predictionCloseAt(LocalDateTime.of(2026, 7, 4, 23, 59))
//                        .createdBy(admin)
//                        .build(),
//                Race.builder()
//                        .round(finalRound)
//                        .name("Final Race")
//                        .startTime(LocalDateTime.of(2026, 7, 12, 9, 0))
//                        .endTime(LocalDateTime.of(2026, 7, 12, 10, 0))
//                        .trackCondition("Firm")
//                        .distance(2000.0f)
//                        .maxEntries(8)
//                        .status(RoundStatus.SCHEDULED)
//                        .schedulePublishedAt(LocalDateTime.now())
//                        .predictionOpenAt(LocalDateTime.of(2026, 7, 1, 0, 0))
//                        .predictionCloseAt(LocalDateTime.of(2026, 7, 11, 23, 59))
//                        .createdBy(admin)
//                        .build()
//        );
//
//        raceRepository.saveAll(races);
//    }
//
//    void seedHorseRegistrations() {
//        if (horseTournamentRegistrationRepository.count() > 0) return;
//
//        Tournament tournament = tournamentRepository.findAllByOrderByCreatedAtDesc().get(0);
//        HorseOwner owner = horseOwnerRepository.findByUser_Username("owner1").orElseThrow();
//        List<Horse> horses = horseRepository.findByOwner_OwnerId(owner.getOwnerId());
//        Horse horse1 = horses.get(0);
//        Horse horse2 = horses.get(1);
//
//        List<HorseTournamentRegistration> registrations = List.of(
//                HorseTournamentRegistration.builder()
//                        .tournament(tournament)
//                        .horse(horse1)
//                        .owner(owner)
//                        .status(RegistrationStatus.APPROVED)
//                        .build(),
//                HorseTournamentRegistration.builder()
//                        .tournament(tournament)
//                        .horse(horse2)
//                        .owner(owner)
//                        .status(RegistrationStatus.APPROVED)
//                        .build()
//        );
//
//        horseTournamentRegistrationRepository.saveAll(registrations);
//    }
//
//    void seedJockeyRegistrations() {
//        if (jockeyTournamentRegistrationRepository.count() > 0) return;
//
//        Tournament tournament = tournamentRepository.findAllByOrderByCreatedAtDesc().get(0);
//        Jockey jockey = jockeyRepository.findByUser_Username("jockey1").orElseThrow();
//
//        List<JockeyTournamentRegistration> registrations = List.of(
//                JockeyTournamentRegistration.builder()
//                        .tournament(tournament)
//                        .jockey(jockey)
//                        .status(RegistrationStatus.APPROVED)
//                        .build()
//        );
//
//        jockeyTournamentRegistrationRepository.saveAll(registrations);
//    }
//
//    String toDescription(RoleName name) {
//        return switch (name) {
//            case ADMIN -> "System administrator";
//            case HORSE_OWNER -> "Horse owner";
//            case JOCKEY -> "Jockey";
//            case SPECTATOR -> "Spectator";
//            case REFEREE -> "Referee";
//            case VETERINARIAN -> "Veterinarian";
//            case MEDICAL_STAFF -> "Medical staff";
//        };
//    }
//}

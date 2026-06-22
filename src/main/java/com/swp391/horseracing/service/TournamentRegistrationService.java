package com.swp391.horseracing.service;

import com.swp391.horseracing.dto.registration.response.HorseTournamentRegistrationResponse;
import com.swp391.horseracing.dto.registration.response.JockeyTournamentRegistrationResponse;

import java.util.UUID;

public interface TournamentRegistrationService {

    HorseTournamentRegistrationResponse registerHorse(UUID tournamentId, UUID horseId);

    JockeyTournamentRegistrationResponse registerJockey(UUID tournamentId);

    HorseTournamentRegistrationResponse approveHorseRegistration(UUID registrationId);

    HorseTournamentRegistrationResponse rejectHorseRegistration(UUID registrationId, String reason);

    JockeyTournamentRegistrationResponse approveJockeyRegistration(UUID registrationId);

    JockeyTournamentRegistrationResponse rejectJockeyRegistration(UUID registrationId, String reason);
}

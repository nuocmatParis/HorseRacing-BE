package com.swp391.horseracing.service;

import com.swp391.horseracing.dto.registration.request.RegisterJockeyRequest;
import com.swp391.horseracing.dto.registration.response.HorseTournamentRegistrationResponse;
import com.swp391.horseracing.dto.registration.response.JockeyTournamentRegistrationResponse;

import java.util.List;
import java.util.UUID;

public interface TournamentRegistrationService {

    HorseTournamentRegistrationResponse registerHorse(UUID tournamentId, UUID horseId);

    JockeyTournamentRegistrationResponse registerJockey(UUID tournamentId, RegisterJockeyRequest request);

    List<JockeyTournamentRegistrationResponse> getMyJockeyRegistrations();

    List<HorseTournamentRegistrationResponse> getMyHorseRegistrations();

    List<HorseTournamentRegistrationResponse> getAllHorseRegistrations();

    List<JockeyTournamentRegistrationResponse> getAllJockeyRegistrations();

    HorseTournamentRegistrationResponse approveHorseRegistration(UUID registrationId);

    HorseTournamentRegistrationResponse rejectHorseRegistration(UUID registrationId, String reason);

    JockeyTournamentRegistrationResponse approveJockeyRegistration(UUID registrationId);

    JockeyTournamentRegistrationResponse rejectJockeyRegistration(UUID registrationId, String reason);

    List<JockeyTournamentRegistrationResponse> getApprovedJockeysByTournament(UUID tournamentId);

    List<HorseTournamentRegistrationResponse> getApprovedHorsesByTournament(UUID tournamentId);
}

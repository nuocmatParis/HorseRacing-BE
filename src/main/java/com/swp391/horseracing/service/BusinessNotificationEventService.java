package com.swp391.horseracing.service;

import com.swp391.horseracing.entity.*;

import java.time.LocalDateTime;

public interface BusinessNotificationEventService {
    void tournamentPublished(Tournament tournament);
    void schedulePublished(Tournament tournament);
    void horseRegistrationApproved(HorseTournamentRegistration registration);
    void horseRegistrationRejected(HorseTournamentRegistration registration, String reason);
    void jockeyRegistrationApproved(JockeyTournamentRegistration registration);
    void jockeyRegistrationRejected(JockeyTournamentRegistration registration, String reason);
    void horseRegistrationWithdrawn(HorseTournamentRegistration registration, String reason);
    void contractInvited(JockeyHorseContract contract);
    void contractAccepted(JockeyHorseContract contract);
    void contractRejected(JockeyHorseContract contract, String reason);
    void contractApproved(JockeyHorseContract contract);
    void contractCancelled(JockeyHorseContract contract, String reason);
    void raceRescheduled(Race race, LocalDateTime oldStartTime, String reason);
    void raceCancelled(Race race, String reason);
    void raceStarted(Race race);
    void entryScratched(RaceEntry entry);
    void horseInspectionFailed(RaceEntry entry);
    void jockeyInspectionFailed(RaceEntry entry);
    void predictedEntryScratched(RaceEntry entry);
    void resultPublished(Race race);
    void predictionScored(Prediction prediction);
    void predictionVoided(Prediction prediction, String reason);
    void prizeReceived(RaceResult result);
    void jockeyPayoutReleased(JockeyHorseContract contract);
    void roundTransitionBlocked(Round round);
    void appealSubmitted(Appeal appeal);
    void appealReviewed(Appeal appeal);
}

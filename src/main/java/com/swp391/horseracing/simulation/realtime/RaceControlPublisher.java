package com.swp391.horseracing.simulation.realtime;

import com.swp391.horseracing.entity.Race;
import com.swp391.horseracing.entity.RaceReferee;
import com.swp391.horseracing.repository.RaceRefereeRepository;
import com.swp391.horseracing.simulation.api.LiveRaceMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class RaceControlPublisher {
    private final SimpMessagingTemplate messagingTemplate;
    private final RaceRefereeRepository raceRefereeRepository;

    public void publishPublic(UUID raceId, LiveRaceMessage message) {
        messagingTemplate.convertAndSend("/topic/races/" + raceId + "/live", message);
    }

    public void publishPrivate(Race race, LiveRaceMessage message) {
        Set<UUID> userIds = new LinkedHashSet<>();
        if (race.getRound().getHeadReferee() != null) {
            userIds.add(race.getRound().getHeadReferee().getUser().getUserId());
        }
        for (RaceReferee assignment : raceRefereeRepository.findByRace_RaceId(race.getRaceId())) {
            userIds.add(assignment.getReferee().getUser().getUserId());
        }
        for (UUID userId : userIds) {
            messagingTemplate.convertAndSendToUser(
                    userId.toString(),
                    "/queue/races/" + race.getRaceId() + "/control",
                    message);
        }
    }
}

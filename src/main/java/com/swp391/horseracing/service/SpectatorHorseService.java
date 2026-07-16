package com.swp391.horseracing.service;

import com.swp391.horseracing.dto.common.PageResponse;
import com.swp391.horseracing.dto.spectator.response.SpectatorHorseResponse;

import java.util.UUID;

public interface SpectatorHorseService {
    PageResponse<SpectatorHorseResponse> searchHorses(
            String query, String raceClass, String healthStatus, int page, int size);

    PageResponse<SpectatorHorseResponse> getFollowingHorses(int page, int size);

    SpectatorHorseResponse followHorse(UUID horseId);

    void unfollowHorse(UUID horseId);

    SpectatorHorseResponse getHorseDetail(UUID horseId);
}

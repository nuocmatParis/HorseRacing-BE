package com.swp391.horseracing.service;

import com.swp391.horseracing.dto.jockey.response.JockeyAssignedHorseResponse;

import java.util.List;

public interface JockeyAssignedHorseService {
    List<JockeyAssignedHorseResponse> getMyAssignedHorses();
}

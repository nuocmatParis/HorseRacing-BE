package com.swp391.horseracing.service;

import com.swp391.horseracing.dto.horse.request.HorseCreationRequest;
import com.swp391.horseracing.dto.horse.request.HorseUpdateRequest;
import com.swp391.horseracing.dto.horse.response.HorseResponse;

import java.util.List;
import java.util.UUID;

public interface HorseService {
    HorseResponse create(HorseCreationRequest request);
    List<HorseResponse> getAll();
    HorseResponse getById(UUID horseId);
    HorseResponse update(UUID horseId, HorseUpdateRequest request);
    void delete(UUID horseId);
}

package com.swp391.horseracing.service;

import com.swp391.horseracing.dto.violation.request.ViolationCreateRequest;
import com.swp391.horseracing.dto.violation.response.ViolationResponse;

import java.util.List;
import java.util.UUID;

public interface ViolationService {

    ViolationResponse createViolation(UUID entryId, ViolationCreateRequest request);

    List<ViolationResponse> getViolationsByRaceId(UUID raceId);
}

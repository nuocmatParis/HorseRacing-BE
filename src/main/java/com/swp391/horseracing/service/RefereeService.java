package com.swp391.horseracing.service;

import com.swp391.horseracing.dto.referee.request.RefereeCreationRequest;
import com.swp391.horseracing.dto.referee.request.RefereeUpdateRequest;
import com.swp391.horseracing.dto.referee.response.RefereeResponse;
import com.swp391.horseracing.enums.RefereeStatus;

import java.util.List;
import java.util.UUID;

public interface RefereeService {

    RefereeResponse create(RefereeCreationRequest request);

    List<RefereeResponse> getAll();

    List<RefereeResponse> getAllReferees(RefereeStatus status);

    RefereeResponse getById(UUID id);

    RefereeResponse update(UUID id, RefereeUpdateRequest request);

    void delete(UUID id);
}

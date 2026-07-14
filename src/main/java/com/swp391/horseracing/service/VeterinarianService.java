package com.swp391.horseracing.service;

import com.swp391.horseracing.dto.veterinarian.request.VeterinarianCreationRequest;
import com.swp391.horseracing.dto.veterinarian.request.VeterinarianUpdateRequest;
import com.swp391.horseracing.dto.veterinarian.response.VeterinarianResponse;

import java.util.List;
import java.util.UUID;

public interface VeterinarianService {

    VeterinarianResponse create(VeterinarianCreationRequest request);

    List<VeterinarianResponse> getAll();

    VeterinarianResponse getById(UUID id);

    VeterinarianResponse update(UUID id, VeterinarianUpdateRequest request);

    void delete(UUID id);
}

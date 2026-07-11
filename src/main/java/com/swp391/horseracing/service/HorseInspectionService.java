package com.swp391.horseracing.service;

import com.swp391.horseracing.dto.horseinspection.request.HorseInspectionRequest;
import com.swp391.horseracing.dto.horseinspection.response.HorseInspectionResponse;

import java.util.UUID;

public interface HorseInspectionService {
    HorseInspectionResponse createInspection(UUID entryId, HorseInspectionRequest request);
}

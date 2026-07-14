package com.swp391.horseracing.service;

import com.swp391.horseracing.dto.jockeyinspection.request.JockeyInspectionRequest;
import com.swp391.horseracing.dto.jockeyinspection.response.JockeyInspectionResponse;

import java.util.UUID;

public interface JockeyInspectionService {
    JockeyInspectionResponse createInspection(UUID entryId, JockeyInspectionRequest request);
    JockeyInspectionResponse getInspection(UUID entryId);
}

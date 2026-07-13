package com.swp391.horseracing.service;

import com.swp391.horseracing.dto.medicalstaff.request.AssignInspectionStaffRequest;
import com.swp391.horseracing.dto.medicalstaff.response.InspectionStaffAssignmentResponse;

import java.util.UUID;

public interface RaceInspectionStaffService {
    InspectionStaffAssignmentResponse assign(UUID raceId, AssignInspectionStaffRequest request);

    InspectionStaffAssignmentResponse autoAssign(UUID raceId);
}

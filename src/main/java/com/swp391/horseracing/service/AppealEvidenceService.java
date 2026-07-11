package com.swp391.horseracing.service;

import com.swp391.horseracing.dto.appeal_evidence.request.AddAppealEvidenceRequest;
import com.swp391.horseracing.dto.appeal_evidence.response.AppealEvidenceResponse;

import java.util.List;
import java.util.UUID;

public interface AppealEvidenceService {

    AppealEvidenceResponse addEvidence(UUID appealId, AddAppealEvidenceRequest request);

    List<AppealEvidenceResponse> getEvidencesByAppealId(UUID appealId);
}

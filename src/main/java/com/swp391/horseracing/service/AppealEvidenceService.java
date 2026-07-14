package com.swp391.horseracing.service;

import com.swp391.horseracing.dto.appeal_evidence.request.AddAppealEvidenceRequest;
import com.swp391.horseracing.dto.appeal_evidence.response.AppealEvidenceResponse;

import java.util.List;
import java.util.UUID;
import com.swp391.horseracing.enums.AppealEvidenceType;
import org.springframework.web.multipart.MultipartFile;

public interface AppealEvidenceService {

    AppealEvidenceResponse addEvidence(UUID appealId, AddAppealEvidenceRequest request);

    List<AppealEvidenceResponse> getEvidencesByAppealId(UUID appealId);

    AppealEvidenceResponse updateEvidence(UUID appealId, UUID evidenceId, AddAppealEvidenceRequest request);

    void deleteEvidence(UUID appealId, UUID evidenceId);

    AppealEvidenceResponse uploadEvidence(
            UUID appealId, AppealEvidenceType type, MultipartFile file, String description);
}

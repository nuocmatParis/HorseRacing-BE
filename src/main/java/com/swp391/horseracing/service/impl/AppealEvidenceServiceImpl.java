package com.swp391.horseracing.service.impl;

import com.swp391.horseracing.dto.appeal_evidence.request.AddAppealEvidenceRequest;
import com.swp391.horseracing.dto.appeal_evidence.response.AppealEvidenceResponse;
import com.swp391.horseracing.entity.Appeal;
import com.swp391.horseracing.entity.AppealEvidence;
import com.swp391.horseracing.enums.AppealEvidenceType;
import com.swp391.horseracing.enums.AppealStatus;
import com.swp391.horseracing.exception.AppException;
import com.swp391.horseracing.exception.ErrorCode;
import com.swp391.horseracing.mapper.AppealEvidenceMapper;
import com.swp391.horseracing.repository.AppealEvidenceRepository;
import com.swp391.horseracing.repository.AppealRepository;
import com.swp391.horseracing.service.AppealEvidenceService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequiredArgsConstructor
public class AppealEvidenceServiceImpl implements AppealEvidenceService {

    AppealEvidenceRepository appealEvidenceRepository;
    AppealRepository appealRepository;
    AppealEvidenceMapper appealEvidenceMapper;

    @Override
    @Transactional
    public AppealEvidenceResponse addEvidence(UUID appealId, AddAppealEvidenceRequest request) {
        Appeal appeal = appealRepository.findById(appealId)
                .orElseThrow(() -> new AppException(ErrorCode.APPEAL_NOT_FOUND));

        if (appeal.getStatus() != AppealStatus.Pending) {
            throw new AppException(ErrorCode.APPEAL_NOT_PENDING);
        }

        if (request.getType() == AppealEvidenceType.Image
                || request.getType() == AppealEvidenceType.Video
                || request.getType() == AppealEvidenceType.Document) {
            if (request.getFileUrl() == null || request.getFileUrl().isBlank()) {
                throw new AppException(ErrorCode.APPEAL_EVIDENCE_REQUIRED);
            }
        }

        if (request.getType() == AppealEvidenceType.Text) {
            if (request.getTextContent() == null || request.getTextContent().isBlank()) {
                throw new AppException(ErrorCode.APPEAL_EVIDENCE_REQUIRED);
            }
        }

        AppealEvidence evidence = AppealEvidence.builder()
                .appeal(appeal)
                .type(request.getType())
                .fileUrl(request.getFileUrl())
                .textContent(request.getTextContent())
                .description(request.getDescription())
                .build();

        return appealEvidenceMapper.toAppealEvidenceResponse(
                appealEvidenceRepository.save(evidence));
    }

    @Override
    public List<AppealEvidenceResponse> getEvidencesByAppealId(UUID appealId) {
        if (!appealRepository.existsById(appealId)) {
            throw new AppException(ErrorCode.APPEAL_NOT_FOUND);
        }
        return appealEvidenceRepository.findByAppeal_AppealId(appealId)
                .stream()
                .map(appealEvidenceMapper::toAppealEvidenceResponse)
                .toList();
    }
}

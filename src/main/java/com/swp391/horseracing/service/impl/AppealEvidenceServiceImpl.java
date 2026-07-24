package com.swp391.horseracing.service.impl;

import com.swp391.horseracing.dto.appeal_evidence.request.AddAppealEvidenceRequest;
import com.swp391.horseracing.dto.appeal_evidence.response.AppealEvidenceResponse;
import com.swp391.horseracing.entity.Appeal;
import com.swp391.horseracing.entity.AppealEvidence;
import com.swp391.horseracing.entity.Race;
import com.swp391.horseracing.entity.Referee;
import com.swp391.horseracing.entity.Round;
import com.swp391.horseracing.entity.User;
import com.swp391.horseracing.enums.AppealEvidenceType;
import com.swp391.horseracing.enums.AppealStatus;
import com.swp391.horseracing.exception.AppException;
import com.swp391.horseracing.exception.ErrorCode;
import com.swp391.horseracing.mapper.AppealEvidenceMapper;
import com.swp391.horseracing.repository.AppealEvidenceRepository;
import com.swp391.horseracing.repository.AppealRepository;
import com.swp391.horseracing.repository.RaceRefereeRepository;
import com.swp391.horseracing.repository.RefereeRepository;
import com.swp391.horseracing.service.AppealEvidenceService;
import com.swp391.horseracing.service.CloudinaryService;
import com.swp391.horseracing.service.UserCurrentService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequiredArgsConstructor
public class AppealEvidenceServiceImpl implements AppealEvidenceService {

    AppealEvidenceRepository appealEvidenceRepository;
    AppealRepository appealRepository;
    AppealEvidenceMapper appealEvidenceMapper;
    UserCurrentService userCurrentService;
    CloudinaryService cloudinaryService;
    RefereeRepository refereeRepository;
    RaceRefereeRepository raceRefereeRepository;

    @NonFinal
    @Value("${appeal.evidence.max-file-size-bytes:52428800}")
    long maxFileSizeBytes;

    @Override
    @Transactional
    public AppealEvidenceResponse addEvidence(UUID appealId, AddAppealEvidenceRequest request) {
        Appeal appeal = getPendingOwnedAppeal(appealId);
        validateEvidenceData(request.getType(), request.getFileUrl(), request.getTextContent());
        AppealEvidence evidence = buildEvidence(
                appeal, request.getType(), request.getFileUrl(), request.getTextContent(), request.getDescription());
        return appealEvidenceMapper.toAppealEvidenceResponse(appealEvidenceRepository.save(evidence));
    }

    @Override
    @Transactional(readOnly = true)
    public List<AppealEvidenceResponse> getEvidencesByAppealId(UUID appealId) {
        Appeal appeal = appealRepository.findById(appealId)
                .orElseThrow(() -> new AppException(ErrorCode.APPEAL_NOT_FOUND));
        validateCanReadEvidence(appeal);
        List<AppealEvidence> evidences = appealEvidenceRepository.findByAppeal_AppealId(appealId);
        List<AppealEvidenceResponse> responses = new ArrayList<>();
        for (AppealEvidence evidence : evidences) {
            responses.add(appealEvidenceMapper.toAppealEvidenceResponse(evidence));
        }
        return responses;
    }

    private void validateCanReadEvidence(Appeal appeal) {
        User currentUser = userCurrentService.getCurrentUser();
        if (appeal.getSubmittedBy() != null
                && appeal.getSubmittedBy().getUserId().equals(currentUser.getUserId())) {
            return;
        }

        Referee referee = refereeRepository.findByUser_UserId(currentUser.getUserId())
                .orElseThrow(() -> new AppException(ErrorCode.ACCESS_DENIED));
        Race race = appeal.getEntry().getRace();
        boolean directRaceReferee = raceRefereeRepository
                .existsByRace_RaceIdAndReferee_RefereeId(race.getRaceId(), referee.getRefereeId());
        Round round = race.getRound();
        boolean headReferee = round != null
                && round.getHeadReferee() != null
                && round.getHeadReferee().getRefereeId().equals(referee.getRefereeId());
        if (!directRaceReferee && !headReferee) {
            throw new AppException(ErrorCode.ACCESS_DENIED);
        }
    }

    @Override
    @Transactional
    public AppealEvidenceResponse updateEvidence(
            UUID appealId, UUID evidenceId, AddAppealEvidenceRequest request) {
        Appeal appeal = getPendingOwnedAppeal(appealId);
        AppealEvidence evidence = getEvidence(appealId, evidenceId);
        validateEvidenceData(request.getType(), request.getFileUrl(), request.getTextContent());
        evidence.setAppeal(appeal);
        evidence.setType(request.getType());
        evidence.setFileUrl(request.getFileUrl());
        evidence.setTextContent(request.getTextContent());
        evidence.setDescription(request.getDescription());
        return appealEvidenceMapper.toAppealEvidenceResponse(appealEvidenceRepository.save(evidence));
    }

    @Override
    @Transactional
    public void deleteEvidence(UUID appealId, UUID evidenceId) {
        getPendingOwnedAppeal(appealId);
        AppealEvidence evidence = getEvidence(appealId, evidenceId);
        appealEvidenceRepository.delete(evidence);
    }

    @Override
    @Transactional
    public AppealEvidenceResponse uploadEvidence(
            UUID appealId, AppealEvidenceType type, MultipartFile file, String description) {
        Appeal appeal = getPendingOwnedAppeal(appealId);
        validateUpload(type, file);
        String resourceType = resourceType(type);
        try {
            String fileUrl = cloudinaryService.uploadFile(file, "appeal_evidences", resourceType);
            AppealEvidence evidence = buildEvidence(appeal, type, fileUrl, null, description);
            return appealEvidenceMapper.toAppealEvidenceResponse(appealEvidenceRepository.save(evidence));
        } catch (IOException exception) {
            throw new AppException(ErrorCode.APPEAL_EVIDENCE_UPLOAD_FAILED);
        }
    }

    private Appeal getPendingOwnedAppeal(UUID appealId) {
        Appeal appeal = appealRepository.findById(appealId)
                .orElseThrow(() -> new AppException(ErrorCode.APPEAL_NOT_FOUND));
        if (appeal.getStatus() != AppealStatus.Pending) {
            throw new AppException(ErrorCode.APPEAL_NOT_PENDING);
        }
        User currentUser = userCurrentService.getCurrentUser();
        if (!appeal.getSubmittedBy().getUserId().equals(currentUser.getUserId())) {
            throw new AppException(ErrorCode.ACCESS_DENIED);
        }
        return appeal;
    }

    private AppealEvidence getEvidence(UUID appealId, UUID evidenceId) {
        AppealEvidence evidence = appealEvidenceRepository.findById(evidenceId)
                .orElseThrow(() -> new AppException(ErrorCode.APPEAL_EVIDENCE_NOT_FOUND));
        if (!evidence.getAppeal().getAppealId().equals(appealId)) {
            throw new AppException(ErrorCode.APPEAL_EVIDENCE_NOT_FOUND);
        }
        return evidence;
    }

    private void validateEvidenceData(AppealEvidenceType type, String fileUrl, String textContent) {
        if (type == AppealEvidenceType.Text) {
            if (textContent == null || textContent.isBlank()) {
                throw new AppException(ErrorCode.APPEAL_EVIDENCE_REQUIRED);
            }
            return;
        }
        if (fileUrl == null || fileUrl.isBlank()) {
            throw new AppException(ErrorCode.APPEAL_EVIDENCE_REQUIRED);
        }
    }

    private void validateUpload(AppealEvidenceType type, MultipartFile file) {
        if (type == null || type == AppealEvidenceType.Text || file == null || file.isEmpty()) {
            throw new AppException(ErrorCode.APPEAL_EVIDENCE_FILE_TYPE_INVALID);
        }
        if (file.getSize() > maxFileSizeBytes) {
            throw new AppException(ErrorCode.APPEAL_EVIDENCE_FILE_TOO_LARGE);
        }
        String contentType = file.getContentType() == null ? "" : file.getContentType().toLowerCase();
        boolean valid;
        if (type == AppealEvidenceType.Image) {
            valid = contentType.startsWith("image/");
        } else if (type == AppealEvidenceType.Video) {
            valid = contentType.startsWith("video/");
        } else {
            valid = contentType.startsWith("application/") || contentType.startsWith("text/");
        }
        if (!valid) {
            throw new AppException(ErrorCode.APPEAL_EVIDENCE_FILE_TYPE_INVALID);
        }
    }

    private String resourceType(AppealEvidenceType type) {
        if (type == AppealEvidenceType.Video) {
            return "video";
        }
        if (type == AppealEvidenceType.Document) {
            return "raw";
        }
        return "image";
    }

    private AppealEvidence buildEvidence(
            Appeal appeal, AppealEvidenceType type, String fileUrl, String textContent, String description) {
        return AppealEvidence.builder()
                .appeal(appeal)
                .type(type)
                .fileUrl(fileUrl)
                .textContent(textContent)
                .description(description)
                .build();
    }
}

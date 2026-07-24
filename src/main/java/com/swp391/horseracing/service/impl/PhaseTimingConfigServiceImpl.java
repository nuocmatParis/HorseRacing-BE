package com.swp391.horseracing.service.impl;

import com.swp391.horseracing.dto.phasetiming.request.PhaseTimingConfigBatchRequest;
import com.swp391.horseracing.dto.phasetiming.request.PhaseTimingConfigRequest;
import com.swp391.horseracing.dto.phasetiming.response.PhaseTimingConfigResponse;
import com.swp391.horseracing.entity.PhaseTimingConfig;
import com.swp391.horseracing.exception.AppException;
import com.swp391.horseracing.exception.ErrorCode;
import com.swp391.horseracing.repository.PhaseTimingConfigRepository;
import com.swp391.horseracing.service.PhaseTimingConfigService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class PhaseTimingConfigServiceImpl implements PhaseTimingConfigService {

    PhaseTimingConfigRepository phaseTimingConfigRepository;

    @Override
    public List<PhaseTimingConfigResponse> getAllConfigs() {
        return phaseTimingConfigRepository.findAllByOrderByPhaseNameAscMinCapacityAsc()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    public PhaseTimingConfigResponse getConfigById(Long id) {
        PhaseTimingConfig config = phaseTimingConfigRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_FOUND));
        return toResponse(config);
    }

    @Override
    @Transactional
    public PhaseTimingConfigResponse createConfig(PhaseTimingConfigRequest request) {
        validateCapacityRange(request.getMinCapacity(), request.getMaxCapacity());
        PhaseTimingConfig config = PhaseTimingConfig.builder()
                .phaseName(request.getPhaseName().toUpperCase())
                .minCapacity(request.getMinCapacity())
                .maxCapacity(request.getMaxCapacity())
                .durationDays(request.getDurationDays())
                .description(request.getDescription())
                .build();
        return toResponse(phaseTimingConfigRepository.save(config));
    }

    @Override
    @Transactional
    public PhaseTimingConfigResponse updateConfig(Long id, PhaseTimingConfigRequest request) {
        validateCapacityRange(request.getMinCapacity(), request.getMaxCapacity());
        PhaseTimingConfig config = phaseTimingConfigRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_FOUND));
        config.setPhaseName(request.getPhaseName().toUpperCase());
        config.setMinCapacity(request.getMinCapacity());
        config.setMaxCapacity(request.getMaxCapacity());
        config.setDurationDays(request.getDurationDays());
        config.setDescription(request.getDescription());
        return toResponse(phaseTimingConfigRepository.save(config));
    }

    @Override
    @Transactional
    public void deleteConfig(Long id) {
        if (!phaseTimingConfigRepository.existsById(id)) {
            throw new AppException(ErrorCode.RESOURCE_NOT_FOUND);
        }
        phaseTimingConfigRepository.deleteById(id);
    }

    @Override
    @Transactional
    public List<PhaseTimingConfigResponse> batchSaveConfigs(PhaseTimingConfigBatchRequest batchRequest) {
        phaseTimingConfigRepository.deleteAll();
        List<PhaseTimingConfig> entities = new ArrayList<>();
        for (PhaseTimingConfigRequest req : batchRequest.getConfigs()) {
            validateCapacityRange(req.getMinCapacity(), req.getMaxCapacity());
            PhaseTimingConfig config = PhaseTimingConfig.builder()
                    .phaseName(req.getPhaseName().toUpperCase())
                    .minCapacity(req.getMinCapacity())
                    .maxCapacity(req.getMaxCapacity())
                    .durationDays(req.getDurationDays())
                    .description(req.getDescription())
                    .build();
            entities.add(config);
        }
        return phaseTimingConfigRepository.saveAll(entities)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    private void validateCapacityRange(int minCap, int maxCap) {
        if (minCap > maxCap) {
            throw new IllegalArgumentException("minCapacity cannot be greater than maxCapacity");
        }
    }

    private PhaseTimingConfigResponse toResponse(PhaseTimingConfig config) {
        return PhaseTimingConfigResponse.builder()
                .id(config.getId())
                .phaseName(config.getPhaseName())
                .minCapacity(config.getMinCapacity())
                .maxCapacity(config.getMaxCapacity())
                .durationDays(config.getDurationDays())
                .description(config.getDescription())
                .createdAt(config.getCreatedAt())
                .updatedAt(config.getUpdatedAt())
                .build();
    }
}

package com.swp391.horseracing.service;

import com.swp391.horseracing.dto.phasetiming.request.PhaseTimingConfigBatchRequest;
import com.swp391.horseracing.dto.phasetiming.request.PhaseTimingConfigRequest;
import com.swp391.horseracing.dto.phasetiming.response.PhaseTimingConfigResponse;

import java.util.List;

public interface PhaseTimingConfigService {
    List<PhaseTimingConfigResponse> getAllConfigs();
    PhaseTimingConfigResponse getConfigById(Long id);
    PhaseTimingConfigResponse createConfig(PhaseTimingConfigRequest request);
    PhaseTimingConfigResponse updateConfig(Long id, PhaseTimingConfigRequest request);
    void deleteConfig(Long id);
    List<PhaseTimingConfigResponse> batchSaveConfigs(PhaseTimingConfigBatchRequest batchRequest);
}

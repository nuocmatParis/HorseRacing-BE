package com.swp391.horseracing.service;

import com.swp391.horseracing.dto.phasetiming.request.PhaseTimingConfigBatchRequest;
import com.swp391.horseracing.dto.phasetiming.request.PhaseTimingConfigRequest;
import com.swp391.horseracing.dto.phasetiming.response.PhaseTimingConfigResponse;
import com.swp391.horseracing.entity.PhaseTimingConfig;
import com.swp391.horseracing.repository.PhaseTimingConfigRepository;
import com.swp391.horseracing.service.impl.PhaseTimingConfigServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PhaseTimingConfigServiceTest {

    @Mock
    private PhaseTimingConfigRepository repository;

    @InjectMocks
    private PhaseTimingConfigServiceImpl service;

    private PhaseTimingConfig config;

    @BeforeEach
    void setUp() {
        config = PhaseTimingConfig.builder()
                .id(1L)
                .phaseName("REGISTRATION")
                .minCapacity(0)
                .maxCapacity(16)
                .durationDays(5)
                .description("Registration 16 entries")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    @Test
    void getAllConfigs_Success() {
        when(repository.findAllByOrderByPhaseNameAscMinCapacityAsc()).thenReturn(List.of(config));

        List<PhaseTimingConfigResponse> responses = service.getAllConfigs();

        assertNotNull(responses);
        assertEquals(1, responses.size());
        assertEquals("REGISTRATION", responses.get(0).getPhaseName());
    }

    @Test
    void getConfigById_Success() {
        when(repository.findById(1L)).thenReturn(Optional.of(config));

        PhaseTimingConfigResponse response = service.getConfigById(1L);

        assertNotNull(response);
        assertEquals(1L, response.getId());
        assertEquals(5, response.getDurationDays());
    }

    @Test
    void createConfig_Success() {
        PhaseTimingConfigRequest req = PhaseTimingConfigRequest.builder()
                .phaseName("REVIEW")
                .minCapacity(0)
                .maxCapacity(16)
                .durationDays(4)
                .description("Review period")
                .build();

        when(repository.save(any(PhaseTimingConfig.class))).thenReturn(config);

        PhaseTimingConfigResponse response = service.createConfig(req);

        assertNotNull(response);
        verify(repository, times(1)).save(any());
    }

    @Test
    void createConfig_InvalidRange_ThrowsException() {
        PhaseTimingConfigRequest req = PhaseTimingConfigRequest.builder()
                .phaseName("REVIEW")
                .minCapacity(20)
                .maxCapacity(10)
                .durationDays(4)
                .build();

        assertThrows(IllegalArgumentException.class, () -> service.createConfig(req));
    }

    @Test
    void batchSaveConfigs_Success() {
        PhaseTimingConfigRequest req = PhaseTimingConfigRequest.builder()
                .phaseName("SCHEDULING")
                .minCapacity(0)
                .maxCapacity(32)
                .durationDays(3)
                .build();

        PhaseTimingConfigBatchRequest batchReq = PhaseTimingConfigBatchRequest.builder()
                .configs(List.of(req))
                .build();

        when(repository.saveAll(anyList())).thenReturn(List.of(config));

        List<PhaseTimingConfigResponse> responses = service.batchSaveConfigs(batchReq);

        assertNotNull(responses);
        assertEquals(1, responses.size());
        verify(repository, times(1)).deleteAll();
        verify(repository, times(1)).saveAll(anyList());
    }
}

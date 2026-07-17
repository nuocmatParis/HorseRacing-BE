package com.swp391.horseracing.controller;

import com.swp391.horseracing.dto.race_entry.request.CreateRaceEntryRequest;
import com.swp391.horseracing.dto.race_entry.response.RaceEntryResponse;
import com.swp391.horseracing.service.ContractService;
import com.swp391.horseracing.service.PrizeStructureService;
import com.swp391.horseracing.service.RaceEntryService;
import com.swp391.horseracing.service.RaceInspectionStaffService;
import com.swp391.horseracing.service.RaceRefereeService;
import com.swp391.horseracing.service.RaceService;
import com.swp391.horseracing.service.RefereeService;
import com.swp391.horseracing.service.RoundService;
import com.swp391.horseracing.service.TournamentEligibilityService;
import com.swp391.horseracing.service.TournamentRegistrationService;
import com.swp391.horseracing.service.TournamentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;

@ExtendWith(MockitoExtension.class)
class AdminRaceEntryEndpointTest {

    @Mock TournamentService tournamentService;
    @Mock RoundService roundService;
    @Mock RaceService raceService;
    @Mock PrizeStructureService prizeStructureService;
    @Mock TournamentEligibilityService tournamentEligibilityService;
    @Mock TournamentRegistrationService tournamentRegistrationService;
    @Mock ContractService contractService;
    @Mock RaceEntryService raceEntryService;
    @Mock RaceRefereeService raceRefereeService;
    @Mock RefereeService refereeService;
    @Mock RaceInspectionStaffService raceInspectionStaffService;

    @InjectMocks AdminController adminController;

    MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();
        mockMvc = standaloneSetup(adminController)
                .setValidator(validator)
                .build();
    }

    @Test
    void createRaceEntryUsesRaceIdFromPathInsteadOfRequiringItInBody() throws Exception {
        UUID raceId = UUID.randomUUID();
        UUID contractId = UUID.randomUUID();
        when(raceEntryService.create(any(CreateRaceEntryRequest.class)))
                .thenReturn(new RaceEntryResponse());

        mockMvc.perform(post("/api/admin/races/{raceId}/entries", raceId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"contractId\":\"" + contractId + "\"}"))
                .andExpect(status().isOk());

        ArgumentCaptor<CreateRaceEntryRequest> captor =
                ArgumentCaptor.forClass(CreateRaceEntryRequest.class);
        verify(raceEntryService).create(captor.capture());
        assertEquals(raceId, captor.getValue().getRaceId());
        assertEquals(contractId, captor.getValue().getContractId());
        assertNull(captor.getValue().getLaneNumber());
    }
}

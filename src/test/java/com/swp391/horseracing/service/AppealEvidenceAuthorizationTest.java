package com.swp391.horseracing.service;

import com.swp391.horseracing.entity.Appeal;
import com.swp391.horseracing.entity.Race;
import com.swp391.horseracing.entity.RaceEntry;
import com.swp391.horseracing.entity.Referee;
import com.swp391.horseracing.entity.Round;
import com.swp391.horseracing.entity.User;
import com.swp391.horseracing.exception.AppException;
import com.swp391.horseracing.exception.ErrorCode;
import com.swp391.horseracing.mapper.AppealEvidenceMapper;
import com.swp391.horseracing.repository.AppealEvidenceRepository;
import com.swp391.horseracing.repository.AppealRepository;
import com.swp391.horseracing.repository.RaceRefereeRepository;
import com.swp391.horseracing.repository.RefereeRepository;
import com.swp391.horseracing.service.impl.AppealEvidenceServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AppealEvidenceAuthorizationTest {

    @Mock AppealEvidenceRepository appealEvidenceRepository;
    @Mock AppealRepository appealRepository;
    @Mock AppealEvidenceMapper appealEvidenceMapper;
    @Mock UserCurrentService userCurrentService;
    @Mock CloudinaryService cloudinaryService;
    @Mock RefereeRepository refereeRepository;
    @Mock RaceRefereeRepository raceRefereeRepository;
    @InjectMocks AppealEvidenceServiceImpl service;

    private UUID appealId;
    private Appeal appeal;
    private Race race;
    private User currentUser;

    @BeforeEach
    void setUp() {
        appealId = UUID.randomUUID();
        currentUser = new User();
        currentUser.setUserId(UUID.randomUUID());

        User submitter = new User();
        submitter.setUserId(UUID.randomUUID());
        Round round = new Round();
        round.setRoundId(UUID.randomUUID());
        race = new Race();
        race.setRaceId(UUID.randomUUID());
        race.setRound(round);
        RaceEntry entry = new RaceEntry();
        entry.setEntryId(UUID.randomUUID());
        entry.setRace(race);
        appeal = new Appeal();
        appeal.setAppealId(appealId);
        appeal.setEntry(entry);
        appeal.setSubmittedBy(submitter);

        when(appealRepository.findById(appealId)).thenReturn(Optional.of(appeal));
        when(userCurrentService.getCurrentUser()).thenReturn(currentUser);
    }

    @Test
    void unrelatedUserCannotReadEvidence() {
        when(refereeRepository.findByUser_UserId(currentUser.getUserId()))
                .thenReturn(Optional.empty());

        AppException exception = assertThrows(AppException.class,
                () -> service.getEvidencesByAppealId(appealId));

        assertEquals(ErrorCode.ACCESS_DENIED, exception.getErrorCode());
    }

    @Test
    void assignedRaceRefereeCanReadEvidence() {
        Referee referee = new Referee();
        referee.setRefereeId(UUID.randomUUID());
        referee.setUser(currentUser);
        when(refereeRepository.findByUser_UserId(currentUser.getUserId()))
                .thenReturn(Optional.of(referee));
        when(raceRefereeRepository.existsByRace_RaceIdAndReferee_RefereeId(
                race.getRaceId(), referee.getRefereeId())).thenReturn(true);
        when(appealEvidenceRepository.findByAppeal_AppealId(appealId))
                .thenReturn(new ArrayList<>());

        assertTrue(service.getEvidencesByAppealId(appealId).isEmpty());
    }
}

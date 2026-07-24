package com.swp391.horseracing.service;

import com.swp391.horseracing.dto.appeal.request.CreateAppealRequest;
import com.swp391.horseracing.dto.appeal.request.ReviewAppealRequest;
import com.swp391.horseracing.dto.appeal.response.AppealResponse;
import com.swp391.horseracing.entity.Appeal;
import com.swp391.horseracing.entity.HorseOwner;
import com.swp391.horseracing.entity.JockeyHorseContract;
import com.swp391.horseracing.entity.Race;
import com.swp391.horseracing.entity.RaceEntry;
import com.swp391.horseracing.entity.Referee;
import com.swp391.horseracing.entity.Round;
import com.swp391.horseracing.entity.User;
import com.swp391.horseracing.enums.AccountStatus;
import com.swp391.horseracing.enums.AppealStatus;
import com.swp391.horseracing.enums.RefereeStatus;
import com.swp391.horseracing.enums.RoundStatus;
import com.swp391.horseracing.exception.AppException;
import com.swp391.horseracing.exception.ErrorCode;
import com.swp391.horseracing.mapper.AppealMapper;
import com.swp391.horseracing.repository.AppealCategoryRepository;
import com.swp391.horseracing.repository.AppealRepository;
import com.swp391.horseracing.repository.HorseOwnerRepository;
import com.swp391.horseracing.repository.JockeyRepository;
import com.swp391.horseracing.repository.RaceEntryRepository;
import com.swp391.horseracing.repository.RaceRefereeRepository;
import com.swp391.horseracing.repository.RaceReportRepository;
import com.swp391.horseracing.repository.RaceResultRepository;
import com.swp391.horseracing.repository.RefereeRepository;
import com.swp391.horseracing.repository.ViolationRepository;
import com.swp391.horseracing.service.impl.AppealServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AppealRaceFinishedGuardTest {

    @Mock AppealRepository appealRepository;
    @Mock AppealCategoryRepository appealCategoryRepository;
    @Mock RaceEntryRepository raceEntryRepository;
    @Mock RaceResultRepository raceResultRepository;
    @Mock RaceReportRepository raceReportRepository;
    @Mock HorseOwnerRepository horseOwnerRepository;
    @Mock JockeyRepository jockeyRepository;
    @Mock RefereeRepository refereeRepository;
    @Mock RaceRefereeRepository raceRefereeRepository;
    @Mock AppealMapper appealMapper;
    @Mock ViolationRepository violationRepository;
    @Mock UserCurrentService userCurrentService;
    @Mock BusinessNotificationEventService notificationEventService;

    @InjectMocks AppealServiceImpl service;

    @Test
    void ownerCannotAppealWhileRaceIsStillOngoing() {
        UUID userId = UUID.randomUUID();
        UUID ownerId = UUID.randomUUID();
        UUID entryId = UUID.randomUUID();
        User user = User.builder().userId(userId).status(AccountStatus.ACTIVE).build();
        HorseOwner owner = HorseOwner.builder().ownerId(ownerId).user(user).build();
        Race race = Race.builder().raceId(UUID.randomUUID()).status(RoundStatus.ONGOING).build();
        JockeyHorseContract contract = JockeyHorseContract.builder().owner(owner).build();
        RaceEntry entry = RaceEntry.builder().entryId(entryId).race(race).contract(contract).build();
        CreateAppealRequest request = CreateAppealRequest.builder()
                .entryId(entryId)
                .categoryId(UUID.randomUUID())
                .description("Yêu cầu xem lại kết quả")
                .build();

        when(userCurrentService.getCurrentUser()).thenReturn(user);
        when(raceEntryRepository.findById(entryId)).thenReturn(Optional.of(entry));
        when(horseOwnerRepository.findByUser_UserId(userId)).thenReturn(Optional.of(owner));
        when(jockeyRepository.findByUser_UserId(userId)).thenReturn(Optional.empty());

        AppException exception = assertThrows(AppException.class, () -> service.create(request));

        assertEquals(ErrorCode.RACE_HAS_NOT_FINISHED, exception.getErrorCode());
    }

    @Test
    void unrelatedRefereeCannotReadAppealDetail() {
        UUID userId = UUID.randomUUID();
        UUID refereeId = UUID.randomUUID();
        UUID appealId = UUID.randomUUID();
        User user = User.builder().userId(userId).build();
        Referee referee = Referee.builder()
                .refereeId(refereeId)
                .user(user)
                .status(RefereeStatus.AVAILABLE)
                .build();
        Round round = Round.builder().roundId(UUID.randomUUID()).build();
        Race race = Race.builder().raceId(UUID.randomUUID()).round(round).build();
        RaceEntry entry = RaceEntry.builder().entryId(UUID.randomUUID()).race(race).build();
        Appeal appeal = Appeal.builder().appealId(appealId).entry(entry).build();

        when(userCurrentService.getCurrentUser()).thenReturn(user);
        when(refereeRepository.findByUser_UserId(userId)).thenReturn(Optional.of(referee));
        when(appealRepository.findById(appealId)).thenReturn(Optional.of(appeal));
        when(raceRefereeRepository.existsByRace_RaceIdAndReferee_RefereeId(
                race.getRaceId(), refereeId)).thenReturn(false);

        AppException exception = assertThrows(AppException.class,
                () -> service.getAppealDetail(appealId));

        assertEquals(ErrorCode.ACCESS_DENIED, exception.getErrorCode());
    }

    @Test
    void directRaceRefereeCanReviewAppeal() {
        UUID userId = UUID.randomUUID();
        UUID refereeId = UUID.randomUUID();
        UUID appealId = UUID.randomUUID();
        User user = User.builder().userId(userId).build();
        Referee referee = Referee.builder()
                .refereeId(refereeId)
                .user(user)
                .status(RefereeStatus.AVAILABLE)
                .build();
        Race race = Race.builder().raceId(UUID.randomUUID()).build();
        RaceEntry entry = RaceEntry.builder().entryId(UUID.randomUUID()).race(race).build();
        Appeal appeal = Appeal.builder()
                .appealId(appealId)
                .entry(entry)
                .status(AppealStatus.Pending)
                .build();
        ReviewAppealRequest request = ReviewAppealRequest.builder()
                .status(AppealStatus.Accepted)
                .resolution("Accepted after video review")
                .build();
        AppealResponse mappedResponse = new AppealResponse();

        when(userCurrentService.getCurrentUser()).thenReturn(user);
        when(refereeRepository.findByUser_UserId(userId)).thenReturn(Optional.of(referee));
        when(appealRepository.findForUpdateByAppealId(appealId)).thenReturn(Optional.of(appeal));
        when(raceRefereeRepository.existsByRace_RaceIdAndReferee_RefereeId(
                race.getRaceId(), refereeId)).thenReturn(true);
        when(appealRepository.save(appeal)).thenReturn(appeal);
        when(appealMapper.toAppealResponse(appeal)).thenReturn(mappedResponse);

        AppealResponse response = service.review(appealId, request);

        assertEquals(mappedResponse, response);
        assertEquals(AppealStatus.Accepted, appeal.getStatus());
        assertEquals(referee, appeal.getReviewedBy());
        verify(notificationEventService).appealReviewed(appeal);
    }
}

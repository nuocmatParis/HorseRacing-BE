package com.swp391.horseracing.service.impl;

import com.swp391.horseracing.dto.common.PageResponse;
import com.swp391.horseracing.dto.spectator.response.SpectatorHorseResponse;
import com.swp391.horseracing.entity.Horse;
import com.swp391.horseracing.entity.RaceEntry;
import com.swp391.horseracing.entity.Spectator;
import com.swp391.horseracing.entity.SpectatorHorseFollow;
import com.swp391.horseracing.entity.User;
import com.swp391.horseracing.enums.AccountStatus;
import com.swp391.horseracing.enums.RoleName;
import com.swp391.horseracing.exception.AppException;
import com.swp391.horseracing.exception.ErrorCode;
import com.swp391.horseracing.repository.HorseRepository;
import com.swp391.horseracing.repository.RaceEntryRepository;
import com.swp391.horseracing.repository.SpectatorHorseFollowRepository;
import com.swp391.horseracing.repository.SpectatorRepository;
import com.swp391.horseracing.service.SpectatorHorseService;
import com.swp391.horseracing.service.UserCurrentService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class SpectatorHorseServiceImpl implements SpectatorHorseService {
    HorseRepository horseRepository;
    SpectatorRepository spectatorRepository;
    SpectatorHorseFollowRepository followRepository;
    RaceEntryRepository raceEntryRepository;
    UserCurrentService userCurrentService;

    @Override
    @Transactional(readOnly = true)
    public PageResponse<SpectatorHorseResponse> searchHorses(
            String query, String raceClass, String healthStatus, int page, int size) {
        validatePage(page, size);
        Spectator spectator = getCurrentSpectator();
        String normalizedQuery = normalizeOptional(query);
        String normalizedRaceClass = normalizeOptional(raceClass);
        String normalizedHealthStatus = normalizeOptional(healthStatus);
        Page<Horse> horses = horseRepository.searchForSpectator(
                normalizedQuery, normalizedRaceClass, normalizedHealthStatus, PageRequest.of(page, size));
        List<SpectatorHorseResponse> items = new ArrayList<>();
        for (Horse horse : horses.getContent()) {
            items.add(toResponse(horse, spectator, null));
        }
        return toPageResponse(horses, items);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<SpectatorHorseResponse> getFollowingHorses(int page, int size) {
        validatePage(page, size);
        Spectator spectator = getCurrentSpectator();
        Page<SpectatorHorseFollow> follows = followRepository
                .findBySpectator_SpectatorIdOrderByFollowedAtDesc(
                        spectator.getSpectatorId(), PageRequest.of(page, size));
        List<SpectatorHorseResponse> items = new ArrayList<>();
        for (SpectatorHorseFollow follow : follows.getContent()) {
            items.add(toResponse(follow.getHorse(), spectator, follow.getFollowedAt()));
        }
        return new PageResponse<>(items, follows.getNumber(), follows.getSize(), follows.getTotalElements(),
                follows.getTotalPages(), follows.isFirst(), follows.isLast());
    }

    @Override
    @Transactional
    public SpectatorHorseResponse followHorse(UUID horseId) {
        Spectator spectator = getCurrentSpectator();
        Horse horse = getHorse(horseId);
        if (followRepository.existsBySpectator_SpectatorIdAndHorse_HorseId(
                spectator.getSpectatorId(), horseId)) {
            throw new AppException(ErrorCode.HORSE_ALREADY_FOLLOWED);
        }
        SpectatorHorseFollow follow = followRepository.save(SpectatorHorseFollow.builder()
                .spectator(spectator)
                .horse(horse)
                .build());
        return toResponse(horse, spectator, follow.getFollowedAt());
    }

    @Override
    @Transactional
    public void unfollowHorse(UUID horseId) {
        Spectator spectator = getCurrentSpectator();
        SpectatorHorseFollow follow = followRepository
                .findBySpectator_SpectatorIdAndHorse_HorseId(spectator.getSpectatorId(), horseId)
                .orElseThrow(() -> new AppException(ErrorCode.HORSE_FOLLOW_NOT_FOUND));
        followRepository.delete(follow);
    }

    @Override
    @Transactional(readOnly = true)
    public SpectatorHorseResponse getHorseDetail(UUID horseId) {
        Spectator spectator = getCurrentSpectator();
        return toResponse(getHorse(horseId), spectator, null);
    }

    private Spectator getCurrentSpectator() {
        User user = userCurrentService.getCurrentUser();
        if (user.getRole().getRoleName() != RoleName.SPECTATOR) {
            throw new AppException(ErrorCode.ACCESS_DENIED);
        }
        if (user.getStatus() != AccountStatus.ACTIVE) {
            throw new AppException(ErrorCode.USER_INACTIVE);
        }
        return spectatorRepository.findByUser_UserId(user.getUserId())
                .orElseThrow(() -> new AppException(ErrorCode.SPECTATOR_PROFILE_NOT_FOUND));
    }

    private Horse getHorse(UUID horseId) {
        return horseRepository.findById(horseId)
                .orElseThrow(() -> new AppException(ErrorCode.HORSE_NOT_FOUND));
    }

    private SpectatorHorseResponse toResponse(
            Horse horse, Spectator spectator, LocalDateTime followedAt) {
        boolean followed = followRepository.existsBySpectator_SpectatorIdAndHorse_HorseId(
                spectator.getSpectatorId(), horse.getHorseId());
        RaceEntry nextEntry = findNextRaceEntry(horse.getHorseId());
        SpectatorHorseResponse.SpectatorHorseResponseBuilder builder = SpectatorHorseResponse.builder()
                .horseId(horse.getHorseId())
                .horseName(horse.getName())
                .imageUrl(horse.getImageUrl())
                .breed(horse.getBreed())
                .age(horse.getAge())
                .weight(horse.getWeight())
                .raceClass(horse.getRaceClass())
                .currentRating(horse.getCurrentRating())
                .totalRaces(horse.getTotalRaces())
                .totalWins(horse.getTotalWins())
                .totalTop3Finishes(horse.getTotalTop3Finishes())
                .winRate(horse.getWinRate())
                .healthStatus(horse.getHealthStatus())
                .ownerId(horse.getOwner().getOwnerId())
                .ownerName(horse.getOwner().getUser().getFullName())
                .followedByCurrentUser(followed)
                .followedAt(followedAt);
        if (nextEntry != null) {
            builder.nextRaceId(nextEntry.getRace().getRaceId())
                    .nextRaceName(nextEntry.getRace().getName())
                    .nextTournamentName(nextEntry.getRace().getRound().getTournament().getName())
                    .nextRaceStartTime(nextEntry.getRace().getStartTime());
        }
        return builder.build();
    }

    private RaceEntry findNextRaceEntry(UUID horseId) {
        LocalDateTime now = LocalDateTime.now();
        RaceEntry selected = null;
        List<RaceEntry> entries = raceEntryRepository.findByContract_Horse_HorseId(horseId);
        for (RaceEntry entry : entries) {
            if (entry.getRace() == null || entry.getRace().getStartTime() == null
                    || entry.getRace().getSchedulePublishedAt() == null
                    || entry.getRace().getStartTime().isBefore(now)) {
                continue;
            }
            if (selected == null || entry.getRace().getStartTime().isBefore(selected.getRace().getStartTime())) {
                selected = entry;
            }
        }
        return selected;
    }

    private String normalizeOptional(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        return value.trim();
    }

    private void validatePage(int page, int size) {
        if (page < 0 || size < 1 || size > 100) {
            throw new AppException(ErrorCode.INVALID_PAGE_REQUEST);
        }
    }

    private PageResponse<SpectatorHorseResponse> toPageResponse(
            Page<Horse> source, List<SpectatorHorseResponse> items) {
        return new PageResponse<>(items, source.getNumber(), source.getSize(), source.getTotalElements(),
                source.getTotalPages(), source.isFirst(), source.isLast());
    }
}

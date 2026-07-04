package com.swp391.horseracing.service.impl;

import com.swp391.horseracing.dto.horse.request.HorseCreationRequest;
import com.swp391.horseracing.dto.horse.request.HorseUpdateRequest;
import com.swp391.horseracing.dto.horse.response.HorseResponse;
import com.swp391.horseracing.entity.Horse;
import com.swp391.horseracing.entity.HorseOwner;
import com.swp391.horseracing.entity.User;
import com.swp391.horseracing.enums.HealthStatus;
import com.swp391.horseracing.exception.AppException;
import com.swp391.horseracing.exception.ErrorCode;
import com.swp391.horseracing.mapper.HorseMapper;
import com.swp391.horseracing.repository.HorseOwnerRepository;
import com.swp391.horseracing.repository.HorseRepository;
import com.swp391.horseracing.repository.UserRepository;
import com.swp391.horseracing.service.HorseService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequiredArgsConstructor
public class HorseServiceImpl implements HorseService {

    HorseRepository horseRepository;
    HorseOwnerRepository horseOwnerRepository;
    UserRepository userRepository;
    HorseMapper horseMapper;

    @Override
    @Transactional
    public HorseResponse create(HorseCreationRequest request) {
        HorseOwner owner = getCurrentOwner();

        Horse horse = horseMapper.toHorse(request);
        horse.setOwner(owner);
        if (request.getHealthStatus() == null) {
            horse.setHealthStatus(HealthStatus.HEALTHY);
        }
        horse.setCreatedAt(LocalDateTime.now());

        return horseMapper.toHorseResponse(horseRepository.save(horse));
    }

    @Override
    public List<HorseResponse> getAll() {
        HorseOwner owner = getCurrentOwner();
        return horseRepository.findByOwner_OwnerId(owner.getOwnerId())
                .stream()
                .map(horseMapper::toHorseResponse)
                .collect(Collectors.toList());
    }

    @Override
    public HorseResponse getById(UUID horseId) {
        HorseOwner owner = getCurrentOwner();
        Horse horse = horseRepository.findByHorseIdAndOwner_OwnerId(horseId, owner.getOwnerId())
                .orElseThrow(() -> new AppException(ErrorCode.HORSE_NOT_FOUND));
        return horseMapper.toHorseResponse(horse);
    }

    @Override
    @Transactional
    public HorseResponse update(UUID horseId, HorseUpdateRequest request) {
        HorseOwner owner = getCurrentOwner();
        Horse horse = horseRepository.findByHorseIdAndOwner_OwnerId(horseId, owner.getOwnerId())
                .orElseThrow(() -> new AppException(ErrorCode.HORSE_NOT_FOUND));

        horseMapper.updateHorse(horse, request);
        return horseMapper.toHorseResponse(horseRepository.save(horse));
    }

    @Override
    @Transactional
    public void delete(UUID horseId) {
        HorseOwner owner = getCurrentOwner();
        Horse horse = horseRepository.findByHorseIdAndOwner_OwnerId(horseId, owner.getOwnerId())
                .orElseThrow(() -> new AppException(ErrorCode.HORSE_NOT_FOUND));
        horseRepository.delete(horse);
    }

    private HorseOwner getCurrentOwner() {
        var context = SecurityContextHolder.getContext();
        String username = context.getAuthentication().getName();

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        return horseOwnerRepository.findByUser_UserId(user.getUserId())
                .orElseThrow(() -> new AppException(ErrorCode.OWNER_PROFILE_NOT_FOUND));
    }
}

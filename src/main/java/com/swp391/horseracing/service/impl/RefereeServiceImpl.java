package com.swp391.horseracing.service.impl;

import com.swp391.horseracing.dto.referee.request.RefereeCreationRequest;
import com.swp391.horseracing.dto.referee.request.RefereeUpdateRequest;
import com.swp391.horseracing.dto.referee.response.RefereeResponse;
import com.swp391.horseracing.entity.Referee;
import com.swp391.horseracing.entity.User;
import com.swp391.horseracing.enums.RefereeStatus;
import com.swp391.horseracing.exception.AppException;
import com.swp391.horseracing.exception.ErrorCode;
import com.swp391.horseracing.mapper.RefereeMapper;
import com.swp391.horseracing.repository.RaceRefereeRepository;
import com.swp391.horseracing.repository.RefereeRepository;
import com.swp391.horseracing.repository.UserRepository;
import com.swp391.horseracing.service.RefereeService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequiredArgsConstructor
public class RefereeServiceImpl implements RefereeService {

    RefereeRepository refereeRepository;
    UserRepository userRepository;
    RaceRefereeRepository raceRefereeRepository;
    RefereeMapper refereeMapper;

    @Override
    @Transactional
    public RefereeResponse create(RefereeCreationRequest request) {
        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        if (refereeRepository.findByUser_UserId(user.getUserId()).isPresent()) {
            throw new AppException(ErrorCode.DUPLICATE_RESOURCE);
        }

        Referee referee = refereeMapper.toReferee(request);
        referee.setUser(user);
        referee.setStatus(RefereeStatus.AVAILABLE);
        referee.setCreatedAt(LocalDateTime.now());

        return refereeMapper.toRefereeResponse(refereeRepository.save(referee));
    }

    @Override
    @Transactional(readOnly = true)
    public List<RefereeResponse> getAll() {
        return refereeRepository.findAll()
                .stream()
                .map(refereeMapper::toRefereeResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<RefereeResponse> getAllReferees(RefereeStatus status) {
        List<Referee> referees;

        if (status != null) {
            referees = refereeRepository.findByStatus(status);
        } else {
            referees = refereeRepository.findAll();
        }

        return referees.stream()
                .map(refereeMapper::toRefereeResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public RefereeResponse getById(UUID id) {
        Referee referee = refereeRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.REFEREE_PROFILE_NOT_FOUND));
        return refereeMapper.toRefereeResponse(referee);
    }

    @Override
    @Transactional
    public RefereeResponse update(UUID id, RefereeUpdateRequest request) {
        Referee referee = refereeRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.REFEREE_PROFILE_NOT_FOUND));

        refereeMapper.updateReferee(referee, request);
        return refereeMapper.toRefereeResponse(refereeRepository.save(referee));
    }

    @Override
    @Transactional
    public void delete(UUID id) {
        Referee referee = refereeRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.REFEREE_PROFILE_NOT_FOUND));

        if (raceRefereeRepository.existsByReferee_RefereeId(id)) {
            throw new AppException(ErrorCode.RACE_REFEREE_ALREADY_ASSIGNED);
        }

        refereeRepository.delete(referee);
    }
}

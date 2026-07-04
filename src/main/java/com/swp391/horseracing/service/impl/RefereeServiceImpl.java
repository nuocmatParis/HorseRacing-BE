package com.swp391.horseracing.service.impl;

import com.swp391.horseracing.dto.referee.response.RefereeResponse;
import com.swp391.horseracing.entity.Referee;
import com.swp391.horseracing.enums.RefereeStatus;
import com.swp391.horseracing.mapper.RefereeMapper;
import com.swp391.horseracing.repository.RefereeRepository;
import com.swp391.horseracing.service.RefereeService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequiredArgsConstructor
public class RefereeServiceImpl implements RefereeService {

    RefereeRepository refereeRepository;
    RefereeMapper refereeMapper;

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
}

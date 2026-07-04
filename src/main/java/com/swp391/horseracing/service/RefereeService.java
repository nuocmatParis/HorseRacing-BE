package com.swp391.horseracing.service;

import com.swp391.horseracing.dto.referee.response.RefereeResponse;
import com.swp391.horseracing.enums.RefereeStatus;

import java.util.List;

public interface RefereeService {

    List<RefereeResponse> getAllReferees(RefereeStatus status);
}

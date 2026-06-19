package com.swp391.horseracing.service;

import com.swp391.horseracing.dto.jockey.request.JockeyCreationRequest;
import com.swp391.horseracing.dto.jockey.request.JockeyUpdateRequest;
import com.swp391.horseracing.dto.jockey.response.JockeyResponse;
import com.swp391.horseracing.entity.Jockey;
import com.swp391.horseracing.entity.User;
import com.swp391.horseracing.exception.AppException;
import com.swp391.horseracing.exception.ErrorCode;
import com.swp391.horseracing.mapper.JockeyMapper;
import com.swp391.horseracing.repository.JockeyRepository;
import com.swp391.horseracing.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;


public interface JockeyService {
    List<JockeyResponse> getAll();
    JockeyResponse createMyProfile(JockeyCreationRequest request);
    JockeyResponse getById(UUID id);
    JockeyResponse getMyProfile();
    JockeyResponse updateMyProfile(JockeyUpdateRequest request);
}

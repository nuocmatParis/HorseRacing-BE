package com.swp391.horseracing.service.impl;


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
import com.swp391.horseracing.service.JockeyService;
import com.swp391.horseracing.service.UserService;
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
public class JockeyServiceImpl implements JockeyService {

    UserRepository userRepository;
    JockeyRepository jockeyRepository;
    JockeyMapper jockeyMapper;
    private final UserService userService;



    @Override
    public List<JockeyResponse> getAll(){
        return jockeyRepository.findAll()
                .stream()
                .map(jockeyMapper::toJockeyResponse)
                .collect(Collectors.toList());
    }

    private User getCurrentUser(){
        var context = SecurityContextHolder.getContext();
        String userName = context.getAuthentication().getName();
        return userRepository.findByUsername(userName)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
        }


    @Override
    @Transactional
    public JockeyResponse createMyProfile(JockeyCreationRequest request){
        User user = getCurrentUser();
        if (jockeyRepository.existsByUser_UserId(user.getUserId())){
            throw new AppException(ErrorCode.DUPLICATE_RESOURCE);
        }

        Jockey jockey = jockeyMapper.toJockey(request);
        jockey.setUser(user);
        jockey.setCreatedAt(LocalDateTime.now());

        return jockeyMapper.toJockeyResponse(jockeyRepository.save(jockey));
    }

    @Override
    public JockeyResponse getById(UUID id){
        Jockey jockey = jockeyRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.JOCKEY_PROFILE_NOT_FOUND));
        return jockeyMapper.toJockeyResponse(jockey);
    }

    @Override
    public JockeyResponse getMyProfile() {
        User user = getCurrentUser();
        Jockey jockey = jockeyRepository.findByUser_UserId(user.getUserId())
                .orElseThrow(() -> new AppException(ErrorCode.JOCKEY_PROFILE_NOT_FOUND));
        return jockeyMapper.toJockeyResponse(jockey);
    }

    @Override
    public JockeyResponse updateMyProfile(JockeyUpdateRequest request){
        User user = getCurrentUser();
        Jockey jockey = jockeyRepository.findByUser_UserId(user.getUserId())
                .orElseThrow(() -> new AppException(ErrorCode.JOCKEY_PROFILE_NOT_FOUND));
        jockeyMapper.updateJockey(jockey, request);
        return jockeyMapper.toJockeyResponse(jockeyRepository.save(jockey));
    }

}

package com.swp391.horseracing.service.impl;

import com.swp391.horseracing.dto.horse.response.HorseResponse;
import com.swp391.horseracing.dto.jockey.request.JockeyCreationRequest;
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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
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
    @Transactional
    public JockeyResponse create(JockeyCreationRequest request){
        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
        if (jockeyRepository.existsByUser_UserId(request.getUserId())){
            throw new AppException(ErrorCode.DUPLICATE_RESOURCE);
        }

        Jockey jockey = jockeyMapper.toJockey(request);
        jockey.setUser(user);

        return jockeyMapper.toJockeyResponse(jockeyRepository.save(jockey));
    }


    @Override
    public List<JockeyResponse> getAll(){
        return jockeyRepository.findAll()
                .stream()
                .map(jockeyMapper::toJockeyResponse)
                .collect(Collectors.toList());
    }
}

package com.swp391.horseracing.service.impl;

import com.swp391.horseracing.entity.User;
import com.swp391.horseracing.exception.AppException;
import com.swp391.horseracing.exception.ErrorCode;
import com.swp391.horseracing.repository.UserRepository;
import com.swp391.horseracing.service.UserCurrentService;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequiredArgsConstructor
public class UserCurrentServiceImpl implements UserCurrentService {
    UserRepository userRepository;

    @Override
    public User getCurrentUser() {
        var context = SecurityContextHolder.getContext().getAuthentication();

        if(context == null || !context.isAuthenticated()) {
            throw new AppException(ErrorCode.UNAUTHENTICATED);
        }

        String name = context.getName();

        return userRepository.findByUsername(name).orElseThrow(()
                -> new AppException(ErrorCode.USER_NOT_FOUND));
    }
}

package com.swp391.horseracing.service.impl;

import com.swp391.horseracing.dto.HorseOwner.request.OwnerCreationRequest;
import com.swp391.horseracing.dto.HorseOwner.request.OwnerUpdateRequest;
import com.swp391.horseracing.dto.HorseOwner.response.OwnerResponse;
import com.swp391.horseracing.entity.HorseOwner;
import com.swp391.horseracing.entity.User;
import com.swp391.horseracing.exception.AppException;
import com.swp391.horseracing.exception.ErrorCode;
import com.swp391.horseracing.mapper.OwnerMapper;
import com.swp391.horseracing.repository.HorseOwnerRepository;
import com.swp391.horseracing.repository.UserRepository;
import com.swp391.horseracing.service.OwnerService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequiredArgsConstructor
public class OwnerServiceImpl implements OwnerService {

    UserRepository userRepository;
    HorseOwnerRepository ownerRepository;
    OwnerMapper ownerMapper;

    public User getCurrentUser (){
        var context = SecurityContextHolder.getContext();
        String userName = context.getAuthentication().getName();
        return userRepository.findByUsername(userName)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
    }

    @Override
    @Transactional
    public OwnerResponse createMyProfile(OwnerCreationRequest request){
        User user = getCurrentUser();
        if (ownerRepository.existsByUser_UserId(user.getUserId())){
            throw new AppException(ErrorCode.DUPLICATE_RESOURCE);
        }
        HorseOwner owner = ownerMapper.toOwner(request);
        owner.setUser(user);

        return ownerMapper.toOwnerResponse(ownerRepository.save(owner));
    }

    @Override
    public  OwnerResponse updateMyProfile(OwnerUpdateRequest request){
        User user = getCurrentUser();
        HorseOwner owner = ownerRepository.findByUser_UserId(user.getUserId())
                .orElseThrow(() -> new AppException(ErrorCode.OWNER_PROFILE_NOT_FOUND));
        ownerMapper.updateOwner(owner, request);
        return ownerMapper.toOwnerResponse(ownerRepository.save(owner));
    }

    @Override
    public OwnerResponse getMyProfile(){
        User user = getCurrentUser();
        HorseOwner owner = ownerRepository.findByUser_UserId(user.getUserId())
                .orElseThrow(() -> new AppException(ErrorCode.OWNER_PROFILE_NOT_FOUND));
        return ownerMapper.toOwnerResponse(owner);
    }

}

package com.swp391.horseracing.service.impl;

import com.swp391.horseracing.dto.user.request.UserCreationRequest;
import com.swp391.horseracing.dto.user.response.UserResponse;
import com.swp391.horseracing.entity.Role;
import com.swp391.horseracing.entity.User;
import com.swp391.horseracing.enums.AccountStatus;
import com.swp391.horseracing.enums.RoleName;
import com.swp391.horseracing.exception.AppException;
import com.swp391.horseracing.exception.ErrorCode;
import com.swp391.horseracing.mapper.UserMapper;
import com.swp391.horseracing.repository.RoleRepository;
import com.swp391.horseracing.repository.UserRepository;
import com.swp391.horseracing.service.UserService;
import com.swp391.horseracing.service.WalletService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Service
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {
    UserRepository userRepository;
    RoleRepository roleRepository;
    PasswordEncoder passwordEncoder;
    UserMapper userMapper;
    WalletService walletService;

    static Set<RoleName> SELF_REGISTER_ALLOWED_ROLES = Set.of(
            RoleName.SPECTATOR,
            RoleName.HORSE_OWNER,
            RoleName.JOCKEY);

    @Override
    @Transactional
    public UserResponse create(UserCreationRequest request) {

        if(!SELF_REGISTER_ALLOWED_ROLES.contains(request.getRoleName())){
            throw new AppException(ErrorCode.ROLE_NOT_ALLOWED);
        }

        Role role = roleRepository.findByRoleName(request.getRoleName()).orElseThrow(()
                -> new AppException(ErrorCode.ROLE_NOT_FOUND));

        if(userRepository.existsByUsername(request.getUsername()))
            throw new AppException(ErrorCode.USER_ALREADY_EXISTS);

        if(userRepository.existsByEmail(request.getEmail())){
            throw new AppException(ErrorCode.EMAIL_ALREADY_EXISTS);
        }

        if(userRepository.existsByPhoneNumber(request.getPhoneNumber()))
            throw new AppException(ErrorCode.PHONE_NUMBER_ALREADY_EXISTS);

        User user = userMapper.toUser(request);
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRole(role);
        user.setStatus(AccountStatus.ACTIVE);
        user.setCreatedAt(LocalDateTime.now());

        User savedUser = userRepository.save(user);

        RoleName roleName = savedUser.getRole().getRoleName();

        if (roleName == RoleName.HORSE_OWNER || roleName == RoleName.JOCKEY) {
            walletService.createUserWallet(savedUser);
        }
        return userMapper.toUserResponse(savedUser);
    }

    @Override
    public List<UserResponse> findAll() {
        List<User> list = userRepository.findAll();

        List<UserResponse> responseList = new ArrayList<>();
        for(User user : list){
            responseList.add(userMapper.toUserResponse(user));
        }
        return responseList;
    }
}

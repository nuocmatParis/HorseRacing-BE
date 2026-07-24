package com.swp391.horseracing.service.impl;

import com.swp391.horseracing.dto.auth.request.ResendOtp;
import com.swp391.horseracing.dto.auth.request.VerifyEmail;
import com.swp391.horseracing.dto.user.request.UserCreationRequest;
import com.swp391.horseracing.dto.user.response.UserResponse;
import com.swp391.horseracing.entity.EmailVerification;
import com.swp391.horseracing.entity.Role;
import com.swp391.horseracing.entity.Spectator;
import com.swp391.horseracing.entity.User;
import com.swp391.horseracing.enums.AccountStatus;
import com.swp391.horseracing.enums.RoleName;
import com.swp391.horseracing.exception.AppException;
import com.swp391.horseracing.exception.ErrorCode;
import com.swp391.horseracing.mapper.EmailVerificationMapper;
import com.swp391.horseracing.mapper.UserMapper;
import com.swp391.horseracing.repository.EmailVerificationRepository;
import com.swp391.horseracing.repository.RoleRepository;
import com.swp391.horseracing.repository.SpectatorRepository;
import com.swp391.horseracing.repository.UserRepository;
import com.swp391.horseracing.service.CloudinaryService;
import com.swp391.horseracing.service.EmailService;
import com.swp391.horseracing.service.UserService;
import com.swp391.horseracing.service.WalletService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
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
    EmailService emailService;
    EmailVerificationRepository emailVerificationRepository;
    EmailVerificationMapper emailVerificationMapper;
    CloudinaryService cloudinaryService;
    SpectatorRepository spectatorRepository;

    static Set<RoleName> SELF_REGISTER_ALLOWED_ROLES = Set.of(
            RoleName.SPECTATOR,
            RoleName.HORSE_OWNER,
            RoleName.JOCKEY);

    static  int OTP_EXPIRE_MINITUES = 5;

    @Override
    @Transactional
    public void requestRegisterOtp(UserCreationRequest request) {
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

        checkPendingRegisterNotConflict(request);

        String otpCode = generateOtp();
        EmailVerification emailVerification = emailVerificationMapper.toEmailVerification(request);
        emailVerification.setOtpCode(otpCode);
        emailVerification.setExpiredAt(LocalDateTime.now().plusMinutes(OTP_EXPIRE_MINITUES));
        emailVerification.setCreatedAt(LocalDateTime.now());

        emailVerificationRepository.save(emailVerification);

        emailService.sendOtp(request.getEmail(), otpCode);
    }

    @Override
    @Transactional
    public UserResponse verifyRegisterOtp(VerifyEmail request) {
        EmailVerification emailVerification = emailVerificationRepository.findByEmail(request.getEmail())
                .orElseThrow(()
                        -> new AppException(ErrorCode.EMAIL_VERIFICATION_NOT_FOUND));

        Role role = roleRepository.findByRoleName(emailVerification.getRoleName()).orElseThrow(()
                -> new AppException(ErrorCode.ROLE_NOT_FOUND));

        if(emailVerification.getExpiredAt().isBefore(LocalDateTime.now()))
            throw new AppException(ErrorCode.OTP_EXPIRED);

        if(!emailVerification.getOtpCode().equals(request.getOtpCode()))
            throw new AppException(ErrorCode.INVALID_OTP);

        User user = userMapper.toUser(emailVerification);
        user.setPassword(passwordEncoder.encode(emailVerification.getPassword()));
        user.setRole(role);
        user.setStatus(AccountStatus.ACTIVE);
        user.setCreatedAt(LocalDateTime.now());

        User savedUser = userRepository.save(user);

        RoleName roleName = savedUser.getRole().getRoleName();

        if (roleName == RoleName.HORSE_OWNER || roleName == RoleName.JOCKEY) {
            walletService.createUserWallet(savedUser);
        }
        if (roleName == RoleName.SPECTATOR) {
            Spectator spectator = Spectator.builder()
                    .user(savedUser)
                    .totalPoints(0)
                    .build();
            spectatorRepository.save(spectator);
        }

        emailVerificationRepository.delete(emailVerification);

        return userMapper.toUserResponse(savedUser);
    }

    @Override
    public void resendOtp(ResendOtp request) {
        EmailVerification verification = emailVerificationRepository.findByEmail(request.getEmail())
                .orElseThrow(()
                        -> new AppException(ErrorCode.EMAIL_VERIFICATION_NOT_FOUND));

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new AppException(ErrorCode.EMAIL_ALREADY_EXISTS);
        }

        String otpCode = generateOtp();

        verification.setOtpCode(otpCode);
        verification.setExpiredAt(LocalDateTime.now().plusMinutes(OTP_EXPIRE_MINITUES));
        verification.setCreatedAt(LocalDateTime.now());

        emailVerificationRepository.save(verification);

        emailService.sendOtp(request.getEmail(), otpCode);
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

    @Override
    @Transactional
    public UserResponse uploadAvatar(MultipartFile file) {
        var context = SecurityContextHolder.getContext();
        String username = context.getAuthentication().getName();
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        try {
            String imageUrl = cloudinaryService.uploadImage(file, "avatars");
            user.setImageUrl(imageUrl);
            return userMapper.toUserResponse(userRepository.save(user));
        } catch (IOException e) {
            throw new AppException(ErrorCode.FILE_UPLOAD_FAILED);
        }
    }

    private String generateOtp(){
        int otp = new SecureRandom().nextInt(900000) + 100000;
        return String.valueOf(otp);
    }

    private void checkPendingRegisterNotConflict(UserCreationRequest request) {
        Optional<EmailVerification> optionalVerification =
                emailVerificationRepository.findByUsername(request.getUsername());

        if (optionalVerification.isPresent()) {
            EmailVerification verification = optionalVerification.get();

            boolean isDifferentEmail =
                    !verification.getEmail().equals(request.getEmail());

            if (isDifferentEmail) {
                throw new AppException(ErrorCode.USERNAME_PENDING_VERIFICATION);
            }
        }
    }
}

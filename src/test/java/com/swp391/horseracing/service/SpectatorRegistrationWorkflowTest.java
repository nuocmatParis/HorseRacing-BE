package com.swp391.horseracing.service;

import com.swp391.horseracing.dto.auth.request.VerifyEmail;
import com.swp391.horseracing.entity.EmailVerification;
import com.swp391.horseracing.entity.Role;
import com.swp391.horseracing.entity.Spectator;
import com.swp391.horseracing.entity.User;
import com.swp391.horseracing.enums.RoleName;
import com.swp391.horseracing.mapper.EmailVerificationMapper;
import com.swp391.horseracing.mapper.UserMapper;
import com.swp391.horseracing.repository.EmailVerificationRepository;
import com.swp391.horseracing.repository.RoleRepository;
import com.swp391.horseracing.repository.SpectatorRepository;
import com.swp391.horseracing.repository.UserRepository;
import com.swp391.horseracing.service.impl.UserServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SpectatorRegistrationWorkflowTest {

    @Mock UserRepository userRepository;
    @Mock RoleRepository roleRepository;
    @Mock PasswordEncoder passwordEncoder;
    @Mock UserMapper userMapper;
    @Mock WalletService walletService;
    @Mock EmailService emailService;
    @Mock EmailVerificationRepository emailVerificationRepository;
    @Mock EmailVerificationMapper emailVerificationMapper;
    @Mock CloudinaryService cloudinaryService;
    @Mock SpectatorRepository spectatorRepository;
    @InjectMocks UserServiceImpl service;

    @Test
    void verifyingSpectatorOtpCreatesSpectatorProfileWithoutWallet() {
        VerifyEmail request = VerifyEmail.builder()
                .email("spectator@example.com")
                .otpCode("123456")
                .build();
        EmailVerification verification = EmailVerification.builder()
                .email(request.getEmail())
                .otpCode(request.getOtpCode())
                .password("plain-password")
                .roleName(RoleName.SPECTATOR)
                .expiredAt(LocalDateTime.now().plusMinutes(5))
                .build();
        Role role = Role.builder().roleName(RoleName.SPECTATOR).build();
        User user = new User();
        user.setUserId(UUID.randomUUID());

        when(emailVerificationRepository.findByEmail(request.getEmail()))
                .thenReturn(Optional.of(verification));
        when(roleRepository.findByRoleName(RoleName.SPECTATOR)).thenReturn(Optional.of(role));
        when(userMapper.toUser(verification)).thenReturn(user);
        when(passwordEncoder.encode("plain-password")).thenReturn("encoded-password");
        when(userRepository.save(user)).thenReturn(user);

        service.verifyRegisterOtp(request);

        ArgumentCaptor<Spectator> spectatorCaptor = ArgumentCaptor.forClass(Spectator.class);
        verify(spectatorRepository).save(spectatorCaptor.capture());
        assertSame(user, spectatorCaptor.getValue().getUser());
        assertEquals(0, spectatorCaptor.getValue().getTotalPoints());
        verify(walletService, never()).createUserWallet(user);
        verify(emailVerificationRepository).delete(verification);
    }
}

package com.swp391.horseracing.service;

import com.swp391.horseracing.dto.horseowner.request.OwnerCreationRequest;
import com.swp391.horseracing.entity.Role;
import com.swp391.horseracing.entity.User;
import com.swp391.horseracing.enums.RoleName;
import com.swp391.horseracing.exception.AppException;
import com.swp391.horseracing.exception.ErrorCode;
import com.swp391.horseracing.mapper.OwnerMapper;
import com.swp391.horseracing.repository.HorseOwnerRepository;
import com.swp391.horseracing.repository.UserRepository;
import com.swp391.horseracing.service.impl.OwnerServiceImpl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OwnerProfileAuthorizationTest {

    @Mock UserRepository userRepository;
    @Mock HorseOwnerRepository ownerRepository;
    @Mock OwnerMapper ownerMapper;
    @InjectMocks OwnerServiceImpl service;

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void nonOwnerRoleCannotCreateOwnerProfile() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("spectator", "password"));
        Role spectatorRole = Role.builder().roleName(RoleName.SPECTATOR).build();
        User user = new User();
        user.setUserId(UUID.randomUUID());
        user.setRole(spectatorRole);
        when(userRepository.findByUsername("spectator")).thenReturn(Optional.of(user));

        OwnerCreationRequest request = new OwnerCreationRequest();
        AppException exception = assertThrows(AppException.class,
                () -> service.createMyProfile(request));

        assertEquals(ErrorCode.ACCESS_DENIED, exception.getErrorCode());
        verify(ownerMapper, never()).toOwner(request);
    }
}

package com.swp391.horseracing.mapper;

import com.swp391.horseracing.dto.user.request.UserCreationRequest;
import com.swp391.horseracing.entity.EmailVerification;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface EmailVerificationMapper {
    EmailVerification toEmailVerification(UserCreationRequest request);
}

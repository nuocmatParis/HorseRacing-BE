package com.swp391.horseracing.mapper;

import com.swp391.horseracing.dto.HorseOwner.request.OwnerCreationRequest;
import com.swp391.horseracing.dto.HorseOwner.request.OwnerUpdateRequest;
import com.swp391.horseracing.dto.HorseOwner.response.OwnerResponse;
import com.swp391.horseracing.entity.HorseOwner;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface OwnerMapper {
    @Mapping(target = "userId", source = "user.userId")
    @Mapping(target = "fullName", source = "user.fullName")
    OwnerResponse toOwnerResponse(HorseOwner owner);

    @Mapping(target = "ownerId", ignore = true)
    @Mapping(target = "user", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    HorseOwner toOwner(OwnerCreationRequest request);

    @Mapping(target = "ownerId", ignore = true)
    @Mapping(target = "user", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    void updateOwner(@MappingTarget HorseOwner owner, OwnerUpdateRequest request);
}

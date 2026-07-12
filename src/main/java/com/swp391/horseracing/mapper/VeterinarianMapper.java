package com.swp391.horseracing.mapper;

import com.swp391.horseracing.dto.veterinarian.request.VeterinarianCreationRequest;
import com.swp391.horseracing.dto.veterinarian.request.VeterinarianUpdateRequest;
import com.swp391.horseracing.dto.veterinarian.response.VeterinarianResponse;
import com.swp391.horseracing.entity.Veterinarian;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface VeterinarianMapper {

    @Mapping(target = "userId", source = "user.userId")
    @Mapping(target = "fullName", source = "user.fullName")
    @Mapping(target = "email", source = "user.email")
    @Mapping(target = "username", source = "user.username")
    VeterinarianResponse toVeterinarianResponse(Veterinarian veterinarian);

    @Mapping(target = "vetId", ignore = true)
    @Mapping(target = "user", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    Veterinarian toVeterinarian(VeterinarianCreationRequest request);

    @Mapping(target = "vetId", ignore = true)
    @Mapping(target = "user", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    void updateVeterinarian(@MappingTarget Veterinarian veterinarian, VeterinarianUpdateRequest request);
}

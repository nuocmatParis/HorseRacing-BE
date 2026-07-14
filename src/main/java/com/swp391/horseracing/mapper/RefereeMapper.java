package com.swp391.horseracing.mapper;

import com.swp391.horseracing.dto.referee.request.RefereeCreationRequest;
import com.swp391.horseracing.dto.referee.request.RefereeUpdateRequest;
import com.swp391.horseracing.dto.referee.response.RefereeResponse;
import com.swp391.horseracing.entity.Referee;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface RefereeMapper {

    @Mapping(target = "userId", source = "user.userId")
    @Mapping(target = "fullName", source = "user.fullName")
    @Mapping(target = "email", source = "user.email")
    @Mapping(target = "username", source = "user.username")
    RefereeResponse toRefereeResponse(Referee referee);

    @Mapping(target = "refereeId", ignore = true)
    @Mapping(target = "user", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    Referee toReferee(RefereeCreationRequest request);

    @Mapping(target = "refereeId", ignore = true)
    @Mapping(target = "user", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    void updateReferee(@MappingTarget Referee referee, RefereeUpdateRequest request);
}

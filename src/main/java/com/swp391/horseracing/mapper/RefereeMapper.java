package com.swp391.horseracing.mapper;

import com.swp391.horseracing.dto.referee.response.RefereeResponse;
import com.swp391.horseracing.entity.Referee;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface RefereeMapper {

    @Mapping(target = "userId", source = "user.userId")
    @Mapping(target = "fullName", source = "user.fullName")
    @Mapping(target = "email", source = "user.email")
    @Mapping(target = "username", source = "user.username")
    RefereeResponse toRefereeResponse(Referee referee);
}

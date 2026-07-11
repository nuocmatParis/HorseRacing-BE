package com.swp391.horseracing.mapper;

import com.swp391.horseracing.dto.appeal_evidence.response.AppealEvidenceResponse;
import com.swp391.horseracing.entity.AppealEvidence;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface AppealEvidenceMapper {

    @Mapping(target = "appealId", source = "appeal.appealId")
    AppealEvidenceResponse toAppealEvidenceResponse(AppealEvidence appealEvidence);
}

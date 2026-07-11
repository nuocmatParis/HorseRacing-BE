package com.swp391.horseracing.mapper;

import com.swp391.horseracing.dto.appeal_category.request.CreateAppealCategoryRequest;
import com.swp391.horseracing.dto.appeal_category.response.AppealCategoryResponse;
import com.swp391.horseracing.entity.AppealCategory;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface AppealCategoryMapper {

    AppealCategoryResponse toAppealCategoryResponse(AppealCategory appealCategory);

    @Mapping(target = "categoryId", ignore = true)
    @Mapping(target = "isActive", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    AppealCategory toAppealCategory(CreateAppealCategoryRequest request);
}

package com.swp391.horseracing.service;

import com.swp391.horseracing.dto.appeal_category.request.CreateAppealCategoryRequest;
import com.swp391.horseracing.dto.appeal_category.response.AppealCategoryResponse;

import java.util.List;
import java.util.UUID;

public interface AppealCategoryService {

    AppealCategoryResponse create(CreateAppealCategoryRequest request);

    AppealCategoryResponse update(UUID categoryId, CreateAppealCategoryRequest request);

    AppealCategoryResponse getById(UUID categoryId);

    List<AppealCategoryResponse> getAll();

    void toggleActive(UUID categoryId);
}

package com.swp391.horseracing.service.impl;

import com.swp391.horseracing.dto.appeal_category.request.CreateAppealCategoryRequest;
import com.swp391.horseracing.dto.appeal_category.response.AppealCategoryResponse;
import com.swp391.horseracing.entity.AppealCategory;
import com.swp391.horseracing.exception.AppException;
import com.swp391.horseracing.exception.ErrorCode;
import com.swp391.horseracing.mapper.AppealCategoryMapper;
import com.swp391.horseracing.repository.AppealCategoryRepository;
import com.swp391.horseracing.service.AppealCategoryService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequiredArgsConstructor
public class AppealCategoryServiceImpl implements AppealCategoryService {

    AppealCategoryRepository appealCategoryRepository;
    AppealCategoryMapper appealCategoryMapper;

    @Override
    @Transactional
    public AppealCategoryResponse create(CreateAppealCategoryRequest request) {
        if (appealCategoryRepository.existsByCode(request.getCode())) {
            throw new AppException(ErrorCode.APPEAL_CATEGORY_CODE_EXISTS);
        }

        AppealCategory category = appealCategoryMapper.toAppealCategory(request);
        category.setIsActive(true);

        return appealCategoryMapper.toAppealCategoryResponse(appealCategoryRepository.save(category));
    }

    @Override
    @Transactional
    public AppealCategoryResponse update(UUID categoryId, CreateAppealCategoryRequest request) {
        AppealCategory category = appealCategoryRepository.findById(categoryId)
                .orElseThrow(() -> new AppException(ErrorCode.APPEAL_CATEGORY_NOT_FOUND));

        if (!category.getCode().equals(request.getCode())
                && appealCategoryRepository.existsByCode(request.getCode())) {
            throw new AppException(ErrorCode.APPEAL_CATEGORY_CODE_EXISTS);
        }

        category.setCode(request.getCode());
        category.setName(request.getName());
        category.setDescription(request.getDescription());

        return appealCategoryMapper.toAppealCategoryResponse(appealCategoryRepository.save(category));
    }

    @Override
    public AppealCategoryResponse getById(UUID categoryId) {
        AppealCategory category = appealCategoryRepository.findById(categoryId)
                .orElseThrow(() -> new AppException(ErrorCode.APPEAL_CATEGORY_NOT_FOUND));
        return appealCategoryMapper.toAppealCategoryResponse(category);
    }

    @Override
    public List<AppealCategoryResponse> getAll() {
        return appealCategoryRepository.findByIsActiveTrue()
                .stream()
                .map(appealCategoryMapper::toAppealCategoryResponse)
                .toList();
    }

    @Override
    @Transactional
    public void toggleActive(UUID categoryId) {
        AppealCategory category = appealCategoryRepository.findById(categoryId)
                .orElseThrow(() -> new AppException(ErrorCode.APPEAL_CATEGORY_NOT_FOUND));
        category.setIsActive(!category.getIsActive());
        appealCategoryRepository.save(category);
    }
}

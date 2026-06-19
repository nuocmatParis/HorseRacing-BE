package com.swp391.horseracing.service;

import com.swp391.horseracing.dto.horseowner.request.OwnerCreationRequest;
import com.swp391.horseracing.dto.horseowner.request.OwnerUpdateRequest;
import com.swp391.horseracing.dto.horseowner.response.OwnerResponse;

public interface OwnerService {
    OwnerResponse createMyProfile(OwnerCreationRequest request);
    OwnerResponse updateMyProfile(OwnerUpdateRequest request);
    OwnerResponse getMyProfile();
}

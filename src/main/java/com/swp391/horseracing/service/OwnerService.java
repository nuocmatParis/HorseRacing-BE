package com.swp391.horseracing.service;

import com.swp391.horseracing.dto.HorseOwner.request.OwnerCreationRequest;
import com.swp391.horseracing.dto.HorseOwner.request.OwnerUpdateRequest;
import com.swp391.horseracing.dto.HorseOwner.response.OwnerResponse;

public interface OwnerService {
    OwnerResponse createMyProfile(OwnerCreationRequest request);
    OwnerResponse updateMyProfile(OwnerUpdateRequest request);
    OwnerResponse getMyProfile();
}

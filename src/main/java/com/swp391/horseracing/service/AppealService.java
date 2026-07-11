package com.swp391.horseracing.service;

import com.swp391.horseracing.dto.appeal.request.CreateAppealRequest;
import com.swp391.horseracing.dto.appeal.request.ReviewAppealRequest;
import com.swp391.horseracing.dto.appeal.response.AppealResponse;

import java.util.List;
import java.util.UUID;

public interface AppealService {

    AppealResponse create(CreateAppealRequest request);

    AppealResponse update(UUID appealId, CreateAppealRequest request);

    void cancel(UUID appealId);

    List<AppealResponse> getMyAppeals();

    List<AppealResponse> getAllAppeals();

    AppealResponse getAppealDetail(UUID appealId);

    AppealResponse review(UUID appealId, ReviewAppealRequest request);
}

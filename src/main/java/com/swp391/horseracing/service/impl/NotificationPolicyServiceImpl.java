package com.swp391.horseracing.service.impl;

import com.swp391.horseracing.enums.NotificationEventType;
import com.swp391.horseracing.service.NotificationPolicyService;
import org.springframework.stereotype.Service;

@Service
public class NotificationPolicyServiceImpl implements NotificationPolicyService {
    @Override
    public boolean defaultInAppEnabled(NotificationEventType eventType) {
        return true;
    }

    @Override
    public boolean defaultEmailEnabled(NotificationEventType eventType) {
        return true;
    }

    @Override
    public boolean showToast(NotificationEventType eventType) {
        return eventType != NotificationEventType.TOURNAMENT_PUBLISHED
                && eventType != NotificationEventType.PREDICTION_SCORED;
    }
}

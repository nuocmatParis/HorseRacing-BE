package com.swp391.horseracing.service;

import com.swp391.horseracing.enums.NotificationEventType;

public interface NotificationPolicyService {
    boolean defaultInAppEnabled(NotificationEventType eventType);
    boolean defaultEmailEnabled(NotificationEventType eventType);
    boolean showToast(NotificationEventType eventType);
}

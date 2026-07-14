package com.swp391.horseracing.service;

import com.swp391.horseracing.dto.notification.NotificationMessage;
import com.swp391.horseracing.entity.NotificationEvent;

public interface NotificationTemplateService {
    NotificationMessage build(NotificationEvent event);
}

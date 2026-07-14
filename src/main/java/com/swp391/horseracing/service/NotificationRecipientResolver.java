package com.swp391.horseracing.service;

import com.swp391.horseracing.entity.NotificationEvent;

import java.util.Set;
import java.util.UUID;

public interface NotificationRecipientResolver {
    Set<UUID> resolve(NotificationEvent event);
}

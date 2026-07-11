package com.swp391.horseracing.mapper;

import com.swp391.horseracing.dto.notification.response.NotificationResponse;
import com.swp391.horseracing.entity.Notification;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface NotificationMapper {

    NotificationResponse toNotificationResponse(Notification notification);
}

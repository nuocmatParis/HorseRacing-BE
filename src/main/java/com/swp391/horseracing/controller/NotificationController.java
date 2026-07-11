package com.swp391.horseracing.controller;

import com.swp391.horseracing.dto.common.ApiResponse;
import com.swp391.horseracing.dto.notification.response.NotificationResponse;
import com.swp391.horseracing.service.NotificationService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequestMapping("/api/notifications")
public class NotificationController {

    NotificationService notificationService;

    @GetMapping
    public ApiResponse<List<NotificationResponse>> getMyNotifications() {
        return ApiResponse.<List<NotificationResponse>>builder()
                .result(notificationService.getMyNotifications())
                .build();
    }

    @PatchMapping("/{notiId}/read")
    public ApiResponse<Void> markAsRead(@PathVariable UUID notiId) {
        notificationService.markAsRead(notiId);
        return ApiResponse.<Void>builder().build();
    }

    @GetMapping("/unread-count")
    public ApiResponse<Map<String, Integer>> getUnreadCount() {
        return ApiResponse.<Map<String, Integer>>builder()
                .result(Map.of("count", notificationService.getUnreadCount()))
                .build();
    }
}

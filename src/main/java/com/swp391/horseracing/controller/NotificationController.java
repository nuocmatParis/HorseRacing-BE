package com.swp391.horseracing.controller;

import com.swp391.horseracing.dto.common.ApiResponse;
import com.swp391.horseracing.dto.common.PageResponse;
import com.swp391.horseracing.dto.notification.request.UpdateNotificationPreferenceRequest;
import com.swp391.horseracing.dto.notification.response.NotificationPreferenceResponse;
import com.swp391.horseracing.dto.notification.response.NotificationResponse;
import com.swp391.horseracing.enums.NotificationEventType;
import com.swp391.horseracing.service.NotificationService;
import jakarta.validation.Valid;
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
    public ApiResponse<PageResponse<NotificationResponse>> getMyNotifications(
            @RequestParam(required = false) Boolean isRead,
            @RequestParam(required = false) NotificationEventType type,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.<PageResponse<NotificationResponse>>builder()
                .result(notificationService.getMyNotifications(isRead, type, page, size))
                .build();
    }

    @GetMapping("/unread-count")
    public ApiResponse<Map<String, Long>> getUnreadCount() {
        return ApiResponse.<Map<String, Long>>builder()
                .result(Map.of("count", notificationService.getUnreadCount()))
                .build();
    }

    @PatchMapping("/{notificationId}/read")
    public ApiResponse<Void> markAsRead(@PathVariable UUID notificationId) {
        notificationService.markAsRead(notificationId);
        return ApiResponse.<Void>builder().build();
    }

    @PatchMapping("/read-all")
    public ApiResponse<Map<String, Integer>> markAllAsRead() {
        return ApiResponse.<Map<String, Integer>>builder()
                .result(Map.of("updated", notificationService.markAllAsRead()))
                .build();
    }

    @DeleteMapping("/{notificationId}")
    public ApiResponse<Void> archive(@PathVariable UUID notificationId) {
        notificationService.archive(notificationId);
        return ApiResponse.<Void>builder().build();
    }

    @GetMapping("/preferences")
    public ApiResponse<List<NotificationPreferenceResponse>> getPreferences() {
        return ApiResponse.<List<NotificationPreferenceResponse>>builder()
                .result(notificationService.getMyPreferences())
                .build();
    }

    @PutMapping("/preferences/{eventType}")
    public ApiResponse<NotificationPreferenceResponse> updatePreference(
            @PathVariable NotificationEventType eventType,
            @RequestBody @Valid UpdateNotificationPreferenceRequest request) {
        return ApiResponse.<NotificationPreferenceResponse>builder()
                .result(notificationService.updateMyPreference(eventType, request))
                .build();
    }
}

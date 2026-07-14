package com.swp391.horseracing.dto.notification.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class UpdateNotificationPreferenceRequest {
    @NotNull
    private Boolean inAppEnabled;

    @NotNull
    private Boolean emailEnabled;
}

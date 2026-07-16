package com.swp391.horseracing.service;

import tools.jackson.databind.ObjectMapper;
import com.swp391.horseracing.dto.notification.NotificationMessage;
import com.swp391.horseracing.entity.NotificationEvent;
import com.swp391.horseracing.enums.NotificationEventType;
import com.swp391.horseracing.service.impl.NotificationTemplateServiceImpl;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class NotificationTemplateServiceTest {
    @Test
    void contractActivationNotificationDoesNotMentionAdminApproval() {
        NotificationPolicyService policyService = mock(NotificationPolicyService.class);
        NotificationTemplateServiceImpl service =
                new NotificationTemplateServiceImpl(new ObjectMapper(), policyService);
        NotificationEvent event = NotificationEvent.builder()
                .eventType(NotificationEventType.CONTRACT_APPROVED)
                .payloadJson("{\"horseName\":\"Sấm Sét\",\"tournamentName\":\"Cúp Mùa Hè\"}")
                .build();

        NotificationMessage message = service.build(event);

        assertEquals("Hợp đồng đã có hiệu lực", message.title());
        assertTrue(message.content().contains("đã có hiệu lực"));
        assertFalse(message.content().toLowerCase().contains("admin"));
        assertFalse(message.content().contains("được duyệt"));
    }

    @Test
    void everyEventTypeHasAVietnameseTemplate() {
        NotificationPolicyService policyService = mock(NotificationPolicyService.class);
        NotificationTemplateServiceImpl service =
                new NotificationTemplateServiceImpl(new ObjectMapper(), policyService);
        String payload = """
                {
                  "tournamentName":"Giải mùa hè",
                  "raceName":"Race 1",
                  "horseName":"Ngựa A",
                  "jockeyName":"Jockey A",
                  "reason":"Mưa lớn",
                  "oldStartTime":"2026-07-20T08:00:00",
                  "newStartTime":"2026-07-20T09:00:00",
                  "points":10
                }
                """;

        for (NotificationEventType type : NotificationEventType.values()) {
            when(policyService.showToast(type)).thenReturn(true);
            NotificationEvent event = NotificationEvent.builder()
                    .eventType(type)
                    .payloadJson(payload)
                    .build();

            NotificationMessage message = service.build(event);

            assertNotNull(message.title(), type.name());
            assertFalse(message.title().isBlank(), type.name());
            assertNotNull(message.content(), type.name());
            assertFalse(message.content().isBlank(), type.name());
        }
    }
}

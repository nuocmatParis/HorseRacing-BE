package com.swp391.horseracing.service;

public interface NotificationEmailService {
    void send(String toEmail, String subject, String content);
}

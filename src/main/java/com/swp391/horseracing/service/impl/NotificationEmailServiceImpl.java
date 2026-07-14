package com.swp391.horseracing.service.impl;

import com.swp391.horseracing.service.NotificationEmailService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class NotificationEmailServiceImpl implements NotificationEmailService {
    JavaMailSender mailSender;

    @Override
    public void send(String toEmail, String subject, String content) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(toEmail);
        message.setSubject("Horse Racing - " + subject);
        message.setText(content);
        mailSender.send(message);
    }
}

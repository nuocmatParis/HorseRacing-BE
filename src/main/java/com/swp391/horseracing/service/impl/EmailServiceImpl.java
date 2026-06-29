package com.swp391.horseracing.service.impl;

import com.swp391.horseracing.service.EmailService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class EmailServiceImpl implements EmailService {
    JavaMailSender mailSender;

    @Override
    public void sendOtp(String toEmail, String otpCode) {
        SimpleMailMessage mailMessage = new SimpleMailMessage();

        mailMessage.setTo(toEmail);
        mailMessage.setSubject("Horse Racing - Email Verification");
        mailMessage.setText("""
            Your verification code is: %s
            
            This code will expire in 5 minutes.
            """.formatted(otpCode));

        mailSender.send(mailMessage);
    }
}
